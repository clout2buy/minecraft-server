package com.pallarium.eventmaker.gui;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import com.pallarium.eventmaker.util.Items;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class MainMenu extends Menu {
    private final List<GameEvent> shown = new ArrayList<>();
    private int page = 0;

    public MainMenu(EventMakerPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "&0&lEvent Maker";
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void draw() {
        shown.clear();
        List<GameEvent> all = new ArrayList<>(plugin.store().all());
        int perPage = 36;
        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < all.size(); i++) {
            GameEvent e = all.get(start + i);
            shown.add(e);
            boolean running = plugin.engine().isRunning(e);
            List<String> lore = new ArrayList<>();
            lore.add("&7Type: &f" + e.type.label);
            lore.add("&7Trigger: &f" + e.trigger.label + (e.trigger.name().equals("INTERVAL") ? " &8(every " + e.intervalMinutes + "m)" : ""));
            lore.add("&7Duration: &f" + com.pallarium.eventmaker.util.Text.time(e.durationSeconds));
            lore.add("&7Rewards: &f" + e.rewards.size() + " item(s)");
            lore.add("&7Status: " + (running ? "&a&lRUNNING" : e.enabled ? "&aReady" : "&cDisabled"));
            lore.add("");
            lore.add(running ? "&cShift+Click &7to stop" : "&aLeft-click &7to start");
            lore.add("&eRight-click &7to edit");
            inv.setItem(i, Items.of(running ? Material.LIME_CONCRETE : e.type.icon,
                    (running ? "&a&l" : "&d&l") + e.name, lore));
        }
        inv.setItem(45, Items.of(Material.ARROW, "&7Previous page"));
        inv.setItem(53, Items.of(Material.ARROW, "&7Next page"));
        inv.setItem(49, Items.of(Material.NETHER_STAR, "&a&lCreate New Event",
                "&7Opens a step-by-step builder.", "", "&aClick to begin"));
        inv.setItem(47, Items.of(Material.BOOK, "&fHow it works",
                "&7Pick a type, set a trigger,",
                "&7add rewards, press Start.",
                "&7Repeating events fire on",
                "&7their own while players",
                "&7are online."));
        int running = plugin.engine().running().size();
        inv.setItem(51, Items.of(Material.CLOCK, "&fActive events: &a" + running,
                running > 0 ? "&7Shift+Click any running event to stop it." : "&7Nothing running right now."));
    }

    @Override
    public void click(InventoryClickEvent e) {
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;
        if (slot == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            new EditorMenu(plugin, player, GameEvent.fresh("New Event"), true).open();
            return;
        }
        if (slot == 45 && page > 0) { page--; refresh(); return; }
        if (slot == 53 && (page + 1) * 36 < plugin.store().all().size()) { page++; refresh(); return; }
        if (slot < shown.size()) {
            GameEvent ev = shown.get(slot);
            if (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT) {
                new EditorMenu(plugin, player, ev, false).open();
            } else if (e.getClick() == ClickType.SHIFT_LEFT) {
                if (plugin.engine().stop(ev)) plugin.msg(player, "&cStopped &f" + ev.name);
                refresh();
            } else if (e.getClick() == ClickType.LEFT) {
                if (plugin.engine().isRunning(ev)) {
                    plugin.msg(player, "&7That event is already running.");
                } else {
                    plugin.engine().start(ev);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                }
                refresh();
            }
        }
    }
}
