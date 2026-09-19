package com.pallarium.endgame.service;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.data.PlayerProfile;
import com.pallarium.endgame.ui.Icon;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Gives players a clickable hotbar item that opens the menu, so nobody has to
 * remember a command. Tagged with persistent data so it survives restarts and
 * can never be confused with a normal nether star.
 */
public class MenuItemService {

    private final EndgamePlugin plugin;
    private final NamespacedKey key;

    public MenuItemService(EndgamePlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "endgame_menu");
    }

    public ItemStack create() {
        ItemStack stack = Icon.of(Material.NETHER_STAR)
                .name("ENDGAME", Icon.NEON)
                .line("Your skills, perks and rank", Icon.DIM)
                .blank()
                .line("Right click to open", Icon.ACCENT)
                .build();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public boolean isMenuItem(ItemStack stack) {
        if (stack == null || stack.getType() != Material.NETHER_STAR) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer()
                .has(key, PersistentDataType.BYTE);
    }

    /** Ensures the player's hotbar matches their setting. */
    public void sync(Player player, PlayerProfile profile) {
        boolean shouldHave = plugin.settings().giveMenuItem() && profile.menuItemEnabled();
        int slot = plugin.settings().menuItemSlot();

        // strip any existing copies first
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isMenuItem(contents[i])) {
                player.getInventory().setItem(i, null);
            }
        }

        if (!shouldHave) {
            player.updateInventory();
            return;
        }

        ItemStack existing = player.getInventory().getItem(slot);
        if (existing == null || existing.getType() == Material.AIR) {
            player.getInventory().setItem(slot, create());
        } else {
            player.getInventory().addItem(create());
        }
        player.updateInventory();
    }
}
