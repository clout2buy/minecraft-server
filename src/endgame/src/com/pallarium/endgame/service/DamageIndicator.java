package com.pallarium.endgame.service;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Floating damage numbers. Spawns a short lived TextDisplay beside the victim
 * that drifts upward, scales down and vanishes. Crits, kills, heals and perk
 * procs each get their own glyph and color so you can read a fight at a glance.
 */
public class DamageIndicator {

    /** Visual flavors, each with its own glyph, color and motion. */
    public enum Kind {
        NORMAL("\u2756", TextColor.fromHexString("#E5E7EB"), 0.85f, false),
        CRIT("\u2726", TextColor.fromHexString("#FACC15"), 1.25f, true),
        HEAVY("\u25C6", TextColor.fromHexString("#FB923C"), 1.10f, true),
        RANGED("\u27B3", TextColor.fromHexString("#C084FC"), 0.90f, false),
        MAGIC("\u2749", TextColor.fromHexString("#22D3EE"), 0.95f, false),
        HEAL("\u2764", TextColor.fromHexString("#4ADE80"), 0.90f, false),
        KILL("\u2620", TextColor.fromHexString("#F87171"), 1.35f, true),
        PERK("\u25C8", TextColor.fromHexString("#A78BFA"), 0.95f, false);

        private final String glyph;
        private final TextColor color;
        private final float scale;
        private final boolean bold;

        Kind(String glyph, TextColor color, float scale, boolean bold) {
            this.glyph = glyph;
            this.color = color;
            this.scale = scale;
            this.bold = bold;
        }

        public String glyph() {
            return glyph;
        }

        public TextColor color() {
            return color;
        }
    }

    private static final DecimalFormat FMT = new DecimalFormat("0.#");
    private static final int LIFETIME_TICKS = 26;

    private final EndgamePlugin plugin;

    public DamageIndicator(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    /** Damage number above a hit entity. */
    public void damage(Player viewer, LivingEntity victim, double amount, Kind kind) {
        if (!plugin.settings().damageNumbers() || amount <= 0) {
            return;
        }
        Component text = Component.text(kind.glyph() + " " + FMT.format(amount), kind.color())
                .decoration(TextDecoration.BOLD, kind.bold)
                .decoration(TextDecoration.ITALIC, false);
        spawn(victim.getLocation().add(0, victim.getHeight() * 0.85, 0), text, kind.scale);
    }

    /** Free floating label, used for perk procs and xp pops. */
    public void label(Location at, String text, TextColor color, Kind kind) {
        if (!plugin.settings().damageNumbers()) {
            return;
        }
        Component comp = Component.text(kind.glyph() + " " + text, color)
                .decoration(TextDecoration.ITALIC, false);
        spawn(at, comp, kind.scale);
    }

    /** Announces a perk firing, in that perk's own accent color. */
    public void perkProc(Location at, String perkName) {
        label(at, perkName, Icon.ACCENT, Kind.PERK);
    }

    private void spawn(Location base, Component text, float scale) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Location at = base.clone().add(
                rng.nextDouble(-0.45, 0.45), rng.nextDouble(-0.1, 0.25), rng.nextDouble(-0.45, 0.45));

        if (at.getWorld() == null) {
            return;
        }

        TextDisplay display = at.getWorld().spawn(at, TextDisplay.class, entity -> {
            entity.text(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setShadowed(true);
            entity.setSeeThrough(false);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(90, 9, 9, 11));
            entity.setViewRange(0.35f);
            entity.setPersistent(false);
            entity.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    entity.getTransformation().getLeftRotation(),
                    new Vector3f(scale, scale, scale),
                    entity.getTransformation().getRightRotation()));
        });

        animate(display, scale);
    }

    private void animate(TextDisplay display, float startScale) {
        final double driftX = ThreadLocalRandom.current().nextDouble(-0.012, 0.012);
        final double driftZ = ThreadLocalRandom.current().nextDouble(-0.012, 0.012);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!display.isValid()) {
                    cancel();
                    return;
                }
                tick++;
                if (tick >= LIFETIME_TICKS) {
                    display.remove();
                    cancel();
                    return;
                }

                double progress = (double) tick / LIFETIME_TICKS;
                double rise = 0.055 * (1.0 - progress * 0.55);
                display.teleport(display.getLocation().add(driftX, rise, driftZ));

                // pop on spawn, shrink as it fades out
                float scale;
                if (tick <= 3) {
                    scale = startScale * (1.0f + (4 - tick) * 0.12f);
                } else {
                    scale = startScale * (float) (1.0 - progress * 0.45);
                }
                Transformation current = display.getTransformation();
                display.setTransformation(new Transformation(
                        current.getTranslation(), current.getLeftRotation(),
                        new Vector3f(scale, scale, scale), current.getRightRotation()));

                if (progress > 0.6) {
                    int alpha = (int) (255 * (1.0 - (progress - 0.6) / 0.4));
                    display.setBackgroundColor(org.bukkit.Color.fromARGB(
                            Math.max(0, Math.min(90, alpha / 3)), 9, 9, 11));
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Cleans up any indicators left behind by a crash or reload. */
    public void purge() {
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof TextDisplay display && !display.isPersistent()) {
                    display.remove();
                }
            }
        }
    }
}
