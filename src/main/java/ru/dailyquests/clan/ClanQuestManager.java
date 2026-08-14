package ru.dailyquests.clan;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import ru.dailyquests.DailyQuestsPlugin;
import ru.dailyquests.data.DataStorage;
import ru.dailyquests.quest.Quest;
import ru.dailyquests.quest.QuestGenerator;
import ru.dailyquests.quest.QuestState;
import ru.dailyquests.quest.QuestType;
import ru.fcclans.api.events.ClanJoinEvent;
import ru.fcclans.api.events.ClanLeaveEvent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClanQuestManager implements Listener {

    private final DailyQuestsPlugin plugin;
    private final Map<String, ClanQuestData> clans = new HashMap<>();
    private final Map<String, Set<UUID>> onlineToday = new HashMap<>();
    private final ZoneId zone;
    private String currentDay;

    public ClanQuestManager(DailyQuestsPlugin plugin) {
        this.plugin = plugin;
        this.zone = plugin.getConfigManager().getZone();
        this.currentDay = today();
        DataStorage storage = plugin.getDataStorage();
        for (ClanQuestData data : storage.loadClans().values()) {
            if (!FcClansHook.clanExists(data.getClanName())) {
                continue;
            }
            normalizeRewards(data);
            if (needsRegenerate(data)) {
                generateQuests(data);
            }
            clans.put(data.getClanName(), data);
        }
        refreshOnlineToday();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1200L, 1200L);
    }

    private void tick() {
        String today = today();
        if (!today.equals(currentDay)) {
            currentDay = today;
            for (ClanQuestData data : clans.values()) {
                generateQuests(data);
                FcClansHook.broadcastToClan(data.getClanName(),
                        plugin.componentOf("clan-new-quests"), plugin);
            }
            refreshOnlineToday();
            saveAll();
            plugin.getLogger().info("Клановые квесты сброшены. Выданы новые квесты.");
        }
        boolean changed = clans.entrySet().removeIf(e -> !FcClansHook.clanExists(e.getKey()));
        if (changed) {
            onlineToday.keySet().removeIf(clan -> !clans.containsKey(clan));
            saveAll();
        }
    }

    private String today() {
        return LocalDate.now(zone).toString();
    }

    private void generateQuests(ClanQuestData data) {
        data.setDate(currentDay);
        data.setQuests(QuestGenerator.generateClan(plugin));
    }

    private void normalizeRewards(ClanQuestData data) {
        int base = plugin.getConfigManager().getClanReward();
        for (Quest quest : data.getQuests()) {
            ru.dailyquests.config.QuestTypeData typeData =
                    plugin.getConfigManager().getQuestTypeData(quest.getType());
            double mult = typeData == null ? 1.0 : typeData.getRewardMultiplier();
            quest.setReward(Math.max(1, (int) Math.round(base * mult)));
        }
    }

    private void refreshOnlineToday() {
        onlineToday.clear();
        for (String clanName : clans.keySet()) {
            onlineToday.put(clanName, FcClansHook.getOnlineMemberUuids(clanName));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String clanName = FcClansHook.getClanName(player);
        if (clanName == null) {
            return;
        }
        onlineToday.computeIfAbsent(clanName, k -> new HashSet<>()).add(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanJoin(ClanJoinEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        String clanName = event.getClan() != null ? event.getClan().getName() : FcClansHook.getClanName(event.getPlayer());
        if (clanName == null) {
            return;
        }
        onlineToday.computeIfAbsent(clanName, k -> new HashSet<>()).add(event.getPlayer().getUniqueId());
        getClanData(clanName);
        saveAll();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanLeave(ClanLeaveEvent event) {
        ClanQuestData data = null;
        String clanName = null;
        if (event.getClan() != null) {
            clanName = event.getClan().getName();
        } else if (event.getPlayer() != null) {
            clanName = FcClansHook.getClanName(event.getPlayer());
        }
        if (clanName != null) {
            data = clans.get(clanName);
            Set<UUID> online = onlineToday.get(clanName);
            if (online != null && event.getPlayer() != null) {
                online.remove(event.getPlayer().getUniqueId());
            }
        }
        if (data != null) {
            String playerName = event.getPlayer() != null ? event.getPlayer().getName() : "?";
            plugin.getLogger().info("Игрок " + playerName
                    + " покинул клан, прогресс клана сохранён: " + data.getClanName());
        }
    }

    public boolean isAvailable() {
        return plugin.getConfigManager().isClanQuestsEnabled() && FcClansHook.isAvailable();
    }

    public ClanQuestData getClanData(String clanName) {
        if (clanName == null) {
            return null;
        }
        ClanQuestData data = clans.get(clanName);
        if (data == null) {
            data = new ClanQuestData(clanName, currentDay, QuestGenerator.generateClan(plugin));
            clans.put(clanName, data);
            saveAll();
        } else if (needsRegenerate(data)) {
            generateQuests(data);
            saveAll();
        }
        return data;
    }

    private boolean needsRegenerate(ClanQuestData data) {
        return !data.getDate().equals(currentDay)
                || data.getQuests().size() != plugin.getConfigManager().getClanQuestsPerDay();
    }

    public ClanQuestData getData(Player player) {
        String clanName = FcClansHook.getClanName(player);
        return clanName == null ? null : getClanData(clanName);
    }

    public boolean hasActiveQuest(Player player, QuestType type, String target) {
        String clanName = FcClansHook.getClanName(player);
        if (clanName == null) {
            return false;
        }
        ClanQuestData data = clans.get(clanName);
        if (data == null) {
            return false;
        }
        for (Quest quest : data.getQuests()) {
            if (quest.getState() != QuestState.ACTIVE || quest.getType() != type) {
                continue;
            }
            if (quest.getTarget().isEmpty() || quest.getTarget().equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    public int getEffectiveReward(String clanName, Quest quest) {
        int online = onlineToday.getOrDefault(clanName, Set.of()).size();
        return quest.getReward() * Math.max(1, online);
    }

    public int getOnlineToday(String clanName) {
        return onlineToday.getOrDefault(clanName, Set.of()).size();
    }

    public boolean takeQuest(Player player, int index) {
        ClanQuestData data = getData(player);
        if (data == null || index < 0 || index >= data.getQuests().size()) {
            return false;
        }
        if (data.hasTakenQuest()) {
            return false;
        }
        Quest quest = data.getQuests().get(index);
        if (quest.getState() != QuestState.AVAILABLE) {
            return false;
        }
        quest.setState(QuestState.ACTIVE);
        quest.setTakenBy(player.getName());
        quest.setTakenAt(LocalTime.now(zone)
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        saveAll();
        return true;
    }

    public int claimQuest(Player player, int index) {
        ClanQuestData data = getData(player);
        if (data == null || index < 0 || index >= data.getQuests().size()) {
            return -1;
        }
        Quest quest = data.getQuests().get(index);
        if (quest.getState() != QuestState.COMPLETED) {
            return -1;
        }
        int reward = getEffectiveReward(data.getClanName(), quest);
        if (!FcClansHook.addClanMoney(data.getClanName(), reward)) {
            plugin.msg(player, "clan-bank-error");
            return -1;
        }
        quest.setState(QuestState.CLAIMED);
        saveAll();
        return reward;
    }

    public void incrementProgress(Player player, QuestType type, String target, int amount) {
        if (!isAvailable() || amount <= 0) {
            return;
        }
        String clanName = FcClansHook.getClanName(player);
        if (clanName == null) {
            return;
        }
        ClanQuestData data = clans.get(clanName);
        if (data == null) {
            return;
        }
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
                plugin.msg(player, "clan-quest-completed", "{display}", quest.getDisplay());
                FcClansHook.broadcastToClan(clanName,
                        plugin.componentOf("clan-quest-completed-broadcast", "{display}", quest.getDisplay()),
                        plugin);
            } else {
                plugin.msg(player, "clan-progress",
                        "{display}", quest.getDisplay(),
                        "{progress}", String.valueOf(quest.getProgress()),
                        "{count}", String.valueOf(quest.getCount()));
            }
            saveAll();
            return;
        }
    }

    public boolean rerollClan(String clanName) {
        if (clanName == null || !FcClansHook.clanExists(clanName)) {
            return false;
        }
        ClanQuestData data = getClanData(clanName);
        generateQuests(data);
        saveAll();
        return true;
    }

    public Map<String, ClanQuestData> getClans() {
        return clans;
    }

    public void saveAll() {
        plugin.getDataStorage().saveAll(plugin.getQuestManager().getPlayers(), clans);
    }
}