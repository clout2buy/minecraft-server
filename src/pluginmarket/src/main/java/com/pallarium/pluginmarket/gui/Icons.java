package com.pallarium.pluginmarket.gui;

import com.pallarium.pluginmarket.model.Category;
import com.pallarium.pluginmarket.model.PluginInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Picks a Material that says what a plugin does, from its name and tagline. */
public final class Icons {
    private static final Map<Material, List<String>> RULES = new LinkedHashMap<>();

    static {
        RULES.put(Material.EMERALD, List.of("econom", "shop", "money", "coin", "bank", "market", "sell", "buy", "vault", "auction"));
        RULES.put(Material.DIAMOND_SWORD, List.of("pvp", "combat", "duel", "kit", "arena", "battle", "fight", "war", "kill"));
        RULES.put(Material.NAME_TAG, List.of("chat", "message", "msg", "tag", "prefix", "nick", "format", "announce", "broadcast"));
        RULES.put(Material.OAK_SIGN, List.of("sign", "hologram", "display", "scoreboard", "tab", "bossbar", "title"));
        RULES.put(Material.GRASS_BLOCK, List.of("world", "terrain", "generat", "biome", "border", "region", "claim", "land", "plot"));
        RULES.put(Material.ANVIL, List.of("enchant", "item", "craft", "recipe", "custom", "smith", "repair"));
        RULES.put(Material.IRON_DOOR, List.of("protect", "guard", "lock", "anti", "cheat", "ban", "punish", "security", "spam", "block"));
        RULES.put(Material.COMMAND_BLOCK, List.of("admin", "staff", "command", "manage", "essential", "core", "tool", "util", "api", "lib"));
        RULES.put(Material.ZOMBIE_HEAD, List.of("mob", "boss", "monster", "creature", "spawn", "entity", "pet"));
        RULES.put(Material.EXPERIENCE_BOTTLE, List.of("level", "skill", "rank", "xp", "rpg", "quest", "class", "mmo", "progress"));
        RULES.put(Material.ENDER_PEARL, List.of("teleport", "tp", "warp", "home", "portal", "travel", "spawn"));
        RULES.put(Material.CHEST, List.of("inventory", "backpack", "storage", "chest", "vault", "gui", "menu"));
        RULES.put(Material.REDSTONE, List.of("placeholder", "hook", "bridge", "integration", "discord", "web", "sync"));
        RULES.put(Material.BEACON, List.of("hub", "lobby", "server", "network", "bungee", "proxy", "queue"));
        RULES.put(Material.FIREWORK_ROCKET, List.of("particle", "effect", "cosmetic", "trail", "firework", "fx", "animation"));
        RULES.put(Material.CAKE, List.of("fun", "party", "game", "minigame", "event", "hunt", "race", "parkour"));
        RULES.put(Material.WHEAT, List.of("farm", "crop", "harvest", "grow", "plant"));
        RULES.put(Material.JUKEBOX, List.of("music", "sound", "song", "audio", "radio"));
        RULES.put(Material.PAINTING, List.of("map", "image", "picture", "banner", "art"));
        RULES.put(Material.CLOCK, List.of("timer", "schedul", "restart", "cooldown", "daily", "vote", "reward"));
        RULES.put(Material.WRITABLE_BOOK, List.of("skript", "script", ".sk", "addon"));
    }

    private Icons() {
    }

    public static Material forPlugin(PluginInfo info) {
        String hay = (info.name + " " + info.tag).toLowerCase(Locale.ROOT);
        for (Map.Entry<Material, List<String>> e : RULES.entrySet()) {
            for (String kw : e.getValue()) if (hay.contains(kw)) return e.getKey();
        }
        if (info.category == Category.SKRIPT) return Material.WRITABLE_BOOK;
        return info.premium ? Material.GOLD_BLOCK : Material.BOOK;
    }

    /** Player head skinned from the creator's Minecraft username, when it resolves. */
    public static ItemStack creatorHead(String author) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        String clean = author.replaceAll("[^A-Za-z0-9_]", "");
        if (!clean.isEmpty() && clean.length() <= 16) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(clean));
        }
        head.setItemMeta(meta);
        return head;
    }
}
