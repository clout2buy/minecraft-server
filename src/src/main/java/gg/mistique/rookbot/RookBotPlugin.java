package gg.mistique.rookbot;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RookBotPlugin extends JavaPlugin {
    private final Map<UUID, Bot> bots = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("rook").setExecutor(new RookCommand(this));
        getServer().getPluginManager().registerEvents(new BotListener(this), this);
        // one shared tick loop drives every bot: 4x/sec is plenty for pathing + mining rhythm
        Bukkit.getScheduler().runTaskTimer(this, () -> bots.values().forEach(Bot::tick), 20L, 5L);
        getLogger().info("RookBot online. /rook spawn");
    }

    @Override
    public void onDisable() { bots.values().forEach(Bot::remove); bots.clear(); }

    public Bot bot(Player owner) { return bots.get(owner.getUniqueId()); }
    public Bot spawn(Player owner) {
        Bot b = bots.remove(owner.getUniqueId()); if (b != null) b.remove();
        b = new Bot(this, owner); bots.put(owner.getUniqueId(), b); return b;
    }
    public void dismiss(Player owner) { Bot b = bots.remove(owner.getUniqueId()); if (b != null) b.remove(); }
    public Map<UUID, Bot> bots() { return bots; }
}
