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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The perk tree, drawn as a real tree instead of two loose columns.
 *
 * Five tiers flow left to right across the chest. Each tier is a column of two
 * perks, joined to the next tier by arrow heads that light up only once that
 * tier is reachable, so the eye follows the progression path. A tier ruler runs
 * between the two branches showing the level gate for each column, a nine
 * segment XP bar sits under the header, and every skill has its own tab along
 * the bottom so there is no blind prev/next stepping.
 */
public class PerkTreeMenu extends Menu {

    /** Header row. */
    private static final int BACK = 0;
    private static final int BANNER = 4;
    private static final int RESET = 8;

    /** Row 1 is the nine segment XP bar, slots 9 to 17. */
    private static final int BAR_ROW = 9;

    /** Branch rows: upper perks on row 2, lower perks on row 4. */
    private static final int TOP_ROW = 18;
    private static final int RULER_ROW = 27;
    private static final int BOTTOM_ROW = 36;

    /** Tab row. */
    private static final int TAB_ROW = 45;
    private static final int CLOSE = 53;

    private static final int TIERS = 5;

    private Skill skill;
    private final Map<Integer, Perk> slotMap = new HashMap<>();
    private final Map<Integer, Skill> tabMap = new HashMap<>();

    public PerkTreeMenu(EndgamePlugin plugin, Player player) {
        this(plugin, player, Skill.values()[0]);
    }

    public PerkTreeMenu(EndgamePlugin plugin, Player player, Skill skill) {
        super(plugin, player);
        this.skill = skill;
    }

    /** Old call sites passed a page number. Kept so nothing else breaks. */
    public PerkTreeMenu(EndgamePlugin plugin, Player player, Skill skill, int ignoredPage) {
        this(plugin, player, skill);
    }

    @Override
    protected Component title() {
        return Component.text(skill.display(), skill.color())
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text("  Perk Tree", Icon.DIM)
                        .decoration(TextDecoration.BOLD, false));
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void draw() {
        PlayerProfile profile = plugin.store().getOrCreate(player.getUniqueId(), player.getName());
        slotMap.clear();
        tabMap.clear();

        List<Perk> perks = Perk.of(skill);
        int level = profile.level(skill);

        set(BACK, Icon.back("the hub"));
        set(BANNER, banner(profile, perks));
        set(RESET, resetButton(profile));

        drawXpBar(profile);

        // Five tiers across, two perks per tier, arrows between the columns.
        for (int tier = 0; tier < TIERS; tier++) {
            int col = tier * 2;
            int top = perkIndex(tier, true);
            int bottom = perkIndex(tier, false);

            if (top < perks.size()) {
                Perk perk = perks.get(top);
                set(TOP_ROW + col, card(profile, perk, top + 1));
                slotMap.put(TOP_ROW + col, perk);
            }
            if (bottom < perks.size()) {
                Perk perk = perks.get(bottom);
                set(BOTTOM_ROW + col, card(profile, perk, bottom + 1));
                slotMap.put(BOTTOM_ROW + col, perk);
            }

            set(RULER_ROW + col, ruler(perks, tier, level));

            if (tier < TIERS - 1) {
                int nextGate = gateOf(perks, tier + 1);
                boolean open = level >= nextGate;
                set(TOP_ROW + col + 1, branchArrow(open, nextGate));
                set(BOTTOM_ROW + col + 1, branchArrow(open, nextGate));
                set(RULER_ROW + col + 1, spacer(open));
            }
        }

        drawTabs(profile);
        set(CLOSE, Icon.close());

        fillEmpty();
    }

    /** Upper branch takes the even perks, lower branch the odd ones. */
    private int perkIndex(int tier, boolean upper) {
        return tier * 2 + (upper ? 0 : 1);
    }

    private int gateOf(List<Perk> perks, int tier) {
        int idx = tier * 2;
        if (idx >= perks.size()) {
            return Integer.MAX_VALUE;
        }
        return perks.get(idx).unlockLevel();
    }

    /** Nine glass segments under the header showing progress to the next level. */
    private void drawXpBar(PlayerProfile profile) {
        double progress = profile.progress(skill);
        int filled = (int) Math.round(Math.max(0, Math.min(1, progress)) * 9);
        int level = profile.level(skill);

        for (int i = 0; i < 9; i++) {
            boolean lit = i < filled;
            ItemStack seg = Icon.of(lit ? paneFor(skill) : Material.BLACK_STAINED_GLASS_PANE)
                    .plainName(lit ? "\u2588" : " ", lit ? skill.color() : Icon.DIM)
                    .line(Component.text("Level " + level, Icon.DIM)
                            .append(Component.text("  \u2192  ", Icon.DIM))
                            .append(Component.text("Level " + (level + 1), skill.color())))
                    .line(Component.text(Math.round(progress * 100) + "% of the way there",
                            Icon.TEXT))
                    .build();
            set(BAR_ROW + i, seg);
        }
    }

    /** One tab per skill along the bottom, current tab glowing. */
    private void drawTabs(PlayerProfile profile) {
        Skill[] all = Skill.values();
        for (int i = 0; i < all.length && i < 7; i++) {
            Skill s = all[i];
            int slot = TAB_ROW + 1 + i;
            boolean current = s == skill;
            int points = profile.points(s);

            Icon icon = Icon.of(s.icon())
                    .name(s.display(), current ? s.color() : Icon.DIM)
                    .line(Component.text("Level ", Icon.DIM)
                            .append(Component.text(profile.level(s) + " / " + XpTable.MAX_LEVEL,
                                    current ? s.color() : Icon.TEXT)))
                    .line(Icon.bar(profile.progress(s), 12, s.color()));

            if (points > 0) {
                icon.blank().line(Component.text(points + " point" + (points == 1 ? "" : "s")
                        + " waiting", Icon.GOOD));
            }
            icon.blank().line(current ? "Viewing this tree" : "Click to view",
                    current ? Icon.ACCENT : Icon.DIM);

            set(slot, icon.glow(current || points > 0).build());
            tabMap.put(slot, s);
        }
    }

    /** The arrow joining one tier to the next. */
    private ItemStack branchArrow(boolean open, int gate) {
        return Icon.head(open ? Heads.ARROW_RIGHT : Heads.ARROW_RIGHT_DIM)
                .plainName(open ? "\u25B6" : "\u25B7", open ? Icon.NEON : Icon.DIM)
                .line(open ? "Path open" : "Path sealed", open ? Icon.GOOD : Icon.BAD)
                .line(open ? "The next tier is reachable"
                        : "Reach level " + gate + " to continue", Icon.DIM)
                .build();
    }

    private ItemStack spacer(boolean open) {
        return Icon.of(open ? Material.CYAN_STAINED_GLASS_PANE
                        : Material.BLACK_STAINED_GLASS_PANE)
                .plainName(" ", Icon.DIM)
                .build();
    }

    /** Tier marker sitting between the two branches. */
    private ItemStack ruler(List<Perk> perks, int tier, int level) {
        int gate = gateOf(perks, tier);
        boolean reached = level >= gate;
        int owned = 0;
        int possible = 0;
        for (int i = tier * 2; i < tier * 2 + 2 && i < perks.size(); i++) {
            Perk p = perks.get(i);
            owned += plugin.store().getOrCreate(player.getUniqueId(), player.getName()).rank(p);
            possible += p.maxRank();
        }

        Material mat;
        if (possible > 0 && owned >= possible) {
            mat = Material.LIME_STAINED_GLASS_PANE;
        } else if (owned > 0) {
            mat = paneFor(skill);
        } else if (reached) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
        } else {
            mat = Material.RED_STAINED_GLASS_PANE;
        }

        return Icon.of(mat)
                .plainName("Tier " + (tier + 1), reached ? skill.color() : Icon.DIM)
                .line(reached ? "Unlocked at level " + gate
                        : "Locked until level " + gate, reached ? Icon.GOOD : Icon.BAD)
                .line(Component.text("Ranks taken  ", Icon.DIM)
                        .append(Component.text(owned + " / " + possible, Icon.TEXT)))
                .build();
    }

    private ItemStack banner(PlayerProfile profile, List<Perk> perks) {
        int owned = 0;
        int possible = 0;
        for (Perk p : perks) {
            owned += profile.rank(p);
            possible += p.maxRank();
        }
        int level = profile.level(skill);

        return Icon.of(skill.icon())
                .name(skill.display(), skill.color())
                .line(skill.blurb(), Icon.DIM)
                .blank()
                .line(Component.text("Level ", Icon.DIM)
                        .append(Component.text(level + " / " + XpTable.MAX_LEVEL, skill.color())))
                .line(Icon.bar(profile.progress(skill), 18, skill.color()))
                .blank()
                .line(Component.text("Perks unlocked  ", Icon.DIM)
                        .append(Component.text(owned + " / " + possible, Icon.TEXT)))
                .line(Component.text("Points to spend ", Icon.DIM)
                        .append(Component.text(String.valueOf(profile.points(skill)),
                                profile.points(skill) > 0 ? Icon.GOOD : Icon.DIM)))
                .glow(profile.points(skill) > 0)
                .build();
    }

    private ItemStack resetButton(PlayerProfile profile) {
        int spent = 0;
        for (Perk p : Perk.of(skill)) {
            for (int i = 1; i <= profile.rank(p); i++) {
                spent += p.costFor(i);
            }
        }
        return Icon.head(Heads.RESET)
                .name("Refund Perks", spent > 0 ? Icon.WARN : Icon.DIM)
                .line("Clear every " + skill.display() + " perk", Icon.DIM)
                .blank()
                .line(Component.text("Returns ", Icon.DIM)
                        .append(Component.text(spent + " point" + (spent == 1 ? "" : "s"),
                                spent > 0 ? Icon.GOOD : Icon.DIM)))
                .blank()
                .line(spent > 0 ? "Click to refund" : "Nothing to refund",
                        spent > 0 ? Icon.ACCENT : Icon.DIM)
                .build();
    }

    private ItemStack card(PlayerProfile profile, Perk perk, int position) {
        int rank = profile.rank(perk);
        boolean unlocked = profile.level(skill) >= perk.unlockLevel();
        boolean maxed = rank >= perk.maxRank();
        int cost = perk.costFor(rank + 1);
        boolean affordable = profile.points(skill) >= cost;

        Material mat = unlocked ? perk.icon() : Material.GRAY_DYE;

        Icon icon = Icon.of(mat, Math.max(1, rank))
                .name(perk.display(), unlocked ? skill.color() : Icon.DIM)
                .line(perk.flavour(), Icon.DIM)
                .blank()
                .line(Component.text(pips(rank, perk.maxRank()), rankColor(rank, perk.maxRank()))
                        .append(Component.text("  " + rank + " / " + perk.maxRank(), Icon.TEXT)));

        if (rank > 0) {
            icon.line(Component.text("Now  ", Icon.DIM)
                    .append(Component.text(perk.describe(rank), Icon.GOOD)));
        }
        if (unlocked && !maxed) {
            icon.line(Component.text("Next ", Icon.DIM)
                    .append(Component.text(perk.describe(rank + 1), Icon.NEON)));
        }
        if (rank == 0 && unlocked) {
            icon.line(Component.text("At rank 1  ", Icon.DIM)
                    .append(Component.text(perk.describe(1), Icon.NEON)));
        }

        icon.blank();
        if (!unlocked) {
            icon.line("LOCKED", Icon.BAD)
                    .line("Needs " + skill.display() + " level " + perk.unlockLevel(), Icon.DIM);
        } else if (maxed) {
            icon.line("MAXED", Icon.WARN);
        } else {
            icon.line(Component.text(cost + " point" + (cost == 1 ? "" : "s"),
                    affordable ? Icon.GOOD : Icon.BAD));
            icon.line(affordable ? "Click to rank up" : "Not enough points",
                    affordable ? Icon.ACCENT : Icon.DIM);
        }

        return icon.glow(unlocked && !maxed && affordable).build();
    }

    /** Filled and hollow dots read as rank far faster than a solid bar. */
    private String pips(int rank, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(i < rank ? '\u25CF' : '\u25CB');
            if (i < max - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private net.kyori.adventure.text.format.TextColor rankColor(int rank, int max) {
        if (rank >= max) {
            return Icon.WARN;
        }
        if (rank > 0) {
            return Icon.GOOD;
        }
        return Icon.DIM;
    }

    /** Closest stained glass to each skill's accent colour. */
    private static Material paneFor(Skill skill) {
        return switch (skill) {
            case MINING -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case WOODCUTTING -> Material.GREEN_STAINED_GLASS_PANE;
            case EXCAVATION -> Material.BROWN_STAINED_GLASS_PANE;
            case FARMING -> Material.YELLOW_STAINED_GLASS_PANE;
            case COMBAT -> Material.RED_STAINED_GLASS_PANE;
            case ARCHERY -> Material.LIME_STAINED_GLASS_PANE;
            case FISHING -> Material.CYAN_STAINED_GLASS_PANE;
        };
    }

    @Override
    public void click(int slot, InventoryClickEvent event) {
        Skill tab = tabMap.get(slot);
        if (tab != null) {
            if (tab != skill) {
                skill = tab;
                Fx.click(player);
                refresh();
            }
            return;
        }

        switch (slot) {
            case BACK -> {
                Fx.click(player);
                new MainMenu(plugin, player).open();
                return;
            }
            case CLOSE -> {
                Fx.click(player);
                player.closeInventory();
                return;
            }
            case RESET -> {
                PlayerProfile profile =
                        plugin.store().getOrCreate(player.getUniqueId(), player.getName());
                int refunded = profile.resetPerks(skill);
                if (refunded > 0) {
                    Fx.purchase(player);
                    player.sendMessage(Component.text("  Refunded ", Icon.TEXT)
                            .append(Component.text(refunded + " point" + (refunded == 1 ? "" : "s"),
                                    Icon.GOOD))
                            .append(Component.text(" of " + skill.display() + " perks.", Icon.TEXT))
                            .decoration(TextDecoration.ITALIC, false));
                    plugin.store().saveAsync(profile);
                } else {
                    Fx.deny(player);
                }
                refresh();
                return;
            }
            default -> {
            }
        }

        Perk perk = slotMap.get(slot);
        if (perk == null) {
            return;
        }
        PlayerProfile profile = plugin.store().getOrCreate(player.getUniqueId(), player.getName());
        if (profile.buyPerk(perk)) {
            Fx.purchase(player);
            int rank = profile.rank(perk);
            player.sendMessage(Component.text("  " + perk.display() + " ", perk.skill().color())
                    .decoration(TextDecoration.BOLD, true)
                    .append(Component.text("rank " + rank + ".  ", Icon.TEXT)
                            .decoration(TextDecoration.BOLD, false))
                    .append(Component.text(perk.describe(rank), Icon.GOOD)
                            .decoration(TextDecoration.BOLD, false))
                    .decoration(TextDecoration.ITALIC, false));
            plugin.store().saveAsync(profile);
        } else {
            Fx.deny(player);
        }
        refresh();
    }
}
