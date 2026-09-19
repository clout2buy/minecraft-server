package com.pallarium.endgame.ui;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.data.PlayerProfile;
import com.pallarium.endgame.service.Fx;
import com.pallarium.endgame.skill.Perk;
import com.pallarium.endgame.skill.Skill;
import com.pallarium.endgame.skill.XpTable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * The hub.
 *
 * Row 1 is your identity strip, row 2 and 3 hold the seven skills in a
 * centered block, row 5 is the action bar. Everything else is flat black so
 * the heads are the only thing with colour in the window.
 */
public class MainMenu extends Menu {

    /** Four skills on the upper shelf, three centered underneath. */
    private static final int[] SKILL_SLOTS = {19, 20, 21, 22, 23, 24, 25};

    private static final int PROFILE_SLOT = 4;
    private static final int PERKS = 38;
    private static final int LEADERBOARD = 39;
    private static final int SETTINGS = 40;
    private static final int ADMIN = 41;
    private static final int CLOSE = 42;

    public MainMenu(EndgamePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Component.text("\u25C6 ENDGAME", Icon.NEON)
                .decoration(TextDecoration.BOLD, true);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void draw() {
        PlayerProfile profile = plugin.store().getOrCreate(player.getUniqueId(), player.getName());

        set(PROFILE_SLOT, profileHead(profile));

        Skill[] skills = Skill.values();
        for (int i = 0; i < skills.length && i < SKILL_SLOTS.length; i++) {
            set(SKILL_SLOTS[i], skillIcon(profile, skills[i]));
        }

        // thin divider above the action bar
        for (int i = 27; i < 36; i++) {
            set(i, Icon.edge());
        }

        set(PERKS, perksButton(profile));
        set(LEADERBOARD, leaderboardButton());
        set(SETTINGS, settingsButton());
        if (player.hasPermission("endgame.admin")) {
            set(ADMIN, adminButton());
        }
        set(CLOSE, Icon.close());

        fillEmpty();
    }

    private ItemStack skillIcon(PlayerProfile profile, Skill skill) {
        int level = profile.level(skill);
        int need = XpTable.toNext(level);
        int points = profile.points(skill);

        Icon icon = Icon.of(skill.icon(), Math.max(1, Math.min(64, level)))
                .name(skill.display(), skill.color())
                .line(Component.text("Level ", Icon.DIM)
                        .append(Component.text(level + " / " + XpTable.MAX_LEVEL, skill.color())))
                .line(Icon.bar(profile.progress(skill), 16, skill.color()));

        if (need > 0) {
            icon.line(Component.text(Icon.compact(profile.xp(skill)) + " / "
                    + Icon.compact(need) + " xp", Icon.DIM));
        } else {
            icon.line("MASTERED", Icon.WARN);
        }

        icon.blank().line(skill.blurb(), Icon.DIM).blank();

        if (points > 0) {
            icon.line(Component.text("\u25C6 " + points + " point"
                    + (points == 1 ? "" : "s") + " to spend", Icon.GOOD));
        }
        icon.line("Click to open this tree", Icon.ACCENT);

        return icon.glow(points > 0).build();
    }

    private ItemStack profileHead(PlayerProfile profile) {
        int unspent = 0;
        int maxed = 0;
        for (Skill s : Skill.values()) {
            unspent += profile.points(s);
            if (profile.level(s) >= XpTable.MAX_LEVEL) {
                maxed++;
            }
        }
        int perkRanks = 0;
        for (Perk p : Perk.values()) {
            perkRanks += profile.rank(p);
        }

        int power = profile.power();
        int maxPower = Skill.values().length * XpTable.MAX_LEVEL;

        ItemStack built = Icon.of(Material.PLAYER_HEAD)
                .name(player.getName(), Icon.NEON)
                .line(Component.text("Power ", Icon.DIM)
                        .append(Component.text(power + " / " + maxPower, Icon.NEON)))
                .line(Icon.bar((double) power / maxPower, 16, Icon.NEON))
                .blank()
                .line(Component.text("Perk ranks   ", Icon.DIM)
                        .append(Component.text(String.valueOf(perkRanks), Icon.ACCENT)))
                .line(Component.text("Mastered     ", Icon.DIM)
                        .append(Component.text(maxed + " / " + Skill.values().length, Icon.WARN)))
                .line(Component.text("Unspent      ", Icon.DIM)
                        .append(Component.text(String.valueOf(unspent),
                                unspent > 0 ? Icon.GOOD : Icon.DIM)))
                .build();

        if (built.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(player);
            built.setItemMeta(skull);
        }
        return built;
    }

    private ItemStack perksButton(PlayerProfile profile) {
        int unspent = 0;
        for (Skill s : Skill.values()) {
            unspent += profile.points(s);
        }
        return Icon.of(Material.ENCHANTED_BOOK)
                .name("Perk Tree", Icon.ACCENT)
                .line("Walk every tree top to bottom", Icon.DIM)
                .blank()
                .line(Component.text(unspent + " point" + (unspent == 1 ? "" : "s") + " ready",
                        unspent > 0 ? Icon.GOOD : Icon.DIM))
                .blank()
                .line("Click to open", Icon.ACCENT)
                .glow(unspent > 0)
                .build();
    }

    private ItemStack leaderboardButton() {
        return Icon.of(Material.GOLDEN_HELMET)
                .name("Leaderboard", Icon.WARN)
                .line("Top players ranked by Power", Icon.DIM)
                .blank()
                .line("Click to open", Icon.ACCENT)
                .build();
    }

    private ItemStack settingsButton() {
        return Icon.of(Material.COMPARATOR)
                .name("Settings", Icon.TEXT)
                .line("Action bar, sounds, menu item", Icon.DIM)
                .blank()
                .line("Click to open", Icon.ACCENT)
                .build();
    }

    private ItemStack adminButton() {
        int running = plugin.events().active().size();
        return Icon.of(Material.COMMAND_BLOCK)
                .name("Admin Console", Icon.BAD)
                .line("Spawn events, spawn elites, purge", Icon.DIM)
                .blank()
                .line(Component.text(running + " event" + (running == 1 ? "" : "s") + " running",
                        running > 0 ? Icon.GOOD : Icon.DIM))
                .blank()
                .line("Click to open", Icon.ACCENT)
                .glow(running > 0)
                .build();
    }

    @Override
    public void click(int slot, InventoryClickEvent event) {
        for (int i = 0; i < SKILL_SLOTS.length; i++) {
            if (SKILL_SLOTS[i] == slot && i < Skill.values().length) {
                Fx.click(player);
                new PerkTreeMenu(plugin, player, Skill.values()[i], 0).open();
                return;
            }
        }
        switch (slot) {
            case PERKS -> {
                Fx.click(player);
                new PerkTreeMenu(plugin, player).open();
            }
            case LEADERBOARD -> {
                Fx.click(player);
                new LeaderboardMenu(plugin, player).open();
            }
            case SETTINGS -> {
                Fx.click(player);
                new SettingsMenu(plugin, player).open();
            }
            case ADMIN -> {
                if (!player.hasPermission("endgame.admin")) {
                    return;
                }
                Fx.click(player);
                new AdminMenu(plugin, player).open();
            }
            case CLOSE -> {
                Fx.click(player);
                player.closeInventory();
            }
            default -> {
            }
        }
    }
}
