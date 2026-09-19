package com.pallarium.questboard.model;

import org.bukkit.Material;

public enum QuestType {
    MINE("Mine", Material.IRON_PICKAXE),
    PLACE("Place", Material.BRICKS),
    KILL("Kill", Material.IRON_SWORD),
    FISH("Catch", Material.FISHING_ROD),
    CRAFT("Craft", Material.CRAFTING_TABLE),
    WALK("Walk", Material.LEATHER_BOOTS);

    public final String verb;
    public final Material icon;

    QuestType(String verb, Material icon) {
        this.verb = verb;
        this.icon = icon;
    }
}
