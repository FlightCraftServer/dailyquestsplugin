package ru.dailyquests;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .preProcessor(DailyQuestsPlugin::legacyToMiniTags)
            .build();

    public static Component text(String legacy) {
        if (legacy == null) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(legacy);
    }

    private static String legacyToMiniTags(String input) {
        StringBuilder out = new StringBuilder(input.length());
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if ((c == '&' || c == '§') && i + 1 < chars.length) {
                String tag = legacyCodeTag(Character.toLowerCase(chars[i + 1]));
                if (tag != null) {
                    out.append('<').append(tag).append('>');
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String legacyCodeTag(char code) {
        return switch (code) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underline";
            case 'o' -> "italic";
            case 'r' -> "reset";
            default -> null;
        };
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
