package com.pallarium.endgame.boss;

import com.pallarium.endgame.EndgamePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns every live boss fight. Spawning goes through here, damage routes
 * through here, and shutdown cleans every one of them up.
 */
public class BossService {

    private final EndgamePlugin plugin;
    private final Map<UUID, BossInstance> live = new ConcurrentHashMap<>();

    public BossService(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    /** Spawns a boss by id at a location. Returns null if the id is unknown. */
    public BossInstance spawn(String id, Location at) {
        BossDef def = Bosses.get(id);
        if (def == null || at.getWorld() == null) {
            return null;
        }
        org.bukkit.entity.Entity spawned = at.getWorld().spawnEntity(at, def.type());
        if (!(spawned instanceof LivingEntity body)) {
            spawned.remove();
            return null;
        }
        BossInstance inst = new BossInstance(plugin, def, body);
        live.put(inst.entity().getUniqueId(), inst);
        inst.begin();
        return inst;
    }

    public BossInstance of(Entity e) {
        if (e == null) {
            return null;
        }
        return live.get(e.getUniqueId());
    }

    public boolean isBoss(Entity e) {
        return e != null && live.containsKey(e.getUniqueId());
    }

    /** True if this entity is a minion belonging to any live fight. */
    public BossInstance ownerOfMinion(Entity e) {
        for (BossInstance inst : live.values()) {
            if (inst.ownsMinion(e)) {
                return inst;
            }
        }
        return null;
    }

    public void forget(BossInstance inst) {
        if (inst.entity() != null) {
            live.remove(inst.entity().getUniqueId());
        }
    }

    public List<BossInstance> active() {
        return new ArrayList<>(live.values());
    }

    /** Kills every running fight, used by /endgame boss purge and on disable. */
    public int purge() {
        int n = 0;
        for (BossInstance inst : new ArrayList<>(live.values())) {
            inst.cleanup(true);
            n++;
        }
        live.clear();
        return n;
    }

    public void shutdown() {
        for (BossInstance inst : new ArrayList<>(live.values())) {
            inst.cleanup(true);
        }
        live.clear();
    }

    /** Nearest live boss to a player, for the bossbar and the UI. */
    public BossInstance nearest(Player p, double within) {
        BossInstance best = null;
        double bestD = within * within;
        for (BossInstance inst : live.values()) {
            LivingEntity e = inst.entity();
            if (e == null || !e.isValid() || e.getWorld() != p.getWorld()) {
                continue;
            }
            double d = e.getLocation().distanceSquared(p.getLocation());
            if (d < bestD) {
                bestD = d;
                best = inst;
            }
        }
        return best;
    }
}
