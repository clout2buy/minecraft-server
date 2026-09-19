package com.pallarium.endgame.ui;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.mob.EliteTier;
import com.pallarium.endgame.service.Fx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Elite spawner. Top row picks the tier, the grid picks the mob. Click spawns
 * one, shift click spawns a pack of five in a loose ring in front of you.
 */
public class MobAdminMenu extends Menu {

    /** The mob types worth spawning as elites, in menu order. */
    static final EntityType[] MOBS = {
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
            EntityType.CREEPER, EntityType.ENDERMAN, EntityType.WITCH,
            EntityType.HUSK, EntityType.STRAY, EntityType.DROWNED,
            EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
            EntityType.RAVAGER, EntityType.WITHER_SKELETON, EntityType.BLAZE,
            EntityType.PIGLIN_BRUTE, EntityType.HOGLIN, EntityType.ZOGLIN,
            EntityType.PHANTOM, EntityType.CAVE_SPIDER, EntityType.SILVERFISH,
            EntityType.MAGMA_CUBE, EntityType.SLIME, EntityType.GUARDIAN,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOMBIE_VILLAGER, EntityType.VEX
    };

    private static final int[] TIER_SLOTS = {10, 11, 12, 13, 14, 15};
    private static final int[] GRID = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int RANDOM = 16;
    private static final int BACK = 49;

    private EliteTier tier = EliteTier.ELITE;
    private int page;

    public MobAdminMenu(EndgamePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Component.text("\u25C6 Elite Spawner", Icon.ACCENT)
                .decoration(TextDecoration.BOLD, true);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void draw() {
        EliteTier[] tiers = EliteTier.values();
        for (int i = 0; i < TIER_SLOTS.length && i < tiers.length; i++) {
            EliteTier t = tiers[i];
            boolean on = t == tier;
            set(TIER_SLOTS[i], Icon.of(on ? Material.LIME_DYE : Material.GRAY_DYE)
                    .name(t.display(), on ? t.textColor() : Icon.DIM)
                    .line(t.glyph() + " tier " + (i + 1), Icon.DIM)
                    .blank()
                    .line(Component.text("Health   ", Icon.DIM)
                            .append(Component.text("x" + t.healthMult(), Icon.TEXT)))
                    .line(Component.text("Damage   ", Icon.DIM)
                            .append(Component.text("x" + t.damageMult(), Icon.TEXT)))
                    .line(Component.text("Affixes  ", Icon.DIM)
                            .append(Component.text(String.valueOf(t.affixes()), Icon.TEXT)))
                    .blank()
                    .line(on ? "SELECTED" : "Click to select", on ? Icon.GOOD : Icon.ACCENT)
                    .glow(on)
                    .build());
        }

        set(RANDOM, Icon.of(Material.BONE)
                .name("Random Elite", Icon.WARN)
                .line("Random tier and mob", Icon.DIM)
                .blank()
                .line("Shift click for five", Icon.DIM)
                .build());

        for (int i = 0; i < GRID.length; i++) {
            int idx = page * GRID.length + i;
            if (idx >= MOBS.length) {
                break;
            }
            EntityType type = MOBS[idx];
            set(GRID[i], Icon.of(eggFor(type))
                    .name(pretty(type), tier.textColor())
                    .line("Spawns as " + tier.display(), Icon.DIM)
                    .blank()
                    .line("Click for one", Icon.ACCENT)
                    .line("Shift click for five", Icon.ACCENT)
                    .build());
        }

        set(BACK, Icon.back("the console"));
        fillEmpty();
    }

    /** Best matching spawn egg, falling back to a skull when there is none. */
    private Material eggFor(EntityType type) {
        Material egg = Material.matchMaterial(type.name() + "_SPAWN_EGG");
        return egg != null ? egg : Material.SKELETON_SKULL;
    }

    private String pretty(EntityType type) {
        String[] parts = type.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    @Override
    public void click(int slot, InventoryClickEvent event) {
        if (!player.hasPermission("endgame.admin")) {
            Fx.deny(player);
            player.closeInventory();
            return;
        }
        if (slot == BACK) {
            Fx.click(player);
            new AdminMenu(plugin, player).open();
            return;
        }
        if (slot == RANDOM) {
            spawnRandom(plugin, player, event.isShiftClick() ? 5 : 1);
            Fx.purchase(player);
            return;
        }
        EliteTier[] tiers = EliteTier.values();
        for (int i = 0; i < TIER_SLOTS.length && i < tiers.length; i++) {
            if (TIER_SLOTS[i] == slot) {
                tier = tiers[i];
                Fx.click(player);
                refresh();
                return;
            }
        }
        for (int i = 0; i < GRID.length; i++) {
            if (GRID[i] != slot) {
                continue;
            }
            int idx = page * GRID.length + i;
            if (idx >= MOBS.length) {
                return;
            }
            int count = event.isShiftClick() ? 5 : 1;
            for (int n = 0; n < count; n++) {
                spawn(plugin, player, MOBS[idx], tier, n);
            }
            Fx.purchase(player);
            player.sendMessage(Component.text("[ENDGAME] ", Icon.NEON)
                    .append(Component.text("Spawned " + count + " " + tier.display()
                            + " " + pretty(MOBS[idx]), tier.textColor())));
            return;
        }
    }

    /** Drops a random tier and type in front of the player. */
    static void spawnRandom(EndgamePlugin plugin, Player player, int count) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            EliteTier t = EliteTier.values()[rng.nextInt(EliteTier.values().length)];
            spawn(plugin, player, MOBS[rng.nextInt(MOBS.length)], t, i);
        }
    }

    /** Places one elite a few blocks ahead, spread out when spawning a pack. */
    private static void spawn(EndgamePlugin plugin, Player player,
                              EntityType type, EliteTier tier, int index) {
        Location at = player.getLocation().clone()
                .add(player.getLocation().getDirection().setY(0).normalize().multiply(4));
        if (index > 0) {
            double a = (Math.PI * 2 / 5) * index;
            at.add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5);
        }
        if (at.getWorld() == null) {
            return;
        }
        at.getChunk().load();
        Entity spawned = at.getWorld().spawnEntity(at, type);
        if (!(spawned instanceof LivingEntity le)) {
            spawned.remove();
            return;
        }
        plugin.elites().promote(le, tier);
    }
}
