package com.pallarium.endgame.event;

import com.pallarium.endgame.mob.EliteTier;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Arrays;
import java.util.List;

/**
 * The event catalogue. Each entry is a multi stage chain with its own trigger,
 * mobs, tier ceiling, set piece and reward weight. Stages run in order; failing
 * a timer fails the whole chain.
 */
public enum WorldEvent {

    /* ---------------------------------------------------------------- */
    /*  DIGGING                                                          */
    /* ---------------------------------------------------------------- */

    BREACH("The Breach",
            "Your picks went too deep. Something came up through the hole.",
            Trigger.DIGGING, EliteTier.SAVAGE, 40,
            TextColor.fromHexString("#22D3EE"), Material.DEEPSLATE, SetPiece.NONE,
            new EntityType[]{EntityType.SILVERFISH, EntityType.CAVE_SPIDER, EntityType.ZOMBIE},
            List.of(
                    EventStage.kill("Seal the swarm", "Kill what crawled out", 12, 120),
                    EventStage.boss("The thing below", "Kill the Warden of the Breach", 150))),

    VEIN_CURSE("The Greedy Vein",
            "The ore bleeds. It wants paying back.",
            Trigger.DIGGING, EliteTier.ELITE, 30,
            TextColor.fromHexString("#FACC15"), Material.GOLD_ORE, SetPiece.NONE,
            new EntityType[]{EntityType.HUSK, EntityType.ZOMBIE, EntityType.SKELETON},
            List.of(
                    EventStage.brk("Strip the cursed rock", "Break the glowing blocks", 8, 90),
                    EventStage.defend("Hold the shaft", "Survive the collapse", 45))),

    SOUL_QUARRY("The Soul Quarry",
            "Every block you pulled had something in it. It wants out.",
            Trigger.DIGGING, EliteTier.CHAMPION, 34,
            TextColor.fromHexString("#C084FC"), Material.SOUL_SAND, SetPiece.BRAZIERS,
            new EntityType[]{EntityType.WITHER_SKELETON, EntityType.SKELETON, EntityType.ZOMBIE},
            List.of(
                    EventStage.ritual("Light the old marks", "Stand on each brazier", 5, 130),
                    EventStage.collect("Gather what tore loose", "Pick up the essences", 10, 120),
                    EventStage.boss("The Quarry Master", "Kill the Master", 150))),

    /* ---------------------------------------------------------------- */
    /*  LOGGING                                                          */
    /* ---------------------------------------------------------------- */

    ROOTWAKE("Rootwake",
            "The treeline has had enough of your axe.",
            Trigger.LOGGING, EliteTier.SAVAGE, 35,
            TextColor.fromHexString("#4ADE80"), Material.OAK_LOG, SetPiece.NONE,
            new EntityType[]{EntityType.SPIDER, EntityType.ZOMBIE, EntityType.WITCH},
            List.of(
                    EventStage.kill("Cut down the wakened", "Kill the angry wood", 10, 110),
                    EventStage.boss("The Elder Trunk", "Fell the Elder", 140))),

    SAP_THIEF("The Sap Thief",
            "Something small just robbed the grove and ran.",
            Trigger.LOGGING, EliteTier.ELITE, 45,
            TextColor.fromHexString("#FB923C"), Material.HONEYCOMB, SetPiece.NONE,
            new EntityType[]{EntityType.SPIDER, EntityType.SILVERFISH, EntityType.ZOMBIE},
            List.of(
                    EventStage.chase("Run it down", "Catch the thief before it clears the ring", 70),
                    EventStage.kill("It brought company", "Kill the nest", 12, 110))),

    /* ---------------------------------------------------------------- */
    /*  SLAUGHTER                                                        */
    /* ---------------------------------------------------------------- */

    WARBAND("The Warband",
            "Word of your killing spread. They brought friends.",
            Trigger.SLAUGHTER, EliteTier.CHAMPION, 45,
            TextColor.fromHexString("#F87171"), Material.IRON_SWORD, SetPiece.NONE,
            new EntityType[]{EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.SKELETON},
            List.of(
                    EventStage.kill("Break the first rank", "Kill the raiders", 14, 130),
                    EventStage.kill("Break the second rank", "They keep coming", 16, 130),
                    EventStage.boss("The Warchief", "Kill the Warchief", 160))),

    BLOOD_MOON("Blood Debt",
            "Too much blood in one place. The dark answers.",
            Trigger.SLAUGHTER, EliteTier.ELITE, 30,
            TextColor.fromHexString("#A78BFA"), Material.REDSTONE, SetPiece.NONE,
            new EntityType[]{EntityType.PHANTOM, EntityType.SKELETON, EntityType.ZOMBIE},
            List.of(
                    EventStage.defend("Outlast the debt", "Stay alive in the circle", 60),
                    EventStage.kill("Collect the interest", "Kill what remains", 8, 90))),

    GALLOWS("The Gallows Cage",
            "They caged one of their own out here. It has been screaming for hours.",
            Trigger.SLAUGHTER, EliteTier.CHAMPION, 36,
            TextColor.fromHexString("#FACC15"), Material.IRON_BARS, SetPiece.CAGE,
            new EntityType[]{EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.WITCH},
            List.of(
                    EventStage.boss("Kill the keeper", "Drop the one holding the key", 120),
                    EventStage.escort("Walk it out", "Get the captive to the beacon", 170))),

    /* ---------------------------------------------------------------- */
    /*  HARVEST                                                          */
    /* ---------------------------------------------------------------- */

    BLIGHT("The Blight",
            "Something is eating the crop, and it is not you.",
            Trigger.HARVEST, EliteTier.SAVAGE, 30,
            TextColor.fromHexString("#FB923C"), Material.WHEAT, SetPiece.ALTAR,
            new EntityType[]{EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.SILVERFISH},
            List.of(
                    EventStage.kill("Burn the blight", "Kill the infested", 10, 100),
                    EventStage.deliver("Cleanse the soil", "Drop wheat on the altar", 12, 90))),

    SCARECROW("The Standing Man",
            "The scarecrow moved. You are sure of it now.",
            Trigger.HARVEST, EliteTier.ELITE, 32,
            TextColor.fromHexString("#4ADE80"), Material.HAY_BLOCK, SetPiece.SPIRE,
            new EntityType[]{EntityType.HUSK, EntityType.ZOMBIE, EntityType.SPIDER},
            List.of(
                    EventStage.protect("Keep the totem standing", "Hold them off the spire", 75),
                    EventStage.boss("It comes down itself", "Kill the Standing Man", 140))),

    /* ---------------------------------------------------------------- */
    /*  TIDE                                                             */
    /* ---------------------------------------------------------------- */

    DROWNED_TIDE("The Drowned Tide",
            "The water gave something back.",
            Trigger.TIDE, EliteTier.ELITE, 30,
            TextColor.fromHexString("#60A5FA"), Material.PRISMARINE_SHARD, SetPiece.NONE,
            new EntityType[]{EntityType.DROWNED, EntityType.GUARDIAN, EntityType.DROWNED},
            List.of(
                    EventStage.kill("Push back the tide", "Kill the drowned", 12, 120),
                    EventStage.boss("The Tidecaller", "Kill the Tidecaller", 140))),

    SUNKEN_RITE("The Sunken Rite",
            "Lanterns under the water, arranged by somebody.",
            Trigger.TIDE, EliteTier.CHAMPION, 34,
            TextColor.fromHexString("#22D3EE"), Material.SEA_LANTERN, SetPiece.BRAZIERS,
            new EntityType[]{EntityType.DROWNED, EntityType.GUARDIAN, EntityType.WITCH},
            List.of(
                    EventStage.ritual("Relight the rite", "Stand on each brazier", 6, 140),
                    EventStage.collect("Take the offerings", "Pick up the essences", 8, 110))),

    /* ---------------------------------------------------------------- */
    /*  DREAD                                                            */
    /* ---------------------------------------------------------------- */

    NIGHT_COURT("The Night Court",
            "You stood too long in the dark and it noticed you back.",
            Trigger.DREAD, EliteTier.CHAMPION, 40,
            TextColor.fromHexString("#C084FC"), Material.SOUL_LANTERN, SetPiece.BRAZIERS,
            new EntityType[]{EntityType.SKELETON, EntityType.PHANTOM, EntityType.WITCH},
            List.of(
                    EventStage.kill("Scatter the court", "Kill the courtiers", 12, 120),
                    EventStage.boss("The Pale Sovereign", "Kill the Sovereign", 160))),

    LOST_SOUL("The Lost Soul",
            "Something frightened is trying to get home.",
            Trigger.DREAD, EliteTier.SAVAGE, 30,
            TextColor.fromHexString("#D1D5DB"), Material.SOUL_SAND, SetPiece.SPIRE,
            new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER},
            List.of(
                    EventStage.escort("Walk it home", "Keep the soul alive to the beacon", 180))),

    HARVESTER("The Harvester",
            "It is taking souls and it has not finished counting.",
            Trigger.DREAD, EliteTier.NIGHTMARE, 38,
            TextColor.fromHexString("#F87171"), Material.WITHER_SKELETON_SKULL, SetPiece.ALTAR,
            new EntityType[]{EntityType.WITHER_SKELETON, EntityType.PHANTOM, EntityType.SKELETON},
            List.of(
                    EventStage.collect("Take them back", "Pick up the stolen essences", 12, 130),
                    EventStage.protect("Guard the altar", "It wants them returned", 70),
                    EventStage.boss("The Harvester", "Kill it", 170))),

    /* ---------------------------------------------------------------- */
    /*  AMBIENT                                                          */
    /* ---------------------------------------------------------------- */

    CARAVAN("The Broken Caravan",
            "Someone else's bad day is your opportunity.",
            Trigger.AMBIENT, EliteTier.ELITE, 25,
            TextColor.fromHexString("#22D3EE"), Material.CHEST, SetPiece.CARAVAN,
            new EntityType[]{EntityType.PILLAGER, EntityType.ZOMBIE, EntityType.SKELETON},
            List.of(
                    EventStage.kill("Drive off the looters", "Kill the looters", 10, 110),
                    EventStage.defend("Guard the wreck", "Hold the site", 45))),

    METEOR("Fallen Star",
            "Something hit the ground hard and it is still hot.",
            Trigger.AMBIENT, EliteTier.CHAMPION, 35,
            TextColor.fromHexString("#FACC15"), Material.MAGMA_BLOCK, SetPiece.CRATER,
            new EntityType[]{EntityType.BLAZE, EntityType.MAGMA_CUBE, EntityType.ZOMBIE},
            List.of(
                    EventStage.brk("Crack the shell", "Break the molten crust", 6, 80),
                    EventStage.boss("What rode it down", "Kill the Starborn", 150))),

    COLLECTOR("The Collector",
            "It takes one thing from every place it visits. This is your place.",
            Trigger.AMBIENT, EliteTier.NIGHTMARE, 42,
            TextColor.fromHexString("#A78BFA"), Material.ENDER_EYE, SetPiece.ALTAR,
            new EntityType[]{EntityType.ENDERMAN, EntityType.PHANTOM, EntityType.WITHER_SKELETON},
            List.of(
                    EventStage.chase("Do not let it leave", "Catch the Collector", 80),
                    EventStage.deliver("Pay the toll instead", "Drop gold on the altar", 8, 90),
                    EventStage.boss("It refuses the toll", "Kill the Collector", 180)));

    /** The set piece built at the event site, torn down when it ends. */
    public enum SetPiece { NONE, ALTAR, BRAZIERS, CRATER, CARAVAN, SPIRE, CAGE }

    private final String title;
    private final String flavour;
    private final Trigger trigger;
    private final EliteTier ceiling;
    private final int radius;
    private final TextColor color;
    private final Material icon;
    private final SetPiece setPiece;
    private final EntityType[] mobs;
    private final List<EventStage> stages;

    WorldEvent(String title, String flavour, Trigger trigger, EliteTier ceiling, int radius,
               TextColor color, Material icon, SetPiece setPiece, EntityType[] mobs,
               List<EventStage> stages) {
        this.title = title;
        this.flavour = flavour;
        this.trigger = trigger;
        this.ceiling = ceiling;
        this.radius = radius;
        this.color = color;
        this.icon = icon;
        this.setPiece = setPiece;
        this.mobs = mobs;
        this.stages = stages;
    }

    public String title() {
        return title;
    }

    public String flavour() {
        return flavour;
    }

    public Trigger trigger() {
        return trigger;
    }

    public EliteTier ceiling() {
        return ceiling;
    }

    public int radius() {
        return radius;
    }

    public TextColor color() {
        return color;
    }

    public Material icon() {
        return icon;
    }

    public SetPiece setPiece() {
        return setPiece;
    }

    public EntityType randomMob() {
        return mobs[java.util.concurrent.ThreadLocalRandom.current().nextInt(mobs.length)];
    }

    public List<EventStage> stages() {
        return stages;
    }

    /** All events that answer to a given trigger. */
    public static List<WorldEvent> forTrigger(Trigger trigger) {
        return Arrays.stream(values()).filter(e -> e.trigger == trigger).toList();
    }

    /** The item this event asks for on a DELIVER stage. */
    public Material deliverItem() {
        return switch (this) {
            case BLIGHT -> Material.WHEAT;
            case COLLECTOR -> Material.GOLD_INGOT;
            default -> Material.GOLD_INGOT;
        };
    }
}
