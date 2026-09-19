package com.pallarium.pluginmarket.model;

/** Flat view of a Spiget resource - enough for a listing tile and the detail page. */
public class PluginInfo {
    public int id;
    public String name = "";
    public String tag = "";
    public String author = "";
    public int downloads;
    public double rating;
    public int ratingCount;
    public long updateEpochMs;
    public long releaseEpochMs;
    public String version = "";
    public boolean premium;
    public boolean externalDownload;
    public boolean nativeMinecraft;
    public String iconUrl = "";
    public Category category = Category.SPIGOT;

    public String updatedAgo() {
        return ago(updateEpochMs);
    }

    public String releasedAgo() {
        return ago(releaseEpochMs);
    }

    private static String ago(long epochMs) {
        if (epochMs <= 0) return "unknown";
        long days = (System.currentTimeMillis() - epochMs) / 86400000L;
        if (days <= 0) return "today";
        if (days == 1) return "1 day ago";
        if (days < 30) return days + " days ago";
        if (days < 365) return (days / 30) + " months ago";
        return (days / 365) + " years ago";
    }

    public String downloadsShort() {
        if (downloads >= 1_000_000) return String.format("%.1fM", downloads / 1_000_000.0);
        if (downloads >= 1_000) return String.format("%.1fk", downloads / 1_000.0);
        return String.valueOf(downloads);
    }
}
