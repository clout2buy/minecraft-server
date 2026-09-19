package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

public class DropParty extends ActiveEvent {
    private Location center;

    public DropParty(EventMakerPlugin plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    protected void onStart() {
        center = event.center().add(0, 6, 0);
        plugin.broadcast("&bDrop party at &f" + center.getBlockX() + ", " + (center.getBlockY() - 6) + ", " + center.getBlockZ() + "&b!");
        if (event.rewards.isEmpty()) plugin.broadcast("&7(No reward items set - add some in the editor.)");
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        ItemStack loot = randomReward();
        if (loot == null) return;
        int spread = Math.max(2, Math.min(event.radius, 10));
        for (int i = 0; i < 2; i++) {
            Location l = center.clone().add(random.nextDouble() * spread * 2 - spread, 0, random.nextDouble() * spread * 2 - spread);
            ItemStack single = loot.clone();
            single.setAmount(1);
            Item drop = center.getWorld().dropItem(l, single);
            drop.setVelocity(new Vector(0, -0.1, 0));
            drop.setPickupDelay(10);
        }
        if (elapsed % 5 == 0) {
            Firework fw = center.getWorld().spawn(center, Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().withColor(Color.FUCHSIA, Color.AQUA).with(FireworkEffect.Type.BALL).build());
            meta.setPower(0);
            fw.setFireworkMeta(meta);
            center.getWorld().playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        }
    }

    @Override
    protected void onEnd(boolean forced) {
        plugin.broadcast("&bThe drop party is over. Hope you grabbed something!");
    }

    @Override
    protected String scoreLabel() {
        return "";
    }
}
