package ru.dailyquests.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.dailyquests.DailyQuestsPlugin;
import ru.dailyquests.quest.Quest;
import ru.dailyquests.quest.QuestDifficulty;
import ru.dailyquests.quest.QuestState;
import ru.dailyquests.quest.QuestType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataStorage {

    private final DailyQuestsPlugin plugin;
    private final File file;

    public DataStorage(DailyQuestsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public Map<UUID, PlayerData> loadAll() {
        Map<UUID, PlayerData> result = new HashMap<>();
        if (!file.exists()) {
            return result;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = cfg.getConfigurationSection("players");
        if (sec == null) {
            return result;
        }
        for (String key : sec.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (Exception e) {
                continue;
            }
            String date = sec.getString(key + ".date", "");
            String name = sec.getString(key + ".name", "");
            int completed = sec.getInt(key + ".completed", 0);
            List<Quest> quests = new ArrayList<>();
            List<?> list = sec.getList(key + ".quests");
            if (list != null) {
                for (Object obj : list) {
                    if (!(obj instanceof Map<?, ?> map)) {
                        continue;
                    }
                    try {
                        QuestType type = QuestType.valueOf(str(map.get("type")));
                        String target = str(map.get("target"));
                        QuestDifficulty difficulty = QuestDifficulty.valueOf(str(map.get("difficulty")));
                        int count = num(map.get("count"));
                        int reward = num(map.get("reward"));
                        String display = str(map.get("display"));
                        Quest q = new Quest(type, target, difficulty, count, reward, display);
                        q.setProgress(num(map.get("progress")));
                        q.setState(QuestState.valueOf(str(map.get("state"))));
                        quests.add(q);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Не удалось загрузить квест игрока " + key + ": " + ex.getMessage());
                    }
                }
            }
            PlayerData data = new PlayerData(id, date, quests);
            data.setName(name);
            data.setCompleted(completed);
            result.put(id, data);
        }
        return result;
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int num(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    public void saveAll(Map<UUID, PlayerData> players) {        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            String path = "players." + entry.getKey() + ".";
            cfg.set(path + "date", entry.getValue().getDate());
            cfg.set(path + "name", entry.getValue().getName());
            cfg.set(path + "completed", entry.getValue().getCompleted());
            List<Map<String, Object>> list = new ArrayList<>();
            for (Quest q : entry.getValue().getQuests()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("type", q.getType().name());
                m.put("target", q.getTarget());
                m.put("difficulty", q.getDifficulty().name());
                m.put("count", q.getCount());
                m.put("reward", q.getReward());
                m.put("progress", q.getProgress());
                m.put("state", q.getState().name());
                m.put("display", q.getDisplay());
                list.add(m);
            }
            cfg.set(path + "quests", list);
        }
        try {
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Не удалось сохранить data.yml: " + ex.getMessage());
        }
    }
}
