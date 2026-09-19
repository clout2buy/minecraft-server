package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import com.pallarium.eventmaker.util.Text;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;

/** Generic "do the thing the most before the timer ends" event. Fed by GameListener. */
public class ScoreRace extends ActiveEvent {
    public enum Kind { MINING, ORE, LOGS, FISH, PVP, DISTANCE, BUILD, XP }

    public final Kind kind;

    public ScoreRace(EventMakerPlugin plugin, GameEvent event, Kind kind) {
        super(plugin, event);
        this.kind = kind;
    }

    public static BiFunction<EventMakerPlugin, GameEvent, ActiveEvent> of(Kind kind) {
        return (p, e) -> new ScoreRace(p, e, kind);
    }

    public static boolean isOre(Material m) {
        return m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS;
    }

    public static boolean isLog(Material m) {
        return Tag.LOGS.isTagged(m);
    }

    public void score(Player p, double amount) {
        if (!event.participants().contains(p)) return;
        addScore(p, amount);
        p.sendActionBar(Text.color("&d" + scoreLabel() + ": &f" + formatScore(scores.get(p.getUniqueId()))));
    }

    @Override
    protected void onStart() {
        plugin.broadcast("&d" + event.type.line1 + " &7Go!");
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (remaining == 30 || remaining == 10) {
            plugin.broadcast("&7" + remaining + "s left in &d" + event.name + "&7!");
        }
    }

    @Override
    protected void onEnd(boolean forced) {
        if (!forced) announceWinnersAndReward();
    }

    @Override
    protected String scoreLabel() {
        switch (kind) {
            case MINING: return "blocks mined";
            case ORE: return "ores mined";
            case LOGS: return "logs chopped";
            case FISH: return "fish caught";
            case PVP: return "player kills";
            case DISTANCE: return "blocks travelled";
            case BUILD: return "blocks placed";
            default: return "xp gained";
        }
    }
}
