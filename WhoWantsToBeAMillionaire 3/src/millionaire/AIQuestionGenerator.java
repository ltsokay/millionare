package millionaire;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Динамическая генерация вопросов средствами генеративного ИИ через REST-API
 */
public class AIQuestionGenerator {

    private static final String CONFIG_FILE = "ai_config.properties";

    private final boolean enabled;
    private final String url;
    private final String apiKey;
    private final String model;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public AIQuestionGenerator() {
        Properties props = new Properties();
        Path config = Path.of(CONFIG_FILE);
        if (Files.exists(config)) {
            try {
                props.load(Files.newBufferedReader(config, StandardCharsets.UTF_8));
            } catch (IOException ex) {
                System.out.println("Не удалось прочитать " + CONFIG_FILE + ": " + ex.getMessage());
            }
        }
        this.enabled = Boolean.parseBoolean(props.getProperty("ai.enabled", "false").trim());
        this.url = props.getProperty("ai.url", "https://api.openai.com/v1/chat/completions").trim();
        this.apiKey = props.getProperty("ai.key", "").trim();
        this.model = props.getProperty("ai.model", "gpt-4o-mini").trim();
    }

    public boolean isEnabled() {
        return enabled && !apiKey.isEmpty();
    }

    /**
     * Сгенерировать вопрос заданного уровня сложности (1..15)
     * Возвращает null при любой ошибке — игра продолжается на вопросах из БД
     */
    public Question generate(int level) {
        if (!isEnabled()) {
            return null;
        }
        try {
            String content = requestCompletion(buildPrompt(level));
            return parseQuestion(content, level);
        } catch (Exception ex) {
            System.out.println("Генерация вопроса ИИ не удалась: " + ex.getMessage());
            return null;
        }
    }

    private String buildPrompt(int level) {
        return "Сгенерируй один вопрос для игры \"Кто хочет стать миллионером\" "
                + "на русском языке. Уровень сложности — " + level + " из 15 "
                + "(1 — очень лёгкий, 15 — очень трудный). "
                + "Вопрос должен иметь ровно 4 варианта ответа, из которых верен только один. "
                + "Ответь СТРОГО в формате JSON без пояснений и без markdown-разметки: "
                + "{\"text\":\"текст вопроса\",\"answers\":[\"вариант1\",\"вариант2\","
                + "\"вариант3\",\"вариант4\"],\"correct\":N}, где N — номер верного "
                + "варианта от 1 до 4.";
    }

    /** Отправляет запрос к API и возвращает текст ответа модели (поле content) */
    private String requestCompletion(String userPrompt) throws IOException, InterruptedException {
        String body = "{"
                + "\"model\":\"" + escape(model) + "\","
                + "\"temperature\":0.9,"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"Ты — генератор вопросов для викторины. "
                + "Отвечай только корректным JSON.\"},"
                + "{\"role\":\"user\",\"content\":\"" + escape(userPrompt) + "\"}"
                + "]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(40))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return extractContent(response.body());
    }

    /** Достаёт choices[0].message.content из ответа в формате OpenAI */
    @SuppressWarnings("unchecked")
    private String extractContent(String responseJson) {
        Object root = Json.parse(responseJson);
        Map<String, Object> map = (Map<String, Object>) root;
        List<Object> choices = (List<Object>) map.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("Ответ API не содержит вариантов (choices)");
        }
        Map<String, Object> first = (Map<String, Object>) choices.get(0);
        Map<String, Object> message = (Map<String, Object>) first.get("message");
        String content = (String) message.get("content");
        // «Рассуждающие» модели иногда оставляют content пустым, а итоговый
        // JSON помещают в поле reasoning — используем его как запасной источник.
        if (content == null || content.isBlank()) {
            Object reasoning = message.get("reasoning");
            if (reasoning instanceof String) {
                content = (String) reasoning;
            }
        }
        return content;
    }

    /** Разбирает JSON-вопрос, который вернул ИИ, в объект Question */
    @SuppressWarnings("unchecked")
    private Question parseQuestion(String content, int level) {
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Модель вернула пустой ответ");
        }
        String json = extractJsonObject(content);
        Map<String, Object> obj = (Map<String, Object>) Json.parse(json);

        String text = (String) obj.get("text");
        List<Object> answersList = (List<Object>) obj.get("answers");
        if (text == null || answersList == null || answersList.size() != 4) {
            throw new IllegalStateException("Некорректная структура вопроса от ИИ");
        }
        String[] answers = new String[4];
        for (int i = 0; i < 4; i++) {
            answers[i] = String.valueOf(answersList.get(i));
        }
        int correct = toAnswerNumber(obj.get("correct"));
        if (correct < 1 || correct > 4) {
            throw new IllegalStateException("Неверный номер правильного ответа: " + correct);
        }
        return new Question(text, answers, String.valueOf(correct), level);
    }

    /**
     * Выделяет JSON-объект из ответа модели. Многие модели добавляют пояснения до или после JSON либо оборачивают его в ```-блок — берём подстроку от
     * первой '{' до последней '}'.
     */
    private String extractJsonObject(String content) {
        String t = stripCodeFences(content);
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("В ответе модели не найден JSON-объект");
        }
        return t.substring(start, end + 1);
    }

    /** Номер верного ответа может прийти числом (3 или 3.0) или строкой ("3", "Вариант 3") */
    private int toAnswerNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            String s = (String) value;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c >= '1' && c <= '4') {
                    return c - '0';
                }
            }
        }
        throw new IllegalStateException("Не удалось определить номер правильного ответа");
    }

    /** Убирает обёртку ```json ... ``` если модель её добавила */
    private String stripCodeFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNewLine = t.indexOf('\n');
            if (firstNewLine >= 0) {
                t = t.substring(firstNewLine + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.trim();
    }

    /** Экранирование строки для вставки в JSON */
    private String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
