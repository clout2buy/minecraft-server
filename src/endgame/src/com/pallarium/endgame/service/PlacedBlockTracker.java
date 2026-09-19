package com.pallarium.endgame.service;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Remembers player placed blocks so place then break loops award nothing.
 * Bounded so the set cannot grow without limit on a busy server.
 */
public class PlacedBlockTracker {

    private static final int CAP = 60_000;

    private final Set<String> placed = Collections.synchronizedSet(new LinkedHashSet<>());

    private String key(Location loc) {
        return loc.getWorld().getUID() + ":" + loc.getBlockX() + ":"
                + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public void mark(Block block) {
        String k = key(block.getLocation());
        synchronized (placed) {
            placed.add(k);
            if (placed.size() > CAP) {
                var it = placed.iterator();
                for (int i = 0; i < 5000 && it.hasNext(); i++) {
                    it.next();
                    it.remove();
                }
            }
        }
    }

    /** Non destructive check. Use when another listener still needs the mark. */
    public boolean isPlaced(Block block) {
        return placed.contains(key(block.getLocation()));
    }

    /** Consumes the mark, returning true when this block was player placed. */
    public boolean consume(Block block) {
        return placed.remove(key(block.getLocation()));
    }

    public void clear() {
        placed.clear();
    }

    public int size() {
        return placed.size();
    }
}
