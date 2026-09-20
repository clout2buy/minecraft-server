package com.pallarium.logviewer;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class LogViewerPlugin extends JavaPlugin implements Listener {
    private LogReader reader;
    private LogMenu menu;
    private ChatInput chatInput;

    @Override
    public void onEnable() {
        reader = new LogReader(getDataFolder().getParentFile().getParentFile());
        menu = new LogMenu(this);
        chatInput = new ChatInput(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(chatInput, this);
        getLogger().info("LogViewer ready. /logs opens the viewer.");
    }

    public LogReader reader() { return reader; }
    public ChatInput chatInput() { return chatInput; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (LogMenu.owns(e)) menu.handleClick(e);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().contains("Logs: ")) e.setCancelled(true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only - the console already has the logs.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("logviewer.use")) {
            p.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        LogMenu.State st = menu.state(p);
        if (args.length > 0) {
            st.query = String.join(" ", args);
            st.filter = LogFilter.ALL;
            st.page = 0;
        }
        menu.open(p);
        return true;
    }
}
