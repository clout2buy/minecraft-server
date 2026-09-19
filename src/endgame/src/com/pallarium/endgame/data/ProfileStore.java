package com.pallarium.endgame.data;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.skill.Perk;
import com.pallarium.endgame.skill.Skill;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flat file persistence, one yml per player under /playerdata.
 * All disk IO happens off the main thread.
 */
public class ProfileStore {

    private final EndgamePlugin plugin;
    private final File folder;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    public ProfileStore(EndgamePlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create playerdata folder.");
        }
    }

    public PlayerProfile cached(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerProfile getOrCreate(UUID uuid, String name) {
        PlayerProfile existing = cache.get(uuid);
        if (existing != null) {
            existing.name(name);
            return existing;
        }
        PlayerProfile loaded = read(uuid, name);
        cache.put(uuid, loaded);
        return loaded;
    }

    public void unload(UUID uuid) {
        PlayerProfile profile = cache.remove(uuid);
        if (profile != null) {
            saveAsync(profile);
        }
    }

    private File fileFor(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }

    private PlayerProfile read(UUID uuid, String name) {
        PlayerProfile profile = new PlayerProfile(uuid, name);
        File file = fileFor(uuid);
        if (!file.exists()) {
            return profile;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (Skill s : Skill.values()) {
            String base = "skills." + s.key();
            profile.loadSkill(s,
                    yml.getInt(base + ".level", 1),
                    yml.getInt(base + ".xp", 0),
                    yml.getInt(base + ".points", 0));
        }
        for (Perk p : Perk.values()) {
            profile.loadPerk(p, yml.getInt("perks." + p.key(), 0));
        }
        profile.loadSettings(
                yml.getBoolean("settings.actionbar", true),
                yml.getBoolean("settings.levelsound", true),
                yml.getBoolean("settings.menuitem", true));
        profile.clean();
        return profile;
    }

    /** Serialises on the calling thread, writes on an async task. */
    public void saveAsync(PlayerProfile profile) {
        YamlConfiguration yml = snapshot(profile);
        File file = fileFor(profile.uuid());
        profile.clean();
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(yml, file));
        } else {
            write(yml, file);
        }
    }

    /** Blocking save, used on disable. */
    public void saveNow(PlayerProfile profile) {
        YamlConfiguration yml = snapshot(profile);
        profile.clean();
        write(yml, fileFor(profile.uuid()));
    }

    private YamlConfiguration snapshot(PlayerProfile profile) {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("name", profile.name());
        for (Skill s : Skill.values()) {
            String base = "skills." + s.key();
            yml.set(base + ".level", profile.level(s));
            yml.set(base + ".xp", profile.xp(s));
            yml.set(base + ".points", profile.points(s));
        }
        for (Perk p : Perk.values()) {
            yml.set("perks." + p.key(), profile.rank(p));
        }
        yml.set("settings.actionbar", profile.actionBarEnabled());
        yml.set("settings.levelsound", profile.levelSoundEnabled());
        yml.set("settings.menuitem", profile.menuItemEnabled());
        return yml;
    }

    private void write(YamlConfiguration yml, File file) {
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed saving " + file.getName() + ": " + e.getMessage());
        }
    }

    public void saveAllOnline() {
        for (PlayerProfile p : cache.values()) {
            if (p.dirty()) {
                saveAsync(p);
            }
        }
    }

    public void shutdown() {
        for (PlayerProfile p : cache.values()) {
            saveNow(p);
        }
        cache.clear();
    }

    /**
     * Reads every stored profile for the leaderboard. Call async only.
     */
    public List<Entry> topByPower(int limit) {
        List<Entry> all = new ArrayList<>();
        File[] files = folder.listFiles((dir, n) -> n.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String raw = f.getName().substring(0, f.getName().length() - 4);
                UUID id;
                try {
                    id = UUID.fromString(raw);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                PlayerProfile live = cache.get(id);
                if (live != null) {
                    all.add(new Entry(live.name(), live.power()));
                    continue;
                }
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
                int power = 0;
                for (Skill s : Skill.values()) {
                    power += Math.max(1, yml.getInt("skills." + s.key() + ".level", 1));
                }
                all.add(new Entry(yml.getString("name", "Unknown"), power));
            }
        }
        for (PlayerProfile live : cache.values()) {
            boolean present = false;
            for (Entry e : all) {
                if (e.name().equalsIgnoreCase(live.name())) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                all.add(new Entry(live.name(), live.power()));
            }
        }
        all.sort(Comparator.comparingInt(Entry::power).reversed());
        return all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
    }

    public record Entry(String name, int power) {
    }
}
