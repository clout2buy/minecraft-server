package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/** Lightning Storm / Anvil Rain / Arrow-ish chaos around every player. Whoever takes the least damage wins. */
public class SkyHazard extends ActiveEvent {
    public static final String META = "eventmaker_hazard";
    public enum Kind { LIGHTNING, ANVILS }

    private final Kind kind;
    private final Map<World, boolean[]> savedWeather = new HashMap<>();

    public SkyHazard(EventMakerPlugin plugin, GameEvent event, Kind kind) {
        super(plugin, event);
        this.kind = kind;
    }

    public static BiFunction<EventMakerPlugin, GameEvent, ActiveEvent> of(Kind kind) {
        return (p, e) -> new SkyHazard(p, e, kind);
    }

    @Override
    protected void onStart() {
        for (Player p : event.participants()) {
            World w = p.getWorld();
            if (!savedWeather.containsKey(w)) {
                savedWeather.put(w, new boolean[]{w.hasStorm(), w.isThundering()});
                if (kind == Kind.LIGHTNING) { w.setStorm(true); w.setThundering(true); }
            }
            scores.putIfAbsent(p.getUniqueId(), 0.0);
        }
        plugin.broadcast(kind == Kind.LIGHTNING ? "&eThe sky is angry. Keep moving!" : "&7Look up. Seriously, look up.");
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        int every = kind == Kind.LIGHTNING ? 3 : 1;
        if (elapsed % every != 0) return;
        for (Player p : event.participants()) {
            scores.putIfAbsent(p.getUniqueId(), 0.0);
            double a = random.nextDouble() * Math.PI * 2;
            if (kind == Kind.LIGHTNING) {
                double d = 3 + random.nextInt(10);
                Location l = p.getLocation().clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                l.setY(l.getWorld().getHighestBlockYAt(l) + 1);
                if (random.nextInt(4) == 0) l.getWorld().strikeLightning(l);
                else l.getWorld().strikeLightningEffect(l);
            } else {
                for (int i = 0; i < 2; i++) {
                    double d = random.nextDouble() * 5;
                    Location l = p.getLocation().clone().add(Math.cos(a) * d, 14 + random.nextInt(6), Math.sin(a) * d);
                    FallingBlock fb = l.getWorld().spawnFallingBlock(l, Material.ANVIL.createBlockData());
                    fb.setDropItem(false);
                    fb.setHurtEntities(true);
                    fb.setMetadata(META, new FixedMetadataValue(plugin, event.id));
                }
                if (elapsed % 4 == 0) p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, 0.6f, 0.8f);
            }
        }
    }

    /** Called by GameListener when a participant takes damage from our hazard. */
    public void onHurt(Player p, double dmg) {
        addScore(p, -dmg);
    }

    @Override
    protected void onEnd(boolean forced) {
        for (Map.Entry<World, boolean[]> e : savedWeather.entrySet()) {
            e.getKey().setStorm(e.getValue()[0]);
            e.getKey().setThundering(e.getValue()[1]);
        }
        if (!forced) announceWinnersAndReward();
    }

    @Override
    protected String formatScore(double v) {
        return String.format("%.1f dmg taken", -v);
    }

    @Override
    protected String scoreLabel() {
        return "least damage taken";
    }
}
