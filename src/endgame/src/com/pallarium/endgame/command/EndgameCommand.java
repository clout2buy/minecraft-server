package com.pallarium.endgame.command;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.data.PlayerProfile;
import com.pallarium.endgame.service.Fx;
import com.pallarium.endgame.skill.Skill;
import com.pallarium.endgame.ui.Icon;
import com.pallarium.endgame.ui.MainMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The menu is the product, this command is only the door plus admin tools.
 */
public class EndgameCommand implements CommandExecutor, TabCompleter {

    private final EndgamePlugin plugin;

    public EndgameCommand(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(tag().append(
                        Component.text("Only a player can open the menu.", Icon.BAD)));
                return true;
            }
            Fx.open(player);
            new MainMenu(plugin, player).open();
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "admin" -> {
                if (!sender.hasPermission("endgame.admin")) {
                    deny(sender);
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(tag().append(
                            Component.text("Only a player can open the console.", Icon.BAD)));
                    return true;
                }
                Fx.open(player);
                new com.pallarium.endgame.ui.AdminMenu(plugin, player).open();
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("endgame.admin")) {
                    deny(sender);
                    return true;
                }
                plugin.settings().reload();
                sender.sendMessage(tag().append(Component.text("Config reloaded.", Icon.GOOD)));
                return true;
            }
            case "boss" -> {
                if (!sender.hasPermission("endgame.admin")) {
                    deny(sender);
                    return true;
                }
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(tag().append(Component.text(
                            "Players only.", Icon.BAD)));
                    return true;
                }
                if (args.length < 2) {
                    new com.pallarium.endgame.ui.BossMenu(plugin, p).open();
                    return true;
                }
                if (args[1].equalsIgnoreCase("purge")) {
                    int n = plugin.bosses().purge();
                    sender.sendMessage(tag().append(Component.text(
                            "Purged " + n + " boss fight" + (n == 1 ? "" : "s") + ".",
                            Icon.GOOD)));
                    return true;
                }
                if (args[1].equalsIgnoreCase("list")) {
                    for (com.pallarium.endgame.boss.BossDef d
                            : com.pallarium.endgame.boss.Bosses.all()) {
                        sender.sendMessage(Component.text("  " + d.id() + "  ", Icon.DIM)
                                .append(Component.text(d.display(), d.color()))
                                .append(Component.text("  " + (int) d.health() + "hp  "
                                        + d.phases().size() + " phases", Icon.DIM)));
                    }
                    return true;
                }
                com.pallarium.endgame.boss.BossInstance inst =
                        plugin.bosses().spawn(args[1].toLowerCase(), p.getLocation());
                if (inst == null) {
                    sender.sendMessage(tag().append(Component.text(
                            "Unknown boss. Try /endgame boss list", Icon.BAD)));
                } else {
                    sender.sendMessage(tag().append(Component.text(
                            "Spawned " + inst.def().display() + ".", Icon.GOOD)));
                }
                return true;
            }
            case "elite" -> {
                if (!sender.hasPermission("endgame.admin")) {
                    deny(sender);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(tag().append(Component.text(
                            "Usage: /endgame elite <tier> [mob]", Icon.TEXT)));
                    StringBuilder tiers = new StringBuilder();
                    for (com.pallarium.endgame.mob.EliteTier t
                            : com.pallarium.endgame.mob.EliteTier.values()) {
                        if (tiers.length() > 0) tiers.append(", ");
                        tiers.append(t.name().toLowerCase());
                    }
                    sender.sendMessage(tag().append(Component.text(
                            tiers.toString(), Icon.DIM)));
                    return true;
                }
                com.pallarium.endgame.mob.EliteTier tier;
                try {
                    tier = com.pallarium.endgame.mob.EliteTier
                            .valueOf(args[1].toUpperCase());
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(tag().append(Component.text("Unknown tier.", Icon.BAD)));
                    return true;
                }
                org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.ZOMBIE;
                if (args.length >= 3) {
                    try {
                        type = org.bukkit.entity.EntityType.valueOf(args[2].toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        sender.sendMessage(tag().append(Component.text("Unknown mob.", Icon.BAD)));
                        return true;
                    }
                }
                org.bukkit.Location at;
                if (sender instanceof Player player) {
                    at = player.getLocation().add(
                            player.getLocation().getDirection().multiply(3));
                } else if (args.length >= 6) {
                    // console form: /endgame elite <tier> <mob> <x> <y> <z> [world]
                    org.bukkit.World world = args.length >= 7
                            ? plugin.getServer().getWorld(args[6])
                            : plugin.getServer().getWorlds().get(0);
                    if (world == null) {
                        sender.sendMessage(tag().append(Component.text("Unknown world.", Icon.BAD)));
                        return true;
                    }
                    try {
                        at = new org.bukkit.Location(world,
                                Double.parseDouble(args[3]),
                                Double.parseDouble(args[4]),
                                Double.parseDouble(args[5]));
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(tag().append(Component.text(
                                "Coordinates must be numbers.", Icon.BAD)));
                        return true;
                    }
                } else {
                    sender.sendMessage(tag().append(Component.text(
                            "From console: /endgame elite <tier> <mob> <x> <y> <z> [world]",
                            Icon.TEXT)));
                    return true;
                }
                at.getChunk().load();
                org.bukkit.entity.Entity spawned = at.getWorld().spawnEntity(at, type);
                if (!(spawned instanceof org.bukkit.entity.LivingEntity le)) {
                    spawned.remove();
                    sender.sendMessage(tag().append(Component.text(
                            "That mob cannot be an elite.", Icon.BAD)));
                    return true;
                }
                plugin.elites().promote(le, tier);
                sender.sendMessage(tag().append(Component.text(
                        "Spawned " + plugin.elites().nameOf(le), tier.textColor())));
                return true;
            }
            case "card" -> {
                if (!sender.hasPermission("endgame.admin")) {
                    deny(sender);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(tag().append(Component.text(
                            "Usage: /endgame card <material>", Icon.TEXT)));
                    return true;
                }
                org.bukkit.Material mat;
                try {
                    mat = org.bukkit.Material.valueOf(args[1].toUpperCase());
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(tag().append(Component.text("Unknown material.", Icon.BAD)));
                    return true;
                }
                org.bukkit.inventory.ItemStack card =
                        com.pallarium.endgame.item.Tooltip.apply(plugin,
                                new org.bukkit.inventory.ItemStack(mat));
                org.bukkit.World w = plugin.getServer().getWorlds().get(0);
                double cx = 900, cy = 60, cz = 900;
                if (args.length >= 5) {
                    try {
                        cx = Double.parseDouble(args[2]);
                        cy = Double.parseDouble(args[3]);
                        cz = Double.parseDouble(args[4]);
                    } catch (NumberFormatException ignored) {
                        // keep defaults
                    }
                }
                if (sender instanceof Player p) {
                    p.getInventory().addItem(card);
                } else {
                    w.dropItem(new org.bukkit.Location(w, cx, cy, cz), card);
                }
                sender.sendMessage(tag().append(Component.text(
                        "Card: " + mat.name(), Icon.GOOD)));
                return true;
            }
            case "give" -> {
                if (!sender.hasPermission("endgame.admin")) {
                    deny(sender);
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(tag().append(Component.text(
                            "Usage: /endgame give <player> <skill> <xp>", Icon.TEXT)));
                    return true;
                }
                Player target = plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(tag().append(Component.text("Player not online.", Icon.BAD)));
                    return true;
                }
                Skill skill = parseSkill(args[2]);
                if (skill == null) {
                    sender.sendMessage(tag().append(Component.text("Unknown skill.", Icon.BAD)));
                    return true;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(tag().append(Component.text("XP must be a number.", Icon.BAD)));
                    return true;
                }
                plugin.xp().award(target, skill, amount);
                sender.sendMessage(tag().append(Component.text(
                        "Gave " + amount + " " + skill.display() + " xp to " + target.getName() + ".",
                        Icon.GOOD)));
                return true;
            }
            case "reset" -> {
                if (!sender.hasPermission("endgame.admin")) {
                    deny(sender);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(tag().append(Component.text(
                            "Usage: /endgame reset <player>", Icon.TEXT)));
                    return true;
                }
                Player target = plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(tag().append(Component.text("Player not online.", Icon.BAD)));
                    return true;
                }
                UUID id = target.getUniqueId();
                plugin.store().unload(id);
                java.io.File file = new java.io.File(
                        new java.io.File(plugin.getDataFolder(), "playerdata"), id + ".yml");
                if (file.exists() && !file.delete()) {
                    sender.sendMessage(tag().append(Component.text("Could not delete data.", Icon.BAD)));
                    return true;
                }
                PlayerProfile fresh = plugin.store().getOrCreate(id, target.getName());
                plugin.menuItem().sync(target, fresh);
                sender.sendMessage(tag().append(Component.text(
                        "Wiped " + target.getName() + "'s progress.", Icon.GOOD)));
                return true;
            }
            case "top" -> {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    var list = plugin.store().topByPower(10);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(tag().append(Component.text("Top players by Power",
                                Icon.TEXT)));
                        int place = 1;
                        for (var e : list) {
                            sender.sendMessage(Component.text("  #" + place++ + "  ", Icon.DIM)
                                    .append(Component.text(e.name(), Icon.TEXT))
                                    .append(Component.text("   " + e.power(), Icon.NEON))
                                    .decoration(TextDecoration.ITALIC, false));
                        }
                        if (list.isEmpty()) {
                            sender.sendMessage(Component.text("  Nobody has trained yet.", Icon.DIM));
                        }
                    });
                });
                return true;
            }
            case "purge" -> {
                if (!sender.hasPermission("endgame.admin")) {
                    deny(sender);
                    return true;
                }
                int mobs = 0;
                int bars = 0;
                for (org.bukkit.World w : plugin.getServer().getWorlds()) {
                    for (org.bukkit.entity.Entity e : w.getEntities()) {
                        if (e instanceof org.bukkit.entity.TextDisplay td
                                && td.getPersistentDataContainer().has(plugin.keyBarOwner(),
                                org.bukkit.persistence.PersistentDataType.STRING)) {
                            td.remove();
                            bars++;
                        } else if (e instanceof org.bukkit.entity.LivingEntity le
                                && !(e instanceof Player) && plugin.elites().isElite(le)) {
                            le.remove();
                            mobs++;
                        }
                    }
                }
                sender.sendMessage(tag().append(Component.text(
                        "Cleared " + mobs + " old elites and " + bars + " bars.", Icon.GOOD)));
                return true;
            }
            case "event" -> {
                if (!sender.hasPermission("endgame.admin")) {
                    deny(sender);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(tag().append(Component.text(
                            "Usage: /endgame event <name|list|stop>", Icon.TEXT)));
                    StringBuilder names = new StringBuilder();
                    for (com.pallarium.endgame.event.WorldEvent e
                            : com.pallarium.endgame.event.WorldEvent.values()) {
                        if (names.length() > 0) names.append(", ");
                        names.append(e.name().toLowerCase());
                    }
                    sender.sendMessage(Component.text("  " + names, Icon.DIM));
                    return true;
                }
                String which = args[1].toLowerCase(Locale.ROOT);
                if (which.equals("list")) {
                    var running = plugin.events().active();
                    long[] c = plugin.events().counters();
                    sender.sendMessage(tag().append(Component.text(
                            running.size() + " running  (fired " + c[0]
                                    + ", held " + c[1] + ", lost " + c[2] + ")", Icon.TEXT)));
                    for (var ev : running) {
                        sender.sendMessage(Component.text("  " + ev.def().title()
                                + "  stage " + (ev.stageIndex() + 1) + "/" + ev.stageCount()
                                + "  " + ev.progress() + "/" + ev.stage().goal()
                                + "  " + (ev.ticksLeft() / 20) + "s left", Icon.DIM));
                    }
                    return true;
                }
                if (which.equals("stop")) {
                    plugin.events().shutdown();
                    sender.sendMessage(tag().append(Component.text(
                            "All events cleared.", Icon.GOOD)));
                    return true;
                }
                com.pallarium.endgame.event.WorldEvent def;
                try {
                    def = com.pallarium.endgame.event.WorldEvent.valueOf(which.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(tag().append(Component.text("Unknown event.", Icon.BAD)));
                    return true;
                }
                org.bukkit.Location where;
                Player cause = null;
                if (sender instanceof Player p) {
                    where = p.getLocation();
                    cause = p;
                } else if (args.length >= 5) {
                    // console form: /endgame event <name> <x> <y> <z> [world]
                    org.bukkit.World world = args.length >= 6
                            ? plugin.getServer().getWorld(args[5])
                            : plugin.getServer().getWorlds().get(0);
                    if (world == null) {
                        sender.sendMessage(tag().append(Component.text("Unknown world.", Icon.BAD)));
                        return true;
                    }
                    try {
                        where = new org.bukkit.Location(world,
                                Double.parseDouble(args[2]),
                                Double.parseDouble(args[3]),
                                Double.parseDouble(args[4]));
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(tag().append(Component.text(
                                "Coordinates must be numbers.", Icon.BAD)));
                        return true;
                    }
                    where.getChunk().load();
                } else {
                    sender.sendMessage(tag().append(Component.text(
                            "From console: /endgame event <name> <x> <y> <z> [world]", Icon.TEXT)));
                    return true;
                }
                var ev = plugin.events().fire(def, where, cause);
                sender.sendMessage(tag().append(Component.text(
                        ev == null ? "Event cap reached." : "Started " + def.title(),
                        ev == null ? Icon.BAD : Icon.GOOD)));
                return true;
            }
            case "heat" -> {
                if (!sender.hasPermission("endgame.admin")) {
                    deny(sender);
                    return true;
                }
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(tag().append(Component.text(
                            "Run this in game.", Icon.BAD)));
                    return true;
                }
                double thr = plugin.getConfig().getDouble("events.heat-threshold", 100.0);
                sender.sendMessage(tag().append(Component.text("Your event heat:", Icon.TEXT)));
                for (com.pallarium.endgame.event.Trigger t
                        : com.pallarium.endgame.event.Trigger.values()) {
                    double h = plugin.events().heatOf(p, t);
                    sender.sendMessage(Component.text(String.format(
                            "  %-10s %.0f / %.0f", t.name().toLowerCase(), h, thr),
                            h >= thr * 0.85 ? Icon.WARN : Icon.DIM));
                }
                return true;
            }
            case "elitestats" -> {
                if (!sender.hasPermission("endgame.admin")) {
                    deny(sender);
                    return true;
                }
                long[] c = plugin.elites().counters();
                double spawnPct = c[0] == 0 ? 0 : (c[1] * 100.0 / c[0]);
                double ascPct = c[2] == 0 ? 0 : (c[3] * 100.0 / c[2]);
                sender.sendMessage(tag().append(Component.text(
                        "Since last load:", Icon.TEXT)));
                sender.sendMessage(Component.text(String.format(
                        "  natural spawns rolled %d, became elite %d (%.2f%%)",
                        c[0], c[1], spawnPct), Icon.DIM));
                sender.sendMessage(Component.text(String.format(
                        "  kills rolled %d, ascended %d (%.2f%%)",
                        c[2], c[3], ascPct), Icon.DIM));
                return true;
            }
            case "help" -> {
                sender.sendMessage(tag().append(Component.text("Commands", Icon.TEXT)));
                sender.sendMessage(line("/endgame", "open the menu"));
                sender.sendMessage(line("/endgame top", "print the leaderboard"));
                if (sender.hasPermission("endgame.admin")) {
                    sender.sendMessage(line("/endgame give <player> <skill> <xp>", "award xp"));
                    sender.sendMessage(line("/endgame elite <tier> [mob]", "spawn an elite"));
                    sender.sendMessage(line("/endgame reset <player>", "wipe progress"));
                    sender.sendMessage(line("/endgame reload", "reload config"));
                }
                return true;
            }
            default -> {
                if (sender instanceof Player player) {
                    Fx.open(player);
                    new MainMenu(plugin, player).open();
                } else {
                    sender.sendMessage(tag().append(
                            Component.text("Unknown subcommand. Try /endgame help", Icon.TEXT)));
                }
                return true;
            }
        }
    }

    private Component tag() {
        return Component.text("[ENDGAME] ", Icon.NEON)
                .decoration(TextDecoration.ITALIC, false);
    }

    private Component line(String cmd, String desc) {
        return Component.text("  " + cmd + "  ", Icon.ACCENT)
                .append(Component.text(desc, Icon.DIM))
                .decoration(TextDecoration.ITALIC, false);
    }

    private void deny(CommandSender sender) {
        sender.sendMessage(tag().append(Component.text("You lack permission.", Icon.BAD)));
    }

    private Skill parseSkill(String raw) {
        for (Skill s : Skill.values()) {
            if (s.key().equalsIgnoreCase(raw)) {
                return s;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias,
                                      String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("top");
            out.add("help");
            if (sender.hasPermission("endgame.admin")) {
                out.addAll(Arrays.asList("give", "reset", "reload", "boss"));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("boss")) {
            out.add("list");
            out.add("purge");
            for (com.pallarium.endgame.boss.BossDef d
                    : com.pallarium.endgame.boss.Bosses.all()) {
                out.add(d.id());
            }
        } else if (args.length == 2
                && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("reset"))) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                out.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Skill s : Skill.values()) {
                out.add(s.key());
            }
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
