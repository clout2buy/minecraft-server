package com.pallarium.endgame.ui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fluent item builder. Strips vanilla italics and hides attribute clutter so
 * every menu reads clean.
 */
public class Icon {

    public static final TextColor NEON = TextColor.fromHexString("#22D3EE");
    public static final TextColor ACCENT = TextColor.fromHexString("#A78BFA");
    public static final TextColor GOOD = TextColor.fromHexString("#4ADE80");
    public static final TextColor WARN = TextColor.fromHexString("#FACC15");
    public static final TextColor BAD = TextColor.fromHexString("#F87171");
    public static final TextColor DIM = TextColor.fromHexString("#6B7280");
    public static final TextColor TEXT = TextColor.fromHexString("#D1D5DB");

    private final ItemStack stack;
    private final ItemMeta meta;
    private final List<Component> lore = new ArrayList<>();

    private Icon(Material material, int amount) {
        this.stack = new ItemStack(material, Math.max(1, Math.min(64, amount)));
        this.meta = stack.getItemMeta();
    }

    public static Icon of(Material material) {
        return new Icon(material, 1);
    }

    public static Icon of(Material material, int amount) {
        return new Icon(material, amount);
    }

    /** Custom textured player head from a base64 skin value. */
    public static Icon head(String texture) {
        return head(texture, 1);
    }

    public static Icon head(String texture, int amount) {
        Icon icon = new Icon(Material.PLAYER_HEAD, amount);
        icon.applyTexture(texture);
        return icon;
    }

    private void applyTexture(String texture) {
        if (!(meta instanceof SkullMeta skull) || texture == null || texture.isEmpty()) {
            return;
        }
        UUID id = UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
        // Paper rejects a null name on some builds, so always supply one.
        String name = "eg" + Integer.toHexString(texture.hashCode());
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        try {
            PlayerProfile profile = Bukkit.createProfile(id, name);
            profile.setProperty(new ProfileProperty("textures", texture));
            skull.setPlayerProfile(profile);
            return;
        } catch (Throwable first) {
            lastFailure = first;
        }
        try {
            // Fallback for servers where the Paper profile API differs: go
            // straight at the Mojang GameProfile behind the SkullMeta.
            Object gameProfile = Class.forName("com.mojang.authlib.GameProfile")
                    .getConstructor(UUID.class, String.class)
                    .newInstance(id, name);
            Object properties = gameProfile.getClass()
                    .getMethod("getProperties").invoke(gameProfile);
            Object property = Class.forName("com.mojang.authlib.properties.Property")
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", texture);
            properties.getClass()
                    .getMethod("put", Object.class, Object.class)
                    .invoke(properties, "textures", property);

            java.lang.reflect.Field field = skull.getClass().getDeclaredField("profile");
            field.setAccessible(true);
            field.set(skull, gameProfile);
        } catch (Throwable second) {
            if (!warned) {
                warned = true;
                Bukkit.getLogger().warning("[ENDGAME] Custom head textures unavailable: "
                        + (lastFailure != null ? lastFailure : second));
            }
        }
    }

    private static Throwable lastFailure;
    private static boolean warned;

    public Icon name(String text, TextColor color) {
        if (meta != null) {
            meta.displayName(Component.text(text, color)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
        }
        return this;
    }

    public Icon plainName(String text, TextColor color) {
        if (meta != null) {
            meta.displayName(Component.text(text, color).decoration(TextDecoration.ITALIC, false));
        }
        return this;
    }

    public Icon line(String text, TextColor color) {
        lore.add(Component.text(text, color).decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public Icon line(Component component) {
        lore.add(component.decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public Icon blank() {
        lore.add(Component.empty());
        return this;
    }

    public Icon glow(boolean on) {
        if (on && meta != null) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE,
                    ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_POTION_EFFECTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // --- shared widgets ---

    public static ItemStack filler() {
        return Icon.of(Material.BLACK_STAINED_GLASS_PANE)
                .plainName(" ", NamedTextColor.DARK_GRAY)
                .build();
    }

    public static ItemStack edge() {
        return Icon.of(Material.GRAY_STAINED_GLASS_PANE)
                .plainName(" ", NamedTextColor.DARK_GRAY)
                .build();
    }

    public static ItemStack back(String target) {
        return Icon.head(Heads.ARROW_LEFT)
                .name("Back", TEXT)
                .line("Return to " + target, DIM)
                .build();
    }

    public static ItemStack close() {
        return Icon.head(Heads.CLOSE)
                .name("Close", BAD)
                .line("Shut the menu", DIM)
                .build();
    }

    /** Directional navigation head. Dim variant when the move is unavailable. */
    public static ItemStack nav(String texture, String label, String sub, boolean active) {
        return Icon.head(texture)
                .name(label, active ? NEON : DIM)
                .line(sub, DIM)
                .build();
    }

    /**
     * Unicode progress bar, filled portion in {@code color}.
     */
    public static Component bar(double progress, int width, TextColor color) {
        int filled = (int) Math.round(Math.max(0, Math.min(1, progress)) * width);
        Component out = Component.empty();
        if (filled > 0) {
            out = out.append(Component.text("\u2588".repeat(filled), color));
        }
        if (filled < width) {
            out = out.append(Component.text("\u2588".repeat(width - filled),
                    TextColor.fromHexString("#27272A")));
        }
        return out.decoration(TextDecoration.ITALIC, false);
    }

    public static String compact(long value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        }
        if (value >= 10_000) {
            return String.format("%.1fk", value / 1000.0);
        }
        return String.valueOf(value);
    }
}
