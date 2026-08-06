package ru.dailyquests.config;

import ru.dailyquests.quest.QuestDifficulty;
import ru.dailyquests.quest.QuestType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CustomQuestData {

    private final String id;
    private final QuestType type;
    private final boolean enabled;
    private final int weight;
    private final String icon;
    private final String description;
    private final int countMin;
    private final int countMax;
    private final boolean fixedCount;
    private final double rewardMultiplier;
    private final List<String> targets;
    private final Map<QuestDifficulty, Integer> rewards;

    public CustomQuestData(String id, QuestType type, boolean enabled, int weight, String icon, String description,
                           int countMin, int countMax, boolean fixedCount, double rewardMultiplier,
                           List<String> targets, Map<QuestDifficulty, Integer> rewards) {
        this.id = id;
        this.type = type;
        this.enabled = enabled;
        this.weight = weight;
        this.icon = icon;
        this.description = description;
        this.countMin = countMin;
        this.countMax = countMax;
        this.fixedCount = fixedCount;
        this.rewardMultiplier = rewardMultiplier;
        this.targets = targets == null ? new ArrayList<>() : targets;
        this.rewards = rewards == null ? new EnumMap<>(QuestDifficulty.class) : rewards;
    }

    public String getId() {
        return id;
    }

    public QuestType getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getWeight() {
        return weight;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFixedCount() {
        return fixedCount;
    }

    public double getRewardMultiplier() {
        return rewardMultiplier;
    }

    public List<String> getTargets() {
        return targets;
    }

    public Map<QuestDifficulty, Integer> getRewards() {
        return rewards;
    }

    public int randomCount(Random random) {
        if (countMax <= countMin) {
            return countMin;
        }
        return countMin + random.nextInt(countMax - countMin + 1);
    }

    public String pickTarget(Random random) {
        if (targets.isEmpty()) {
            return "";
        }
        return targets.get(random.nextInt(targets.size()));
    }

    public int rewardFor(QuestDifficulty difficulty, ConfigManager config) {
        Integer fixed = rewards.get(difficulty);
        if (fixed != null) {
            return fixed;
        }
        return Math.max(1, (int) Math.round(config.getReward(difficulty) * rewardMultiplier));
    }
}
