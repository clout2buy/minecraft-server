package com.pallarium.endgame.listener;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.data.PlayerProfile;
import com.pallarium.endgame.service.DamageIndicator;
import com.pallarium.endgame.service.PerkEngine;
import com.pallarium.endgame.service.XpSources;
import com.pallarium.endgame.skill.Effect;
import com.pallarium.endgame.skill.Perk;
import com.pallarium.endgame.skill.Skill;
import com.pallarium.endgame.ui.Icon;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

/**
 * Every XP source and every perk trigger. The actual effects live in
 * {@link PerkEngine} so this class stays a dispatch table.
 */
public class SkillListener implements Listener {

    private final EndgamePlugin plugin;

    public SkillListener(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    private PerkEngine fx() {
        return plugin.perks();
    }

    private boolean counts(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL
                || player.getGameMode() == GameMode.ADVENTURE;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.settings().antiFarm()) {
            plugin.placed().mark(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!counts(player)) {
            return;
        }
        Block block = event.getBlock();
        boolean wasPlaced = plugin.settings().antiFarm() && plugin.placed().consume(block);

        XpSources.Entry entry = XpSources.forBlock(block.getType());
        if (entry == null || wasPlaced) {
            return;
        }

        boolean wasCrop = XpSources.isAgeable(block.getType());
        if (wasCrop && block.getBlockData() instanceof Ageable ageable
                && ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        PlayerProfile profile = plugin.store().cached(player.getUniqueId());
        if (profile == null) {
            return;
        }

        plugin.xp().award(player, entry.skill(), entry.xp());
        applyBreakPerks(player, profile, entry.skill(), block, wasCrop);
    }

    private void applyBreakPerks(Player player, PlayerProfile profile, Skill skill,
                                 Block block, boolean wasCrop) {
        PerkEngine fx = fx();
        Location at = block.getLocation().add(0.5, 0.35, 0.5);

        for (Perk perk : Perk.of(skill)) {
            double value = profile.perkValue(perk);
            if (value <= 0) {
                continue;
            }
            switch (perk.effect()) {
                case DOUBLE_DROP -> {
                    if (fx.roll(value)) {
                        fx.duplicateDrops(block, at);
                    }
                }
                case HASTE -> {
                    if (fx.roll(value)) {
                        fx.potion(player, PotionEffectType.FAST_DIGGING, 8, 1);
                    }
                }
                case SPEED -> {
                    if (fx.roll(value)) {
                        fx.potion(player, PotionEffectType.SPEED, 8, 1);
                    }
                }
                case REGEN -> {
                    if (fx.roll(value)) {
                        fx.potion(player, PotionEffectType.REGENERATION, 6, 0);
                    }
                }
                case NIGHT_VISION -> {
                    if (block.getLocation().getBlockY() < 50 && fx.roll(value)) {
                        fx.potion(player, PotionEffectType.NIGHT_VISION, 30, 0);
                    }
                }
                case FOOD -> {
                    if (fx.roll(value)) {
                        fx.feed(player, 1);
                    }
                }
                case TREASURE -> {
                    if (fx.roll(value)) {
                        fx.dropTreasure(player, at);
                    }
                }
                case AUTO_SMELT -> {
                    if (fx.roll(value)) {
                        fx.autoSmelt(player, block, at);
                    }
                }
                case MAGNET -> {
                    if (fx.roll(value)) {
                        fx.magnet(player, at);
                    }
                }
                case ORE_BURST -> {
                    if (fx.roll(value)) {
                        fx.oreBurst(player, at);
                    }
                }
                case LOOT_CHEST -> {
                    if (fx.roll(value)) {
                        fx.lootChest(player, block.getLocation(), perk);
                    }
                }
                case TIMBER_FELL -> {
                    if (fx.roll(value)) {
                        fx.fellTree(player, block);
                    }
                }
                case REPLANT -> {
                    if (wasCrop && fx.roll(value)) {
                        fx.replant(block);
                    }
                }
                default -> {
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        PerkEngine fx = fx();

        if (event.getDamager() instanceof Player player && counts(player)) {
            PlayerProfile profile = plugin.store().cached(player.getUniqueId());
            if (profile == null) {
                return;
            }
            meleeOrRanged(player, profile, victim, event, Skill.COMBAT, fx);
            return;
        }

        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter && counts(shooter)) {
            PlayerProfile profile = plugin.store().cached(shooter.getUniqueId());
            if (profile == null) {
                return;
            }
            meleeOrRanged(shooter, profile, victim, event, Skill.ARCHERY, fx);

            if (projectile instanceof Arrow arrow) {
                double save = fx.sum(profile, Skill.ARCHERY, Effect.ARROW_SAVE);
                if (fx.roll(save) && arrow.getPickupStatus() != AbstractArrow.PickupStatus.DISALLOWED) {
                    shooter.getInventory().addItem(new ItemStack(org.bukkit.Material.ARROW));
                }
            }
        }
    }

    private void meleeOrRanged(Player player, PlayerProfile profile, LivingEntity victim,
                               EntityDamageByEntityEvent event, Skill skill, PerkEngine fx) {
        double bonus = 0;
        boolean critFired = false;

        for (Perk perk : Perk.of(skill)) {
            double value = profile.perkValue(perk);
            if (value <= 0) {
                continue;
            }
            switch (perk.effect()) {
                case MELEE_DAMAGE, BOW_DAMAGE -> bonus += value;
                case CRIT -> {
                    if (!critFired && fx.roll(value)) {
                        critFired = true;
                    }
                }
                case LIGHTNING -> {
                    if (fx.roll(value)) {
                        fx.lightning(player, victim);
                    }
                }
                case ARROW_STORM -> {
                    if (fx.roll(value)) {
                        fx.arrowStorm(player, victim);
                    }
                }
                case SPEED -> {
                    if (fx.roll(value)) {
                        fx.potion(player, PotionEffectType.SPEED, 8, 1);
                    }
                }
                case LIFESTEAL -> {
                    fx.heal(player, value);
                    plugin.indicators().label(player.getLocation().add(0, 1.9, 0),
                            String.format("%.1f", value), Icon.GOOD, DamageIndicator.Kind.HEAL);
                }
                default -> {
                }
            }
        }

        if (bonus > 0) {
            event.setDamage(event.getDamage() + bonus);
        }
        if (critFired) {
            event.setDamage(event.getDamage() * 2.0);
            victim.getWorld().spawnParticle(org.bukkit.Particle.CRIT_MAGIC,
                    victim.getLocation().add(0, 1, 0), 24, 0.3, 0.4, 0.3, 0.2);
            victim.getWorld().playSound(victim.getLocation(),
                    org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
        }

        double total = event.getFinalDamage();
        DamageIndicator.Kind kind;
        if (victim.getHealth() - total <= 0) {
            kind = DamageIndicator.Kind.KILL;
        } else if (critFired) {
            kind = DamageIndicator.Kind.CRIT;
        } else if (bonus > 0) {
            kind = DamageIndicator.Kind.HEAVY;
        } else {
            kind = DamageIndicator.Kind.NORMAL;
        }
        plugin.indicators().damage(player, victim, total, kind);
    }

    /** Damage reduction stacks across every skill the player has trained. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerHurt(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !counts(player)) {
            return;
        }
        PlayerProfile profile = plugin.store().cached(player.getUniqueId());
        if (profile == null) {
            return;
        }
        double reduction = fx().sumAll(profile, Effect.DAMAGE_REDUCTION);
        if (reduction <= 0) {
            return;
        }
        reduction = Math.min(60.0, reduction);
        event.setDamage(event.getDamage() * (1.0 - reduction / 100.0));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null || !counts(killer)) {
            return;
        }
        int xp = XpSources.forMob(dead.getType());
        if (xp <= 0) {
            return;
        }

        boolean ranged = dead.getLastDamageCause() instanceof EntityDamageByEntityEvent cause
                && cause.getDamager() instanceof Projectile;
        Skill skill = ranged ? Skill.ARCHERY : Skill.COMBAT;

        plugin.xp().award(killer, skill, xp);

        PlayerProfile profile = plugin.store().cached(killer.getUniqueId());
        if (profile == null) {
            return;
        }
        PerkEngine fx = fx();
        Location at = dead.getLocation();

        for (Perk perk : Perk.of(skill)) {
            double value = profile.perkValue(perk);
            if (value <= 0) {
                continue;
            }
            switch (perk.effect()) {
                case DOUBLE_DROP -> {
                    if (fx.roll(value)) {
                        for (ItemStack drop : event.getDrops()) {
                            if (drop != null) {
                                at.getWorld().dropItemNaturally(at, drop.clone());
                            }
                        }
                    }
                }
                case TREASURE -> {
                    if (fx.roll(value)) {
                        fx.dropTreasure(killer, at);
                    }
                }
                case LOOT_CHEST -> {
                    if (fx.roll(value)) {
                        fx.lootChest(killer, at, perk);
                    }
                }
                case SUMMON_WOLF -> {
                    if (fx.roll(value)) {
                        fx.summonWolf(killer, at);
                    }
                }
                case REGEN -> {
                    if (fx.roll(value)) {
                        fx.potion(killer, PotionEffectType.REGENERATION, 6, 0);
                    }
                }
                case SPEED -> {
                    if (fx.roll(value)) {
                        fx.potion(killer, PotionEffectType.SPEED, 8, 1);
                    }
                }
                default -> {
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        if (!counts(player)) {
            return;
        }
        PlayerProfile profile = plugin.store().cached(player.getUniqueId());
        if (profile == null) {
            return;
        }

        plugin.xp().award(player, Skill.FISHING, 26);

        PerkEngine fx = fx();
        Location at = event.getCaught() != null
                ? event.getCaught().getLocation()
                : player.getLocation();

        for (Perk perk : Perk.of(Skill.FISHING)) {
            double value = profile.perkValue(perk);
            if (value <= 0) {
                continue;
            }
            switch (perk.effect()) {
                case TREASURE -> {
                    if (fx.roll(value)) {
                        fx.dropTreasure(player, at);
                    }
                }
                case WATER_BREATH -> {
                    if (fx.roll(value)) {
                        fx.potion(player, PotionEffectType.WATER_BREATHING, 30, 0);
                    }
                }
                case FOOD -> {
                    if (fx.roll(value)) {
                        fx.feed(player, 2);
                    }
                }
                case REGEN -> {
                    if (fx.roll(value)) {
                        fx.potion(player, PotionEffectType.REGENERATION, 6, 0);
                    }
                }
                case SPEED -> {
                    if (fx.roll(value)) {
                        fx.potion(player, PotionEffectType.SPEED, 8, 1);
                    }
                }
                case LOOT_CHEST -> {
                    if (fx.roll(value)) {
                        fx.lootChest(player, player.getLocation(), perk);
                    }
                }
                case FISH_FRENZY -> {
                    if (fx.roll(value)) {
                        fx.fishFrenzy(player, at);
                    }
                }
                default -> {
                }
            }
        }
    }
}
