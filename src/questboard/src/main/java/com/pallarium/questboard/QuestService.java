package com.pallarium.questboard;

import com.pallarium.questboard.data.PlayerData;
import com.pallarium.questboard.model.Quest;
import com.pallarium.questboard.model.QuestType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class QuestService {
    private final QuestBoardPlugin plugin;
    private final Map<String, Quest> pool = new LinkedHashMap<>();
    private int perDay = 3;
    private int freeRerolls = 1;
    private List<String> dailyCommands = new ArrayList<>();
    private final Map<Integer, List<String>> streakRewards = new LinkedHashMap<>();

    public QuestService(QuestBoardPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        pool.clear();
        streakRewards.clear();
        ConfigurationSection root = plugin.getConfig();
        perDay = Math.max(1, root.getInt("quests-per-day", 3));
        freeRerolls = root.getInt("free-rerolls-per-day", 1);
        dailyCommands = root.getStringList("daily-complete-commands");
        for (Map<?, ?> m : root.getMapList("quests")) {
            try {
                String id = String.valueOf(m.get("id"));
                QuestType type = QuestType.valueOf(String.valueOf(m.get("type")).toUpperCase());
                List<String> cmds = new ArrayList<>();
                Object c = m.get("commands");
                if (c instanceof List) for (Object o : (List<?>) c) cmds.add(String.valueOf(o));
                pool.put(id, new Quest(id, String.valueOf(m.get("name")), type, String.valueOf(m.get("target")),
                        ((Number) m.get("amount")).intValue(), m.get("xp") == null ? 0 : ((Number) m.get("xp")).intValue(), cmds));
            } catch (Exception ex) {
                plugin.getLogger().warning("Bad quest entry: " + m + " (" + ex.getMessage() + ")");
            }
        }
        ConfigurationSection s = root.getConfigurationSection("streak-rewards");
        if (s != null) for (String k : s.getKeys(false)) {
            try {
                streakRewards.put(Integer.parseInt(k), s.getStringList(k));
            } catch (NumberFormatException ignored) {
            }
        }
        plugin.getLogger().info("Loaded " + pool.size() + " quests, " + perDay + " per day.");
    }

    public String today() {
        return LocalDate.now().toString();
    }

    public int perDay() {
        return perDay;
    }

    public int rerollsLeft(PlayerData d) {
        return Math.max(0, freeRerolls - d.rerollsUsed);
    }

    public Quest quest(String id) {
        return pool.get(id);
    }

    /** ensureToday rolls a fresh quest set if the stored day is stale. */
    public PlayerData ensureToday(Player p) {
        PlayerData d = plugin.store().get(p.getUniqueId());
        String today = today();
        if (!today.equals(d.day)) {
            if (!d.lastFullDay.isEmpty() && !d.lastFullDay.equals(LocalDate.now().minusDays(1).toString()) && !d.lastFullDay.equals(today)) {
                d.streak = 0;
            }
            d.day = today;
            d.questIds = roll(p, today, Collections.emptyList());
            d.progress.clear();
            d.completed.clear();
            d.dailyClaimed = false;
            d.rerollsUsed = 0;
        }
        return d;
    }

    private List<String> roll(Player p, String day, List<String> exclude) {
        List<String> ids = new ArrayList<>(pool.keySet());
        ids.removeAll(exclude);
        Collections.shuffle(ids, new Random((day + p.getUniqueId()).hashCode() + exclude.size() * 7919L));
        return new ArrayList<>(ids.subList(0, Math.min(perDay, ids.size())));
    }

    public boolean reroll(Player p, String questId) {
        PlayerData d = ensureToday(p);
        if (rerollsLeft(d) <= 0 || !d.questIds.contains(questId) || d.completed.contains(questId)) return false;
        List<String> exclude = new ArrayList<>(d.questIds);
        List<String> ids = new ArrayList<>(pool.keySet());
        ids.removeAll(exclude);
        if (ids.isEmpty()) return false;
        String pick = ids.get(new Random().nextInt(ids.size()));
        d.questIds.set(d.questIds.indexOf(questId), pick);
        d.progress.remove(questId);
        d.rerollsUsed++;
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.2f);
        return true;
    }

    public List<Quest> active(Player p) {
        PlayerData d = ensureToday(p);
        List<Quest> out = new ArrayList<>();
        for (String id : d.questIds) {
            Quest q = pool.get(id);
            if (q != null) out.add(q);
        }
        return out;
    }

    public int progress(PlayerData d, Quest q) {
        return d.progress.getOrDefault(q.id, 0);
    }

    /** progress adds to every active quest of the given type that matches, completing when the target is reached. */
    public void progress(Player p, QuestType type, java.util.function.Predicate<Quest> matches, int amount) {
        if (!p.hasPermission("questboard.use")) return;
        PlayerData d = ensureToday(p);
        for (String id : d.questIds) {
            Quest q = pool.get(id);
            if (q == null || q.type != type || d.completed.contains(id) || !matches.test(q)) continue;
            int now = Math.min(q.amount, d.progress.getOrDefault(id, 0) + amount);
            d.progress.put(id, now);
            if (now >= q.amount) complete(p, d, q);
            else if (type != QuestType.WALK && (now % Math.max(1, q.amount / 4) == 0)) {
                p.sendActionBar(net.kyori.adventure.text.Component.text(ChatColor.YELLOW + q.name + " " + ChatColor.GRAY + now + "/" + q.amount));
            }
        }
    }

    private void complete(Player p, PlayerData d, Quest q) {
        d.completed.add(q.id);
        d.totalCompleted++;
        p.sendMessage(ChatColor.GREEN + "✔ Quest complete: " + ChatColor.GOLD + q.name);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        if (q.xp > 0) p.giveExpLevels(q.xp);
        run(p, q.commands);
        if (!d.dailyClaimed && d.completed.size() >= d.questIds.size()) {
            d.dailyClaimed = true;
            if (!d.lastFullDay.equals(today())) {
                boolean consecutive = d.lastFullDay.equals(LocalDate.now().minusDays(1).toString());
                d.streak = consecutive ? d.streak + 1 : 1;
                d.lastFullDay = today();
            }
            Bukkit.broadcastMessage(ChatColor.GOLD + "★ " + ChatColor.AQUA + p.getName() + ChatColor.GOLD + " finished all their daily quests! " + ChatColor.GRAY + "(streak " + d.streak + ")");
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            run(p, dailyCommands);
            List<String> bonus = streakRewards.get(d.streak);
            if (bonus != null) {
                p.sendMessage(ChatColor.LIGHT_PURPLE + "Streak bonus! " + ChatColor.GRAY + d.streak + " days in a row.");
                run(p, bonus);
            }
        }
        plugin.store().save();
    }

    private void run(Player p, List<String> cmds) {
        for (String c : cmds) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c.replace("%player%", p.getName()));
    }
}
