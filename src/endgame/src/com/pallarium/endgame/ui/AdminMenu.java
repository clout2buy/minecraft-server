package com.pallarium.endgame.ui;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.event.ActiveEvent;
import com.pallarium.endgame.event.WorldEvent;
import com.pallarium.endgame.service.Fx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The operator hub. Live counters up top, then the two spawn decks and the
 * panic buttons. Nothing here touches player progression.
 */
public class AdminMenu extends Menu {

    private static final int STATUS = 4;
    private static final int EVENTS = 20;
    private static final int MOBS = 22;
    private static final int RUNNING = 24;
    private static final int RANDOM_EVENT = 29;
    private static final int RANDOM_MOB = 31;
    private static final int CHAOS = 33;
    private static final int BOSSES = 40;
    private static final int STOP_ALL = 48;
    private static final int PURGE = 50;
    private static final int CLOSE = 49;

    public AdminMenu(EndgamePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Component.text("\u25C6 ENDGAME Admin", Icon.BAD)
                .decoration(TextDecoration.BOLD, true);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void draw() {
        long[] ec = plugin.events().counters();
        long[] mc = plugin.elites().counters();
        int running = plugin.events().active().size();

        set(STATUS, Icon.head(Heads.CROWN)
                .name("Operator Console", Icon.BAD)
                .line("Live server state", Icon.DIM)
                .blank()
                .line(Component.text("Events running  ", Icon.DIM)
                        .append(Component.text(String.valueOf(running),
                                running > 0 ? Icon.GOOD : Icon.DIM)))
                .line(Component.text("Fired           ", Icon.DIM)
                        .append(Component.text(String.valueOf(ec[0]), Icon.NEON)))
                .line(Component.text("Held            ", Icon.DIM)
                        .append(Component.text(String.valueOf(ec[1]), Icon.GOOD)))
                .line(Component.text("Lost            ", Icon.DIM)
                        .append(Component.text(String.valueOf(ec[2]), Icon.BAD)))
                .blank()
                .line(Component.text("Elites spawned  ", Icon.DIM)
                        .append(Component.text(String.valueOf(mc[1]), Icon.ACCENT)))
                .line(Component.text("Ascensions      ", Icon.DIM)
                        .append(Component.text(String.valueOf(mc[3]), Icon.WARN)))
                .glow(running > 0)
                .build());

        for (int i = 9; i < 18; i++) {
            set(i, Icon.edge());
        }

        set(EVENTS, Icon.of(Material.BEACON)
                .name("Event Catalogue", Icon.NEON)
                .line("Every world event, ready to fire", Icon.DIM)
                .blank()
                .line(Component.text(WorldEvent.values().length + " events", Icon.NEON))
                .blank()
                .line("Click to browse", Icon.ACCENT)
                .build());

        set(BOSSES, Icon.of(Material.END_CRYSTAL)
                .name("Boss Registry", Icon.BAD)
                .line("Scripted multi phase boss fights", Icon.DIM)
                .blank()
                .line(Component.text(com.pallarium.endgame.boss.Bosses.all().size()
                        + " bosses authored", Icon.TEXT))
                .line(Component.text(plugin.bosses().active().size() + " fight"
                        + (plugin.bosses().active().size() == 1 ? "" : "s") + " live",
                        plugin.bosses().active().isEmpty() ? Icon.DIM : Icon.GOOD))
                .blank()
                .line("Click to open", Icon.ACCENT)
                .glow(!plugin.bosses().active().isEmpty())
                .build());

        set(MOBS, Icon.of(Material.ZOMBIE_HEAD)
                .name("Elite Spawner", Icon.ACCENT)
                .line("Pick a tier, pick a mob, drop it", Icon.DIM)
                .blank()
                .line("Spawns three blocks ahead of you", Icon.DIM)
                .blank()
                .line("Click to open", Icon.ACCENT)
                .build());

        set(RUNNING, Icon.of(running > 0 ? Material.CLOCK : Material.GRAY_DYE)
                .name("Running Events", running > 0 ? Icon.GOOD : Icon.DIM)
                .line("Watch or kill live events", Icon.DIM)
                .blank()
                .line(Component.text(running + " active",
                        running > 0 ? Icon.GOOD : Icon.DIM))
                .blank()
                .line(running > 0 ? "Click to manage" : "Nothing running", Icon.ACCENT)
                .glow(running > 0)
                .build());

        set(RANDOM_EVENT, Icon.of(Material.ENDER_EYE)
                .name("Random Event", Icon.WARN)
                .line("Rolls any event from the catalogue", Icon.DIM)
                .line("and fires it on your position", Icon.DIM)
                .blank()
                .line("Mutator rolls normally", Icon.DIM)
                .blank()
                .line("Click to roll", Icon.ACCENT)
                .build());

        set(RANDOM_MOB, Icon.of(Material.BONE)
                .name("Random Elite", Icon.WARN)
                .line("Random tier, random mob type", Icon.DIM)
                .blank()
                .line("Shift click for a pack of five", Icon.DIM)
                .blank()
                .line("Click to roll", Icon.ACCENT)
                .build());

        set(CHAOS, Icon.of(Material.TNT)
                .name("Chaos Roll", Icon.BAD)
                .line("One random event plus a random", Icon.DIM)
                .line("elite pack, all at once", Icon.DIM)
                .blank()
                .line("This is as loud as it gets", Icon.BAD)
                .blank()
                .line("Click if you mean it", Icon.ACCENT)
                .build());

        set(STOP_ALL, Icon.of(Material.BARRIER)
                .name("Stop All Events", Icon.BAD)
                .line("Cleans every site and restores blocks", Icon.DIM)
                .blank()
                .line("Click to clear", Icon.ACCENT)
                .build());

        set(PURGE, Icon.of(Material.LAVA_BUCKET)
                .name("Purge Elites", Icon.BAD)
                .line("Removes every elite and stray bar", Icon.DIM)
                .blank()
                .line("Click to purge", Icon.ACCENT)
                .build());

        set(CLOSE, Icon.close());
        fillEmpty();
    }

    @Override
    public void click(int slot, InventoryClickEvent event) {
        if (!player.hasPermission("endgame.admin")) {
            Fx.deny(player);
            player.closeInventory();
            return;
        }
        switch (slot) {
            case EVENTS -> {
                Fx.click(player);
                new EventAdminMenu(plugin, player, 0).open();
            }
            case MOBS -> {
                Fx.click(player);
                new MobAdminMenu(plugin, player).open();
            }
            case BOSSES -> {
                Fx.click(player);
                new BossMenu(plugin, player).open();
            }
            case RUNNING -> {
                Fx.click(player);
                new RunningEventMenu(plugin, player).open();
            }
            case RANDOM_EVENT -> {
                WorldEvent[] all = WorldEvent.values();
                WorldEvent pick = all[ThreadLocalRandom.current().nextInt(all.length)];
                ActiveEvent ev = plugin.events().fire(pick, player.getLocation(), player);
                if (ev == null) {
                    Fx.deny(player);
                    player.sendMessage(Component.text("[ENDGAME] ", Icon.NEON)
                            .append(Component.text("Event cap reached.", Icon.BAD)));
                } else {
                    Fx.purchase(player);
                }
                refresh();
            }
            case RANDOM_MOB -> {
                int count = event.isShiftClick() ? 5 : 1;
                MobAdminMenu.spawnRandom(plugin, player, count);
                Fx.purchase(player);
                refresh();
            }
            case CHAOS -> {
                WorldEvent[] all = WorldEvent.values();
                plugin.events().fire(all[ThreadLocalRandom.current().nextInt(all.length)],
                        player.getLocation(), player);
                MobAdminMenu.spawnRandom(plugin, player, 5);
                Fx.milestone(player);
                refresh();
            }
            case STOP_ALL -> {
                int n = plugin.events().active().size();
                plugin.events().shutdown();
                Fx.click(player);
                player.sendMessage(Component.text("[ENDGAME] ", Icon.NEON)
                        .append(Component.text("Cleared " + n + " events.", Icon.GOOD)));
                refresh();
            }
            case PURGE -> {
                int mobs = 0;
                for (org.bukkit.World w : plugin.getServer().getWorlds()) {
                    for (org.bukkit.entity.Entity e : w.getEntities()) {
                        if (e instanceof org.bukkit.entity.TextDisplay td
                                && td.getPersistentDataContainer().has(plugin.keyBarOwner(),
                                org.bukkit.persistence.PersistentDataType.STRING)) {
                            td.remove();
                        } else if (e instanceof org.bukkit.entity.LivingEntity le
                                && !(e instanceof Player) && plugin.elites().isElite(le)) {
                            le.remove();
                            mobs++;
                        }
                    }
                }
                Fx.click(player);
                player.sendMessage(Component.text("[ENDGAME] ", Icon.NEON)
                        .append(Component.text("Purged " + mobs + " elites.", Icon.GOOD)));
                refresh();
            }
            case CLOSE -> {
                Fx.click(player);
                player.closeInventory();
            }
            default -> {
            }
        }
    }

    static ItemStack tip(String text) {
        return Icon.of(Material.PAPER).name(text, Icon.DIM).build();
    }
}
