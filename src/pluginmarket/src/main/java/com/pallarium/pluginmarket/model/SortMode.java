package com.pallarium.pluginmarket.model;

public enum SortMode {
    MOST_DOWNLOADED("Most Downloaded", "-downloads", org.bukkit.Material.HOPPER),
    TOP_RATED("Top Rated", "-rating", org.bukkit.Material.NETHER_STAR),
    LATEST_UPDATE("Latest Update", "-updateDate", org.bukkit.Material.CLOCK),
    NEWEST("Newest", "-releaseDate", org.bukkit.Material.EMERALD);

    public final String label;
    public final String spigetSort;
    public final org.bukkit.Material icon;

    SortMode(String label, String spigetSort, org.bukkit.Material icon) {
        this.label = label;
        this.spigetSort = spigetSort;
        this.icon = icon;
    }

    public SortMode next() {
        SortMode[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
