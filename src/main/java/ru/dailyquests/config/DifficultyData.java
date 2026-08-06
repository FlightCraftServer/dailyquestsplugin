package ru.dailyquests.config;

public class DifficultyData {

    private final int weight;
    private final double countMultiplier;
    private final int reward;
    private final String display;

    public DifficultyData(int weight, double countMultiplier, int reward, String display) {
        this.weight = weight;
        this.countMultiplier = countMultiplier;
        this.reward = reward;
        this.display = display;
    }

    public int getWeight() {
        return weight;
    }

    public double getCountMultiplier() {
        return countMultiplier;
    }

    public int getReward() {
        return reward;
    }

    public String getDisplay() {
        return display;
    }
}
