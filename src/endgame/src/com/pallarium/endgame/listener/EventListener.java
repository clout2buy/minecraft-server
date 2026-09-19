package com.pallarium.endgame.listener;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.event.ActiveEvent;
import com.pallarium.endgame.event.Trigger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;

/**
 * Turns ordinary play into event heat, and routes event relevant actions
 * (kills, marked blocks, the escort dying) back into the service.
 */
public class EventListener implements Listener {

    private final EndgamePlugin plugin;

    public EventListener(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    /* ------------------------------------------------------------------ */
    /*  blocks feed DIGGING / LOGGING / HARVEST                            */
    /* ------------------------------------------------------------------ */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // an event's marked block takes priority over heat
        if (plugin.events().onMarkedBreak(block, player)) {
            return;
        }
        if (plugin.placed().isPlaced(block)) {
            return;
        }

        Material type = block.getType();
        String n = type.name();

        if (n.endsWith("_ORE") || n.equals("ANCIENT_DEBRIS")) {
            // deeper ore stirs more
            double depth = block.getY() < 0 ? 2.2 : block.getY() < 32 ? 1.5 : 1.0;
            plugin.events().heat(player, Trigger.DIGGING, 4.0 * depth);
        } else if (n.endsWith("_LOG") || n.endsWith("_STEM")) {
            plugin.events().heat(player, Trigger.LOGGING, 2.5);
        } else if (type == Material.WHEAT || type == Material.CARROTS
                || type == Material.POTATOES || type == Material.BEETROOTS
                || type == Material.NETHER_WART || type == Material.PUMPKIN
                || type == Material.MELON) {
            plugin.events().heat(player, Trigger.HARVEST, 2.0);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  kills feed SLAUGHTER, and drive event stages                       */
    /* ------------------------------------------------------------------ */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();

        ActiveEvent owner = plugin.events().eventOf(dead);
        if (owner != null) {
            // the escort dying is a loss, not progress
            if (dead instanceof Villager && owner.escortee() != null
                    && owner.escortee().getUniqueId().equals(dead.getUniqueId())) {
                plugin.events().onEscortDeath(dead);
                return;
            }
            plugin.events().onEventMobDeath(dead, killer);
            return;
        }

        if (killer != null && dead instanceof org.bukkit.entity.Monster) {
            plugin.events().heat(killer, Trigger.SLAUGHTER, 3.0);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  fishing feeds TIDE                                                 */
    /* ------------------------------------------------------------------ */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            plugin.events().heat(event.getPlayer(), Trigger.TIDE, 6.0);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  picking up essences drives COLLECT stages                          */
    /* ------------------------------------------------------------------ */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.events().onEssencePickup(player, event.getItem().getItemStack());
        }
    }
}
