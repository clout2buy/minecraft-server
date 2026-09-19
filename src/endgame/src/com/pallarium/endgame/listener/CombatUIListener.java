package com.pallarium.endgame.listener;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.item.Tooltip;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Drives the floating combat UI: the heart bar that follows a mob and the
 * damage number that pops on every hit. Runs at MONITOR so the value shown is
 * the real damage after armour, resistance and every affix has taken its cut.
 */
public class CombatUIListener implements Listener {

    private final EndgamePlugin plugin;

    public CombatUIListener(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("healthbars.enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim) || victim instanceof Player) {
            return;
        }

        double dealt = event.getFinalDamage();
        // the bar must read the health AFTER this hit lands
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (victim.isValid() && !victim.isDead()) {
                plugin.bars().show(victim);
            }
        });

        if (!plugin.getConfig().getBoolean("healthbars.damage-numbers", true)) {
            return;
        }
        Player source = resolve(event);
        if (source == null) {
            return;
        }
        // vanilla crit: falling, not on ground, not riding, no blindness
        boolean crit = source.getFallDistance() > 0 && !source.isOnGround()
                && !source.isInsideVehicle() && !source.isSprinting();
        if (event.getDamager() instanceof Arrow a && a.isCritical()) {
            crit = true;
        }
        plugin.bars().popDamage(victim, dealt, crit);
    }

    /** Clears the bar the moment the mob dies so it never lingers. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        plugin.bars().clear(event.getEntity());
    }

    /** Elites wear their plate from the instant they appear. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            LivingEntity e = event.getEntity();
            if (e.isValid() && !e.isDead() && plugin.elites().isElite(e)) {
                plugin.bars().attach(e);
            }
        }, 3L);
    }

    /* ------------------------------------------------------------------ */
    /*  tooltips                                                           */
    /* ------------------------------------------------------------------ */

    /** Anything you pick up gets the custom card if it does not have one. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (!plugin.getConfig().getBoolean("tooltips.enabled", true)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (stack == null || !Tooltip.needsCard(plugin, stack)) {
            return;
        }
        event.getItem().setItemStack(Tooltip.apply(plugin, stack));
    }

    /** Re-cards whatever is already in a returning player's inventory. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("tooltips.enabled", true)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player p = event.getPlayer();
            ItemStack[] contents = p.getInventory().getContents();
            boolean changed = false;
            for (int i = 0; i < contents.length; i++) {
                ItemStack s = contents[i];
                if (s == null || !Tooltip.needsCard(plugin, s)) {
                    continue;
                }
                contents[i] = Tooltip.apply(plugin, s);
                changed = true;
            }
            if (changed) {
                p.getInventory().setContents(contents);
            }
        }, 20L);
    }

    private Player resolve(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) {
            return p;
        }
        if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }
}
