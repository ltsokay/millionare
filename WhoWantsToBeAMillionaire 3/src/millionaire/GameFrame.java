package millionaire;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * Главное окно игры «Кто хочет стать миллионером?»
 */
public class GameFrame extends JFrame {

    //цвета оформления
    private static final Color BG = new Color(7, 11, 52);
    private static final Color PANEL = new Color(10, 16, 70);
    private static final Color ANSWER = new Color(14, 26, 95);
    private static final Color CORRECT = new Color(0, 150, 30);
    private static final Color WRONG = new Color(170, 0, 0);
    private static final Color GOLD = new Color(255, 196, 0);
    private static final String[] LETTERS = {"А", "Б", "В", "Г"};

    //данные и сервисы
    private Database db;
    private final AIQuestionGenerator ai = new AIQuestionGenerator();
    private List<Question> questions = new ArrayList<>();
    private final Random rnd = new Random();

    //состояние игры
    private int level = 0;                 //номер текущего вопроса (1..15)
    private Question currentQuestion;
    private String playerName = "Игрок";
    private int safeLevel = 5;             //уровень несгораемой суммы
    private long safeAmount = Prizes.prizeForLevel(5);
    private boolean gameOver = false;

    //подсказки
    private int hintsUsed = 0;             //не более 4 из 5
    private boolean used5050, usedAudience, usedFriend, usedSecondChance, usedSwap;
    private boolean secondChanceActive;    //«право на ошибку» активно для текущего вопроса
    private boolean firstWrongTaken;       //первый неверный ответ при активном «праве на ошибку»

    //элементы интерфейса
    private final JButton[] answerButtons = new JButton[4];
    private final JLabel lblQuestionText = new JLabel("", SwingConstants.CENTER);
    private final JLabel lblInfo = new JLabel("", SwingConstants.CENTER);
    private final JList<String> lstLevel = new JList<>();
    private final JLabel lblLogo = new JLabel("", SwingConstants.CENTER);

    private JButton btn5050, btnAudience, btnFriend, btnSecondChance, btnSwap, btnTakeMoney;

    public GameFrame() {
        setTitle("Кто хочет стать миллионером?");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setMinimumSize(new Dimension(860, 640));
        setLocationRelativeTo(null);

        buildMenu();
        buildUI();

        //Подключение к БД
        try {
            db = new Database();
            questions = db.loadAllQuestions();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось подключиться к базе данных:\n" + ex.getMessage()
                    + "\n\nПроверьте, что библиотека sqlite-jdbc.jar подключена к проекту,"
                    + "\nа файл \"Вопросы.txt\" находится рядом с приложением.",
                    "Ошибка базы данных", JOptionPane.ERROR_MESSAGE);
        }

        if (ai.isEnabled()) {
            System.out.println("Режим генерации вопросов через ИИ включён.");
        }
    }

    //Построение интерфейса

    private void buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("Игра");

        JMenuItem newGame = new JMenuItem("Новая игра");
        newGame.addActionListener(e -> startGame());

        JMenuItem records = new JMenuItem("Таблица рекордов");
        records.addActionListener(e -> showRecords());

        JMenuItem exit = new JMenuItem("Выход");
        exit.addActionListener(e -> System.exit(0));

        menu.add(newGame);
        menu.add(records);
        menu.addSeparator();
        menu.add(exit);
        bar.add(menu);
        setJMenuBar(bar);
    }

    private void buildUI() {
        getContentPane().setBackground(BG);
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

        //Верхняя информационная строка
        lblInfo.setForeground(Color.WHITE);
        lblInfo.setFont(lblInfo.getFont().deriveFont(Font.BOLD, 14f));
        lblInfo.setBorder(new EmptyBorder(4, 4, 8, 4));
        add(lblInfo, BorderLayout.NORTH);

        //Левая панель — подсказки и «Забрать деньги»
        add(buildHintsPanel(), BorderLayout.WEST);

        //Правая панель — призовая лестница
        add(buildLadderPanel(), BorderLayout.EAST);

        //Центр — логотип и текст вопроса
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(BG);
        loadLogo();
        center.add(lblLogo, BorderLayout.CENTER);

        lblQuestionText.setForeground(Color.WHITE);
        lblQuestionText.setFont(lblQuestionText.getFont().deriveFont(Font.BOLD, 18f));
        lblQuestionText.setBorder(new EmptyBorder(10, 20, 10, 20));
        lblQuestionText.setPreferredSize(new Dimension(100, 90));
        center.add(lblQuestionText, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        //Низ — кнопки ответов 2×2
        add(buildAnswersPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildHintsPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 6, 10, 16));

        btn5050 = makeHintButton("50 : 50");
        btn5050.addActionListener(e -> useFiftyFifty());

        btnAudience = makeHintButton("Помощь зала");
        btnAudience.addActionListener(e -> useAudience());

        btnFriend = makeHintButton("Звонок другу");
        btnFriend.addActionListener(e -> useFriendCall());

        btnSecondChance = makeHintButton("Право на ошибку");
        btnSecondChance.addActionListener(e -> useSecondChance());

        btnSwap = makeHintButton("Замена вопроса");
        btnSwap.addActionListener(e -> useSwap());

        btnTakeMoney = makeHintButton("Забрать деньги");
        btnTakeMoney.setBackground(new Color(120, 30, 30));
        btnTakeMoney.addActionListener(e -> takeMoney());

        panel.add(btn5050);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnAudience);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnFriend);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnSecondChance);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnSwap);
        panel.add(Box.createVerticalStrut(30));
        panel.add(btnTakeMoney);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JButton makeHintButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(20, 40, 120));
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setMaximumSize(new Dimension(160, 40));
        b.setPreferredSize(new Dimension(160, 40));
        b.setAlignmentX(CENTER_ALIGNMENT);
        return b;
    }

    private JPanel buildLadderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(10, 16, 10, 6));

        DefaultListModel<String> model = new DefaultListModel<>();
        //Сверху самая большая сумма (уровень 15), снизу — наименьшая (уровень 1)
        for (int lvl = Prizes.LEVELS; lvl >= 1; lvl--) {
            model.addElement(lvl + ".  " + Prizes.format(Prizes.prizeForLevel(lvl)));
        }
        lstLevel.setModel(model);
        lstLevel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstLevel.setBackground(PANEL);
        lstLevel.setForeground(Color.WHITE);
        lstLevel.setSelectionBackground(GOLD);
        lstLevel.setSelectionForeground(Color.BLACK);
        lstLevel.setFont(lstLevel.getFont().deriveFont(Font.BOLD, 14f));
        lstLevel.setEnabled(false);
        lstLevel.setFixedCellHeight(26);
        //Список отключён (выбор делает программа), но отключённый JList рисует текст серым. Свой рендерер всегда показывает контрастные цвета.
        lstLevel.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setOpaque(true);
                if (index == lstLevel.getSelectedIndex()) {
                    label.setBackground(GOLD);
                    label.setForeground(Color.BLACK);
                } else {
                    label.setBackground(PANEL);
                    label.setForeground(Color.WHITE);
                }
                label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
                return label;
            }
        });

        JScrollPane sp = new JScrollPane(lstLevel);
        sp.setPreferredSize(new Dimension(190, 100));
        sp.setBorder(BorderFactory.createLineBorder(new Color(60, 80, 160)));
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAnswersPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 12, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(10, 4, 4, 4));

        for (int i = 0; i < 4; i++) {
            JButton b = new JButton();
            b.setActionCommand(String.valueOf(i + 1)); // ActionCommand «1».."4"
            b.setBackground(ANSWER);
            b.setForeground(Color.WHITE);
            b.setOpaque(true);
            b.setFocusPainted(false);
            b.setFont(b.getFont().deriveFont(Font.BOLD, 15f));
            b.setPreferredSize(new Dimension(100, 55));
            b.addActionListener(this::onAnswer);
            answerButtons[i] = b;
            panel.add(b);
        }
        return panel;
    }

    private void loadLogo() {
        try {
            File f = new File("picture.jpg");
            if (f.exists()) {
                Image img = ImageIO.read(f);
                Image scaled = img.getScaledInstance(420, 260, Image.SCALE_SMOOTH);
                lblLogo.setIcon(new ImageIcon(scaled));
            } else {
                lblLogo.setText("Кто хочет стать миллионером?");
                lblLogo.setForeground(GOLD);
                lblLogo.setFont(lblLogo.getFont().deriveFont(Font.BOLD, 26f));
            }
        } catch (Exception ex) {
            lblLogo.setText("Кто хочет стать миллионером?");
            lblLogo.setForeground(GOLD);
            lblLogo.setFont(lblLogo.getFont().deriveFont(Font.BOLD, 26f));
        }
    }

    //Жизненный цикл игры

    /** Запуск новой игры: запрос имени и несгораемой суммы, сброс состояния */
    public void startGame() {
        if (questions.isEmpty() && !ai.isEnabled()) {
            JOptionPane.showMessageDialog(this,
                    "Нет загруженных вопросов. Проверьте базу данных и файл \"Вопросы.txt\".",
                    "Игра невозможна", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!askStartParameters()) {
            return; //пользователь отменил
        }

        level = 0;
        gameOver = false;
        hintsUsed = 0;
        used5050 = usedAudience = usedFriend = usedSecondChance = usedSwap = false;
        refreshHintButtons();
        nextStep();
    }

    /** Диалог ввода имени игрока и выбора несгораемой суммы */
    private boolean askStartParameters() {
        JTextField nameField = new JTextField(playerName, 16);

        String[] options = new String[Prizes.LEVELS];
        for (int lvl = 1; lvl <= Prizes.LEVELS; lvl++) {
            options[lvl - 1] = lvl + " — " + Prizes.format(Prizes.prizeForLevel(lvl));
        }
        JComboBox<String> safeBox = new JComboBox<>(options);
        safeBox.setSelectedIndex(safeLevel - 1);

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Имя игрока:"));
        panel.add(nameField);
        panel.add(new JLabel("Несгораемая сумма (уровень):"));
        panel.add(safeBox);

        int result = JOptionPane.showConfirmDialog(this, panel, "Новая игра",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return false;
        }
        String name = nameField.getText().trim();
        playerName = name.isEmpty() ? "Игрок" : name;
        safeLevel = safeBox.getSelectedIndex() + 1;
        safeAmount = Prizes.prizeForLevel(safeLevel);
        return true;
    }

    /** Переход к следующему вопросу */
    private void nextStep() {
        level++;
        if (level > Prizes.LEVELS) {
            win();
            return;
        }
        secondChanceActive = false;
        firstWrongTaken = false;
        loadQuestion(level);
    }

    /**
     * Загружает вопрос заданного уровня (сначала пробует ИИ, затем БД) в фоновом потоке, чтобы не блокировать интерфейс
     */
    private void loadQuestion(int targetLevel) {
        setControlsEnabled(false);
        lblQuestionText.setText("Загрузка вопроса...");
        lblInfo.setText("Игрок: " + playerName
                + "   |   Несгораемая сумма: " + Prizes.format(safeAmount));

        SwingWorker<Question, Void> worker = new SwingWorker<>() {
            @Override
            protected Question doInBackground() {
                return obtainQuestion(targetLevel);
            }

            @Override
            protected void done() {
                Question q;
                try {
                    q = get();
                } catch (Exception ex) {
                    q = null;
                }
                if (q == null) {
                    JOptionPane.showMessageDialog(GameFrame.this,
                            "Не удалось получить вопрос уровня " + targetLevel + ".",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    setControlsEnabled(true);
                    return;
                }
                currentQuestion = q;
                beginQuestion();
            }
        };
        worker.execute();
    }

    /** Получить вопрос: приоритет — ИИ, запасной вариант — база данных */
    private Question obtainQuestion(int targetLevel) {
        if (ai.isEnabled()) {
            Question generated = ai.generate(targetLevel);
            if (generated != null) {
                return generated;
            }
        }
        return getQuestionFromDb(targetLevel);
    }

    /** Случайный вопрос нужного уровня из загруженного списка */
    private Question getQuestionFromDb(int targetLevel) {
        List<Question> list = questions.stream()
                .filter(q -> q.getLevel() == targetLevel)
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            return null;
        }
        return list.get(rnd.nextInt(list.size()));
    }

    /** Отобразить загруженный вопрос и подготовить кнопки */
    private void beginQuestion() {
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setEnabled(true);
            answerButtons[i].setBackground(ANSWER);
        }
        showQuestion(currentQuestion);
        lstLevel.setSelectedIndex(Prizes.LEVELS - level);
        lstLevel.ensureIndexIsVisible(Prizes.LEVELS - level);
        setControlsEnabled(true);
        refreshHintButtons();
        updateInfo();
    }

    private void showQuestion(Question q) {
        lblQuestionText.setText("<html><div style='text-align:center'>"
                + escapeHtml(q.getText()) + "</div></html>");
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setText(LETTERS[i] + ": " + q.getAnswer(i));
        }
    }

    private void updateInfo() {
        long current = Prizes.prizeForLevel(level - 1); // за уже отвеченные вопросы
        lblInfo.setText("Игрок: " + playerName
                + "   |   Вопрос " + level + " из " + Prizes.LEVELS
                + " (" + Prizes.format(Prizes.prizeForLevel(level)) + " ₽)"
                + "   |   Несгораемая: " + Prizes.format(safeAmount)
                + "   |   Текущий выигрыш: " + Prizes.format(current));
    }

    //Обработка ответа

    private void onAnswer(ActionEvent evt) {
        if (gameOver || currentQuestion == null) {
            return;
        }
        int chosenIndex = Integer.parseInt(evt.getActionCommand()) - 1;
        boolean isRight = evt.getActionCommand().equals(currentQuestion.getRightAnswer());

        if (isRight) {
            answerButtons[chosenIndex].setBackground(CORRECT);
            setControlsEnabled(false);
            //короткая пауза, чтобы игрок увидел подсветку
            Timer t = new Timer(900, e -> {
                ((Timer) e.getSource()).stop();
                nextStep();
            });
            t.setRepeats(false);
            t.start();
        } else {
            //«Право на ошибку»: первый неверный ответ не завершает игру
            if (secondChanceActive && !firstWrongTaken) {
                firstWrongTaken = true;
                answerButtons[chosenIndex].setBackground(WRONG);
                answerButtons[chosenIndex].setEnabled(false);
                JOptionPane.showMessageDialog(this,
                        "Неверно! Но у вас есть право на ошибку — попробуйте ещё раз.",
                        "Право на ошибку", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            answerButtons[chosenIndex].setBackground(WRONG);
            int rightIndex = currentQuestion.getRightIndex();
            answerButtons[rightIndex].setBackground(CORRECT);
            setControlsEnabled(false);
            lose();
        }
    }

    /** Проигрыш: игрок получает несгораемую сумму, если успел её достичь */
    private void lose() {
        gameOver = true;
        int answered = level - 1;                 // верно отвечено вопросов
        long prize = (answered >= safeLevel) ? safeAmount : 0;
        saveResult(prize, answered);

        JOptionPane.showMessageDialog(this,
                "Неверный ответ!\n\n"
                + "Правильный ответ: " + LETTERS[currentQuestion.getRightIndex()]
                + " — " + currentQuestion.getAnswer(currentQuestion.getRightIndex()) + "\n"
                + "Вы пройдёте с выигрышем: " + Prizes.format(prize) + " ₽",
                "Игра окончена", JOptionPane.INFORMATION_MESSAGE);
        offerNewGame();
    }

    /** Победа: пройдены все 15 вопросов */
    private void win() {
        gameOver = true;
        long prize = Prizes.prizeForLevel(Prizes.LEVELS);
        setControlsEnabled(false);
        saveResult(prize, Prizes.LEVELS);
        JOptionPane.showMessageDialog(this,
                "Поздравляем, " + playerName + "!\n"
                + "Вы ответили на все 15 вопросов и выиграли "
                + Prizes.format(prize) + " ₽!",
                "Победа!", JOptionPane.INFORMATION_MESSAGE);
        offerNewGame();
    }

    /** «Забрать деньги»: игрок забирает сумму за последний отвеченный вопрос */
    private void takeMoney() {
        if (gameOver || currentQuestion == null) {
            return;
        }
        long prize = Prizes.prizeForLevel(level - 1);
        int answer = JOptionPane.showConfirmDialog(this,
                "Забрать выигрыш " + Prizes.format(prize) + " ₽ и завершить игру?",
                "Забрать деньги", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        gameOver = true;
        setControlsEnabled(false);
        saveResult(prize, level - 1);
        JOptionPane.showMessageDialog(this,
                playerName + ", вы забрали " + Prizes.format(prize) + " ₽. Поздравляем!",
                "Игра завершена", JOptionPane.INFORMATION_MESSAGE);
        offerNewGame();
    }

    private void saveResult(long prize, int answeredQuestions) {
        if (db != null) {
            try {
                db.saveRecord(playerName, prize, answeredQuestions);
            } catch (SQLException ex) {
                System.out.println("Не удалось сохранить рекорд: " + ex.getMessage());
            }
        }
    }

    private void offerNewGame() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Сыграть ещё раз?", "Новая игра", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            startGame();
        }
    }

    //Подсказки

    /** Текущее состояние «включённости» кнопок ответов (для подсказок) */
    private boolean[] enabledFlags() {
        boolean[] flags = new boolean[4];
        for (int i = 0; i < 4; i++) {
            flags[i] = answerButtons[i].isEnabled();
        }
        return flags;
    }

    private void useFiftyFifty() {
        if (!canUseHint() || currentQuestion == null) {
            return;
        }
        int removed = 0;
        int attempts = 0;
        while (removed < 2 && attempts < 100) {
            attempts++;
            int n = rnd.nextInt(4);
            if (!answerButtons[n].getActionCommand().equals(currentQuestion.getRightAnswer())
                    && answerButtons[n].isEnabled()) {
                answerButtons[n].setEnabled(false);
                removed++;
            }
        }
        markHintUsed(btn5050);
        used5050 = true;
    }

    private void useAudience() {
        if (!canUseHint() || currentQuestion == null) {
            return;
        }
        new AudienceDialog(this, currentQuestion, enabledFlags(), level).setVisible(true);
        markHintUsed(btnAudience);
        usedAudience = true;
    }

    private void useFriendCall() {
        if (!canUseHint() || currentQuestion == null) {
            return;
        }
        new FriendCallDialog(this, currentQuestion, enabledFlags(), level).setVisible(true);
        markHintUsed(btnFriend);
        usedFriend = true;
    }

    private void useSecondChance() {
        if (!canUseHint() || currentQuestion == null) {
            return;
        }
        secondChanceActive = true;
        firstWrongTaken = false;
        markHintUsed(btnSecondChance);
        usedSecondChance = true;
        JOptionPane.showMessageDialog(this,
                "Подсказка «Право на ошибку» активирована для текущего вопроса.\n"
                + "Вы можете дать два ответа.",
                "Право на ошибку", JOptionPane.INFORMATION_MESSAGE);
    }

    private void useSwap() {
        if (!canUseHint() || currentQuestion == null) {
            return;
        }
        markHintUsed(btnSwap);
        usedSwap = true;
        //Загрузить другой вопрос того же уровня, сохранив активные флаги «права на ошибку»
        loadQuestion(level);
    }

    /** Можно ли использовать ещё одну подсказку (не больше четырёх из пяти) */
    private boolean canUseHint() {
        if (gameOver) {
            return false;
        }
        if (hintsUsed >= 4) {
            JOptionPane.showMessageDialog(this,
                    "Можно использовать только четыре подсказки из пяти.",
                    "Подсказки исчерпаны", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void markHintUsed(JButton button) {
        button.setEnabled(false);
        hintsUsed++;
        if (hintsUsed >= 4) {
            disableUnusedHints();
        }
    }

    /** После четвёртой подсказки оставшаяся (пятая) блокируется */
    private void disableUnusedHints() {
        btn5050.setEnabled(false);
        btnAudience.setEnabled(false);
        btnFriend.setEnabled(false);
        btnSecondChance.setEnabled(false);
        btnSwap.setEnabled(false);
    }

    /** Привести кнопки подсказок в соответствие с уже использованными */
    private void refreshHintButtons() {
        if (hintsUsed >= 4) {
            disableUnusedHints();
            return;
        }
        btn5050.setEnabled(!used5050);
        btnAudience.setEnabled(!usedAudience);
        btnFriend.setEnabled(!usedFriend);
        btnSecondChance.setEnabled(!usedSecondChance);
        btnSwap.setEnabled(!usedSwap);
    }

    //Прочее

    /** Блокировка/разблокировка кнопок ответов и подсказок на время загрузки */
    private void setControlsEnabled(boolean enabled) {
        for (JButton b : answerButtons) {
            b.setEnabled(enabled);
        }
        if (enabled) {
            refreshHintButtons();
            btnTakeMoney.setEnabled(!gameOver);
        } else {
            btn5050.setEnabled(false);
            btnAudience.setEnabled(false);
            btnFriend.setEnabled(false);
            btnSecondChance.setEnabled(false);
            btnSwap.setEnabled(false);
            btnTakeMoney.setEnabled(false);
        }
    }

    private void showRecords() {
        if (db == null) {
            JOptionPane.showMessageDialog(this, "База данных недоступна.",
                    "Рекорды", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            new RecordsDialog(this, db.getTopRecords(10)).setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось загрузить рекорды: " + ex.getMessage(),
                    "Рекорды", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
