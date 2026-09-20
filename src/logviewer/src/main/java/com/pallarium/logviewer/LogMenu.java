package com.pallarium.logviewer;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LogMenu {
    private static final int ROWS_PER_PAGE = 36; // slots 9..44
    private final LogViewerPlugin plugin;
    private final Map<UUID, State> states = new HashMap<>();

    static class State {
        LogFilter filter = LogFilter.ALL;
        String query = "";
        int page = 0;
        boolean newestFirst = true;
        List<String> lines = new ArrayList<>();
    }

    private static class Holder implements InventoryHolder {
        final List<String> slotLines = new ArrayList<>();
        public Inventory getInventory() { return null; }
    }

    public LogMenu(LogViewerPlugin plugin) {
        this.plugin = plugin;
    }

    State state(Player p) {
        return states.computeIfAbsent(p.getUniqueId(), k -> new State());
    }

    public void open(Player p) {
        State st = state(p);
        st.lines = plugin.reader().filtered(st.filter, st.query);
        if (st.newestFirst) java.util.Collections.reverse(st.lines);
        int pages = Math.max(1, (st.lines.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        if (st.page >= pages) st.page = pages - 1;
        if (st.page < 0) st.page = 0;

        Holder h = new Holder();
        String title = ChatColor.BLACK + "" + ChatColor.BOLD + "Logs: " + ChatColor.DARK_AQUA + st.filter.label
                + ChatColor.DARK_GRAY + "  " + (st.page + 1) + "/" + pages;
        Inventory inv = Bukkit.createInventory(h, 54, title);

        // top row: filter tabs
        LogFilter[] fs = LogFilter.values();
        for (int i = 0; i < fs.length && i < 8; i++) {
            LogFilter f = fs[i];
            boolean active = f == st.filter;
            ItemStack it = item(active ? Material.LIME_STAINED_GLASS_PANE : f.icon,
                    (active ? ChatColor.GREEN + "" + ChatColor.BOLD : ChatColor.WHITE + "") + f.label,
                    List.of(active ? ChatColor.GRAY + "Currently showing" : ChatColor.YELLOW + "Click to filter"));
            if (active) { ItemMeta m = it.getItemMeta(); m.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true); m.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS); it.setItemMeta(m); }
            inv.setItem(i, it);
        }
        inv.setItem(8, item(Material.OAK_SIGN, ChatColor.AQUA + "" + ChatColor.BOLD + "Search",
                List.of(st.query.isEmpty() ? ChatColor.GRAY + "No search active" : ChatColor.WHITE + "\"" + st.query + "\"",
                        "", ChatColor.YELLOW + "Left-click: type a search", ChatColor.YELLOW + "Right-click: clear")));

        // body
        int start = st.page * ROWS_PER_PAGE;
        for (int i = 0; i < ROWS_PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= st.lines.size()) { h.slotLines.add(null); continue; }
            String raw = st.lines.get(idx);
            h.slotLines.add(raw);
            inv.setItem(9 + i, lineItem(raw));
        }

        // bottom row
        inv.setItem(45, item(Material.ARROW, st.page > 0 ? ChatColor.YELLOW + "\u2190 Previous" : ChatColor.DARK_GRAY + "Previous", null));
        inv.setItem(47, item(Material.COMPASS, ChatColor.GOLD + (st.newestFirst ? "Newest first" : "Oldest first"),
                List.of(ChatColor.YELLOW + "Click to flip order")));
        inv.setItem(49, item(Material.PAPER, ChatColor.WHITE + "" + st.lines.size() + " lines",
                List.of(ChatColor.GRAY + "Hover a line to read it in full", ChatColor.GRAY + "Click a line to print it to chat")));
        inv.setItem(51, item(Material.LIME_DYE, ChatColor.GREEN + "Refresh", List.of(ChatColor.GRAY + "Re-read latest.log")));
        inv.setItem(53, item(Material.ARROW, st.page < pages - 1 ? ChatColor.YELLOW + "Next \u2192" : ChatColor.DARK_GRAY + "Next", null));
        for (int i = 45; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, item(Material.BLACK_STAINED_GLASS_PANE, " ", null));

        p.openInventory(inv);
    }

    private ItemStack lineItem(String raw) {
        String[] parts = LogReader.split(raw);
        String level = parts[1];
        Material mat;
        ChatColor col;
        if (LogFilter.ERRORS.matches(raw)) { mat = Material.RED_DYE; col = ChatColor.RED; }
        else if (level.equals("WARN")) { mat = Material.ORANGE_DYE; col = ChatColor.GOLD; }
        else if (LogFilter.CHAT.matches(raw)) { mat = Material.LIGHT_BLUE_DYE; col = ChatColor.AQUA; }
        else if (LogFilter.JOINS.matches(raw)) { mat = Material.LIME_DYE; col = ChatColor.GREEN; }
        else if (LogFilter.COMMANDS.matches(raw)) { mat = Material.PURPLE_DYE; col = ChatColor.LIGHT_PURPLE; }
        else { mat = Material.WHITE_DYE; col = ChatColor.WHITE; }

        String msg = ChatColor.stripColor(parts[2]);
        String head = msg.length() > 40 ? msg.substring(0, 40) + "\u2026" : msg;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + parts[0] + "  " + col + level);
        for (String chunk : wrap(msg, 50)) lore.add(ChatColor.GRAY + chunk);
        return item(mat, col + head, lore);
    }

    private static List<String> wrap(String s, int width) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < s.length() && out.size() < 12) {
            out.add(s.substring(i, Math.min(s.length(), i + width)));
            i += width;
        }
        return out;
    }

    public void handleClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Holder h = (Holder) e.getInventory().getHolder();
        State st = state(p);
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (slot < 8) {
            LogFilter[] fs = LogFilter.values();
            if (slot < fs.length) { st.filter = fs[slot]; st.page = 0; open(p); }
            return;
        }
        if (slot == 8) {
            if (e.isRightClick()) { st.query = ""; st.page = 0; open(p); return; }
            plugin.chatInput().ask(p, "Type what to search for in the logs:", text -> { st.query = text; st.page = 0; open(p); });
            return;
        }
        if (slot >= 9 && slot < 45) {
            String raw = h.slotLines.get(slot - 9);
            if (raw != null) p.sendMessage(ChatColor.DARK_GRAY + "[log] " + ChatColor.RESET + raw);
            return;
        }
        switch (slot) {
            case 45 -> { if (st.page > 0) st.page--; open(p); }
            case 47 -> { st.newestFirst = !st.newestFirst; st.page = 0; open(p); }
            case 51 -> open(p);
            case 53 -> { st.page++; open(p); }
            default -> {}
        }
    }

    public static boolean owns(InventoryClickEvent e) {
        return e.getInventory().getHolder() instanceof Holder;
    }

    static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(meta);
        return it;
    }
}
