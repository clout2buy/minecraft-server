package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import com.pallarium.eventmaker.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** One random player gets a bounty. Kill them to claim the rewards; survive and they keep them. */
public class Bounty extends ActiveEvent {
    private UUID target;
    private boolean claimed = false;

    public Bounty(EventMakerPlugin plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    protected void onStart() {
        List<Player> players = event.participants();
        if (players.isEmpty()) return;
        Player t = players.get(random.nextInt(players.size()));
        target = t.getUniqueId();
        t.setGlowing(true);
        plugin.broadcast("&c&lBOUNTY &7placed on &f" + t.getName() + "&7! Kill them to claim the rewards.");
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.6f);
        t.sendTitle(Text.color("&c&lYOU ARE WANTED"), Text.color("&7Survive to keep the rewards"), 10, 60, 20);
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        Player t = target == null ? null : Bukkit.getPlayer(target);
        if (t == null) return;
        if (elapsed % 30 == 0) {
            plugin.broadcast("&c" + t.getName() + " &7is at &f" + t.getLocation().getBlockX() + ", " + t.getLocation().getBlockZ());
        }
    }

    public boolean isTarget(Player p) {
        return target != null && p.getUniqueId().equals(target) && !claimed;
    }

    public void onTargetKilled(Player killer) {
        claimed = true;
        plugin.broadcast("&a" + killer.getName() + " &7claimed the bounty!");
        if (!event.rewards.isEmpty()) giveRewards(killer);
        stop(false);
    }

    @Override
    protected void onEnd(boolean forced) {
        Player t = target == null ? null : Bukkit.getPlayer(target);
        if (t != null) t.setGlowing(false);
        if (!forced && !claimed && t != null) {
            plugin.broadcast("&a" + t.getName() + " &7survived the bounty!");
            if (!event.rewards.isEmpty()) giveRewards(t);
        }
    }

    @Override
    protected String scoreLabel() {
        return "";
    }
}
