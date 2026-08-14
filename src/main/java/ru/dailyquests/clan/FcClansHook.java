package ru.dailyquests.clan;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.dailyquests.DailyQuestsPlugin;
import ru.fcclans.FCClans;
import ru.fcclans.api.FCClansAPI;
import ru.fcclans.models.Clan;
import ru.fcclans.models.ClanMember;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class FcClansHook {

    private FcClansHook() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("FCClans") != null;
    }

    public static String getClanName(Player player) {
        if (!isAvailable() || player == null) {
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

    public static int getClanMemberCount(String clanName) {
        if (!isAvailable() || clanName == null) {
            return 0;
        }
        FCClans fcClans = FCClans.getInstance();
        if (fcClans == null || fcClans.getClanManager() == null) {
            return 0;
        }
        Clan clan = fcClans.getClanManager().getClanByName(clanName);
        if (clan == null) {
            return 0;
        }
        return fcClans.getClanManager().getMemberCount(clan.getId());
    }

    public static Set<UUID> getOnlineMemberUuids(String clanName) {
        Set<UUID> result = new HashSet<>();
        if (!isAvailable() || clanName == null) {
            return result;
        }
        FCClans fcClans = FCClans.getInstance();
        if (fcClans == null || fcClans.getClanManager() == null) {
            return result;
        }
        Clan clan = fcClans.getClanManager().getClanByName(clanName);
        if (clan == null) {
            return result;
        }
        for (ClanMember member : fcClans.getClanManager().getMembers(clan.getId())) {
            Player online = Bukkit.getPlayer(member.getUuid());
            if (online != null && online.isOnline()) {
                result.add(member.getUuid());
            }
        }
        return result;
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
