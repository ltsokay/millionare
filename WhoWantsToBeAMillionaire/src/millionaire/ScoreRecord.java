package millionaire;

/**
 * Запись в таблице рекордов
 */
public class ScoreRecord {

    private final String playerName;
    private final long prize;
    private final int reachedLevel;
    private final String date;

    public ScoreRecord(String playerName, long prize, int reachedLevel, String date) {
        this.playerName = playerName;
        this.prize = prize;
        this.reachedLevel = reachedLevel;
        this.date = date;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getPrize() {
        return prize;
    }

    public int getReachedLevel() {
        return reachedLevel;
    }

    public String getDate() {
        return date;
    }
}
