package millionaire;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Доступ к базе данных SQLite
 */
public class Database implements AutoCloseable {

    private static final String DB_FILE = "WhoWantsToBeAMillionaire.db";
    private static final String QUESTIONS_FILE = "Вопросы.txt";

    private final Connection connection;

    public Database() throws SQLException {
        try {
            //Явная регистрация драйвера — для совместимости со старыми версиями sqlite-jdbc
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Не найден драйвер SQLite (sqlite-jdbc.jar). "
                    + "Подключите библиотеку к проекту.", ex);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
        createTables();
        seedQuestionsIfEmpty();
    }

    private void createTables() throws SQLException {
        String questions =
                "CREATE TABLE IF NOT EXISTS Questions ("
                + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "  Text TEXT NOT NULL,"
                + "  Answer1 TEXT NOT NULL,"
                + "  Answer2 TEXT NOT NULL,"
                + "  Answer3 TEXT NOT NULL,"
                + "  Answer4 TEXT NOT NULL,"
                + "  RightAnswer TEXT NOT NULL,"
                + "  Level INTEGER NOT NULL"
                + ")";

        String records =
                "CREATE TABLE IF NOT EXISTS Records ("
                + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "  PlayerName TEXT NOT NULL,"
                + "  Prize INTEGER NOT NULL,"
                + "  ReachedLevel INTEGER NOT NULL,"
                + "  PlayedAt TEXT NOT NULL"
                + ")";

        try (Statement st = connection.createStatement()) {
            st.execute(questions);
            st.execute(records);
        }
    }

    /** Если таблица вопросов пуста — загрузить вопросы из текстового файла */
    private void seedQuestionsIfEmpty() throws SQLException {
        if (countQuestions() > 0) {
            return;
        }
        Path file = Path.of(QUESTIONS_FILE);
        if (!Files.exists(file)) {
            System.out.println("Файл \"" + QUESTIONS_FILE + "\" не найден — "
                    + "таблица вопросов осталась пустой.");
            return;
        }

        String insert = "INSERT INTO Questions "
                + "(Text, Answer1, Answer2, Answer3, Answer4, RightAnswer, Level) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        int imported = 0;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             PreparedStatement ps = connection.prepareStatement(insert)) {

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] s = line.split("\t");
                if (s.length < 7) {
                    continue; // строка некорректного формата — пропускаем
                }
                ps.setString(1, s[0]);
                ps.setString(2, s[1]);
                ps.setString(3, s[2]);
                ps.setString(4, s[3]);
                ps.setString(5, s[4]);
                ps.setString(6, s[5].trim());
                ps.setInt(7, Integer.parseInt(s[6].trim()));
                ps.addBatch();
                imported++;
            }
            ps.executeBatch();
            connection.commit();
            System.out.println("Импортировано вопросов в БД: " + imported);
        } catch (IOException | NumberFormatException ex) {
            connection.rollback();
            throw new SQLException("Ошибка импорта вопросов: " + ex.getMessage(), ex);
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    public int countQuestions() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Questions")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Загрузить все вопросы из базы */
    public List<Question> loadAllQuestions() throws SQLException {
        List<Question> list = new ArrayList<>();
        String query = "SELECT Text, Answer1, Answer2, Answer3, Answer4, RightAnswer, Level "
                + "FROM Questions";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                String[] answers = {
                    rs.getString("Answer1"),
                    rs.getString("Answer2"),
                    rs.getString("Answer3"),
                    rs.getString("Answer4")
                };
                list.add(new Question(
                        rs.getString("Text"),
                        answers,
                        rs.getString("RightAnswer"),
                        rs.getInt("Level")));
            }
        }
        return list;
    }

    /** Сохранить результат игры в таблицу рекордов */
    public void saveRecord(String playerName, long prize, int reachedLevel) throws SQLException {
        String insert = "INSERT INTO Records (PlayerName, Prize, ReachedLevel, PlayedAt) "
                + "VALUES (?, ?, ?, ?)";
        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            ps.setString(1, playerName);
            ps.setLong(2, prize);
            ps.setInt(3, reachedLevel);
            ps.setString(4, now);
            ps.executeUpdate();
        }
    }

    /** TOP-10 игроков по сумме выигрыша */
    public List<ScoreRecord> getTopRecords(int limit) throws SQLException {
        List<ScoreRecord> list = new ArrayList<>();
        String query = "SELECT PlayerName, Prize, ReachedLevel, PlayedAt "
                + "FROM Records ORDER BY Prize DESC, id ASC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ScoreRecord(
                            rs.getString("PlayerName"),
                            rs.getLong("Prize"),
                            rs.getInt("ReachedLevel"),
                            rs.getString("PlayedAt")));
                }
            }
        }
        return list;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ex) {
            System.out.println("Ошибка при закрытии БД: " + ex.getMessage());
        }
    }
}
