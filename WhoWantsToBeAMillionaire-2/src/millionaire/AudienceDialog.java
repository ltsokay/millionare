package millionaire;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Подсказка «Помощь зала»: зрители голосуют, игроку показывается статистика в виде столбчатой диаграммы. Голоса смещены в сторону правильного ответа,
 * причем тем сильнее, чем ниже уровень сложности
 */
public class AudienceDialog extends JDialog {

    private static final Color BG = new Color(7, 11, 52);
    private static final Color BAR = new Color(255, 196, 0);
    private static final String[] LETTERS = {"А", "Б", "В", "Г"};

    public AudienceDialog(Window owner, Question question, boolean[] enabled, int level) {
        super(owner, "Помощь зала", ModalityType.APPLICATION_MODAL);
        int[] votes = computeVotes(question.getRightIndex(), enabled, level);

        JPanel content = new JPanel(new java.awt.BorderLayout(10, 10));
        content.setBackground(BG);
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Результаты голосования зрителей:", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        content.add(title, java.awt.BorderLayout.NORTH);

        content.add(new ChartPanel(votes, enabled), java.awt.BorderLayout.CENTER);

        JButton ok = new JButton("ОК");
        ok.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.setBackground(BG);
        south.add(ok);
        content.add(south, java.awt.BorderLayout.SOUTH);

        setContentPane(content);
        setSize(440, 320);
        setLocationRelativeTo(owner);
    }

    /** Распределяет 100 % голосов между доступными вариантами с уклоном к верному */
    private int[] computeVotes(int correctIndex, boolean[] enabled, int level) {
        Random rnd = new Random();
        double pCorrect = 0.85 - (level - 1) * 0.025;
        if (pCorrect < 0.45) {
            pCorrect = 0.45;
        }
        if (!enabled[correctIndex]) {
            pCorrect = 0; // правильный убран подсказкой 50/50 — не выделяем его
        }

        double[] weight = new double[4];
        double sum = 0;
        for (int i = 0; i < 4; i++) {
            if (!enabled[i]) {
                weight[i] = 0;
                continue;
            }
            if (i == correctIndex && pCorrect > 0) {
                weight[i] = pCorrect + rnd.nextDouble() * 0.1;
            } else {
                weight[i] = rnd.nextDouble() * 0.3 + 0.05;
            }
            sum += weight[i];
        }

        int[] votes = new int[4];
        int assigned = 0;
        int last = -1;
        for (int i = 0; i < 4; i++) {
            if (weight[i] > 0) {
                votes[i] = (int) Math.round(weight[i] / sum * 100);
                assigned += votes[i];
                last = i;
            }
        }
        if (last >= 0) {
            votes[last] += 100 - assigned; // компенсация округления до ровно 100 %
        }
        return votes;
    }

    /** Панель, рисующая столбики голосования */
    private static class ChartPanel extends JPanel {
        private final int[] votes;
        private final boolean[] enabled;

        ChartPanel(int[] votes, boolean[] enabled) {
            this.votes = votes;
            this.enabled = enabled;
            setBackground(BG);
            setPreferredSize(new Dimension(400, 220));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int baseline = h - 30;
            int maxBarHeight = baseline - 20;
            int slot = w / 4;
            int barWidth = slot - 30;

            g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
            for (int i = 0; i < 4; i++) {
                int cx = i * slot + slot / 2;
                int barHeight = (int) (maxBarHeight * (votes[i] / 100.0));
                int x = cx - barWidth / 2;
                int y = baseline - barHeight;

                g2.setColor(enabled[i] ? BAR : new Color(80, 80, 80));
                g2.fillRect(x, y, barWidth, barHeight);

                g2.setColor(Color.WHITE);
                g2.drawString(votes[i] + "%", cx - 14, y - 6);
                g2.drawString(LETTERS[i], cx - 5, baseline + 20);
            }
            g2.setColor(new Color(120, 120, 160));
            g2.drawLine(0, baseline, w, baseline);
        }
    }
}
