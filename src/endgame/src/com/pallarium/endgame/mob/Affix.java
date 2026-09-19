package com.pallarium.endgame.mob;

import net.kyori.adventure.text.format.TextColor;

/**
 * A random modifier rolled onto an elite. Each one is a title fragment plus
 * a behaviour handled by {@code EliteService}.
 *
 * <p>Prefixes read before the mob type, suffixes read after, so a mob can end
 * up as "Blazing Zombie of the Swarm" without the grammar falling apart.</p>
 */
public enum Affix {

    // prefixes
    BLAZING("Blazing", true, TextColor.fromHexString("#FB923C"), "Sets you on fire and is immune to burning"),
    FROZEN("Frozen", true, TextColor.fromHexString("#7DD3FC"), "Chills attackers, slowing them badly"),
    VENOMOUS("Venomous", true, TextColor.fromHexString("#84CC16"), "Every hit injects lingering poison"),
    WITHERING("Withering", true, TextColor.fromHexString("#52525B"), "Applies wither on hit"),
    ARMORED("Armored", true, TextColor.fromHexString("#A1A1AA"), "Takes heavily reduced damage"),
    SWIFT("Swift", true, TextColor.fromHexString("#22D3EE"), "Moves dramatically faster"),
    GIANT("Giant", true, TextColor.fromHexString("#F59E0B"), "Oversized, huge health pool"),
    TINY("Tiny", true, TextColor.fromHexString("#D8B4FE"), "Shrunken, fast and hard to hit"),
    VOLATILE("Volatile", true, TextColor.fromHexString("#EF4444"), "Detonates violently on death"),
    VAMPIRIC("Vampiric", true, TextColor.fromHexString("#BE123C"), "Heals itself from the damage it deals"),
    SPECTRAL("Spectral", true, TextColor.fromHexString("#E5E7EB"), "Invisible except for its gear"),
    ELECTRIC("Electric", true, TextColor.fromHexString("#FDE047"), "Calls lightning when it strikes"),
    MOLTEN("Molten", true, TextColor.fromHexString("#F97316"), "Leaves burning ground behind it"),
    HARDENED("Hardened", true, TextColor.fromHexString("#78716C"), "Immune to knockback"),

    // suffixes
    OF_THE_SWARM("of the Swarm", false, TextColor.fromHexString("#A3E635"), "Summons minions when wounded"),
    OF_THIEVES("of Thieves", false, TextColor.fromHexString("#FACC15"), "Steals xp on hit, drops extra loot"),
    OF_THE_VOID("of the Void", false, TextColor.fromHexString("#A78BFA"), "Blinks toward you when struck"),
    OF_THE_STORM("of the Storm", false, TextColor.fromHexString("#38BDF8"), "Launches attackers into the air"),
    OF_DECAY("of Decay", false, TextColor.fromHexString("#65A30D"), "Weakens your attacks with rot"),
    OF_THE_HUNT("of the Hunt", false, TextColor.fromHexString("#FB7185"), "Locks on and never loses you"),
    OF_GREED("of Greed", false, TextColor.fromHexString("#FBBF24"), "Carries a far richer loot table"),
    OF_REBIRTH("of Rebirth", false, TextColor.fromHexString("#34D399"), "Revives once at half health");

    private final String display;
    private final boolean prefix;
    private final TextColor color;
    private final String lore;

    Affix(String display, boolean prefix, TextColor color, String lore) {
        this.display = display;
        this.prefix = prefix;
        this.color = color;
        this.lore = lore;
    }

    public String display() {
        return display;
    }

    public boolean prefix() {
        return prefix;
    }

    public TextColor color() {
        return color;
    }

    public String lore() {
        return lore;
    }
}
