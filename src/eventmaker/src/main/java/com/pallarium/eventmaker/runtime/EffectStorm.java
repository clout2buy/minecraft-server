package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.function.BiFunction;

/** Everyone gets potion effects for the whole event. Rewards go to everyone at the end. */
public class EffectStorm extends ActiveEvent {
    private final PotionEffect[] effects;

    public EffectStorm(EventMakerPlugin plugin, GameEvent event, PotionEffect[] effects) {
        super(plugin, event);
        this.effects = effects;
    }

    public static BiFunction<EventMakerPlugin, GameEvent, ActiveEvent> of(Object... spec) {
        PotionEffect[] fx = new PotionEffect[spec.length / 2];
        for (int i = 0; i < fx.length; i++) {
            fx[i] = new PotionEffect((PotionEffectType) spec[i * 2], 20 * 12, (Integer) spec[i * 2 + 1], true, false, true);
        }
        return (p, e) -> new EffectStorm(p, e, fx);
    }

    @Override
    protected void onStart() {
        apply();
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (elapsed % 5 == 0) apply();
    }

    private void apply() {
        for (Player p : event.participants()) {
            for (PotionEffect fx : effects) p.addPotionEffect(fx);
        }
    }

    @Override
    protected void onEnd(boolean forced) {
        for (Player p : event.participants()) {
            for (PotionEffect fx : effects) p.removePotionEffect(fx.getType());
            if (!forced && !event.rewards.isEmpty()) giveRewards(p);
        }
    }

    @Override
    protected String scoreLabel() {
        return "";
    }
}
