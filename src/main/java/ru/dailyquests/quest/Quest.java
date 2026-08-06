package ru.dailyquests.quest;

public class Quest {

    private final QuestType type;
    private final String target;
    private final QuestDifficulty difficulty;
    private final int count;
    private final int reward;
    private final String display;
    private int progress;
    private QuestState state;

    public Quest(QuestType type, String target, QuestDifficulty difficulty, int count, int reward, String display) {
        this.type = type;
        this.target = target;
        this.difficulty = difficulty;
        this.count = count;
        this.reward = reward;
        this.display = display;
        this.progress = 0;
        this.state = QuestState.AVAILABLE;
    }

    public QuestType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public QuestDifficulty getDifficulty() {
        return difficulty;
    }

    public int getCount() {
        return count;
    }

    public int getReward() {
        return reward;
    }

    public String getDisplay() {
        return display;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.min(progress, count);
    }

    public QuestState getState() {
        return state;
    }

    public void setState(QuestState state) {
        this.state = state;
    }

    public static String formatTarget(String target) {
        if (target == null || target.isEmpty()) {
            return "";
        }
        String[] parts = target.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0)))
              .append(part.substring(1))
              .append(' ');
        }
        return sb.toString().trim();
    }
}
