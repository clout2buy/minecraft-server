package com.pallarium.endgame.item;

import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the custom hover card. Every vanilla tooltip line is suppressed and
 * replaced with our own layout, so nothing reads like default Minecraft: no
 * "+7 Attack Damage" grey block, no purple enchant list, no durability row.
 */
public final class Tooltip {

    /** Bumped whenever the card layout changes, so old cards rebuild. */
    private static final int CARD_VERSION = 3;

    private Tooltip() {
    }

    private static final String BAR_FULL = "\u2588";
    private static final String BAR_EMPTY = "\u2591";
    private static final String RULE = "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500";

    /**
     * Applies the full card to an item: tiered name, stat bars, enchant lines,
     * flavour text and the tier footer. Safe to call repeatedly, it rebuilds
     * from scratch each time.
     */
    public static ItemStack apply(Plugin plugin, ItemStack stack, ItemTier tier, String flavour) {
        if (stack == null || stack.getType() == Material.AIR) {
            return stack;
        }
        ItemStack out = stack.clone();
        ItemMeta meta = out.getItemMeta();
        if (meta == null) {
            return out;
        }

        // kill every vanilla tooltip line
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ARMOR_TRIM, ItemFlag.HIDE_POTION_EFFECTS);

        // a forged name is rolled once and kept, so re-carding cannot rename it
        NamespacedKey nameKey = new NamespacedKey(plugin, "item_name");
        String forged = meta.getPersistentDataContainer()
                .get(nameKey, PersistentDataType.STRING);
        if (forged == null) {
            forged = forgeName(out.getType(), tier);
            meta.getPersistentDataContainer()
                    .set(nameKey, PersistentDataType.STRING, forged);
        }
        meta.displayName(Component.text(forged, tier.color())
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();

        // ---- header: what it is, how good it is ------------------------
        lore.add(pair("Type", groupOf(out.getType()), tier.color()));
        lore.add(Component.text("Tier: ", Icon.DIM)
                .append(stars(tier))
                .decoration(TextDecoration.ITALIC, false));

        // ---- vitals: the one number that matters most ------------------
        double health = healthOf(out.getType());
        if (health > 0) {
            lore.add(Component.empty());
            lore.add(attr("❤", "Health", "+" + trim(health), HEALTH));
        }

        // ---- attributes: one group, one icon per kind ------------------
        List<Component> attrs = new ArrayList<>();
        double dmg = attackOf(out.getType());
        double armor = armorOf(out.getType());
        double tough = toughnessOf(out.getType());
        double speed = speedOf(out.getType());
        double fall = fallOf(out.getType());

        if (dmg > 0) {
            attrs.add(attr("⚔", "Attack Damage", "+" + trim(dmg), POWER));
        }
        if (armor > 0) {
            attrs.add(attr("✥", "Armor", "+" + trim(armor), GUARD));
        }
        if (tough > 0) {
            attrs.add(attr("✥", "Armor Toughness", "+" + trim(tough), GUARD));
        }
        if (fall > 0) {
            attrs.add(attr("▸", "Fall Damage Reduction", "+" + trim(fall) + "%", MOVE));
        }
        if (speed > 0) {
            attrs.add(attr("▸", "Attack Speed", "+" + trim(speed), MOVE));
        }
        if (!attrs.isEmpty()) {
            lore.add(Component.empty());
            lore.addAll(attrs);
        }

        // ---- powers ----------------------------------------------------
        Map<Enchantment, Integer> ench = out.getEnchantments();
        if (!ench.isEmpty()) {
            lore.add(Component.empty());
            for (Map.Entry<Enchantment, Integer> e : ench.entrySet()) {
                lore.add(Component.text("✹ ", tier.color())
                        .append(Component.text(enchantName(e.getKey()) + " "
                                + roman(e.getValue()), POWERTEXT))
                        .decoration(TextDecoration.ITALIC, false));
            }
        }

        // ---- flavour last, quiet, italic -------------------------------
        if (flavour != null && !flavour.isBlank()) {
            lore.add(Component.empty());
            for (String s2 : wrap(flavour, 32)) {
                lore.add(Component.text(s2, Icon.DIM)
                        .decoration(TextDecoration.ITALIC, true));
            }
        }

        meta.lore(lore);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "item_tier"),
                PersistentDataType.STRING, tier.name());
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "card_ver"),
                PersistentDataType.INTEGER, CARD_VERSION);
        out.setItemMeta(meta);
        return out;
    }

    /** Convenience: classify and apply in one go. */
    public static ItemStack apply(Plugin plugin, ItemStack stack) {
        return apply(plugin, stack, tierOf(plugin, stack), flavourFor(stack));
    }

    /**
     * True when this item has no card, or carries a card built by an older
     * layout. The old guard only asked whether a tier existed, so items
     * stamped before a lore change kept their stale lines forever.
     */
    public static boolean needsCard(Plugin plugin, ItemStack stack) {
        if (!isEquipment(stack)) {
            return false;
        }
        if (!stack.hasItemMeta()) {
            return true;
        }
        Integer ver = stack.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "card_ver"), PersistentDataType.INTEGER);
        return ver == null || ver < CARD_VERSION;
    }

    /** Reads back a previously stamped tier, falling back to classification. */
    public static ItemTier tierOf(Plugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return ItemTier.of(stack);
        }
        String s = stack.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(plugin, "item_tier"), PersistentDataType.STRING);
        if (s == null) {
            return ItemTier.of(stack);
        }
        try {
            return ItemTier.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return ItemTier.of(stack);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  pieces                                                             */
    /* ------------------------------------------------------------------ */

    /** A labelled bar, e.g. "Power  ████████░░  8.0". */
    private static Component stat(String label, double value, double max, TextColor color) {
        int filled = (int) Math.round(Math.max(0, Math.min(1, value / max)) * 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? BAR_FULL : BAR_EMPTY);
        }
        String pad = label.length() < 6 ? " ".repeat(6 - label.length()) : "";
        return Component.text(label + pad + " ", Icon.DIM)
                .append(Component.text(bar.toString(), color))
                .append(Component.text(" " + trim(value), Icon.TEXT))
                .decoration(TextDecoration.ITALIC, false);
    }

    private static final TextColor HEALTH = TextColor.fromHexString("#F87171");
    private static final TextColor POWER = TextColor.fromHexString("#FB923C");
    private static final TextColor GUARD = TextColor.fromHexString("#60A5FA");
    private static final TextColor MOVE = TextColor.fromHexString("#94A3B8");
    private static final TextColor WEAR = TextColor.fromHexString("#A78BFA");
    private static final TextColor POWERTEXT = TextColor.fromHexString("#C084FC");
    private static final TextColor STAR = TextColor.fromHexString("#FACC15");

    /** "Tier: " stars. Filled count is the tier rank, always out of six. */
    private static Component stars(ItemTier tier) {
        Component c = Component.empty();
        for (int i = 0; i < 6; i++) {
            c = c.append(Component.text("★", i <= tier.rank() ? STAR : Icon.DIM)
                    .decoration(TextDecoration.BOLD, false));
        }
        return c.decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, false);
    }

    /** "Label: value", label dim, value coloured. */
    private static Component pair(String label, String value, TextColor c) {
        return Component.text(label + ": ", Icon.DIM)
                .append(Component.text(value, c))
                .decoration(TextDecoration.ITALIC, false);
    }

    /** "icon Label: +value" — the icon carries the category colour. */
    private static Component attr(String icon, String label, String value, TextColor c) {
        return Component.text(icon + " ", c)
                .append(Component.text(label + ": ", Icon.DIM))
                .append(Component.text(value, c))
                .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Only real equipment gets a card. Bones, string, rotten flesh and ore are
     * just items: a name tag and lore on them promised an ability they never
     * had, so they now drop exactly as vanilla does.
     */
    public static boolean isEquipment(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        String n = stack.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_PICKAXE")
                || n.endsWith("_SHOVEL") || n.endsWith("_HOE")
                || n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")
                || n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT")
                || n.equals("SHIELD") || n.equals("ELYTRA");
    }

    private static final String[] WEAPON_NOUNS = {
        "Fang", "Edge", "Reaver", "Cleaver", "Thorn", "Talon", "Sliver", "Maw"
    };
    private static final String[] ARMOR_NOUNS = {
        "Ward", "Aegis", "Carapace", "Bulwark", "Shell", "Vestment", "Guard", "Husk"
    };
    private static final String[] TOOL_NOUNS = {
        "Delver", "Breaker", "Bite", "Wedge", "Grasp", "Persuader"
    };
    private static final String[] LOW_ADJ = {
        "Chipped", "Dull", "Plain", "Worn", "Rough", "Scuffed"
    };
    private static final String[] MID_ADJ = {
        "Keen", "Tempered", "Banded", "Hardened", "Grim", "Sure"
    };
    private static final String[] HIGH_ADJ = {
        "Ashen", "Hollow", "Vow", "Dread", "Sunken", "Riven", "Starved"
    };
    private static final String[] EPITHETS = {
        "the Drowned King", "a Quiet War", "the Last Watch", "Nine Winters",
        "the Long Dark", "an Unpaid Debt", "the Second Sun", "Broken Oaths"
    };

    /**
     * Builds the custom name that sits on top of the card. The piece type is
     * already on the Type row, so the name is free to be a name.
     */
    private static String forgeName(Material m, ItemTier tier) {
        String group = groupOf(m);
        boolean gear = group.equals("Armor") || group.equals("Weapon")
                || group.equals("Tool");
        if (!gear) {
            return pretty(m);
        }
        java.util.concurrent.ThreadLocalRandom rng =
                java.util.concurrent.ThreadLocalRandom.current();
        String[] nouns = switch (group) {
            case "Weapon" -> WEAPON_NOUNS;
            case "Tool" -> TOOL_NOUNS;
            default -> ARMOR_NOUNS;
        };
        String noun = nouns[rng.nextInt(nouns.length)];
        int rank = tier.ordinal();
        // the best gear earns a title instead of an adjective
        if (rank >= ItemTier.EPIC.ordinal() && rng.nextBoolean()) {
            return noun + " of " + EPITHETS[rng.nextInt(EPITHETS.length)];
        }
        String[] adjectives = rank >= ItemTier.RARE.ordinal() ? HIGH_ADJ
                : rank >= ItemTier.UNCOMMON.ordinal() ? MID_ADJ : LOW_ADJ;
        return adjectives[rng.nextInt(adjectives.length)] + " " + noun;
    }

    /** The material family, shown as the small italic line under the name. */
    private static String familyOf(Material m) {
        String n = m.name();
        if (n.startsWith("NETHERITE_")) return "Netherite";
        if (n.startsWith("DIAMOND_")) return "Diamond";
        if (n.startsWith("IRON_")) return "Iron";
        if (n.startsWith("GOLDEN_")) return "Golden";
        if (n.startsWith("STONE_")) return "Stone";
        if (n.startsWith("WOODEN_")) return "Wooden";
        if (n.startsWith("CHAINMAIL_")) return "Chainmail";
        if (n.startsWith("LEATHER_")) return "Leather";
        return null;
    }

    /** Broad category for the "Type:" row. */
    private static String groupOf(Material m) {
        String n = m.name();
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")) return "Armor";
        if (m == Material.SHIELD) return "Armor";
        if (n.endsWith("_SWORD") || n.endsWith("_AXE") || m == Material.TRIDENT
                || m == Material.BOW || m == Material.CROSSBOW) return "Weapon";
        if (n.endsWith("_PICKAXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE")) return "Tool";
        if (m == Material.ENCHANTED_BOOK) return "Tome";
        if (m.isEdible()) return "Consumable";
        if (m.isBlock()) return "Material";
        return "Item";
    }

    private static double healthOf(Material m) {
        double a = armorOf(m);
        return a > 0 ? Math.round(a * 1.5) : 0;
    }

    private static double toughnessOf(Material m) {
        String n = m.name();
        if (n.startsWith("NETHERITE_")) return 3;
        if (n.startsWith("DIAMOND_")) return 2;
        return 0;
    }

    private static double fallOf(Material m) {
        return m.name().endsWith("_BOOTS") ? 15 : 0;
    }

    private static Component line(String s, TextColor c) {
        return Component.text(s, c).decoration(TextDecoration.ITALIC, false);
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.format("%.1f", d);
    }

    /* ------------------------------------------------------------------ */
    /*  stat tables                                                        */
    /* ------------------------------------------------------------------ */

    private static double attackOf(Material m) {
        String n = m.name();
        double base = n.contains("NETHERITE") ? 8 : n.contains("DIAMOND") ? 7
                : n.contains("IRON") ? 6 : n.contains("STONE") ? 5
                : n.contains("GOLDEN") ? 4 : n.contains("WOODEN") ? 4 : 0;
        if (base == 0) {
            return m == Material.TRIDENT ? 9 : 0;
        }
        if (n.endsWith("_SWORD")) return base;
        if (n.endsWith("_AXE")) return base + 2;
        if (n.endsWith("_PICKAXE")) return base - 4;
        if (n.endsWith("_SHOVEL")) return base - 4.5;
        if (n.endsWith("_HOE")) return base - 5;
        return 0;
    }

    private static double armorOf(Material m) {
        return switch (m) {
            case NETHERITE_HELMET, DIAMOND_HELMET -> 3;
            case NETHERITE_CHESTPLATE, DIAMOND_CHESTPLATE -> 8;
            case NETHERITE_LEGGINGS, DIAMOND_LEGGINGS -> 6;
            case NETHERITE_BOOTS, DIAMOND_BOOTS -> 3;
            case IRON_HELMET -> 2;
            case IRON_CHESTPLATE -> 6;
            case IRON_LEGGINGS -> 5;
            case IRON_BOOTS -> 2;
            case CHAINMAIL_HELMET, GOLDEN_HELMET, LEATHER_HELMET -> 2;
            case CHAINMAIL_CHESTPLATE, GOLDEN_CHESTPLATE, LEATHER_CHESTPLATE -> 5;
            case CHAINMAIL_LEGGINGS, GOLDEN_LEGGINGS, LEATHER_LEGGINGS -> 4;
            case CHAINMAIL_BOOTS, GOLDEN_BOOTS, LEATHER_BOOTS -> 1;
            case SHIELD -> 4;
            default -> 0;
        };
    }

    private static double speedOf(Material m) {
        String n = m.name();
        if (n.endsWith("_SWORD")) return 1.6;
        if (n.endsWith("_AXE")) return 1.0;
        if (n.endsWith("_PICKAXE")) return 1.2;
        if (n.endsWith("_SHOVEL")) return 1.0;
        if (m == Material.TRIDENT) return 1.1;
        if (m == Material.BOW || m == Material.CROSSBOW) return 1.0;
        return 0;
    }

    private static String kindOf(Material m) {
        String n = m.name();
        if (n.endsWith("_SWORD") || m == Material.TRIDENT) return "Blade";
        if (n.endsWith("_AXE")) return "Axe";
        if (n.endsWith("_PICKAXE")) return "Pick";
        if (n.endsWith("_SHOVEL")) return "Spade";
        if (n.endsWith("_HOE")) return "Hoe";
        if (m == Material.BOW || m == Material.CROSSBOW) return "Ranged";
        if (n.endsWith("_HELMET")) return "Helm";
        if (n.endsWith("_CHESTPLATE")) return "Chest";
        if (n.endsWith("_LEGGINGS")) return "Legs";
        if (n.endsWith("_BOOTS")) return "Boots";
        if (m == Material.SHIELD) return "Shield";
        if (m == Material.ENCHANTED_BOOK) return "Tome";
        if (m.isEdible()) return "Provision";
        if (m.isBlock()) return "Material";
        return "Relic";
    }

    /**
     * Flavour lines survive only for equipment. Materials get nothing, since a
     * poetic line on a bone implied an ability that did not exist.
     */
    public static String flavourFor(ItemStack stack) {
        if (stack == null || !isEquipment(stack)) {
            return null;
        }
        return switch (stack.getType()) {
            case ELYTRA -> "Someone fell a long way in these.";
            case TRIDENT -> "The sea wants it back.";
            case NETHERITE_SWORD -> "Older than the stone around it.";
            case NETHERITE_AXE -> "It does not negotiate.";
            case SHIELD -> "Scarred on one side only.";
            default -> null;
        };
    }

    /* ------------------------------------------------------------------ */
    /*  text helpers                                                       */
    /* ------------------------------------------------------------------ */

    private static String enchantName(Enchantment e) {
        String raw = e.getKey().getKey().replace('_', ' ');
        return switch (raw) {
            case "sharpness" -> "Keen Edge";
            case "protection" -> "Warding";
            case "unbreaking" -> "Endurance";
            case "fire aspect" -> "Emberbrand";
            case "looting" -> "Spoils";
            case "efficiency" -> "Swiftwork";
            case "fortune" -> "Abundance";
            case "power" -> "Draw Force";
            case "mending" -> "Selfmend";
            case "knockback" -> "Repulse";
            case "silk touch" -> "Perfect Cut";
            case "thorns" -> "Reprisal";
            case "feather falling" -> "Soft Landing";
            case "depth strider" -> "Tidewalk";
            default -> capitalize(raw);
        };
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    private static List<String> wrap(String s, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : s.split(" ")) {
            if (cur.length() + word.length() + 1 > width && cur.length() > 0) {
                out.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) {
                cur.append(' ');
            }
            cur.append(word);
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }

    private static String capitalize(String s) {
        StringBuilder sb = new StringBuilder();
        for (String p : s.split(" ")) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    public static String pretty(Material m) {
        return capitalize(m.name().toLowerCase().replace('_', ' '));
    }
}
