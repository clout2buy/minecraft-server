package com.pallarium.endgame.skill;

/**
 * Level curve. Level 1 is the floor, {@link #MAX_LEVEL} the ceiling.
 * Early levels come fast so a new player feels progress inside a minute,
 * the curve then stretches hard toward the cap.
 */
public final class XpTable {

    public static final int MAX_LEVEL = 100;

    private static final int[] TO_NEXT = new int[MAX_LEVEL + 2];
    private static final long[] TOTAL = new long[MAX_LEVEL + 2];

    static {
        long running = 0;
        for (int level = 1; level <= MAX_LEVEL; level++) {
            int need = (int) Math.round(75 * Math.pow(level, 1.42)) + 45 * level;
            TO_NEXT[level] = need;
            TOTAL[level] = running;
            running += need;
        }
        TO_NEXT[MAX_LEVEL] = 0;
        TOTAL[MAX_LEVEL + 1] = running;
    }

    private XpTable() {
    }

    /** XP required to move from {@code level} to {@code level + 1}. 0 at the cap. */
    public static int toNext(int level) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        if (level < 1) {
            return TO_NEXT[1];
        }
        return TO_NEXT[level];
    }

    /** Cumulative XP earned across every completed level below {@code level}. */
    public static long totalFor(int level) {
        if (level < 1) {
            return 0;
        }
        if (level > MAX_LEVEL) {
            return TOTAL[MAX_LEVEL + 1];
        }
        return TOTAL[level];
    }

    /** Skill points granted on reaching {@code level}. Every tenth level pays double. */
    public static int pointsForLevel(int level) {
        return level % 10 == 0 ? 2 : 1;
    }
}
