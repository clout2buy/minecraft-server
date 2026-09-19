package com.pallarium.endgame.mob;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.item.ItemTier;
import com.pallarium.endgame.item.Tooltip;
import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The death spout. Instead of loot plopping on the floor, an elite erupts:
 * items launch straight up in a water-spout column, hang at the top, then drift
 * down trailing a coloured beam sized to how good the item is.
 */
public final class LootFountain {

    private LootFountain() {
    }

    /* ------------------------------------------------------------------ */
    /*  the spout                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Erupts loot out of a dying mob. Items fire up a column, hover, then settle,
     * each carrying a rarity beam and glow for as long as it sits on the ground.
     */
    public static void erupt(EndgamePlugin plugin, Location at, List<ItemStack> loot,
                             EliteTier tier, Player killer) {
        World w = at.getWorld();
        if (w == null || loot.isEmpty()) {
            return;
        }
        Location origin = at.clone().add(0, 0.6, 0);

        // the geyser itself
        w.playSound(origin, Sound.ENTITY_GENERIC_SPLASH, 1.2f, 0.6f);
        w.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 0.9f, 1.6f);
        w.spawnParticle(Particle.EXPLOSION_NORMAL, origin, 6, 0.2, 0.1, 0.2, 0.02);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > 26) {
                    cancel();
                    return;
                }
                double h = t * 0.28;
                for (int i = 0; i < 10; i++) {
                    double a = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
                    double r = 0.25 + ThreadLocalRandom.current().nextDouble() * 0.45;
                    Location p = origin.clone().add(Math.cos(a) * r, h, Math.sin(a) * r);
                    w.spawnParticle(Particle.WATER_SPLASH, p, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.02, 0.02, 0.02, 0.005);
                }
                if (t % 4 == 0) {
                    w.spawnParticle(Particle.DUST_COLOR_TRANSITION,
                            origin.clone().add(0, h, 0), 6, 0.3, 0.1, 0.3, 0,
                            new Particle.DustTransition(
                                    Color.fromRGB(34, 211, 238),
                                    tierColor(tier), 1.6f));
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // launch the items on a short stagger so they arc out one by one
        int delay = 0;
        for (ItemStack stack : loot) {
            final ItemStack copy = stack.clone();
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> launch(plugin, origin, copy, killer), delay);
            delay += 3;
        }
    }

    /** Fires one item up out of the spout and attaches its rarity beam. */
    private static void launch(EndgamePlugin plugin, Location origin,
                               ItemStack stack, Player killer) {
        World w = origin.getWorld();
        if (w == null) {
            return;
        }
        ItemTier rarity = ItemTier.of(stack);
        // materials drop exactly as vanilla; only equipment earns a card
        ItemStack tagged = Tooltip.isEquipment(stack)
                ? Tooltip.apply(plugin, stack, rarity, Tooltip.flavourFor(stack))
                : stack;

        boolean gear = Tooltip.isEquipment(stack);
        Item drop = w.dropItem(origin.clone().add(0, 0.8, 0), tagged);
        drop.setPickupDelay(30);
        // only gear glows. an ore or a bone lighting up read as loot it was not.
        drop.setGlowing(gear);
        drop.setUnlimitedLifetime(false);
        if (killer != null && rarity.ordinal() >= ItemTier.EPIC.ordinal()) {
            // good stuff belongs to whoever earned it, briefly
            drop.setOwner(killer.getUniqueId());
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        // straight up, hard, with a small outward lean so they fan out
        double lift = 0.85 + rarity.ordinal() * 0.07 + rng.nextDouble() * 0.15;
        double lean = 0.06 + rng.nextDouble() * 0.10;
        double ang = rng.nextDouble() * Math.PI * 2;
        drop.setVelocity(new Vector(Math.cos(ang) * lean, lift, Math.sin(ang) * lean));

        w.playSound(origin, rarity.ordinal() >= ItemTier.LEGENDARY.ordinal()
                        ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                0.7f, rarity.pitch());

        if (killer != null && rarity.ordinal() >= ItemTier.LEGENDARY.ordinal()) {
            killer.sendMessage(Component.text("  " + rarity.label() + " drop  ", rarity.color())
                    .append(Component.text(Tooltip.pretty(stack.getType()), Icon.TEXT))
                    .decoration(TextDecoration.ITALIC, false));
            killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE,
                    0.8f, 1.2f);
        }

        if (gear) {
            trail(plugin, drop, rarity);
        }
    }

    /**
     * Follows a dropped item with its beam and particle signature until it is
     * picked up or despawns.
     */
    private static void trail(EndgamePlugin plugin, Item drop, ItemTier rarity) {
        if (rarity == ItemTier.COMMON) {
            return;
        }
        final Particle.DustOptions dust = new Particle.DustOptions(rarity.dust(),
                1.0f + rarity.ordinal() * 0.2f);
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (drop == null || !drop.isValid() || drop.isDead() || t > 1200) {
                    cancel();
                    return;
                }
                World w = drop.getWorld();
                Location l = drop.getLocation().add(0, 0.2, 0);

                // the vertical beam, height scales with rarity
                int h = rarity.beam();
                for (int i = 0; i < h; i++) {
                    double y = i * 0.5;
                    double wob = Math.sin((t + i * 3) * 0.15) * 0.06;
                    w.spawnParticle(Particle.REDSTONE,
                            l.clone().add(wob, y, wob), 1, 0, 0, 0, 0, dust);
                }
                // the signature sparkle
                if (t % 4 == 0) {
                    w.spawnParticle(rarity.particle(), l.clone().add(0, 0.3, 0),
                            rarity.ordinal(), 0.15, 0.15, 0.15, 0.01);
                }
                // mythic and legendary get a slow halo
                if (rarity.ordinal() >= ItemTier.LEGENDARY.ordinal() && t % 2 == 0) {
                    double a = t * 0.2;
                    for (int k = 0; k < 3; k++) {
                        double aa = a + (Math.PI * 2 / 3) * k;
                        w.spawnParticle(Particle.REDSTONE,
                                l.clone().add(Math.cos(aa) * 0.6, 0.5, Math.sin(aa) * 0.6),
                                1, 0, 0, 0, 0, dust);
                    }
                }
                t += 2;
            }
        }.runTaskTimer(plugin, 5L, 2L);
    }

    /* ------------------------------------------------------------------ */
    /*  item dressing                                                      */
    /* ------------------------------------------------------------------ */

    private static String pretty(Material m) {
        String[] parts = m.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private static Color tierColor(EliteTier tier) {
        return switch (tier) {
            case TOUCHED -> Color.fromRGB(156, 163, 175);
            case HAUNTED -> Color.fromRGB(74, 222, 128);
            case SAVAGE -> Color.fromRGB(34, 211, 238);
            case ELITE -> Color.fromRGB(167, 139, 250);
            case CHAMPION -> Color.fromRGB(250, 204, 21);
            case NIGHTMARE -> Color.fromRGB(248, 113, 113);
        };
    }
}
