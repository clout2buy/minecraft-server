package com.pallarium.endgame.event;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A world event that is happening right now. Mutable running state. */
public class ActiveEvent {

    private final WorldEvent def;
    private final Location center;
    private final UUID id = UUID.randomUUID();
    private final long startedAt = System.currentTimeMillis();

    private int stageIndex;
    private int progress;
    private int ticksLeft;
    private boolean finished;
    private boolean failed;

    private BossBar bar;
    private final Set<UUID> mobs = new HashSet<>();
    private final List<Block> marked = new ArrayList<>();
    private final Map<UUID, Integer> contribution = new HashMap<>();
    private LivingEntity escortee;
    private Location escortTarget;
    private long lastMobSpawn;

    private Mutator mutator = Mutator.NONE;
    private final Structure structure = new Structure();
    private final List<Block> braziers = new ArrayList<>();
    private final Set<Block> litBraziers = new HashSet<>();
    private LivingEntity chaseTarget;
    private Block core;
    private double coreHealth = 100;
    private double ringShrink;

    public ActiveEvent(WorldEvent def, Location center) {
        this.def = def;
        this.center = center;
        this.ticksLeft = def.stages().get(0).seconds() * 20;
    }

    public Mutator mutator() {
        return mutator;
    }

    public void mutator(Mutator m) {
        this.mutator = m;
    }

    public Structure structure() {
        return structure;
    }

    public List<Block> braziers() {
        return braziers;
    }

    public Set<Block> litBraziers() {
        return litBraziers;
    }

    public LivingEntity chaseTarget() {
        return chaseTarget;
    }

    public void chaseTarget(LivingEntity e) {
        this.chaseTarget = e;
    }

    public Block core() {
        return core;
    }

    public void core(Block b) {
        this.core = b;
    }

    public double coreHealth() {
        return coreHealth;
    }

    public void damageCore(double amount) {
        coreHealth = Math.max(0, coreHealth - amount);
    }

    /** How far the CLOSING mutator has pulled the ring in, in blocks. */
    public double ringShrink() {
        return ringShrink;
    }

    public void ringShrink(double d) {
        this.ringShrink = d;
    }

    /** The live radius, accounting for a closing ring. */
    public double liveRadius() {
        return Math.max(8, def.radius() - ringShrink);
    }

    public WorldEvent def() {
        return def;
    }

    public Location center() {
        return center;
    }

    public UUID id() {
        return id;
    }

    public long startedAt() {
        return startedAt;
    }

    public EventStage stage() {
        return def.stages().get(stageIndex);
    }

    public int stageIndex() {
        return stageIndex;
    }

    public int stageCount() {
        return def.stages().size();
    }

    public int progress() {
        return progress;
    }

    public void addProgress(int n) {
        progress += n;
    }

    public void setProgress(int n) {
        progress = n;
    }

    public int ticksLeft() {
        return ticksLeft;
    }

    public void tickDown() {
        ticksLeft--;
    }

    public boolean stageComplete() {
        return progress >= stage().goal();
    }

    /** Moves to the next stage. Returns false when the whole chain is done. */
    public boolean advance() {
        stageIndex++;
        progress = 0;
        if (stageIndex >= def.stages().size()) {
            finished = true;
            return false;
        }
        ticksLeft = stage().seconds() * 20;
        return true;
    }

    public boolean finished() {
        return finished;
    }

    public void finish() {
        finished = true;
    }

    public boolean failed() {
        return failed;
    }

    public void fail() {
        failed = true;
        finished = true;
    }

    public BossBar bar() {
        return bar;
    }

    public void bar(BossBar b) {
        this.bar = b;
    }

    public Set<UUID> mobs() {
        return mobs;
    }

    public List<Block> marked() {
        return marked;
    }

    public LivingEntity escortee() {
        return escortee;
    }

    public void escortee(LivingEntity e) {
        this.escortee = e;
    }

    public Location escortTarget() {
        return escortTarget;
    }

    public void escortTarget(Location l) {
        this.escortTarget = l;
    }

    public long lastMobSpawn() {
        return lastMobSpawn;
    }

    public void lastMobSpawn(long t) {
        this.lastMobSpawn = t;
    }

    /** Credits a player for taking part. Drives reward scaling at the end. */
    public void credit(Player player, int weight) {
        contribution.merge(player.getUniqueId(), weight, Integer::sum);
    }

    public Map<UUID, Integer> contribution() {
        return contribution;
    }

    public int contributionOf(Player player) {
        return contribution.getOrDefault(player.getUniqueId(), 0);
    }

    public boolean inRange(Location loc) {
        return loc.getWorld() == center.getWorld()
                && loc.distanceSquared(center) <= (double) def.radius() * def.radius();
    }

    public double fraction() {
        return Math.min(1.0, progress / (double) Math.max(1, stage().goal()));
    }

    public NamespacedKey key(org.bukkit.plugin.Plugin plugin) {
        return new NamespacedKey(plugin, "event_mob");
    }
}
