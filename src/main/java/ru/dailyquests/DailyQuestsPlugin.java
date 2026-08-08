package ru.dailyquests;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dailyquests.clan.ClanQuestManager;
import ru.dailyquests.clan.FcClansHook;
import ru.dailyquests.command.DailyQuestCommand;
import ru.dailyquests.config.ConfigManager;
import ru.dailyquests.data.DataStorage;
import ru.dailyquests.economy.EconomyManager;
import ru.dailyquests.gui.QuestMenu;
import ru.dailyquests.manager.ProgressListener;
import ru.dailyquests.manager.QuestManager;

public final class DailyQuestsPlugin extends JavaPlugin {

    private static DailyQuestsPlugin instance;

    private ConfigManager configManager;
    private DataStorage dataStorage;
    private EconomyManager economyManager;
    private QuestManager questManager;
    private ClanQuestManager clanQuestManager;
    private QuestMenu questMenu;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        dataStorage = new DataStorage(this);
        economyManager = new EconomyManager(this);
        questManager = new QuestManager(this);
        clanQuestManager = new ClanQuestManager(this);
        questMenu = new QuestMenu(this);

        getServer().getPluginManager().registerEvents(questManager, this);
        getServer().getPluginManager().registerEvents(new ProgressListener(this), this);
        getServer().getPluginManager().registerEvents(questMenu, this);
        if (FcClansHook.isAvailable()) {
            getServer().getPluginManager().registerEvents(clanQuestManager, this);
            getLogger().info("FCClans найден, клановые квесты включены.");
        } else {
            getLogger().info("FCClans не найден, клановые квесты отключены.");
        }

        DailyQuestCommand command = new DailyQuestCommand(this);
        getCommand("dailyquests").setExecutor(command);
        getCommand("dailyquests").setTabCompleter(command);

        getLogger().info("DailyQuests включён. Сброс квестов каждый день в "
                + configManager.getResetTime() + " (" + configManager.getZone().getId() + ").");
    }

    @Override
    public void onDisable() {
        if (questManager != null) {
            questManager.saveAll();
        }
        if (clanQuestManager != null) {
            clanQuestManager.saveAll();
        }
    }

    public static DailyQuestsPlugin getInstance() {
        return instance;
    }

    public static Component text(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy.replace('&', '§'));
    }

    public void msg(Player player, String key, String... replacements) {
        String message = configManager.getMessage(key);
        if (message.isEmpty()) {
            return;
        }
        player.sendMessage(build(message, replacements));
    }

    public Component componentOf(String key, String... replacements) {
        return build(configManager.getMessage(key), replacements);
    }

    public void msgWithMenu(Player player, String key, String... replacements) {
        String message = configManager.getMessage(key);
        if (message.isEmpty()) {
            return;
        }
        Component component = build(message, replacements);
        String openMenu = configManager.getMessage("open-menu");
        if (!openMenu.isEmpty()) {
            component = component.append(Component.text(" "))
                    .append(text(openMenu)
                            .clickEvent(ClickEvent.runCommand("/dailyquests")));
        }
        player.sendMessage(component);
    }

    private Component build(String message, String... replacements) {
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return text(configManager.getPrefix() + message);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public ClanQuestManager getClanQuestManager() {
        return clanQuestManager;
    }

    public QuestMenu getQuestMenu() {
        return questMenu;
    }
}
