package com.pallarium.endgame.service;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.data.PlayerProfile;
import com.pallarium.endgame.skill.Effect;
import com.pallarium.endgame.skill.Perk;
import com.pallarium.endgame.skill.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Runs every perk effect. The listener decides WHEN a perk is eligible, this
 * decides WHAT happens, so perks stay data-driven.
 */
public class PerkEngine {

    private static final Random RNG = new Random();

    /** Ore-ish blocks that AUTO_SMELT knows how to convert. */
    private static final Set<Material> LOGS = EnumSet.noneOf(Material.class);

    static {
        for (Material m : Material.values()) {
            String n = m.name();
            if (m.isBlock() && (n.endsWith("_LOG") || n.endsWith("_WOOD")
                    || n.endsWith("_STEM") || n.endsWith("_HYPHAE"))) {
                LOGS.add(m);
            }
        }
    }

    private static final List<Material> ORE_LOOT = List.of(
            Material.COAL, Material.RAW_IRON, Material.RAW_COPPER, Material.RAW_GOLD,
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.REDSTONE, Material.LAPIS_LAZULI,
            Material.AMETHYST_SHARD, Material.QUARTZ, Material.EMERALD, Material.DIAMOND);

    private static final List<Material> TIMBER_LOOT = List.of(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.DARK_OAK_LOG,
            Material.APPLE, Material.STICK, Material.OAK_SAPLING, Material.HONEYCOMB);

    private static final List<Material> RELIC_LOOT = List.of(
            Material.BONE, Material.FLINT, Material.STRING, Material.GOLD_NUGGET,
            Material.IRON_NUGGET, Material.EMERALD, Material.NAME_TAG, Material.SADDLE,
            Material.GOLDEN_APPLE, Material.ENDER_PEARL);

    private static final List<Material> HARVEST_LOOT = List.of(
            Material.WHEAT, Material.CARROT, Material.POTATO, Material.BEETROOT,
            Material.PUMPKIN, Material.MELON_SLICE, Material.GOLDEN_CARROT, Material.BREAD);

    private static final List<Material> SPOILS_LOOT = List.of(
            Material.BONE, Material.ROTTEN_FLESH, Material.GUNPOWDER, Material.IRON_INGOT,
            Material.GOLD_INGOT, Material.EMERALD, Material.ARROW, Material.DIAMOND);

    private static final List<Material> SUNKEN_LOOT = List.of(
            Material.COD, Material.SALMON, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS,
            Material.NAUTILUS_SHELL, Material.HEART_OF_THE_SEA, Material.INK_SAC, Material.GOLD_INGOT);

    private static final List<Material> TREASURE_LOOT = List.of(
            Material.IRON_NUGGET, Material.GOLD_NUGGET, Material.EMERALD,
            Material.AMETHYST_SHARD, Material.LAPIS_LAZULI, Material.DIAMOND,
            Material.BONE, Material.STRING, Material.FLINT, Material.QUARTZ);

    private final EndgamePlugin plugin;

    public PerkEngine(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean roll(double percent) {
        return percent > 0 && RNG.nextDouble() * 100.0 < percent;
    }

    /** Rolls a perk and returns true when it fired. */
    public boolean fires(PlayerProfile profile, Perk perk) {
        return roll(profile.perkValue(perk));
    }

    /** Total value of every perk in a skill that uses the given effect. */
    public double sum(PlayerProfile profile, Skill skill, Effect effect) {
        double total = 0;
        for (Perk p : Perk.of(skill)) {
            if (p.effect() == effect) {
                total += profile.perkValue(p);
            }
        }
        return total;
    }

    /** Total value of an effect across every skill. */
    public double sumAll(PlayerProfile profile, Effect effect) {
        double total = 0;
        for (Perk p : Perk.values()) {
            if (p.effect() == effect) {
                total += profile.perkValue(p);
            }
        }
        return total;
    }

    /** Fires the first perk of this effect that passes its roll. */
    public Perk rollEffect(PlayerProfile profile, Skill skill, Effect effect) {
        for (Perk p : Perk.of(skill)) {
            if (p.effect() == effect && roll(profile.perkValue(p))) {
                return p;
            }
        }
        return null;
    }

    // --- block-break effects ---

    public void duplicateDrops(Block block, Location at) {
        for (ItemStack drop : block.getDrops()) {
            if (drop != null && drop.getType() != Material.AIR) {
                block.getWorld().dropItemNaturally(at, drop.clone());
            }
        }
    }

    /** Turns raw ore drops into their smelted form. */
    public void autoSmelt(Player player, Block block, Location at) {
        boolean any = false;
        for (ItemStack drop : block.getDrops(player.getInventory().getItemInMainHand(), player)) {
            Material smelted = smeltedForm(drop.getType());
            if (smelted == null) {
                continue;
            }
            any = true;
            block.getWorld().dropItemNaturally(at, new ItemStack(smelted, drop.getAmount()));
        }
        if (any) {
            block.getWorld().spawnParticle(Particle.FLAME, at, 12, 0.25, 0.25, 0.25, 0.01);
            player.playSound(at, Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.6f);
        }
    }

    private Material smeltedForm(Material raw) {
        return switch (raw) {
            case RAW_IRON -> Material.IRON_INGOT;
            case RAW_GOLD -> Material.GOLD_INGOT;
            case RAW_COPPER -> Material.COPPER_INGOT;
            case SAND -> Material.GLASS;
            case COBBLESTONE -> Material.STONE;
            case CLAY_BALL -> Material.BRICK;
            case NETHERRACK -> Material.NETHER_BRICK;
            default -> null;
        };
    }

    /** Pulls nearby dropped items straight into the player's inventory. */
    public void magnet(Player player, Location at) {
        boolean pulled = false;
        for (Entity e : player.getWorld().getNearbyEntities(at, 4, 4, 4)) {
            if (e instanceof Item item && !item.isDead()) {
                ItemStack stack = item.getItemStack();
                if (player.getInventory().firstEmpty() == -1) {
                    break;
                }
                player.getInventory().addItem(stack);
                item.remove();
                pulled = true;
            }
        }
        if (pulled) {
            player.getWorld().spawnParticle(Particle.CRIT_MAGIC, at, 14, 0.4, 0.4, 0.4, 0.05);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.8f);
        }
    }

    /** The block bursts, spraying extra ore into the air. */
    public void oreBurst(Player player, Location at) {
        World world = at.getWorld();
        int count = 3 + RNG.nextInt(4);
        for (int i = 0; i < count; i++) {
            Material m = ORE_LOOT.get(RNG.nextInt(ORE_LOOT.size()));
            Item item = world.dropItem(at, new ItemStack(m));
            item.setVelocity(new Vector(
                    (RNG.nextDouble() - 0.5) * 0.35,
                    0.35 + RNG.nextDouble() * 0.25,
                    (RNG.nextDouble() - 0.5) * 0.35));
        }
        world.spawnParticle(Particle.EXPLOSION_NORMAL, at, 6, 0.3, 0.3, 0.3, 0.02);
        world.spawnParticle(Particle.CRIT_MAGIC, at, 30, 0.5, 0.5, 0.5, 0.15);
        world.playSound(at, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.9f);
    }

    /**
     * The signature perk. Slams a real chest into the world in front of the
     * player, fills it with themed loot, and removes it after 30 seconds if
     * nobody touches it.
     */
    public void lootChest(Player player, Location origin, Perk perk) {
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        Location spot = findChestSpot(player, origin);
        if (spot == null) {
            return;
        }

        Block target = spot.getBlock();
        target.setType(Material.CHEST);

        List<Material> table = lootTableFor(perk.skill());
        if (target.getState() instanceof org.bukkit.block.Chest chest) {
            int stacks = 3 + RNG.nextInt(4);
            for (int i = 0; i < stacks; i++) {
                Material m = table.get(RNG.nextInt(table.size()));
                int amount = 1 + RNG.nextInt(6);
                int slot = RNG.nextInt(chest.getBlockInventory().getSize());
                chest.getBlockInventory().setItem(slot, new ItemStack(m, amount));
            }
            chest.update();
        }

        Location center = spot.clone().add(0.5, 0.5, 0.5);
        world.spawnParticle(Particle.FLASH, center, 2);
        world.spawnParticle(Particle.CRIT_MAGIC, center, 40, 0.5, 0.5, 0.5, 0.2);
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.7f, 1.4f);
        world.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);

        player.sendMessage(Component.text("  " + perk.display() + "  ", perk.skill().color())
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(perk.flavour(), Icon0.dim())
                        .decoration(TextDecoration.BOLD, false))
                .decoration(TextDecoration.ITALIC, false));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.getType() == Material.CHEST) {
                target.setType(Material.AIR);
                world.spawnParticle(Particle.SMOKE_NORMAL, center, 12, 0.3, 0.3, 0.3, 0.02);
            }
        }, 20L * 30);
    }

    /** Small shim so this class does not need the ui package for one colour. */
    private static final class Icon0 {
        static net.kyori.adventure.text.format.TextColor dim() {
            return net.kyori.adventure.text.format.TextColor.fromHexString("#6B7280");
        }
    }

    private Location findChestSpot(Player player, Location origin) {
        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        Location front = origin.clone().add(dir.multiply(1.5));
        front.setY(origin.getBlockY());

        for (int dy = 0; dy <= 2; dy++) {
            Location test = front.clone().add(0, dy, 0);
            if (test.getBlock().getType().isAir()
                    || test.getBlock().getType() == Material.CAVE_AIR) {
                return test;
            }
        }
        if (origin.getBlock().getType().isAir()) {
            return origin;
        }
        Location above = origin.clone().add(0, 1, 0);
        return above.getBlock().getType().isAir() ? above : null;
    }

    private List<Material> lootTableFor(Skill skill) {
        return switch (skill) {
            case MINING -> ORE_LOOT;
            case WOODCUTTING -> TIMBER_LOOT;
            case EXCAVATION -> RELIC_LOOT;
            case FARMING -> HARVEST_LOOT;
            case COMBAT, ARCHERY -> SPOILS_LOOT;
            case FISHING -> SUNKEN_LOOT;
        };
    }

    /** Breaks the rest of the tree above the log that was hit. */
    public void fellTree(Player player, Block start) {
        if (!LOGS.contains(start.getType())) {
            return;
        }
        Material type = start.getType();
        List<Block> queue = new ArrayList<>();
        List<Block> found = new ArrayList<>();
        queue.add(start);

        while (!queue.isEmpty() && found.size() < 80) {
            Block b = queue.remove(0);
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }
                        Block n = b.getRelative(x, y, z);
                        if (n.getType() == type && !found.contains(n) && !queue.contains(n)) {
                            queue.add(n);
                            found.add(n);
                        }
                    }
                }
            }
        }

        for (Block b : found) {
            b.breakNaturally(player.getInventory().getItemInMainHand());
        }
        if (!found.isEmpty()) {
            Location at = start.getLocation().add(0.5, 1, 0.5);
            start.getWorld().playSound(at, Sound.BLOCK_WOOD_BREAK, 1f, 0.6f);
            start.getWorld().spawnParticle(Particle.BLOCK_CRACK, at, 30, 0.6, 1.0, 0.6,
                    start.getBlockData());
        }
    }

    /** Puts the crop back in the ground at age zero. */
    public void replant(Block block) {
        Material type = block.getType();
        if (!(block.getBlockData() instanceof Ageable)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (block.getType().isAir()) {
                block.setType(type);
                if (block.getBlockData() instanceof Ageable fresh) {
                    fresh.setAge(0);
                    block.setBlockData(fresh);
                }
                block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
                        block.getLocation().add(0.5, 0.4, 0.5), 8, 0.25, 0.2, 0.25, 0);
            }
        }, 2L);
    }

    // --- combat effects ---

    public void lightning(Player player, LivingEntity victim) {
        World world = victim.getWorld();
        world.strikeLightningEffect(victim.getLocation());
        victim.damage(4.0, player);
        world.spawnParticle(Particle.ELECTRIC_SPARK, victim.getLocation().add(0, 1, 0),
                40, 0.4, 0.8, 0.4, 0.3);
        world.playSound(victim.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.4f);
    }

    /** Rains arrows down onto the victim from above. */
    public void arrowStorm(Player player, LivingEntity victim) {
        World world = victim.getWorld();
        Location above = victim.getLocation().add(0, 8, 0);
        for (int i = 0; i < 8; i++) {
            final int n = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location spawn = above.clone().add(
                        (RNG.nextDouble() - 0.5) * 3, 0, (RNG.nextDouble() - 0.5) * 3);
                Arrow arrow = world.spawn(spawn, Arrow.class);
                arrow.setShooter(player);
                arrow.setVelocity(new Vector(0, -1.6, 0));
                arrow.setDamage(3.0);
                arrow.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
                if (n == 0) {
                    world.playSound(victim.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.7f);
                }
            }, i * 2L);
        }
    }

    /** Spawns a tamed wolf that fights alongside the player. */
    public void summonWolf(Player player, Location at) {
        World world = at.getWorld();
        Wolf wolf = world.spawn(at, Wolf.class);
        wolf.setTamed(true);
        wolf.setOwner(player);
        wolf.setCollarColor(org.bukkit.DyeColor.CYAN);
        wolf.customName(Component.text(player.getName() + "'s Wolf",
                net.kyori.adventure.text.format.TextColor.fromHexString("#22D3EE")));
        wolf.setCustomNameVisible(false);

        world.spawnParticle(Particle.CLOUD, at, 16, 0.3, 0.3, 0.3, 0.02);
        world.playSound(at, Sound.ENTITY_WOLF_HOWL, 0.8f, 1.1f);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!wolf.isDead()) {
                wolf.getWorld().spawnParticle(Particle.SMOKE_NORMAL,
                        wolf.getLocation().add(0, 0.5, 0), 12, 0.3, 0.3, 0.3, 0.02);
                wolf.remove();
            }
        }, 20L * 90);
    }

    /** The water erupts with a burst of fish. */
    public void fishFrenzy(Player player, Location at) {
        World world = at.getWorld();
        for (int i = 0; i < 6 + RNG.nextInt(5); i++) {
            Material m = RNG.nextBoolean() ? Material.COD : Material.SALMON;
            if (RNG.nextInt(8) == 0) {
                m = Material.TROPICAL_FISH;
            }
            Item item = world.dropItem(at, new ItemStack(m));
            item.setVelocity(new Vector(
                    (RNG.nextDouble() - 0.5) * 0.5,
                    0.5 + RNG.nextDouble() * 0.3,
                    (RNG.nextDouble() - 0.5) * 0.5));
        }
        world.spawnParticle(Particle.WATER_SPLASH, at, 60, 0.8, 0.5, 0.8, 0.2);
        world.spawnParticle(Particle.BUBBLE_POP, at, 40, 0.6, 0.4, 0.6, 0.1);
        world.playSound(at, Sound.ENTITY_DOLPHIN_SPLASH, 1f, 0.9f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.7f);
    }

    // --- shared helpers ---

    public void potion(Player player, PotionEffectType type, int seconds, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        if (current != null && current.getDuration() > seconds * 20 - 40) {
            return;
        }
        player.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier, true, false, true));
    }

    public void dropTreasure(Player player, Location at) {
        Material loot = TREASURE_LOOT.get(RNG.nextInt(TREASURE_LOOT.size()));
        at.getWorld().dropItemNaturally(at, new ItemStack(loot));
        Fx.treasure(player, at);
    }

    public void feed(Player player, int amount) {
        if (player.getFoodLevel() < 20) {
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + amount));
            player.setSaturation(Math.min(20f, player.getSaturation() + amount));
        }
    }

    public void heal(Player player, double amount) {
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        double max = attr != null ? attr.getValue() : 20.0;
        player.setHealth(Math.min(max, player.getHealth() + amount));
    }
}
