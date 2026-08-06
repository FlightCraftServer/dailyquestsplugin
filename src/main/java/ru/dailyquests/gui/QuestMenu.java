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
import ru.dailyquests.config.QuestTypeData;
import ru.dailyquests.data.PlayerData;
import ru.dailyquests.quest.Quest;
import ru.dailyquests.quest.QuestDifficulty;
import ru.dailyquests.quest.QuestState;

import java.util.ArrayList;
import java.util.List;

public class QuestMenu implements Listener {

    private static final Component TITLE =
            DailyQuestsPlugin.text("§6§lЕжедневные квесты");
    private static final int[] QUEST_SLOTS = {11, 13, 15};
    private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

    private final DailyQuestsPlugin plugin;

    public QuestMenu(DailyQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        PlayerData data = plugin.getQuestManager().getData(player);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        ItemStack filler = new ItemStack(FILLER);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 27; i++) {
            if (!isQuestSlot(i)) {
                inv.setItem(i, filler);
            }
        }

        List<Quest> quests = data.getQuests();
        for (int i = 0; i < quests.size() && i < QUEST_SLOTS.length; i++) {
            inv.setItem(QUEST_SLOTS[i], buildItem(quests.get(i)));
        }

        player.openInventory(inv);
    }

    private ItemStack buildItem(Quest quest) {
        Material material = Material.PAPER;
        QuestTypeData typeData = plugin.getConfigManager().getQuestTypeData(quest.getType());
        if (typeData != null) {
            Material fromConfig = Material.matchMaterial(typeData.getIcon());
            if (fromConfig != null) {
                material = fromConfig;
            }
        }

        ItemStack item = new ItemStack(material);
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
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
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

        int index = slotIndex(event.getRawSlot());
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
        }
        open(player);
    }

    private static boolean isQuestSlot(int slot) {
        for (int s : QUEST_SLOTS) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }

    private static int slotIndex(int slot) {
        for (int i = 0; i < QUEST_SLOTS.length; i++) {
            if (QUEST_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
}
