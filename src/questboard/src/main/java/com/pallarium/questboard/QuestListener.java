package com.pallarium.questboard;

import com.pallarium.questboard.model.QuestType;
import org.bukkit.Material;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestListener implements Listener {
    private final QuestBoardPlugin plugin;
    private final Map<UUID, Double> walkBuffer = new HashMap<>();

    public QuestListener(QuestBoardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.quests().ensureToday(p);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) p.sendMessage(net.md_5.bungee.api.ChatColor.GOLD + "★ " + net.md_5.bungee.api.ChatColor.YELLOW + "New daily quests are waiting. " + net.md_5.bungee.api.ChatColor.GRAY + "/quests");
        }, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Material m = e.getBlock().getType();
        if (isCrop(m) && !isMature(e.getBlock())) return;
        plugin.quests().progress(e.getPlayer(), QuestType.MINE, q -> q.matchesBlock(m), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Material m = e.getBlock().getType();
        plugin.quests().progress(e.getPlayer(), QuestType.PLACE, q -> q.matchesBlock(m), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        boolean hostile = e.getEntity() instanceof Monster;
        plugin.quests().progress(killer, QuestType.KILL, q -> q.matchesEntity(e.getEntityType(), hostile), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        plugin.quests().progress(e.getPlayer(), QuestType.FISH, q -> true, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        ItemStack result = e.getRecipe().getResult();
        Material m = result.getType();
        int amount = result.getAmount();
        if (e.isShiftClick()) {
            int min = Integer.MAX_VALUE;
            for (ItemStack s : e.getInventory().getMatrix()) {
                if (s != null && s.getType() != Material.AIR) min = Math.min(min, s.getAmount());
            }
            if (min != Integer.MAX_VALUE) amount *= min;
        }
        final int total = amount;
        plugin.quests().progress((Player) e.getWhoClicked(), QuestType.CRAFT, q -> q.matchesBlock(m), total);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null || e.getFrom().getWorld() != e.getTo().getWorld()) return;
        double dx = e.getTo().getX() - e.getFrom().getX();
        double dz = e.getTo().getZ() - e.getFrom().getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d <= 0 || d > 8) return;
        UUID id = e.getPlayer().getUniqueId();
        double buf = walkBuffer.getOrDefault(id, 0.0) + d;
        if (buf >= 1.0) {
            int whole = (int) buf;
            walkBuffer.put(id, buf - whole);
            plugin.quests().progress(e.getPlayer(), QuestType.WALK, q -> true, whole);
        } else {
            walkBuffer.put(id, buf);
        }
    }

    private boolean isCrop(Material m) {
        return m == Material.WHEAT || m == Material.CARROTS || m == Material.POTATOES || m == Material.BEETROOTS;
    }

    private boolean isMature(org.bukkit.block.Block b) {
        if (b.getBlockData() instanceof org.bukkit.block.data.Ageable) {
            org.bukkit.block.data.Ageable a = (org.bukkit.block.data.Ageable) b.getBlockData();
            return a.getAge() >= a.getMaximumAge();
        }
        return true;
    }
}
