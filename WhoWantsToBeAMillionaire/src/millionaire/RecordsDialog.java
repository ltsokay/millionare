package millionaire;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Окно «Таблица рекордов»: выводит TOP-10 игроков (задание №3)
 */
public class RecordsDialog extends JDialog {

    public RecordsDialog(Window owner, List<ScoreRecord> records) {
        super(owner, "Таблица рекордов — TOP 10", ModalityType.APPLICATION_MODAL);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Лучшие игроки", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        content.add(title, BorderLayout.NORTH);

        String[] columns = {"#", "Игрок", "Выигрыш, ₽", "Вопросов пройдено", "Дата"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int place = 1;
        for (ScoreRecord r : records) {
            model.addRow(new Object[]{
                place++,
                r.getPlayerName(),
                Prizes.format(r.getPrize()),
                r.getReachedLevel(),
                r.getDate()
            });
        }
        if (records.isEmpty()) {
            model.addRow(new Object[]{"", "Пока нет результатов", "", "", ""});
        }

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(table.getFont().deriveFont(14f));
        table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD, 14f));
        table.setEnabled(false);
        table.getColumnModel().getColumn(0).setMaxWidth(40);

        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton close = new JButton("Закрыть");
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.add(close);
        content.add(south, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(560, 360);
        setLocationRelativeTo(owner);
    }
}
