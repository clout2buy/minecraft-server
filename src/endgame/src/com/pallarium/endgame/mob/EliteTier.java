package com.pallarium.endgame.mob;

import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.text.format.TextColor;

/**
 * Rarity ladder for spawned elites. Higher tiers are rarer, carry more
 * affixes, hit harder and drop more.
 */
public enum EliteTier {

    TOUCHED("Touched", TextColor.fromHexString("#9CA3AF"), 1, 1.6, 1.15, 0.30, 1.6),
    HAUNTED("Haunted", TextColor.fromHexString("#4ADE80"), 2, 2.4, 1.30, 0.42, 2.4),
    SAVAGE("Savage", TextColor.fromHexString("#22D3EE"), 3, 3.6, 1.50, 0.55, 3.6),
    ELITE("Elite", TextColor.fromHexString("#A78BFA"), 4, 5.5, 1.75, 0.70, 5.5),
    CHAMPION("Champion", TextColor.fromHexString("#FACC15"), 5, 8.0, 2.10, 0.85, 8.0),
    NIGHTMARE("Nightmare", TextColor.fromHexString("#F87171"), 6, 12.0, 2.60, 1.00, 13.0);

    private final String display;
    private final TextColor color;
    private final int affixes;
    private final double healthMult;
    private final double damageMult;
    private final double gearChance;
    private final double lootMult;

    EliteTier(String display, TextColor color, int affixes, double healthMult,
              double damageMult, double gearChance, double lootMult) {
        this.display = display;
        this.color = color;
        this.affixes = affixes;
        this.healthMult = healthMult;
        this.damageMult = damageMult;
        this.gearChance = gearChance;
        this.lootMult = lootMult;
    }

    public String display() {
        return display;
    }

    public TextColor color() {
        return color;
    }

    /** How many affixes a mob of this tier rolls. */
    public int affixes() {
        return affixes;
    }

    public double healthMult() {
        return healthMult;
    }

    public double damageMult() {
        return damageMult;
    }

    /** Chance per armor slot that this tier spawns wearing something. */
    public double gearChance() {
        return gearChance;
    }

    /** Multiplier on combat xp and bonus loot rolls. */
    public double lootMult() {
        return lootMult;
    }

    /** Chance a killed elite of this tier drops a worn piece of its gear. */
    public double gearDropChance() {
        return switch (this) {
            case TOUCHED -> 0.10;
            case HAUNTED -> 0.16;
            case SAVAGE -> 0.24;
            case ELITE -> 0.34;
            case CHAMPION -> 0.50;
            case NIGHTMARE -> 0.75;
        };
    }

    /** Colored glyph used in the mob's name plate. */
    public String glyph() {
        return switch (this) {
            case TOUCHED -> "\u2726";
            case HAUNTED -> "\u2620";
            case SAVAGE -> "\u2694";
            case ELITE -> "\u2756";
            case CHAMPION -> "\u265B";
            case NIGHTMARE -> "\u2623";
        };
    }

    public TextColor textColor() {
        return color == null ? Icon.TEXT : color;
    }
}
