package com.pallarium.endgame.ui;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.event.ActiveEvent;
import com.pallarium.endgame.event.Mutator;
import com.pallarium.endgame.service.Fx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Live view of everything running right now. Click to teleport to an event,
 * shift click to kill it and restore its blocks.
 */
public class RunningEventMenu extends Menu {

    private static final int[] GRID = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static final int STOP_ALL = 48;
    private static final int BACK = 49;
    private static final int REFRESH = 50;

    private final List<ActiveEvent> snapshot = new ArrayList<>();

    public RunningEventMenu(EndgamePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Component.text("\u25C6 Running Events", Icon.GOOD)
                .decoration(TextDecoration.BOLD, true);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void draw() {
        snapshot.clear();
        snapshot.addAll(plugin.events().active());

        if (snapshot.isEmpty()) {
            set(22, Icon.of(Material.GRAY_DYE)
                    .name("Nothing running", Icon.DIM)
                    .line("The world is quiet right now", Icon.DIM)
                    .blank()
                    .line("Fire one from the catalogue", Icon.ACCENT)
                    .build());
        }

        for (int i = 0; i < GRID.length && i < snapshot.size(); i++) {
            set(GRID[i], card(snapshot.get(i)));
        }

        set(STOP_ALL, Icon.of(Material.BARRIER)
                .name("Stop All", Icon.BAD)
                .line("Ends every event and restores blocks", Icon.DIM)
                .build());
        set(REFRESH, Icon.of(Material.CLOCK)
                .name("Refresh", Icon.NEON)
                .line("Redraw with current numbers", Icon.DIM)
                .build());
        set(BACK, Icon.back("the console"));
        fillEmpty();
    }

    private ItemStack card(ActiveEvent ev) {
        int secs = Math.max(0, ev.ticksLeft() / 20);
        double prog = ev.stage().goal() == 0 ? 0
                : Math.min(1.0, (double) ev.progress() / ev.stage().goal());

        Icon icon = Icon.of(ev.def().icon())
                .name(ev.def().title(), ev.def().color())
                .line(Component.text("Stage " + (ev.stageIndex() + 1) + "/" + ev.stageCount()
                        + "  ", Icon.DIM)
                        .append(Component.text(ev.stage().label(), Icon.TEXT)))
                .line(Component.text(ev.stage().type().name().toLowerCase(), Icon.DIM))
                .blank()
                .line(Icon.bar(prog, 16, ev.def().color()))
                .line(Component.text(ev.progress() + " / " + ev.stage().goal() + "   ", Icon.NEON)
                        .append(Component.text(String.format("%d:%02d left",
                                secs / 60, secs % 60), Icon.DIM)));

        if (ev.mutator() != Mutator.NONE) {
            icon.blank().line(Component.text("\u2726 " + ev.mutator().title(),
                            ev.mutator().color()))
                    .line(Component.text("  " + ev.mutator().flavour(), Icon.DIM));
        }

        icon.blank()
                .line(Component.text("Mobs alive  ", Icon.DIM)
                        .append(Component.text(String.valueOf(ev.mobs().size()), Icon.TEXT)))
                .line(Component.text("Players     ", Icon.DIM)
                        .append(Component.text(String.valueOf(ev.contribution().size()), Icon.TEXT)))
                .line(Component.text("At          ", Icon.DIM)
                        .append(Component.text(ev.center().getBlockX() + ", "
                                + ev.center().getBlockY() + ", "
                                + ev.center().getBlockZ(), Icon.TEXT)))
                .blank()
                .line("Click to teleport there", Icon.ACCENT)
                .line("Shift click to stop it", Icon.BAD);

        return icon.glow(true).build();
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
        if (slot == REFRESH) {
            Fx.click(player);
            refresh();
            return;
        }
        if (slot == STOP_ALL) {
            int n = plugin.events().active().size();
            plugin.events().shutdown();
            Fx.click(player);
            player.sendMessage(Component.text("[ENDGAME] ", Icon.NEON)
                    .append(Component.text("Cleared " + n + " events.", Icon.GOOD)));
            refresh();
            return;
        }
        for (int i = 0; i < GRID.length; i++) {
            if (GRID[i] != slot || i >= snapshot.size()) {
                continue;
            }
            ActiveEvent ev = snapshot.get(i);
            if (event.isShiftClick()) {
                plugin.events().stop(ev);
                Fx.click(player);
                player.sendMessage(Component.text("[ENDGAME] ", Icon.NEON)
                        .append(Component.text("Stopped " + ev.def().title(), Icon.GOOD)));
                refresh();
            } else {
                org.bukkit.Location to = ev.center().clone();
                to.setY(to.getWorld().getHighestBlockYAt(to) + 1);
                player.closeInventory();
                player.teleport(to);
                Fx.purchase(player);
            }
            return;
        }
    }
}
