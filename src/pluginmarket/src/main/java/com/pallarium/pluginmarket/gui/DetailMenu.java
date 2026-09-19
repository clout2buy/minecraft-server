package com.pallarium.pluginmarket.gui;

import com.pallarium.pluginmarket.PluginMarketPlugin;
import com.pallarium.pluginmarket.model.PluginInfo;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Full info page for one plugin, with a big download button. */
public class DetailMenu extends Menu {
    private final PluginMarketPlugin plugin;
    private final Map<UUID, PluginInfo> viewing = new HashMap<>();
    private final Map<UUID, String> status = new HashMap<>();
    private final Map<UUID, Boolean> busy = new HashMap<>();

    private static class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public DetailMenu(PluginMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p, PluginInfo info) {
        viewing.put(p.getUniqueId(), info);
        status.remove(p.getUniqueId());
        busy.put(p.getUniqueId(), false);
        render(p);
    }

    private void render(Player p) {
        PluginInfo info = viewing.get(p.getUniqueId());
        if (info == null) return;
        Inventory inv = Bukkit.createInventory(new Holder(), 45, ChatColor.BLACK + "" + ChatColor.BOLD + "Plugin Details");
        for (int i = 0; i < 45; i++) inv.setItem(i, pane());

        inv.setItem(0, item(Material.ARROW, ChatColor.YELLOW + "\u2190 Back to results", null));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "by " + ChatColor.WHITE + info.author);
        lore.add("");
        lore.add(ChatColor.GRAY + "Tag: " + ChatColor.WHITE + (info.tag.isEmpty() ? "n/a" : info.tag));
        Material icon = Icons.forPlugin(info);
        inv.setItem(13, item(icon, ChatColor.WHITE + "" + ChatColor.BOLD + info.name, lore));

        org.bukkit.inventory.ItemStack head = Icons.creatorHead(info.author);
        org.bukkit.inventory.meta.ItemMeta hm = head.getItemMeta();
        hm.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + info.author);
        hm.setLore(List.of(ChatColor.GRAY + "Creator", ChatColor.DARK_GRAY + "Head shows their skin when", ChatColor.DARK_GRAY + "their SpigotMC name matches a Minecraft name"));
        head.setItemMeta(hm);
        inv.setItem(4, head);

        inv.setItem(20, item(Material.HOPPER, ChatColor.GREEN + "Downloads",
                List.of(ChatColor.WHITE + String.format("%,d", info.downloads), ChatColor.GRAY + "(" + info.downloadsShort() + ")")));
        inv.setItem(21, item(Material.NETHER_STAR, ChatColor.YELLOW + "Rating",
                List.of(ChatColor.WHITE + String.format("%.2f", info.rating) + " / 5.0", ChatColor.GRAY + (info.ratingCount + " ratings"))));
        inv.setItem(22, item(Material.IRON_INGOT, ChatColor.AQUA + "Version",
                List.of(ChatColor.WHITE + info.version)));
        inv.setItem(23, item(Material.CLOCK, ChatColor.GOLD + "Released",
                List.of(ChatColor.WHITE + info.releasedAgo())));
        inv.setItem(24, item(Material.RECOVERY_COMPASS, ChatColor.GOLD + "Last updated",
                List.of(ChatColor.WHITE + info.updatedAgo())));

        List<String> priceLore = new ArrayList<>();
        priceLore.add(info.premium ? ChatColor.GOLD + "Premium resource" : ChatColor.GREEN + "Free");
        if (info.externalDownload) priceLore.add(ChatColor.RED + "Hosted externally - can't auto-install");
        inv.setItem(31, item(info.premium ? Material.EMERALD : Material.LIME_DYE,
                info.premium ? ChatColor.GOLD + "" + ChatColor.BOLD + "PREMIUM" : ChatColor.GREEN + "" + ChatColor.BOLD + "FREE", priceLore));

        String stat = status.get(p.getUniqueId());
        boolean isBusy = busy.getOrDefault(p.getUniqueId(), false);
        Material dlIcon = isBusy ? Material.CLOCK : (info.externalDownload ? Material.BARRIER : Material.ANVIL);
        List<String> dlLore = new ArrayList<>();
        if (isBusy) {
            dlLore.add(ChatColor.YELLOW + "Downloading...");
        } else if (info.externalDownload) {
            dlLore.add(ChatColor.RED + "This resource must be downloaded");
            dlLore.add(ChatColor.RED + "from SpigotMC directly.");
        } else if (stat != null) {
            dlLore.add(stat);
        } else {
            dlLore.add(ChatColor.GRAY + "Downloads the jar straight into");
            dlLore.add(ChatColor.GRAY + "your server's plugins folder.");
            dlLore.add("");
            dlLore.add(ChatColor.YELLOW + "Click to install");
        }
        String dlName = isBusy ? ChatColor.YELLOW + "" + ChatColor.BOLD + "Installing..."
                : info.externalDownload ? ChatColor.RED + "" + ChatColor.BOLD + "External Download"
                : ChatColor.GREEN + "" + ChatColor.BOLD + "DOWNLOAD & INSTALL";
        inv.setItem(40, item(dlIcon, dlName, dlLore));

        p.openInventory(inv);
    }

    @Override
    public void handleClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        PluginInfo info = viewing.get(p.getUniqueId());
        if (info == null) return;

        if (e.getRawSlot() == 0) {
            plugin.listingMenu().refresh(p);
            return;
        }
        if (e.getRawSlot() == 40) {
            if (busy.getOrDefault(p.getUniqueId(), false)) return;
            if (!p.hasPermission("pluginmarket.download")) {
                p.sendMessage(ChatColor.RED + "You don't have permission to install plugins.");
                return;
            }
            if (info.externalDownload) return;
            busy.put(p.getUniqueId(), true);
            status.remove(p.getUniqueId());
            render(p);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
            plugin.market().download(p, info,
                    file -> {
                        busy.put(p.getUniqueId(), false);
                        status.put(p.getUniqueId(), ChatColor.GREEN + "Installed! " + ChatColor.GRAY + file.getName());
                        p.sendMessage(ChatColor.GREEN + "[PluginMarket] " + ChatColor.WHITE + info.name + ChatColor.GREEN + " installed. Restart or /reload to load it.");
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
                        if (p.getOpenInventory() != null && p.getOpenInventory().getTitle().equals(ChatColor.BLACK + "" + ChatColor.BOLD + "Plugin Details")) render(p);
                    },
                    ex -> {
                        busy.put(p.getUniqueId(), false);
                        status.put(p.getUniqueId(), ChatColor.RED + "Failed: " + ex.getMessage());
                        p.sendMessage(ChatColor.RED + "[PluginMarket] Install failed: " + ex.getMessage());
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.6f);
                        if (p.getOpenInventory() != null && p.getOpenInventory().getTitle().equals(ChatColor.BLACK + "" + ChatColor.BOLD + "Plugin Details")) render(p);
                    });
        }
    }

    public static boolean owns(InventoryClickEvent e) {
        return e.getInventory().getHolder() instanceof Holder;
    }
}
