package com.pallarium.eventmaker.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class Items {
    private Items() {}

    public static ItemStack of(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            List<String> lines = new ArrayList<>();
            for (String l : lore) lines.add(Text.color(l));
            meta.setLore(lines);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack of(Material mat, String name, List<String> lore) {
        return of(mat, name, lore.toArray(new String[0]));
    }

    public static ItemStack filler() {
        return of(Material.BLACK_STAINED_GLASS_PANE, " ");
    }

    public static ItemStack toggle(boolean on, String label, String... lore) {
        String[] full = new String[lore.length + 2];
        System.arraycopy(lore, 0, full, 0, lore.length);
        full[lore.length] = "";
        full[lore.length + 1] = on ? "&aEnabled &7- click to disable" : "&cDisabled &7- click to enable";
        return of(on ? Material.LIME_DYE : Material.GRAY_DYE, (on ? "&a" : "&c") + label, full);
    }
}
