package com.pallarium.endgame.skill;

/**
 * What a perk actually does when it fires. The listener switches on this, so
 * adding a perk is a data change rather than a code change.
 */
public enum Effect {

    /** Duplicate the block or mob drops. */
    DOUBLE_DROP,
    /** Grant Haste II briefly. */
    HASTE,
    /** Grant Speed II briefly. */
    SPEED,
    /** Grant Regeneration briefly. */
    REGEN,
    /** Grant Night Vision underground. */
    NIGHT_VISION,
    /** Grant Water Breathing. */
    WATER_BREATH,
    /** Restore hunger and saturation. */
    FOOD,
    /** Bonus percentage XP for the owning skill. */
    XP_BOOST,
    /** Reduce incoming damage. */
    DAMAGE_REDUCTION,
    /** Flat bonus melee damage. */
    MELEE_DAMAGE,
    /** Flat bonus bow damage. */
    BOW_DAMAGE,
    /** Double the outgoing damage of the hit. */
    CRIT,
    /** Heal the attacker on a hit. */
    LIFESTEAL,
    /** Recover the arrow that was fired. */
    ARROW_SAVE,
    /** Chance of an extra skill point on level up. */
    POINT_BONUS,
    /** Extra rare drops from the block or mob. */
    TREASURE,

    // --- signature effects, one or two per skill ---

    /** Ore drops already smelted into ingots. */
    AUTO_SMELT,
    /** Drops teleport straight into the inventory. */
    MAGNET,
    /** The broken block erupts with a spray of extra ore. */
    ORE_BURST,
    /** Spawns a physical loot chest in front of the player. */
    LOOT_CHEST,
    /** Fells the entire tree from one log. */
    TIMBER_FELL,
    /** Replants the crop that was just harvested. */
    REPLANT,
    /** Calls a lightning bolt onto the target. */
    LIGHTNING,
    /** Rains a volley of arrows down on the target. */
    ARROW_STORM,
    /** Summons a tamed wolf to fight for you. */
    SUMMON_WOLF,
    /** The water erupts with a burst of fish. */
    FISH_FRENZY
}
