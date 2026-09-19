package com.pallarium.endgame.ui;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.data.PlayerProfile;
import com.pallarium.endgame.service.Fx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Per player toggles. Everything here is stored on the profile, nothing global.
 */
public class SettingsMenu extends Menu {

    public SettingsMenu(EndgamePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Component.text("\u25C6 Settings", Icon.TEXT)
                .decoration(TextDecoration.BOLD, true);
    }

    @Override
    protected int rows() {
        return 3;
    }

    @Override
    protected void draw() {
        PlayerProfile profile = plugin.store().getOrCreate(player.getUniqueId(), player.getName());
        border();

        set(11, toggle("XP Action Bar", profile.actionBarEnabled(),
                "Live xp bar above your hotbar", Material.NAME_TAG));
        set(13, toggle("Level Up Effects", profile.levelSoundEnabled(),
                "Sound and particles on level up", Material.NOTE_BLOCK));
        set(15, toggle("Menu Item", profile.menuItemEnabled(),
                "Keep the ENDGAME item in your hotbar", Material.NETHER_STAR));

        set(22, Icon.back("the hub"));
        fillEmpty();
    }

    private ItemStack toggle(String label, boolean on, String blurb, Material icon) {
        return Icon.of(on ? icon : Material.GRAY_DYE)
                .name(label, on ? Icon.GOOD : Icon.DIM)
                .line(blurb, Icon.DIM)
                .blank()
                .line(on ? "ON" : "OFF", on ? Icon.GOOD : Icon.BAD)
                .blank()
                .line("Click to toggle", Icon.ACCENT)
                .glow(on)
                .build();
    }

    @Override
    public void click(int slot, InventoryClickEvent event) {
        PlayerProfile profile = plugin.store().getOrCreate(player.getUniqueId(), player.getName());
        switch (slot) {
            case 11 -> {
                profile.toggleActionBar();
                Fx.click(player);
                plugin.store().saveAsync(profile);
                refresh();
            }
            case 13 -> {
                profile.toggleLevelSound();
                Fx.click(player);
                plugin.store().saveAsync(profile);
                refresh();
            }
            case 15 -> {
                profile.toggleMenuItem();
                Fx.click(player);
                plugin.store().saveAsync(profile);
                plugin.menuItem().sync(player, profile);
                refresh();
            }
            case 22 -> {
                Fx.click(player);
                new MainMenu(plugin, player).open();
            }
            default -> {
            }
        }
    }
}
