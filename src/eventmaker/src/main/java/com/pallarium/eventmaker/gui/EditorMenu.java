package com.pallarium.eventmaker.gui;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import com.pallarium.eventmaker.model.TriggerType;
import com.pallarium.eventmaker.util.Items;
import com.pallarium.eventmaker.util.Text;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class EditorMenu extends Menu {
    private final GameEvent event;
    /** false while a brand-new event hasn't been saved yet - nothing hits the store until Save/Start. */
    private boolean persisted;

    private static final int NAME = 4, TYPE = 10, TRIGGER = 12, INTERVAL = 14, DURATION = 16,
            LOCATION = 28, RADIUS = 30, REWARDS = 32, ANNOUNCE = 34,
            ENABLED = 38, DELETE = 40, START = 42, SAVE = 49, BACK = 45;

    public EditorMenu(EventMakerPlugin plugin, Player player, GameEvent event, boolean creating) {
        super(plugin, player);
        this.event = event;
        this.persisted = !creating;
    }

    @Override
    protected String title() {
        return "&0&lEdit: &5" + Text.strip(event.name);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void draw() {
        inv.setItem(NAME, Items.of(Material.NAME_TAG, "&f&lName: &d" + event.name,
                "&7Click, then type the new", "&7name in chat."));
        inv.setItem(TYPE, Items.of(event.type.icon, "&f&lType: &d" + event.type.label,
                "&7" + event.type.line1, "&7" + event.type.line2, "", "&eClick to change"));
        inv.setItem(TRIGGER, Items.of(event.trigger == TriggerType.MANUAL ? Material.LEVER : Material.REPEATER,
                "&f&lTrigger: &d" + event.trigger.label,
                "&7" + event.trigger.description, "", "&eClick to switch"));
        inv.setItem(INTERVAL, Items.of(Material.CLOCK, "&f&lEvery: &d" + event.intervalMinutes + " min",
                "&7Only used for Repeating.", "",
                "&aLeft &7+5  &cRight &7-5", "&aShift+Left &7+30  &cShift+Right &7-30"));
        inv.setItem(DURATION, Items.of(Material.COMPASS, "&f&lDuration: &d" + Text.time(event.durationSeconds),
                "&7How long the event runs.", "",
                "&aLeft &7+30s  &cRight &7-30s", "&aShift+Left &7+5m  &cShift+Right &7-5m"));
        String loc = event.location == null ? "&7Not set &8(uses your position)" :
                "&f" + event.location.getWorld().getName() + " " + event.location.getBlockX() + ", "
                        + event.location.getBlockY() + ", " + event.location.getBlockZ();
        inv.setItem(LOCATION, Items.of(Material.ENDER_PEARL, "&f&lLocation",
                loc, "", "&aLeft &7set to where you stand", "&cRight &7clear"));
        inv.setItem(RADIUS, Items.of(Material.SLIME_BALL, "&f&lRadius: &d" + event.radius,
                "&7Spread / zone size in blocks.", "",
                "&aLeft &7+5  &cRight &7-5"));
        inv.setItem(REWARDS, Items.of(Material.CHEST, "&f&lRewards: &d" + event.rewards.size() + " item(s)",
                "&7Opens a chest. Drop items in,", "&7close it, they're saved.", "", "&eClick to edit"));
        inv.setItem(ANNOUNCE, Items.toggle(event.announce, "Announce",
                "&7Broadcast + title + sound", "&7when the event starts."));
        inv.setItem(ENABLED, Items.toggle(event.enabled, "Enabled",
                "&7Disabled events never", "&7auto-run."));
        inv.setItem(DELETE, Items.of(Material.TNT, "&c&lDelete", "&7Shift+Click to delete", "&7this event for good."));
        boolean running = plugin.engine().isRunning(event);
        inv.setItem(START, running
                ? Items.of(Material.RED_CONCRETE, "&c&lStop Now", "&7This event is running.")
                : Items.of(Material.LIME_CONCRETE, "&a&lStart Now", "&7Saves and starts immediately."));
        inv.setItem(SAVE, Items.of(Material.WRITABLE_BOOK, "&a&lSave & Close",
                persisted ? "" : "&7Adds this event to the list."));
        inv.setItem(BACK, persisted
                ? Items.of(Material.ARROW, "&7Back to list", "&8Also saves.")
                : Items.of(Material.ARROW, "&cDiscard", "&8Leaves without creating", "&8this event."));
    }

    private void save() {
        persisted = true;
        plugin.store().put(event);
        plugin.store().save();
    }

    /** Persist only if this event already exists in the list; a fresh one stays in memory. */
    private void saveIfPersisted() {
        if (persisted) save();
    }

    private void tick() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
    }

    @Override
    public void click(InventoryClickEvent e) {
        int slot = e.getRawSlot();
        ClickType c = e.getClick();
        boolean right = c == ClickType.RIGHT || c == ClickType.SHIFT_RIGHT;
        boolean shift = c.isShiftClick();
        switch (slot) {
            case NAME:
                saveIfPersisted();
                player.closeInventory();
                final boolean wasPersisted = persisted;
                plugin.chatInput().ask(player, "&dType the new event name in chat &8(or 'cancel')", text -> {
                    event.name = Text.color(text);
                    if (wasPersisted) save();
                    new EditorMenu(plugin, player, event, !wasPersisted).open();
                });
                return;
            case TYPE:
                saveIfPersisted();
                new TypePickerMenu(plugin, player, event, !persisted).open();
                return;
            case TRIGGER:
                event.trigger = event.trigger.next();
                break;
            case INTERVAL:
                event.intervalMinutes = Math.max(1, event.intervalMinutes + (shift ? 30 : 5) * (right ? -1 : 1));
                break;
            case DURATION:
                event.durationSeconds = Math.max(15, event.durationSeconds + (shift ? 300 : 30) * (right ? -1 : 1));
                break;
            case LOCATION:
                event.location = right ? null : player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
                break;
            case RADIUS:
                event.radius = Math.max(3, Math.min(200, event.radius + (shift ? 25 : 5) * (right ? -1 : 1)));
                break;
            case REWARDS:
                saveIfPersisted();
                new RewardsMenu(plugin, player, event, !persisted).open();
                return;
            case ANNOUNCE:
                event.announce = !event.announce;
                break;
            case ENABLED:
                event.enabled = !event.enabled;
                break;
            case DELETE:
                if (!shift) return;
                if (!persisted) { new MainMenu(plugin, player).open(); return; }
                plugin.engine().stop(event);
                plugin.store().remove(event.id);
                plugin.store().save();
                plugin.msg(player, "&cDeleted &f" + event.name);
                new MainMenu(plugin, player).open();
                return;
            case START:
                save();
                if (plugin.engine().isRunning(event)) plugin.engine().stop(event);
                else plugin.engine().start(event);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                break;
            case SAVE:
                save();
                player.closeInventory();
                plugin.msg(player, "&aSaved &d" + event.name);
                return;
            case BACK:
                saveIfPersisted();
                new MainMenu(plugin, player).open();
                return;
            default:
                return;
        }
        tick();
        saveIfPersisted();
        refresh();
    }
}
