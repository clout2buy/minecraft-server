import json, io

heads = []
for f in ['heads-decoration.json', 'heads-misc.json', 'heads-alpha.json']:
    heads += json.load(open(f, encoding='utf-8'))
idx = {}
for h in heads:
    idx.setdefault(h['name'], h['value'])
names = list(idx)


def find(cands):
    for c in cands:
        if c in idx:
            return c
    for c in cands:
        for n in names:
            if c.lower() == n.lower():
                return n
    for c in cands:
        for n in sorted(names, key=len):
            if c.lower() in n.lower():
                return n
    raise SystemExit('NO HEAD FOR ' + repr(cands))


# skill, KEY, display, head candidates, unlock, maxRank, valuePerRank, effect, desc, flavour
P = [
 ("MINING","VEIN_FORTUNE","Vein Fortune",["Emerald Ore Chest"],1,5,4.0,"DOUBLE_DROP",
  "%s%% chance to double ore and stone drops","The vein gives twice."),
 ("MINING","MINERS_HASTE","Miner's Haste",["Mining Light"],5,5,6.0,"HASTE",
  "%s%% chance of Haste II for 8s","Swing until the stone gives up."),
 ("MINING","SMELTING_TOUCH","Smelting Touch",["Blast Furnace (lit)"],10,5,8.0,"AUTO_SMELT",
  "%s%% chance ore drops as finished ingots","Skip the furnace entirely."),
 ("MINING","MAGNETITE","Magnetite",["Magnet"],15,5,10.0,"MAGNET",
  "%s%% chance drops fly to your inventory","Nothing is left on the floor."),
 ("MINING","GEODE_BURST","Geode Burst",["Amethyst Crystals"],22,5,3.0,"ORE_BURST",
  "%s%% chance the block erupts with extra ore","The rock splits and pays out."),
 ("MINING","STONE_SKIN","Stone Skin",["Blackstone Shield"],30,5,2.0,"DAMAGE_REDUCTION",
  "%s%% less damage taken","Skin like the deepslate."),
 ("MINING","MOTHERLODE","Motherlode",["Diamond Chalice with Pickaxe"],40,3,2.0,"LOOT_CHEST",
  "%s%% chance to drop a loot chest of ore","A chest slams down in front of you."),
 ("MINING","PROSPECTOR","Prospector",["Compass"],50,3,5.0,"POINT_BONUS",
  "%s%% chance of a bonus skill point","You know where to look."),
 ("MINING","NIGHT_EYES","Night Eyes",["Eye of Ender"],62,3,20.0,"NIGHT_VISION",
  "%s%% chance of Night Vision underground","No torch needed."),
 ("MINING","EARTHS_BLESSING","Earth's Blessing",["Blessing Orb"],80,3,8.0,"REGEN",
  "%s%% chance of Regeneration on a break","The mountain heals its own."),

 ("WOODCUTTING","TIMBER","Timber",["Axe on a Stump (oak log)"],1,5,4.0,"DOUBLE_DROP",
  "%s%% chance to double log drops","One swing, two logs."),
 ("WOODCUTTING","LUMBER_FURY","Lumber Fury",["Axe"],5,5,6.0,"HASTE",
  "%s%% chance of Haste II for 8s","The axe does not slow down."),
 ("WOODCUTTING","TREEFELLER","Treefeller",["Chainsaw"],12,5,6.0,"TIMBER_FELL",
  "%s%% chance the whole tree falls at once","Break one log, drop them all."),
 ("WOODCUTTING","SAP_RUNNER","Sap Runner",["Bag Of Honey"],18,5,5.0,"XP_BOOST",
  "+%s%% Woodcutting XP","Every cut teaches you more."),
 ("WOODCUTTING","FOREST_STRIDE","Forest Stride",["Boots of Swiftness"],25,3,12.0,"SPEED",
  "%s%% chance of Speed II for 8s","The forest moves aside."),
 ("WOODCUTTING","BARK_HIDE","Bark Hide",["Crimson Shield"],32,5,2.0,"DAMAGE_REDUCTION",
  "%s%% less damage taken","Hide turned to hardwood."),
 ("WOODCUTTING","SAP_MAGNET","Sap Magnet",["Magnet"],42,5,10.0,"MAGNET",
  "%s%% chance drops fly to your inventory","Sticky fingers."),
 ("WOODCUTTING","WOODSMAN","Woodsman",["Compasses"],52,3,5.0,"POINT_BONUS",
  "%s%% chance of a bonus skill point","Learned from the grain."),
 ("WOODCUTTING","GROVE_CACHE","Grove Cache",["Basket with Logs"],66,3,2.0,"LOOT_CHEST",
  "%s%% chance to drop a loot chest of timber","A hollow trunk spills its cache."),
 ("WOODCUTTING","ANCIENT_GROWTH","Ancient Growth",["Ancient Books"],80,3,8.0,"REGEN",
  "%s%% chance of Regeneration on a chop","Old roots, old magic."),

 ("EXCAVATION","TREASURE_HUNTER","Treasure Hunter",["Blessing Orb"],1,5,2.5,"TREASURE",
  "%s%% chance to unearth buried treasure","Something glints in the dirt."),
 ("EXCAVATION","EARTHMOVER","Earthmover",["Wooden Crate Shovel"],5,5,6.0,"HASTE",
  "%s%% chance of Haste II for 8s","Move the ground itself."),
 ("EXCAVATION","SIFTER","Sifter",["Barrel with Sand"],10,5,4.0,"DOUBLE_DROP",
  "%s%% chance to double dug drops","Sift twice as fast."),
 ("EXCAVATION","RELIC_CHEST","Relic Chest",["Pile of Scrolls"],16,5,2.0,"LOOT_CHEST",
  "%s%% chance to drop a chest of relics","A buried strongbox breaks the surface."),
 ("EXCAVATION","DIGGERS_LUCK","Digger's Luck",["Bag Of Bones"],24,5,5.0,"XP_BOOST",
  "+%s%% Excavation XP","Luck favours the shovel."),
 ("EXCAVATION","LIGHT_STEP","Light Step",["Feather"],30,3,12.0,"SPEED",
  "%s%% chance of Speed II for 8s","Barely touching the ground."),
 ("EXCAVATION","DUST_CLOUD","Dust Cloud",["Cloud"],38,5,2.0,"DAMAGE_REDUCTION",
  "%s%% less damage taken","They cannot hit what they cannot see."),
 ("EXCAVATION","GRAVE_MAGNET","Grave Magnet",["Magnet"],48,5,10.0,"MAGNET",
  "%s%% chance drops fly to your inventory","The dirt hands it over."),
 ("EXCAVATION","SURVEYOR","Surveyor",["Recovery Compass"],60,3,5.0,"POINT_BONUS",
  "%s%% chance of a bonus skill point","You read the land."),
 ("EXCAVATION","BURIED_KING","Buried King",["Crown"],80,3,4.0,"TREASURE",
  "+%s%% treasure chance","Every grave has a crown in it."),

 ("FARMING","BOUNTIFUL","Bountiful Harvest",["Hay Bale"],1,5,4.0,"DOUBLE_DROP",
  "%s%% chance to double crop yield","The field pays double."),
 ("FARMING","GREEN_THUMB","Green Thumb",["Potted Sapling"],6,5,12.0,"REPLANT",
  "%s%% chance the crop replants itself","Harvest without bending down."),
 ("FARMING","REGROWTH","Regrowth",["Basket with Carrots"],12,5,8.0,"FOOD",
  "%s%% chance to restore hunger on harvest","Eat from the field as you work."),
 ("FARMING","SCARECROW","Scarecrow",["Scarecrow"],20,5,2.0,"DAMAGE_REDUCTION",
  "%s%% less damage taken","Something in the field watches back."),
 ("FARMING","FIELD_HANDS","Field Hands",["Barrel of Wheat"],28,5,8.0,"HASTE",
  "%s%% chance of Haste II for 8s","A dozen hands, one farmer."),
 ("FARMING","HARVEST_MAGNET","Harvest Magnet",["Magnet"],36,5,10.0,"MAGNET",
  "%s%% chance drops fly to your inventory","The crop comes to you."),
 ("FARMING","HEARTY_MEAL","Hearty Meal",["Bread"],46,3,10.0,"REGEN",
  "%s%% chance of Regeneration on harvest","A full belly mends fast."),
 ("FARMING","FARMHAND","Farmhand",["Basket with Potatoes"],56,3,5.0,"POINT_BONUS",
  "%s%% chance of a bonus skill point","Learned the hard way."),
 ("FARMING","GOLDEN_FIELDS","Golden Fields",["Golden Carrot"],68,3,8.0,"XP_BOOST",
  "+%s%% Farming XP","The whole field turns gold."),
 ("FARMING","HARVEST_MOON","Harvest Moon",["Blood Moon"],80,3,2.0,"LOOT_CHEST",
  "%s%% chance to drop a chest of harvest","The moon leaves a crate behind."),

 ("COMBAT","BRUTAL","Brutal",["Diamond Chalice with Sword"],1,5,0.5,"MELEE_DAMAGE",
  "+%s bonus melee damage","Hit harder than you should."),
 ("COMBAT","LIFESTEAL","Lifesteal",["Copper Heart"],6,5,0.5,"LIFESTEAL",
  "Heal %s HP on a melee hit","Their loss, your gain."),
 ("COMBAT","CRITICAL_EYE","Critical Eye",["Big Eye Jar"],12,5,4.0,"CRIT",
  "%s%% chance of a critical strike","You see the gap in the guard."),
 ("COMBAT","THUNDER_STRIKE","Thunder Strike",["Lightning"],20,5,3.0,"LIGHTNING",
  "%s%% chance to call lightning on a hit","The sky answers your sword."),
 ("COMBAT","IRON_HIDE","Iron Hide",["Army Helmet"],28,5,2.0,"DAMAGE_REDUCTION",
  "%s%% less damage taken","Armour under the armour."),
 ("COMBAT","ADRENALINE","Adrenaline",["Beating Heart"],36,3,14.0,"SPEED",
  "%s%% chance of Speed II on a kill","The fight is not over yet."),
 ("COMBAT","WOLF_PACK","Wolf Pack",["Wolf"],45,3,6.0,"SUMMON_WOLF",
  "%s%% chance a wolf joins you on a kill","You never fight alone."),
 ("COMBAT","WARLORD","Warlord",["Generals Medallion"],56,3,5.0,"POINT_BONUS",
  "%s%% chance of a bonus skill point","Command is its own reward."),
 ("COMBAT","SECOND_WIND","Second Wind",["Eternal Flame Ring"],68,3,12.0,"REGEN",
  "%s%% chance of Regeneration on a kill","Get back up."),
 ("COMBAT","SPOILS_OF_WAR","Spoils of War",["Animal Skull"],80,3,3.0,"LOOT_CHEST",
  "%s%% chance to drop a chest of spoils","The dead leave a crate behind."),

 ("ARCHERY","PIERCING","Piercing",["Archery Trophy"],1,5,0.5,"BOW_DAMAGE",
  "+%s bonus bow damage","Straight through the plate."),
 ("ARCHERY","QUIVER","Endless Quiver",["Quiver"],6,5,12.0,"ARROW_SAVE",
  "%s%% chance to recover a fired arrow","The quiver never empties."),
 ("ARCHERY","DEAD_EYE","Dead Eye",["Awakened Eye Backpack"],12,5,4.0,"CRIT",
  "%s%% chance of a critical arrow","One eye closed, one shot fired."),
 ("ARCHERY","ARROW_STORM","Arrow Storm",["Cupids Quiver"],20,5,4.0,"ARROW_STORM",
  "%s%% chance to rain arrows on your target","The sky fills with shafts."),
 ("ARCHERY","MARKSMAN","Marksman",["Hay Target"],28,5,5.0,"XP_BOOST",
  "+%s%% Archery XP","Every shot is a lesson."),
 ("ARCHERY","LIGHT_FOOT","Light Foot",["Ruffled Feathers"],36,3,12.0,"SPEED",
  "%s%% chance of Speed II on a hit","Move after you loose."),
 ("ARCHERY","WIND_READER","Wind Reader",["Cloud Block"],44,5,2.0,"DAMAGE_REDUCTION",
  "%s%% less damage taken","You feel the shot coming."),
 ("ARCHERY","FLETCHER","Fletcher",["Arrow"],56,3,5.0,"POINT_BONUS",
  "%s%% chance of a bonus skill point","You make your own."),
 ("ARCHERY","SIPHON_SHOT","Siphon Shot",["Black Heart"],68,3,0.5,"LIFESTEAL",
  "Heal %s HP on an arrow hit","The arrow drinks."),
 ("ARCHERY","STORM_VOLLEY","Storm Volley",["Lightning Card"],80,3,4.0,"LIGHTNING",
  "%s%% chance to call lightning on a hit","Every arrow is a storm."),

 ("FISHING","LUCKY_CATCH","Lucky Catch",["Fishing Trophy"],1,5,4.0,"TREASURE",
  "%s%% chance for a bonus catch","The line comes back heavy."),
 ("FISHING","SEA_INSIGHT","Sea Insight",["Crystal Orb"],6,5,6.0,"XP_BOOST",
  "+%s%% Fishing XP","The water tells you things."),
 ("FISHING","DEEP_LUNGS","Deep Lungs",["Ammonite Shell"],12,5,20.0,"WATER_BREATH",
  "%s%% chance of Water Breathing on a catch","Stay under a while longer."),
 ("FISHING","ANGLERS_FEAST","Angler's Feast",["Bag Of Fish"],20,5,8.0,"FOOD",
  "%s%% chance to restore hunger on a catch","Dinner on the bank."),
 ("FISHING","TREASURE_NET","Treasure Net",["Scroll in a Bottle"],28,5,3.0,"LOOT_CHEST",
  "%s%% chance to reel in a loot chest","A sunken strongbox on the hook."),
 ("FISHING","TIDE_GUARD","Tide Guard",["Prismarine Shield","Bag of Prismarine Shards"],36,5,2.0,"DAMAGE_REDUCTION",
  "%s%% less damage taken","The tide takes the blow."),
 ("FISHING","SWIFT_CURRENT","Swift Current",["Coral (blue)"],44,3,12.0,"SPEED",
  "%s%% chance of Speed II on a catch","Ride the current out."),
 ("FISHING","HARBORMASTER","Harbormaster",["Golden Salmon"],56,3,5.0,"POINT_BONUS",
  "%s%% chance of a bonus skill point","You run this dock."),
 ("FISHING","SIRENS_GIFT","Siren's Gift",["Celestial Starstone"],68,3,12.0,"REGEN",
  "%s%% chance of Regeneration on a catch","Something down there likes you."),
 ("FISHING","LEVIATHAN","Leviathan",["Blue Moon"],80,3,4.0,"FISH_FRENZY",
  "%s%% chance the water erupts with fish","Something enormous surfaces."),
]

assert len(P) == 70, len(P)
by_skill = {}
for row in P:
    by_skill.setdefault(row[0], []).append(row)
for s, rows in by_skill.items():
    assert len(rows) == 10, (s, len(rows))

resolved = []
for sk, key, disp, cands, ul, mr, val, eff, desc, flav in P:
    n = find(cands)
    resolved.append((sk, key, disp, n, idx[n], ul, mr, val, eff, desc, flav))

# ---- PerkHeads.java ----
h = io.StringIO()
h.write('package com.pallarium.endgame.ui;\n\n')
h.write('/**\n * Base64 skin textures for all 70 perk heads.\n')
h.write(' * Values taken verbatim from the minecraft-heads database.\n */\n')
h.write('public final class PerkHeads {\n\n    private PerkHeads() {\n    }\n')
for sk, key, disp, hn, hv, ul, mr, val, eff, desc, flav in resolved:
    h.write('\n    /** %s */\n    public static final String %s =\n            "%s";\n' % (hn, key, hv))
h.write('}\n')
open('src/com/pallarium/endgame/ui/PerkHeads.java', 'w', encoding='utf-8').write(h.getvalue())

# ---- Perk.java ----
p = io.StringIO()
p.write('package com.pallarium.endgame.skill;\n\n')
p.write('import com.pallarium.endgame.ui.PerkHeads;\n\n')
p.write('import java.util.ArrayList;\nimport java.util.List;\n\n')
p.write('/**\n * Every perk: tied to a skill, gated behind a skill level, ranked up with\n')
p.write(' * skill points. Ten per skill, ordered by unlock level.\n */\n')
p.write('public enum Perk {\n')
last = None
for sk, key, disp, hn, hv, ul, mr, val, eff, desc, flav in resolved:
    if sk != last:
        p.write('\n    // --- %s ---\n' % sk.title())
        last = sk
    p.write('    %s(Skill.%s, "%s", PerkHeads.%s, %d, %d, %s, Effect.%s,\n'
            % (key, sk, disp, key, ul, mr, repr(val), eff))
    p.write('            "%s",\n            "%s"),\n' % (desc, flav))
body = p.getvalue()
body = body.rstrip()[:-1] + ';\n'
p = io.StringIO()
p.write(body)
p.write('''
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
''')
open('src/com/pallarium/endgame/skill/Perk.java', 'w', encoding='utf-8').write(p.getvalue())

print('wrote 70 perks')
for sk, rows in by_skill.items():
    print(sk, [r[1] for r in rows][:3], '...')
