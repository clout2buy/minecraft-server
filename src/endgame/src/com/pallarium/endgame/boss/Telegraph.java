package com.pallarium.endgame.boss;

import com.pallarium.endgame.EndgamePlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The thing that makes a boss fight readable instead of unfair.
 *
 * A telegraph paints a shape on the ground in a warning colour, pulses it
 * faster and faster as the windup runs out, then detonates and hands the same
 * shape to whatever code deals the damage. Players get a real window to move.
 */
public final class Telegraph {

    private Telegraph() {
    }

    /**
     * Paints {@code points} for {@code windupTicks}, then runs {@code onDetonate}.
     * The draw rate accelerates so the last half second reads as a hard warning.
     */
    public static void warn(EndgamePlugin plugin, World world, List<Location> points,
                            Color color, int windupTicks, Consumer<Void> onDetonate) {
        warn(plugin, world, () -> points, color, windupTicks, onDetonate);
    }

    /**
     * Same, but the shape is re-evaluated every frame, so a telegraph can
     * track a moving player or rotate while it charges.
     */
    public static void warn(EndgamePlugin plugin, World world,
                            java.util.function.Supplier<List<Location>> shape,
                            Color color, int windupTicks, Consumer<Void> onDetonate) {
        final Particle.DustOptions warnDust = new Particle.DustOptions(color, 1.1f);
        final Particle.DustOptions hotDust = new Particle.DustOptions(
                Color.fromRGB(255, 240, 200), 1.4f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= windupTicks) {
                    cancel();
                    if (onDetonate != null) {
                        onDetonate.accept(null);
                    }
                    return;
                }
                double progress = (double) t / windupTicks;
                // draw every 4 ticks early, every tick at the end
                int every = progress > 0.75 ? 1 : progress > 0.45 ? 2 : 4;
                if (t % every == 0) {
                    boolean hot = progress > 0.8;
                    List<Location> pts = shape.get();
                    // thin the point list out early so a huge shape is cheap
                    int stride = progress > 0.6 ? 1 : 2;
                    for (int i = 0; i < pts.size(); i += stride) {
                        Location l = pts.get(i);
                        world.spawnParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
                                hot ? hotDust : warnDust);
                    }
                    if (t % 6 == 0) {
                        world.playSound(pts.isEmpty() ? world.getSpawnLocation() : pts.get(0),
                                Sound.BLOCK_NOTE_BLOCK_HAT, 0.45f,
                                0.8f + (float) progress * 0.8f);
                    }
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Instant flash of a shape, used the moment an attack actually lands. */
    public static void flash(World world, List<Location> points, Particle particle, int per) {
        for (Location l : points) {
            world.spawnParticle(particle, l, per, 0.08, 0.08, 0.08, 0.01);
        }
    }

    /** Coloured flash for shapes that want an exact colour on impact. */
    public static void flash(World world, List<Location> points, Color color, float size) {
        Particle.DustOptions dust = new Particle.DustOptions(color, size);
        for (Location l : points) {
            world.spawnParticle(Particle.REDSTONE, l, 1, 0.05, 0.05, 0.05, 0, dust);
        }
    }

    /** Rising pillar of particles, for summons and slams. */
    public static void pillar(World world, Location at, Particle particle, double height, int per) {
        for (double y = 0; y <= height; y += 0.35) {
            world.spawnParticle(particle, at.clone().add(0, y, 0), per, 0.15, 0.05, 0.15, 0.01);
        }
    }
}
