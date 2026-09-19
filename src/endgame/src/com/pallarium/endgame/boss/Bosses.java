package com.pallarium.endgame.boss;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The roster. Each boss is a full scripted fight: multiple phases, its own
 * rotation, its own escalation, its own loot. This is the file you add to when
 * you want a new boss, and nothing else has to change.
 */
public final class Bosses {

    private Bosses() {
    }

    private static final Map<String, BossDef> REGISTRY = new LinkedHashMap<>();

    static {
        register(gravelord());
        register(emberTyrant());
        register(tideCaller());
        register(hollowKing());
        register(swarmMother());
        register(voidArchitect());
    }

    private static void register(BossDef def) {
        REGISTRY.put(def.id(), def);
    }

    public static BossDef get(String id) {
        return REGISTRY.get(id.toLowerCase());
    }

    public static List<BossDef> all() {
        return new ArrayList<>(REGISTRY.values());
    }

    /* ================================================================== */
    /*  1. THE GRAVELORD - the tutorial boss. Teaches telegraph reading.   */
    /* ================================================================== */

    private static BossDef gravelord() {
        return new BossDef("gravelord", "The Gravelord", "warden of the old barrows",
                EntityType.WITHER_SKELETON, TextColor.fromHexString("#A78BFA"),
                Material.WITHER_SKELETON_SKULL)
                .stats(520, 10, 10, 0.27)
                .arena(20)
                .lore("The first thing buried here, and the only one that refused to stay down.")
                .lines("claws out of the barrow", "crumbles back into the dirt")
                .minions(EntityType.SKELETON, EntityType.ZOMBIE, EntityType.HUSK)
                .xp(4500)
                .phase(new Phase("Phase I \u00b7 The Warden", 1.0,
                        "it fights like it remembers being a soldier")
                        .abilities(Ability.CLEAVE_ARC, Ability.SHOCKWAVE, Ability.BARRAGE)
                        .cadence(75))
                .phase(new Phase("Phase II \u00b7 The Grave Opens", 0.65,
                        "the dead come up with it")
                        .abilities(Ability.CLEAVE_ARC, Ability.SHOCKWAVE,
                                Ability.SUMMON_ADDS, Ability.ROOT_SNARE, Ability.METEOR_RAIN)
                        .cadence(62)
                        .damage(1.15)
                        .opener(Ability.SUMMON_ADDS))
                .phase(new Phase("Phase III \u00b7 Nothing Stays Buried", 0.3,
                        "it stops holding back")
                        .abilities(Ability.DEATH_CROSS, Ability.METEOR_RAIN,
                                Ability.SHOCKWAVE, Ability.SUMMON_ELITE, Ability.CURSE_MARK)
                        .cadence(48)
                        .damage(1.35)
                        .opener(Ability.ENRAGE))
                .drop(Material.NETHERITE_SCRAP, 1, 2, 45, "Barrow Iron")
                .drop(Material.DIAMOND, 3, 7, 80, null)
                .drop(Material.ENCHANTED_GOLDEN_APPLE, 1, 1, 22, null)
                .drop(Material.TOTEM_OF_UNDYING, 1, 1, 15, null)
                .drop(Material.BONE_BLOCK, 4, 10, 100, null);
    }

    /* ================================================================== */
    /*  2. THE EMBER TYRANT - fire, cones, positioning.                    */
    /* ================================================================== */

    private static BossDef emberTyrant() {
        return new BossDef("ember", "The Ember Tyrant", "what is left when a forge goes mad",
                EntityType.BLAZE, TextColor.fromHexString("#FB923C"),
                Material.BLAZE_POWDER)
                .stats(640, 12, 6, 0.3)
                .arena(24)
                .knockback(1.0)
                .lore("It was a furnace once. Someone kept feeding it after the work was done.")
                .lines("ignites", "goes out")
                .minions(EntityType.BLAZE, EntityType.MAGMA_CUBE, EntityType.PIGLIN_BRUTE)
                .xp(5200)
                .phase(new Phase("Phase I \u00b7 Kindling", 1.0,
                        "stay out of the cone")
                        .abilities(Ability.FLAME_CONE, Ability.BARRAGE, Ability.METEOR_RAIN)
                        .cadence(70))
                .phase(new Phase("Phase II \u00b7 White Heat", 0.6,
                        "the floor is no longer safe")
                        .abilities(Ability.FLAME_CONE, Ability.METEOR_RAIN,
                                Ability.RIFT_LINES, Ability.SUMMON_ADDS, Ability.REPULSE)
                        .cadence(55)
                        .damage(1.2)
                        .opener(Ability.REPULSE))
                .phase(new Phase("Phase III \u00b7 Meltdown", 0.25,
                        "it is trying to take the room with it")
                        .abilities(Ability.SAFE_DONUT, Ability.FLAME_CONE,
                                Ability.METEOR_RAIN, Ability.SPIRAL_LASH, Ability.ENRAGE)
                        .cadence(44)
                        .damage(1.4)
                        .opener(Ability.SAFE_DONUT))
                .drop(Material.NETHERITE_INGOT, 1, 1, 18, "Tyrant's Core")
                .drop(Material.BLAZE_ROD, 8, 16, 100, null)
                .drop(Material.FIRE_CHARGE, 6, 12, 70, null)
                .drop(Material.ANCIENT_DEBRIS, 1, 3, 40, null)
                .drop(Material.ENCHANTED_GOLDEN_APPLE, 1, 2, 28, null);
    }

    /* ================================================================== */
    /*  3. THE TIDE CALLER - control heavy, pulls and snares.              */
    /* ================================================================== */

    private static BossDef tideCaller() {
        return new BossDef("tide", "The Tide Caller", "it sang the harbor under",
                EntityType.ELDER_GUARDIAN, TextColor.fromHexString("#22D3EE"),
                Material.HEART_OF_THE_SEA)
                .stats(700, 11, 14, 0.24)
                .arena(22)
                .knockback(1.0)
                .lore("Every sailor who heard it is still down there, still listening.")
                .lines("surfaces", "sinks for good")
                .minions(EntityType.DROWNED, EntityType.GUARDIAN, EntityType.ZOMBIE_VILLAGER)
                .xp(5600)
                .phase(new Phase("Phase I \u00b7 The Song", 1.0,
                        "it is calling you closer")
                        .abilities(Ability.VOID_PULL, Ability.SEEKER_ORB, Ability.ROOT_SNARE)
                        .cadence(72))
                .phase(new Phase("Phase II \u00b7 Undertow", 0.6,
                        "the water is fighting you now")
                        .abilities(Ability.VOID_PULL, Ability.GRAVITY_WELL,
                                Ability.SPIRAL_LASH, Ability.SUMMON_ADDS, Ability.DRAIN_LIFE)
                        .cadence(58)
                        .damage(1.15)
                        .opener(Ability.TOTEM_DROP))
                .phase(new Phase("Phase III \u00b7 Drowning", 0.28,
                        "it stops pretending to be a fish")
                        .abilities(Ability.KILL_ZONE, Ability.VOID_PULL,
                                Ability.SPIRAL_LASH, Ability.SEEKER_ORB, Ability.DRAIN_LIFE)
                        .cadence(46)
                        .damage(1.35)
                        .opener(Ability.ARENA_CAGE))
                .drop(Material.TRIDENT, 1, 1, 35, "Tidecaller's Fang")
                .drop(Material.HEART_OF_THE_SEA, 1, 2, 55, null)
                .drop(Material.NAUTILUS_SHELL, 4, 9, 90, null)
                .drop(Material.PRISMARINE_CRYSTALS, 10, 22, 100, null)
                .drop(Material.SPONGE, 6, 14, 60, null);
    }

    /* ================================================================== */
    /*  4. THE HOLLOW KING - the hardest. Shields, totems, cage.           */
    /* ================================================================== */

    private static BossDef hollowKing() {
        return new BossDef("hollow", "The Hollow King", "he ruled nothing and never noticed",
                EntityType.WITHER_SKELETON, TextColor.fromHexString("#FACC15"),
                Material.GOLDEN_HELMET)
                .stats(950, 14, 16, 0.29)
                .arena(26)
                .knockback(1.0)
                .lore("The crown outlived the kingdom, the king, and the reason for either.")
                .lines("takes the throne", "is finally unseated")
                .minions(EntityType.VINDICATOR, EntityType.PILLAGER,
                        EntityType.WITHER_SKELETON, EntityType.EVOKER)
                .xp(8000)
                .phase(new Phase("Phase I \u00b7 Court", 1.0,
                        "he will not fight you himself yet")
                        .abilities(Ability.SUMMON_ADDS, Ability.BARRAGE,
                                Ability.CLEAVE_ARC, Ability.ROOT_SNARE)
                        .cadence(66))
                .phase(new Phase("Phase II \u00b7 The Crown Answers", 0.7,
                        "break the totems or this never ends")
                        .abilities(Ability.TOTEM_DROP, Ability.DEATH_CROSS,
                                Ability.SUMMON_ELITE, Ability.BLINK_STRIKE, Ability.CURSE_MARK)
                        .cadence(54)
                        .damage(1.2)
                        .opener(Ability.TOTEM_DROP))
                .phase(new Phase("Phase III \u00b7 Sealed Court", 0.45,
                        "the doors just locked")
                        .abilities(Ability.ARENA_CAGE, Ability.SHIELD_UP,
                                Ability.DEATH_CROSS, Ability.METEOR_RAIN,
                                Ability.SUMMON_ELITE, Ability.BLACKOUT)
                        .cadence(48)
                        .damage(1.3)
                        .opener(Ability.ARENA_CAGE))
                .phase(new Phase("Phase IV \u00b7 Hollow", 0.18,
                        "there was never a man under the crown")
                        .abilities(Ability.KILL_ZONE, Ability.SAFE_DONUT,
                                Ability.DEATH_CROSS, Ability.QUAKE,
                                Ability.BLINK_STRIKE, Ability.CURSE_MARK)
                        .cadence(38)
                        .damage(1.55)
                        .opener(Ability.ENRAGE))
                .drop(Material.NETHERITE_INGOT, 2, 3, 40, "Hollow Crown Shard")
                .drop(Material.ENCHANTED_GOLDEN_APPLE, 2, 4, 65, null)
                .drop(Material.TOTEM_OF_UNDYING, 1, 2, 45, null)
                .drop(Material.DIAMOND_BLOCK, 1, 3, 55, null)
                .drop(Material.NETHER_STAR, 1, 1, 12, "Crownlight")
                .drop(Material.ANCIENT_DEBRIS, 2, 5, 70, null);
    }

    /* ================================================================== */
    /*  5. THE SWARM MOTHER - adds pressure, constant spawning.            */
    /* ================================================================== */

    private static BossDef swarmMother() {
        return new BossDef("swarm", "The Swarm Mother", "she is never alone and neither are you",
                EntityType.SPIDER, TextColor.fromHexString("#4ADE80"),
                Material.SPIDER_EYE)
                .stats(580, 9, 8, 0.33)
                .arena(20)
                .lore("Kill the children and she makes more. Kill her and they stop.")
                .lines("skitters into the light", "curls up and stops moving")
                .minions(EntityType.CAVE_SPIDER, EntityType.SPIDER, EntityType.SILVERFISH)
                .xp(4800)
                .phase(new Phase("Phase I \u00b7 Brood", 1.0,
                        "she hides behind her young")
                        .abilities(Ability.SUMMON_ADDS, Ability.ROOT_SNARE, Ability.CLEAVE_ARC)
                        .cadence(60)
                        .opener(Ability.SUMMON_ADDS))
                .phase(new Phase("Phase II \u00b7 Nest", 0.6,
                        "the floor is all web now")
                        .abilities(Ability.SUMMON_ADDS, Ability.ROOT_SNARE,
                                Ability.SPIRAL_LASH, Ability.SEEKER_ORB, Ability.QUAKE)
                        .cadence(50)
                        .damage(1.15)
                        .opener(Ability.SUMMON_ELITE))
                .phase(new Phase("Phase III \u00b7 Mother", 0.28,
                        "she stops sending them and comes herself")
                        .abilities(Ability.PILLAR_SLAM, Ability.BLINK_STRIKE,
                                Ability.SUMMON_ADDS, Ability.DEATH_CROSS, Ability.ENRAGE)
                        .cadence(40)
                        .damage(1.4)
                        .opener(Ability.ENRAGE))
                .drop(Material.STRING, 20, 40, 100, null)
                .drop(Material.FERMENTED_SPIDER_EYE, 5, 10, 80, null)
                .drop(Material.DIAMOND, 4, 8, 75, null)
                .drop(Material.ENCHANTED_GOLDEN_APPLE, 1, 1, 20, null)
                .drop(Material.EXPERIENCE_BOTTLE, 12, 28, 100, null);
    }

    /* ================================================================== */
    /*  6. THE VOID ARCHITECT - pure geometry. The showpiece.              */
    /* ================================================================== */

    private static BossDef voidArchitect() {
        return new BossDef("architect", "The Void Architect", "it is drawing something with you in it",
                EntityType.EVOKER, TextColor.fromHexString("#F87171"),
                Material.END_CRYSTAL)
                .stats(880, 13, 12, 0.3)
                .arena(28)
                .knockback(1.0)
                .lore("Every shape it makes is a door. None of them open outward.")
                .lines("begins the drawing", "loses its shape")
                .minions(EntityType.VEX, EntityType.ENDERMAN, EntityType.PHANTOM)
                .xp(7500)
                .phase(new Phase("Phase I \u00b7 Sketch", 1.0,
                        "simple shapes, learn them")
                        .abilities(Ability.RIFT_LINES, Ability.DEATH_CROSS, Ability.BARRAGE)
                        .cadence(68)
                        .ordered())
                .phase(new Phase("Phase II \u00b7 Composition", 0.68,
                        "it starts layering them")
                        .abilities(Ability.DEATH_CROSS, Ability.SPIRAL_LASH,
                                Ability.RIFT_LINES, Ability.GRAVITY_WELL, Ability.SEEKER_ORB)
                        .cadence(52)
                        .damage(1.2)
                        .opener(Ability.BLACKOUT))
                .phase(new Phase("Phase III \u00b7 The Finished Work", 0.35,
                        "you are inside the picture")
                        .abilities(Ability.KILL_ZONE, Ability.SAFE_DONUT,
                                Ability.DEATH_CROSS, Ability.SPIRAL_LASH,
                                Ability.VOID_PULL, Ability.METEOR_RAIN)
                        .cadence(42)
                        .damage(1.45)
                        .opener(Ability.ARENA_CAGE))
                .phase(new Phase("Phase IV \u00b7 Erasure", 0.12,
                        "it would rather delete the room than lose")
                        .abilities(Ability.SAFE_DONUT, Ability.KILL_ZONE,
                                Ability.SPIRAL_LASH, Ability.QUAKE, Ability.VOID_PULL)
                        .cadence(34)
                        .damage(1.6)
                        .opener(Ability.ENRAGE))
                .drop(Material.NETHER_STAR, 1, 1, 20, "Architect's Compass")
                .drop(Material.END_CRYSTAL, 2, 4, 60, null)
                .drop(Material.SHULKER_SHELL, 2, 5, 55, null)
                .drop(Material.NETHERITE_INGOT, 1, 2, 30, null)
                .drop(Material.ENCHANTED_GOLDEN_APPLE, 2, 3, 50, null)
                .drop(Material.ECHO_SHARD, 4, 9, 80, null);
    }
}
