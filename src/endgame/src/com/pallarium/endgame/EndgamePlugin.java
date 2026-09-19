package com.pallarium.endgame;

import com.pallarium.endgame.command.EndgameCommand;
import com.pallarium.endgame.data.ProfileStore;
import com.pallarium.endgame.listener.ConnectionListener;
import com.pallarium.endgame.listener.MenuListener;
import com.pallarium.endgame.listener.SkillListener;
import com.pallarium.endgame.service.DamageIndicator;
import com.pallarium.endgame.service.MenuItemService;
import com.pallarium.endgame.service.PerkEngine;
import com.pallarium.endgame.service.PlacedBlockTracker;
import com.pallarium.endgame.service.Settings;
import com.pallarium.endgame.service.XpService;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EndgamePlugin extends JavaPlugin {

    private Settings settings;
    private ProfileStore store;
    private XpService xp;
    private MenuItemService menuItem;
    private PlacedBlockTracker placed;
    private DamageIndicator indicators;
    private PerkEngine perks;
    private com.pallarium.endgame.mob.EliteService elites;
    private com.pallarium.endgame.event.EventService events;
    private com.pallarium.endgame.boss.BossService bosses;
    private com.pallarium.endgame.mob.HealthBars bars;
    private org.bukkit.NamespacedKey keyBarOwner;
    private org.bukkit.NamespacedKey keyBarSeen;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        settings = new Settings(this);
        store = new ProfileStore(this);
        xp = new XpService(this);
        menuItem = new MenuItemService(this);
        placed = new PlacedBlockTracker();
        indicators = new DamageIndicator(this);
        perks = new PerkEngine(this);
        elites = new com.pallarium.endgame.mob.EliteService(this);
        events = new com.pallarium.endgame.event.EventService(this);
        bosses = new com.pallarium.endgame.boss.BossService(this);
        keyBarOwner = new org.bukkit.NamespacedKey(this, "bar_owner");
        keyBarSeen = new org.bukkit.NamespacedKey(this, "bar_seen");
        bars = new com.pallarium.endgame.mob.HealthBars(this);
        indicators.purge();

        getServer().getPluginManager().registerEvents(new SkillListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.pallarium.endgame.listener.EliteListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.pallarium.endgame.listener.EventListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.pallarium.endgame.listener.CombatUIListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.pallarium.endgame.listener.BossListener(this), this);

        PluginCommand command = getCommand("endgame");
        if (command != null) {
            EndgameCommand handler = new EndgameCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        // load anyone already online (plugin reload case)
        for (Player player : getServer().getOnlinePlayers()) {
            store.getOrCreate(player.getUniqueId(), player.getName());
        }

        long interval = settings.autoSaveMinutes() * 60L * 20L;
        getServer().getScheduler().runTaskTimer(this, () -> store.saveAllOnline(),
                interval, interval);

        getLogger().info("ENDGAME online. " + com.pallarium.endgame.skill.Skill.values().length
                + " skills, " + com.pallarium.endgame.skill.Perk.values().length
                + " perks, level cap " + com.pallarium.endgame.skill.XpTable.MAX_LEVEL + ".");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.shutdown();
        }
        if (events != null) {
            events.shutdown();
        }
        if (bosses != null) {
            bosses.shutdown();
        }
        for (org.bukkit.World w : getServer().getWorlds()) {
            for (org.bukkit.entity.TextDisplay td
                    : w.getEntitiesByClass(org.bukkit.entity.TextDisplay.class)) {
                if (keyBarOwner != null && td.getPersistentDataContainer().has(
                        keyBarOwner, org.bukkit.persistence.PersistentDataType.STRING)) {
                    td.remove();
                }
            }
        }
        if (placed != null) {
            placed.clear();
        }
        getLogger().info("ENDGAME saved and stopped.");
    }

    public Settings settings() {
        return settings;
    }

    public ProfileStore store() {
        return store;
    }

    public XpService xp() {
        return xp;
    }

    public MenuItemService menuItem() {
        return menuItem;
    }

    public PlacedBlockTracker placed() {
        return placed;
    }

    public PerkEngine perks() {
        return perks;
    }

    public com.pallarium.endgame.mob.HealthBars bars() {
        return bars;
    }

    public org.bukkit.NamespacedKey keyBarOwner() {
        return keyBarOwner;
    }

    public org.bukkit.NamespacedKey keyBarSeen() {
        return keyBarSeen;
    }

    public com.pallarium.endgame.event.EventService events() {
        return events;
    }

    public com.pallarium.endgame.boss.BossService bosses() {
        return bosses;
    }

    public com.pallarium.endgame.mob.EliteService elites() {
        return elites;
    }

    public DamageIndicator indicators() {
        return indicators;
    }
}
