package com.pallarium.endgame.listener;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.boss.BossInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Routes vanilla combat events into the boss engine: damage accounting for the
 * shield and the threat table, clean death handling, and keeping minions from
 * wandering off after unrelated players.
 */
public class BossListener implements Listener {

    private final EndgamePlugin plugin;

    public BossListener(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    /** Damage dealt TO a boss: shield soak, threat, and the bar refresh. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBossDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        BossInstance inst = plugin.bosses().of(victim);
        if (inst == null) {
            return;
        }
        Player attacker = resolvePlayer(event.getDamager());
        double dealt = event.getFinalDamage();

        if (inst.shielded()) {
            // the barrier soaks it, the boss takes nothing until it breaks
            inst.absorb(dealt, attacker);
            event.setCancelled(true);
            return;
        }
        if (attacker != null) {
            inst.addThreat(attacker, dealt);
        }
        inst.onDamaged(dealt);
    }

    /** A boss dying: loot, xp, announcement, cleanup. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        BossInstance inst = plugin.bosses().of(event.getEntity());
        if (inst != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            inst.onDeath();
            return;
        }
        // a minion died, let its fight know so the counter stays honest
        BossInstance owner = plugin.bosses().ownerOfMinion(event.getEntity());
        if (owner != null) {
            owner.onMinionDeath(event.getEntity());
        }
    }

    /** Boss minions stay on the fight, they do not chase villagers. */
    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        BossInstance owner = plugin.bosses().ownerOfMinion(event.getEntity());
        if (owner == null) {
            return;
        }
        if (event.getTarget() != null && !(event.getTarget() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        for (BossInstance inst : plugin.bosses().active()) {
            inst.dropPlayer(event.getPlayer());
        }
    }

    private Player resolvePlayer(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile proj
                && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }
}
