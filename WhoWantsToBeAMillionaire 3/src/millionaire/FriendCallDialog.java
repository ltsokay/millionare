package millionaire;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.Window;

/**
 * Подсказка «Звонок другу»: в течение 30 секунд один из заранее заявленных друзей советует вариант ответа. Достоверность совета зависит от уровня сложности вопроса
 */
public class FriendCallDialog extends JDialog {

    private static final Color BG = new Color(7, 11, 52);
    private static final String[] FRIENDS = {
        "Алексей", "Мария", "Дмитрий", "Ольга", "Сергей"
    };
    private static final String[] LETTERS = {"А", "Б", "В", "Г"};

    private final Timer timer;
    private int secondsLeft = 30;

    public FriendCallDialog(Window owner, Question question, boolean[] enabled, int level) {
        super(owner, "Звонок другу", ModalityType.APPLICATION_MODAL);

        Random rnd = new Random();
        String friend = FRIENDS[rnd.nextInt(FRIENDS.length)];
        int suggestion = chooseSuggestion(question.getRightIndex(), enabled, level, rnd);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBackground(BG);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Вы звоните другу: " + friend, SwingConstants.CENTER);
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 15f));
        content.add(header, BorderLayout.NORTH);

        JLabel advice = new JLabel("Соединение...", SwingConstants.CENTER);
        advice.setForeground(new Color(255, 196, 0));
        advice.setFont(advice.getFont().deriveFont(Font.BOLD, 16f));
        content.add(advice, BorderLayout.CENTER);

        JLabel timerLabel = new JLabel("Осталось: 30 сек.", SwingConstants.CENTER);
        timerLabel.setForeground(Color.WHITE);

        JButton close = new JButton("Завершить разговор");
        close.addActionListener(e -> dispose());
        close.setEnabled(false);

        JPanel south = new JPanel(new BorderLayout(5, 5));
        south.setBackground(BG);
        south.add(timerLabel, BorderLayout.NORTH);
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(BG);
        btnPanel.add(close);
        south.add(btnPanel, BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(420, 230);
        setLocationRelativeTo(owner);

        //Через 2 секунды друг "отвечает", затем идёт обратный отсчет
        String adviceText = "«Я почти уверен, что это вариант "
                + LETTERS[suggestion] + " — " + question.getAnswer(suggestion) + "»";

        timer = new Timer(1000, e -> {
            secondsLeft--;
            if (secondsLeft == 28) {
                advice.setText("<html><div style='text-align:center;width:340px'>"
                        + adviceText + "</div></html>");
                close.setEnabled(true);
            }
            timerLabel.setText("Осталось: " + Math.max(secondsLeft, 0) + " сек.");
            if (secondsLeft <= 0) {
                ((Timer) e.getSource()).stop();
                dispose();
            }
        });
        timer.start();
    }

    @Override
    public void dispose() {
        if (timer != null) {
            timer.stop();
        }
        super.dispose();
    }

    /** Друг чаще прав на лёгких вопросах и реже — на сложных */
    private int chooseSuggestion(int correctIndex, boolean[] enabled, int level, Random rnd) {
        double pCorrect = 0.9 - (level - 1) * 0.03;
        if (pCorrect < 0.4) {
            pCorrect = 0.4;
        }
        if (enabled[correctIndex] && rnd.nextDouble() < pCorrect) {
            return correctIndex;
        }
        //иначе случайный доступный вариант
        int idx;
        do {
            idx = rnd.nextInt(4);
        } while (!enabled[idx]);
        return idx;
    }
}
