package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EventEngine {
    private final EventMakerPlugin plugin;
    private final Map<String, ActiveEvent> running = new HashMap<>();
    private BukkitTask scheduler;

    public EventEngine(EventMakerPlugin plugin) {
        this.plugin = plugin;
    }

    public void startScheduler() {
        long period = Math.max(5, plugin.getConfig().getInt("scheduler-check-seconds", 30)) * 20L;
        scheduler = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) return;
            long now = System.currentTimeMillis();
            for (GameEvent e : new ArrayList<>(plugin.store().all())) {
                if (!running.containsKey(e.id) && e.isDue(now)) start(e);
            }
        }, period, period);
    }

    public boolean isRunning(GameEvent e) {
        return running.containsKey(e.id);
    }

    public ActiveEvent get(GameEvent e) {
        return running.get(e.id);
    }

    public Collection<ActiveEvent> running() {
        return running.values();
    }

    public boolean start(GameEvent e) {
        if (running.containsKey(e.id)) return false;
        ActiveEvent active = e.type.create(plugin, e);
        running.put(e.id, active);
        e.lastRun = System.currentTimeMillis();
        plugin.store().save();
        active.start();
        return true;
    }

    public boolean stop(GameEvent e) {
        ActiveEvent active = running.get(e.id);
        if (active == null) return false;
        active.stop(true);
        return true;
    }

    void onFinished(ActiveEvent active) {
        running.remove(active.event().id);
    }

    public void shutdown() {
        if (scheduler != null) scheduler.cancel();
        for (ActiveEvent a : new ArrayList<>(running.values())) a.stop(true);
        running.clear();
    }
}
