package ru.dailyquests.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.dailyquests.DailyQuestsPlugin;
import ru.dailyquests.manager.QuestManager.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DailyQuestCommand implements CommandExecutor, TabCompleter {

    private final DailyQuestsPlugin plugin;

    public DailyQuestCommand(DailyQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только игрокам.");
            return true;
        }
        if (!player.hasPermission("dailyquests.use")) {
            plugin.msg(player, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.getQuestMenu().open(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "time" -> plugin.msg(player, "time-left", "{time}", plugin.getQuestManager().timeRemaining());
            case "leaderboard" -> showLeaderboard(player);
            case "reroll" -> reroll(player, args);
            case "reload" -> {
                if (!player.hasPermission("dailyquests.admin")) {
                    plugin.msg(player, "no-permission");
                    return true;
                }
                plugin.getConfigManager().reload();
                plugin.getQuestManager().onConfigReload();
                plugin.msg(player, "reloaded");
            }
            default -> plugin.msg(player, "usage");
        }
        return true;
    }

    private void showLeaderboard(Player player) {
        List<LeaderboardEntry> leaderboard = plugin.getQuestManager().leaderboard();
        plugin.msg(player, "leaderboard-title");
        if (leaderboard.isEmpty()) {
            plugin.msg(player, "leaderboard-empty");
            return;
        }
        int size = plugin.getConfigManager().getLeaderboardSize();
        for (int i = 0; i < Math.min(size, leaderboard.size()); i++) {
            LeaderboardEntry entry = leaderboard.get(i);
            String line = plugin.getConfigManager().getMessage("leaderboard-line")
                    .replace("{place}", String.valueOf(i + 1))
                    .replace("{name}", entry.name())
                    .replace("{completed}", String.valueOf(entry.completed()));
            player.sendMessage(DailyQuestsPlugin.text(line));
        }
    }

    private void reroll(Player player, String[] args) {
        if (!player.hasPermission("dailyquests.admin")) {
            plugin.msg(player, "no-permission");
            return;
        }
        if (args.length < 2) {
            plugin.msg(player, "usage");
            return;
        }
        String name = args[1];
        Player online = Bukkit.getPlayer(name);
        UUID id;
        if (online != null) {
            id = online.getUniqueId();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
            if (offline == null) {
                plugin.msg(player, "player-not-found", "{player}", name);
                return;
            }
            id = offline.getUniqueId();
        }
        plugin.getQuestManager().rerollQuests(id);
        plugin.msg(player, "rerolled", "{player}", name);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sub = List.of("time", "leaderboard", "reroll", "reload");
            return sub.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reroll")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
