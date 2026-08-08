package ru.dailyquests.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ru.dailyquests.DailyQuestsPlugin;
import ru.dailyquests.quest.Quest;
import ru.dailyquests.quest.QuestDifficulty;
import ru.dailyquests.quest.QuestType;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ConfigManager {

    private static final Random RANDOM = new Random();

    private final DailyQuestsPlugin plugin;

    private ZoneId zone = ZoneId.of("Europe/Moscow");
    private LocalTime resetTime = LocalTime.of(0, 0);
    private int questsPerDay = 3;
    private int maxActive = 1;
    private boolean clanQuestsEnabled = true;
    private int clanQuestsPerDay = 3;
    private int clanMaxActive = 1;
    private QuestDifficulty clanDifficulty = QuestDifficulty.MEDIUM;
    private double clanCountMultiplier = 8.0;
    private int clanReward = 500;
    private int leaderboardSize = 10;
    private int topPositions = 3;
    private int bonusPercent = 20;
    private String rewardCommand = "eco give %player% %amount%";
    private String prefix = "";
    private final Map<QuestDifficulty, DifficultyData> difficulties = new EnumMap<>(QuestDifficulty.class);
    private final Map<QuestType, QuestTypeData> questTypes = new EnumMap<>(QuestType.class);
    private final List<CustomQuestData> customQuests = new ArrayList<>();
    private final Map<String, String> targetDisplays = new HashMap<>();
    private final Map<String, String> messages = new HashMap<>();

    public ConfigManager(DailyQuestsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        String tz = c.getString("timezone", "Europe/Moscow");
        try {
            zone = ZoneId.of(tz);
        } catch (Exception e) {
            zone = ZoneId.of("Europe/Moscow");
        }

        String rt = c.getString("reset-time", "00:00");
        String[] parts = rt.split(":");
        try {
            resetTime = LocalTime.of(parseInt(parts[0], 0), parseInt(parts.length > 1 ? parts[1] : "0", 0));
        } catch (Exception e) {
            resetTime = LocalTime.of(0, 0);
        }

        questsPerDay = Math.max(1, c.getInt("quests-per-day", 3));
        maxActive = Math.max(1, c.getInt("max-active", 1));

        ConfigurationSection cl = c.getConfigurationSection("clan-quests");
        if (cl != null) {
            clanQuestsEnabled = cl.getBoolean("enabled", true);
            clanQuestsPerDay = Math.max(1, cl.getInt("quests-per-day", 3));
            clanMaxActive = Math.max(1, cl.getInt("max-active", 1));
            String diffName = cl.getString("difficulty", "MEDIUM");
            try {
                clanDifficulty = QuestDifficulty.valueOf(diffName.toUpperCase());
            } catch (IllegalArgumentException e) {
                clanDifficulty = QuestDifficulty.MEDIUM;
            }
            clanCountMultiplier = Math.max(1.0, cl.getDouble("count-multiplier", 8.0));
            clanReward = Math.max(1, cl.getInt("reward", 500));
        }
        leaderboardSize = Math.max(1, c.getInt("leaderboard.size", 10));
        topPositions = Math.max(0, c.getInt("leaderboard.top-positions", 3));
        bonusPercent = Math.max(0, c.getInt("leaderboard.bonus-percent", 20));
        rewardCommand = c.getString("reward-command", "eco give %player% %amount%");
        prefix = c.getString("messages.prefix", "");

        difficulties.clear();
        ConfigurationSection ds = c.getConfigurationSection("difficulties");
        if (ds != null) {
            for (String key : ds.getKeys(false)) {
                try {
                    QuestDifficulty d = QuestDifficulty.valueOf(key.toUpperCase());
                    int weight = Math.max(0, ds.getInt(key + ".weight", 10));
                    double mult = Math.max(0.1, ds.getDouble(key + ".count-multiplier", 1.0));
                    int reward = Math.max(0, ds.getInt(key + ".reward", 10));
                    String display = ds.getString(key + ".display", key);
                    difficulties.put(d, new DifficultyData(weight, mult, reward, display));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        for (QuestDifficulty d : QuestDifficulty.values()) {
            difficulties.putIfAbsent(d, new DifficultyData(10, 1.0, 10, d.name()));
        }

        questTypes.clear();
        ConfigurationSection qs = c.getConfigurationSection("quests");
        if (qs != null) {
            for (QuestType t : QuestType.values()) {
                String key = t.name();
                if (!qs.contains(key)) {
                    continue;
                }
                boolean enabled = qs.getBoolean(key + ".enabled", true);
                int weight = Math.max(0, qs.getInt(key + ".weight", 100));
                String icon = qs.getString(key + ".icon", "PAPER");
                String description = qs.getString(key + ".description", t.name());
                double mult = Math.max(0, qs.getDouble(key + ".reward-multiplier", 1.0));
                int base = Math.max(1, qs.getInt(key + ".base-count", 5));
                int min = base;
                int max = base;
                List<Integer> range = qs.getIntegerList(key + ".count-range");
                if (range.size() >= 2) {
                    min = Math.max(1, range.get(0));
                    max = Math.max(min, range.get(1));
                }
                List<String> targets = qs.getStringList(key + ".targets");
                questTypes.put(t, new QuestTypeData(enabled, weight, icon, description, min, max, mult, targets));
            }
        }

        customQuests.clear();
        ConfigurationSection cs = c.getConfigurationSection("custom-quests");
        if (cs != null) {
            for (String id : cs.getKeys(false)) {
                String path = id + ".";
                String typeName = cs.getString(path + "type", "");
                QuestType type;
                try {
                    type = QuestType.valueOf(typeName.toUpperCase());
                } catch (Exception e) {
                    plugin.getLogger().warning("Кастомный квест '" + id + "': неизвестный тип '" + typeName + "'");
                    continue;
                }
                boolean enabled = cs.getBoolean(path + "enabled", true);
                int weight = Math.max(0, cs.getInt(path + "weight", 50));
                String icon = cs.getString(path + "icon", "PAPER");
                String description = cs.getString(path + "description", typeName);
                double mult = Math.max(0, cs.getDouble(path + "reward-multiplier", 1.0));
                int count = Math.max(1, cs.getInt(path + "count", 0));
                boolean fixed = cs.contains(path + "count") && count > 0;
                int min = count;
                int max = count;
                if (!fixed) {
                    List<Integer> range = cs.getIntegerList(path + "count-range");
                    if (range.size() >= 2) {
                        min = Math.max(1, range.get(0));
                        max = Math.max(min, range.get(1));
                        fixed = false;
                    } else {
                        min = 1;
                        max = 1;
                    }
                }
                List<String> targets = cs.getStringList(path + "targets");
                Map<QuestDifficulty, Integer> rewards = new EnumMap<>(QuestDifficulty.class);
                ConfigurationSection rs = cs.getConfigurationSection(path + "rewards");
                if (rs != null) {
                    for (String k : rs.getKeys(false)) {
                        try {
                            rewards.put(QuestDifficulty.valueOf(k.toUpperCase()), Math.max(0, rs.getInt(k)));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                customQuests.add(new CustomQuestData(id, type, enabled, weight, icon, description,
                        min, max, fixed, mult, targets, rewards));
            }
        }

        targetDisplays.clear();
        ConfigurationSection td = c.getConfigurationSection("target-displays");
        if (td != null) {
            for (String key : td.getKeys(false)) {
                targetDisplays.put(key.toUpperCase(), td.getString(key, ""));
            }
        }

        messages.clear();
        ConfigurationSection ms = c.getConfigurationSection("messages");
        if (ms != null) {
            for (String key : ms.getKeys(false)) {
                messages.put(key, ms.getString(key, ""));
            }
        }
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public ZoneId getZone() {
        return zone;
    }

    public LocalTime getResetTime() {
        return resetTime;
    }

    public int getQuestsPerDay() {
        return questsPerDay;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public boolean isClanQuestsEnabled() {
        return clanQuestsEnabled;
    }

    public int getClanQuestsPerDay() {
        return clanQuestsPerDay;
    }

    public int getClanMaxActive() {
        return clanMaxActive;
    }

    public QuestDifficulty getClanDifficulty() {
        return clanDifficulty;
    }

    public double getClanCountMultiplier() {
        return clanCountMultiplier;
    }

    public int getClanReward() {
        return clanReward;
    }

    public int getLeaderboardSize() {
        return leaderboardSize;
    }

    public int getTopPositions() {
        return topPositions;
    }

    public int getBonusPercent() {
        return bonusPercent;
    }

    public String getRewardCommand() {
        return rewardCommand;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isQuestTypeEnabled(QuestType type) {
        QuestTypeData data = questTypes.get(type);
        return data != null && data.isEnabled();
    }

    public QuestTypeData getQuestTypeData(QuestType type) {
        return questTypes.get(type);
    }

    public List<CustomQuestData> getCustomQuests() {
        return customQuests;
    }

    public DifficultyData getDifficultyData(QuestDifficulty difficulty) {
        return difficulties.get(difficulty);
    }

    public double getCountMultiplier(QuestDifficulty difficulty) {
        DifficultyData d = difficulties.get(difficulty);
        return d == null ? 1.0 : d.getCountMultiplier();
    }

    public int getReward(QuestDifficulty difficulty) {
        DifficultyData d = difficulties.get(difficulty);
        return d == null ? 10 : d.getReward();
    }

    public String getDifficultyDisplay(QuestDifficulty difficulty) {
        DifficultyData d = difficulties.get(difficulty);
        return d == null ? difficulty.name() : d.getDisplay();
    }

    public String getTargetDisplay(String target) {
        if (target == null || target.isEmpty()) {
            return "";
        }
        String display = targetDisplays.get(target.toUpperCase());
        return display != null ? display : Quest.formatTarget(target);
    }

    public QuestDifficulty pickDifficulty() {
        int total = difficulties.values().stream().mapToInt(DifficultyData::getWeight).sum();
        if (total <= 0) {
            return QuestDifficulty.EASY;
        }
        int r = RANDOM.nextInt(total);
        for (Map.Entry<QuestDifficulty, DifficultyData> e : difficulties.entrySet()) {
            r -= e.getValue().getWeight();
            if (r < 0) {
                return e.getKey();
            }
        }
        return QuestDifficulty.EASY;
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }
}
