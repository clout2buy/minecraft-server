package com.pallarium.endgame.event;

/**
 * What the player was DOING when the world noticed. Every action feeds heat into
 * one of these buckets; when a bucket boils over, the matching event fires near
 * the player who caused it. This is the "you do something, something happens"
 * half of the system.
 */
public enum Trigger {
    /** Breaking ore, especially deep. Wakes things in the stone. */
    DIGGING("the deep"),
    /** Felling trees. The forest keeps count. */
    LOGGING("the treeline"),
    /** Killing mobs. Blood draws a crowd. */
    SLAUGHTER("the horde"),
    /** Farming and harvesting. Something is always hungry. */
    HARVEST("the fields"),
    /** Fishing and being near water. */
    TIDE("the water"),
    /** Standing still in the dark, or being deep underground at night. */
    DREAD("the dark"),
    /** Nothing in particular. Ambient world churn. */
    AMBIENT("the world");

    private final String source;

    Trigger(String source) {
        this.source = source;
    }

    public String source() {
        return source;
    }
}
