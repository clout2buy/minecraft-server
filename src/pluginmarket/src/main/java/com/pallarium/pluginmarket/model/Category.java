package com.pallarium.pluginmarket.model;

public enum Category {
    SPIGOT("Spigot Plugins", org.bukkit.Material.ANVIL),
    BUKKIT("Bukkit Plugins", org.bukkit.Material.CRAFTING_TABLE),
    SKRIPT("Skript Plugins", org.bukkit.Material.WRITABLE_BOOK);

    public final String label;
    public final org.bukkit.Material icon;

    Category(String label, org.bukkit.Material icon) {
        this.label = label;
        this.icon = icon;
    }
}
