package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class GameListener implements Listener {
    private final EventMakerPlugin plugin;

    public GameListener(EventMakerPlugin plugin) {
        this.plugin = plugin;
    }

    private void race(ScoreRace.Kind kind, Player p, double amount) {
        for (ActiveEvent a : plugin.engine().running()) {
            if (a instanceof ScoreRace && ((ScoreRace) a).kind == kind) ((ScoreRace) a).score(p, amount);
        }
    }

    @EventHandler
    public void onMeteorHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Fireball) || !e.getEntity().hasMetadata(MeteorShower.META)) return;
        for (ActiveEvent a : plugin.engine().running()) {
            if (a instanceof MeteorShower) ((MeteorShower) a).onImpact(e.getEntity().getLocation());
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        if (e.getEntity().hasMetadata(MeteorShower.META) || e.getEntity().hasMetadata(Swarm.META)) e.blockList().clear();
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        Player attacker = null;
        if (e.getDamager() instanceof Player) attacker = (Player) e.getDamager();
        else if (e.getDamager() instanceof Projectile && ((Projectile) e.getDamager()).getShooter() instanceof Player) {
            attacker = (Player) ((Projectile) e.getDamager()).getShooter();
        }
        if (attacker == null) return;
        for (ActiveEvent a : plugin.engine().running()) {
            if (a instanceof BossFight && ((BossFight) a).isBoss(e.getEntity())) {
                ((BossFight) a).onDamaged(attacker, e.getFinalDamage());
            }
        }
    }

    @EventHandler
    public void onHazardDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        boolean hazard = e.getCause() == EntityDamageEvent.DamageCause.LIGHTNING
                || e.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK;
        if (!hazard) return;
        for (ActiveEvent a : plugin.engine().running()) {
            if (a instanceof SkyHazard) ((SkyHazard) a).onHurt((Player) e.getEntity(), e.getFinalDamage());
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        Entity dead = e.getEntity();
        Player killer = e.getEntity().getKiller();
        for (ActiveEvent a : plugin.engine().running()) {
            if (a instanceof BossFight && ((BossFight) a).isBoss(dead)) {
                e.getDrops().clear();
                ((BossFight) a).onKilled();
            } else if (a instanceof MobWave && dead.hasMetadata(MobWave.META) && killer != null) {
                ((MobWave) a).onKill(killer);
            } else if (a instanceof Swarm && dead.hasMetadata(Swarm.META) && killer != null) {
                ((Swarm) a).onKill(killer);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || killer == e.getEntity()) return;
        race(ScoreRace.Kind.PVP, killer, 1);
        for (ActiveEvent a : plugin.engine().running()) {
            if (a instanceof Bounty && ((Bounty) a).isTarget(e.getEntity())) ((Bounty) a).onTargetKilled(killer);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.CHEST) return;
        for (ActiveEvent a : plugin.engine().running()) {
            if (a instanceof TreasureHunt && ((TreasureHunt) a).isChest(e.getClickedBlock().getLocation())) {
                ((TreasureHunt) a).onOpened(e.getPlayer());
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Material m = e.getBlock().getType();
        if (m == Material.CHEST) {
            for (ActiveEvent a : plugin.engine().running()) {
                if (a instanceof TreasureHunt && ((TreasureHunt) a).isChest(e.getBlock().getLocation())) {
                    ((TreasureHunt) a).onOpened(e.getPlayer());
                }
            }
        }
        race(ScoreRace.Kind.MINING, e.getPlayer(), 1);
        if (ScoreRace.isOre(m)) race(ScoreRace.Kind.ORE, e.getPlayer(), 1);
        if (ScoreRace.isLog(m)) race(ScoreRace.Kind.LOGS, e.getPlayer(), 1);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        race(ScoreRace.Kind.BUILD, e.getPlayer(), 1);
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) race(ScoreRace.Kind.FISH, e.getPlayer(), 1);
    }

    @EventHandler
    public void onXp(PlayerExpChangeEvent e) {
        if (e.getAmount() > 0) race(ScoreRace.Kind.XP, e.getPlayer(), e.getAmount());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null || e.getFrom().getWorld() != e.getTo().getWorld()) return;
        double d = e.getFrom().distance(e.getTo());
        if (d <= 0 || d > 10) return;
        race(ScoreRace.Kind.DISTANCE, e.getPlayer(), d);
    }
}
