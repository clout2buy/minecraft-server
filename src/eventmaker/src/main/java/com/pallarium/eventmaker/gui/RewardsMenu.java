package com.pallarium.eventmaker.gui;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class RewardsMenu extends Menu {
    private final GameEvent event;
    private final boolean creating;

    public RewardsMenu(EventMakerPlugin plugin, Player player, GameEvent event, boolean creating) {
        super(plugin, player);
        this.event = event;
        this.creating = creating;
    }

    @Override
    protected String title() {
        return "&0&lRewards &8- drop items here, then close";
    }

    @Override
    protected int rows() {
        return 3;
    }

    @Override
    protected void fill() {}

    @Override
    protected void draw() {
        int i = 0;
        for (ItemStack item : event.rewards) {
            if (i >= inv.getSize()) break;
            inv.setItem(i++, item.clone());
        }
    }

    @Override
    public boolean lockBottom() {
        return false;
    }

    @Override
    public void click(InventoryClickEvent e) {
        e.setCancelled(false);
    }

    @Override
    public void closed(InventoryCloseEvent e) {
        event.rewards = new ArrayList<>();
        for (ItemStack item : inv.getContents()) {
            if (item != null && !item.getType().isAir()) event.rewards.add(item.clone());
        }
        if (!creating) {
            plugin.store().put(event);
            plugin.store().save();
        }
        plugin.msg(player, "&aSet &f" + event.rewards.size() + " &areward item(s).");
        Bukkit.getScheduler().runTask(plugin, () -> new EditorMenu(plugin, player, event, creating).open());
    }
}
