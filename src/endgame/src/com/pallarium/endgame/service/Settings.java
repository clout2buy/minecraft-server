package com.pallarium.endgame.service;

import com.pallarium.endgame.EndgamePlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed view over config.yml so the rest of the plugin never touches raw keys.
 */
public class Settings {

    private final EndgamePlugin plugin;

    private double xpMultiplier = 1.0;
    private boolean broadcastMilestones = true;
    private boolean antiFarm = true;
    private boolean giveMenuItem = true;
    private int menuItemSlot = 8;
    private int autoSaveMinutes = 5;

    public Settings(EndgamePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        xpMultiplier = Math.max(0.1, Math.min(20.0, c.getDouble("xp-multiplier", 1.0)));
        broadcastMilestones = c.getBoolean("broadcast-milestones", true);
        antiFarm = c.getBoolean("anti-farm", true);
        giveMenuItem = c.getBoolean("menu-item.enabled", true);
        menuItemSlot = Math.max(0, Math.min(8, c.getInt("menu-item.slot", 8)));
        autoSaveMinutes = Math.max(1, c.getInt("auto-save-minutes", 5));
    }

    public double xpMultiplier() {
        return xpMultiplier;
    }

    public boolean broadcastMilestones() {
        return broadcastMilestones;
    }

    public boolean antiFarm() {
        return antiFarm;
    }

    public boolean giveMenuItem() {
        return giveMenuItem;
    }

    public int menuItemSlot() {
        return menuItemSlot;
    }

    public boolean damageNumbers() {
        return plugin.getConfig().getBoolean("damage-numbers", true);
    }

    public double critThreshold() {
        return plugin.getConfig().getDouble("crit-threshold", 1.5);
    }

    public int autoSaveMinutes() {
        return autoSaveMinutes;
    }
}
