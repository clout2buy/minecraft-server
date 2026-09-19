package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import com.pallarium.eventmaker.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public abstract class ActiveEvent {
    protected final EventMakerPlugin plugin;
    protected final GameEvent event;
    protected final Random random = new Random();
    protected final Map<UUID, Double> scores = new HashMap<>();
    protected BossBar bar;
    private BukkitTask ticker;
    private int elapsed = 0;
    private boolean finished = false;

    protected ActiveEvent(EventMakerPlugin plugin, GameEvent event) {
        this.plugin = plugin;
        this.event = event;
    }

    public GameEvent event() {
        return event;
    }

    public boolean isFinished() {
        return finished;
    }

    protected abstract void onStart();

    protected abstract void onSecond(int elapsed, int remaining);

    protected abstract void onEnd(boolean forced);

    protected abstract String scoreLabel();

    public final void start() {
        bar = Bukkit.createBossBar(Text.color("&d&l" + event.name + " &7- " + Text.time(event.durationSeconds)),
                BarColor.PURPLE, BarStyle.SEGMENTED_10);
        for (Player p : event.participants()) bar.addPlayer(p);
        if (event.announce) {
            plugin.broadcast("&d&l" + event.name + " &7has started! &8(" + event.type.label + ")");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
                if (plugin.getConfig().getBoolean("titles", true)) {
                    p.sendTitle(Text.color("&d&l" + event.name), Text.color("&7" + event.type.line1), 10, 60, 20);
                }
            }
        }
        onStart();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        if (finished) return;
        elapsed++;
        int remaining = event.durationSeconds - elapsed;
        if (bar != null) {
            for (Player p : event.participants()) if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
            bar.setProgress(Math.max(0, Math.min(1, remaining / (double) event.durationSeconds)));
            bar.setTitle(Text.color("&d&l" + event.name + " &7- " + Text.time(Math.max(0, remaining))));
        }
        onSecond(elapsed, remaining);
        if (remaining <= 0) stop(false);
    }

    public final void stop(boolean forced) {
        if (finished) return;
        finished = true;
        if (ticker != null) ticker.cancel();
        if (bar != null) bar.removeAll();
        onEnd(forced);
        if (event.announce) {
            plugin.broadcast("&d&l" + event.name + " &7has ended" + (forced ? " &8(stopped)" : "") + "&7.");
        }
        plugin.engine().onFinished(this);
    }

    protected void addScore(Player p, double amount) {
        scores.merge(p.getUniqueId(), amount, Double::sum);
    }

    protected List<Map.Entry<UUID, Double>> leaderboard() {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list;
    }

    protected void announceWinnersAndReward() {
        List<Map.Entry<UUID, Double>> board = leaderboard();
        if (board.isEmpty()) {
            plugin.broadcast("&7Nobody scored this time.");
            return;
        }
        plugin.broadcast("&d&lResults &8- &7" + scoreLabel());
        int shown = 0;
        for (Map.Entry<UUID, Double> e : board) {
            if (shown++ >= 3) break;
            Player p = Bukkit.getPlayer(e.getKey());
            String name = p != null ? p.getName() : "?";
            String medal = shown == 1 ? "&6#1" : shown == 2 ? "&f#2" : "&c#3";
            plugin.broadcast(" " + medal + " &f" + name + " &8- &e" + formatScore(e.getValue()));
        }
        Player winner = Bukkit.getPlayer(board.get(0).getKey());
        if (winner != null) giveRewards(winner);
    }

    protected String formatScore(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.1f", v);
    }

    protected void giveRewards(Player p) {
        if (event.rewards.isEmpty()) return;
        for (ItemStack item : event.rewards) {
            Map<Integer, ItemStack> left = p.getInventory().addItem(item.clone());
            for (ItemStack l : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), l);
        }
        plugin.msg(p, "&aYou received the rewards for &d" + event.name + "&a!");
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    protected ItemStack randomReward() {
        if (event.rewards.isEmpty()) return null;
        return event.rewards.get(random.nextInt(event.rewards.size())).clone();
    }
}
