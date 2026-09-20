package com.pallarium.endgame.boss;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One live boss fight. Owns the mob, the bossbar, the phase state machine, the
 * ability cooldowns, the damage ledger for loot, and the death payout.
 */
public class BossInstance {

    private final EndgamePlugin plugin;
    private final BossDef def;
    private final LivingEntity entity;
    private final Location arenaCenter;
    private final UUID id = UUID.randomUUID();

    private final BossBar bar;
    private final Map<UUID, Double> damageLedger = new LinkedHashMap<>();
    private final Map<Ability, Integer> cooldowns = new HashMap<>();
    private final List<LivingEntity> minions = new ArrayList<>();
    private final List<Player> barViewers = new ArrayList<>();

    private Phase phase;
    private int phaseIndex = -1;
    private int castTimer;
    private int rotationCursor;
    private double shieldRemaining;
    private BukkitTask task;
    private boolean ended;
    private final long startedAt = System.currentTimeMillis();

    public BossInstance(EndgamePlugin plugin, BossDef def, LivingEntity entity) {
        this.plugin = plugin;
        this.def = def;
        this.entity = entity;
        this.arenaCenter = entity.getLocation().clone();
        this.bar = BossBar.bossBar(barTitle(), 1.0f,
                BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
    }

    /* ================================================================== */
    /*  lifecycle                                                          */
    /* ================================================================== */

    public void start() {
        applyStats();
        entity.customName(Component.text("\u2620 ", def.color())
                .append(Component.text(def.display(), def.color()))
                .decoration(TextDecoration.ITALIC, false));
        entity.setCustomNameVisible(false);
        entity.setRemoveWhenFarAway(false);
        if (entity instanceof Mob mob) {
            mob.setPersistent(true);
        }

        announceSpawn();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 1L);
    }

    private void applyStats() {
        set(Attribute.GENERIC_MAX_HEALTH, def.health());
        entity.setHealth(Math.min(def.health(), def.health()));
        set(Attribute.GENERIC_ATTACK_DAMAGE, def.damage());
        set(Attribute.GENERIC_ARMOR, def.armor());
        set(Attribute.GENERIC_MOVEMENT_SPEED, def.speed());
        set(Attribute.GENERIC_KNOCKBACK_RESISTANCE, def.knockbackResist());
        set(Attribute.GENERIC_FOLLOW_RANGE, def.arenaRadius() + 16);
    }

    private void set(Attribute attr, double value) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst != null) {
            inst.setBaseValue(value);
        }
    }

    private void announceSpawn() {
        World w = entity.getWorld();
        Location at = entity.getLocation();
        w.playSound(at, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.6f);
        w.playSound(at, Sound.ENTITY_WITHER_SPAWN, 1.6f, 0.7f);
        Telegraph.pillar(w, at, Particle.SOUL_FIRE_FLAME, 8, 20);
        Telegraph.flash(w, Shapes.ring(at, def.arenaRadius(), 90), def.colorRgb(), 1.6f);

        for (Player p : playersInRange()) {
            p.showTitle(net.kyori.adventure.title.Title.title(
                    Component.text(def.display(), def.color())
                            .decoration(TextDecoration.BOLD, true),
                    Component.text(def.subtitle(), Icon.DIM),
                    net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ofMillis(300),
                            java.time.Duration.ofMillis(2200),
                            java.time.Duration.ofMillis(600))));
            p.sendMessage(Component.text("\u2620 ", def.color())
                    .append(Component.text(def.display() + " " + def.spawnLine(), Icon.TEXT)));
        }
    }

    /* ================================================================== */
    /*  the tick                                                           */
    /* ================================================================== */

    private void tick() {
        if (ended) {
            return;
        }
        if (entity == null || !entity.isValid() || entity.isDead()) {
            end(true);
            return;
        }

        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
        updateBar();
        checkPhase();
        ambient();
        leash();

        if (castTimer > 0) {
            castTimer--;
            return;
        }
        if (phase == null || phase.rotation().isEmpty()) {
            return;
        }
        if (playersInRange().isEmpty()) {
            return;
        }

        Ability pick = nextAbility();
        if (pick == null) {
            castTimer = 20;
            return;
        }
        if (Mechanics.fire(plugin, this, pick)) {
            cooldowns.put(pick, pick.cooldownTicks());
            castTimer = phase.castEveryTicks();
        } else {
            castTimer = 20;
        }
    }

    private Ability nextAbility() {
        List<Ability> pool = new ArrayList<>();
        for (Ability a : phase.rotation()) {
            if (cooldowns.getOrDefault(a, 0) <= 0) {
                pool.add(a);
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        if (!phase.random()) {
            Ability a = pool.get(rotationCursor % pool.size());
            rotationCursor++;
            return a;
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    /** Moves to a new phase when health crosses a threshold. */
    private void checkPhase() {
        double frac = healthFraction();
        Phase target = def.phaseFor(frac);
        if (target == null || target == phase) {
            return;
        }
        int idx = def.phases().indexOf(target);
        if (idx <= phaseIndex) {
            return;
        }
        phaseIndex = idx;
        phase = target;
        castTimer = 30;
        rotationCursor = 0;
        transition(target);
    }

    private void transition(Phase p) {
        World w = entity.getWorld();
        Location at = entity.getLocation();
        w.playSound(at, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.8f, 0.5f);
        w.playSound(at, Sound.BLOCK_END_PORTAL_SPAWN, 1.2f, 1.6f);
        Telegraph.flash(w, Shapes.sphere(at.clone().add(0, 1.5, 0), 4, 90),
                def.colorRgb(), 1.8f);
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.DAMAGE_RESISTANCE, 40, 4, false, false));

        for (Player pl : playersInRange()) {
            pl.showTitle(net.kyori.adventure.title.Title.title(
                    Component.text(p.name(), def.color())
                            .decoration(TextDecoration.BOLD, true),
                    Component.text(p.line(), Icon.DIM),
                    net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ofMillis(200),
                            java.time.Duration.ofMillis(1600),
                            java.time.Duration.ofMillis(400))));
            pl.playSound(pl.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.1f);
        }

        if (p.onEnter() != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> Mechanics.fire(plugin, this, p.onEnter()), 30L);
        }
    }

    /** Constant aura so the boss reads as a boss even standing still. */
    private void ambient() {
        if (entity.getTicksLived() % 6 != 0) {
            return;
        }
        World w = entity.getWorld();
        Location at = entity.getLocation().add(0, entity.getHeight() * 0.6, 0);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, at, 2, 0.45, 0.4, 0.45, 0.005);
        if (shielded()) {
            Telegraph.flash(w, Shapes.dome(entity.getLocation(), 3.0, 22),
                    org.bukkit.Color.fromRGB(34, 211, 238), 1.0f);
        }
    }

    /** Bosses do not wander off. If it strays, it snaps back. */
    private void leash() {
        if (entity.getTicksLived() % 40 != 0) {
            return;
        }
        if (entity.getWorld() != arenaCenter.getWorld()) {
            return;
        }
        if (entity.getLocation().distanceSquared(arenaCenter)
                > (def.arenaRadius() + 14) * (def.arenaRadius() + 14)) {
            entity.teleport(arenaCenter);
            entity.getWorld().playSound(arenaCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
        }
    }

    /* ================================================================== */
    /*  bossbar                                                            */
    /* ================================================================== */

    private Component barTitle() {
        String phaseName = phase == null ? "" : "  \u00b7  " + phase.name();
        return Component.text("\u2620 ", def.color())
                .append(Component.text(def.display(), def.color())
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.text(phaseName, Icon.DIM));
    }

    private void updateBar() {
        float frac = (float) Math.max(0, Math.min(1, healthFraction()));
        bar.progress(frac);
        bar.name(barTitle());
        bar.color(shielded() ? BossBar.Color.BLUE
                : frac > 0.5 ? BossBar.Color.RED
                : frac > 0.25 ? BossBar.Color.YELLOW : BossBar.Color.PURPLE);

        List<Player> near = playersInRange();
        for (Player p : near) {
            if (!barViewers.contains(p)) {
                p.showBossBar(bar);
                barViewers.add(p);
            }
        }
        barViewers.removeIf(p -> {
            if (!p.isOnline() || !near.contains(p)) {
                p.hideBossBar(bar);
                return true;
            }
            return false;
        });
    }

    /* ================================================================== */
    /*  combat helpers used by Mechanics                                   */
    /* ================================================================== */

    /** Every player inside the arena. The target list for every mechanic. */
    public List<Player> playersInRange() {
        List<Player> out = new ArrayList<>();
        if (entity == null || !entity.isValid()) {
            return out;
        }
        double r = def.arenaRadius() + 6;
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR
                    || p.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                continue;
            }
            if (p.getLocation().distanceSquared(entity.getLocation()) <= r * r) {
                out.add(p);
            }
        }
        return out;
    }

    /** Damage scaled by phase multiplier and party size. */
    public double scaled(double base) {
        double mult = phase == null ? 1.0 : phase.damageMult();
        int party = Math.max(1, playersInRange().size());
        // solo players take slightly less so a fight is still soloable
        double partyScale = party == 1 ? 0.78 : 1.0 + (party - 2) * 0.05;
        return base * mult * partyScale;
    }

    /** Applies boss damage to a player with the fight's own feedback. */
    public void hit(Player p, double amount) {
        if (p == null || !p.isValid() || p.isDead()) {
            return;
        }
        p.damage(amount, entity);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.7f, 0.9f);
    }

    public void say(String what) {
        Component msg = Component.text("\u2620 ", def.color())
                .append(Component.text(def.display() + " ", def.color()))
                .append(Component.text(what, Icon.DIM));
        for (Player p : playersInRange()) {
            p.sendActionBar(msg);
        }
    }

    public void title(String main, String sub, org.bukkit.Color color) {
        TextColor tc = TextColor.color(color.getRed(), color.getGreen(), color.getBlue());
        for (Player p : playersInRange()) {
            p.showTitle(net.kyori.adventure.title.Title.title(
                    Component.text(main, tc).decoration(TextDecoration.BOLD, true),
                    Component.text(sub, Icon.DIM),
                    net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ofMillis(120),
                            java.time.Duration.ofMillis(1100),
                            java.time.Duration.ofMillis(250))));
        }
    }

    public void trackMinion(LivingEntity e) {
        minions.add(e);
    }

    /* ---- shield ------------------------------------------------------ */

    public void shield(double amount) {
        shieldRemaining = amount;
    }

    public boolean shielded() {
        return shieldRemaining > 0;
    }

    public void clearShield() {
        shieldRemaining = 0;
        World w = entity.getWorld();
        w.playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.6f, 0.7f);
        Telegraph.flash(w, Shapes.dome(entity.getLocation(), 3.0, 60),
                org.bukkit.Color.fromRGB(255, 255, 255), 1.5f);
        say("its barrier shatters");
    }

    /**
     * Routes incoming damage through the shield first. Returns the damage that
     * should actually land on the boss.
     */
    public double absorb(double incoming, Player from) {
        if (from != null) {
            damageLedger.merge(from.getUniqueId(), incoming, Double::sum);
        }
        if (shieldRemaining <= 0) {
            return incoming;
        }
        shieldRemaining -= incoming;
        World w = entity.getWorld();
        w.spawnParticle(Particle.CRIT_MAGIC, entity.getLocation().add(0, 1.4, 0),
                10, 0.5, 0.5, 0.5, 0.1);
        if (shieldRemaining <= 0) {
            clearShield();
        }
        return 0;
    }

    /* ================================================================== */
    /*  ending                                                             */
    /* ================================================================== */

    public void end(boolean killed) {
        if (ended) {
            return;
        }
        ended = true;
        if (task != null) {
            task.cancel();
        }
        for (Player p : barViewers) {
            p.hideBossBar(bar);
        }
        barViewers.clear();
        for (LivingEntity m : minions) {
            if (m != null && m.isValid()) {
                m.remove();
            }
        }
        minions.clear();

        if (killed) {
            payout();
        }
        plugin.bosses().forget(this);
    }

    private void payout() {
        World w = entity.getWorld();
        Location at = entity.getLocation();
        long seconds = (System.currentTimeMillis() - startedAt) / 1000;

        w.playSound(at, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.6f, 0.9f);
        w.playSound(at, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);
        for (int i = 0; i < 3; i++) {
            final int d = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    Telegraph.flash(w, Shapes.sphere(at.clone().add(0, 1.5, 0),
                            2 + d * 2.5, 80), def.colorRgb(), 1.7f), i * 6L);
        }

        // who actually fought it
        UUID top = null;
        double best = 0;
        for (Map.Entry<UUID, Double> e : damageLedger.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                top = e.getKey();
            }
        }

        for (Map.Entry<UUID, Double> e : damageLedger.entrySet()) {
            Player p = plugin.getServer().getPlayer(e.getKey());
            if (p == null || !p.isOnline()) {
                continue;
            }
            boolean isTop = e.getKey().equals(top);
            p.sendMessage(Component.empty());
            p.sendMessage(Component.text("\u2620 " + def.display() + " " + def.deathLine(),
                    def.color()).decoration(TextDecoration.BOLD, true));
            p.sendMessage(Component.text("  your damage  ", Icon.DIM)
                    .append(Component.text(String.format("%.0f", e.getValue()), Icon.TEXT))
                    .append(Component.text(isTop ? "   top damage" : "", Icon.WARN)));
            p.sendMessage(Component.text("  fight time  ", Icon.DIM)
                    .append(Component.text(seconds + "s", Icon.TEXT)));
            p.sendMessage(Component.empty());

            p.showTitle(net.kyori.adventure.title.Title.title(
                    Component.text("VICTORY", def.color())
                            .decoration(TextDecoration.BOLD, true),
                    Component.text(def.display() + " has fallen", Icon.DIM),
                    net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ofMillis(250),
                            java.time.Duration.ofMillis(2000),
                            java.time.Duration.ofMillis(500))));

            // combat xp for the kill, weighted by contribution
            double share = best <= 0 ? 1 : e.getValue() / Math.max(1, sumLedger());
            int xp = (int) Math.max(200, def.xpReward() * share);
            plugin.xp().award(p, com.pallarium.endgame.skill.Skill.COMBAT, xp);

            rollLoot(p, at, isTop);
        }
    }

    private double sumLedger() {
        double t = 0;
        for (double d : damageLedger.values()) {
            t += d;
        }
        return t;
    }

    private void rollLoot(Player p, Location at, boolean topDamage) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (BossDef.Drop d : def.drops()) {
            double chance = topDamage ? d.chance() * 1.5 : d.chance();
            if (rng.nextDouble() * 100.0 >= chance) {
                continue;
            }
            int amount = d.min() + rng.nextInt(Math.max(1, d.max() - d.min() + 1));
            org.bukkit.inventory.ItemStack stack =
                    new org.bukkit.inventory.ItemStack(d.material(), amount);
            if (d.name() != null && !d.name().isEmpty()) {
                org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(d.name(), def.color())
                            .decoration(TextDecoration.ITALIC, false));
                    meta.lore(List.of(
                            Component.text("dropped by " + def.display(), Icon.DIM)
                                    .decoration(TextDecoration.ITALIC, false)));
                    stack.setItemMeta(meta);
                }
            }
            at.getWorld().dropItemNaturally(at, stack);
        }
    }

    /* ================================================================== */
    /*  reads                                                              */
    /* ================================================================== */

    public double healthFraction() {
        AttributeInstance max = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double m = max == null ? 20 : max.getValue();
        return m <= 0 ? 0 : entity.getHealth() / m;
    }

    public boolean alive() {
        return !ended && entity != null && entity.isValid() && !entity.isDead();
    }

    public LivingEntity entity() {
        return entity;
    }

    public BossDef def() {
        return def;
    }

    public Phase phase() {
        return phase;
    }

    public UUID id() {
        return id;
    }

    /* ================================================================== */
    /*  names the service, listener and UI call in on                      */
    /* ================================================================== */

    /** Starts the fight. */
    public void begin() {
        start();
    }

    /** Ends the fight and removes everything. */
    public void cleanup(boolean removeBoss) {
        end(false);
        if (removeBoss && entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    /** Called when the boss dies for real. */
    public void onDeath() {
        end(true);
    }

    /** Records damage a player dealt, for the loot and xp split. */
    public void addThreat(Player p, double amount) {
        damageLedger.merge(p.getUniqueId(), amount, Double::sum);
    }

    /** Refreshes the bar after the boss took a hit. */
    public void onDamaged(double amount) {
        updateBar();
    }

    /** True if this entity is one of our summoned minions. */
    public boolean ownsMinion(org.bukkit.entity.Entity e) {
        if (e == null) {
            return false;
        }
        for (LivingEntity m : minions) {
            if (m.getUniqueId().equals(e.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    /** Drops a dead minion off the tracked list. */
    public void onMinionDeath(org.bukkit.entity.Entity e) {
        minions.removeIf(m -> m.getUniqueId().equals(e.getUniqueId()));
    }

    /** Removes a player's bossbar when they log out mid fight. */
    public void dropPlayer(Player p) {
        p.hideBossBar(bar);
        barViewers.remove(p);
    }

    /** The phase currently running. */
    public Phase currentPhase() {
        return phase;
    }

    /** Health as a short readable percent, for the admin UI. */
    public String healthPercentText() {
        return (int) Math.round(healthFraction() * 100) + "%";
    }

    public Location arenaCenter() {
        return arenaCenter;
    }

    public long startedAt() {
        return startedAt;
    }
}
