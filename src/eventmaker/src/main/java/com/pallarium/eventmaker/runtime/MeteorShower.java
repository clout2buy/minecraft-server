package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.List;

public class MeteorShower extends ActiveEvent {
    public static final String META = "eventmaker_meteor";

    public MeteorShower(EventMakerPlugin plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    protected void onStart() {
        plugin.broadcast("&cLook up! Meteors incoming for " + com.pallarium.eventmaker.util.Text.time(event.durationSeconds) + ".");
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (elapsed % 2 != 0) return;
        List<Player> players = event.participants();
        if (players.isEmpty()) return;
        Player target = players.get(random.nextInt(players.size()));
        Location base = target.getLocation();
        double ox = (random.nextDouble() * 2 - 1) * event.radius;
        double oz = (random.nextDouble() * 2 - 1) * event.radius;
        Location spawn = base.clone().add(ox, 40 + random.nextInt(15), oz);
        Location aim = base.clone().add(ox + random.nextInt(7) - 3, 0, oz + random.nextInt(7) - 3);
        Vector dir = aim.toVector().subtract(spawn.toVector()).normalize().multiply(0.8);
        Fireball fb = spawn.getWorld().spawn(spawn, Fireball.class);
        fb.setDirection(dir);
        fb.setVelocity(dir);
        fb.setYield(2.0f);
        fb.setIsIncendiary(false);
        fb.setMetadata(META, new FixedMetadataValue(plugin, event.id));
        spawn.getWorld().playSound(base, Sound.ENTITY_GHAST_SHOOT, 1f, 0.6f);
        spawn.getWorld().spawnParticle(Particle.LAVA, spawn, 10);
    }

    public void onImpact(Location where) {
        ItemStack loot = randomReward();
        where.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, where, 3);
        where.getWorld().playSound(where, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
        if (loot != null && random.nextInt(3) == 0) {
            Item drop = where.getWorld().dropItemNaturally(where.clone().add(0, 1, 0), loot);
            drop.setGlowing(true);
            drop.setCustomName(com.pallarium.eventmaker.util.Text.color("&6Meteor Loot"));
            drop.setCustomNameVisible(true);
        }
    }

    @Override
    protected void onEnd(boolean forced) {
        plugin.broadcast("&7The skies are clear again.");
    }

    @Override
    protected String scoreLabel() {
        return "";
    }
}
