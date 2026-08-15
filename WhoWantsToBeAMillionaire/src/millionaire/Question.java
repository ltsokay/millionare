package millionaire;

/**
 * Модель вопроса игры
 *
 * Формат строки исходного файла "Вопросы.txt" (разделитель — табуляция):
 *   текст \t ответ1 \t ответ2 \t ответ3 \t ответ4 \t номерПравильного(1-4) \t уровень(1-15)
 */
public class Question {

    private String text;
    private final String[] answers = new String[4];
    /** Номер правильного ответа в виде строки: "1", "2", "3" или "4" */
    private String rightAnswer;
    private int level;

    /** Пустой конструктор — удобен при создании вопроса вручную (например, от ИИ) */
    public Question() {
    }

    /**
     * Конструктор из массива строк (как в исходном задании)
     * s[0] — текст, s[1..4] — варианты, s[5] — номер правильного, s[6] — уровень
     */
    public Question(String[] s) {
        this.text = s[0];
        for (int i = 0; i < 4; i++) {
            this.answers[i] = s[i + 1];
        }
        this.rightAnswer = s[5].trim();
        this.level = Integer.parseInt(s[6].trim());
    }

    public Question(String text, String[] answers, String rightAnswer, int level) {
        this.text = text;
        System.arraycopy(answers, 0, this.answers, 0, 4);
        this.rightAnswer = rightAnswer;
        this.level = level;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String[] getAnswers() {
        return answers;
    }

    public String getAnswer(int index) {
        return answers[index];
    }

    public String getRightAnswer() {
        return rightAnswer;
    }

    public void setRightAnswer(String rightAnswer) {
        this.rightAnswer = rightAnswer;
    }

    /** Индекс правильного ответа в массиве answers (0..3) */
    public int getRightIndex() {
        return Integer.parseInt(rightAnswer) - 1;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
