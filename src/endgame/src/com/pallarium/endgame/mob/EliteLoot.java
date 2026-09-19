package com.pallarium.endgame.mob;

import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rolls the bonus drops an elite pays out on death. Scales with tier, and
 * OF_GREED swaps in a noticeably richer pool.
 */
public final class EliteLoot {

    private EliteLoot() {
    }

    private record Entry(Material material, int min, int max, double weight) {
    }

    private static final List<Entry> COMMON = List.of(
            new Entry(Material.IRON_INGOT, 2, 6, 20),
            new Entry(Material.GOLD_INGOT, 2, 5, 16),
            new Entry(Material.COAL, 4, 10, 18),
            new Entry(Material.BONE, 2, 6, 14),
            new Entry(Material.ARROW, 6, 16, 12),
            new Entry(Material.REDSTONE, 4, 12, 10),
            new Entry(Material.LAPIS_LAZULI, 3, 9, 10),
            new Entry(Material.EXPERIENCE_BOTTLE, 2, 5, 14));

    private static final List<Entry> RARE = List.of(
            new Entry(Material.DIAMOND, 1, 4, 16),
            new Entry(Material.EMERALD, 2, 6, 14),
            new Entry(Material.GOLDEN_APPLE, 1, 2, 10),
            new Entry(Material.ENDER_PEARL, 1, 3, 10),
            new Entry(Material.BLAZE_ROD, 1, 3, 8),
            new Entry(Material.AMETHYST_SHARD, 2, 6, 10),
            new Entry(Material.EXPERIENCE_BOTTLE, 6, 14, 12),
            new Entry(Material.DIAMOND_BLOCK, 1, 1, 4));

    private static final List<Entry> LEGENDARY = List.of(
            new Entry(Material.NETHERITE_SCRAP, 1, 3, 12),
            new Entry(Material.ENCHANTED_GOLDEN_APPLE, 1, 1, 5),
            new Entry(Material.NETHERITE_INGOT, 1, 1, 4),
            new Entry(Material.TOTEM_OF_UNDYING, 1, 1, 4),
            new Entry(Material.DIAMOND, 4, 10, 14),
            new Entry(Material.ANCIENT_DEBRIS, 1, 2, 6),
            new Entry(Material.HEART_OF_THE_SEA, 1, 1, 4),
            new Entry(Material.NETHER_STAR, 1, 1, 1));

    /** Builds the bonus drop list for a killed elite. */
    public static List<ItemStack> roll(EliteTier tier, List<Affix> affixes) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        boolean greedy = affixes.contains(Affix.OF_GREED);
        boolean thief = affixes.contains(Affix.OF_THIEVES);

        int rolls = 1 + (int) Math.floor(tier.lootMult() / 2.5);
        if (greedy) rolls += 2;
        if (thief) rolls += 1;

        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < rolls; i++) {
            List<Entry> pool = pickPool(tier, greedy, rng);
            Entry e = weighted(pool, rng);
            int amount = e.min() + rng.nextInt(Math.max(1, e.max() - e.min() + 1));
            out.add(new ItemStack(e.material(), amount));
        }

        // an enchanted book as a trophy from the top tiers
        if (tier.ordinal() >= EliteTier.CHAMPION.ordinal() && rng.nextDouble() < 0.4) {
            out.add(trophyBook(tier, rng));
        }
        return out;
    }

    private static List<Entry> pickPool(EliteTier tier, boolean greedy, ThreadLocalRandom rng) {
        double r = rng.nextDouble();
        double legendary = tier.ordinal() * 0.035 + (greedy ? 0.12 : 0);
        double rare = 0.18 + tier.ordinal() * 0.09 + (greedy ? 0.15 : 0);
        if (r < legendary) return LEGENDARY;
        if (r < legendary + rare) return RARE;
        return COMMON;
    }

    private static Entry weighted(List<Entry> pool, ThreadLocalRandom rng) {
        double total = 0;
        for (Entry e : pool) total += e.weight();
        double pick = rng.nextDouble() * total;
        for (Entry e : pool) {
            pick -= e.weight();
            if (pick <= 0) return e;
        }
        return pool.get(0);
    }

    private static ItemStack trophyBook(EliteTier tier, ThreadLocalRandom rng) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        Enchantment[] pool = {Enchantment.DAMAGE_ALL, Enchantment.PROTECTION_ENVIRONMENTAL,
                Enchantment.DIG_SPEED, Enchantment.LOOT_BONUS_BLOCKS, Enchantment.LOOT_BONUS_MOBS,
                Enchantment.DURABILITY, Enchantment.MENDING, Enchantment.ARROW_DAMAGE};
        Enchantment ench = pool[rng.nextInt(pool.length)];
        book.addUnsafeEnchantment(ench, 2 + rng.nextInt(1 + tier.ordinal()));
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Trophy of the " + tier.display(), tier.textColor())
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Torn from a fallen elite.", Icon.DIM)
                    .decoration(TextDecoration.ITALIC, false)));
            book.setItemMeta(meta);
        }
        return book;
    }
}
