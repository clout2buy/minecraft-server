package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import com.pallarium.eventmaker.util.Text;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class KingOfTheHill extends ActiveEvent {
    private Location hill;
    private int zone;

    public KingOfTheHill(EventMakerPlugin plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    protected void onStart() {
        hill = event.center();
        zone = Math.max(3, Math.min(event.radius, 12));
        plugin.broadcast("&6Hold the hill at &f" + hill.getBlockX() + ", " + hill.getBlockY() + ", " + hill.getBlockZ() + "&6! Zone radius: " + zone);
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        for (int i = 0; i < 24; i++) {
            double a = i / 24.0 * Math.PI * 2;
            hill.getWorld().spawnParticle(Particle.FLAME,
                    hill.clone().add(Math.cos(a) * zone, 0.3, Math.sin(a) * zone), 1, 0, 0, 0, 0);
        }
        Player king = null;
        int inside = 0;
        for (Player p : hill.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(hill) <= zone * zone) {
                inside++;
                king = p;
            }
        }
        if (inside == 1) {
            addScore(king, 1);
            king.sendActionBar(Text.color("&6&lYou hold the hill! &e" + formatScore(scores.get(king.getUniqueId())) + "s"));
        } else if (inside > 1) {
            for (Player p : hill.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(hill) <= zone * zone) {
                    p.sendActionBar(Text.color("&cContested! &7" + inside + " players on the hill"));
                }
            }
        }
        if (elapsed % 30 == 0 && !scores.isEmpty()) {
            Map.Entry<UUID, Double> top = leaderboard().get(0);
            Player p = plugin.getServer().getPlayer(top.getKey());
            if (p != null) plugin.broadcast("&6Current king: &f" + p.getName() + " &7(" + formatScore(top.getValue()) + "s)");
        }
    }

    @Override
    protected void onEnd(boolean forced) {
        if (!forced) announceWinnersAndReward();
    }

    @Override
    protected String scoreLabel() {
        return "seconds held";
    }
}
