package com.pallarium.endgame.ui;

import com.pallarium.endgame.EndgamePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Base for every ENDGAME screen. Being an InventoryHolder is what lets the
 * click listener identify our menus without any title string matching.
 */
public abstract class Menu implements InventoryHolder {

    protected final EndgamePlugin plugin;
    protected final Player player;
    protected Inventory inventory;

    protected Menu(EndgamePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    protected abstract Component title();

    protected abstract int rows();

    /** Fill the inventory. Called on open and on every refresh. */
    protected abstract void draw();

    /** Handle a click on one of our own slots. Cancellation is already done. */
    public abstract void click(int slot, InventoryClickEvent event);

    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = plugin.getServer().createInventory(this, rows() * 9, title());
        }
        return inventory;
    }

    public void open() {
        getInventory().clear();
        draw();
        player.openInventory(getInventory());
    }

    public void refresh() {
        if (inventory == null) {
            return;
        }
        inventory.clear();
        draw();
        player.updateInventory();
    }

    protected void set(int slot, ItemStack item) {
        if (slot >= 0 && slot < getInventory().getSize()) {
            inventory.setItem(slot, item);
        }
    }

    /** Paints a border of dark panes around the edge of the menu. */
    protected void border() {
        int size = rows() * 9;
        ItemStack pane = Icon.filler();
        for (int i = 0; i < 9; i++) {
            set(i, pane);
            set(size - 9 + i, pane);
        }
        for (int r = 1; r < rows() - 1; r++) {
            set(r * 9, pane);
            set(r * 9 + 8, pane);
        }
    }

    /** Fills every empty slot with dark panes. */
    protected void fillEmpty() {
        ItemStack pane = Icon.filler();
        for (int i = 0; i < getInventory().getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, pane);
            }
        }
    }

    public Player player() {
        return player;
    }
}
