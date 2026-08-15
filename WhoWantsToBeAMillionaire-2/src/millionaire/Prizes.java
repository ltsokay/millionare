package millionaire;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Призовая лестница из 15 уровней
 * Индекс 0 соответствует уровню 1 (500), индекс 14 — уровню 15 (3 000 000)
 */
public final class Prizes {

    /** Суммы по уровням 1..15 */
    public static final long[] LADDER = {
        500, 1_000, 2_000, 3_000, 5_000,
        10_000, 15_000, 25_000, 50_000, 100_000,
        200_000, 400_000, 800_000, 1_500_000, 3_000_000
    };

    public static final int LEVELS = LADDER.length;

    private static final DecimalFormat MONEY;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        MONEY = new DecimalFormat("#,###", symbols);
    }

    private Prizes() {
    }

    /** Сумма за верный ответ на вопрос уровня level (1..15) */
    public static long prizeForLevel(int level) {
        if (level < 1) {
            return 0;
        }
        if (level > LEVELS) {
            return LADDER[LEVELS - 1];
        }
        return LADDER[level - 1];
    }

    /** Форматирование суммы с разделением разрядов пробелом: 1 500 000 */
    public static String format(long amount) {
        return MONEY.format(amount);
    }
}
