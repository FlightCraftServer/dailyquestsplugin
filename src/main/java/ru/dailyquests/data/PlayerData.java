package ru.dailyquests.data;

import ru.dailyquests.quest.Quest;
import ru.dailyquests.quest.QuestState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerData {

    private final UUID playerId;
    private String date;
    private List<Quest> quests;
    private String name = "";
    private int completed;

    public PlayerData(UUID playerId, String date, List<Quest> quests) {
        this.playerId = playerId;
        this.date = date;
        this.quests = quests == null ? new ArrayList<>() : quests;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<Quest> getQuests() {
        return quests;
    }

    public void setQuests(List<Quest> quests) {
        this.quests = quests;
    }

    public boolean hasActiveQuest() {
        return quests.stream().anyMatch(q -> q.getState() == QuestState.ACTIVE);
    }

    public int activeCount() {
        return (int) quests.stream().filter(q -> q.getState() == QuestState.ACTIVE).count();
    }
}
