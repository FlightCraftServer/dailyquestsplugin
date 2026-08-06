package ru.dailyquests.economy;

import org.bukkit.entity.Player;
import ru.dailyquests.DailyQuestsPlugin;

public class EconomyManager {

    private final DailyQuestsPlugin plugin;

    public EconomyManager(DailyQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean giveReward(Player player, int amount) {
        String command = plugin.getConfigManager().getRewardCommand()
                .replace("%player%", player.getName())
                .replace("%amount%", String.valueOf(amount));
        boolean ok = plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        if (!ok) {
            plugin.getLogger().warning("Не удалось выдать награду " + player.getName()
                    + " (" + amount + " монет). Команда: " + command);
        }
        return ok;
    }
}
