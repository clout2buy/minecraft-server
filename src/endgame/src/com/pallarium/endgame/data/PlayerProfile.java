package com.pallarium.endgame.data;

import com.pallarium.endgame.skill.Perk;
import com.pallarium.endgame.skill.Skill;
import com.pallarium.endgame.skill.XpTable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Live progression state for one player. Held in memory while online,
 * flushed to disk by {@link ProfileStore}.
 */
public class PlayerProfile {

    private final UUID uuid;
    private String name;

    private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
    private final Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
    private final Map<Skill, Integer> points = new EnumMap<>(Skill.class);
    private final Map<Perk, Integer> perks = new EnumMap<>(Perk.class);

    private boolean actionBarEnabled = true;
    private boolean levelSoundEnabled = true;
    private boolean menuItemEnabled = true;
    private boolean dirty = false;

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        for (Skill s : Skill.values()) {
            levels.put(s, 1);
            xp.put(s, 0);
            points.put(s, 0);
        }
        for (Perk p : Perk.values()) {
            perks.put(p, 0);
        }
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        if (!name.equals(this.name)) {
            this.name = name;
            dirty = true;
        }
    }

    public int level(Skill s) {
        return levels.getOrDefault(s, 1);
    }

    public int xp(Skill s) {
        return xp.getOrDefault(s, 0);
    }

    public int points(Skill s) {
        return points.getOrDefault(s, 0);
    }

    public int rank(Perk p) {
        return perks.getOrDefault(p, 0);
    }

    public boolean actionBarEnabled() {
        return actionBarEnabled;
    }

    public boolean levelSoundEnabled() {
        return levelSoundEnabled;
    }

    public boolean menuItemEnabled() {
        return menuItemEnabled;
    }

    public boolean dirty() {
        return dirty;
    }

    public void clean() {
        dirty = false;
    }

    public void toggleActionBar() {
        actionBarEnabled = !actionBarEnabled;
        dirty = true;
    }

    public void toggleLevelSound() {
        levelSoundEnabled = !levelSoundEnabled;
        dirty = true;
    }

    public void toggleMenuItem() {
        menuItemEnabled = !menuItemEnabled;
        dirty = true;
    }

    /** Awards skill points directly, used by the bonus-point perks. */
    public void grantPoint(Skill s, int amount) {
        if (amount > 0) {
            points.merge(s, amount, Integer::sum);
            dirty = true;
        }
    }

    /** Total effect value of a perk at its current rank. */
    public double perkValue(Perk p) {
        return p.valueAt(rank(p));
    }

    /**
     * Adds XP and rolls levels forward.
     *
     * @return number of levels gained, 0 if none
     */
    public int addXp(Skill s, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int level = level(s);
        if (level >= XpTable.MAX_LEVEL) {
            return 0;
        }
        int pool = xp(s) + amount;
        int gained = 0;
        while (level < XpTable.MAX_LEVEL) {
            int need = XpTable.toNext(level);
            if (need <= 0 || pool < need) {
                break;
            }
            pool -= need;
            level++;
            gained++;
            points.merge(s, XpTable.pointsForLevel(level), Integer::sum);
        }
        if (level >= XpTable.MAX_LEVEL) {
            pool = 0;
        }
        levels.put(s, level);
        xp.put(s, pool);
        dirty = true;
        return gained;
    }

    /** Buys one rank of a perk. Returns false when unaffordable, capped or locked. */
    public boolean buyPerk(Perk p) {
        int current = rank(p);
        if (current >= p.maxRank()) {
            return false;
        }
        if (level(p.skill()) < p.unlockLevel()) {
            return false;
        }
        int cost = p.costFor(current + 1);
        if (points(p.skill()) < cost) {
            return false;
        }
        points.merge(p.skill(), -cost, Integer::sum);
        perks.put(p, current + 1);
        dirty = true;
        return true;
    }

    /** Refunds every perk in a skill and returns the points spent. */
    public int resetPerks(Skill s) {
        int refunded = 0;
        for (Perk p : Perk.of(s)) {
            int r = rank(p);
            for (int i = 1; i <= r; i++) {
                refunded += p.costFor(i);
            }
            perks.put(p, 0);
        }
        if (refunded > 0) {
            points.merge(s, refunded, Integer::sum);
            dirty = true;
        }
        return refunded;
    }

    /** Sum of all skill levels, used for the leaderboard. */
    public int power() {
        int total = 0;
        for (Skill s : Skill.values()) {
            total += level(s);
        }
        return total;
    }

    /** Progress 0.0 to 1.0 toward the next level. */
    public double progress(Skill s) {
        int need = XpTable.toNext(level(s));
        if (need <= 0) {
            return 1.0;
        }
        return Math.min(1.0, (double) xp(s) / need);
    }

    // --- raw setters used only by the store when loading ---

    public void loadSkill(Skill s, int level, int skillXp, int skillPoints) {
        levels.put(s, Math.max(1, Math.min(XpTable.MAX_LEVEL, level)));
        xp.put(s, Math.max(0, skillXp));
        points.put(s, Math.max(0, skillPoints));
    }

    public void loadPerk(Perk p, int rank) {
        perks.put(p, Math.max(0, Math.min(p.maxRank(), rank)));
    }

    public void loadSettings(boolean actionBar, boolean sound, boolean menuItem) {
        this.actionBarEnabled = actionBar;
        this.levelSoundEnabled = sound;
        this.menuItemEnabled = menuItem;
    }
}
