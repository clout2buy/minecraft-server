package com.pallarium.questboard.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class DataStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public PlayerData get(UUID id) {
        return cache.computeIfAbsent(id, k -> new PlayerData());
    }

    public void load() {
        cache.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String key : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(key);
            if (s == null) continue;
            PlayerData d = new PlayerData();
            d.day = s.getString("day", "");
            d.questIds = new ArrayList<>(s.getStringList("quests"));
            d.completed = new HashSet<>(s.getStringList("completed"));
            d.dailyClaimed = s.getBoolean("dailyClaimed", false);
            d.rerollsUsed = s.getInt("rerollsUsed", 0);
            d.streak = s.getInt("streak", 0);
            d.lastFullDay = s.getString("lastFullDay", "");
            d.totalCompleted = s.getInt("totalCompleted", 0);
            ConfigurationSection p = s.getConfigurationSection("progress");
            if (p != null) for (String q : p.getKeys(false)) d.progress.put(q, p.getInt(q));
            try {
                cache.put(UUID.fromString(key), d);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> e : cache.entrySet()) {
            PlayerData d = e.getValue();
            String k = e.getKey().toString();
            y.set(k + ".day", d.day);
            y.set(k + ".quests", d.questIds);
            y.set(k + ".completed", new ArrayList<>(d.completed));
            y.set(k + ".dailyClaimed", d.dailyClaimed);
            y.set(k + ".rerollsUsed", d.rerollsUsed);
            y.set(k + ".streak", d.streak);
            y.set(k + ".lastFullDay", d.lastFullDay);
            y.set(k + ".totalCompleted", d.totalCompleted);
            for (Map.Entry<String, Integer> p : d.progress.entrySet()) y.set(k + ".progress." + p.getKey(), p.getValue());
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save players.yml: " + ex.getMessage());
        }
    }

    public void reset(UUID id) {
        cache.remove(id);
    }
}
