package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import com.pallarium.eventmaker.util.Text;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class MobWave extends ActiveEvent {
    public static final String META = "eventmaker_wave";
    private static final EntityType[] POOL = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
            EntityType.HUSK, EntityType.STRAY, EntityType.PILLAGER, EntityType.VINDICATOR};
    private int wave = 0;

    public MobWave(EventMakerPlugin plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    protected void onStart() {
        spawnWave();
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (elapsed % 25 == 0) spawnWave();
    }

    private void spawnWave() {
        wave++;
        List<Player> players = event.participants();
        if (players.isEmpty()) return;
        int perPlayer = Math.min(3 + wave, 10);
        for (Player p : players) {
            for (int i = 0; i < perPlayer; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double d = 8 + random.nextInt(8);
                Location base = p.getLocation().clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                base.setY(base.getWorld().getHighestBlockYAt(base) + 1);
                EntityType type = POOL[random.nextInt(Math.min(POOL.length, 3 + wave))];
                LivingEntity mob = (LivingEntity) base.getWorld().spawnEntity(base, type);
                mob.setCustomName(Text.color("&cWave " + wave));
                mob.setRemoveWhenFarAway(true);
                mob.setMetadata(META, new FixedMetadataValue(plugin, event.id));
            }
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.4f, 1.5f);
        }
        plugin.broadcast("&c&lWave " + wave + " &7incoming! &8(" + perPlayer + " per player)");
    }

    public void onKill(Player killer) {
        addScore(killer, 1);
        killer.sendActionBar(Text.color("&cKills: &f" + formatScore(scores.get(killer.getUniqueId()))));
    }

    @Override
    protected void onEnd(boolean forced) {
        for (Entity e : event.center().getWorld().getEntities()) {
            if (e.hasMetadata(META)) e.remove();
        }
        if (!forced) announceWinnersAndReward();
    }

    @Override
    protected String scoreLabel() {
        return "kills";
    }
}
