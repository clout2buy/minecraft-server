package com.pallarium.eventmaker.util;

import org.bukkit.ChatColor;

public final class Text {
    private Text() {}

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    public static String strip(String s) {
        return ChatColor.stripColor(color(s));
    }

    public static String time(int seconds) {
        if (seconds < 60) return seconds + "s";
        int m = seconds / 60, s = seconds % 60;
        if (m < 60) return s == 0 ? m + "m" : m + "m " + s + "s";
        int h = m / 60; m = m % 60;
        return m == 0 ? h + "h" : h + "h " + m + "m";
    }
}
