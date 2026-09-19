package com.pallarium.endgame.service;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.data.PlayerProfile;
import com.pallarium.endgame.skill.Perk;
import com.pallarium.endgame.skill.Skill;
import com.pallarium.endgame.skill.XpTable;
import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Single funnel for awarding XP. Handles the action bar readout, level up
 * titles, sounds and the global broadcast on milestone levels.
 */
public class XpService {

    private final EndgamePlugin plugin;

    public XpService(EndgamePlugin plugin) {
        this.plugin = plugin;
    }

    public void award(Player player, Skill skill, int amount) {
        if (amount <= 0 || !player.isOnline()) {
            return;
        }
        PlayerProfile profile = plugin.store().cached(player.getUniqueId());
        if (profile == null) {
            return;
        }

        int finalAmount = amount;
        double xpBonus = plugin.perks().sum(profile, skill,
                com.pallarium.endgame.skill.Effect.XP_BOOST);
        if (xpBonus > 0) {
            finalAmount = (int) Math.round(finalAmount * (1 + xpBonus / 100.0));
        }
        finalAmount = Math.max(1, (int) Math.round(finalAmount * plugin.settings().xpMultiplier()));

        int before = profile.level(skill);
        int gained = profile.addXp(skill, finalAmount);

        if (gained > 0) {
            double pointChance = plugin.perks().sum(profile, skill,
                    com.pallarium.endgame.skill.Effect.POINT_BONUS);
            for (int i = 0; i < gained; i++) {
                if (plugin.perks().roll(pointChance)) {
                    profile.grantPoint(skill, 1);
                    player.sendMessage(net.kyori.adventure.text.Component
                            .text("  Bonus skill point!  ", Icon.GOOD)
                            .append(net.kyori.adventure.text.Component
                                    .text(skill.display(), skill.color()))
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            announceLevel(player, profile, skill, before + gained);
        } else if (profile.actionBarEnabled()) {
            player.sendActionBar(actionBar(profile, skill, finalAmount));
        }
    }

    private Component actionBar(PlayerProfile profile, Skill skill, int gained) {
        int level = profile.level(skill);
        int need = XpTable.toNext(level);
        String right = need <= 0
                ? "MAX"
                : profile.xp(skill) + " / " + need;

        return Component.text(skill.display() + " ", skill.color())
                .append(Component.text("Lv " + level + "  ", Icon.TEXT))
                .append(Icon.bar(profile.progress(skill), 12, skill.color()))
                .append(Component.text("  " + right + "  ", Icon.DIM))
                .append(Component.text("+" + gained, Icon.GOOD))
                .decoration(TextDecoration.ITALIC, false);
    }

    private void announceLevel(Player player, PlayerProfile profile, Skill skill, int newLevel) {
        if (profile.levelSoundEnabled()) {
            Fx.levelUp(player);
        }

        Component main = Component.text(skill.display(), skill.color())
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false);
        Component sub = Component.text("Level " + newLevel, Icon.TEXT)
                .append(Component.text("   +" + XpTable.pointsForLevel(newLevel) + " skill point",
                        Icon.GOOD))
                .decoration(TextDecoration.ITALIC, false);

        player.showTitle(Title.title(main, sub, Title.Times.times(
                Duration.ofMillis(180), Duration.ofMillis(1100), Duration.ofMillis(420))));

        player.sendMessage(Component.text("  LEVEL UP  ", skill.color())
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(skill.display() + " is now level " + newLevel + ".", Icon.TEXT)
                        .decoration(TextDecoration.BOLD, false))
                .decoration(TextDecoration.ITALIC, false));

        boolean milestone = newLevel % 25 == 0 || newLevel == XpTable.MAX_LEVEL;
        if (milestone) {
            Fx.milestone(player);
            if (plugin.settings().broadcastMilestones()) {
                Component msg = Component.text("[ENDGAME] ", Icon.ACCENT)
                        .append(Component.text(player.getName(), skill.color()))
                        .append(Component.text(" reached ", Icon.TEXT))
                        .append(Component.text(skill.display() + " " + newLevel, skill.color()))
                        .decoration(TextDecoration.ITALIC, false);
                plugin.getServer().sendMessage(msg);
            }
        }
    }
}
