package com.pallarium.eventmaker;

import com.pallarium.eventmaker.gui.ChatInput;
import com.pallarium.eventmaker.gui.MainMenu;
import com.pallarium.eventmaker.gui.MenuListener;
import com.pallarium.eventmaker.model.GameEvent;
import com.pallarium.eventmaker.runtime.ActiveEvent;
import com.pallarium.eventmaker.runtime.EventEngine;
import com.pallarium.eventmaker.runtime.GameListener;
import com.pallarium.eventmaker.storage.EventStore;
import com.pallarium.eventmaker.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EventMakerPlugin extends JavaPlugin {
    private EventStore store;
    private EventEngine engine;
    private ChatInput chatInput;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new EventStore(this);
        engine = new EventEngine(this);
        chatInput = new ChatInput(this);
        store.load();
        engine.startScheduler();
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
        Bukkit.getPluginManager().registerEvents(chatInput, this);
        getLogger().info("EventMaker ready. /em opens the GUI.");
    }

    @Override
    public void onDisable() {
        if (engine != null) engine.shutdown();
        if (store != null) store.save();
    }

    public EventStore store() {
        return store;
    }

    public EventEngine engine() {
        return engine;
    }

    public ChatInput chatInput() {
        return chatInput;
    }

    public String prefix() {
        return Text.color(getConfig().getString("prefix", "&8[&dEvents&8] &7"));
    }

    public void broadcast(String msg) {
        Bukkit.broadcastMessage(prefix() + Text.color(msg));
    }

    /** Send a prefixed, color-translated message to one player. */
    public void msg(CommandSender to, String msg) {
        to.sendMessage(prefix() + Text.color(msg));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Console: /em list | start <name> | stop <name> | reload");
                return true;
            }
            new MainMenu(this, (Player) sender).open();
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list": {
                msg(sender, "&f" + store.all().size() + " event(s):");
                for (GameEvent e : store.all()) {
                    sender.sendMessage(Text.color(" " + (engine.isRunning(e) ? "&a● " : "&8○ ") + "&f" + e.name
                            + " &8(" + e.type.label + ", " + e.trigger.label + ")"));
                }
                return true;
            }
            case "start":
            case "stop": {
                if (args.length < 2) {
                    msg(sender, "&cUsage: /em " + sub + " <event name>");
                    return true;
                }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                GameEvent e = store.byName(name);
                if (e == null) {
                    msg(sender, "&cNo event called &f" + name);
                    return true;
                }
                boolean ok = sub.equals("start") ? engine.start(e) : engine.stop(e);
                msg(sender, (ok ? "&aDone." : "&7Nothing changed - already " + (sub.equals("start") ? "running." : "stopped.")));
                return true;
            }
            case "reload": {
                reloadConfig();
                for (ActiveEvent a : new java.util.ArrayList<>(engine.running())) a.stop(true);
                store.load();
                msg(sender, "&aReloaded.");
                return true;
            }
            default:
                msg(sender, "&7/em &8| &7/em list &8| &7/em start <name> &8| &7/em stop <name> &8| &7/em reload");
                return true;
        }
    }
}
