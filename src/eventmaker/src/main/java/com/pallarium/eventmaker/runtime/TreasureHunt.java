package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TreasureHunt extends ActiveEvent {
    private Location chest;
    private boolean found = false;

    public TreasureHunt(EventMakerPlugin plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    protected void onStart() {
        Location c = event.center();
        int r = Math.max(10, event.radius);
        for (int attempt = 0; attempt < 40; attempt++) {
            int x = c.getBlockX() + random.nextInt(r * 2 + 1) - r;
            int z = c.getBlockZ() + random.nextInt(r * 2 + 1) - r;
            int y = c.getWorld().getHighestBlockYAt(x, z) + 1;
            Block b = c.getWorld().getBlockAt(x, y, z);
            if (b.getType() == Material.AIR && b.getRelative(0, -1, 0).getType().isSolid()) {
                chest = b.getLocation();
                break;
            }
        }
        if (chest == null) chest = c.clone().add(0, 1, 0);
        chest.getBlock().setType(Material.CHEST);
        Chest state = (Chest) chest.getBlock().getState();
        for (ItemStack item : event.rewards) state.getInventory().addItem(item.clone());
        state.update();
        String biome = chest.getBlock().getBiome().name().toLowerCase().replace('_', ' ');
        plugin.broadcast("&eA treasure chest has been hidden within &f" + r + " blocks &eof &f"
                + c.getBlockX() + ", " + c.getBlockZ() + "&e! First to open it wins.");
        plugin.broadcast("&eHint 1: &7It sits in a &f" + biome + "&7 biome.");
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (found) return;
        chest.getWorld().spawnParticle(Particle.END_ROD, chest.clone().add(0.5, 1.2, 0.5), 2, 0.2, 0.3, 0.2, 0);
        if (elapsed == 45) {
            plugin.broadcast("&eHint 2: &7It's at height &fY " + (chest.getBlockY() / 10 * 10) + "&7-ish.");
        } else if (elapsed == 90) {
            String ns = chest.getBlockZ() < event.center().getBlockZ() ? "north" : "south";
            String ew = chest.getBlockX() < event.center().getBlockX() ? "west" : "east";
            plugin.broadcast("&eHint 3: &7Head &f" + ns + "-" + ew + " &7from the center.");
        } else if (elapsed % 60 == 0 && elapsed > 90) {
            plugin.broadcast("&eHint: &7X is within 15 of &f" + (chest.getBlockX() + random.nextInt(21) - 10) + "&7.");
        }
    }

    public boolean isChest(Location l) {
        return chest != null && !found && l.getBlockX() == chest.getBlockX()
                && l.getBlockY() == chest.getBlockY() && l.getBlockZ() == chest.getBlockZ()
                && l.getWorld().equals(chest.getWorld());
    }

    public void onOpened(Player p) {
        if (found) return;
        found = true;
        addScore(p, 1);
        plugin.broadcast("&a&l" + p.getName() + " &afound the treasure!");
        stop(false);
    }

    @Override
    protected void onEnd(boolean forced) {
        if (!found && chest != null) {
            if (chest.getBlock().getType() == Material.CHEST) {
                ((Chest) chest.getBlock().getState()).getInventory().clear();
                chest.getBlock().setType(Material.AIR);
            }
            if (!forced) plugin.broadcast("&7Nobody found the treasure. It's gone.");
        }
    }

    @Override
    protected String scoreLabel() {
        return "treasure found";
    }
}
