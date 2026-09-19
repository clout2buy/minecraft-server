package com.pallarium.endgame.item;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

/**
 * The single source of truth for how good an item is. Drives the tooltip
 * colour, the loot beam, the drop sound and the inventory name.
 */
public enum ItemTier {

    COMMON("Common", "\u25CB", TextColor.fromHexString("#9CA3AF"),
            Color.fromRGB(156, 163, 175), Particle.CRIT, 1.0f, 0),
    UNCOMMON("Uncommon", "\u25C7", TextColor.fromHexString("#4ADE80"),
            Color.fromRGB(74, 222, 128), Particle.VILLAGER_HAPPY, 1.1f, 1),
    RARE("Rare", "\u25C8", TextColor.fromHexString("#22D3EE"),
            Color.fromRGB(34, 211, 238), Particle.SOUL_FIRE_FLAME, 1.25f, 2),
    EPIC("Epic", "\u2726", TextColor.fromHexString("#C084FC"),
            Color.fromRGB(192, 132, 252), Particle.DRAGON_BREATH, 1.4f, 3),
    LEGENDARY("Legendary", "\u2739", TextColor.fromHexString("#FACC15"),
            Color.fromRGB(250, 204, 21), Particle.END_ROD, 1.6f, 4),
    MYTHIC("Mythic", "\u2620", TextColor.fromHexString("#F87171"),
            Color.fromRGB(248, 113, 113), Particle.FLAME, 1.9f, 5);

    private final String label;
    private final String glyph;
    private final TextColor color;
    private final Color dust;
    private final Particle particle;
    private final float pitch;
    private final int rank;

    ItemTier(String label, String glyph, TextColor color, Color dust,
             Particle particle, float pitch, int rank) {
        this.label = label;
        this.glyph = glyph;
        this.color = color;
        this.dust = dust;
        this.particle = particle;
        this.pitch = pitch;
        this.rank = rank;
    }

    public String label() {
        return label;
    }

    public String glyph() {
        return glyph;
    }

    public TextColor color() {
        return color;
    }

    public Color dust() {
        return dust;
    }

    public Particle particle() {
        return particle;
    }

    public float pitch() {
        return pitch;
    }

    public int rank() {
        return rank;
    }

    /** Beam column height in blocks. Common gets none. */
    public int beam() {
        return rank * 2;
    }

    /**
     * Works out an item's tier from its material and enchantments.
     * Enchantments push an item up the ladder.
     */
    public static ItemTier of(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return COMMON;
        }
        Material m = stack.getType();
        int ench = stack.getEnchantments().size();
        int levels = stack.getEnchantments().values().stream()
                .mapToInt(Integer::intValue).sum();

        ItemTier base = switch (m) {
            case NETHER_STAR, DRAGON_EGG, ENCHANTED_GOLDEN_APPLE, HEART_OF_THE_SEA -> MYTHIC;
            case NETHERITE_INGOT, NETHERITE_SCRAP, ANCIENT_DEBRIS, TOTEM_OF_UNDYING,
                 NETHERITE_SWORD, NETHERITE_AXE, NETHERITE_HELMET, NETHERITE_CHESTPLATE,
                 NETHERITE_LEGGINGS, NETHERITE_BOOTS, ELYTRA, BEACON -> LEGENDARY;
            case DIAMOND, DIAMOND_SWORD, DIAMOND_AXE, DIAMOND_HELMET, DIAMOND_CHESTPLATE,
                 DIAMOND_LEGGINGS, DIAMOND_BOOTS, EMERALD_BLOCK, GOLDEN_APPLE,
                 ENCHANTED_BOOK, EXPERIENCE_BOTTLE, TRIDENT -> EPIC;
            case EMERALD, GOLD_BLOCK, IRON_BLOCK, LAPIS_BLOCK, OBSIDIAN, BLAZE_ROD,
                 ENDER_PEARL, GHAST_TEAR, PHANTOM_MEMBRANE, SHULKER_SHELL,
                 IRON_SWORD, IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS -> RARE;
            case GOLD_INGOT, IRON_INGOT, REDSTONE_BLOCK, COAL_BLOCK, QUARTZ,
                 AMETHYST_SHARD, PRISMARINE_CRYSTALS, SLIME_BALL, GUNPOWDER -> UNCOMMON;
            default -> COMMON;
        };

        int bump = 0;
        if (ench >= 1) bump++;
        if (levels >= 6) bump++;
        if (levels >= 12) bump++;
        return values()[Math.min(values().length - 1, base.ordinal() + bump)];
    }
}
