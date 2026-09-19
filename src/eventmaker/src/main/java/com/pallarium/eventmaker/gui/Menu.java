package com.pallarium.eventmaker.gui;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.util.Items;
import com.pallarium.eventmaker.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class Menu implements InventoryHolder {
    protected final EventMakerPlugin plugin;
    protected final Player player;
    protected Inventory inv;

    protected Menu(EventMakerPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    protected abstract String title();

    protected abstract int rows();

    protected abstract void draw();

    public abstract void click(InventoryClickEvent e);

    public void closed(InventoryCloseEvent e) {}

    /** Whether clicks in the player's own inventory should be cancelled too. */
    public boolean lockBottom() {
        return true;
    }

    public void open() {
        inv = Bukkit.createInventory(this, rows() * 9, Text.color(title()));
        fill();
        draw();
        player.openInventory(inv);
    }

    public void refresh() {
        inv.clear();
        fill();
        draw();
    }

    protected void fill() {
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, Items.filler());
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
