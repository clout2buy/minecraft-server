package com.pallarium.endgame.boss;

import com.pallarium.endgame.EndgamePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The actual implementations. Every method here telegraphs first, then checks
 * a real hitbox against the same geometry it drew, then applies the effect.
 * Nothing in here damages a player who was standing outside the shape.
 */
public final class Mechanics {

    private Mechanics() {
    }

    private static final Color WARN_RED = Color.fromRGB(220, 38, 38);
    private static final Color WARN_ORANGE = Color.fromRGB(249, 115, 22);
    private static final Color WARN_PURPLE = Color.fromRGB(168, 85, 247);
    private static final Color WARN_CYAN = Color.fromRGB(34, 211, 238);

    /** Dispatch. Returns false if the ability could not run right now. */
    public static boolean fire(EndgamePlugin plugin, BossInstance inst, Ability ability) {
        LivingEntity boss = inst.entity();
        if (boss == null || !boss.isValid()) {
            return false;
        }
        List<Player> targets = inst.playersInRange();
        if (targets.isEmpty()) {
            return false;
        }
        switch (ability) {
            case SHOCKWAVE -> shockwave(plugin, inst);
            case METEOR_RAIN -> meteorRain(plugin, inst, targets);
            case FLAME_CONE -> flameCone(plugin, inst);
            case CLEAVE_ARC -> cleaveArc(plugin, inst);
            case DEATH_CROSS -> deathCross(plugin, inst);
            case SPIRAL_LASH -> spiralLash(plugin, inst);
            case SAFE_DONUT -> collapse(plugin, inst);
            case KILL_ZONE -> killZone(plugin, inst);
            case PILLAR_SLAM -> pillarSlam(plugin, inst, targets);
            case RIFT_LINES -> riftLines(plugin, inst);
            case VOID_PULL -> voidPull(plugin, inst);
            case REPULSE -> repulse(plugin, inst);
            case ROOT_SNARE -> rootSnare(plugin, inst, targets);
            case BLINK_STRIKE -> blinkStrike(plugin, inst, targets);
            case GRAVITY_WELL -> gravityWell(plugin, inst, targets);
            case BARRAGE -> barrage(plugin, inst, targets);
            case SKYFALL -> skyfall(plugin, inst);
            case SEEKER_ORB -> seekerOrb(plugin, inst, targets);
            case SUMMON_ADDS -> summonAdds(plugin, inst);
            case SUMMON_ELITE -> summonElite(plugin, inst);
            case TOTEM_DROP -> totems(plugin, inst);
            case SHIELD_UP -> shieldUp(plugin, inst);
            case ENRAGE -> enrage(plugin, inst);
            case DRAIN_LIFE -> drain(plugin, inst);
            case CURSE_MARK -> curse(plugin, inst, targets);
            case ARENA_CAGE -> cage(plugin, inst);
            case BLACKOUT -> blackout(plugin, inst);
            case QUAKE -> quake(plugin, inst);
        }
        return true;
    }

    /* ================================================================== */
    /*  ground shapes                                                      */
    /* ================================================================== */

    /** Ring that expands outward from the boss. You move or you eat it. */
    private static void shockwave(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Location origin = boss.getLocation().clone();
        inst.say("braces and slams the ground");
        w.playSound(origin, Sound.ENTITY_RAVAGER_ROAR, 1.4f, 0.6f);

        new BukkitRunnable() {
            double r = 1.5;

            @Override
            public void run() {
                if (r > inst.def().arenaRadius() || !inst.alive()) {
                    cancel();
                    return;
                }
                List<Location> band = Shapes.ring(origin, r, (int) (r * 8));
                Telegraph.flash(w, band, WARN_ORANGE, 1.3f);
                w.spawnParticle(Particle.EXPLOSION_NORMAL, origin.clone().add(0, 0.2, 0), 2,
                        r * 0.3, 0.1, r * 0.3, 0.01);
                for (Player p : inst.playersInRange()) {
                    if (Shapes.inDonut(origin, p.getLocation(), r - 1.1, r + 0.5)
                            && p.getLocation().getY() - origin.getY() < 1.4) {
                        inst.hit(p, inst.scaled(9.0));
                        p.setVelocity(p.getLocation().toVector()
                                .subtract(origin.toVector()).setY(0).normalize()
                                .multiply(0.9).setY(0.45));
                    }
                }
                w.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.8f);
                r += 1.6;
            }
        }.runTaskTimer(plugin, 10L, 2L);
    }

    /** Circles mark under every player, then meteors land on them. */
    private static void meteorRain(EndgamePlugin plugin, BossInstance inst, List<Player> targets) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        inst.say("calls down the sky");
        w.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.5f);

        int waves = 3;
        for (int wave = 0; wave < waves; wave++) {
            final int delay = wave * 22;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!inst.alive()) {
                    return;
                }
                for (Player p : inst.playersInRange()) {
                    Location mark = p.getLocation().clone();
                    Telegraph.warn(plugin, w, Shapes.circle(mark, 3.0, 5), WARN_RED, 26, v -> {
                        w.spawnParticle(Particle.EXPLOSION_LARGE, mark.clone().add(0, 0.5, 0),
                                4, 0.8, 0.3, 0.8, 0);
                        w.playSound(mark, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.9f);
                        Telegraph.pillar(w, mark, Particle.FLAME, 4, 8);
                        for (Player t : inst.playersInRange()) {
                            if (Shapes.inCircle(mark, t.getLocation(), 3.0)) {
                                inst.hit(t, inst.scaled(13.0));
                                t.setFireTicks(60);
                            }
                        }
                    });
                }
            }, delay);
        }
    }

    /** Wide cone breath in the direction the boss faces. */
    private static void flameCone(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        inst.say("inhales");

        // shape tracks the boss while it charges, so it aims at you
        Telegraph.warn(plugin, w,
                () -> Shapes.cone(boss.getLocation(), boss.getLocation().getDirection(),
                        12, 70, 3),
                WARN_ORANGE, 30, v -> {
                    if (!inst.alive()) {
                        return;
                    }
                    Location o = boss.getLocation();
                    Vector dir = o.getDirection();
                    List<Location> shape = Shapes.cone(o, dir, 12, 70, 4);
                    Telegraph.flash(w, shape, Particle.FLAME, 2);
                    w.playSound(o, Sound.ENTITY_BLAZE_SHOOT, 1.6f, 0.6f);
                    w.playSound(o, Sound.ITEM_FIRECHARGE_USE, 1.4f, 0.7f);
                    for (Player p : inst.playersInRange()) {
                        if (Shapes.inCone(o, dir, p.getLocation(), 12, 70)) {
                            inst.hit(p, inst.scaled(16.0));
                            p.setFireTicks(100);
                        }
                    }
                });
    }

    /** Fast short arc, the boss's bread and butter melee. */
    private static void cleaveArc(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Telegraph.warn(plugin, w,
                () -> Shapes.cone(boss.getLocation(), boss.getLocation().getDirection(),
                        5.5, 110, 4),
                WARN_RED, 14, v -> {
                    if (!inst.alive()) {
                        return;
                    }
                    Location o = boss.getLocation();
                    Vector dir = o.getDirection();
                    Telegraph.flash(w, Shapes.cone(o, dir, 5.5, 110, 5),
                            Color.fromRGB(255, 80, 80), 1.2f);
                    w.playSound(o, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.7f);
                    for (Player p : inst.playersInRange()) {
                        if (Shapes.inCone(o, dir, p.getLocation(), 5.5, 110)) {
                            inst.hit(p, inst.scaled(12.0));
                            p.setVelocity(dir.clone().setY(0.35).multiply(1.1));
                        }
                    }
                });
    }

    /** Four beams that rotate. Players have to read the gaps. */
    private static void deathCross(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Location origin = boss.getLocation().clone();
        inst.say("splits the air into four");
        final double[] rot = {ThreadLocalRandom.current().nextDouble(Math.PI)};

        Telegraph.warn(plugin, w,
                () -> Shapes.cross(origin, inst.def().arenaRadius(), 2.2, rot[0]),
                WARN_PURPLE, 34, v -> {
                    if (!inst.alive()) {
                        return;
                    }
                    new BukkitRunnable() {
                        int t = 0;

                        @Override
                        public void run() {
                            if (t > 50 || !inst.alive()) {
                                cancel();
                                return;
                            }
                            rot[0] += 0.05;
                            List<Location> shape = Shapes.cross(
                                    origin, inst.def().arenaRadius(), 2.2, rot[0]);
                            Telegraph.flash(w, shape, WARN_PURPLE, 1.3f);
                            for (Player p : inst.playersInRange()) {
                                if (Shapes.inStar(origin, p.getLocation(), 4,
                                        inst.def().arenaRadius(), 2.2, rot[0])) {
                                    inst.hit(p, inst.scaled(4.0));
                                }
                            }
                            if (t % 8 == 0) {
                                w.playSound(origin, Sound.BLOCK_BEACON_AMBIENT, 1.2f, 1.7f);
                            }
                            t++;
                        }
                    }.runTaskTimer(plugin, 0L, 2L);
                });
    }

    /** A spiral arm sweeps the arena. Beautiful and lethal. */
    private static void spiralLash(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Location origin = boss.getLocation().clone();
        inst.say("unwinds a spiral");
        double radius = inst.def().arenaRadius();

        new BukkitRunnable() {
            double phase = 0;
            int t = 0;

            @Override
            public void run() {
                if (t > 70 || !inst.alive()) {
                    cancel();
                    return;
                }
                phase += 0.22;
                List<Location> arm = Shapes.spiral(origin, radius, 1.6, phase, 70);
                Telegraph.flash(w, arm, WARN_CYAN, 1.2f);
                for (Player p : inst.playersInRange()) {
                    for (Location l : arm) {
                        if (l.distanceSquared(p.getLocation()) < 2.0) {
                            inst.hit(p, inst.scaled(5.0));
                            break;
                        }
                    }
                }
                if (t % 10 == 0) {
                    w.playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, 0.8f);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** Everything burns except a small ring at the boss's feet. Run in. */
    private static void collapse(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Location origin = boss.getLocation().clone();
        double safe = 4.0;
        double outer = inst.def().arenaRadius();
        inst.say("collapses the floor. GET CLOSE");
        inst.title("COLLAPSE", "get close to the boss", WARN_RED);

        Telegraph.warn(plugin, w, Shapes.donut(origin, safe, outer, 3), WARN_RED, 50, v -> {
            if (!inst.alive()) {
                return;
            }
            Telegraph.flash(w, Shapes.donut(origin, safe, outer, 4),
                    Particle.EXPLOSION_NORMAL, 1);
            w.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.5f);
            for (Player p : inst.playersInRange()) {
                if (Shapes.inDonut(origin, p.getLocation(), safe, outer + 8)) {
                    inst.hit(p, inst.scaled(24.0));
                }
            }
        });
    }

    /** Inverse of collapse: one small disc somewhere is the only safe tile. */
    private static void killZone(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Location origin = boss.getLocation().clone();
        double r = inst.def().arenaRadius();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double a = rng.nextDouble(Math.PI * 2);
        double d = rng.nextDouble(r * 0.35, r * 0.8);
        Location safe = origin.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);

        inst.say("leaves one way out");
        inst.title("ONE SAFE SPOT", "find the green ring", Icon2.GREEN);

        // green safe marker drawn the whole windup
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > 60 || !inst.alive()) {
                    cancel();
                    return;
                }
                Telegraph.flash(w, Shapes.ring(safe, 3.0, 26),
                        Color.fromRGB(74, 222, 128), 1.5f);
                t += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        Telegraph.warn(plugin, w, Shapes.donut(origin, 0.5, r, 2), WARN_RED, 60, v -> {
            if (!inst.alive()) {
                return;
            }
            w.playSound(origin, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.6f);
            Telegraph.flash(w, Shapes.circle(origin, r, 3), Particle.SOUL_FIRE_FLAME, 1);
            for (Player p : inst.playersInRange()) {
                if (!Shapes.inCircle(safe, p.getLocation(), 3.2)) {
                    inst.hit(p, inst.scaled(26.0));
                } else {
                    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.8f);
                }
            }
        });
    }

    /** Boss leaps at a target and lands with a crater. */
    private static void pillarSlam(EndgamePlugin plugin, BossInstance inst, List<Player> targets) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Player target = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        Location dest = target.getLocation().clone();
        inst.say("leaps");
        w.playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_STEP, 1.5f, 0.6f);

        boss.setVelocity(dest.toVector().subtract(boss.getLocation().toVector())
                .normalize().multiply(0.8).setY(1.1));

        Telegraph.warn(plugin, w, Shapes.circle(dest, 5.0, 5), WARN_ORANGE, 30, v -> {
            if (!inst.alive()) {
                return;
            }
            boss.teleport(dest);
            w.spawnParticle(Particle.EXPLOSION_LARGE, dest, 6, 1.2, 0.3, 1.2, 0);
            w.playSound(dest, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.6f);
            w.playSound(dest, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
            for (Player p : inst.playersInRange()) {
                if (Shapes.inCircle(dest, p.getLocation(), 5.0)) {
                    inst.hit(p, inst.scaled(18.0));
                    p.setVelocity(p.getLocation().toVector().subtract(dest.toVector())
                            .setY(0).normalize().multiply(1.2).setY(0.6));
                }
            }
        });
    }

    /** Parallel beams sweeping across the arena in one direction. */
    private static void riftLines(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Location origin = boss.getLocation().clone();
        double r = inst.def().arenaRadius();
        double baseAngle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        Vector dir = new Vector(Math.cos(baseAngle), 0, Math.sin(baseAngle));
        Vector side = new Vector(-dir.getZ(), 0, dir.getX());
        inst.say("tears the ground open");

        for (int i = 0; i < 5; i++) {
            final int idx = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!inst.alive()) {
                    return;
                }
                double off = -r + (r * 2 / 4.0) * idx;
                Location start = origin.clone()
                        .add(side.clone().multiply(off))
                        .add(dir.clone().multiply(-r));
                List<Location> shape = Shapes.line(start, dir, r * 2, 2.0, 3);
                Telegraph.warn(plugin, w, shape, WARN_CYAN, 20, v -> {
                    Telegraph.flash(w, shape, Particle.SOUL_FIRE_FLAME, 1);
                    w.playSound(start, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.6f);
                    for (Player p : inst.playersInRange()) {
                        if (Shapes.inLine(start, dir, p.getLocation(), r * 2, 2.0)) {
                            inst.hit(p, inst.scaled(11.0));
                        }
                    }
                });
            }, idx * 14L);
        }
    }

    /* ================================================================== */
    /*  movement and control                                               */
    /* ================================================================== */

    private static void voidPull(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Location origin = boss.getLocation().clone();
        inst.say("pulls everything inward");
        inst.title("VOID PULL", "you are being dragged in", WARN_PURPLE);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > 40 || !inst.alive()) {
                    cancel();
                    if (inst.alive()) {
                        // the payoff: everything close detonates
                        Telegraph.flash(w, Shapes.sphere(origin.clone().add(0, 1, 0), 6, 80),
                                WARN_PURPLE, 1.6f);
                        w.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.5f);
                        for (Player p : inst.playersInRange()) {
                            if (Shapes.inCircle(origin, p.getLocation(), 7)) {
                                inst.hit(p, inst.scaled(20.0));
                                p.setVelocity(p.getLocation().toVector()
                                        .subtract(origin.toVector()).setY(0.6).normalize()
                                        .multiply(1.4));
                            }
                        }
                    }
                    return;
                }
                Telegraph.flash(w, Shapes.spiral(origin, 12, 2, t * 0.3, 50),
                        WARN_PURPLE, 1.1f);
                for (Player p : inst.playersInRange()) {
                    Vector pull = origin.toVector().subtract(p.getLocation().toVector())
                            .setY(0);
                    if (pull.lengthSquared() > 1) {
                        p.setVelocity(p.getVelocity().add(pull.normalize().multiply(0.28)));
                    }
                }
                if (t % 8 == 0) {
                    w.playSound(origin, Sound.BLOCK_PORTAL_AMBIENT, 1.3f, 0.5f);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void repulse(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Location origin = boss.getLocation().clone();
        Telegraph.warn(plugin, w, Shapes.circle(origin, 9, 4), WARN_CYAN, 18, v -> {
            if (!inst.alive()) {
                return;
            }
            w.playSound(origin, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.4f, 1.3f);
            Telegraph.flash(w, Shapes.ring(origin, 9, 50), WARN_CYAN, 1.5f);
            for (Player p : inst.playersInRange()) {
                if (Shapes.inCircle(origin, p.getLocation(), 9)) {
                    inst.hit(p, inst.scaled(7.0));
                    p.setVelocity(p.getLocation().toVector().subtract(origin.toVector())
                            .setY(0).normalize().multiply(1.9).setY(0.75));
                }
            }
        });
    }

    private static void rootSnare(EndgamePlugin plugin, BossInstance inst, List<Player> targets) {
        World w = inst.entity().getWorld();
        inst.say("binds you where you stand");
        for (Player p : targets) {
            if (p.getLocation().distance(inst.entity().getLocation()) > 14) {
                continue;
            }
            Location at = p.getLocation().clone();
            Telegraph.flash(w, Shapes.ring(at, 1.2, 14), Color.fromRGB(120, 200, 120), 1.3f);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 44, 6, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 44, 128, false, false));
            p.playSound(at, Sound.BLOCK_SWEET_BERRY_BUSH_PLACE, 1.2f, 0.7f);
        }
    }

    private static void blinkStrike(EndgamePlugin plugin, BossInstance inst, List<Player> targets) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Player target = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        Location from = boss.getLocation().clone();
        Telegraph.flash(w, Shapes.sphere(from.clone().add(0, 1, 0), 1.2, 30),
                WARN_PURPLE, 1.3f);
        w.playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1.3f, 0.7f);

        Location behind = target.getLocation().clone()
                .subtract(target.getLocation().getDirection().setY(0).normalize().multiply(2));
        behind.setY(target.getLocation().getY());
        boss.teleport(behind);
        if (boss instanceof Mob mob) {
            mob.setTarget(target);
        }
        w.playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1.3f, 0.7f);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!inst.alive() || !target.isValid()) {
                return;
            }
            if (target.getLocation().distance(boss.getLocation()) < 4.5) {
                inst.hit(target, inst.scaled(15.0));
                w.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.4f, 0.8f);
            }
        }, 8L);
    }

    private static void gravityWell(EndgamePlugin plugin, BossInstance inst, List<Player> targets) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        inst.say("lifts you off the ground");
        for (Player p : targets) {
            Location at = p.getLocation().clone();
            Telegraph.warn(plugin, w, Shapes.ring(at, 2.0, 18), WARN_PURPLE, 22, v -> {
                if (!p.isValid() || !inst.alive()) {
                    return;
                }
                if (Shapes.inCircle(at, p.getLocation(), 2.4)) {
                    p.setVelocity(new Vector(0, 1.5, 0));
                    p.playSound(p.getLocation(), Sound.ENTITY_SHULKER_TELEPORT, 1.2f, 0.6f);
                    Telegraph.pillar(w, p.getLocation(), Particle.PORTAL, 6, 10);
                }
            });
        }
    }

    /* ================================================================== */
    /*  projectiles                                                        */
    /* ================================================================== */

    private static void barrage(EndgamePlugin plugin, BossInstance inst, List<Player> targets) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        inst.say("opens fire");
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!inst.alive()) {
                    return;
                }
                Player t = targets.get(idx % targets.size());
                if (!t.isValid()) {
                    return;
                }
                Location from = boss.getEyeLocation();
                Vector dir = t.getEyeLocation().toVector()
                        .subtract(from.toVector()).normalize();
                travelBolt(plugin, inst, from, dir, 1.1, inst.scaled(8.0),
                        Particle.FLAME, Sound.ENTITY_BLAZE_SHOOT);
            }, idx * 5L);
        }
    }

    private static void skyfall(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Location origin = boss.getLocation().clone();
        double r = inst.def().arenaRadius();
        inst.say("darkens the sky");

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > 60 || !inst.alive()) {
                    cancel();
                    return;
                }
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                for (int i = 0; i < 3; i++) {
                    double a = rng.nextDouble(Math.PI * 2);
                    double d = rng.nextDouble(r);
                    Location at = origin.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                    Telegraph.flash(w, Shapes.ring(at, 1.0, 8), WARN_RED, 1.0f);
                    w.spawnParticle(Particle.CRIT, at.clone().add(0, 6, 0), 6, 0.2, 2, 0.2, 0.1);
                    for (Player p : inst.playersInRange()) {
                        if (Shapes.inCircle(at, p.getLocation(), 1.5)) {
                            inst.hit(p, inst.scaled(4.0));
                        }
                    }
                }
                if (t % 6 == 0) {
                    w.playSound(origin, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.4f);
                }
                t += 3;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    private static void seekerOrb(EndgamePlugin plugin, BossInstance inst, List<Player> targets) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Player target = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        inst.say("sends a hunter after " + target.getName());
        target.showTitle(net.kyori.adventure.title.Title.title(
                Component.text("HUNTED", TextColor.fromHexString("#A855F7")),
                Component.text("an orb is chasing you", TextColor.fromHexString("#6B7280")),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(150),
                        java.time.Duration.ofMillis(1200),
                        java.time.Duration.ofMillis(300))));

        final Location[] pos = {boss.getEyeLocation().clone()};
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > 120 || !inst.alive() || !target.isValid()) {
                    cancel();
                    return;
                }
                Vector toward = target.getEyeLocation().toVector()
                        .subtract(pos[0].toVector());
                if (toward.lengthSquared() < 1.6) {
                    Telegraph.flash(w, Shapes.sphere(pos[0], 2.5, 40), WARN_PURPLE, 1.5f);
                    w.playSound(pos[0], Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.2f);
                    for (Player p : inst.playersInRange()) {
                        if (Shapes.inCircle(pos[0], p.getLocation(), 3.0)) {
                            inst.hit(p, inst.scaled(14.0));
                        }
                    }
                    cancel();
                    return;
                }
                pos[0].add(toward.normalize().multiply(0.38));
                w.spawnParticle(Particle.DRAGON_BREATH, pos[0], 6, 0.12, 0.12, 0.12, 0.01);
                Telegraph.flash(w, Shapes.sphere(pos[0], 0.5, 10), WARN_PURPLE, 1.1f);
                if (t % 10 == 0) {
                    w.playSound(pos[0], Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.7f);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Shared straight-flying projectile with a real hit check each step. */
    private static void travelBolt(EndgamePlugin plugin, BossInstance inst, Location from,
                                   Vector dir, double speed, double damage,
                                   Particle trail, Sound fire) {
        World w = from.getWorld();
        w.playSound(from, fire, 1.1f, 1.2f);
        final Location pos = from.clone();
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > 50 || !inst.alive()) {
                    cancel();
                    return;
                }
                pos.add(dir.clone().multiply(speed));
                if (pos.getBlock().getType().isSolid()) {
                    w.spawnParticle(Particle.SMOKE_NORMAL, pos, 10, 0.2, 0.2, 0.2, 0.02);
                    cancel();
                    return;
                }
                w.spawnParticle(trail, pos, 4, 0.05, 0.05, 0.05, 0.01);
                for (Player p : inst.playersInRange()) {
                    if (p.getEyeLocation().distanceSquared(pos) < 2.2) {
                        inst.hit(p, damage);
                        w.spawnParticle(Particle.EXPLOSION_NORMAL, pos, 3, 0.2, 0.2, 0.2, 0.01);
                        cancel();
                        return;
                    }
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /* ================================================================== */
    /*  summoning                                                          */
    /* ================================================================== */

    private static void summonAdds(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        inst.say("calls for help");
        EntityType[] pool = inst.def().minions();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int count = 4 + rng.nextInt(3);

        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2 / count) * i;
            Location at = boss.getLocation().clone().add(Math.cos(a) * 4, 0, Math.sin(a) * 4);
            Telegraph.warn(plugin, w, Shapes.ring(at, 1.0, 12), WARN_PURPLE, 20, v -> {
                if (!inst.alive()) {
                    return;
                }
                Telegraph.pillar(w, at, Particle.SOUL, 3, 12);
                w.playSound(at, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.2f, 0.8f);
                Entity e = w.spawnEntity(at, pool[rng.nextInt(pool.length)]);
                if (e instanceof LivingEntity le) {
                    le.setRemoveWhenFarAway(false);
                    inst.trackMinion(le);
                    if (le instanceof Mob m && !inst.playersInRange().isEmpty()) {
                        m.setTarget(inst.playersInRange().get(0));
                    }
                }
            });
        }
    }

    private static void summonElite(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        inst.say("summons a champion");
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        EntityType[] pool = inst.def().minions();
        Location at = boss.getLocation().clone().add(
                rng.nextDouble(-4, 4), 0, rng.nextDouble(-4, 4));

        Telegraph.warn(plugin, w, Shapes.ring(at, 2.0, 20), WARN_RED, 30, v -> {
            if (!inst.alive()) {
                return;
            }
            Telegraph.pillar(w, at, Particle.SOUL_FIRE_FLAME, 4, 18);
            w.playSound(at, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.3f);
            Entity e = w.spawnEntity(at, pool[rng.nextInt(pool.length)]);
            if (e instanceof LivingEntity le) {
                plugin.elites().promote(le,
                        com.pallarium.endgame.mob.EliteTier.CHAMPION);
                inst.trackMinion(le);
            }
        });
    }

    /** Totems the party must break, or the boss heals off them. */
    private static void totems(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        inst.say("raises healing totems");
        inst.title("TOTEMS UP", "break them or it heals", WARN_ORANGE);

        List<LivingEntity> totems = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double a = (Math.PI * 2 / 4) * i;
            Location at = boss.getLocation().clone().add(Math.cos(a) * 7, 0, Math.sin(a) * 7);
            Entity e = w.spawnEntity(at, EntityType.ARMOR_STAND);
            if (e instanceof org.bukkit.entity.ArmorStand stand) {
                stand.setInvulnerable(false);
                stand.setBasePlate(false);
                stand.setArms(false);
                stand.setGravity(false);
                stand.setCustomNameVisible(true);
                stand.customName(Component.text("\u25C6 Totem",
                        TextColor.fromHexString("#FACC15")));
                stand.getEquipment().setHelmet(
                        new org.bukkit.inventory.ItemStack(Material.SOUL_LANTERN));
                stand.setMaxHealth(40);
                stand.setHealth(40);
                totems.add(stand);
                inst.trackMinion(stand);
            }
        }

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                totems.removeIf(e -> !e.isValid() || e.isDead());
                if (t > 300 || !inst.alive() || totems.isEmpty()) {
                    for (LivingEntity e : totems) {
                        if (e.isValid()) {
                            e.remove();
                        }
                    }
                    if (inst.alive() && totems.isEmpty()) {
                        inst.say("screams as its totems shatter");
                        inst.entity().getWorld().playSound(inst.entity().getLocation(),
                                Sound.ENTITY_ELDER_GUARDIAN_HURT, 1.4f, 0.7f);
                    }
                    cancel();
                    return;
                }
                LivingEntity b = inst.entity();
                for (LivingEntity totem : totems) {
                    // beam from totem to boss, and it heals
                    Vector step = b.getLocation().toVector()
                            .subtract(totem.getLocation().toVector());
                    double len = step.length();
                    Vector unit = step.normalize();
                    for (double d = 0; d < len; d += 0.6) {
                        Location l = totem.getLocation().clone()
                                .add(unit.clone().multiply(d)).add(0, 1.2, 0);
                        w.spawnParticle(Particle.HEART, l, 1, 0, 0, 0, 0);
                    }
                }
                double heal = totems.size() * 4.0;
                double max = b.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                        ? b.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20;
                b.setHealth(Math.min(max, b.getHealth() + heal));
                t += 20;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /* ================================================================== */
    /*  defensive                                                          */
    /* ================================================================== */

    private static void shieldUp(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        inst.shield(120.0);
        inst.say("raises a barrier");
        inst.title("BARRIER", "break 120 damage through it", WARN_CYAN);
        w.playSound(boss.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 0.8f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!inst.alive() || !inst.shielded() || t > 400) {
                    if (inst.alive() && inst.shielded()) {
                        inst.clearShield();
                    }
                    cancel();
                    return;
                }
                Telegraph.flash(w, Shapes.dome(boss.getLocation(), 3.0, 40),
                        WARN_CYAN, 1.0f);
                t += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private static void enrage(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        inst.say("goes berserk");
        inst.title("ENRAGED", "back off for a moment", WARN_RED);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 160, 1, false, true));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1, false, true));
        w.playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.6f, 0.7f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > 160 || !inst.alive()) {
                    cancel();
                    return;
                }
                w.spawnParticle(Particle.FLAME, boss.getLocation().add(0, 1, 0),
                        8, 0.4, 0.6, 0.4, 0.02);
                t += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private static void drain(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        inst.say("drinks your life");
        Telegraph.warn(plugin, w, Shapes.circle(boss.getLocation(), 10, 3),
                Color.fromRGB(139, 0, 0), 26, v -> {
                    if (!inst.alive()) {
                        return;
                    }
                    double healed = 0;
                    for (Player p : inst.playersInRange()) {
                        if (Shapes.inCircle(boss.getLocation(), p.getLocation(), 10)) {
                            inst.hit(p, inst.scaled(8.0));
                            healed += 6;
                            Vector step = boss.getLocation().toVector()
                                    .subtract(p.getLocation().toVector());
                            double len = step.length();
                            Vector unit = step.normalize();
                            for (double d = 0; d < len; d += 0.5) {
                                w.spawnParticle(Particle.DAMAGE_INDICATOR,
                                        p.getLocation().clone().add(unit.clone().multiply(d))
                                                .add(0, 1, 0), 1, 0, 0, 0, 0);
                            }
                        }
                    }
                    double max = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                            ? boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20;
                    boss.setHealth(Math.min(max, boss.getHealth() + healed));
                    w.playSound(boss.getLocation(), Sound.ENTITY_WITCH_DRINK, 1.3f, 0.6f);
                });
    }

    /** Marks a player, then blows up on wherever they are standing. */
    private static void curse(EndgamePlugin plugin, BossInstance inst, List<Player> targets) {
        World w = inst.entity().getWorld();
        Player target = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        inst.say("marks " + target.getName());
        target.showTitle(net.kyori.adventure.title.Title.title(
                Component.text("MARKED", TextColor.fromHexString("#F87171")),
                Component.text("get away from the group", TextColor.fromHexString("#6B7280")),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(120),
                        java.time.Duration.ofMillis(1600),
                        java.time.Duration.ofMillis(300))));

        // the mark follows the player for the whole windup
        Telegraph.warn(plugin, w, () -> Shapes.circle(target.getLocation(), 5.0, 4),
                WARN_RED, 60, v -> {
                    if (!inst.alive() || !target.isValid()) {
                        return;
                    }
                    Location at = target.getLocation().clone();
                    Telegraph.flash(w, Shapes.sphere(at.clone().add(0, 1, 0), 5, 70),
                            WARN_RED, 1.5f);
                    w.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                    for (Player p : inst.playersInRange()) {
                        if (Shapes.inCircle(at, p.getLocation(), 5.0)) {
                            inst.hit(p, inst.scaled(17.0));
                        }
                    }
                });
    }

    /* ================================================================== */
    /*  arena                                                              */
    /* ================================================================== */

    private static void cage(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Location origin = boss.getLocation().clone();
        double r = inst.def().arenaRadius();
        inst.say("seals the arena");
        inst.title("SEALED", "no way out until it falls", WARN_PURPLE);
        w.playSound(origin, Sound.BLOCK_END_PORTAL_SPAWN, 1.2f, 1.4f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!inst.alive() || t > 1200) {
                    cancel();
                    return;
                }
                Telegraph.flash(w, Shapes.wall(origin, r, 4, 40), WARN_PURPLE, 1.0f);
                for (Player p : inst.playersInRange()) {
                    double d = p.getLocation().toVector().setY(0)
                            .distance(origin.toVector().setY(0));
                    if (d > r - 0.5) {
                        p.setVelocity(origin.toVector().subtract(p.getLocation().toVector())
                                .setY(0).normalize().multiply(0.9));
                        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.8f);
                    }
                }
                t += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private static void blackout(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        inst.say("snuffs out the light");
        for (Player p : inst.playersInRange()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 70, 0, false, false));
            p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1.4f, 0.5f);
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.3f, 0.5f);

        // it moves while you cannot see
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!inst.alive()) {
                return;
            }
            List<Player> ps = inst.playersInRange();
            if (ps.isEmpty()) {
                return;
            }
            Player t = ps.get(ThreadLocalRandom.current().nextInt(ps.size()));
            boss.teleport(t.getLocation().clone().add(
                    ThreadLocalRandom.current().nextDouble(-3, 3), 0,
                    ThreadLocalRandom.current().nextDouble(-3, 3)));
        }, 40L);
    }

    private static void quake(EndgamePlugin plugin, BossInstance inst) {
        LivingEntity boss = inst.entity();
        World w = boss.getWorld();
        Location origin = boss.getLocation().clone();
        inst.say("shakes the earth");

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > 100 || !inst.alive()) {
                    cancel();
                    return;
                }
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                for (int i = 0; i < 8; i++) {
                    double a = rng.nextDouble(Math.PI * 2);
                    double d = rng.nextDouble(inst.def().arenaRadius());
                    w.spawnParticle(Particle.BLOCK_CRACK,
                            origin.clone().add(Math.cos(a) * d, 0.2, Math.sin(a) * d),
                            6, 0.3, 0.1, 0.3, 0.1,
                            Material.DEEPSLATE.createBlockData());
                }
                for (Player p : inst.playersInRange()) {
                    if (p.isOnGround() && Shapes.inCircle(origin, p.getLocation(),
                            inst.def().arenaRadius())) {
                        inst.hit(p, inst.scaled(2.0));
                        p.setVelocity(p.getVelocity().add(new Vector(
                                rng.nextDouble(-0.12, 0.12), 0.18,
                                rng.nextDouble(-0.12, 0.12))));
                    }
                }
                w.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.4f);
                t += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    /** Small colour holder so mechanics can reference named colours. */
    static final class Icon2 {
        static final Color GREEN = Color.fromRGB(74, 222, 128);
    }
}
