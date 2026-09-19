package com.pallarium.endgame.ui;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.data.ProfileStore;
import com.pallarium.endgame.service.Fx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/**
 * Top ten by Power. Disk scan runs async, the menu paints a loading state first.
 */
public class LeaderboardMenu extends Menu {

    private static final int[] PODIUM = {12, 13, 14};
    private static final int[] REST = {19, 20, 21, 22, 23, 24, 25};

    private List<ProfileStore.Entry> entries;
    private boolean loading = true;

    public LeaderboardMenu(EndgamePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Component.text("\u25C6 Leaderboard", Icon.WARN)
                .decoration(TextDecoration.BOLD, true);
    }

    @Override
    protected int rows() {
        return 4;
    }

    @Override
    public void open() {
        super.open();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ProfileStore.Entry> result = plugin.store().topByPower(10);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                entries = result;
                loading = false;
                if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() == this) {
                    refresh();
                }
            });
        });
    }

    @Override
    protected void draw() {
        border();

        if (loading || entries == null) {
            set(13, Icon.of(Material.CLOCK)
                    .name("Reading records", Icon.DIM)
                    .line("One moment", Icon.DIM)
                    .build());
            set(31, Icon.back("the hub"));
            fillEmpty();
            return;
        }

        if (entries.isEmpty()) {
            set(13, Icon.of(Material.PAPER)
                    .name("No records yet", Icon.DIM)
                    .line("Go break something", Icon.DIM)
                    .build());
        }

        for (int i = 0; i < entries.size(); i++) {
            int slot;
            if (i < 3) {
                slot = PODIUM[i];
            } else if (i - 3 < REST.length) {
                slot = REST[i - 3];
            } else {
                break;
            }
            set(slot, entryIcon(i + 1, entries.get(i)));
        }

        set(31, Icon.back("the hub"));
        fillEmpty();
    }

    private ItemStack entryIcon(int place, ProfileStore.Entry entry) {
        TextColor color = switch (place) {
            case 1 -> Icon.WARN;
            case 2 -> Icon.TEXT;
            case 3 -> TextColor.fromHexString("#D97706");
            default -> Icon.DIM;
        };
        String medal = switch (place) {
            case 1 -> "\u2605 ";
            case 2 -> "\u2606 ";
            case 3 -> "\u2606 ";
            default -> "";
        };

        boolean self = entry.name().equalsIgnoreCase(player.getName());

        Icon icon = Icon.of(Material.PLAYER_HEAD, Math.max(1, Math.min(64, place)))
                .name(medal + "#" + place + "  " + entry.name(), self ? Icon.NEON : color)
                .line(Component.text("Power  ", Icon.TEXT)
                        .append(Component.text(String.valueOf(entry.power()), Icon.NEON)))
                .line(Icon.bar(entry.power() / 700.0, 20, self ? Icon.NEON : color));

        if (self) {
            icon.blank().line("This is you", Icon.GOOD);
        }

        ItemStack built = icon.glow(self).build();
        if (built.getItemMeta() instanceof SkullMeta skull) {
            OfflinePlayer off = plugin.getServer().getOfflinePlayer(entry.name());
            skull.setOwningPlayer(off);
            built.setItemMeta(skull);
        }
        return built;
    }

    @Override
    public void click(int slot, InventoryClickEvent event) {
        if (slot == 31) {
            Fx.click(player);
            new MainMenu(plugin, player).open();
        }
    }
}
