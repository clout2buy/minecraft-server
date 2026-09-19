package com.pallarium.endgame.service;

import com.pallarium.endgame.skill.Skill;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Static lookup of what a block or mob is worth. Anything absent awards nothing,
 * which keeps junk blocks from being XP.
 */
public final class XpSources {

    private static final Map<Material, Entry> BLOCKS = new EnumMap<>(Material.class);
    private static final Map<EntityType, Integer> MOBS = new EnumMap<>(EntityType.class);

    public record Entry(Skill skill, int xp) {
    }

    private XpSources() {
    }

    private static void block(Skill skill, int xp, Material... mats) {
        for (Material m : mats) {
            if (m != null) {
                BLOCKS.put(m, new Entry(skill, xp));
            }
        }
    }

    static {
        // --- Mining ---
        block(Skill.MINING, 4, Material.STONE, Material.COBBLESTONE, Material.GRANITE,
                Material.DIORITE, Material.ANDESITE, Material.TUFF, Material.CALCITE,
                Material.DRIPSTONE_BLOCK, Material.BASALT, Material.BLACKSTONE);
        block(Skill.MINING, 6, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.NETHERRACK,
                Material.END_STONE, Material.SANDSTONE, Material.RED_SANDSTONE);
        block(Skill.MINING, 14, Material.COAL_ORE, Material.NETHER_QUARTZ_ORE);
        block(Skill.MINING, 18, Material.DEEPSLATE_COAL_ORE, Material.COPPER_ORE);
        block(Skill.MINING, 22, Material.IRON_ORE, Material.DEEPSLATE_COPPER_ORE);
        block(Skill.MINING, 28, Material.DEEPSLATE_IRON_ORE, Material.NETHER_GOLD_ORE);
        block(Skill.MINING, 34, Material.GOLD_ORE, Material.REDSTONE_ORE, Material.LAPIS_ORE);
        block(Skill.MINING, 42, Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_REDSTONE_ORE,
                Material.DEEPSLATE_LAPIS_ORE, Material.AMETHYST_CLUSTER);
        block(Skill.MINING, 70, Material.DIAMOND_ORE, Material.EMERALD_ORE);
        block(Skill.MINING, 90, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE);
        block(Skill.MINING, 140, Material.ANCIENT_DEBRIS);

        // --- Woodcutting ---
        block(Skill.WOODCUTTING, 12, Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
                Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
                Material.MANGROVE_LOG, Material.CHERRY_LOG);
        block(Skill.WOODCUTTING, 12, Material.STRIPPED_OAK_LOG, Material.STRIPPED_BIRCH_LOG,
                Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_JUNGLE_LOG,
                Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
                Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG);
        block(Skill.WOODCUTTING, 14, Material.CRIMSON_STEM, Material.WARPED_STEM);

        // --- Excavation ---
        block(Skill.EXCAVATION, 3, Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT,
                Material.GRASS_BLOCK, Material.PODZOL, Material.MYCELIUM, Material.MUD);
        block(Skill.EXCAVATION, 4, Material.SAND, Material.RED_SAND, Material.GRAVEL,
                Material.SOUL_SAND, Material.SOUL_SOIL);
        block(Skill.EXCAVATION, 5, Material.CLAY, Material.SNOW_BLOCK, Material.SUSPICIOUS_SAND,
                Material.SUSPICIOUS_GRAVEL);

        // --- Farming ---
        block(Skill.FARMING, 10, Material.WHEAT, Material.CARROTS, Material.POTATOES,
                Material.BEETROOTS, Material.NETHER_WART);
        block(Skill.FARMING, 8, Material.PUMPKIN, Material.MELON, Material.SUGAR_CANE,
                Material.BAMBOO, Material.COCOA);
        block(Skill.FARMING, 14, Material.SWEET_BERRY_BUSH, Material.CAVE_VINES_PLANT,
                Material.CHORUS_FLOWER);
        block(Skill.FARMING, 6, Material.BROWN_MUSHROOM_BLOCK, Material.RED_MUSHROOM_BLOCK);

        // --- Combat / Archery values (shared table, split by damage source) ---
        MOBS.put(EntityType.ZOMBIE, 16);
        MOBS.put(EntityType.SKELETON, 18);
        MOBS.put(EntityType.SPIDER, 14);
        MOBS.put(EntityType.CAVE_SPIDER, 18);
        MOBS.put(EntityType.CREEPER, 22);
        MOBS.put(EntityType.ENDERMAN, 34);
        MOBS.put(EntityType.WITCH, 34);
        MOBS.put(EntityType.DROWNED, 20);
        MOBS.put(EntityType.HUSK, 18);
        MOBS.put(EntityType.STRAY, 20);
        MOBS.put(EntityType.PHANTOM, 26);
        MOBS.put(EntityType.SLIME, 10);
        MOBS.put(EntityType.MAGMA_CUBE, 14);
        MOBS.put(EntityType.BLAZE, 40);
        MOBS.put(EntityType.GHAST, 46);
        MOBS.put(EntityType.PIGLIN, 22);
        MOBS.put(EntityType.PIGLIN_BRUTE, 48);
        MOBS.put(EntityType.HOGLIN, 30);
        MOBS.put(EntityType.ZOGLIN, 34);
        MOBS.put(EntityType.ZOMBIFIED_PIGLIN, 20);
        MOBS.put(EntityType.WITHER_SKELETON, 50);
        MOBS.put(EntityType.GUARDIAN, 34);
        MOBS.put(EntityType.ELDER_GUARDIAN, 90);
        MOBS.put(EntityType.SHULKER, 44);
        MOBS.put(EntityType.EVOKER, 60);
        MOBS.put(EntityType.VINDICATOR, 40);
        MOBS.put(EntityType.PILLAGER, 28);
        MOBS.put(EntityType.RAVAGER, 70);
        MOBS.put(EntityType.VEX, 24);
        MOBS.put(EntityType.SILVERFISH, 10);
        MOBS.put(EntityType.ENDERMITE, 12);
        MOBS.put(EntityType.WARDEN, 300);
        MOBS.put(EntityType.ENDER_DRAGON, 500);
        MOBS.put(EntityType.WITHER, 400);
        MOBS.put(EntityType.COW, 6);
        MOBS.put(EntityType.PIG, 6);
        MOBS.put(EntityType.SHEEP, 6);
        MOBS.put(EntityType.CHICKEN, 5);
        MOBS.put(EntityType.RABBIT, 5);
    }

    public static Entry forBlock(Material material) {
        return BLOCKS.get(material);
    }

    public static int forMob(EntityType type) {
        return MOBS.getOrDefault(type, 0);
    }

    /** True when this block is a crop that must be fully grown to pay out. */
    public static boolean isAgeable(Material material) {
        return material == Material.WHEAT || material == Material.CARROTS
                || material == Material.POTATOES || material == Material.BEETROOTS
                || material == Material.NETHER_WART;
    }
}
