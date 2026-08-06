package ru.dailyquests.quest;

import ru.dailyquests.DailyQuestsPlugin;
import ru.dailyquests.config.ConfigManager;
import ru.dailyquests.config.CustomQuestData;
import ru.dailyquests.config.QuestTypeData;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class QuestGenerator {

    private static final Random RANDOM = new Random();
    private static final Set<QuestType> NO_COUNT_SCALE = EnumSet.of(
            QuestType.VISIT_BIOME, QuestType.VISIT_DIMENSION);

    private QuestGenerator() {
    }

    public static List<Quest> generate(DailyQuestsPlugin plugin) {
        ConfigManager config = plugin.getConfigManager();
        List<Source> sources = new ArrayList<>();
        for (QuestType type : QuestType.values()) {
            QuestTypeData data = config.getQuestTypeData(type);
            if (data != null && data.isEnabled() && data.getWeight() > 0) {
                sources.add(new Source(type, data.getWeight(), data, null));
            }
        }
        for (CustomQuestData custom : config.getCustomQuests()) {
            if (custom.isEnabled() && custom.getWeight() > 0) {
                sources.add(new Source(custom.getType(), custom.getWeight(), null, custom));
            }
        }

        List<Quest> quests = new ArrayList<>();
        int perDay = config.getQuestsPerDay();
        while (quests.size() < perDay && !sources.isEmpty()) {
            Source source = pick(sources);
            sources.remove(source);
            quests.add(source.build(config, RANDOM));
        }
        return quests;
    }

    private static Source pick(List<Source> sources) {
        int total = 0;
        for (Source s : sources) {
            total += s.weight;
        }
        int r = RANDOM.nextInt(Math.max(1, total));
        for (Source s : sources) {
            r -= s.weight;
            if (r < 0) {
                return s;
            }
        }
        return sources.get(sources.size() - 1);
    }

    private record Source(QuestType type, int weight, QuestTypeData typeData, CustomQuestData custom) {

        Quest build(ConfigManager config, Random random) {
            QuestDifficulty difficulty = config.pickDifficulty();
            String target;
            int count;
            String description;
            int reward;

            if (custom != null) {
                target = custom.pickTarget(random);
                count = custom.isFixedCount() ? custom.randomCount(random) : scale(custom.randomCount(random), config, difficulty, type);
                reward = custom.rewardFor(difficulty, config);
                description = custom.getDescription();
            } else {
                target = typeData.pickTarget(random);
                count = scale(typeData.randomCount(random), config, difficulty, type);
                reward = Math.max(1, (int) Math.round(config.getReward(difficulty) * typeData.getRewardMultiplier()));
                description = typeData.getDescription();
            }

            String display = description
                    .replace("{count}", String.valueOf(count))
                    .replace("{target}", config.getTargetDisplay(target))
                    .replace("{difficulty}", config.getDifficultyDisplay(difficulty))
                    .replace("{reward}", String.valueOf(reward));
            return new Quest(type, target, difficulty, count, reward, display);
        }

        private static int scale(int base, ConfigManager config, QuestDifficulty difficulty, QuestType type) {
            if (NO_COUNT_SCALE.contains(type)) {
                return Math.max(1, base);
            }
            return Math.max(1, (int) Math.round(base * config.getCountMultiplier(difficulty)));
        }
    }
}
