package ru.dailyquests.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import io.papermc.paper.event.player.PlayerTradeEvent;
import ru.dailyquests.DailyQuestsPlugin;
import ru.dailyquests.quest.QuestType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProgressListener implements Listener {

    private static final int DIST_WALK = 0;
    private static final int DIST_SPRINT = 1;
    private static final int DIST_SWIM = 2;
    private static final int DIST_FLY = 3;

    private final DailyQuestsPlugin plugin;
    private final Map<UUID, String> lastBiome = new HashMap<>();
    private final Map<UUID, Boolean> lastOnGround = new HashMap<>();
    private final Map<UUID, double[]> distances = new HashMap<>();

    public ProgressListener(DailyQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastBiome.remove(id);
        lastOnGround.remove(id);
        distances.remove(id);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        String block = event.getBlock().getType().name();
        plugin.getQuestManager().incrementProgress(p, QuestType.BREAK_BLOCK, block, 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMineBlock(BlockBreakEvent event) {
        Player p = event.getPlayer();
        Block block = event.getBlock();
        String target = block.getType().name();
        if (!plugin.getQuestManager().hasActiveQuest(p, QuestType.MINE_BLOCK, target)) {
            return;
        }
        Collection<ItemStack> drops = block.getDrops(p.getInventory().getItemInMainHand());
        int amount = 0;
        for (ItemStack drop : drops) {
            if (drop != null && drop.getType() != Material.AIR) {
                amount += Math.max(1, drop.getAmount());
            }
        }
        if (amount > 0) {
            plugin.getQuestManager().incrementProgress(p, QuestType.MINE_BLOCK, target, amount);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        plugin.getQuestManager().incrementProgress(p, QuestType.PLACE_BLOCK, event.getBlock().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Player p = event.getEntity().getKiller();
        if (p == null) {
            return;
        }
        plugin.getQuestManager().incrementProgress(p, QuestType.KILL_MOB, event.getEntityType().name(), 1);
        plugin.getQuestManager().incrementProgress(p, QuestType.KILL_ANY, "", 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        plugin.getQuestManager().incrementProgress(p, QuestType.PICKUP_ITEM, item.getType().name(),
                Math.max(1, item.getAmount()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        plugin.getQuestManager().incrementProgress(p, QuestType.DROP_ITEM,
                event.getItemDrop().getItemStack().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player p)) {
            return;
        }
        if (event.getEntity() == p || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        int damage = (int) Math.ceil(event.getFinalDamage());
        if (damage > 0) {
            plugin.getQuestManager().incrementProgress(p, QuestType.DEAL_DAMAGE, "", damage);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player p) {
            plugin.getQuestManager().incrementProgress(p, QuestType.SHOOT, "", 1);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        if (dx == 0 && dy == 0 && dz == 0) {
            return;
        }

        boolean blockMoved = from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
        if (blockMoved) {
            UUID id = p.getUniqueId();

            String biome = to.getBlock().getBiome().name();
            String last = lastBiome.put(id, biome);
            if (!biome.equals(last)) {
                plugin.getQuestManager().incrementProgress(p, QuestType.VISIT_BIOME, biome, 1);
            }

            boolean onGround = p.isOnGround();
            Boolean wasOnGround = lastOnGround.put(id, onGround);
            if (dy > 0 && Boolean.TRUE.equals(wasOnGround) && !onGround) {
                plugin.getQuestManager().incrementProgress(p, QuestType.JUMP, "", 1);
            }
        }

        double dist2d = Math.sqrt(dx * dx + dz * dz);
        if (dist2d <= 0) {
            return;
        }
        QuestManager qm = plugin.getQuestManager();
        UUID id = p.getUniqueId();
        if (p.isOnGround() && !p.isSprinting() && qm.hasActiveQuest(p, QuestType.WALK, "")) {
            addDistance(id, DIST_WALK, dist2d, QuestType.WALK, p);
        }
        if (p.isOnGround() && p.isSprinting() && qm.hasActiveQuest(p, QuestType.SPRINT, "")) {
            addDistance(id, DIST_SPRINT, dist2d, QuestType.SPRINT, p);
        }
        if (p.isSwimming() && qm.hasActiveQuest(p, QuestType.SWIM, "")) {
            addDistance(id, DIST_SWIM, dist2d, QuestType.SWIM, p);
        }
        if ((p.isGliding() || p.isFlying()) && qm.hasActiveQuest(p, QuestType.FLY, "")) {
            addDistance(id, DIST_FLY, dist2d, QuestType.FLY, p);
        }
    }

    private void addDistance(UUID id, int index, double amount, QuestType type, Player p) {
        double[] acc = distances.computeIfAbsent(id, k -> new double[4]);
        acc[index] += amount;
        if (acc[index] >= 1.0) {
            int meters = (int) acc[index];
            acc[index] -= meters;
            plugin.getQuestManager().incrementProgress(p, type, "", meters);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        plugin.getQuestManager().incrementProgress(event.getPlayer(), QuestType.FISH, "", 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTrade(PlayerTradeEvent event) {
        plugin.getQuestManager().incrementProgress(event.getPlayer(), QuestType.TRADE, "", 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        plugin.getQuestManager().incrementProgress(p, QuestType.CRAFT, result.getType().name(),
                Math.max(1, result.getAmount()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSmelt(FurnaceExtractEvent event) {
        Player p = event.getPlayer();
        int amount = Math.max(1, event.getItemAmount());
        plugin.getQuestManager().incrementProgress(p, QuestType.SMELT, event.getItemType().name(), amount);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEnchant(EnchantItemEvent event) {
        if (!(event.getEnchanter() instanceof Player p)) {
            return;
        }
        plugin.getQuestManager().incrementProgress(p, QuestType.ENCHANT, "", 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player p)) {
            return;
        }
        plugin.getQuestManager().incrementProgress(p, QuestType.BREED, "", 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player p) {
            plugin.getQuestManager().incrementProgress(p, QuestType.TAME, "", 1);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onShear(PlayerShearEntityEvent event) {
        plugin.getQuestManager().incrementProgress(event.getPlayer(), QuestType.SHEAR, "", 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMilk(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        String type = event.getRightClicked().getType().name();
        if (!type.equals("COW") && !type.equals("MUSHROOM_COW") && !type.equals("GOAT")) {
            return;
        }
        if (p.getInventory().getItemInMainHand().getType() != Material.BUCKET) {
            return;
        }
        plugin.getQuestManager().incrementProgress(p, QuestType.MILK, "", 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onConsume(PlayerItemConsumeEvent event) {
        plugin.getQuestManager().incrementProgress(event.getPlayer(), QuestType.EAT,
                event.getItem().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onUseItem(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        plugin.getQuestManager().incrementProgress(event.getPlayer(), QuestType.USE_ITEM,
                item.getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        String env = switch (p.getWorld().getEnvironment()) {
            case NORMAL -> "OVERWORLD";
            case NETHER -> "NETHER";
            case THE_END -> "THE_END";
            case CUSTOM -> p.getWorld().getName();
        };
        plugin.getQuestManager().incrementProgress(p, QuestType.VISIT_DIMENSION, env, 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onExpChange(PlayerExpChangeEvent event) {
        int amount = event.getAmount();
        if (amount > 0) {
            plugin.getQuestManager().incrementProgress(event.getPlayer(), QuestType.XP, "", amount);
        }
    }
}
