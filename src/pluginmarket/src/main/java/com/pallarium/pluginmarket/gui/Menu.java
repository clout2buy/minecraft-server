package com.pallarium.pluginmarket.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public abstract class Menu {
    public abstract void handleClick(org.bukkit.event.inventory.InventoryClickEvent e);

    protected static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(meta);
        return it;
    }

    protected static ItemStack pane() {
        return item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
    }
}
