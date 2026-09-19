package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ExperienceOrb;

public class XpFountain extends ActiveEvent {
    private Location center;

    public XpFountain(EventMakerPlugin plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    protected void onStart() {
        center = event.center().add(0, 3, 0);
        plugin.broadcast("&aAn XP fountain is bubbling at &f" + center.getBlockX() + ", " + (center.getBlockY() - 3) + ", " + center.getBlockZ() + "&a!");
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        int spread = Math.max(2, Math.min(event.radius, 6));
        for (int i = 0; i < 3; i++) {
            Location l = center.clone().add(random.nextDouble() * spread * 2 - spread, random.nextInt(3), random.nextDouble() * spread * 2 - spread);
            ExperienceOrb orb = center.getWorld().spawn(l, ExperienceOrb.class);
            orb.setExperience(3 + random.nextInt(8));
        }
        center.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, center, 12, spread, 1, spread);
        if (elapsed % 3 == 0) center.getWorld().playSound(center, Sound.ENTITY_EXPERIENCE_BOTTLE_THROW, 0.7f, 1.4f);
    }

    @Override
    protected void onEnd(boolean forced) {
        plugin.broadcast("&aThe XP fountain has dried up.");
    }

    @Override
    protected String scoreLabel() {
        return "";
    }
}
