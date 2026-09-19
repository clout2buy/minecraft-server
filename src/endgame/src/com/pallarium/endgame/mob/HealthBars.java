package com.pallarium.endgame.mob;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.attribute.Attribute;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Floating combat UI. Every damaged mob gets a heart bar pinned above its head
 * that tracks it, and every hit spits a damage number that arcs up and fades.
 * Both use text displays so they are client side smooth and never block hits.
 */
public class HealthBars {

    private final EndgamePlugin plugin;

    private static final String HEART_FULL = "\u2665";
    private static final String HEART_EMPTY = "\u2661";
    private static final int PIPS = 10;

    public HealthBars(EndgamePlugin plugin) {
        this.plugin = plugin;
        start();
    }

    /* ------------------------------------------------------------------ */
    /*  the follower bar                                                   */
    /* ------------------------------------------------------------------ */

    /**
     * Shows or refreshes the bar for a mob. Called on every hit, so the bar
     * appears the moment you engage and refreshes its lifetime as you fight.
     */
    public void show(LivingEntity mob) {
        if (mob instanceof Player || !mob.isValid()) {
            return;
        }
        TextDisplay bar = findBar(mob);
        if (bar == null) {
            bar = spawnBar(mob);
        }
        if (bar != null) {
            bar.text(render(mob));
            // reset the idle timer
            bar.getPersistentDataContainer().set(plugin.keyBarSeen(),
                    org.bukkit.persistence.PersistentDataType.LONG,
                    System.currentTimeMillis());
        }
    }

    private TextDisplay spawnBar(LivingEntity mob) {
        Location at = mob.getLocation();
        // riding already lifts the display to the mount point (~1.46 on a zombie),
        // so the translation only adds the last bit of clearance over the head.
        // Adding full height here stacked the two and floated the bar way up.
        final float lift = 0.6f;
        TextDisplay d = mob.getWorld().spawn(at, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(false);
            td.setShadowed(false);
            td.setDefaultBackground(false);
            td.setBackgroundColor(org.bukkit.Color.fromARGB(110, 6, 8, 14));
            td.setViewRange(0.55f);
            td.setPersistent(false);
            // a passenger snaps to the mount point, so the height has to live
            // in the transform translation or the bar sits inside the mob
            td.setTransformation(new Transformation(
                    new Vector3f(0, lift, 0), new AxisAngle4f(),
                    new Vector3f(0.8f, 0.8f, 0.8f), new AxisAngle4f()));
            td.getPersistentDataContainer().set(plugin.keyBarOwner(),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    mob.getUniqueId().toString());
        });
        // the vanilla nameplate would stack a second copy above ours
        mob.setCustomNameVisible(false);
        // ride the mob so it tracks perfectly with zero jitter
        mob.addPassenger(d);
        return d;
    }

    private TextDisplay findBar(LivingEntity mob) {
        for (Entity p : mob.getPassengers()) {
            if (p instanceof TextDisplay td && td.getPersistentDataContainer()
                    .has(plugin.keyBarOwner(),
                            org.bukkit.persistence.PersistentDataType.STRING)) {
                return td;
            }
        }
        return null;
    }

    /** Builds the heart row, coloured by how hurt the mob is. */
    private Component render(LivingEntity mob) {
        double max = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                ? mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20;
        double hp = Math.max(0, mob.getHealth());
        double frac = max <= 0 ? 0 : hp / max;
        int filled = (int) Math.ceil(frac * PIPS);
        if (hp > 0 && filled == 0) {
            filled = 1;
        }

        TextColor color = frac > 0.6 ? TextColor.fromHexString("#4ADE80")
                : frac > 0.3 ? TextColor.fromHexString("#FACC15")
                : TextColor.fromHexString("#F87171");

        StringBuilder full = new StringBuilder();
        StringBuilder empty = new StringBuilder();
        for (int i = 0; i < filled; i++) {
            full.append(HEART_FULL);
        }
        for (int i = filled; i < PIPS; i++) {
            empty.append(HEART_EMPTY);
        }

        Component hearts = Component.text(full.toString(), color)
                .append(Component.text(empty.toString(), TextColor.fromHexString("#2A2F3A")))
                .decoration(TextDecoration.ITALIC, false);

        // elites get their name and tier stacked above the hearts
        if (plugin.elites().isElite(mob)) {
            EliteTier tier = plugin.elites().tierOf(mob);
            String name = plugin.elites().nameOf(mob);
            if (name.length() > 22) {
                name = name.substring(0, 21).trim() + "…";
            }
            return Component.text(tier.glyph() + " ", tier.textColor())
                    .append(Component.text(name, tier.textColor()))
                    .append(Component.newline())
                    .append(hearts)
                    .decoration(TextDecoration.ITALIC, false);
        }
        return hearts;
    }

    /* ------------------------------------------------------------------ */
    /*  damage numbers                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * Pops a floating damage number off a mob. Bigger hits are bigger, hotter
     * and get a crit marker.
     */
    public void popDamage(LivingEntity victim, double amount, boolean crit) {
        if (amount <= 0 || !victim.isValid()) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Location at = victim.getLocation().add(
                rng.nextDouble(-0.4, 0.4),
                victim.getHeight() * 0.75 + rng.nextDouble(0, 0.3),
                rng.nextDouble(-0.4, 0.4));

        TextColor color = crit ? TextColor.fromHexString("#FACC15")
                : amount >= 12 ? TextColor.fromHexString("#F87171")
                : amount >= 6 ? TextColor.fromHexString("#FB923C")
                : TextColor.fromHexString("#E5E7EB");

        String text = (crit ? "\u2726 " : "") + fmt(amount);
        float scale = (float) Math.min(1.7, 0.75 + amount * 0.045) * (crit ? 1.25f : 1f);

        TextDisplay d = victim.getWorld().spawn(at, TextDisplay.class, td -> {
            td.text(Component.text(text, color).decoration(TextDecoration.ITALIC, false));
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(true);
            td.setShadowed(true);
            td.setDefaultBackground(false);
            td.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            td.setViewRange(0.6f);
            td.setPersistent(false);
            td.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0), new AxisAngle4f(),
                    new Vector3f(scale, scale, scale), new AxisAngle4f()));
        });

        // arc up and fade out
        final double driftX = rng.nextDouble(-0.02, 0.02);
        final double driftZ = rng.nextDouble(-0.02, 0.02);
        new org.bukkit.scheduler.BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!d.isValid() || t > 20) {
                    if (d.isValid()) {
                        d.remove();
                    }
                    cancel();
                    return;
                }
                double rise = 0.075 * (1 - t / 24.0);
                d.teleport(d.getLocation().add(driftX, rise, driftZ));
                if (t > 12) {
                    float s = scale * (1 - (t - 12) / 10.0f);
                    d.setTransformation(new Transformation(
                            new Vector3f(0, 0, 0), new AxisAngle4f(),
                            new Vector3f(Math.max(0.01f, s), Math.max(0.01f, s),
                                    Math.max(0.01f, s)), new AxisAngle4f()));
                }
                t++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private String fmt(double d) {
        return d >= 10 ? String.valueOf((int) Math.round(d))
                : String.format("%.1f", d).replace(".0", "");
    }

    /* ------------------------------------------------------------------ */
    /*  upkeep                                                             */
    /* ------------------------------------------------------------------ */

    private void start() {
        // refresh live bars and retire idle ones
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            long idle = plugin.getConfig().getLong("healthbars.hide-after-seconds", 8) * 1000L;
            for (org.bukkit.World w : plugin.getServer().getWorlds()) {
                for (Entity e : w.getEntitiesByClass(TextDisplay.class)) {
                    TextDisplay td = (TextDisplay) e;
                    String owner = td.getPersistentDataContainer().get(plugin.keyBarOwner(),
                            org.bukkit.persistence.PersistentDataType.STRING);
                    if (owner == null) {
                        continue;
                    }
                    Entity host = plugin.getServer().getEntity(
                            java.util.UUID.fromString(owner));
                    if (!(host instanceof LivingEntity le) || !le.isValid() || le.isDead()) {
                        td.remove();
                        continue;
                    }
                    Long seen = td.getPersistentDataContainer().get(plugin.keyBarSeen(),
                            org.bukkit.persistence.PersistentDataType.LONG);
                    // elites keep their plate up permanently, normals fade out
                    boolean elite = plugin.elites().isElite(le);
                    if (!elite && seen != null && now - seen > idle) {
                        td.remove();
                        continue;
                    }
                    td.text(render(le));
                }
            }
        }, 20L, 4L);
    }

    /** Elites get their bar immediately on spawn instead of on first hit. */
    public void attach(LivingEntity mob) {
        show(mob);
    }

    /** Removes a mob's bar, used on death. */
    public void clear(LivingEntity mob) {
        TextDisplay td = findBar(mob);
        if (td != null) {
            td.remove();
        }
    }
}
