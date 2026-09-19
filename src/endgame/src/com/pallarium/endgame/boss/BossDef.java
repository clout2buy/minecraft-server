package com.pallarium.endgame.boss;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * A boss as data. Everything a fight needs lives here: the shell mob, its
 * stats, its phases, what it says, what it drops. Adding a new boss is adding
 * one of these to {@link Bosses}, nothing else.
 */
public class BossDef {

    private final String id;
    private final String display;
    private final String subtitle;
    private final EntityType type;
    private final TextColor color;
    private final Material icon;

    private double health = 400;
    private double damage = 9;
    private double armor = 8;
    private double speed = 0.28;
    private double arenaRadius = 22;
    private double knockbackResist = 0.8;

    private final List<Phase> phases = new ArrayList<>();
    private EntityType[] minions = {EntityType.ZOMBIE, EntityType.SKELETON};
    private final List<Drop> drops = new ArrayList<>();
    private String spawnLine = "has awoken";
    private String deathLine = "falls";
    private String lore = "";
    private int xpReward = 4000;

    public BossDef(String id, String display, String subtitle, EntityType type,
                   TextColor color, Material icon) {
        this.id = id;
        this.display = display;
        this.subtitle = subtitle;
        this.type = type;
        this.color = color;
        this.icon = icon;
    }

    /** A single loot entry with a roll chance and a quantity band. */
    public record Drop(Material material, int min, int max, double chance, String name) {
    }

    /* ---- builders ---------------------------------------------------- */

    public BossDef stats(double health, double damage, double armor, double speed) {
        this.health = health;
        this.damage = damage;
        this.armor = armor;
        this.speed = speed;
        return this;
    }

    public BossDef arena(double radius) {
        this.arenaRadius = radius;
        return this;
    }

    public BossDef knockback(double resist) {
        this.knockbackResist = resist;
        return this;
    }

    public BossDef phase(Phase p) {
        phases.add(p);
        return this;
    }

    public BossDef minions(EntityType... types) {
        this.minions = types;
        return this;
    }

    public BossDef drop(Material m, int min, int max, double chance, String name) {
        drops.add(new Drop(m, min, max, chance, name));
        return this;
    }

    public BossDef lines(String spawn, String death) {
        this.spawnLine = spawn;
        this.deathLine = death;
        return this;
    }

    public BossDef lore(String text) {
        this.lore = text;
        return this;
    }

    public BossDef xp(int amount) {
        this.xpReward = amount;
        return this;
    }

    /* ---- reads ------------------------------------------------------- */

    public String id() {
        return id;
    }

    public String display() {
        return display;
    }

    public String subtitle() {
        return subtitle;
    }

    public EntityType type() {
        return type;
    }

    public TextColor color() {
        return color;
    }

    public Material icon() {
        return icon;
    }

    /** The theme colour as a Bukkit colour, for particles. */
    public org.bukkit.Color colorRgb() {
        int v = color.value();
        return org.bukkit.Color.fromRGB((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF);
    }

    public double health() {
        return health;
    }

    public double damage() {
        return damage;
    }

    public double armor() {
        return armor;
    }

    public double speed() {
        return speed;
    }

    public double arenaRadius() {
        return arenaRadius;
    }

    public double knockbackResist() {
        return knockbackResist;
    }

    public List<Phase> phases() {
        return phases;
    }

    public EntityType[] minions() {
        return minions;
    }

    public List<Drop> drops() {
        return drops;
    }

    public String spawnLine() {
        return spawnLine;
    }

    public String deathLine() {
        return deathLine;
    }

    public String lore() {
        return lore;
    }

    public int xpReward() {
        return xpReward;
    }

    /** Picks the phase matching a health fraction. Phases run high to low. */
    public Phase phaseFor(double frac) {
        Phase best = phases.isEmpty() ? null : phases.get(0);
        for (Phase p : phases) {
            if (frac <= p.healthAbove()) {
                best = p;
            }
        }
        return best;
    }
}
