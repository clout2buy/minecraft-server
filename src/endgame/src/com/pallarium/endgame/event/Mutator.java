package com.pallarium.endgame.event;

import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A random condition rolled onto an event when it fires. Mutators change how the
 * fight actually plays, not just the numbers: gravity, fog, armour rot, mobs
 * that split, a floor that hurts to stand on. Two events of the same type should
 * never feel the same twice.
 */
public enum Mutator {

    NONE("", "", TextColor.fromHexString("#6B7280"), 0),

    /** Everything in the ring is dark. Your own light is all you get. */
    BLACKOUT("Blackout", "the light will not reach in here",
            TextColor.fromHexString("#6B7280"), 14),

    /** Low gravity. Everyone floats, mobs included. */
    THIN_AIR("Thin Air", "the ground has stopped insisting",
            TextColor.fromHexString("#22D3EE"), 12),

    /** Standing still hurts. Keep moving or bleed. */
    RESTLESS("Restless", "stand still and it finds you",
            TextColor.fromHexString("#F87171"), 12),

    /** Mobs split into two smaller ones when they die. */
    SPLITTING("Splitting", "killing it only makes more of it",
            TextColor.fromHexString("#A78BFA"), 11),

    /** Mobs explode into a soul blast on death. */
    VOLATILE("Volatile", "they are full of something that wants out",
            TextColor.fromHexString("#FB923C"), 11),

    /** Everything moves fast. Mobs and players alike. */
    FRENZY("Frenzy", "nothing here knows how to wait",
            TextColor.fromHexString("#FACC15"), 12),

    /** Armour durability burns off while you are inside. */
    CORROSIVE("Corrosive", "the air chews on your gear",
            TextColor.fromHexString("#4ADE80"), 9),

    /** Healing is halved inside the ring. */
    WITHERING("Withering", "wounds stay open in here",
            TextColor.fromHexString("#C084FC"), 10),

    /** The ring shrinks as the stage runs. Fight closer. */
    CLOSING("Closing Walls", "the edge is coming to you",
            TextColor.fromHexString("#F87171"), 9),

    /** Double loot, double everything trying to kill you. */
    GILDED("Gilded", "rich, and it knows it",
            TextColor.fromHexString("#FACC15"), 8),

    /** Every mob spawns one tier above the ceiling. */
    ASCENDANT("Ascendant", "these ones came up from somewhere worse",
            TextColor.fromHexString("#F87171"), 7);

    private final String title;
    private final String flavour;
    private final TextColor color;
    private final int weight;

    Mutator(String title, String flavour, TextColor color, int weight) {
        this.title = title;
        this.flavour = flavour;
        this.color = color;
        this.weight = weight;
    }

    public String title() {
        return title;
    }

    public String flavour() {
        return flavour;
    }

    public TextColor color() {
        return color;
    }

    public int weight() {
        return weight;
    }

    /** Loot multiplier this mutator is worth on completion. */
    public double rewardBonus() {
        return switch (this) {
            case NONE -> 1.0;
            case GILDED -> 2.0;
            case ASCENDANT, CLOSING -> 1.6;
            case SPLITTING, VOLATILE, CORROSIVE, WITHERING -> 1.4;
            default -> 1.25;
        };
    }

    /** Weighted pick. Returns NONE when the roll misses entirely. */
    public static Mutator roll(double chance) {
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return NONE;
        }
        List<Mutator> pool = new ArrayList<>();
        int total = 0;
        for (Mutator m : values()) {
            if (m.weight > 0) {
                pool.add(m);
                total += m.weight;
            }
        }
        int pick = ThreadLocalRandom.current().nextInt(total);
        for (Mutator m : pool) {
            pick -= m.weight;
            if (pick < 0) {
                return m;
            }
        }
        return NONE;
    }
}
