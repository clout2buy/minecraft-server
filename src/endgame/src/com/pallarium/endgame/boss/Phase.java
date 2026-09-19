package com.pallarium.endgame.boss;

import java.util.ArrayList;
import java.util.List;

/**
 * One act of a fight. A phase owns its own ability rotation, its own cast
 * speed, and the health fraction that opens it. Crossing into a phase fires a
 * transition: a roar, a line of dialogue, and optionally an instant mechanic.
 */
public class Phase {

    private final String name;
    private final String line;
    private final double healthAbove;
    private final List<Ability> rotation = new ArrayList<>();
    private int castEveryTicks = 70;
    private double damageMult = 1.0;
    private Ability onEnter;
    private boolean random = true;

    public Phase(String name, double healthAbove, String line) {
        this.name = name;
        this.healthAbove = healthAbove;
        this.line = line;
    }

    public Phase abilities(Ability... list) {
        rotation.addAll(List.of(list));
        return this;
    }

    /** Ticks between casts in this phase. Lower is a harder phase. */
    public Phase cadence(int ticks) {
        this.castEveryTicks = ticks;
        return this;
    }

    public Phase damage(double mult) {
        this.damageMult = mult;
        return this;
    }

    /** Ability fired the instant the phase begins. */
    public Phase opener(Ability ability) {
        this.onEnter = ability;
        return this;
    }

    /** Walk the rotation in order instead of picking at random. */
    public Phase ordered() {
        this.random = false;
        return this;
    }

    public String name() {
        return name;
    }

    public String line() {
        return line;
    }

    /** Phase is active while boss health fraction is above this value. */
    public double healthAbove() {
        return healthAbove;
    }

    public List<Ability> rotation() {
        return rotation;
    }

    public int castEveryTicks() {
        return castEveryTicks;
    }

    public double damageMult() {
        return damageMult;
    }

    public Ability onEnter() {
        return onEnter;
    }

    public boolean random() {
        return random;
    }
}
