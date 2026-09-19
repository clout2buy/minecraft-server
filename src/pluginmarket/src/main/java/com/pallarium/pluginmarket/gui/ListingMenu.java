package com.pallarium.pluginmarket.gui;

import com.pallarium.pluginmarket.PluginMarketPlugin;
import com.pallarium.pluginmarket.model.Category;
import com.pallarium.pluginmarket.model.PluginInfo;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Search + sort + paginated results grid for one category. */
public class ListingMenu extends Menu {
    private final PluginMarketPlugin plugin;
    private final Map<UUID, BrowseState> states = new HashMap<>();
    private static final int[] GRID = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static class Holder implements InventoryHolder {
        final List<Integer> slotIds = new ArrayList<>();

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public ListingMenu(PluginMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public BrowseState stateOf(Player p) {
        return states.computeIfAbsent(p.getUniqueId(), k -> new BrowseState());
    }

    public void open(Player p, Category category) {
        BrowseState st = stateOf(p);
        if (category != null && category != st.category) {
            st.category = category;
            st.page = 0;
            st.query = "";
        }
        render(p, st);
        fetch(p, st);
    }

    public void refresh(Player p) {
        BrowseState st = stateOf(p);
        render(p, st);
        fetch(p, st);
    }

    private void fetch(Player p, BrowseState st) {
        st.loading = true;
        st.error = null;
        plugin.market().search(st.category, st.sort, st.query, st.page,
                results -> {
                    st.loading = false;
                    st.results = results;
                    if (p.isOnline() && titleMatches(p)) render(p, st);
                },
                ex -> {
                    st.loading = false;
                    st.error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    if (p.isOnline() && titleMatches(p)) render(p, st);
                });
    }

    private boolean titleMatches(Player p) {
        return p.getOpenInventory() != null && p.getOpenInventory().getTitle().startsWith(ChatColor.BLACK + "" + ChatColor.BOLD + "Market:");
    }

    private void render(Player p, BrowseState st) {
        Holder h = new Holder();
        String title = ChatColor.BLACK + "" + ChatColor.BOLD + "Market: " + st.category.label;
        Inventory inv = Bukkit.createInventory(h, 54, title);
        for (int i = 0; i < 54; i++) inv.setItem(i, pane());

        inv.setItem(0, item(Material.ARROW, ChatColor.YELLOW + "\u2190 Categories", null));
        inv.setItem(4, item(st.sort.icon, ChatColor.GOLD + "Sort: " + ChatColor.WHITE + st.sort.label,
                java.util.List.of(ChatColor.GRAY + "Click to cycle sort mode")));
        String qLabel = st.query.isEmpty() ? ChatColor.GRAY + "(none)" : ChatColor.WHITE + st.query;
        inv.setItem(8, item(Material.OAK_SIGN, ChatColor.GOLD + "Search: " + qLabel,
                java.util.List.of(ChatColor.GRAY + "Click to type a search term", ChatColor.GRAY + "Shift-click to clear")));

        if (st.loading) {
            inv.setItem(31, item(Material.CLOCK, ChatColor.YELLOW + "Loading...", null));
        } else if (st.error != null) {
            inv.setItem(31, item(Material.BARRIER, ChatColor.RED + "Error", java.util.List.of(ChatColor.GRAY + st.error, ChatColor.YELLOW + "Click to retry")));
        } else if (st.results.isEmpty()) {
            inv.setItem(31, item(Material.BARRIER, ChatColor.RED + "No results", java.util.List.of(ChatColor.GRAY + "Try a different search or sort")));
        }

        h.slotIds.clear();
        for (int i = 0; i < GRID.length; i++) h.slotIds.add(-1);
        for (int i = 0; i < st.results.size() && i < GRID.length; i++) {
            PluginInfo info = st.results.get(i);
            inv.setItem(GRID[i], tile(info));
            h.slotIds.set(i, info.id);
        }

        inv.setItem(45, item(Material.RED_STAINED_GLASS_PANE, st.page > 0 ? ChatColor.YELLOW + "\u2190 Previous page" : ChatColor.DARK_GRAY + "No previous page", null));
        inv.setItem(49, item(Material.BOOK, ChatColor.AQUA + "Page " + (st.page + 1), java.util.List.of(ChatColor.GRAY + (st.results.size() + " shown"))));
        inv.setItem(53, item(Material.LIME_STAINED_GLASS_PANE, st.results.size() >= plugin.getConfig().getInt("page-size", 28) ? ChatColor.YELLOW + "Next page \u2192" : ChatColor.DARK_GRAY + "No next page", null));

        Inventory prevOpen = p.getOpenInventory() != null ? p.getOpenInventory().getTopInventory() : null;
        boolean alreadyOpen = prevOpen != null && prevOpen.getHolder() instanceof Holder && p.getOpenInventory().getTitle().equals(title);
        p.openInventory(inv);
    }

    private ItemStack tile(PluginInfo info) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "by " + ChatColor.WHITE + info.author);
        lore.add(ChatColor.GRAY + "v" + info.version + (info.premium ? ChatColor.GOLD + "  $ premium" : ""));
        lore.add("");
        lore.add(ChatColor.GREEN + "\u2193 " + info.downloadsShort() + " downloads");
        lore.add(ChatColor.YELLOW + starBar(info.rating) + ChatColor.GRAY + " (" + info.ratingCount + ")");
        lore.add(ChatColor.DARK_GRAY + "Updated " + info.updatedAgo());
        lore.add("");
        lore.add(ChatColor.AQUA + "Click for details");
        Material icon = info.category == Category.SKRIPT ? Material.WRITABLE_BOOK
                : info.premium ? Material.GOLD_INGOT : Material.PAPER;
        return item(icon, ChatColor.WHITE + "" + ChatColor.BOLD + trim(info.name, 32), lore);
    }

    private String starBar(double rating) {
        int filled = (int) Math.round(rating);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < filled ? '\u2605' : '\u2606');
        return sb.toString();
    }

    private String trim(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }

    @Override
    public void handleClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Holder h = (Holder) e.getInventory().getHolder();
        BrowseState st = stateOf(p);
        int slot = e.getRawSlot();

        if (slot == 0) {
            plugin.categoryMenu().open(p);
            return;
        }
        if (slot == 4) {
            st.sort = st.sort.next();
            st.page = 0;
            refresh(p);
            return;
        }
        if (slot == 8) {
            if (e.isShiftClick()) {
                st.query = "";
                st.page = 0;
                refresh(p);
            } else {
                plugin.chatInput().ask(p, "Type a search term for " + st.category.label + ":", term -> {
                    st.query = term;
                    st.page = 0;
                    open(p, null);
                });
            }
            return;
        }
        if (slot == 31 && st.error != null) {
            refresh(p);
            return;
        }
        if (slot == 45 && st.page > 0) {
            st.page--;
            refresh(p);
            return;
        }
        if (slot == 53 && st.results.size() >= plugin.getConfig().getInt("page-size", 28)) {
            st.page++;
            refresh(p);
            return;
        }
        for (int i = 0; i < GRID.length; i++) {
            if (GRID[i] == slot && i < h.slotIds.size() && h.slotIds.get(i) != -1) {
                int id = h.slotIds.get(i);
                for (PluginInfo info : st.results) {
                    if (info.id == id) {
                        plugin.detailMenu().open(p, info);
                        return;
                    }
                }
            }
        }
    }

    public static boolean owns(InventoryClickEvent e) {
        return e.getInventory().getHolder() instanceof Holder;
    }
}
