package gg.mistique.rookbot;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.block.Action;

public final class BotListener implements Listener {
    private final RookBotPlugin plugin;
    BotListener(RookBotPlugin plugin) { this.plugin = plugin; }

    private boolean isBot(org.bukkit.entity.Entity e) {
        for (Bot b : plugin.bots().values()) if (b.alive() && b.body().equals(e)) return true;
        return false;
    }

    // mobs ignore the bot, and nothing hurts it
    @EventHandler public void onTarget(EntityTargetEvent e) { if (e.getTarget() != null && isBot(e.getTarget())) e.setCancelled(true); }
    @EventHandler public void onDamage(EntityDamageEvent e) { if (isBot(e.getEntity())) e.setCancelled(true); }

    // possession: spectator movement drives the body
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Bot b = plugin.bot(e.getPlayer());
        if (b == null || b.mode() != Bot.Mode.POSSESSED) return;
        b.driveFrom(e.getPlayer(), e.getFrom(), e.getTo());
        e.setCancelled(true); // camera stays on the body; driveFrom teleports it
    }
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Bot b = plugin.bot(e.getPlayer());
        if (b == null || b.mode() != Bot.Mode.POSSESSED) return;
        if (e.getClickedBlock() == null) return;
        if (e.getAction() == Action.LEFT_CLICK_BLOCK) b.mineAs(e.getPlayer(), e.getClickedBlock());
        else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) b.placeAs(e.getPlayer(), e.getClickedBlock(), e.getBlockFace());
        e.setCancelled(true);
    }
    // shift = quick exit from possession (spectators use shift to dismount anyway)
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Bot b = plugin.bot(e.getPlayer());
        if (b != null && b.mode() == Bot.Mode.POSSESSED && e.isSneaking()) b.unpossess();
    }
    @EventHandler public void onQuit(PlayerQuitEvent e) { Bot b = plugin.bot(e.getPlayer()); if (b != null) b.stop(); }
}
