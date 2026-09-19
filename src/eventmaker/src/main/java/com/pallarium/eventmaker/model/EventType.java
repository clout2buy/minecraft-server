package com.pallarium.eventmaker.model;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.runtime.*;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.function.BiFunction;

public enum EventType {
    // --- originals ---
    METEOR_SHOWER("Meteor Shower", Material.FIRE_CHARGE,
            "Fireballs rain near players.", "Impacts drop reward items.", false,
            MeteorShower::new),
    BOSS_FIGHT("Boss Fight", Material.RAVAGER_SPAWN_EGG,
            "A buffed boss spawns at the location.", "Top damage dealer wins the rewards.", true,
            BossFight::new),
    KING_OF_THE_HILL("King of the Hill", Material.GOLDEN_HELMET,
            "Hold the zone at the location.", "Most seconds inside wins.", true,
            KingOfTheHill::new),
    TREASURE_HUNT("Treasure Hunt", Material.CHEST,
            "A loot chest is hidden within the radius.", "Hints every 45s. First to open it wins.", true,
            TreasureHunt::new),
    MOB_WAVE("Mob Wave", Material.ZOMBIE_HEAD,
            "Waves of mobs spawn around players.", "Most kills wins the rewards.", false,
            MobWave::new),
    DROP_PARTY("Drop Party", Material.FIREWORK_ROCKET,
            "Reward items rain down at the location.", "Everyone grabs what they can.", true,
            DropParty::new),

    // --- swarms (most kills wins) ---
    ZOMBIE_HORDE("Zombie Horde", Material.ROTTEN_FLESH,
            "Endless zombies shamble in.", "Most kills wins.", false,
            Swarm.of(false, 5, 20, EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER)),
    SKELETON_SIEGE("Skeleton Siege", Material.BOW,
            "Archers surround every player.", "Most kills wins.", false,
            Swarm.of(false, 4, 20, EntityType.SKELETON, EntityType.STRAY)),
    CREEPER_CHAOS("Creeper Chaos", Material.CREEPER_HEAD,
            "Creepers. Lots of creepers.", "Most kills wins.", false,
            Swarm.of(false, 3, 25, EntityType.CREEPER)),
    SPIDER_NEST("Spider Nest", Material.STRING,
            "Spiders and cave spiders swarm.", "Most kills wins.", false,
            Swarm.of(false, 5, 18, EntityType.SPIDER, EntityType.CAVE_SPIDER)),
    RAID("Village Raid", Material.CROSSBOW,
            "Pillagers, vindicators and ravagers.", "Most kills wins.", false,
            Swarm.of(false, 3, 25, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.RAVAGER)),
    NETHER_INVASION("Nether Invasion", Material.NETHERRACK,
            "Blazes, piglins and hoglins pour in.", "Most kills wins.", false,
            Swarm.of(false, 3, 22, EntityType.BLAZE, EntityType.PIGLIN_BRUTE, EntityType.HOGLIN, EntityType.WITHER_SKELETON)),
    PHANTOM_NIGHT("Phantom Night", Material.PHANTOM_MEMBRANE,
            "Phantoms dive from the sky.", "Most kills wins.", false,
            Swarm.of(true, 3, 15, EntityType.PHANTOM)),
    END_BREACH("End Breach", Material.ENDER_PEARL,
            "Endermen and shulkers slip through.", "Most kills wins.", false,
            Swarm.of(false, 3, 25, EntityType.ENDERMAN, EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.SHULKER)),
    SLIME_RAIN("Slime Rain", Material.SLIME_BALL,
            "Slimes fall from the sky.", "Most kills wins.", false,
            Swarm.of(true, 4, 10, EntityType.SLIME, EntityType.MAGMA_CUBE)),
    WITCH_COVEN("Witch Coven", Material.BREWING_STAND,
            "Witches and vexes harass everyone.", "Most kills wins.", false,
            Swarm.of(false, 2, 25, EntityType.WITCH, EntityType.VEX)),
    ANIMAL_STAMPEDE("Animal Stampede", Material.LEATHER,
            "Cows, pigs and sheep everywhere.", "Most kills wins. Free food!", false,
            Swarm.of(false, 6, 15, EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN)),
    CHICKEN_APOCALYPSE("Chicken Apocalypse", Material.EGG,
            "It's raining chickens.", "Most kills wins.", false,
            Swarm.of(true, 8, 5, EntityType.CHICKEN)),

    // --- races (most X wins) ---
    MINING_FRENZY("Mining Frenzy", Material.IRON_PICKAXE,
            "Mine as many blocks as you can.", "Most blocks mined wins.", false,
            ScoreRace.of(ScoreRace.Kind.MINING)),
    ORE_RUSH("Ore Rush", Material.DIAMOND_ORE,
            "Find and mine ores.", "Most ores mined wins.", false,
            ScoreRace.of(ScoreRace.Kind.ORE)),
    LUMBERJACK("Lumberjack", Material.IRON_AXE,
            "Chop down as many logs as you can.", "Most logs wins.", false,
            ScoreRace.of(ScoreRace.Kind.LOGS)),
    FISHING_DERBY("Fishing Derby", Material.FISHING_ROD,
            "Cast your line and reel them in.", "Most catches wins.", false,
            ScoreRace.of(ScoreRace.Kind.FISH)),
    PVP_FRENZY("PvP Frenzy", Material.DIAMOND_SWORD,
            "Free for all. Hunt each other.", "Most player kills wins.", false,
            ScoreRace.of(ScoreRace.Kind.PVP)),
    MARATHON("Marathon", Material.LEATHER_BOOTS,
            "Run. Just run.", "Most distance travelled wins.", false,
            ScoreRace.of(ScoreRace.Kind.DISTANCE)),
    BUILD_BATTLE("Build Battle", Material.BRICKS,
            "Place as many blocks as you can.", "Most blocks placed wins.", false,
            ScoreRace.of(ScoreRace.Kind.BUILD)),
    XP_GRIND("XP Grind", Material.EXPERIENCE_BOTTLE,
            "Earn experience any way you like.", "Most XP gained wins.", false,
            ScoreRace.of(ScoreRace.Kind.XP)),

    // --- hazards ---
    LIGHTNING_STORM("Lightning Storm", Material.LIGHTNING_ROD,
            "Lightning strikes around players.", "Least damage taken wins.", false,
            SkyHazard.of(SkyHazard.Kind.LIGHTNING)),
    ANVIL_RAIN("Anvil Rain", Material.ANVIL,
            "Anvils fall from the sky.", "Least damage taken wins.", false,
            SkyHazard.of(SkyHazard.Kind.ANVILS)),
    BOUNTY("Bounty", Material.WITHER_SKELETON_SKULL,
            "One random player is marked.", "Kill them to claim, or survive to keep.", false,
            Bounty::new),

    // --- buffs / freebies (everyone gets rewards) ---
    SPEED_HOUR("Speed Hour", Material.SUGAR,
            "Everyone gets Speed II and Haste.", "Rewards to everyone at the end.", false,
            EffectStorm.of(PotionEffectType.SPEED, 1, PotionEffectType.FAST_DIGGING, 1)),
    LOW_GRAVITY("Low Gravity", Material.FEATHER,
            "Jump Boost IV and Slow Falling.", "Rewards to everyone at the end.", false,
            EffectStorm.of(PotionEffectType.JUMP, 3, PotionEffectType.SLOW_FALLING, 0)),
    TITAN_MODE("Titan Mode", Material.NETHERITE_CHESTPLATE,
            "Strength, Resistance and Regen.", "Rewards to everyone at the end.", false,
            EffectStorm.of(PotionEffectType.INCREASE_DAMAGE, 1, PotionEffectType.DAMAGE_RESISTANCE, 1, PotionEffectType.REGENERATION, 0)),
    DARKNESS("Darkness", Material.BLACK_CANDLE,
            "Blindness and Darkness for all.", "Rewards to everyone who endures.", false,
            EffectStorm.of(PotionEffectType.DARKNESS, 0, PotionEffectType.BLINDNESS, 0)),
    XP_FOUNTAIN("XP Fountain", Material.LIME_DYE,
            "XP orbs pour out at the location.", "Everyone grabs what they can.", true,
            XpFountain::new);

    public final String label;
    public final Material icon;
    public final String line1;
    public final String line2;
    private final boolean needsLocation;
    private final BiFunction<EventMakerPlugin, GameEvent, ActiveEvent> factory;

    EventType(String label, Material icon, String line1, String line2, boolean needsLocation,
              BiFunction<EventMakerPlugin, GameEvent, ActiveEvent> factory) {
        this.label = label;
        this.icon = icon;
        this.line1 = line1;
        this.line2 = line2;
        this.needsLocation = needsLocation;
        this.factory = factory;
    }

    public ActiveEvent create(EventMakerPlugin plugin, GameEvent event) {
        return factory.apply(plugin, event);
    }

    public EventType next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public EventType prev() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    public boolean needsLocation() {
        return needsLocation;
    }
}
