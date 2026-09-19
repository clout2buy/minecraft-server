package com.pallarium.endgame.mob;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Turns ordinary spawns into randomized elites: a rarity tier, one or more
 * affixes, rolled gear, scaled stats and a loot table that pays out on death.
 */
public class EliteService {

    private final EndgamePlugin plugin;

    private final NamespacedKey keyTier;
    private final NamespacedKey keyAffixes;
    private final NamespacedKey keyLevel;
    private final NamespacedKey keyName;

    /** Mob types eligible to be promoted to an elite. */
    private static final Set<EntityType> ELIGIBLE = EnumSet.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
            EntityType.CREEPER, EntityType.ENDERMAN, EntityType.WITCH,
            EntityType.DROWNED, EntityType.HUSK, EntityType.STRAY,
            EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.RAVAGER,
            EntityType.BLAZE, EntityType.WITHER_SKELETON, EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE, EntityType.HOGLIN, EntityType.ZOGLIN,
            EntityType.CAVE_SPIDER, EntityType.SILVERFISH, EntityType.PHANTOM,
            EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.ZOMBIE_VILLAGER,
            EntityType.EVOKER, EntityType.ILLUSIONER, EntityType.GUARDIAN);

    private static final Material[] HELMETS = {
            Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
            Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET};
    private static final Material[] CHESTS = {
            Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
            Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE};
    private static final Material[] LEGS = {
            Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
            Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS};
    private static final Material[] BOOTS = {
            Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
            Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS};
    private static final Material[] WEAPONS = {
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.IRON_AXE, Material.DIAMOND_AXE, Material.TRIDENT};

    /** Fantasy title fragments so two elites rarely read the same. */
    private static final String[] EPITHETS = {
            "the Forgotten", "the Unbroken", "the Hollow", "the Pale", "the Gravewalker",
            "the Ashen", "the Ruin", "the Sunless", "the Kindled", "the Wretched",
            "the Nameless", "the Last Light", "the Cinder", "the Deepborn", "the Rotcrown",
            "the Long Night", "the Salt King", "the Mourner", "the Iron Wake", "the Gloom"};

    public EliteService(EndgamePlugin plugin) {
        this.plugin = plugin;
        this.keyTier = new NamespacedKey(plugin, "elite_tier");
        this.keyAffixes = new NamespacedKey(plugin, "elite_affixes");
        this.keyLevel = new NamespacedKey(plugin, "elite_level");
        this.keyName = new NamespacedKey(plugin, "elite_name");
    }

    /* ------------------------------------------------------------------ */
    /*  spawning                                                           */
    /* ------------------------------------------------------------------ */

    private long ascensionRolls;
    private long ascensionHits;
    private long spawnRolls;
    private long spawnHits;

    /** Live counters for /endgame elitestats: rolls vs actual promotions. */
    public long[] counters() {
        return new long[]{spawnRolls, spawnHits, ascensionRolls, ascensionHits};
    }

    public boolean eligible(LivingEntity entity) {
        return ELIGIBLE.contains(entity.getType()) && !isElite(entity);
    }

    /**
     * Rolls whether this spawn becomes an elite, and if so builds it.
     * Deeper caves and further-from-spawn locations skew toward higher tiers.
     */
    public void considerSpawn(LivingEntity entity) {
        if (!eligible(entity)) {
            return;
        }
        spawnRolls++;
        double chance = plugin.getConfig().getDouble("elites.spawn-chance", 6.0);
        if (ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) {
            return;
        }
        spawnHits++;
        promote(entity, rollTier(entity.getLocation()));
    }

    /** Difficulty weight from depth and distance, 0.0 to 1.0. */
    private double danger(Location loc) {
        double depth = Math.max(0, 62 - loc.getY()) / 120.0;
        double dist = Math.min(1.0, loc.toVector().length() / 4000.0);
        double env = loc.getWorld() != null
                && loc.getWorld().getEnvironment() != World.Environment.NORMAL ? 0.35 : 0.0;
        return Math.min(1.0, depth * 0.5 + dist * 0.4 + env);
    }

    private EliteTier rollTier(Location loc) {
        double d = danger(loc);
        double r = ThreadLocalRandom.current().nextDouble();
        // base weights shift upward as danger climbs
        double nightmare = 0.01 + d * 0.06;
        double champion = 0.04 + d * 0.11;
        double elite = 0.10 + d * 0.15;
        double savage = 0.18 + d * 0.14;
        double haunted = 0.27;
        if (r < nightmare) return EliteTier.NIGHTMARE;
        if (r < nightmare + champion) return EliteTier.CHAMPION;
        if (r < nightmare + champion + elite) return EliteTier.ELITE;
        if (r < nightmare + champion + elite + savage) return EliteTier.SAVAGE;
        if (r < nightmare + champion + elite + savage + haunted) return EliteTier.HAUNTED;
        return EliteTier.TOUCHED;
    }

    /** Builds a full elite on an existing mob. Used by spawns and by command. */
    public void promote(LivingEntity entity, EliteTier tier) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        int level = Math.max(1, (int) Math.round(
                (1 + danger(entity.getLocation()) * 60) * (0.6 + tier.ordinal() * 0.25)
                        + rng.nextInt(8)));
        level = Math.min(100, level);

        List<Affix> affixes = rollAffixes(tier.affixes());

        // stats
        double hpMult = tier.healthMult() * (1 + level / 140.0);
        if (affixes.contains(Affix.GIANT)) hpMult *= 1.8;
        if (affixes.contains(Affix.TINY)) hpMult *= 0.65;
        setAttribute(entity, Attribute.GENERIC_MAX_HEALTH, hpMult, 1024.0);
        entity.setHealth(Math.min(entity.getHealth() * hpMult,
                attributeValue(entity, Attribute.GENERIC_MAX_HEALTH)));

        double dmgMult = tier.damageMult() * (1 + level / 220.0);
        if (affixes.contains(Affix.TINY)) dmgMult *= 1.25;
        setAttribute(entity, Attribute.GENERIC_ATTACK_DAMAGE, dmgMult, 60.0);

        if (affixes.contains(Affix.SWIFT)) {
            setAttribute(entity, Attribute.GENERIC_MOVEMENT_SPEED, 1.45, 0.65);
        }
        if (affixes.contains(Affix.ARMORED)) {
            setAttribute(entity, Attribute.GENERIC_ARMOR, 1.0, 20.0);
            AttributeInstance a = entity.getAttribute(Attribute.GENERIC_ARMOR);
            if (a != null) a.setBaseValue(Math.min(20.0, a.getBaseValue() + 12));
        }
        if (affixes.contains(Affix.HARDENED)) {
            setAttribute(entity, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1.0, 1.0);
            AttributeInstance a = entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
            if (a != null) a.setBaseValue(1.0);
        }
        if (affixes.contains(Affix.OF_THE_HUNT)) {
            setAttribute(entity, Attribute.GENERIC_FOLLOW_RANGE, 1.0, 64.0);
            AttributeInstance a = entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
            if (a != null) a.setBaseValue(64.0);
        }

        // visual scale
        if (affixes.contains(Affix.GIANT)) {
            setScale(entity, 1.55);
        } else if (affixes.contains(Affix.TINY)) {
            setScale(entity, 0.55);
        }

        if (affixes.contains(Affix.BLAZING)) {
            entity.setFireTicks(0);
            entity.setVisualFire(true);
        }
        if (affixes.contains(Affix.SPECTRAL)) {
            entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        }
        if (affixes.contains(Affix.FROZEN)) {
            entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        }

        entity.setRemoveWhenFarAway(false);
        if (entity instanceof Mob mob) {
            mob.setPersistent(true);
        }

        gearUp(entity, tier, level, affixes);

        String display = buildName(entity, tier, affixes);
        entity.customName(nameplate(display, tier, level));
        entity.setCustomNameVisible(true);

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(keyTier, PersistentDataType.STRING, tier.name());
        pdc.set(keyLevel, PersistentDataType.INTEGER, level);
        pdc.set(keyName, PersistentDataType.STRING, display);
        StringBuilder sb = new StringBuilder();
        for (Affix a : affixes) {
            if (sb.length() > 0) sb.append(',');
            sb.append(a.name());
        }
        pdc.set(keyAffixes, PersistentDataType.STRING, sb.toString());

        spawnAura(entity, tier);
    }

    private List<Affix> rollAffixes(int count) {
        List<Affix> pool = new ArrayList<>(List.of(Affix.values()));
        List<Affix> picked = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            Affix a = pool.remove(rng.nextInt(pool.size()));
            // never roll both size affixes
            if (a == Affix.GIANT) pool.remove(Affix.TINY);
            if (a == Affix.TINY) pool.remove(Affix.GIANT);
            picked.add(a);
        }
        return picked;
    }

    private void setAttribute(LivingEntity e, Attribute attr, double mult, double cap) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) {
            return;
        }
        inst.setBaseValue(Math.min(cap, Math.max(0.01, inst.getBaseValue() * mult)));
    }

    private double attributeValue(LivingEntity e, Attribute attr) {
        AttributeInstance inst = e.getAttribute(attr);
        return inst == null ? e.getHealth() : inst.getValue();
    }

    private void setScale(LivingEntity e, double scale) {
        // 1.20.1 has no scale attribute; slimes are the one type that can resize,
        // everything else conveys size through its name and stats instead.
        if (e instanceof org.bukkit.entity.Slime slime) {
            slime.setSize(Math.max(1, (int) Math.round(slime.getSize() * scale * 1.6)));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  gear                                                               */
    /* ------------------------------------------------------------------ */

    private void gearUp(LivingEntity entity, EliteTier tier, int level, List<Affix> affixes) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        // material quality band tracks tier and level
        int band = Math.min(5, (int) Math.round(tier.ordinal() * 0.8 + level / 34.0));

        eq.setHelmet(rollPiece(HELMETS, band, tier, rng));
        eq.setChestplate(rollPiece(CHESTS, band, tier, rng));
        eq.setLeggings(rollPiece(LEGS, band, tier, rng));
        eq.setBoots(rollPiece(BOOTS, band, tier, rng));

        if (rng.nextDouble() < tier.gearChance() + 0.25) {
            Material w = WEAPONS[Math.min(WEAPONS.length - 1,
                    rng.nextInt(band + 2))];
            ItemStack weapon = new ItemStack(w);
            enchant(weapon, tier, rng);
            eq.setItemInMainHand(weapon);
        }
        if (tier.ordinal() >= EliteTier.ELITE.ordinal() && rng.nextDouble() < 0.35) {
            eq.setItemInOffHand(new ItemStack(Material.SHIELD));
        }

        // elites should not fling their whole kit on death, we handle drops ourselves
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
        eq.setItemInOffHandDropChance(0f);
    }

    private ItemStack rollPiece(Material[] set, int band, EliteTier tier, ThreadLocalRandom rng) {
        if (rng.nextDouble() > tier.gearChance()) {
            return null;
        }
        int idx = Math.max(0, Math.min(set.length - 1, band - rng.nextInt(2)));
        ItemStack item = new ItemStack(set[idx]);
        if (item.getType().name().startsWith("LEATHER")) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof LeatherArmorMeta leather) {
                leather.setColor(Color.fromRGB(
                        rng.nextInt(256), rng.nextInt(256), rng.nextInt(256)));
                item.setItemMeta(leather);
            }
        }
        enchant(item, tier, rng);
        return item;
    }

    private void enchant(ItemStack item, EliteTier tier, ThreadLocalRandom rng) {
        int rolls = tier.ordinal() >= EliteTier.SAVAGE.ordinal() ? 2 : 1;
        if (rng.nextDouble() > 0.35 + tier.ordinal() * 0.1) {
            return;
        }
        Enchantment[] pool = item.getType().name().contains("SWORD")
                || item.getType().name().contains("AXE")
                ? new Enchantment[]{Enchantment.DAMAGE_ALL, Enchantment.FIRE_ASPECT,
                        Enchantment.KNOCKBACK, Enchantment.DURABILITY}
                : new Enchantment[]{Enchantment.PROTECTION_ENVIRONMENTAL, Enchantment.DURABILITY,
                        Enchantment.THORNS, Enchantment.PROTECTION_FIRE};
        for (int i = 0; i < rolls; i++) {
            Enchantment ench = pool[rng.nextInt(pool.length)];
            item.addUnsafeEnchantment(ench, 1 + rng.nextInt(1 + tier.ordinal()));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  naming                                                             */
    /* ------------------------------------------------------------------ */

    /**
     * Short nameplates. One tag at most, never a prefix and a suffix together,
     * and suffixes are collapsed to a single word. A nameplate is a label you
     * read at a glance, not a sentence.
     */
    private String buildName(LivingEntity entity, EliteTier tier, List<Affix> affixes) {
        String base = pretty(entity.getType().name());

        // top tiers get a title instead of a tag, it reads better and stays short
        if (tier.ordinal() >= EliteTier.CHAMPION.ordinal()
                && ThreadLocalRandom.current().nextDouble() < 0.5) {
            String ep = EPITHETS[ThreadLocalRandom.current().nextInt(EPITHETS.length)];
            return cap(ep.startsWith("the ") ? ep.substring(4) + " " + base : ep + " " + base);
        }

        // otherwise exactly one tag: prefer a prefix, else collapse a suffix
        for (Affix a : affixes) {
            if (a.prefix()) {
                return cap(a.display() + " " + base);
            }
        }
        for (Affix a : affixes) {
            if (!a.prefix()) {
                return cap(shortTag(a) + " " + base);
            }
        }
        return cap(base);
    }

    /** Turns "of the Storm" into "Storm" so it can sit in front of the name. */
    private String shortTag(Affix a) {
        String d = a.display();
        if (d.startsWith("of the ")) {
            return d.substring(7);
        }
        if (d.startsWith("of ")) {
            return d.substring(3);
        }
        return d;
    }

    private String cap(String s) {
        return s.length() > 18 ? s.substring(0, 17).trim() + "…" : s;
    }

    private String pretty(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private Component nameplate(String display, EliteTier tier, int level) {
        return Component.text(tier.glyph() + " ", tier.textColor())
                .append(Component.text("Lv" + level + " ", Icon.DIM))
                .append(Component.text(display, tier.textColor()))
                .decoration(TextDecoration.ITALIC, false);
    }

    private void spawnAura(LivingEntity e, EliteTier tier) {
        World w = e.getWorld();
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, e.getLocation().add(0, 1, 0),
                12, 0.3, 0.5, 0.3, 0.01);
        if (tier.ordinal() >= EliteTier.CHAMPION.ordinal()) {
            w.playSound(e.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.7f, 0.6f);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  reading back                                                       */
    /* ------------------------------------------------------------------ */

    public boolean isElite(Entity e) {
        return e.getPersistentDataContainer().has(keyTier, PersistentDataType.STRING);
    }

    public EliteTier tierOf(Entity e) {
        String s = e.getPersistentDataContainer().get(keyTier, PersistentDataType.STRING);
        if (s == null) {
            return null;
        }
        try {
            return EliteTier.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public int levelOf(Entity e) {
        Integer i = e.getPersistentDataContainer().get(keyLevel, PersistentDataType.INTEGER);
        return i == null ? 1 : i;
    }

    public String nameOf(Entity e) {
        String s = e.getPersistentDataContainer().get(keyName, PersistentDataType.STRING);
        return s == null ? "Elite" : s;
    }

    public List<Affix> affixesOf(Entity e) {
        List<Affix> out = new ArrayList<>();
        String s = e.getPersistentDataContainer().get(keyAffixes, PersistentDataType.STRING);
        if (s == null || s.isEmpty()) {
            return out;
        }
        for (String part : s.split(",")) {
            try {
                out.add(Affix.valueOf(part));
            } catch (IllegalArgumentException ignored) {
                // affix removed in an update, skip it
            }
        }
        return out;
    }

    public boolean has(Entity e, Affix affix) {
        return affixesOf(e).contains(affix);
    }

    /** Announces a high tier elite to nearby players when first engaged. */
    public void announce(Player player, LivingEntity mob) {
        EliteTier tier = tierOf(mob);
        if (tier == null || tier.ordinal() < EliteTier.CHAMPION.ordinal()) {
            return;
        }
        player.showTitle(net.kyori.adventure.title.Title.title(
                Component.text(tier.glyph() + " " + nameOf(mob), tier.textColor()),
                Component.text(tier.display() + "  Lv" + levelOf(mob), Icon.DIM),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(200),
                        java.time.Duration.ofMillis(1400),
                        java.time.Duration.ofMillis(400))));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.4f);
    }

    /* ------------------------------------------------------------------ */
    /*  ascension: an elite tears itself out of a dying mob                */
    /* ------------------------------------------------------------------ */

    /**
     * Rolls whether killing an ordinary mob rips an elite out of the corpse.
     * Chance climbs with danger, and climbs hard at night and on higher
     * difficulty. Returns the spawned elite, or null if nothing rose.
     */
    public LivingEntity considerAscension(LivingEntity dead, Player killer, Location at) {
        if (!plugin.getConfig().getBoolean("elites.ascension.enabled", true)) {
            return null;
        }
        if (isElite(dead) || !ELIGIBLE.contains(dead.getType()) || killer == null
                || at == null || at.getWorld() == null) {
            return null;
        }
        ascensionRolls++;
        double base = plugin.getConfig().getDouble("elites.ascension.chance", 9.0);
        double d = danger(at);
        double chance = base * (1.0 + d * 1.6);
        World w = at.getWorld();
        if (w != null && w.getEnvironment() == World.Environment.NORMAL) {
            long t = w.getTime();
            if (t > 13000 && t < 23000) {
                chance *= 1.5;
            }
        }
        if (ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) {
            return null;
        }
        ascensionHits++;

        EliteTier tier = rollAscensionTier(at);
        EntityType type = ascendedType(dead.getType());
        if (w == null) {
            return null;
        }
        Entity spawned = w.spawnEntity(at.clone().add(0, 0.2, 0), type);
        if (!(spawned instanceof LivingEntity risen)) {
            spawned.remove();
            return null;
        }
        promote(risen, tier);

        // it comes up angry and already looking at you
        if (risen instanceof Mob mob) {
            mob.setTarget(killer);
        }
        risen.setInvulnerable(true);
        risen.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 6, false, false));
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> { if (risen.isValid()) risen.setInvulnerable(false); }, 30L);

        playAscension(at, tier, killer);
        return risen;
    }

    /** Ascension skews one tier hotter than a normal spawn. */
    private EliteTier rollAscensionTier(Location loc) {
        EliteTier rolled = rollTier(loc);
        if (ThreadLocalRandom.current().nextDouble() < 0.45
                && rolled.ordinal() < EliteTier.NIGHTMARE.ordinal()) {
            return EliteTier.values()[rolled.ordinal() + 1];
        }
        return rolled;
    }

    /** Small chance the risen thing is nastier than what you killed. */
    private EntityType ascendedType(EntityType original) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (rng.nextDouble() < 0.25) {
            EntityType[] upgrades = switch (original) {
                case ZOMBIE, ZOMBIE_VILLAGER, HUSK ->
                        new EntityType[]{EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
                                EntityType.PIGLIN_BRUTE};
                case SKELETON, STRAY ->
                        new EntityType[]{EntityType.WITHER_SKELETON, EntityType.STRAY};
                case SPIDER, CAVE_SPIDER ->
                        new EntityType[]{EntityType.CAVE_SPIDER, EntityType.WITCH};
                case PILLAGER, VINDICATOR ->
                        new EntityType[]{EntityType.EVOKER, EntityType.RAVAGER};
                case PIGLIN, HOGLIN ->
                        new EntityType[]{EntityType.PIGLIN_BRUTE, EntityType.ZOGLIN};
                default -> null;
            };
            if (upgrades != null) {
                return upgrades[rng.nextInt(upgrades.length)];
            }
        }
        return original;
    }

    /** The corpse rips open. */
    private void playAscension(Location at, EliteTier tier, Player killer) {
        World w = at.getWorld();
        if (w == null) {
            return;
        }
        w.spawnParticle(Particle.SQUID_INK, at.clone().add(0, 0.8, 0), 60, 0.4, 0.5, 0.4, 0.05);
        w.spawnParticle(Particle.SOUL, at.clone().add(0, 0.6, 0), 45, 0.3, 0.7, 0.3, 0.08);
        w.spawnParticle(Particle.CRIMSON_SPORE, at.clone().add(0, 1, 0), 50, 0.6, 0.8, 0.6, 0.02);
        w.playSound(at, Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.6f);
        w.playSound(at, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.2f, 0.6f);
        w.playSound(at, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 1.4f);

        killer.showTitle(net.kyori.adventure.title.Title.title(
                Component.text(tier.glyph() + " Something rises", tier.textColor()),
                Component.text("the corpse was not empty", Icon.DIM),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(150),
                        java.time.Duration.ofMillis(1300),
                        java.time.Duration.ofMillis(450))));
    }

    public NamespacedKey engagedKey() {
        return new NamespacedKey(plugin, "elite_engaged");
    }
}
