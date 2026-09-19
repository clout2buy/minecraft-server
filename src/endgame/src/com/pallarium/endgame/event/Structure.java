package com.pallarium.endgame.event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds the small set pieces an event stands on, and remembers every block it
 * overwrote so the world can be put back exactly as it was. Nothing here is
 * permanent: restore() replays the snapshot in reverse.
 */
public class Structure {

    /** One overwritten block, kept so we can undo it. */
    private record Snap(BlockState state) {
    }

    private final Deque<Snap> history = new ArrayDeque<>();

    /**
     * Not every event icon is placeable: ender eyes, honeycomb, prismarine
     * shards and swords are items only, and setType on those throws. Anything
     * that cannot be a block falls back to a lantern so the site still reads.
     */
    public static Material placeable(Material material, Material fallback) {
        return material != null && material.isBlock() ? material : fallback;
    }

    /** Sets a block and records what used to be there. */
    private void set(Block block, Material material) {
        if (!material.isBlock()) {
            return;
        }
        if (block.getType() == material) {
            return;
        }
        history.push(new Snap(block.getState()));
        block.setType(material, false);
    }

    /** Puts every block this structure touched back the way it was. */
    public void restore() {
        while (!history.isEmpty()) {
            BlockState state = history.pop().state();
            state.update(true, false);
        }
    }

    public int blockCount() {
        return history.size();
    }

    /* ------------------------------------------------------------------ */
    /*  set pieces                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * A stepped altar with a lit core. This is the anchor for DELIVER and
     * PROTECT stages and reads as an obvious "stand here" from a distance.
     */
    public Location altar(Location center, Material accent) {
        World w = center.getWorld();
        if (w == null) {
            return center;
        }
        Location base = ground(center);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 && Math.abs(z) == 2) {
                    continue;
                }
                set(base.clone().add(x, -1, z).getBlock(), Material.POLISHED_BLACKSTONE);
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(base.clone().add(x, 0, z).getBlock(), Material.POLISHED_BLACKSTONE_BRICKS);
            }
        }
        set(base.clone().add(0, 1, 0).getBlock(),
                placeable(accent, Material.SOUL_LANTERN));
        for (int[] c : new int[][]{{2, 2}, {2, -2}, {-2, 2}, {-2, -2}}) {
            set(base.clone().add(c[0], 0, c[1]).getBlock(), Material.POLISHED_BLACKSTONE_WALL);
            set(base.clone().add(c[0], 1, c[1]).getBlock(), Material.SOUL_LANTERN);
        }
        return base.clone().add(0, 2, 0);
    }

    /**
     * A ring of unlit braziers for a RITUAL stage. Returns their tops so the
     * service can track which ones are still cold.
     */
    public List<Block> braziers(Location center, int count, double radius) {
        World w = center.getWorld();
        if (w == null) {
            return List.of();
        }
        java.util.List<Block> out = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2 / count) * i;
            Location at = ground(center.clone().add(
                    Math.cos(a) * radius, 0, Math.sin(a) * radius));
            set(at.clone().add(0, -1, 0).getBlock(), Material.POLISHED_BLACKSTONE);
            Block top = at.getBlock();
            set(top, Material.CAULDRON);
            out.add(top);
        }
        return out;
    }

    /**
     * A crashed meteor: a scorched crater with a molten core buried in it.
     * The crust blocks are returned as the BREAK targets.
     */
    public List<Block> crater(Location center, int shards) {
        World w = center.getWorld();
        if (w == null) {
            return List.of();
        }
        Location base = ground(center);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > 4.2) {
                    continue;
                }
                Block b = base.clone().add(x, -1, z).getBlock();
                set(b, d < 1.6 ? Material.MAGMA_BLOCK
                        : d < 3 ? Material.BLACKSTONE : Material.BASALT);
                if (d < 3.4 && d > 1.2) {
                    set(base.clone().add(x, 0, z).getBlock(), Material.AIR);
                }
            }
        }
        java.util.List<Block> out = new java.util.ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < shards; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double d = 1 + rng.nextDouble() * 2.5;
            Block b = base.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d).getBlock();
            set(b, Material.SHROOMLIGHT);
            out.add(b);
        }
        return out;
    }

    /**
     * A wrecked caravan: broken carts, scattered crates, a dead campfire.
     * Pure scenery, it makes the CARAVAN event look like a place.
     */
    public void caravanWreck(Location center) {
        World w = center.getWorld();
        if (w == null) {
            return;
        }
        Location base = ground(center);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        set(base.clone().add(0, 0, 0).getBlock(), Material.CAMPFIRE);
        for (int i = 0; i < 6; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double d = 2 + rng.nextDouble() * 4;
            Location at = ground(base.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            Material m = switch (rng.nextInt(4)) {
                case 0 -> Material.BARREL;
                case 1 -> Material.HAY_BLOCK;
                case 2 -> Material.OAK_FENCE;
                default -> Material.OAK_SLAB;
            };
            set(at.getBlock(), m);
        }
        for (int i = 0; i < 3; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            Location at = ground(base.clone().add(Math.cos(a) * 5, 0, Math.sin(a) * 5));
            set(at.getBlock(), Material.OAK_FENCE);
            set(at.clone().add(0, 1, 0).getBlock(), Material.LANTERN);
        }
    }

    /**
     * A spire of bone and soul fire. Used as the PROTECT core and as the goal
     * beacon for an escort, tall enough to see over trees.
     */
    public Block spire(Location center, Material core, int height) {
        World w = center.getWorld();
        if (w == null) {
            return center.getBlock();
        }
        Location base = ground(center);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(base.clone().add(x, -1, z).getBlock(), Material.POLISHED_BASALT);
            }
        }
        Material top = placeable(core, Material.SOUL_LANTERN);
        for (int y = 0; y < height; y++) {
            set(base.clone().add(0, y, 0).getBlock(),
                    y == height - 1 ? top : Material.BONE_BLOCK);
        }
        set(base.clone().add(0, height, 0).getBlock(), Material.SOUL_FIRE);
        return base.clone().add(0, height - 1, 0).getBlock();
    }

    /** A cage of iron bars for a captive, broken open by killing the keeper. */
    public void cage(Location center) {
        Location base = ground(center);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(base.clone().add(x, -1, z).getBlock(), Material.POLISHED_BLACKSTONE);
                if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                    set(base.clone().add(x, 0, z).getBlock(), Material.IRON_BARS);
                    set(base.clone().add(x, 1, z).getBlock(), Material.IRON_BARS);
                }
            }
        }
        set(base.clone().add(0, 2, 0).getBlock(), Material.SOUL_LANTERN);
    }

    /** Rips the cage open so the captive can walk out. */
    public void openCage(Location center) {
        Location base = ground(center);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block b = base.clone().add(x, 0, z).getBlock();
                if (b.getType() == Material.IRON_BARS) {
                    b.setType(Material.AIR, false);
                }
                Block up = base.clone().add(x, 1, z).getBlock();
                if (up.getType() == Material.IRON_BARS) {
                    up.setType(Material.AIR, false);
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */

    /** Drops a location down to the first solid surface under it. */
    private Location ground(Location loc) {
        World w = loc.getWorld();
        if (w == null) {
            return loc;
        }
        Location l = loc.clone();
        int y = l.getBlockY();
        // underground sites keep their own level, surface sites snap to terrain
        if (y > 55) {
            int top = w.getHighestBlockYAt(l);
            if (Math.abs(top - y) < 14) {
                l.setY(top + 1);
            }
        }
        return l;
    }
}
