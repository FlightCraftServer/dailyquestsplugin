package ru.dailyquests.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuestTypeData {

    private final boolean enabled;
    private final int weight;
    private final String icon;
    private final String description;
    private final int countMin;
    private final int countMax;
    private final double rewardMultiplier;
    private final List<String> targets;

    public QuestTypeData(boolean enabled, int weight, String icon, String description,
                         int countMin, int countMax, double rewardMultiplier, List<String> targets) {
        this.enabled = enabled;
        this.weight = weight;
        this.icon = icon;
        this.description = description;
        this.countMin = countMin;
        this.countMax = countMax;
        this.rewardMultiplier = rewardMultiplier;
        this.targets = targets == null ? new ArrayList<>() : targets;
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

    public int getCountMin() {
        return countMin;
    }

    public int getCountMax() {
        return countMax;
    }

    public double getRewardMultiplier() {
        return rewardMultiplier;
    }

    public List<String> getTargets() {
        return targets;
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
}
