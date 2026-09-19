package com.pallarium.pluginmarket.gui;

import com.pallarium.pluginmarket.PluginMarketPlugin;
import com.pallarium.pluginmarket.model.Category;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Arrays;
import java.util.List;

/** Landing page: three big category tiles. */
public class CategoryMenu extends Menu {
    public static final String TITLE = ChatColor.BLACK + "" + ChatColor.BOLD + "Plugin Market";
    private final PluginMarketPlugin plugin;

    private static class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public CategoryMenu(PluginMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(new Holder(), 27, TITLE);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane());

        put(inv, 11, Category.SPIGOT, "Spigot / Bukkit's original marketplace category. Server plugins built on the Bukkit API.");
        put(inv, 13, Category.BUKKIT, "General purpose Bukkit-compatible plugins, filtered to free-only.");
        put(inv, 15, Category.SKRIPT, "Skript add-ons and scripts - drop-in behaviour with no coding.");

        inv.setItem(4, item(Material.COMPASS, ChatColor.GOLD + "" + ChatColor.BOLD + "Browse Plugins",
                Arrays.asList(ChatColor.GRAY + "Pick a category below.", ChatColor.GRAY + "Live data from SpigotMC via Spiget.")));
        p.openInventory(inv);
    }

    private void put(Inventory inv, int slot, Category c, String desc) {
        inv.setItem(slot, item(c.icon, ChatColor.AQUA + "" + ChatColor.BOLD + c.label,
                Arrays.asList(ChatColor.GRAY + desc, "", ChatColor.YELLOW + "Click to browse")));
    }

    @Override
    public void handleClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Category c = switch (e.getRawSlot()) {
            case 11 -> Category.SPIGOT;
            case 13 -> Category.BUKKIT;
            case 15 -> Category.SKRIPT;
            default -> null;
        };
        if (c != null) plugin.listingMenu().open(p, c);
    }

    public static boolean owns(InventoryClickEvent e) {
        return e.getInventory().getHolder() instanceof Holder;
    }
}
