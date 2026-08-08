package ru.dailyquests.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.dailyquests.DailyQuestsPlugin;
import ru.dailyquests.clan.ClanQuestData;
import ru.dailyquests.clan.FcClansHook;
import ru.dailyquests.config.QuestTypeData;
import ru.dailyquests.data.PlayerData;
import ru.dailyquests.quest.Quest;
import ru.dailyquests.quest.QuestState;

import java.util.ArrayList;
import java.util.List;

public class QuestMenu implements Listener {

    private static final Component TITLE =
            DailyQuestsPlugin.text("§6§lЕжедневные квесты");
    private static final int[] QUEST_SLOTS = {11, 13, 15};
    private static final int[] CLAN_SLOTS = {29, 31, 33};
    private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

    private final DailyQuestsPlugin plugin;

    public QuestMenu(DailyQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        PlayerData data = plugin.getQuestManager().getData(player);
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        for (int i = 0; i < 45; i++) {
            if (!isPersonalSlot(i) && !isClanSlot(i) && i != 4 && i != 30) {
                inv.setItem(i, filler());
            }
        }

        inv.setItem(4, textItem(Material.PLAYER_HEAD, "§6Личные квесты"));
        inv.setItem(30, textItem(Material.RED_BANNER, clanHeader(player)));

        List<Quest> quests = data.getQuests();
        for (int i = 0; i < quests.size() && i < QUEST_SLOTS.length; i++) {
            inv.setItem(QUEST_SLOTS[i], buildItem(quests.get(i)));
        }

        buildClanSection(player, inv);
        player.openInventory(inv);
    }

    private String clanHeader(Player player) {
        if (plugin.getClanQuestManager() == null || !plugin.getConfigManager().isClanQuestsEnabled()) {
            return "&cКлановые квесты отключены";
        }
        String clanName = FcClansHook.getClanName(player);
        if (clanName == null) {
            return "&cВы не состоите в клане";
        }
        return "&6Клановые квесты клана &f" + clanName;
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(FILLER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack textItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(DailyQuestsPlugin.text(name));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildItem(Quest quest) {
        ItemStack item = new ItemStack(iconFor(quest));
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.displayName(DailyQuestsPlugin.text(
                plugin.getConfigManager().getDifficultyDisplay(quest.getDifficulty())
                        + " | " + quest.getDisplay()));

        List<Component> lore = new ArrayList<>();
        lore.add(DailyQuestsPlugin.text(
                "§7Сложность: " + plugin.getConfigManager().getDifficultyDisplay(quest.getDifficulty())));
        lore.add(DailyQuestsPlugin.text("§7Награда: §e" + quest.getReward() + " монет"));
        lore.add(Component.text(" "));

        switch (quest.getState()) {
            case AVAILABLE -> lore.add(DailyQuestsPlugin.text(
                    "§7Нажмите ЛКМ, чтобы взять квест"));
            case ACTIVE -> lore.add(DailyQuestsPlugin.text(
                    "§eПрогресс: §b" + quest.getProgress() + "§7/§b" + quest.getCount()
                            + "§7\n§7Выполняйте задание — прогресс пишется в чат"));
            case COMPLETED -> {
                lore.add(DailyQuestsPlugin.text("§aКвест выполнен!"));
                lore.add(DailyQuestsPlugin.text("§eНажмите ЛКМ, чтобы сдать квест"));
            }
            case CLAIMED -> lore.add(DailyQuestsPlugin.text("§8Квест сдан"));
            case BLOCKED -> lore.add(DailyQuestsPlugin.text("§8Квест заменён"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildClanItem(Quest quest, String takenBy) {
        ItemStack item = new ItemStack(iconFor(quest));
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.displayName(DailyQuestsPlugin.text(
                "§b[Клан] §f" + quest.getDisplay()));

        List<Component> lore = new ArrayList<>();
        lore.add(DailyQuestsPlugin.text("§7Награда: §e" + quest.getReward() + " монет в казну клана"));
        if (takenBy != null && !takenBy.isEmpty()) {
            lore.add(DailyQuestsPlugin.text("§7Взял: §a" + takenBy));
        }
        lore.add(Component.text(" "));

        switch (quest.getState()) {
            case AVAILABLE -> lore.add(DailyQuestsPlugin.text(
                    "§7Нажмите ЛКМ, чтобы взять квест для клана"));
            case ACTIVE -> lore.add(DailyQuestsPlugin.text(
                    "§eПрогресс: §b" + quest.getProgress() + "§7/§b" + quest.getCount()
                            + "\n§7Прогресс считается со всего клана"));
            case COMPLETED -> {
                lore.add(DailyQuestsPlugin.text("§aКвест выполнен!"));
                lore.add(DailyQuestsPlugin.text("§eНажмите ЛКМ, чтобы получить награду в казну"));
            }
            case CLAIMED -> lore.add(DailyQuestsPlugin.text("§8Квест сдан"));
            case BLOCKED -> lore.add(DailyQuestsPlugin.text("§8Квест заблокирован"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material iconFor(Quest quest) {
        QuestTypeData typeData = plugin.getConfigManager().getQuestTypeData(quest.getType());
        if (typeData != null) {
            Material fromConfig = Material.matchMaterial(typeData.getIcon());
            if (fromConfig != null) {
                return fromConfig;
            }
        }
        return Material.PAPER;
    }

    private void buildClanSection(Player player, Inventory inv) {
        if (plugin.getClanQuestManager() == null || !plugin.getConfigManager().isClanQuestsEnabled()) {
            return;
        }
        String clanName = FcClansHook.getClanName(player);
        if (clanName == null) {
            return;
        }
        ClanQuestData data = plugin.getClanQuestManager().getClanData(clanName);
        List<Quest> quests = data.getQuests();
        for (int i = 0; i < quests.size() && i < CLAN_SLOTS.length; i++) {
            inv.setItem(CLAN_SLOTS[i], buildClanItem(quests.get(i), data.getTakenBy()));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!event.getView().title().equals(TITLE)) {
            return;
        }
        event.setCancelled(true);

        int raw = event.getRawSlot();
        int clanIndex = clanSlotIndex(raw);
        if (clanIndex >= 0) {
            handleClanClick(player, clanIndex);
            open(player);
            return;
        }
        int index = personalSlotIndex(raw);
        if (index < 0) {
            return;
        }
        PlayerData data = plugin.getQuestManager().getData(player);
        if (index >= data.getQuests().size()) {
            return;
        }
        Quest quest = data.getQuests().get(index);

        switch (quest.getState()) {
            case AVAILABLE -> {
                if (plugin.getQuestManager().takeQuest(player, index)) {
                    plugin.msg(player, "quest-taken", "{display}", quest.getDisplay());
                } else {
                    plugin.msg(player, "quest-already-active");
                }
            }
            case COMPLETED -> {
                int total = plugin.getQuestManager().claimQuest(player, index);
                if (total >= 0) {
                    plugin.msg(player, "quest-claimed",
                            "{display}", quest.getDisplay(),
                            "{reward}", String.valueOf(total));
                }
            }
            case ACTIVE -> plugin.msg(player, "quest-in-progress",
                    "{display}", quest.getDisplay(),
                    "{progress}", String.valueOf(quest.getProgress()),
                    "{count}", String.valueOf(quest.getCount()));
            case CLAIMED -> plugin.msg(player, "quest-claimed-already");
            case BLOCKED -> plugin.msg(player, "quest-claimed-already");
        }
        open(player);
    }

    private void handleClanClick(Player player, int raw) {
        if (plugin.getClanQuestManager() == null || !plugin.getConfigManager().isClanQuestsEnabled()) {
            plugin.msg(player, "clan-quests-off");
            return;
        }
        String clanName = FcClansHook.getClanName(player);
        if (clanName == null) {
            plugin.msg(player, "not-in-clan");
            return;
        }
        ClanQuestData data = plugin.getClanQuestManager().getClanData(clanName);
        int index = clanSlotIndex(raw);
        if (index < 0 || index >= data.getQuests().size()) {
            return;
        }
        Quest quest = data.getQuests().get(index);

        switch (quest.getState()) {
            case AVAILABLE -> {
                if (plugin.getClanQuestManager().takeQuest(player, index)) {
                    plugin.msg(player, "clan-quest-taken", "{display}", quest.getDisplay());
                } else {
                    plugin.msg(player, "clan-already-active");
                }
            }
            case COMPLETED -> {
                int reward = plugin.getClanQuestManager().claimQuest(player, index);
                if (reward >= 0) {
                    plugin.msg(player, "clan-quest-claimed",
                            "{display}", quest.getDisplay(),
                            "{reward}", String.valueOf(reward));
                }
            }
            case ACTIVE -> plugin.msg(player, "clan-quest-in-progress",
                    "{display}", quest.getDisplay(),
                    "{progress}", String.valueOf(quest.getProgress()),
                    "{count}", String.valueOf(quest.getCount()));
            case CLAIMED -> plugin.msg(player, "clan-quest-claimed-already");
            case BLOCKED -> plugin.msg(player, "clan-quest-claimed-already");
        }
    }

    private static boolean isPersonalSlot(int slot) {
        return personalSlotIndex(slot) >= 0;
    }

    private static boolean isClanSlot(int slot) {
        return clanSlotIndex(slot) >= 0;
    }

    private static int personalSlotIndex(int slot) {
        for (int i = 0; i < QUEST_SLOTS.length; i++) {
            if (QUEST_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private static int clanSlotIndex(int slot) {
        for (int i = 0; i < CLAN_SLOTS.length; i++) {
            if (CLAN_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
}