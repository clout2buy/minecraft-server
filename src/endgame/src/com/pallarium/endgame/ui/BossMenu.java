package com.pallarium.endgame.ui;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.boss.Ability;
import com.pallarium.endgame.boss.BossDef;
import com.pallarium.endgame.boss.BossInstance;
import com.pallarium.endgame.boss.Bosses;
import com.pallarium.endgame.boss.Phase;
import com.pallarium.endgame.service.Fx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Boss summoning screen. Left side is the roster, clicking a boss shows its
 * full fight breakdown, shift clicking summons it on the spot.
 */
public class BossMenu extends Menu {

    private static final int[] ROSTER = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] DETAIL = {28, 29, 30, 31, 32, 33, 34};
    private static final int PURGE = 48;
    private static final int LIVE = 49;
    private static final int BACK = 50;

    private BossDef selected;

    public BossMenu(EndgamePlugin plugin, Player player) {
        super(plugin, player);
        List<BossDef> all = Bosses.all();
        this.selected = all.isEmpty() ? null : all.get(0);
    }

    @Override
    protected Component title() {
        return Component.text("\u25C6 Boss Registry", Icon.BAD)
                .decoration(TextDecoration.BOLD, true);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void draw() {
        List<BossDef> all = Bosses.all();
        for (int i = 0; i < ROSTER.length && i < all.size(); i++) {
            BossDef def = all.get(i);
            boolean current = def == selected;
            Icon icon = Icon.of(def.icon())
                    .name(def.name(), current ? def.color() : Icon.TEXT)
                    .line(def.subtitle(), Icon.DIM)
                    .blank()
                    .line(Component.text("Health ", Icon.DIM)
                            .append(Component.text((int) def.health() + " hp", Icon.TEXT)))
                    .line(Component.text("Phases ", Icon.DIM)
                            .append(Component.text(String.valueOf(def.phases().size()),
                                    Icon.TEXT)))
                    .line(Component.text("Arena  ", Icon.DIM)
                            .append(Component.text((int) def.arenaRadius() + " blocks",
                                    Icon.TEXT)))
                    .blank()
                    .line("Click to inspect the fight", Icon.ACCENT)
                    .line("Shift click to summon it here", Icon.BAD);
            set(ROSTER[i], icon.glow(current).build());
        }

        if (selected != null) {
            drawDetail();
        }

        set(PURGE, Icon.of(Material.LAVA_BUCKET)
                .name("Purge All Bosses", Icon.BAD)
                .line("Removes every live fight and its minions", Icon.DIM)
                .blank()
                .line(Component.text(plugin.bosses().active().size() + " running", Icon.TEXT))
                .blank()
                .line("Click to purge", Icon.BAD)
                .build());

        set(LIVE, liveCard());
        set(BACK, Icon.back());
        fillEmpty();
    }

    /** The fight breakdown: one item per phase with its whole rotation. */
    private void drawDetail() {
        set(19, Icon.of(selected.icon())
                .name(selected.name(), selected.color())
                .line(selected.lore(), Icon.DIM)
                .blank()
                .line(Component.text("Damage    ", Icon.DIM)
                        .append(Component.text(String.valueOf((int) selected.damage()),
                                Icon.TEXT)))
                .line(Component.text("Armor     ", Icon.DIM)
                        .append(Component.text(String.valueOf((int) selected.armor()),
                                Icon.TEXT)))
                .line(Component.text("Xp reward ", Icon.DIM)
                        .append(Component.text(String.valueOf(selected.xpReward()), Icon.GOOD)))
                .line(Component.text("Loot rows ", Icon.DIM)
                        .append(Component.text(String.valueOf(selected.drops().size()),
                                Icon.TEXT)))
                .glow(true)
                .build());

        List<Phase> phases = selected.phases();
        for (int i = 0; i < DETAIL.length && i < phases.size(); i++) {
            Phase phase = phases.get(i);
            Icon icon = Icon.of(phaseIcon(i))
                    .name(phase.name(), selected.color())
                    .line(Component.text("\"" + phase.line() + "\"", Icon.DIM))
                    .blank()
                    .line(Component.text("Opens below ", Icon.DIM)
                            .append(Component.text(
                                    (int) (phase.healthAbove() * 100) + "% hp", Icon.TEXT)))
                    .line(Component.text("Casts every ", Icon.DIM)
                            .append(Component.text(
                                    String.format("%.1fs", phase.castEveryTicks() / 20.0),
                                    Icon.TEXT)))
                    .line(Component.text("Damage x", Icon.DIM)
                            .append(Component.text(String.format("%.2f", phase.damageMult()),
                                    Icon.WARN)))
                    .blank()
                    .line("Mechanics", Icon.ACCENT);
            if (phase.onEnter() != null) {
                icon.line(Component.text("  opener: ", Icon.DIM)
                        .append(Component.text(phase.onEnter().display(), Icon.BAD)));
            }
            for (Ability a : phase.rotation()) {
                icon.line(Component.text("  \u25B8 ", Icon.DIM)
                        .append(Component.text(a.display(), Icon.TEXT)));
            }
            set(DETAIL[i], icon.build());
        }

        // loot preview row
        int slot = 37;
        for (BossDef.Drop drop : selected.drops()) {
            if (slot > 43) {
                break;
            }
            set(slot++, Icon.of(drop.material())
                    .name(drop.label() == null
                            ? pretty(drop.material().name()) : drop.label(), Icon.WARN)
                    .line(Component.text(drop.min() + " to " + drop.max(), Icon.TEXT))
                    .line(Component.text(drop.chance() + "% chance", Icon.DIM))
                    .build());
        }
    }

    private ItemStack liveCard() {
        List<BossInstance> active = plugin.bosses().active();
        Icon icon = Icon.of(Material.BEACON)
                .name("Live Fights", Icon.NEON)
                .line("Everything currently swinging", Icon.DIM)
                .blank();
        if (active.isEmpty()) {
            icon.line("nothing running", Icon.DIM);
        } else {
            for (BossInstance inst : active) {
                icon.line(Component.text("\u25C6 ", inst.def().color())
                        .append(Component.text(inst.def().name() + "  ", Icon.TEXT))
                        .append(Component.text(inst.healthPercentText(), Icon.GOOD)));
                icon.line(Component.text("   " + inst.currentPhase().name(), Icon.DIM));
            }
        }
        return icon.build();
    }

    private Material phaseIcon(int index) {
        return switch (index) {
            case 0 -> Material.IRON_SWORD;
            case 1 -> Material.DIAMOND_SWORD;
            case 2 -> Material.NETHERITE_SWORD;
            case 3 -> Material.END_CRYSTAL;
            default -> Material.PAPER;
        };
    }

    private String pretty(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    @Override
    public void click(int slot, InventoryClickEvent event) {
        List<BossDef> all = Bosses.all();
        for (int i = 0; i < ROSTER.length && i < all.size(); i++) {
            if (ROSTER[i] == slot) {
                BossDef def = all.get(i);
                if (event.isShiftClick()) {
                    player.closeInventory();
                    BossInstance inst = plugin.bosses().spawn(def.id(),
                            player.getLocation());
                    if (inst == null) {
                        player.sendMessage(Component.text("That boss failed to spawn.",
                                Icon.BAD));
                        Fx.deny(player);
                    } else {
                        Fx.milestone(player);
                    }
                } else {
                    selected = def;
                    Fx.click(player);
                    refresh();
                }
                return;
            }
        }
        if (slot == PURGE) {
            int n = plugin.bosses().purge();
            player.sendMessage(Component.text("Purged " + n + " boss fight"
                    + (n == 1 ? "" : "s") + ".", Icon.WARN));
            Fx.click(player);
            refresh();
            return;
        }
        if (slot == BACK) {
            Fx.click(player);
            new AdminMenu(plugin, player).open();
        }
    }
}
