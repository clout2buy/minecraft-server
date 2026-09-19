package com.pallarium.eventmaker.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameEvent {
    public final String id;
    public String name;
    public EventType type = EventType.METEOR_SHOWER;
    public TriggerType trigger = TriggerType.MANUAL;
    public int intervalMinutes = 60;
    public int durationSeconds = 180;
    public Location location;
    public int radius = 30;
    public List<ItemStack> rewards = new ArrayList<>();
    public boolean announce = true;
    public boolean enabled = true;
    public long lastRun = 0;

    public GameEvent(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static GameEvent fresh(String name) {
        return new GameEvent(UUID.randomUUID().toString().substring(0, 8), name);
    }

    public Location center() {
        if (location != null && location.getWorld() != null) return location.clone();
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (!online.isEmpty()) return online.get(0).getLocation();
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public List<Player> participants() {
        List<Player> out = new ArrayList<>();
        if (location == null || location.getWorld() == null) {
            out.addAll(Bukkit.getOnlinePlayers());
            return out;
        }
        double reach = Math.max(radius, 50) * 3.0;
        for (Player p : location.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(location) <= reach * reach) out.add(p);
        }
        return out;
    }

    public boolean isDue(long now) {
        if (!enabled || trigger != TriggerType.INTERVAL || intervalMinutes <= 0) return false;
        return now - lastRun >= intervalMinutes * 60_000L;
    }
}
