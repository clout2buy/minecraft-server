package com.pallarium.endgame.ui;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.event.ActiveEvent;
import com.pallarium.endgame.event.EventStage;
import com.pallarium.endgame.event.Trigger;
import com.pallarium.endgame.event.WorldEvent;
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
 * Browse every world event and fire one where you stand. Paged, because the
 * catalogue keeps growing. Left click fires on you, shift click fires it
 * thirty blocks out so you can watch it from a distance.
 */
public class EventAdminMenu extends Menu {

    private static final int[] GRID = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static final int PREV = 45;
    private static final int BACK = 49;
    private static final int NEXT = 53;

    private final int page;
    private final List<WorldEvent> all = new ArrayList<>();

    public EventAdminMenu(EndgamePlugin plugin, Player player, int page) {
        super(plugin, player);
        this.page = page;
        for (Trigger t : Trigger.values()) {
            all.addAll(WorldEvent.forTrigger(t));
        }
    }

    @Override
    protected Component title() {
        int pages = Math.max(1, (all.size() + GRID.length - 1) / GRID.length);
        return Component.text("\u25C6 Events  " + (page + 1) + "/" + pages, Icon.NEON)
                .decoration(TextDecoration.BOLD, true);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void draw() {
        int start = page * GRID.length;
        for (int i = 0; i < GRID.length; i++) {
            int idx = start + i;
            if (idx >= all.size()) {
                break;
            }
            set(GRID[i], card(all.get(idx)));
        }

        boolean hasPrev = page > 0;
        boolean hasNext = start + GRID.length < all.size();

        set(PREV, Icon.nav(hasPrev ? Heads.ARROW_LEFT : Heads.ARROW_LEFT_DIM,
                "Previous", hasPrev ? "Page " + page : "Nothing back there", hasPrev));
        set(NEXT, Icon.nav(hasNext ? Heads.ARROW_RIGHT : Heads.ARROW_RIGHT_DIM,
                "Next", hasNext ? "Page " + (page + 2) : "End of the list", hasNext));
        set(BACK, Icon.back("the console"));
        fillEmpty();
    }

    private ItemStack card(WorldEvent def) {
        Icon icon = Icon.of(def.icon())
                .name(def.title(), def.color())
                .line(def.flavour(), Icon.DIM)
                .blank()
                .line(Component.text("Trigger   ", Icon.DIM)
                        .append(Component.text(def.trigger().name().toLowerCase(), Icon.TEXT)))
                .line(Component.text("Ceiling   ", Icon.DIM)
                        .append(Component.text(def.ceiling().display(), def.ceiling().textColor())))
                .line(Component.text("Radius    ", Icon.DIM)
                        .append(Component.text(def.radius() + " blocks", Icon.TEXT)))
                .line(Component.text("Site      ", Icon.DIM)
                        .append(Component.text(def.setPiece().name().toLowerCase(), Icon.TEXT)))
                .blank()
                .line(Component.text("Stages", Icon.TEXT));

        int n = 1;
        for (EventStage stage : def.stages()) {
            icon.line(Component.text("  " + n++ + ". ", Icon.DIM)
                    .append(Component.text(stage.label(), def.color()))
                    .append(Component.text("  " + stage.type().name().toLowerCase()
                            + " x" + stage.goal() + "  " + stage.seconds() + "s", Icon.DIM)));
        }

        return icon.blank()
                .line("Click to fire here", Icon.ACCENT)
                .line("Shift click to fire 30 blocks out", Icon.ACCENT)
                .build();
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
        if (slot == PREV && page > 0) {
            Fx.click(player);
            new EventAdminMenu(plugin, player, page - 1).open();
            return;
        }
        if (slot == NEXT && (page + 1) * GRID.length < all.size()) {
            Fx.click(player);
            new EventAdminMenu(plugin, player, page + 1).open();
            return;
        }
        for (int i = 0; i < GRID.length; i++) {
            if (GRID[i] != slot) {
                continue;
            }
            int idx = page * GRID.length + i;
            if (idx >= all.size()) {
                return;
            }
            WorldEvent def = all.get(idx);
            org.bukkit.Location at = player.getLocation();
            if (event.isShiftClick()) {
                at = at.clone().add(at.getDirection().setY(0).normalize().multiply(30));
                at.setY(at.getWorld().getHighestBlockYAt(at) + 1);
            }
            ActiveEvent ev = plugin.events().fire(def, at, player);
            if (ev == null) {
                Fx.deny(player);
                player.sendMessage(Component.text("[ENDGAME] ", Icon.NEON)
                        .append(Component.text("Event cap reached, stop one first.", Icon.BAD)));
            } else {
                Fx.purchase(player);
                player.closeInventory();
            }
            return;
        }
    }
}
