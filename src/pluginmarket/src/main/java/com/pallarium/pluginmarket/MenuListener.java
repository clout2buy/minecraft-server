package com.pallarium.pluginmarket;

import com.pallarium.pluginmarket.gui.CategoryMenu;
import com.pallarium.pluginmarket.gui.DetailMenu;
import com.pallarium.pluginmarket.gui.ListingMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuListener implements Listener {
    private final PluginMarketPlugin plugin;

    public MenuListener(PluginMarketPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (CategoryMenu.owns(e)) {
            plugin.categoryMenu().handleClick(e);
        } else if (ListingMenu.owns(e)) {
            plugin.listingMenu().handleClick(e);
        } else if (DetailMenu.owns(e)) {
            plugin.detailMenu().handleClick(e);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (title.contains("Plugin Market") || title.contains("Market:") || title.contains("Plugin Details")) {
            e.setCancelled(true);
        }
    }
}
