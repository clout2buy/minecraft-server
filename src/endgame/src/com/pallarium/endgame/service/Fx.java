package com.pallarium.endgame.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Small sound and particle helpers so feedback is consistent everywhere.
 */
public final class Fx {

    private Fx() {
    }

    public static void click(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.45f, 1.7f);
    }

    public static void deny(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
    }

    public static void purchase(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.2f);
    }

    public static void open(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.35f, 1.8f);
    }

    public static void levelUp(Player p) {
        Location at = p.getLocation();
        p.playSound(at, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        p.playSound(at, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.8f);
        p.getWorld().spawnParticle(Particle.TOTEM, at.clone().add(0, 1.1, 0), 40, 0.45, 0.7, 0.45, 0.32);
        p.getWorld().spawnParticle(Particle.END_ROD, at.clone().add(0, 1.3, 0), 14, 0.35, 0.5, 0.35, 0.05);
    }

    public static void milestone(Player p) {
        Location at = p.getLocation();
        p.playSound(at, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        p.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, at.clone().add(0, 1.2, 0),
                60, 0.6, 0.8, 0.6, 0.18);
    }

    public static void treasure(Player p, Location at) {
        p.playSound(at, Sound.ENTITY_ITEM_PICKUP, 0.7f, 0.8f);
        p.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, at.clone().add(0.5, 0.6, 0.5),
                12, 0.3, 0.3, 0.3, 0.02);
    }

    public static Component text(String s, TextColor color) {
        return Component.text(s, color);
    }
}
