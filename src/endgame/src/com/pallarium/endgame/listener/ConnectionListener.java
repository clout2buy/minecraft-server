package com.pallarium.endgame.listener;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.data.PlayerProfile;
import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {

    private final EndgamePlugin plugin;

    public ConnectionListener(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.store().getOrCreate(player.getUniqueId(), player.getName());

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            plugin.menuItem().sync(player, profile);

            int unspent = 0;
            for (var s : com.pallarium.endgame.skill.Skill.values()) {
                unspent += profile.points(s);
            }
            player.sendMessage(Component.text("  ENDGAME  ", Icon.NEON)
                    .decoration(TextDecoration.BOLD, true)
                    .append(Component.text("Power " + profile.power(), Icon.TEXT)
                            .decoration(TextDecoration.BOLD, false))
                    .decoration(TextDecoration.ITALIC, false));
            if (unspent > 0) {
                player.sendMessage(Component.text("  You have ", Icon.TEXT)
                        .append(Component.text(unspent + " unspent skill point"
                                + (unspent == 1 ? "" : "s"), Icon.GOOD))
                        .append(Component.text(". Open the menu to spend them.", Icon.TEXT))
                        .decoration(TextDecoration.ITALIC, false));
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.store().unload(event.getPlayer().getUniqueId());
    }
}
