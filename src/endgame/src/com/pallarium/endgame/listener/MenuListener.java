package com.pallarium.endgame.listener;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.service.Fx;
import com.pallarium.endgame.ui.MainMenu;
import com.pallarium.endgame.ui.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Routes clicks into the owning {@link Menu} and protects the menu item
 * from being dropped or moved out of the hotbar.
 */
public class MenuListener implements Listener {

    private final EndgamePlugin plugin;

    public MenuListener(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof Menu menu)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.getUniqueId().equals(menu.player().getUniqueId())) {
            return;
        }
        // only react to clicks inside our own inventory
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }
        menu.click(event.getRawSlot(), event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!plugin.menuItem().isMenuItem(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Fx.open(player);
        new MainMenu(plugin, player).open();
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.menuItem().isMenuItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
