package com.pallarium.eventmaker.gui;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.EventType;
import com.pallarium.eventmaker.model.GameEvent;
import com.pallarium.eventmaker.util.Items;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class TypePickerMenu extends Menu {
    private final GameEvent event;
    private final boolean creating;
    private static final int GRID = 45; // 5 rows of types, packed left to right

    public TypePickerMenu(EventMakerPlugin plugin, Player player, GameEvent event, boolean creating) {
        super(plugin, player);
        this.event = event;
        this.creating = creating;
    }

    @Override
    protected String title() {
        return "&0&lPick an event type";
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void draw() {
        EventType[] types = EventType.values();
        for (int i = 0; i < types.length && i < GRID; i++) {
            EventType t = types[i];
            boolean sel = event.type == t;
            inv.setItem(i, Items.of(t.icon, (sel ? "&a&l" : "&d&l") + t.label,
                    "&7" + t.line1, "&7" + t.line2, "",
                    t.needsLocation() ? "&8Uses a set location (or you)" : "&8Happens around every player",
                    "", sel ? "&aSelected" : "&eClick to choose"));
        }
        inv.setItem(49, Items.of(Material.BARRIER, "&cBack"));
    }

    @Override
    public void click(InventoryClickEvent e) {
        int slot = e.getRawSlot();
        if (slot == 49) {
            new EditorMenu(plugin, player, event, creating).open();
            return;
        }
        EventType[] types = EventType.values();
        if (slot >= 0 && slot < GRID && slot < types.length) {
            event.type = types[slot];
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.4f);
            new EditorMenu(plugin, player, event, creating).open();
        }
    }
}
