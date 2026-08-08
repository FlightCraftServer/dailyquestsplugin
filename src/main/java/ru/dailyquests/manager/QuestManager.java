package ru.dailyquests.manager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.dailyquests.DailyQuestsPlugin;
import ru.dailyquests.data.PlayerData;
import ru.dailyquests.quest.Quest;
import ru.dailyquests.quest.QuestGenerator;
import ru.dailyquests.quest.QuestState;
import ru.dailyquests.quest.QuestType;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestManager implements Listener {

    private final DailyQuestsPlugin plugin;
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final ZoneId zone;
    private String currentDay;

    public QuestManager(DailyQuestsPlugin plugin) {
        this.plugin = plugin;
        this.zone = plugin.getConfigManager().getZone();
        this.currentDay = today();
        players.putAll(plugin.getDataStorage().loadAll());
        for (PlayerData data : players.values()) {
            if (!data.getDate().equals(currentDay)) {
                generateQuests(data);
            }
        }
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1200L, 1200L);
    }

    public void onConfigReload() {
        refreshZone();
    }

    private void tick() {
        String today = today();
        if (!today.equals(currentDay)) {
            currentDay = today;
            for (PlayerData data : players.values()) {
                generateQuests(data);
            }
            saveAll();
            plugin.getLogger().info("Ежедневные квесты сброшены. Выданы новые квесты.");
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                plugin.msg(p, "new-quests");
            }
        }
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            incrementProgress(p, QuestType.PLAY_TIME, "", 1);
        }
    }

    private void refreshZone() {
        // подхватываем смену часового пояса после /dailyquests reload
        currentDay = today();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        PlayerData data = players.get(p.getUniqueId());
        boolean newQuests = false;
        if (data == null) {
            data = new PlayerData(p.getUniqueId(), currentDay, QuestGenerator.generate(plugin));
            players.put(p.getUniqueId(), data);
            newQuests = true;
        } else if (!data.getDate().equals(currentDay)) {
            generateQuests(data);
            newQuests = true;
        }
        data.setName(p.getName());
        if (newQuests) {
            saveAll();
            plugin.msg(p, "new-quests");
        } else {
            saveAll();
        }
    }

    private String today() {
        return LocalDate.now(zone).toString();
    }

    private void generateQuests(PlayerData data) {
        data.setDate(currentDay);
        data.setQuests(QuestGenerator.generate(plugin));
    }

    public PlayerData getData(Player player) {
        return getData(player.getUniqueId());
    }

    public PlayerData getData(UUID playerId) {
        PlayerData data = players.get(playerId);
        if (data == null) {
            data = new PlayerData(playerId, currentDay, QuestGenerator.generate(plugin));
            players.put(playerId, data);
            saveAll();
        } else if (!data.getDate().equals(currentDay)) {
            generateQuests(data);
            saveAll();
        }
        return data;
    }

    public void rerollQuests(UUID playerId) {
        PlayerData data = getData(playerId);
        generateQuests(data);
        saveAll();
    }

    public boolean takeQuest(Player player, int index) {
        PlayerData data = getData(player);
        if (index < 0 || index >= data.getQuests().size()) {
            return false;
        }
        if (data.activeCount() >= plugin.getConfigManager().getMaxActive()) {
            return false;
        }
        Quest quest = data.getQuests().get(index);
        if (quest.getState() != QuestState.AVAILABLE) {
            return false;
        }
        quest.setState(QuestState.ACTIVE);
        saveAll();
        return true;
    }

    public int claimQuest(Player player, int index) {
        PlayerData data = getData(player);
        if (index < 0 || index >= data.getQuests().size()) {
            return -1;
        }
        Quest quest = data.getQuests().get(index);
        if (quest.getState() != QuestState.COMPLETED) {
            return -1;
        }
        int reward = quest.getReward();
        int bonus = bonusFor(player.getUniqueId(), reward);
        int total = reward + bonus;
        plugin.getEconomyManager().giveReward(player, total);
        if (bonus > 0) {
            plugin.msg(player, "top-bonus", "{bonus}", String.valueOf(bonus));
        }
        quest.setState(QuestState.CLAIMED);
        data.setName(player.getName());
        data.setCompleted(data.getCompleted() + 1);
        saveAll();
        return total;
    }

    public record LeaderboardEntry(UUID uuid, String name, int completed) {
    }

    public List<LeaderboardEntry> leaderboard() {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (PlayerData data : players.values()) {
            if (data.getCompleted() <= 0) {
                continue;
            }
            String name = data.getName();
            if (name.isEmpty()) {
                name = data.getPlayerId().toString().substring(0, 8);
            }
            entries.add(new LeaderboardEntry(data.getPlayerId(), name, data.getCompleted()));
        }
        entries.sort((a, b) -> Integer.compare(b.completed(), a.completed()));
        return entries;
    }

    private int bonusFor(UUID playerId, int reward) {
        int topPositions = plugin.getConfigManager().getTopPositions();
        int percent = plugin.getConfigManager().getBonusPercent();
        if (topPositions <= 0 || percent <= 0 || reward <= 0) {
            return 0;
        }
        List<LeaderboardEntry> lb = leaderboard();
        for (int i = 0; i < lb.size(); i++) {
            if (lb.get(i).uuid().equals(playerId)) {
                if (i < topPositions) {
                    return Math.max(1, (int) Math.round(reward * percent / 100.0));
                }
                return 0;
            }
        }
        return 0;
    }

    public void incrementProgress(Player player, QuestType type, String target, int amount) {
        if (amount <= 0) {
            return;
        }
        PlayerData data = players.get(player.getUniqueId());
        if (data != null) {
            for (Quest quest : data.getQuests()) {
                if (quest.getState() != QuestState.ACTIVE || quest.getType() != type) {
                    continue;
                }
                if (!quest.getTarget().isEmpty() && !quest.getTarget().equalsIgnoreCase(target)) {
                    continue;
                }
                quest.setProgress(quest.getProgress() + amount);
                if (quest.getProgress() >= quest.getCount()) {
                    quest.setProgress(quest.getCount());
                    quest.setState(QuestState.COMPLETED);
                    plugin.msgWithMenu(player, "quest-completed",
                            "{display}", quest.getDisplay());
                } else {
                    plugin.msgWithMenu(player, "progress",
                            "{display}", quest.getDisplay(),
                            "{progress}", String.valueOf(quest.getProgress()),
                            "{count}", String.valueOf(quest.getCount()));
                }
                saveAll();
                break;
            }
        }
        if (plugin.getClanQuestManager() != null) {
            plugin.getClanQuestManager().incrementProgress(player, type, target, amount);
        }
    }

    public boolean hasActiveQuest(Player player, QuestType type, String target) {
        PlayerData data = players.get(player.getUniqueId());
        if (data == null) {
            return false;
        }
        for (Quest quest : data.getQuests()) {
            if (quest.getState() == QuestState.ACTIVE && quest.getType() == type) {
                if (quest.getTarget().isEmpty() || quest.getTarget().equalsIgnoreCase(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String timeRemaining() {
        ZoneId currentZone = plugin.getConfigManager().getZone();
        ZonedDateTime now = ZonedDateTime.now(currentZone);
        ZonedDateTime next = now.toLocalDate().plusDays(1).atTime(plugin.getConfigManager().getResetTime()).atZone(currentZone);
        long seconds = Math.max(0, Duration.between(now, next).getSeconds());
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dч %02dм %02dс", hours, minutes, secs);
    }

    public void saveAll() {
        plugin.getDataStorage().saveAll(players, plugin.getClanQuestManager() == null
                ? java.util.Map.of() : plugin.getClanQuestManager().getClans());
    }

    public Map<UUID, PlayerData> getPlayers() {
        return players;
    }
}
