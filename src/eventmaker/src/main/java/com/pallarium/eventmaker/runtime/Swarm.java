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
import java.util.function.BiFunction;

/** Generic "mobs keep spawning around players, most kills wins" event. */
public class Swarm extends ActiveEvent {
    public static final String META = "eventmaker_swarm";

    private final EntityType[] pool;
    private final boolean fromSky;
    private final int perPlayer;
    private final int every;
    private int wave = 0;

    public Swarm(EventMakerPlugin plugin, GameEvent event, EntityType[] pool, boolean fromSky, int perPlayer, int every) {
        super(plugin, event);
        this.pool = pool;
        this.fromSky = fromSky;
        this.perPlayer = perPlayer;
        this.every = every;
    }

    public static BiFunction<EventMakerPlugin, GameEvent, ActiveEvent> of(boolean fromSky, int perPlayer, int every, EntityType... pool) {
        return (p, e) -> new Swarm(p, e, pool, fromSky, perPlayer, every);
    }

    @Override
    protected void onStart() {
        spawn();
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (elapsed % every == 0) spawn();
    }

    private void spawn() {
        wave++;
        List<Player> players = event.participants();
        if (players.isEmpty()) return;
        int count = Math.min(perPlayer + wave / 2, perPlayer * 2);
        for (Player p : players) {
            for (int i = 0; i < count; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double d = fromSky ? 2 + random.nextInt(8) : 8 + random.nextInt(8);
                Location base = p.getLocation().clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                if (fromSky) base.add(0, 18 + random.nextInt(8), 0);
                else base.setY(base.getWorld().getHighestBlockYAt(base) + 1);
                EntityType type = pool[random.nextInt(pool.length)];
                Entity ent = base.getWorld().spawnEntity(base, type);
                if (ent instanceof LivingEntity) {
                    LivingEntity mob = (LivingEntity) ent;
                    mob.setCustomName(Text.color("&c" + event.name));
                    mob.setRemoveWhenFarAway(true);
                }
                ent.setMetadata(META, new FixedMetadataValue(plugin, event.id));
            }
            p.playSound(p.getLocation(), fromSky ? Sound.ENTITY_PHANTOM_FLAP : Sound.ENTITY_WITHER_SPAWN, 0.4f, 1.5f);
        }
        plugin.broadcast("&c&lWave " + wave + " &7incoming! &8(" + count + " per player)");
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
