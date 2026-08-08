package ru.dailyquests.clan;

import ru.dailyquests.quest.Quest;
import ru.dailyquests.quest.QuestState;

import java.util.ArrayList;
import java.util.List;

public class ClanQuestData {

    private final String clanName;
    private String date;
    private List<Quest> quests;
    private String takenBy = "";

    public ClanQuestData(String clanName, String date, List<Quest> quests) {
        this.clanName = clanName;
        this.date = date;
        this.quests = quests == null ? new ArrayList<>() : quests;
    }

    public String getClanName() {
        return clanName;
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

    public String getTakenBy() {
        return takenBy;
    }

    public void setTakenBy(String takenBy) {
        this.takenBy = takenBy == null ? "" : takenBy;
    }

    public boolean hasActiveQuest() {
        return quests.stream().anyMatch(q -> q.getState() == QuestState.ACTIVE);
    }

    public int activeCount() {
        return (int) quests.stream().filter(q -> q.getState() == QuestState.ACTIVE).count();
    }

    public boolean hasClaimedOrCompleted() {
        return quests.stream().anyMatch(q ->
                q.getState() == QuestState.COMPLETED
                        || q.getState() == QuestState.CLAIMED
                        || q.getState() == QuestState.BLOCKED);
    }

    public String activeTakenBy() {
        for (Quest q : quests) {
            if (q.getState() == QuestState.ACTIVE) {
                return takenBy;
            }
        }
        return "";
    }
}
