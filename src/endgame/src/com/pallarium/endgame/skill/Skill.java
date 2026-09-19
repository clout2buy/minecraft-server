package com.pallarium.endgame.skill;

import com.pallarium.endgame.ui.Heads;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

/**
 * The seven trainable skills. Colors are neon-on-dark to match the GUI theme.
 */
public enum Skill {

    MINING("Mining", Heads.MINING, TextColor.fromHexString("#22D3EE"),
            "Break stone, ores and deepslate."),
    WOODCUTTING("Woodcutting", Heads.WOODCUTTING, TextColor.fromHexString("#4ADE80"),
            "Fell logs of every kind."),
    EXCAVATION("Excavation", Heads.EXCAVATION, TextColor.fromHexString("#FB923C"),
            "Dig dirt, sand, gravel and snow."),
    FARMING("Farming", Heads.FARMING, TextColor.fromHexString("#FACC15"),
            "Harvest fully grown crops."),
    COMBAT("Combat", Heads.COMBAT, TextColor.fromHexString("#F87171"),
            "Slay mobs in melee."),
    ARCHERY("Archery", Heads.ARCHERY, TextColor.fromHexString("#C084FC"),
            "Land killing blows at range."),
    FISHING("Fishing", Heads.FISHING, TextColor.fromHexString("#60A5FA"),
            "Reel in the deep.");

    private final String display;
    private final String texture;
    private final TextColor color;
    private final String blurb;

    Skill(String display, String texture, TextColor color, String blurb) {
        this.display = display;
        this.texture = texture;
        this.color = color;
        this.blurb = blurb;
    }

    /** Base64 skin for this skill's custom head. */
    public String texture() {
        return texture;
    }

    public String display() {
        return display;
    }

    /** Real item shown for this skill in menus and tabs. */
    public Material icon() {
        return switch (this) {
            case MINING -> Material.DIAMOND_PICKAXE;
            case WOODCUTTING -> Material.DIAMOND_AXE;
            case EXCAVATION -> Material.DIAMOND_SHOVEL;
            case FARMING -> Material.DIAMOND_HOE;
            case COMBAT -> Material.DIAMOND_SWORD;
            case ARCHERY -> Material.BOW;
            case FISHING -> Material.FISHING_ROD;
        };
    }

    public TextColor color() {
        return color;
    }

    public String blurb() {
        return blurb;
    }

    public String key() {
        return name().toLowerCase();
    }
}
