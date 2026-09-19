package com.pallarium.pluginmarket;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInput implements Listener {
    private final PluginMarketPlugin plugin;
    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    public ChatInput(PluginMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void ask(Player p, String prompt, Consumer<String> onAnswer) {
        pending.put(p.getUniqueId(), onAnswer);
        p.closeInventory();
        p.sendMessage(net.md_5.bungee.api.ChatColor.AQUA + "[PluginMarket] " + net.md_5.bungee.api.ChatColor.WHITE + prompt
                + net.md_5.bungee.api.ChatColor.GRAY + " (type 'cancel' to abort)");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Consumer<String> cb = pending.remove(e.getPlayer().getUniqueId());
        if (cb == null) return;
        e.setCancelled(true);
        String msg = e.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            e.getPlayer().sendMessage(net.md_5.bungee.api.ChatColor.RED + "Cancelled.");
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> cb.accept(msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
    }
}
