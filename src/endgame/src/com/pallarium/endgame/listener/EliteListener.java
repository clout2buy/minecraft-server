package com.pallarium.endgame.listener;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.mob.Affix;
import com.pallarium.endgame.mob.EliteLoot;
import com.pallarium.endgame.mob.EliteService;
import com.pallarium.endgame.mob.EliteTier;
import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Spawn rolls, affix behaviour in combat, and elite death drops. */
public class EliteListener implements Listener {

    private final EndgamePlugin plugin;

    public EliteListener(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    private EliteService elites() {
        return plugin.elites();
    }

    /* ------------------------------------------------------------------ */
    /*  spawn                                                              */
    /* ------------------------------------------------------------------ */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (!plugin.getConfig().getBoolean("elites.enabled", true)) {
            return;
        }
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason != CreatureSpawnEvent.SpawnReason.NATURAL
                && reason != CreatureSpawnEvent.SpawnReason.SPAWNER
                && reason != CreatureSpawnEvent.SpawnReason.RAID
                && reason != CreatureSpawnEvent.SpawnReason.PATROL) {
            return;
        }
        LivingEntity entity = event.getEntity();
        // run a tick later so vanilla finishes equipping the mob first
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (entity.isValid() && !entity.isDead()) {
                elites().considerSpawn(entity);
            }
        });
    }

    /* ------------------------------------------------------------------ */
    /*  elite hits a player                                                */
    /* ------------------------------------------------------------------ */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEliteAttack(EntityDamageByEntityEvent event) {
        LivingEntity attacker = resolveAttacker(event.getDamager());
        if (attacker == null || !elites().isElite(attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        List<Affix> affixes = elites().affixesOf(attacker);
        EliteTier tier = elites().tierOf(attacker);
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        if (affixes.contains(Affix.BLAZING)) {
            victim.setFireTicks(Math.max(victim.getFireTicks(), 100));
        }
        if (affixes.contains(Affix.FROZEN)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2));
            victim.getWorld().spawnParticle(Particle.SNOWBALL,
                    victim.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.02);
        }
        if (affixes.contains(Affix.VENOMOUS)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120, 1));
        }
        if (affixes.contains(Affix.WITHERING)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0));
        }
        if (affixes.contains(Affix.OF_DECAY)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 140, 0));
        }
        if (affixes.contains(Affix.ELECTRIC) && rng.nextDouble() < 0.25) {
            victim.getWorld().strikeLightningEffect(victim.getLocation());
            victim.damage(3.0, attacker);
        }
        if (affixes.contains(Affix.OF_THE_STORM) && rng.nextDouble() < 0.3) {
            victim.setVelocity(victim.getVelocity().setY(1.1));
            victim.getWorld().playSound(victim.getLocation(),
                    Sound.ENTITY_PHANTOM_FLAP, 1f, 1f);
        }
        if (affixes.contains(Affix.VAMPIRIC)) {
            double heal = Math.min(event.getFinalDamage() * 0.5,
                    attacker.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)
                            .getValue() - attacker.getHealth());
            if (heal > 0) {
                attacker.setHealth(attacker.getHealth() + heal);
                attacker.getWorld().spawnParticle(Particle.HEART,
                        attacker.getLocation().add(0, 1.6, 0), 3, 0.2, 0.2, 0.2, 0);
            }
        }
        if (affixes.contains(Affix.MOLTEN) && rng.nextDouble() < 0.3) {
            Location at = victim.getLocation();
            if (at.getBlock().getType() == Material.AIR) {
                at.getBlock().setType(Material.FIRE);
            }
        }
        if (affixes.contains(Affix.OF_THIEVES) && rng.nextDouble() < 0.35) {
            victim.sendMessage(Component.text("  Your pockets feel lighter.", Icon.WARN)
                    .decoration(TextDecoration.ITALIC, false));
            victim.giveExp(-Math.min(victim.getTotalExperience(), 20));
        }
        if (tier != null) {
            markEngaged(victim, attacker);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  player hits an elite                                               */
    /* ------------------------------------------------------------------ */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEliteHurt(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob) || !elites().isElite(mob)) {
            return;
        }
        Player player = resolvePlayer(event.getDamager());
        if (player == null) {
            return;
        }
        markEngaged(player, mob);

        List<Affix> affixes = elites().affixesOf(mob);
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        if (affixes.contains(Affix.ARMORED)) {
            event.setDamage(event.getDamage() * 0.45);
        }
        if (affixes.contains(Affix.OF_THE_VOID) && rng.nextDouble() < 0.25) {
            Location behind = player.getLocation().add(
                    player.getLocation().getDirection().multiply(-1.5));
            behind.setY(player.getLocation().getY());
            mob.getWorld().spawnParticle(Particle.PORTAL, mob.getLocation(), 30, 0.4, 0.8, 0.4, 0.4);
            mob.teleport(behind);
            mob.getWorld().playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
        }
        if (affixes.contains(Affix.OF_THE_SWARM)) {
            double pct = mob.getHealth() / mob.getAttribute(
                    org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            if (pct < 0.5 && !mob.getPersistentDataContainer().has(
                    plugin.elites().engagedKey(), PersistentDataType.INTEGER)) {
                summonSwarm(mob, player);
            }
        }
        if (affixes.contains(Affix.SPECTRAL) && rng.nextDouble() < 0.4) {
            mob.getWorld().spawnParticle(Particle.SMOKE_NORMAL,
                    mob.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.01);
        }
    }

    private void summonSwarm(LivingEntity mob, Player target) {
        mob.getPersistentDataContainer().set(
                plugin.elites().engagedKey(), PersistentDataType.INTEGER, 1);
        World w = mob.getWorld();
        int count = 2 + ThreadLocalRandom.current().nextInt(3);
        for (int i = 0; i < count; i++) {
            Location at = mob.getLocation().add(
                    ThreadLocalRandom.current().nextDouble(-2, 2), 0,
                    ThreadLocalRandom.current().nextDouble(-2, 2));
            Entity spawned = w.spawnEntity(at, mob.getType() == EntityType.SKELETON
                    ? EntityType.SKELETON : EntityType.ZOMBIE);
            if (spawned instanceof Mob minion) {
                minion.setTarget(target);
                minion.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)
                        .setBaseValue(8.0);
                minion.setHealth(8.0);
                minion.setRemoveWhenFarAway(true);
            }
        }
        w.spawnParticle(Particle.SCULK_SOUL, mob.getLocation().add(0, 1, 0),
                30, 0.6, 0.6, 0.6, 0.05);
        w.playSound(mob.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 1.1f);
        target.sendMessage(Component.text("  The swarm answers its call.", Icon.BAD)
                .decoration(TextDecoration.ITALIC, false));
    }

    /* ------------------------------------------------------------------ */
    /*  death                                                              */
    /* ------------------------------------------------------------------ */

    /** An ordinary mob dies and something worse climbs out of it. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNormalDeath(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("elites.enabled", true)) {
            return;
        }
        LivingEntity dead = event.getEntity();
        if (elites().isElite(dead)) {
            return;
        }
        Player killer = dead.getKiller();
        if (killer == null) {
            return;
        }
        Location where = dead.getLocation().clone();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!killer.isOnline()) {
                return;
            }
            LivingEntity risen = elites().considerAscension(dead, killer, where);
            if (risen == null) {
                return;
            }
            EliteTier tier = elites().tierOf(risen);
            if (tier == null) {
                return;
            }
            killer.sendMessage(Component.text("  " + tier.glyph() + " ", tier.textColor())
                    .append(Component.text("Risen from the corpse: ", Icon.WARN))
                    .append(Component.text(elites().nameOf(risen), tier.textColor()))
                    .decoration(TextDecoration.ITALIC, false));
            if (tier.ordinal() >= EliteTier.CHAMPION.ordinal()
                    && plugin.settings().broadcastMilestones()) {
                plugin.getServer().broadcast(Component.text("  " + tier.glyph() + " ",
                                tier.textColor())
                        .append(Component.text(elites().nameOf(risen), tier.textColor()))
                        .append(Component.text(" tore free of a corpse near ", Icon.DIM))
                        .append(Component.text(killer.getName(), Icon.NEON))
                        .decoration(TextDecoration.ITALIC, false));
            }
        }, 12L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEliteDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        if (!elites().isElite(mob)) {
            return;
        }
        EliteTier tier = elites().tierOf(mob);
        if (tier == null) {
            return;
        }
        List<Affix> affixes = elites().affixesOf(mob);
        Location at = mob.getLocation();
        World w = at.getWorld();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // rebirth: one free resurrection instead of dying
        if (affixes.contains(Affix.OF_REBIRTH) && !mob.getPersistentDataContainer()
                .has(rebirthKey(), PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            event.getDrops().clear();
            double max = mob.getAttribute(
                    org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            mob.setHealth(max * 0.5);
            mob.getPersistentDataContainer().set(rebirthKey(), PersistentDataType.INTEGER, 1);
            mob.setFireTicks(0);
            w.spawnParticle(Particle.TOTEM, at.add(0, 1, 0), 60, 0.5, 0.8, 0.5, 0.4);
            w.playSound(at, Sound.ITEM_TOTEM_USE, 1f, 0.8f);
            Player k = mob.getKiller();
            if (k != null) {
                k.sendMessage(Component.text("  It refuses to stay dead.", Icon.WARN)
                        .decoration(TextDecoration.ITALIC, false));
            }
            return;
        }

        // volatile mobs detonate
        if (affixes.contains(Affix.VOLATILE)) {
            w.createExplosion(at, 2.5f, false, false, mob);
            w.spawnParticle(Particle.EXPLOSION_LARGE, at, 3);
        }

        event.setDroppedExp((int) Math.round(event.getDroppedExp() * tier.lootMult()));

        List<ItemStack> loot = new java.util.ArrayList<>(EliteLoot.roll(tier, affixes));

        // a chance at the actual gear it was wearing, thrown into the same spout
        if (mob.getEquipment() != null) {
            ItemStack[] worn = {
                    mob.getEquipment().getHelmet(), mob.getEquipment().getChestplate(),
                    mob.getEquipment().getLeggings(), mob.getEquipment().getBoots(),
                    mob.getEquipment().getItemInMainHand()};
            for (ItemStack piece : worn) {
                if (piece != null && piece.getType() != Material.AIR
                        && rng.nextDouble() < tier.gearDropChance()) {
                    loot.add(piece.clone());
                }
            }
        }

        // vanilla drops would plop on the floor and spoil the eruption
        event.getDrops().clear();
        com.pallarium.endgame.mob.LootFountain.erupt(
                plugin, at, loot, tier, mob.getKiller());

        w.spawnParticle(Particle.SOUL_FIRE_FLAME, at.clone().add(0, 1, 0),
                40, 0.4, 0.6, 0.4, 0.06);
        w.playSound(at, Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.5f);

        Player killer = mob.getKiller();
        if (killer != null) {
            killer.sendMessage(Component.text("  " + tier.glyph() + " ", tier.textColor())
                    .append(Component.text("Slain: ", Icon.DIM))
                    .append(Component.text(elites().nameOf(mob), tier.textColor()))
                    .append(Component.text("  (+" + loot.size() + " drops)", Icon.GOOD))
                    .decoration(TextDecoration.ITALIC, false));
            // elites are worth far more combat xp
            plugin.xp().award(killer, com.pallarium.endgame.skill.Skill.COMBAT,
                    (int) Math.round(20 * tier.lootMult() * (1 + elites().levelOf(mob) / 40.0)));
            if (tier.ordinal() >= EliteTier.CHAMPION.ordinal()
                    && plugin.settings().broadcastMilestones()) {
                plugin.getServer().broadcast(Component.text("  " + tier.glyph() + " ",
                                tier.textColor())
                        .append(Component.text(killer.getName(), Icon.NEON))
                        .append(Component.text(" felled ", Icon.DIM))
                        .append(Component.text(elites().nameOf(mob), tier.textColor()))
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
    }

    /* ------------------------------------------------------------------ */

    private org.bukkit.NamespacedKey rebirthKey() {
        return new org.bukkit.NamespacedKey(plugin, "elite_reborn");
    }

    private void markEngaged(Player player, LivingEntity mob) {
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "elite_seen");
        if (!mob.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            mob.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
            elites().announce(player, mob);
        }
    }

    private LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity le) {
            return le;
        }
        if (damager instanceof Projectile proj
                && proj.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }
}
