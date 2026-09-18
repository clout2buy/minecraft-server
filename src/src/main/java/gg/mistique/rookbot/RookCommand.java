package gg.mistique.rookbot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public final class RookCommand implements TabExecutor {
    private static final List<String> SUB = List.of("spawn","dismiss","follow","stay","come","wood","stone","house","inv","give","status","possess","stop");
    private final RookBotPlugin plugin;
    RookCommand(RookBotPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        if (!(sender instanceof Player p)) { sender.sendMessage("players only"); return true; }
        if (a.length == 0) { help(p); return true; }
        String sub = a[0].toLowerCase();
        if (sub.equals("spawn")) { plugin.spawn(p); return true; }
        Bot b = plugin.bot(p);
        if (b == null || !b.alive()) { msg(p, "no bot. /rook spawn"); return true; }
        switch (sub) {
            case "dismiss" -> { plugin.dismiss(p); msg(p, "gone."); }
            case "follow"  -> { b.follow(); msg(p, "following."); }
            case "stay"    -> { b.stay(); msg(p, "staying put."); }
            case "come"    -> { b.come(); msg(p, "on my way."); }
            case "wood"    -> { int n = num(a, 16); b.gather(Bot.Mode.WOOD, n); msg(p, "getting " + n + " logs."); }
            case "stone"   -> { int n = num(a, 32); b.gather(Bot.Mode.STONE, n); msg(p, "getting " + n + " stone."); }
            case "house"   -> b.buildHouse(p);
            case "inv"     -> p.openInventory(b.inventory());
            case "give"    -> { int n = b.giveAll(p); msg(p, n == 0 ? "bag's empty." : "handed you " + n + " items."); }
            case "status"  -> msg(p, b.status());
            case "possess" -> b.possess(p);
            case "stop"    -> { if (b.mode() == Bot.Mode.POSSESSED) b.unpossess(); else { b.stop(); msg(p, "stopped."); } }
            default -> help(p);
        }
        return true;
    }

    private static int num(String[] a, int def) { try { return Math.max(1, Math.min(256, Integer.parseInt(a[1]))); } catch (Exception e) { return def; } }
    private void help(Player p) {
        msg(p, "/rook spawn | dismiss | follow | stay | come");
        msg(p, "/rook wood [n] | stone [n] | house | inv | give | status");
        msg(p, "/rook possess  - ride in the bot; /rook stop to leave");
    }
    private static void msg(Player p, String s) { p.sendMessage(Component.text("[Rook] ", NamedTextColor.AQUA).append(Component.text(s, NamedTextColor.WHITE))); }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        return a.length == 1 ? SUB.stream().filter(x -> x.startsWith(a[0].toLowerCase())).toList() : List.of();
    }
}
