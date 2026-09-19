package com.pallarium.endgame.boss;

import org.bukkit.Material;

/**
 * Every mechanic a boss can fire. Each one is a real, telegraphed, hitbox
 * checked attack, not a potion effect with a name. Bosses are built by
 * choosing from this list per phase.
 */
public enum Ability {

    /* ---- ground shapes -------------------------------------------- */
    SHOCKWAVE("Shockwave", "Expanding ring from the boss. Jump or run.",
            Material.HEAVY_WEIGHTED_PRESSURE_PLATE, 120),
    METEOR_RAIN("Meteor Rain", "Marked circles fall from the sky onto every player.",
            Material.MAGMA_BLOCK, 160),
    FLAME_CONE("Flame Cone", "Wide breath in the direction the boss is facing.",
            Material.BLAZE_POWDER, 100),
    CLEAVE_ARC("Cleave Arc", "Short brutal melee arc with heavy knockback.",
            Material.NETHERITE_AXE, 70),
    DEATH_CROSS("Death Cross", "Four rotating beams. Stand in a quadrant.",
            Material.END_CRYSTAL, 150),
    SPIRAL_LASH("Spiral Lash", "A spiral arm sweeps outward around the arena.",
            Material.CHAIN, 170),
    SAFE_DONUT("Collapse", "Everything except the boss's feet explodes.",
            Material.SCULK_SHRIEKER, 140),
    KILL_ZONE("Kill Zone", "Only a small marked disc is safe. Get in it.",
            Material.RESPAWN_ANCHOR, 200),
    PILLAR_SLAM("Pillar Slam", "Boss leaps and lands on its target.",
            Material.ANVIL, 130),
    RIFT_LINES("Rift Lines", "Parallel beams sweep across the arena floor.",
            Material.LIGHTNING_ROD, 150),

    /* ---- movement and control -------------------------------------- */
    VOID_PULL("Void Pull", "Yanks everyone into the boss, then detonates.",
            Material.ENDER_EYE, 180),
    REPULSE("Repulse", "Launches everyone away and off their footing.",
            Material.PISTON, 90),
    ROOT_SNARE("Snare", "Roots the nearest players in place for two seconds.",
            Material.COBWEB, 110),
    BLINK_STRIKE("Blink Strike", "Teleports behind a random player and hits hard.",
            Material.ENDER_PEARL, 80),
    GRAVITY_WELL("Gravity Well", "Throws targets skyward, fall damage on landing.",
            Material.SHULKER_SHELL, 160),

    /* ---- projectiles ------------------------------------------------ */
    BARRAGE("Barrage", "Fires a spread of homing bolts.",
            Material.FIRE_CHARGE, 110),
    SKYFALL("Skyfall", "Arrow storm rains down on the whole arena.",
            Material.SPECTRAL_ARROW, 140),
    SEEKER_ORB("Seeker Orb", "A slow orb chases one player until it hits.",
            Material.DRAGON_BREATH, 150),

    /* ---- summoning --------------------------------------------------- */
    SUMMON_ADDS("Summon Adds", "Calls in a wave of minions.",
            Material.ZOMBIE_HEAD, 200),
    SUMMON_ELITE("Summon Elite", "Calls in one real elite escort.",
            Material.WITHER_SKELETON_SKULL, 260),
    TOTEM_DROP("Totems", "Spawns totems that must be broken or the boss heals.",
            Material.SOUL_LANTERN, 240),

    /* ---- defensive ---------------------------------------------------- */
    SHIELD_UP("Barrier", "Immune until the damage threshold is met.",
            Material.SHIELD, 300),
    ENRAGE("Enrage", "Damage and speed spike for eight seconds.",
            Material.BLAZE_ROD, 220),
    DRAIN_LIFE("Drain", "Siphons health from everyone in range.",
            Material.GHAST_TEAR, 170),
    CURSE_MARK("Curse", "Marks a player. The mark detonates on them.",
            Material.WITHER_ROSE, 160),

    /* ---- arena --------------------------------------------------------- */
    ARENA_CAGE("Cage", "Walls the arena off for the rest of the phase.",
            Material.IRON_BARS, 400),
    BLACKOUT("Blackout", "Blinds the arena while the boss repositions.",
            Material.BLACK_CONCRETE, 200),
    QUAKE("Quake", "The whole floor shakes, continuous chip damage.",
            Material.DEEPSLATE, 180);

    private final String display;
    private final String blurb;
    private final Material icon;
    private final int cooldownTicks;

    Ability(String display, String blurb, Material icon, int cooldownTicks) {
        this.display = display;
        this.blurb = blurb;
        this.icon = icon;
        this.cooldownTicks = cooldownTicks;
    }

    public String display() {
        return display;
    }

    public String blurb() {
        return blurb;
    }

    public Material icon() {
        return icon;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }
}
