package ru.dailyquests.clan;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.dailyquests.DailyQuestsPlugin;
import ru.fcclans.FCClans;
import ru.fcclans.api.FCClansAPI;
import ru.fcclans.models.Clan;
import ru.fcclans.models.ClanMember;

import java.util.List;

public final class FcClansHook {

    private FcClansHook() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("FCClans") != null;
    }

    public static String getClanName(Player player) {
        if (!isAvailable()) {
            return null;
        }
        return FCClansAPI.getClanName(player);
    }

    public static boolean clanExists(String clanName) {
        if (!isAvailable() || clanName == null) {
            return false;
        }
        FCClans fcClans = FCClans.getInstance();
        return fcClans != null && fcClans.getClanManager() != null
                && fcClans.getClanManager().getClanByName(clanName) != null;
    }

    public static boolean addClanMoney(String clanName, double amount) {
        if (!isAvailable()) {
            return false;
        }
        return FCClansAPI.addClanMoney(clanName, amount);
    }

    public static void broadcastToClan(String clanName, Component message, DailyQuestsPlugin plugin) {
        if (!isAvailable() || message == null) {
            return;
        }
        FCClans fcClans = FCClans.getInstance();
        if (fcClans == null || fcClans.getClanManager() == null) {
            return;
        }
        Clan clan = fcClans.getClanManager().getClanByName(clanName);
        if (clan == null) {
            return;
        }
        List<ClanMember> members = fcClans.getClanManager().getMembers(clan.getId());
        for (ClanMember member : members) {
            Player online = Bukkit.getPlayer(member.getUuid());
            if (online != null && online.isOnline()) {
                online.sendMessage(message);
            }
        }
    }
}
