package com.pallarium.endgame.skill;

import com.pallarium.endgame.ui.PerkHeads;

import java.util.ArrayList;
import java.util.List;

/**
 * Every perk: tied to a skill, gated behind a skill level, ranked up with
 * skill points. Ten per skill, ordered by unlock level.
 */
public enum Perk {

    // --- Mining ---
    VEIN_FORTUNE(Skill.MINING, "Vein Fortune", PerkHeads.VEIN_FORTUNE, 1, 5, 4.0, Effect.DOUBLE_DROP,
            "%s%% chance to double ore and stone drops",
            "The vein gives twice."),
    MINERS_HASTE(Skill.MINING, "Miner's Haste", PerkHeads.MINERS_HASTE, 5, 5, 6.0, Effect.HASTE,
            "%s%% chance of Haste II for 8s",
            "Swing until the stone gives up."),
    SMELTING_TOUCH(Skill.MINING, "Smelting Touch", PerkHeads.SMELTING_TOUCH, 10, 5, 8.0, Effect.AUTO_SMELT,
            "%s%% chance ore drops as finished ingots",
            "Skip the furnace entirely."),
    MAGNETITE(Skill.MINING, "Magnetite", PerkHeads.MAGNETITE, 15, 5, 10.0, Effect.MAGNET,
            "%s%% chance drops fly to your inventory",
            "Nothing is left on the floor."),
    GEODE_BURST(Skill.MINING, "Geode Burst", PerkHeads.GEODE_BURST, 22, 5, 3.0, Effect.ORE_BURST,
            "%s%% chance the block erupts with extra ore",
            "The rock splits and pays out."),
    STONE_SKIN(Skill.MINING, "Stone Skin", PerkHeads.STONE_SKIN, 30, 5, 2.0, Effect.DAMAGE_REDUCTION,
            "%s%% less damage taken",
            "Skin like the deepslate."),
    MOTHERLODE(Skill.MINING, "Motherlode", PerkHeads.MOTHERLODE, 40, 3, 2.0, Effect.LOOT_CHEST,
            "%s%% chance to drop a loot chest of ore",
            "A chest slams down in front of you."),
    PROSPECTOR(Skill.MINING, "Prospector", PerkHeads.PROSPECTOR, 50, 3, 5.0, Effect.POINT_BONUS,
            "%s%% chance of a bonus skill point",
            "You know where to look."),
    NIGHT_EYES(Skill.MINING, "Night Eyes", PerkHeads.NIGHT_EYES, 62, 3, 20.0, Effect.NIGHT_VISION,
            "%s%% chance of Night Vision underground",
            "No torch needed."),
    EARTHS_BLESSING(Skill.MINING, "Earth's Blessing", PerkHeads.EARTHS_BLESSING, 80, 3, 8.0, Effect.REGEN,
            "%s%% chance of Regeneration on a break",
            "The mountain heals its own."),

    // --- Woodcutting ---
    TIMBER(Skill.WOODCUTTING, "Timber", PerkHeads.TIMBER, 1, 5, 4.0, Effect.DOUBLE_DROP,
            "%s%% chance to double log drops",
            "One swing, two logs."),
    LUMBER_FURY(Skill.WOODCUTTING, "Lumber Fury", PerkHeads.LUMBER_FURY, 5, 5, 6.0, Effect.HASTE,
            "%s%% chance of Haste II for 8s",
            "The axe does not slow down."),
    TREEFELLER(Skill.WOODCUTTING, "Treefeller", PerkHeads.TREEFELLER, 12, 5, 6.0, Effect.TIMBER_FELL,
            "%s%% chance the whole tree falls at once",
            "Break one log, drop them all."),
    SAP_RUNNER(Skill.WOODCUTTING, "Sap Runner", PerkHeads.SAP_RUNNER, 18, 5, 5.0, Effect.XP_BOOST,
            "+%s%% Woodcutting XP",
            "Every cut teaches you more."),
    FOREST_STRIDE(Skill.WOODCUTTING, "Forest Stride", PerkHeads.FOREST_STRIDE, 25, 3, 12.0, Effect.SPEED,
            "%s%% chance of Speed II for 8s",
            "The forest moves aside."),
    BARK_HIDE(Skill.WOODCUTTING, "Bark Hide", PerkHeads.BARK_HIDE, 32, 5, 2.0, Effect.DAMAGE_REDUCTION,
            "%s%% less damage taken",
            "Hide turned to hardwood."),
    SAP_MAGNET(Skill.WOODCUTTING, "Sap Magnet", PerkHeads.SAP_MAGNET, 42, 5, 10.0, Effect.MAGNET,
            "%s%% chance drops fly to your inventory",
            "Sticky fingers."),
    WOODSMAN(Skill.WOODCUTTING, "Woodsman", PerkHeads.WOODSMAN, 52, 3, 5.0, Effect.POINT_BONUS,
            "%s%% chance of a bonus skill point",
            "Learned from the grain."),
    GROVE_CACHE(Skill.WOODCUTTING, "Grove Cache", PerkHeads.GROVE_CACHE, 66, 3, 2.0, Effect.LOOT_CHEST,
            "%s%% chance to drop a loot chest of timber",
            "A hollow trunk spills its cache."),
    ANCIENT_GROWTH(Skill.WOODCUTTING, "Ancient Growth", PerkHeads.ANCIENT_GROWTH, 80, 3, 8.0, Effect.REGEN,
            "%s%% chance of Regeneration on a chop",
            "Old roots, old magic."),

    // --- Excavation ---
    TREASURE_HUNTER(Skill.EXCAVATION, "Treasure Hunter", PerkHeads.TREASURE_HUNTER, 1, 5, 2.5, Effect.TREASURE,
            "%s%% chance to unearth buried treasure",
            "Something glints in the dirt."),
    EARTHMOVER(Skill.EXCAVATION, "Earthmover", PerkHeads.EARTHMOVER, 5, 5, 6.0, Effect.HASTE,
            "%s%% chance of Haste II for 8s",
            "Move the ground itself."),
    SIFTER(Skill.EXCAVATION, "Sifter", PerkHeads.SIFTER, 10, 5, 4.0, Effect.DOUBLE_DROP,
            "%s%% chance to double dug drops",
            "Sift twice as fast."),
    RELIC_CHEST(Skill.EXCAVATION, "Relic Chest", PerkHeads.RELIC_CHEST, 16, 5, 2.0, Effect.LOOT_CHEST,
            "%s%% chance to drop a chest of relics",
            "A buried strongbox breaks the surface."),
    DIGGERS_LUCK(Skill.EXCAVATION, "Digger's Luck", PerkHeads.DIGGERS_LUCK, 24, 5, 5.0, Effect.XP_BOOST,
            "+%s%% Excavation XP",
            "Luck favours the shovel."),
    LIGHT_STEP(Skill.EXCAVATION, "Light Step", PerkHeads.LIGHT_STEP, 30, 3, 12.0, Effect.SPEED,
            "%s%% chance of Speed II for 8s",
            "Barely touching the ground."),
    DUST_CLOUD(Skill.EXCAVATION, "Dust Cloud", PerkHeads.DUST_CLOUD, 38, 5, 2.0, Effect.DAMAGE_REDUCTION,
            "%s%% less damage taken",
            "They cannot hit what they cannot see."),
    GRAVE_MAGNET(Skill.EXCAVATION, "Grave Magnet", PerkHeads.GRAVE_MAGNET, 48, 5, 10.0, Effect.MAGNET,
            "%s%% chance drops fly to your inventory",
            "The dirt hands it over."),
    SURVEYOR(Skill.EXCAVATION, "Surveyor", PerkHeads.SURVEYOR, 60, 3, 5.0, Effect.POINT_BONUS,
            "%s%% chance of a bonus skill point",
            "You read the land."),
    BURIED_KING(Skill.EXCAVATION, "Buried King", PerkHeads.BURIED_KING, 80, 3, 4.0, Effect.TREASURE,
            "+%s%% treasure chance",
            "Every grave has a crown in it."),

    // --- Farming ---
    BOUNTIFUL(Skill.FARMING, "Bountiful Harvest", PerkHeads.BOUNTIFUL, 1, 5, 4.0, Effect.DOUBLE_DROP,
            "%s%% chance to double crop yield",
            "The field pays double."),
    GREEN_THUMB(Skill.FARMING, "Green Thumb", PerkHeads.GREEN_THUMB, 6, 5, 12.0, Effect.REPLANT,
            "%s%% chance the crop replants itself",
            "Harvest without bending down."),
    REGROWTH(Skill.FARMING, "Regrowth", PerkHeads.REGROWTH, 12, 5, 8.0, Effect.FOOD,
            "%s%% chance to restore hunger on harvest",
            "Eat from the field as you work."),
    SCARECROW(Skill.FARMING, "Scarecrow", PerkHeads.SCARECROW, 20, 5, 2.0, Effect.DAMAGE_REDUCTION,
            "%s%% less damage taken",
            "Something in the field watches back."),
    FIELD_HANDS(Skill.FARMING, "Field Hands", PerkHeads.FIELD_HANDS, 28, 5, 8.0, Effect.HASTE,
            "%s%% chance of Haste II for 8s",
            "A dozen hands, one farmer."),
    HARVEST_MAGNET(Skill.FARMING, "Harvest Magnet", PerkHeads.HARVEST_MAGNET, 36, 5, 10.0, Effect.MAGNET,
            "%s%% chance drops fly to your inventory",
            "The crop comes to you."),
    HEARTY_MEAL(Skill.FARMING, "Hearty Meal", PerkHeads.HEARTY_MEAL, 46, 3, 10.0, Effect.REGEN,
            "%s%% chance of Regeneration on harvest",
            "A full belly mends fast."),
    FARMHAND(Skill.FARMING, "Farmhand", PerkHeads.FARMHAND, 56, 3, 5.0, Effect.POINT_BONUS,
            "%s%% chance of a bonus skill point",
            "Learned the hard way."),
    GOLDEN_FIELDS(Skill.FARMING, "Golden Fields", PerkHeads.GOLDEN_FIELDS, 68, 3, 8.0, Effect.XP_BOOST,
            "+%s%% Farming XP",
            "The whole field turns gold."),
    HARVEST_MOON(Skill.FARMING, "Harvest Moon", PerkHeads.HARVEST_MOON, 80, 3, 2.0, Effect.LOOT_CHEST,
            "%s%% chance to drop a chest of harvest",
            "The moon leaves a crate behind."),

    // --- Combat ---
    BRUTAL(Skill.COMBAT, "Brutal", PerkHeads.BRUTAL, 1, 5, 0.5, Effect.MELEE_DAMAGE,
            "+%s bonus melee damage",
            "Hit harder than you should."),
    LIFESTEAL(Skill.COMBAT, "Lifesteal", PerkHeads.LIFESTEAL, 6, 5, 0.5, Effect.LIFESTEAL,
            "Heal %s HP on a melee hit",
            "Their loss, your gain."),
    CRITICAL_EYE(Skill.COMBAT, "Critical Eye", PerkHeads.CRITICAL_EYE, 12, 5, 4.0, Effect.CRIT,
            "%s%% chance of a critical strike",
            "You see the gap in the guard."),
    THUNDER_STRIKE(Skill.COMBAT, "Thunder Strike", PerkHeads.THUNDER_STRIKE, 20, 5, 3.0, Effect.LIGHTNING,
            "%s%% chance to call lightning on a hit",
            "The sky answers your sword."),
    IRON_HIDE(Skill.COMBAT, "Iron Hide", PerkHeads.IRON_HIDE, 28, 5, 2.0, Effect.DAMAGE_REDUCTION,
            "%s%% less damage taken",
            "Armour under the armour."),
    ADRENALINE(Skill.COMBAT, "Adrenaline", PerkHeads.ADRENALINE, 36, 3, 14.0, Effect.SPEED,
            "%s%% chance of Speed II on a kill",
            "The fight is not over yet."),
    WOLF_PACK(Skill.COMBAT, "Wolf Pack", PerkHeads.WOLF_PACK, 45, 3, 6.0, Effect.SUMMON_WOLF,
            "%s%% chance a wolf joins you on a kill",
            "You never fight alone."),
    WARLORD(Skill.COMBAT, "Warlord", PerkHeads.WARLORD, 56, 3, 5.0, Effect.POINT_BONUS,
            "%s%% chance of a bonus skill point",
            "Command is its own reward."),
    SECOND_WIND(Skill.COMBAT, "Second Wind", PerkHeads.SECOND_WIND, 68, 3, 12.0, Effect.REGEN,
            "%s%% chance of Regeneration on a kill",
            "Get back up."),
    SPOILS_OF_WAR(Skill.COMBAT, "Spoils of War", PerkHeads.SPOILS_OF_WAR, 80, 3, 3.0, Effect.LOOT_CHEST,
            "%s%% chance to drop a chest of spoils",
            "The dead leave a crate behind."),

    // --- Archery ---
    PIERCING(Skill.ARCHERY, "Piercing", PerkHeads.PIERCING, 1, 5, 0.5, Effect.BOW_DAMAGE,
            "+%s bonus bow damage",
            "Straight through the plate."),
    QUIVER(Skill.ARCHERY, "Endless Quiver", PerkHeads.QUIVER, 6, 5, 12.0, Effect.ARROW_SAVE,
            "%s%% chance to recover a fired arrow",
            "The quiver never empties."),
    DEAD_EYE(Skill.ARCHERY, "Dead Eye", PerkHeads.DEAD_EYE, 12, 5, 4.0, Effect.CRIT,
            "%s%% chance of a critical arrow",
            "One eye closed, one shot fired."),
    ARROW_STORM(Skill.ARCHERY, "Arrow Storm", PerkHeads.ARROW_STORM, 20, 5, 4.0, Effect.ARROW_STORM,
            "%s%% chance to rain arrows on your target",
            "The sky fills with shafts."),
    MARKSMAN(Skill.ARCHERY, "Marksman", PerkHeads.MARKSMAN, 28, 5, 5.0, Effect.XP_BOOST,
            "+%s%% Archery XP",
            "Every shot is a lesson."),
    LIGHT_FOOT(Skill.ARCHERY, "Light Foot", PerkHeads.LIGHT_FOOT, 36, 3, 12.0, Effect.SPEED,
            "%s%% chance of Speed II on a hit",
            "Move after you loose."),
    WIND_READER(Skill.ARCHERY, "Wind Reader", PerkHeads.WIND_READER, 44, 5, 2.0, Effect.DAMAGE_REDUCTION,
            "%s%% less damage taken",
            "You feel the shot coming."),
    FLETCHER(Skill.ARCHERY, "Fletcher", PerkHeads.FLETCHER, 56, 3, 5.0, Effect.POINT_BONUS,
            "%s%% chance of a bonus skill point",
            "You make your own."),
    SIPHON_SHOT(Skill.ARCHERY, "Siphon Shot", PerkHeads.SIPHON_SHOT, 68, 3, 0.5, Effect.LIFESTEAL,
            "Heal %s HP on an arrow hit",
            "The arrow drinks."),
    STORM_VOLLEY(Skill.ARCHERY, "Storm Volley", PerkHeads.STORM_VOLLEY, 80, 3, 4.0, Effect.LIGHTNING,
            "%s%% chance to call lightning on a hit",
            "Every arrow is a storm."),

    // --- Fishing ---
    LUCKY_CATCH(Skill.FISHING, "Lucky Catch", PerkHeads.LUCKY_CATCH, 1, 5, 4.0, Effect.TREASURE,
            "%s%% chance for a bonus catch",
            "The line comes back heavy."),
    SEA_INSIGHT(Skill.FISHING, "Sea Insight", PerkHeads.SEA_INSIGHT, 6, 5, 6.0, Effect.XP_BOOST,
            "+%s%% Fishing XP",
            "The water tells you things."),
    DEEP_LUNGS(Skill.FISHING, "Deep Lungs", PerkHeads.DEEP_LUNGS, 12, 5, 20.0, Effect.WATER_BREATH,
            "%s%% chance of Water Breathing on a catch",
            "Stay under a while longer."),
    ANGLERS_FEAST(Skill.FISHING, "Angler's Feast", PerkHeads.ANGLERS_FEAST, 20, 5, 8.0, Effect.FOOD,
            "%s%% chance to restore hunger on a catch",
            "Dinner on the bank."),
    TREASURE_NET(Skill.FISHING, "Treasure Net", PerkHeads.TREASURE_NET, 28, 5, 3.0, Effect.LOOT_CHEST,
            "%s%% chance to reel in a loot chest",
            "A sunken strongbox on the hook."),
    TIDE_GUARD(Skill.FISHING, "Tide Guard", PerkHeads.TIDE_GUARD, 36, 5, 2.0, Effect.DAMAGE_REDUCTION,
            "%s%% less damage taken",
            "The tide takes the blow."),
    SWIFT_CURRENT(Skill.FISHING, "Swift Current", PerkHeads.SWIFT_CURRENT, 44, 3, 12.0, Effect.SPEED,
            "%s%% chance of Speed II on a catch",
            "Ride the current out."),
    HARBORMASTER(Skill.FISHING, "Harbormaster", PerkHeads.HARBORMASTER, 56, 3, 5.0, Effect.POINT_BONUS,
            "%s%% chance of a bonus skill point",
            "You run this dock."),
    SIRENS_GIFT(Skill.FISHING, "Siren's Gift", PerkHeads.SIRENS_GIFT, 68, 3, 12.0, Effect.REGEN,
            "%s%% chance of Regeneration on a catch",
            "Something down there likes you."),
    LEVIATHAN(Skill.FISHING, "Leviathan", PerkHeads.LEVIATHAN, 80, 3, 4.0, Effect.FISH_FRENZY,
            "%s%% chance the water erupts with fish",
            "Something enormous surfaces.");

    private final Skill skill;
    private final String display;
    private final String texture;
    private final int unlockLevel;
    private final int maxRank;
    private final double valuePerRank;
    private final Effect effect;
    private final String descTemplate;
    private final String flavour;

    Perk(Skill skill, String display, String texture, int unlockLevel, int maxRank,
         double valuePerRank, Effect effect, String descTemplate, String flavour) {
        this.skill = skill;
        this.display = display;
        this.texture = texture;
        this.unlockLevel = unlockLevel;
        this.maxRank = maxRank;
        this.valuePerRank = valuePerRank;
        this.effect = effect;
        this.descTemplate = descTemplate;
        this.flavour = flavour;
    }

    public Skill skill() {
        return skill;
    }

    public String display() {
        return display;
    }

    /** Base64 skin for this perk's custom head. */
    public String texture() {
        return texture;
    }

    public Effect effect() {
        return effect;
    }

    public String flavour() {
        return flavour;
    }

    public int unlockLevel() {
        return unlockLevel;
    }

    public int maxRank() {
        return maxRank;
    }

    public double valuePerRank() {
        return valuePerRank;
    }

    /** Effect strength at a given rank. */
    public double valueAt(int rank) {
        return valuePerRank * Math.max(0, rank);
    }

    /** Human readable effect line for a rank. */
    public String describe(int rank) {
        double v = valueAt(rank);
        String num = (v == Math.floor(v)) ? String.valueOf((int) v) : String.valueOf(v);
        return String.format(descTemplate, num);
    }

    /** Skill points needed to buy the next rank. */
    public int costFor(int nextRank) {
        return Math.max(1, nextRank);
    }

    public String key() {
        return name().toLowerCase();
    }

    /**
     * Real vanilla item for this perk's menu icon. Custom heads all resolved to
     * the same blank skull in game, so every perk reads as a recognisable item
     * instead. Heads are reserved for arrows and navigation only.
     */
    public org.bukkit.Material icon() {
        return switch (this) {
            // Mining
            case VEIN_FORTUNE -> org.bukkit.Material.RAW_IRON;
            case MINERS_HASTE -> org.bukkit.Material.GOLDEN_PICKAXE;
            case SMELTING_TOUCH -> org.bukkit.Material.BLAST_FURNACE;
            case MAGNETITE -> org.bukkit.Material.LODESTONE;
            case GEODE_BURST -> org.bukkit.Material.AMETHYST_CLUSTER;
            case STONE_SKIN -> org.bukkit.Material.DEEPSLATE_TILES;
            case MOTHERLODE -> org.bukkit.Material.CHEST;
            case PROSPECTOR -> org.bukkit.Material.BRUSH;
            case NIGHT_EYES -> org.bukkit.Material.GLOW_INK_SAC;
            case EARTHS_BLESSING -> org.bukkit.Material.ECHO_SHARD;

            // Woodcutting
            case TIMBER -> org.bukkit.Material.OAK_LOG;
            case LUMBER_FURY -> org.bukkit.Material.IRON_AXE;
            case TREEFELLER -> org.bukkit.Material.DIAMOND_AXE;
            case SAP_RUNNER -> org.bukkit.Material.HONEY_BOTTLE;
            case FOREST_STRIDE -> org.bukkit.Material.LEATHER_BOOTS;
            case BARK_HIDE -> org.bukkit.Material.OAK_WOOD;
            case SAP_MAGNET -> org.bukkit.Material.HONEYCOMB;
            case WOODSMAN -> org.bukkit.Material.STRIPPED_OAK_LOG;
            case GROVE_CACHE -> org.bukkit.Material.BARREL;
            case ANCIENT_GROWTH -> org.bukkit.Material.OAK_SAPLING;

            // Excavation
            case TREASURE_HUNTER -> org.bukkit.Material.GOLD_NUGGET;
            case EARTHMOVER -> org.bukkit.Material.IRON_SHOVEL;
            case SIFTER -> org.bukkit.Material.GRAVEL;
            case RELIC_CHEST -> org.bukkit.Material.DECORATED_POT;
            case DIGGERS_LUCK -> org.bukkit.Material.RABBIT_FOOT;
            case LIGHT_STEP -> org.bukkit.Material.FEATHER;
            case DUST_CLOUD -> org.bukkit.Material.SAND;
            case GRAVE_MAGNET -> org.bukkit.Material.BONE;
            case SURVEYOR -> org.bukkit.Material.MAP;
            case BURIED_KING -> org.bukkit.Material.GOLDEN_HELMET;

            // Farming
            case BOUNTIFUL -> org.bukkit.Material.WHEAT;
            case GREEN_THUMB -> org.bukkit.Material.BONE_MEAL;
            case REGROWTH -> org.bukkit.Material.WHEAT_SEEDS;
            case SCARECROW -> org.bukkit.Material.CARVED_PUMPKIN;
            case FIELD_HANDS -> org.bukkit.Material.IRON_HOE;
            case HARVEST_MAGNET -> org.bukkit.Material.HOPPER;
            case HEARTY_MEAL -> org.bukkit.Material.GOLDEN_CARROT;
            case FARMHAND -> org.bukkit.Material.COMPOSTER;
            case GOLDEN_FIELDS -> org.bukkit.Material.HAY_BLOCK;
            case HARVEST_MOON -> org.bukkit.Material.PUMPKIN_PIE;

            // Combat
            case BRUTAL -> org.bukkit.Material.IRON_SWORD;
            case LIFESTEAL -> org.bukkit.Material.GHAST_TEAR;
            case CRITICAL_EYE -> org.bukkit.Material.DIAMOND_SWORD;
            case THUNDER_STRIKE -> org.bukkit.Material.LIGHTNING_ROD;
            case IRON_HIDE -> org.bukkit.Material.IRON_CHESTPLATE;
            case ADRENALINE -> org.bukkit.Material.SUGAR;
            case WOLF_PACK -> org.bukkit.Material.BONE_BLOCK;
            case WARLORD -> org.bukkit.Material.NETHERITE_SWORD;
            case SECOND_WIND -> org.bukkit.Material.TOTEM_OF_UNDYING;
            case SPOILS_OF_WAR -> org.bukkit.Material.GOLD_INGOT;

            // Archery
            case PIERCING -> org.bukkit.Material.ARROW;
            case QUIVER -> org.bukkit.Material.TIPPED_ARROW;
            case DEAD_EYE -> org.bukkit.Material.TARGET;
            case ARROW_STORM -> org.bukkit.Material.CROSSBOW;
            case MARKSMAN -> org.bukkit.Material.BOW;
            case LIGHT_FOOT -> org.bukkit.Material.RABBIT_HIDE;
            case WIND_READER -> org.bukkit.Material.WHITE_BANNER;
            case FLETCHER -> org.bukkit.Material.FLETCHING_TABLE;
            case SIPHON_SHOT -> org.bukkit.Material.SPECTRAL_ARROW;
            case STORM_VOLLEY -> org.bukkit.Material.FIREWORK_ROCKET;

            // Fishing
            case LUCKY_CATCH -> org.bukkit.Material.COD;
            case SEA_INSIGHT -> org.bukkit.Material.NAUTILUS_SHELL;
            case DEEP_LUNGS -> org.bukkit.Material.TURTLE_HELMET;
            case ANGLERS_FEAST -> org.bukkit.Material.COOKED_SALMON;
            case TREASURE_NET -> org.bukkit.Material.HEART_OF_THE_SEA;
            case TIDE_GUARD -> org.bukkit.Material.SHIELD;
            case SWIFT_CURRENT -> org.bukkit.Material.PRISMARINE_CRYSTALS;
            case HARBORMASTER -> org.bukkit.Material.FISHING_ROD;
            case SIRENS_GIFT -> org.bukkit.Material.PUFFERFISH;
            case LEVIATHAN -> org.bukkit.Material.TRIDENT;
        };
    }

    /** The ten perks of a skill, in unlock order. */
    public static List<Perk> of(Skill skill) {
        List<Perk> out = new ArrayList<>();
        for (Perk p : values()) {
            if (p.skill == skill) {
                out.add(p);
            }
        }
        return out;
    }
}
