package gg.mistique.rookbot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * One bot per owner. The body is a Zombie (player-shaped, works on every Via client, no NMS)
 * with awareness off so it never targets; Paper's Mob.getPathfinder() drives real walking.
 * Mining, building and pickups run from the tick loop: walk to the block, swing with crack
 * stages, break, collect into the bag.
 */
public final class Bot {
    public enum Mode { IDLE, FOLLOW, STAY, COME, WOOD, STONE, HOUSE, POSSESSED }

    private static final double SPEED = 0.28;           // possess-mode manual drive step
    private static final double WALK_SPEED = 1.15;      // navigator speed multiplier (zombie base 0.23 -> ~player walk)
    private static final int   REACH = 4;
    private static final int   SEARCH = 24;
    private static final int   BREAK_TICKS_WOOD = 4;    // in 5-tick steps
    private static final int   BREAK_TICKS_STONE = 6;

    final RookBotPlugin plugin;
    final UUID ownerId;
    final String name;
    private Zombie body;
    private final Inventory inv;
    private Mode mode = Mode.IDLE;
    private int wanted = 0, gathered = 0;
    private Block target; private int breakProgress;
    private Location home;
    private Deque<Location> houseQueue;
    private GameMode ownerPrevMode; private Location ownerPrevLoc;

    Bot(RookBotPlugin plugin, Player owner) {
        this.plugin = plugin; this.ownerId = owner.getUniqueId();
        this.name = "Rook";
        this.inv = Bukkit.createInventory(null, 27, Component.text("Rook's bag", NamedTextColor.GOLD));
        Location at = owner.getLocation().add(owner.getLocation().getDirection().setY(0).normalize().multiply(2));
        at.setY(owner.getLocation().getY());
        body = owner.getWorld().spawn(at, Zombie.class, z -> {
            // AI stays ON so the vanilla navigator animates legs + handles slopes/jumps; goals are
            // stripped so it never chases/attacks. setAware(false) blocks target selection entirely.
            z.setAware(false); z.setSilent(true); z.setBaby(false); z.setShouldBurnInDay(false);
            z.setRemoveWhenFarAway(false); z.setPersistent(true); z.setCanPickupItems(false);
            z.customName(Component.text(name, NamedTextColor.AQUA)); z.setCustomNameVisible(true);
            z.setInvulnerable(true); z.setCollidable(false); z.setGravity(true);
            z.getEquipment().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
        });
        body.getEquipment().setHelmet(null);
        home = at.clone();
        mode = Mode.FOLLOW;
        say(owner, "here. /rook wood|stone|house|follow|stay|inv|give|possess");
    }

    // ---------- public control ----------
    public Mode mode() { return mode; }
    public Inventory inventory() { return inv; }
    public Zombie body() { return body; }
    public boolean alive() { return body != null && body.isValid(); }
    public Player owner() { return Bukkit.getPlayer(ownerId); }

    public void follow() { stopTask(); mode = Mode.FOLLOW; }
    public void stay()   { stopTask(); mode = Mode.STAY; home = body.getLocation(); }
    public void come()   { stopTask(); mode = Mode.COME; }
    public void gather(Mode what, int count) { stopTask(); mode = what; wanted = count; gathered = 0; }
    public void stop()   { stopTask(); mode = Mode.STAY; home = body.getLocation(); }

    public void remove() { unpossess(); if (body != null) body.remove(); body = null; }

    public String status() {
        String t = switch (mode) {
            case WOOD, STONE -> mode.name().toLowerCase() + " " + gathered + "/" + wanted;
            case HOUSE -> "building house, " + (houseQueue == null ? 0 : houseQueue.size()) + " blocks left";
            default -> mode.name().toLowerCase();
        };
        int items = 0; for (ItemStack s : inv.getContents()) if (s != null) items += s.getAmount();
        return t + " | bag: " + items + " items | at " + fmt(body.getLocation());
    }

    /** Hand everything in the bag to the owner (drops what doesn't fit). */
    public int giveAll(Player p) {
        int n = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i); if (s == null) continue;
            n += s.getAmount();
            for (ItemStack rest : p.getInventory().addItem(s).values()) p.getWorld().dropItemNaturally(p.getLocation(), rest);
            inv.setItem(i, null);
        }
        return n;
    }

    // ---------- possession: owner rides in the bot's eyes ----------
    public void possess(Player p) {
        if (mode == Mode.POSSESSED) return;
        stopTask(); mode = Mode.POSSESSED;
        ownerPrevMode = p.getGameMode(); ownerPrevLoc = p.getLocation();
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(body.getLocation()); p.setSpectatorTarget(body);
        say(p, "you're in. WASD moves me, left-click a block to mine it, right-click to place. /rook stop to leave.");
    }
    public void unpossess() {
        Player p = owner();
        if (mode != Mode.POSSESSED || p == null) { if (mode == Mode.POSSESSED) mode = Mode.STAY; return; }
        p.setSpectatorTarget(null);
        p.setGameMode(ownerPrevMode == null ? GameMode.SURVIVAL : ownerPrevMode);
        if (ownerPrevLoc != null) p.teleport(ownerPrevLoc);
        mode = Mode.STAY; home = body.getLocation();
        say(p, "you're out.");
    }
    /** Called from the listener when the possessing player moves: drive the body. */
    void driveFrom(Player p, Location from, Location to) {
        if (mode != Mode.POSSESSED) return;
        Vector d = to.toVector().subtract(from.toVector()); d.setY(0);
        if (d.lengthSquared() < 1e-6) { body.setRotation(to.getYaw(), to.getPitch()); return; }
        body.getPathfinder().stopPathfinding();
        Vector v = d.normalize().multiply(SPEED); v.setY(body.getVelocity().getY());
        if (body.isOnGround() && body.getLocation().add(v.clone().setY(0)).getBlock().getType().isSolid()) { body.setJumping(true); v.setY(0.42); }
        body.setVelocity(v); body.setRotation(to.getYaw(), to.getPitch());
        p.teleport(body.getLocation().add(0, 0, 0)); // keep camera glued
        p.setSpectatorTarget(body);
    }
    void mineAs(Player p, Block b) {
        if (mode != Mode.POSSESSED) return;
        if (b.getLocation().distanceSquared(body.getEyeLocation()) > REACH * REACH * 1.5) return;
        collect(b);
    }
    void placeAs(Player p, Block against, org.bukkit.block.BlockFace face) {
        if (mode != Mode.POSSESSED) return;
        ItemStack first = null; for (ItemStack s : inv.getContents()) if (s != null && s.getType().isBlock()) { first = s; break; }
        if (first == null) { say(p, "nothing placeable in the bag."); return; }
        Block dst = against.getRelative(face);
        if (!dst.getType().isAir()) return;
        dst.setType(first.getType()); first.setAmount(first.getAmount() - 1);
        body.swingMainHand();
    }

    // ---------- tick ----------
    void tick() {
        if (!alive()) return;
        Player o = owner();
        switch (mode) {
            case FOLLOW -> { if (o != null && o.getWorld().equals(body.getWorld())) walkNear(o.getLocation(), 2.5); }
            case COME   -> { if (o != null && walkNear(o.getLocation(), 2.0)) { mode = Mode.STAY; home = body.getLocation(); } }
            case STAY, IDLE -> {}
            case WOOD   -> gatherTick(o, Tag.LOGS::isTagged, BREAK_TICKS_WOOD);
            case STONE  -> gatherTick(o, m -> m == Material.STONE || m == Material.COBBLESTONE || m == Material.DEEPSLATE || m == Material.ANDESITE || m == Material.GRANITE || m == Material.DIORITE, BREAK_TICKS_STONE);
            case HOUSE  -> houseTick(o);
            case POSSESSED -> {}
        }
    }

    private void gatherTick(Player o, java.util.function.Predicate<Material> want, int breakSteps) {
        if (gathered >= wanted) { say(o, "done - " + gathered + " " + (mode == Mode.WOOD ? "logs" : "stone") + ". /rook give to take them."); mode = Mode.FOLLOW; return; }
        if (target == null || !want.test(target.getType())) {
            target = findBlock(want); breakProgress = 0;
            if (target == null) { say(o, "can't find any more " + (mode == Mode.WOOD ? "trees" : "stone") + " within " + SEARCH + " blocks."); mode = Mode.FOLLOW; return; }
        }
        Location stand = target.getLocation().add(0.5, 0, 0.5);
        if (body.getEyeLocation().distanceSquared(stand) > REACH * REACH) { walkNear(stand, REACH - 1); return; }
        body.getPathfinder().stopPathfinding();
        faceAt(stand);
        body.swingMainHand();
        ++breakProgress;
        // real chop feel: crack stages on the block for everyone nearby, hit sound each swing
        int stage = Math.min(9, (int) (9.0 * breakProgress / breakSteps));
        for (Player near : target.getWorld().getPlayers()) if (near.getLocation().distanceSquared(stand) < 64 * 64) near.sendBlockDamage(target.getLocation(), stage / 9f, body.getEntityId());
        target.getWorld().playSound(target.getLocation(), target.getBlockData().getSoundGroup().getHitSound(), 0.6f, 0.9f);
        if (breakProgress >= breakSteps) {
            for (Player near : target.getWorld().getPlayers()) near.sendBlockDamage(target.getLocation(), 0f, body.getEntityId());
            collect(target); gathered++; target = null; // next findBlock() picks the log above (nearest)
        }
    }

    private Block findBlock(java.util.function.Predicate<Material> want) {
        Location c = body.getLocation(); World w = c.getWorld();
        Block best = null; double bd = Double.MAX_VALUE;
        int cx = c.getBlockX(), cy = c.getBlockY(), cz = c.getBlockZ();
        for (int r = 1; r <= SEARCH; r += 2) {
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) for (int y = -6; y <= 12; y++) {
                Block b = w.getBlockAt(cx + x, cy + y, cz + z);
                if (!want.test(b.getType())) continue;
                // must have a free neighbour so it's actually reachable from the surface
                boolean open = false; for (org.bukkit.block.BlockFace f : new org.bukkit.block.BlockFace[]{org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH, org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST, org.bukkit.block.BlockFace.UP}) if (b.getRelative(f).getType().isAir()) { open = true; break; }
                if (!open) continue;
                double d = b.getLocation().distanceSquared(c);
                if (d < bd) { bd = d; best = b; }
            }
            if (best != null) return best;
        }
        return null;
    }

    /** Break a block as a player would and put the drops in the bag. */
    private void collect(Block b) {
        Collection<ItemStack> drops = b.getDrops(new ItemStack(Material.DIAMOND_PICKAXE));
        if (Tag.LOGS.isTagged(b.getType())) drops = b.getDrops(new ItemStack(Material.DIAMOND_AXE));
        b.getWorld().playSound(b.getLocation(), b.getBlockData().getSoundGroup().getBreakSound(), 1f, 1f);
        b.getWorld().spawnParticle(Particle.BLOCK_CRACK, b.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, b.getBlockData());
        b.setType(Material.AIR);
        for (ItemStack s : drops) for (ItemStack rest : inv.addItem(s).values()) b.getWorld().dropItemNaturally(b.getLocation(), rest);
    }

    // ---------- house ----------
    public void buildHouse(Player o) {
        stopTask();
        Material wall = pick(m -> m == Material.COBBLESTONE || m == Material.STONE || m == Material.DEEPSLATE || Tag.PLANKS.isTagged(m) || Tag.LOGS.isTagged(m));
        if (wall == null) { say(o, "bag's empty. /rook wood 32 or /rook stone 48 first."); mode = Mode.FOLLOW; return; }
        Location base = o.getLocation().getBlock().getLocation();
        base = base.add(o.getLocation().getDirection().setY(0).normalize().multiply(4)).getBlock().getLocation();
        // find ground
        while (base.getBlock().getType().isAir() && base.getY() > 0) base.subtract(0, 1, 0);
        base.add(0, 1, 0);
        houseQueue = new ArrayDeque<>();
        int W = 5, D = 5, H = 3;
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            boolean edge = x == 0 || z == 0 || x == W - 1 || z == D - 1;
            if (!edge) continue;
            if (y == 0 && x == 2 && z == 0) continue;           // door gap bottom
            if (y == 1 && x == 2 && z == 0) continue;           // door gap top
            if (y == 1 && (x == 0 || x == W - 1) && z == 2) continue; // windows
            houseQueue.add(base.clone().add(x, y, z));
        }
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) houseQueue.add(base.clone().add(x, H, z)); // roof
        mode = Mode.HOUSE;
        say(o, "building a " + W + "x" + D + " " + wall.name().toLowerCase().replace('_', ' ') + " house, " + houseQueue.size() + " blocks.");
    }
    private void houseTick(Player o) {
        if (houseQueue == null || houseQueue.isEmpty()) { say(o, "house done."); houseQueue = null; mode = Mode.FOLLOW; return; }
        Location next = houseQueue.peek();
        Location stand = next.clone().add(0.5, 0, 0.5);
        if (body.getEyeLocation().distanceSquared(stand) > REACH * REACH) { walkNear(stand, REACH - 1); return; }
        Material m = pick(mat -> mat == Material.COBBLESTONE || mat == Material.STONE || mat == Material.DEEPSLATE || Tag.PLANKS.isTagged(mat) || Tag.LOGS.isTagged(mat));
        if (m == null) { say(o, "ran out of blocks with " + houseQueue.size() + " to go. refill me and /rook house again."); houseQueue = null; mode = Mode.FOLLOW; return; }
        Block b = next.getBlock();
        if (b.getType().isAir() || !b.getType().isSolid()) { b.setType(m); take(m, 1); faceAt(stand); body.swingMainHand(); b.getWorld().playSound(b.getLocation(), b.getBlockData().getSoundGroup().getPlaceSound(), 1f, 1f); }
        houseQueue.poll();
    }
    private Material pick(java.util.function.Predicate<Material> ok) {
        for (ItemStack s : inv.getContents()) if (s != null && ok.test(s.getType())) return s.getType();
        return null;
    }
    private void take(Material m, int n) {
        for (ItemStack s : inv.getContents()) if (s != null && s.getType() == m) { int k = Math.min(n, s.getAmount()); s.setAmount(s.getAmount() - k); n -= k; if (n == 0) return; }
    }

    // ---------- movement (simple steering + step-up; good enough for surface work) ----------
    /** @return true when within dist */
    private boolean walkNear(Location dst, double dist) {
        Location cur = body.getLocation();
        if (!cur.getWorld().equals(dst.getWorld())) { body.teleport(dst); return true; }
        double d = cur.distance(dst);
        if (d <= dist) { body.getPathfinder().stopPathfinding(); return true; }
        if (d > 48) { body.teleport(dst.clone().add(0, 1, 0)); return false; } // fell behind - catch up
        // vanilla navigator: real walking animation, jumps, slopes, doors. Re-issue only when the
        // goal moved so we don't thrash the path every tick.
        var pf = body.getPathfinder();
        var res = pf.getCurrentPath();
        if (res == null || res.getFinalPoint() == null || res.getFinalPoint().distanceSquared(dst) > 1.5) pf.moveTo(dst, WALK_SPEED);
        return false;
    }
    private void faceAt(Location at) {
        Location l = body.getLocation();
        Vector v = at.toVector().subtract(l.toVector());
        float yaw = (float) Math.toDegrees(Math.atan2(-v.getX(), v.getZ()));
        float pitch = (float) Math.toDegrees(-Math.atan2(v.getY(), Math.hypot(v.getX(), v.getZ())));
        body.setRotation(yaw, pitch);
    }
    private void stopTask() { target = null; breakProgress = 0; houseQueue = null; if (mode == Mode.POSSESSED) unpossess(); }

    void say(Player p, String msg) { if (p != null) p.sendMessage(Component.text("[Rook] ", NamedTextColor.AQUA).append(Component.text(msg, NamedTextColor.WHITE))); }
    private static String fmt(Location l) { return l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ(); }
}
