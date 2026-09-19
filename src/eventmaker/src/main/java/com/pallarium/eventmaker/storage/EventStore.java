package com.pallarium.eventmaker.storage;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.EventType;
import com.pallarium.eventmaker.model.GameEvent;
import com.pallarium.eventmaker.model.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EventStore {
    private final EventMakerPlugin plugin;
    private final File file;
    private final Map<String, GameEvent> events = new LinkedHashMap<>();

    public EventStore(EventMakerPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "events.yml");
    }

    public Collection<GameEvent> all() {
        return events.values();
    }

    public GameEvent get(String id) {
        return events.get(id);
    }

    public GameEvent byName(String name) {
        for (GameEvent e : events.values()) {
            if (e.name.equalsIgnoreCase(name)) return e;
        }
        return null;
    }

    public void put(GameEvent e) {
        events.put(e.id, e);
    }

    public void remove(String id) {
        events.remove(id);
    }

    public void load() {
        events.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("events");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            GameEvent e = new GameEvent(id, s.getString("name", "Event"));
            e.type = parse(EventType.class, s.getString("type"), EventType.METEOR_SHOWER);
            e.trigger = parse(TriggerType.class, s.getString("trigger"), TriggerType.MANUAL);
            e.intervalMinutes = s.getInt("interval", 60);
            e.durationSeconds = s.getInt("duration", 180);
            e.radius = s.getInt("radius", 30);
            e.announce = s.getBoolean("announce", true);
            e.enabled = s.getBoolean("enabled", true);
            e.lastRun = s.getLong("lastRun", 0);
            ConfigurationSection loc = s.getConfigurationSection("location");
            if (loc != null) {
                World w = Bukkit.getWorld(loc.getString("world", ""));
                if (w != null) {
                    e.location = new Location(w, loc.getDouble("x"), loc.getDouble("y"), loc.getDouble("z"));
                }
            }
            List<?> raw = s.getList("rewards");
            if (raw != null) {
                for (Object o : raw) if (o instanceof ItemStack) e.rewards.add((ItemStack) o);
            }
            events.put(id, e);
        }
        plugin.getLogger().info("Loaded " + events.size() + " event(s).");
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (GameEvent e : events.values()) {
            ConfigurationSection s = yml.createSection("events." + e.id);
            s.set("name", e.name);
            s.set("type", e.type.name());
            s.set("trigger", e.trigger.name());
            s.set("interval", e.intervalMinutes);
            s.set("duration", e.durationSeconds);
            s.set("radius", e.radius);
            s.set("announce", e.announce);
            s.set("enabled", e.enabled);
            s.set("lastRun", e.lastRun);
            if (e.location != null && e.location.getWorld() != null) {
                s.set("location.world", e.location.getWorld().getName());
                s.set("location.x", e.location.getX());
                s.set("location.y", e.location.getY());
                s.set("location.z", e.location.getZ());
            }
            s.set("rewards", new ArrayList<>(e.rewards));
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save events.yml: " + ex.getMessage());
        }
    }

    private static <T extends Enum<T>> T parse(Class<T> cls, String raw, T def) {
        if (raw == null) return def;
        try {
            return Enum.valueOf(cls, raw);
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }
}
