package com.pallarium.eventmaker.gui;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.util.Text;
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
    private final EventMakerPlugin plugin;
    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    public ChatInput(EventMakerPlugin plugin) {
        this.plugin = plugin;
    }

    public void ask(Player p, String prompt, Consumer<String> onAnswer) {
        pending.put(p.getUniqueId(), onAnswer);
        p.sendMessage(plugin.prefix() + Text.color(prompt));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Consumer<String> cb = pending.remove(e.getPlayer().getUniqueId());
        if (cb == null) return;
        e.setCancelled(true);
        String msg = e.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            plugin.msg(e.getPlayer(), "&7Cancelled.");
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> cb.accept(msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
    }
}
