package com.pallarium.questboard;

import com.pallarium.questboard.data.PlayerData;
import com.pallarium.questboard.model.Quest;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BoardMenu implements Listener {
    private static final String TITLE = ChatColor.BLACK + "" + ChatColor.BOLD + "Daily Quests";
    private final QuestBoardPlugin plugin;

    public BoardMenu(QuestBoardPlugin plugin) {
        this.plugin = plugin;
    }

    private static class Holder implements InventoryHolder {
        final List<String> slots = new ArrayList<>();

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public void open(Player p) {
        PlayerData d = plugin.quests().ensureToday(p);
        List<Quest> quests = plugin.quests().active(p);
        Holder h = new Holder();
        Inventory inv = Bukkit.createInventory(h, 27, TITLE);
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        int[] slots = quests.size() <= 3 ? new int[]{11, 13, 15} : new int[]{10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < quests.size() && i < slots.length; i++) {
            Quest q = quests.get(i);
            int prog = plugin.quests().progress(d, q);
            boolean done = d.completed.contains(q.id);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + q.type.verb + " " + q.amount + " " + q.targetLabel());
            lore.add("");
            lore.add(bar(prog, q.amount) + ChatColor.GRAY + " " + prog + "/" + q.amount);
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Reward:");
            if (q.xp > 0) lore.add(ChatColor.GREEN + "  +" + q.xp + " xp levels");
            for (String c : q.commands) lore.add(ChatColor.AQUA + "  " + describe(c));
            if (done) {
                lore.add("");
                lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "COMPLETE");
            } else if (plugin.quests().rerollsLeft(d) > 0) {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Right-click to reroll " + ChatColor.DARK_GRAY + "(" + plugin.quests().rerollsLeft(d) + " left)");
            }
            ItemStack it = item(done ? Material.LIME_DYE : q.icon(), (done ? ChatColor.GREEN : ChatColor.GOLD) + "" + ChatColor.BOLD + q.name, lore);
            if (done) {
                ItemMeta m = it.getItemMeta();
                m.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                m.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                it.setItemMeta(m);
            }
            inv.setItem(slots[i], it);
            while (h.slots.size() <= slots[i]) h.slots.add(null);
            h.slots.set(slots[i], q.id);
        }

        List<String> info = new ArrayList<>();
        info.add(ChatColor.GRAY + "Done today: " + ChatColor.WHITE + d.completed.size() + "/" + quests.size());
        info.add(ChatColor.GRAY + "Streak: " + ChatColor.LIGHT_PURPLE + d.streak + " day" + (d.streak == 1 ? "" : "s"));
        info.add(ChatColor.GRAY + "Lifetime: " + ChatColor.WHITE + d.totalCompleted + " quests");
        info.add("");
        info.add(ChatColor.DARK_GRAY + "New quests in " + ChatColor.YELLOW + untilMidnight());
        inv.setItem(22, item(Material.CLOCK, ChatColor.AQUA + "" + ChatColor.BOLD + "Your stats", info));
        inv.setItem(26, item(Material.BARRIER, ChatColor.RED + "Close", null));
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Holder h = (Holder) e.getInventory().getHolder();
        int slot = e.getRawSlot();
        if (slot == 26) {
            p.closeInventory();
            return;
        }
        if (slot < 0 || slot >= h.slots.size() || h.slots.get(slot) == null) return;
        if (e.isRightClick()) {
            if (plugin.quests().reroll(p, h.slots.get(slot))) {
                plugin.store().save();
                open(p);
            } else {
                p.sendMessage(ChatColor.RED + "No rerolls left today.");
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    private static String bar(int v, int max) {
        int n = 20;
        int filled = max <= 0 ? n : (int) Math.round(n * Math.min(1.0, v / (double) max));
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GREEN);
        for (int i = 0; i < filled; i++) sb.append('▌');
        sb.append(ChatColor.DARK_GRAY);
        for (int i = filled; i < n; i++) sb.append('▌');
        return sb.toString();
    }

    private static String untilMidnight() {
        Duration left = Duration.between(LocalDateTime.now(), LocalDateTime.now().toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT));
        return left.toHours() + "h " + (left.toMinutes() % 60) + "m";
    }

    private static String describe(String cmd) {
        String[] parts = cmd.trim().split("\\s+");
        if (parts.length >= 3 && parts[0].equalsIgnoreCase("give")) {
            String amt = parts.length >= 4 ? parts[3] + "x " : "";
            return amt + parts[2].replace("minecraft:", "").replace('_', ' ');
        }
        if (parts.length >= 4 && parts[0].equalsIgnoreCase("eco")) return "$" + parts[3];
        return cmd.replace("%player%", "you");
    }

    private static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(meta);
        return it;
    }
}
