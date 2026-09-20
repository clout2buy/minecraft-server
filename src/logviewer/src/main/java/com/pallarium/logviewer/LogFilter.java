package com.pallarium.logviewer;

import org.bukkit.Material;

import java.util.Locale;

public enum LogFilter {
    ALL("All", Material.BOOK) {
        public boolean matches(String l) { return true; }
    },
    ERRORS("Errors", Material.REDSTONE_BLOCK) {
        public boolean matches(String l) {
            return l.contains("/ERROR]") || l.contains("/FATAL]") || l.contains("Exception") || l.trim().startsWith("at ");
        }
    },
    WARNINGS("Warnings", Material.ORANGE_WOOL) {
        public boolean matches(String l) { return l.contains("/WARN]"); }
    },
    CHAT("Chat", Material.OAK_SIGN) {
        public boolean matches(String l) {
            int i = l.indexOf("]: ");
            return i > 0 && l.length() > i + 4 && l.charAt(i + 3) == '<';
        }
    },
    JOINS("Joins / Leaves", Material.OAK_DOOR) {
        public boolean matches(String l) {
            return l.contains("joined the game") || l.contains("left the game") || l.contains("logged in with entity id")
                    || l.contains("lost connection");
        }
    },
    COMMANDS("Commands", Material.COMMAND_BLOCK) {
        public boolean matches(String l) { return l.contains("issued server command:"); }
    },
    PLUGINS("Plugins", Material.ANVIL) {
        public boolean matches(String l) {
            String s = l.toLowerCase(Locale.ROOT);
            return s.contains("enabling ") || s.contains("disabling ") || s.contains("loading server plugin");
        }
    };

    public final String label;
    public final Material icon;

    LogFilter(String label, Material icon) {
        this.label = label;
        this.icon = icon;
    }

    public abstract boolean matches(String line);
}
