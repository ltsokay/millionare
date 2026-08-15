package millionaire;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Точка входа приложения «Кто хочет стать миллионером?»
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                //если тема недоступна — используется стандартная
            }
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
            frame.startGame();
        });
    }
}
