package com.pallarium.questboard;

import com.pallarium.questboard.data.DataStore;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class QuestBoardPlugin extends JavaPlugin {
    private DataStore store;
    private QuestService quests;
    private BoardMenu menu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new DataStore(this);
        store.load();
        quests = new QuestService(this);
        quests.loadConfig();
        menu = new BoardMenu(this);
        getServer().getPluginManager().registerEvents(new QuestListener(this), this);
        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getScheduler().runTaskTimer(this, store::save, 20L * 60 * 5, 20L * 60 * 5);
        getLogger().info("QuestBoard ready. /quests opens the board.");
    }

    @Override
    public void onDisable() {
        if (store != null) store.save();
    }

    public DataStore store() {
        return store;
    }

    public QuestService quests() {
        return quests;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("reload") && sender.hasPermission("questboard.admin")) {
                reloadConfig();
                quests.loadConfig();
                sender.sendMessage(ChatColor.GREEN + "QuestBoard config reloaded.");
                return true;
            }
            if (sub.equals("reset") && sender.hasPermission("questboard.admin")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/quests reset <player>");
                    return true;
                }
                OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);
                store.reset(t.getUniqueId());
                store.save();
                sender.sendMessage(ChatColor.GREEN + "Reset quests for " + args[1] + ".");
                return true;
            }
            if (sub.equals("reroll") && sender instanceof Player) {
                Player p = (Player) sender;
                var active = quests.active(p);
                var d = quests.ensureToday(p);
                for (var q : active) {
                    if (!d.completed.contains(q.id) && quests.reroll(p, q.id)) {
                        store.save();
                        p.sendMessage(ChatColor.YELLOW + "Rerolled " + q.name + ".");
                        return true;
                    }
                }
                p.sendMessage(ChatColor.RED + "Nothing to reroll or no rerolls left.");
                return true;
            }
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only. Admin: /quests reload | reset <player>");
            return true;
        }
        menu.open((Player) sender);
        return true;
    }
}
