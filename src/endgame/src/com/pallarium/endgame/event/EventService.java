package com.pallarium.endgame.event;

import com.pallarium.endgame.EndgamePlugin;
import com.pallarium.endgame.mob.EliteLoot;
import com.pallarium.endgame.mob.EliteTier;
import com.pallarium.endgame.skill.Skill;
import com.pallarium.endgame.ui.Icon;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runs the dynamic world event system. Player actions add heat to a trigger
 * bucket; when a bucket boils over an event fires near that player. Events run
 * as staged chains on a shared ticker, with a boss bar, live mob waves, timers,
 * failure states and contribution scaled rewards.
 */
public class EventService {

    private final EndgamePlugin plugin;
    private final NamespacedKey keyMob;
    private final NamespacedKey keyEvent;

    private final List<ActiveEvent> active = new ArrayList<>();
    private final Map<UUID, Map<Trigger, Double>> heat = new HashMap<>();
    private final Map<UUID, Long> lastEventFor = new HashMap<>();
    private final Map<UUID, Location> restLast = new HashMap<>();
    private final Map<UUID, Integer> restTicks = new HashMap<>();

    private long totalFired;
    private long totalCompleted;
    private long totalFailed;

    public EventService(EndgamePlugin plugin) {
        this.plugin = plugin;
        this.keyMob = new NamespacedKey(plugin, "event_mob");
        this.keyEvent = new NamespacedKey(plugin, "event_id");
        start();
    }

    /* ------------------------------------------------------------------ */
    /*  heat                                                               */
    /* ------------------------------------------------------------------ */

    /**
     * Feeds heat into a trigger for this player. When it crosses the threshold
     * an event fires near them and the bucket resets. This is the whole
     * "you did something, so something happened" loop.
     */
    public void heat(Player player, Trigger trigger, double amount) {
        if (!enabled() || player == null) {
            return;
        }
        Map<Trigger, Double> buckets = heat.computeIfAbsent(
                player.getUniqueId(), k -> new HashMap<>());
        double now = buckets.merge(trigger, amount, Double::sum);
        double threshold = plugin.getConfig().getDouble("events.heat-threshold", 100.0);

        // near the boil, the world gets uneasy so the player can feel it coming
        if (now >= threshold * 0.85 && ThreadLocalRandom.current().nextDouble() < 0.25) {
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.4f, 0.6f);
            player.spawnParticle(Particle.SMOKE_NORMAL,
                    player.getLocation().add(0, 1, 0), 8, 0.6, 0.6, 0.6, 0.01);
        }
        if (now < threshold) {
            return;
        }
        buckets.put(trigger, 0.0);
        if (onCooldown(player)) {
            return;
        }
        List<WorldEvent> pool = WorldEvent.forTrigger(trigger);
        if (pool.isEmpty()) {
            return;
        }
        WorldEvent def = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        fire(def, findSite(player, def), player);
    }

    private boolean onCooldown(Player player) {
        long cd = plugin.getConfig().getLong("events.player-cooldown-seconds", 240) * 1000L;
        Long last = lastEventFor.get(player.getUniqueId());
        return last != null && System.currentTimeMillis() - last < cd;
    }

    public double heatOf(Player player, Trigger trigger) {
        Map<Trigger, Double> b = heat.get(player.getUniqueId());
        return b == null ? 0 : b.getOrDefault(trigger, 0.0);
    }

    /* ------------------------------------------------------------------ */
    /*  firing                                                             */
    /* ------------------------------------------------------------------ */

    /** Picks an open spot near the player to stage the event. */
    private Location findSite(Player player, WorldEvent def) {
        World w = player.getWorld();
        Location base = player.getLocation();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < 24; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double dist = 14 + rng.nextDouble() * 18;
            Location c = base.clone().add(Math.cos(ang) * dist, 0, Math.sin(ang) * dist);
            c.setY(w.getHighestBlockYAt(c) + 1);
            // underground players get an underground event, not a surface one
            if (base.getY() < 50 && Math.abs(c.getY() - base.getY()) > 12) {
                c.setY(base.getY());
            }
            if (c.getBlock().isPassable() && c.clone().add(0, 1, 0).getBlock().isPassable()) {
                return c;
            }
        }
        return base.clone();
    }

    /** Starts an event. Public so the admin command can force one. */
    public ActiveEvent fire(WorldEvent def, Location at, Player cause) {
        if (at == null || at.getWorld() == null) {
            return null;
        }
        int cap = plugin.getConfig().getInt("events.max-concurrent", 3);
        if (active.size() >= cap) {
            return null;
        }
        ActiveEvent ev = new ActiveEvent(def, at);
        ev.mutator(Mutator.roll(plugin.getConfig().getDouble("events.mutator-chance", 0.45)));
        buildSetPiece(ev);
        active.add(ev);
        totalFired++;
        if (cause != null) {
            lastEventFor.put(cause.getUniqueId(), System.currentTimeMillis());
        }

        ev.bar(BossBar.bossBar(barText(ev), 1f,
                BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10));

        announce(ev);
        beginStage(ev);
        return ev;
    }

    private void announce(ActiveEvent ev) {
        WorldEvent def = ev.def();
        Location at = ev.center();
        World w = at.getWorld();
        if (w == null) {
            return;
        }
        w.strikeLightningEffect(at);
        w.playSound(at, Sound.ENTITY_WITHER_SPAWN, 1.4f, 0.7f);
        w.spawnParticle(Particle.FLASH, at.clone().add(0, 1, 0), 3);

        for (Player p : nearby(ev, def.radius() + 60)) {
            p.showTitle(Title.title(
                    Component.text(def.title(), def.color()),
                    Component.text(def.flavour(), Icon.DIM),
                    Title.Times.times(Duration.ofMillis(250),
                            Duration.ofMillis(2200), Duration.ofMillis(600))));
            p.playSound(p.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 0.8f, 1.0f);
            p.sendMessage(Component.text("  " + def.title(), def.color())
                    .append(Component.text("  started near you", Icon.DIM))
                    .decoration(TextDecoration.ITALIC, false));
            if (ev.mutator() != Mutator.NONE) {
                p.sendMessage(Component.text("  \u2726 " + ev.mutator().title(),
                                ev.mutator().color())
                        .append(Component.text("  " + ev.mutator().flavour(), Icon.DIM))
                        .decoration(TextDecoration.ITALIC, false));
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.9f, 0.6f);
            }
            ev.bar().addViewer(p);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  set pieces                                                         */
    /* ------------------------------------------------------------------ */

    /** Builds the event's physical site. Every block is restored on cleanup. */
    private void buildSetPiece(ActiveEvent ev) {
        Structure s = ev.structure();
        Location c = ev.center();
        switch (ev.def().setPiece()) {
            case ALTAR -> s.altar(c, ev.def().icon());
            case BRAZIERS -> ev.braziers().addAll(s.braziers(c, 6, ev.def().radius() * 0.45));
            case CRATER -> ev.marked().addAll(s.crater(c, 6));
            case CARAVAN -> s.caravanWreck(c);
            case SPIRE -> ev.core(s.spire(c, ev.def().icon(), 4));
            case CAGE -> s.cage(c);
            case NONE -> { }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  stages                                                             */
    /* ------------------------------------------------------------------ */

    private void beginStage(ActiveEvent ev) {
        EventStage stage = ev.stage();
        Location at = ev.center();
        World w = at.getWorld();
        if (w == null) {
            return;
        }
        for (Player p : nearby(ev, ev.def().radius() + 30)) {
            p.sendMessage(Component.text("  " + (ev.stageIndex() + 1) + "/"
                            + ev.stageCount() + "  ", ev.def().color())
                    .append(Component.text(stage.label(), Icon.TEXT))
                    .append(Component.text("  " + stage.hint(), Icon.DIM))
                    .decoration(TextDecoration.ITALIC, false));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.4f);
        }

        switch (stage.type()) {
            case KILL, DEFEND -> spawnWave(ev, stage.type() == StageType.DEFEND ? 4 : 6);
            case BOSS -> spawnBoss(ev);
            case BREAK -> placeMarked(ev, stage.goal());
            case ESCORT -> spawnEscort(ev);
            case DELIVER -> { /* altar already marked by the ring */ }
            case RITUAL -> {
                if (ev.braziers().isEmpty()) {
                    ev.braziers().addAll(ev.structure()
                            .braziers(ev.center(), stage.goal(), ev.def().radius() * 0.45));
                }
                ev.litBraziers().clear();
                spawnWave(ev, 4);
            }
            case CHASE -> spawnQuarry(ev);
            case COLLECT -> spawnWave(ev, 6);
            case PROTECT -> {
                if (ev.core() == null) {
                    ev.core(ev.structure().spire(ev.center(), ev.def().icon(), 4));
                }
                spawnWave(ev, 5);
            }
        }
    }

    /** The fleeing target of a CHASE stage. Fast, slippery, does not fight. */
    private void spawnQuarry(ActiveEvent ev) {
        World w = ev.center().getWorld();
        if (w == null) {
            return;
        }
        Entity e = w.spawnEntity(ev.center().clone().add(0, 0.5, 0), EntityType.PIGLIN);
        if (!(e instanceof LivingEntity quarry)) {
            e.remove();
            return;
        }
        quarry.customName(Component.text("\u27a4 " + ev.def().title(), ev.def().color())
                .decoration(TextDecoration.ITALIC, false));
        quarry.setCustomNameVisible(true);
        quarry.setRemoveWhenFarAway(false);
        quarry.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
                Integer.MAX_VALUE, 2, false, false));
        quarry.addPotionEffect(new PotionEffect(PotionEffectType.JUMP,
                Integer.MAX_VALUE, 1, false, false));
        if (quarry.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null) {
            quarry.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)
                    .setBaseValue(60);
            quarry.setHealth(60);
        }
        tag(quarry, ev);
        ev.chaseTarget(quarry);
        ev.mobs().add(quarry.getUniqueId());
        spawnWave(ev, 3);
    }

    private void spawnWave(ActiveEvent ev, int count) {
        World w = ev.center().getWorld();
        if (w == null) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            Location at = scatter(ev.center(), ev.def().radius() * 0.6);
            EntityType type = ev.def().randomMob();
            Entity e = w.spawnEntity(at, type);
            if (!(e instanceof LivingEntity mob)) {
                e.remove();
                continue;
            }
            // a slice of event mobs are proper elites, capped by the event
            EliteTier[] tiers = EliteTier.values();
            int max = ev.def().ceiling().ordinal();
            if (ev.mutator() == Mutator.ASCENDANT) {
                plugin.elites().promote(mob, tiers[Math.min(tiers.length - 1, max + 1)]);
            } else if (rng.nextDouble() < 0.35) {
                plugin.elites().promote(mob, tiers[rng.nextInt(max + 1)]);
            }
            if (ev.mutator() == Mutator.FRENZY) {
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
                        Integer.MAX_VALUE, 1, false, false));
            }
            tag(mob, ev);
            mob.getWorld().spawnParticle(Particle.SOUL, at.clone().add(0, 1, 0),
                    14, 0.3, 0.5, 0.3, 0.04);
            if (mob instanceof Mob m) {
                Player near = nearestPlayer(at, 40);
                if (near != null) {
                    m.setTarget(near);
                }
            }
            ev.mobs().add(mob.getUniqueId());
        }
        ev.lastMobSpawn(System.currentTimeMillis());
    }

    private void spawnBoss(ActiveEvent ev) {
        World w = ev.center().getWorld();
        if (w == null) {
            return;
        }
        Location at = ev.center().clone().add(0, 0.5, 0);
        Entity e = w.spawnEntity(at, ev.def().randomMob());
        if (!(e instanceof LivingEntity boss)) {
            e.remove();
            return;
        }
        plugin.elites().promote(boss, ev.def().ceiling());
        tag(boss, ev);
        ev.mobs().add(boss.getUniqueId());

        w.strikeLightningEffect(at);
        w.playSound(at, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 1.3f);
        w.spawnParticle(Particle.EXPLOSION_HUGE, at, 2);
        for (Player p : nearby(ev, ev.def().radius() + 30)) {
            p.showTitle(Title.title(
                    Component.text(ev.stage().label(), ev.def().color()),
                    Component.text(plugin.elites().nameOf(boss), Icon.DIM),
                    Title.Times.times(Duration.ofMillis(200),
                            Duration.ofMillis(1600), Duration.ofMillis(500))));
        }
    }

    private void placeMarked(ActiveEvent ev, int count) {
        World w = ev.center().getWorld();
        if (w == null) {
            return;
        }
        Material mat = ev.def() == WorldEvent.METEOR
                ? Material.MAGMA_BLOCK : Material.GLOWSTONE;
        int placed = 0;
        for (int i = 0; i < 80 && placed < count; i++) {
            Location at = scatter(ev.center(), ev.def().radius() * 0.5);
            Block b = w.getHighestBlockAt(at);
            Block target = b.getRelative(0, 1, 0);
            if (target.getType() == Material.AIR) {
                target.setType(mat);
                ev.marked().add(target);
                w.spawnParticle(Particle.FLAME,
                        target.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
                placed++;
            }
        }
    }

    private void spawnEscort(ActiveEvent ev) {
        World w = ev.center().getWorld();
        if (w == null) {
            return;
        }
        Entity e = w.spawnEntity(ev.center(), EntityType.VILLAGER);
        if (!(e instanceof Villager v)) {
            e.remove();
            return;
        }
        v.customName(Component.text("\u2727 Lost Soul", ev.def().color())
                .decoration(TextDecoration.ITALIC, false));
        v.setCustomNameVisible(true);
        v.setRemoveWhenFarAway(false);
        v.setAI(true);
        v.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1,
                false, false));
        v.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,
                Integer.MAX_VALUE, 1, false, false));
        tag(v, ev);
        ev.escortee(v);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double ang = rng.nextDouble() * Math.PI * 2;
        Location dest = ev.center().clone().add(
                Math.cos(ang) * 45, 0, Math.sin(ang) * 45);
        dest.setY(w.getHighestBlockYAt(dest) + 1);
        ev.escortTarget(dest);

        // a visible goal beacon
        w.spawnParticle(Particle.END_ROD, dest.clone().add(0, 2, 0), 60, 0.2, 2, 0.2, 0.02);
        spawnWave(ev, 3);
    }

    /* ------------------------------------------------------------------ */
    /*  ticker                                                             */
    /* ------------------------------------------------------------------ */

    private void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 1L);
        // ambient world churn: occasionally something just happens
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::ambient, 1200L, 1200L);
        // dread heat for anyone deep or in the dark
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::dread, 200L, 200L);
    }

    private void tick() {
        Iterator<ActiveEvent> it = active.iterator();
        while (it.hasNext()) {
            ActiveEvent ev = it.next();
            ev.tickDown();

            if (ev.ticksLeft() % 10 == 0) {
                updateBar(ev);
                ring(ev);
                checkStage(ev);
            }
            if (ev.ticksLeft() <= 0 && !ev.finished()) {
                failEvent(ev);
            }
            if (ev.finished()) {
                cleanup(ev);
                it.remove();
            }
        }
    }

    /** Evaluates completion conditions that are not event driven. */
    private void checkStage(ActiveEvent ev) {
        EventStage stage = ev.stage();

        if (stage.type() == StageType.DEFEND) {
            boolean held = !nearby(ev, ev.def().radius()).isEmpty();
            if (held) {
                ev.addProgress(1);
                for (Player p : nearby(ev, ev.def().radius())) {
                    ev.credit(p, 1);
                }
            }
            // keep pressure on during a hold
            if (System.currentTimeMillis() - ev.lastMobSpawn() > 9000
                    && ev.mobs().size() < 10) {
                spawnWave(ev, 3);
            }
        }

        if (stage.type() == StageType.ESCORT) {
            LivingEntity soul = ev.escortee();
            if (soul == null || soul.isDead() || !soul.isValid()) {
                failEvent(ev);
                return;
            }
            Location dest = ev.escortTarget();
            if (dest != null) {
                // the soul walks toward the beacon when a player is close
                Player near = nearestPlayer(soul.getLocation(), 16);
                if (near != null && soul instanceof Mob m) {
                    Vector dir = dest.toVector().subtract(soul.getLocation().toVector());
                    if (dir.lengthSquared() > 1) {
                        m.getPathfinder().moveTo(dest, 1.1);
                    }
                    ev.credit(near, 1);
                }
                if (soul.getLocation().distanceSquared(dest) < 9) {
                    ev.setProgress(1);
                }
                if (ev.ticksLeft() % 40 == 0) {
                    soul.getWorld().spawnParticle(Particle.END_ROD,
                            dest.clone().add(0, 2, 0), 20, 0.2, 1.5, 0.2, 0.01);
                }
            }
        }

        if (stage.type() == StageType.DELIVER && ev.ticksLeft() % 20 == 0) {
            // count dropped items of the requested type sitting on the altar
            Material want = ev.def().deliverItem();
            int found = 0;
            for (Entity e : ev.center().getWorld().getNearbyEntities(ev.center(), 4, 4, 4)) {
                if (e instanceof org.bukkit.entity.Item item
                        && item.getItemStack().getType() == want) {
                    found += item.getItemStack().getAmount();
                }
            }
            if (found > ev.progress()) {
                ev.setProgress(found);
                ev.center().getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
                        ev.center().clone().add(0, 1, 0), 20, 0.6, 0.6, 0.6, 0.1);
            }
        }

        if (stage.type() == StageType.RITUAL) {
            for (Block b : ev.braziers()) {
                if (ev.litBraziers().contains(b)) {
                    b.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            b.getLocation().add(0.5, 1.1, 0.5), 3, 0.15, 0.2, 0.15, 0.01);
                    continue;
                }
                b.getWorld().spawnParticle(Particle.SMOKE_NORMAL,
                        b.getLocation().add(0.5, 1.1, 0.5), 2, 0.15, 0.1, 0.15, 0.0);
                Player stander = null;
                for (Player p : b.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(b.getLocation().add(0.5, 0, 0.5)) < 2.5) {
                        stander = p;
                        break;
                    }
                }
                if (stander != null) {
                    ev.litBraziers().add(b);
                    b.setType(Material.FIRE, false);
                    ev.addProgress(1);
                    ev.credit(stander, 4);
                    b.getWorld().playSound(b.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
                    b.getWorld().spawnParticle(Particle.FLAME,
                            b.getLocation().add(0.5, 1, 0.5), 40, 0.3, 0.4, 0.3, 0.08);
                    if (ThreadLocalRandom.current().nextDouble() < 0.6) {
                        spawnWave(ev, 2);
                    }
                }
            }
        }

        if (stage.type() == StageType.CHASE) {
            LivingEntity quarry = ev.chaseTarget();
            if (quarry == null || quarry.isDead() || !quarry.isValid()) {
                ev.setProgress(1);
            } else if (quarry instanceof Mob m) {
                Player hunter = nearestPlayer(quarry.getLocation(), 50);
                if (hunter != null) {
                    Vector away = quarry.getLocation().toVector()
                            .subtract(hunter.getLocation().toVector());
                    if (away.lengthSquared() > 0.01) {
                        Location flee = quarry.getLocation().add(
                                away.normalize().multiply(12));
                        m.getPathfinder().moveTo(flee, 1.45);
                    }
                    ev.credit(hunter, 1);
                }
                quarry.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        quarry.getLocation().add(0, 1.2, 0), 4, 0.2, 0.3, 0.2, 0.01);
                if (quarry.getLocation().distanceSquared(ev.center())
                        > Math.pow(ev.def().radius() * 1.8, 2)) {
                    failEvent(ev);
                    return;
                }
            }
        }

        if (stage.type() == StageType.PROTECT) {
            Block core = ev.core();
            if (core == null || core.getType() == Material.AIR) {
                failEvent(ev);
                return;
            }
            core.getWorld().spawnParticle(Particle.END_ROD,
                    core.getLocation().add(0.5, 1.4, 0.5), 3, 0.2, 0.3, 0.2, 0.01);
            for (UUID id : new ArrayList<>(ev.mobs())) {
                Entity e = plugin.getServer().getEntity(id);
                if (e instanceof Mob m && m.isValid()) {
                    if (m.getLocation().distanceSquared(core.getLocation()) < 9) {
                        ev.damageCore(0.35);
                        m.getWorld().spawnParticle(Particle.CRIT,
                                core.getLocation().add(0.5, 1, 0.5), 6, 0.3, 0.3, 0.3, 0.1);
                    } else if (m.getTarget() == null) {
                        m.getPathfinder().moveTo(core.getLocation().add(0.5, 0, 0.5), 1.0);
                    }
                }
            }
            if (ev.coreHealth() <= 0) {
                core.setType(Material.AIR, false);
                failEvent(ev);
                return;
            }
            boolean held = !nearby(ev, ev.liveRadius()).isEmpty();
            if (held) {
                ev.addProgress(1);
                for (Player p : nearby(ev, ev.liveRadius())) {
                    ev.credit(p, 1);
                }
            }
            if (System.currentTimeMillis() - ev.lastMobSpawn() > 8000 && ev.mobs().size() < 12) {
                spawnWave(ev, 3);
            }
        }

        if (stage.type() == StageType.COLLECT && ev.ticksLeft() % 10 == 0) {
            for (Entity e : ev.center().getWorld().getNearbyEntities(
                    ev.center(), ev.def().radius(), 16, ev.def().radius())) {
                if (e instanceof org.bukkit.entity.Item item
                        && item.getItemStack().getType() == Material.ECHO_SHARD
                        && item.getTicksLived() > 10) {
                    item.getWorld().spawnParticle(Particle.SOUL,
                            item.getLocation().add(0, 0.4, 0), 2, 0.1, 0.1, 0.1, 0.01);
                }
            }
            if (System.currentTimeMillis() - ev.lastMobSpawn() > 7000 && ev.mobs().size() < 8) {
                spawnWave(ev, 3);
            }
        }

        applyMutator(ev);

        if (ev.stageComplete()) {
            completeStage(ev);
        }
    }

    /** Per tick effects of whatever condition got rolled onto this event. */
    private void applyMutator(ActiveEvent ev) {
        Mutator m = ev.mutator();
        if (m == Mutator.NONE) {
            return;
        }
        World w = ev.center().getWorld();
        if (w == null) {
            return;
        }
        if (m == Mutator.CLOSING && ev.ticksLeft() % 40 == 0) {
            ev.ringShrink(ev.ringShrink() + 0.45);
        }
        for (Player p : nearby(ev, ev.liveRadius())) {
            switch (m) {
                case BLACKOUT -> p.addPotionEffect(new PotionEffect(
                        PotionEffectType.BLINDNESS, 60, 0, false, false));
                case THIN_AIR -> p.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOW_FALLING, 60, 0, false, false));
                case FRENZY -> p.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, 60, 1, false, false));
                case WITHERING -> p.addPotionEffect(new PotionEffect(
                        PotionEffectType.HUNGER, 60, 0, false, false));
                case RESTLESS -> {
                    Location last = restLast.get(p.getUniqueId());
                    Location now = p.getLocation();
                    if (last != null && last.getWorld() == now.getWorld()
                            && last.distanceSquared(now) < 0.04) {
                        int n = restTicks.merge(p.getUniqueId(), 1, Integer::sum);
                        if (n > 30) {
                            p.damage(1.5);
                            restTicks.put(p.getUniqueId(), 0);
                            p.spawnParticle(Particle.SOUL, now.add(0, 1, 0),
                                    12, 0.3, 0.5, 0.3, 0.02);
                        }
                    } else {
                        restTicks.put(p.getUniqueId(), 0);
                    }
                    restLast.put(p.getUniqueId(), now.clone());
                }
                case CORROSIVE -> {
                    if (ev.ticksLeft() % 60 == 0) {
                        for (ItemStack piece : p.getInventory().getArmorContents()) {
                            if (piece != null && piece.getItemMeta()
                                    instanceof org.bukkit.inventory.meta.Damageable d) {
                                d.setDamage(d.getDamage() + 3);
                                piece.setItemMeta((org.bukkit.inventory.meta.ItemMeta) d);
                            }
                        }
                    }
                }
                case CLOSING -> {
                    if (p.getLocation().distanceSquared(ev.center())
                            > Math.pow(ev.liveRadius(), 2) + 4) {
                        p.damage(1.0);
                    }
                }
                default -> { }
            }
        }
    }

    /** Extra death behaviour driven by the mutator. */
    private void mutatorOnDeath(ActiveEvent ev, LivingEntity mob) {
        World w = mob.getWorld();
        Location at = mob.getLocation();
        switch (ev.mutator()) {
            case VOLATILE -> {
                w.createExplosion(at, 0f, false, false);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, at.add(0, 1, 0),
                        40, 0.6, 0.6, 0.6, 0.12);
                w.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.5f);
                for (Player p : w.getPlayers()) {
                    if (p.getLocation().distanceSquared(at) < 16) {
                        p.damage(3.0);
                    }
                }
            }
            case SPLITTING -> {
                if (ev.mobs().size() < 16) {
                    for (int i = 0; i < 2; i++) {
                        Entity spawn = w.spawnEntity(at, ev.def().randomMob());
                        if (spawn instanceof LivingEntity le) {
                            if (le.getAttribute(org.bukkit.attribute.Attribute
                                    .GENERIC_MAX_HEALTH) != null) {
                                le.getAttribute(org.bukkit.attribute.Attribute
                                        .GENERIC_MAX_HEALTH).setBaseValue(8);
                                le.setHealth(8);
                            }
                            tag(le, ev);
                            ev.mobs().add(le.getUniqueId());
                        } else {
                            spawn.remove();
                        }
                    }
                    w.spawnParticle(Particle.SLIME, at, 20, 0.4, 0.4, 0.4, 0.05);
                }
            }
            default -> { }
        }
    }

    private void completeStage(ActiveEvent ev) {
        World w = ev.center().getWorld();
        if (w != null) {
            w.playSound(ev.center(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            w.spawnParticle(Particle.TOTEM, ev.center().clone().add(0, 1.5, 0),
                    40, 0.8, 0.8, 0.8, 0.3);
        }
        clearMobs(ev);
        clearMarked(ev);
        if (ev.advance()) {
            beginStage(ev);
        } else {
            succeed(ev);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  outcomes                                                           */
    /* ------------------------------------------------------------------ */

    private void succeed(ActiveEvent ev) {
        totalCompleted++;
        World w = ev.center().getWorld();
        WorldEvent def = ev.def();
        if (w != null) {
            w.playSound(ev.center(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.3f, 1f);
            w.spawnParticle(Particle.TOTEM, ev.center().clone().add(0, 1.5, 0),
                    120, 1.5, 1.5, 1.5, 0.5);
        }

        int top = ev.contribution().values().stream().mapToInt(Integer::intValue).max().orElse(1);
        for (Player p : nearby(ev, def.radius() + 40)) {
            int mine = ev.contributionOf(p);
            double share = top <= 0 ? 0.5 : Math.max(0.25, Math.min(1.0, mine / (double) top));

            p.showTitle(Title.title(
                    Component.text(def.title() + " complete", Icon.GOOD),
                    Component.text(mine > 0 ? "you held the line" : "you were there", Icon.DIM),
                    Title.Times.times(Duration.ofMillis(200),
                            Duration.ofMillis(1800), Duration.ofMillis(500))));

            int xp = (int) Math.round((90 + def.ceiling().ordinal() * 70)
                    * ev.stageCount() * share * ev.mutator().rewardBonus());
            plugin.xp().award(p, Skill.COMBAT, xp);

            List<ItemStack> loot = EliteLoot.roll(def.ceiling(), List.of());
            int rolls = (int) Math.round((share >= 0.8 ? 2 : 1) * ev.mutator().rewardBonus());
            for (int i = 0; i < rolls; i++) {
                for (ItemStack item : loot) {
                    p.getWorld().dropItemNaturally(p.getLocation(), item.clone());
                }
            }
            p.sendMessage(Component.text("  " + def.title(), def.color())
                    .append(Component.text("  +" + xp + " combat xp", Icon.GOOD))
                    .append(Component.text("  (" + Math.round(share * 100) + "% share)", Icon.DIM))
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (plugin.settings().broadcastMilestones()) {
            plugin.getServer().broadcast(Component.text("  \u2726 ", def.color())
                    .append(Component.text(def.title(), def.color()))
                    .append(Component.text(" was held.", Icon.DIM))
                    .decoration(TextDecoration.ITALIC, false));
        }
        ev.finish();
    }

    private void failEvent(ActiveEvent ev) {
        totalFailed++;
        World w = ev.center().getWorld();
        if (w != null) {
            w.playSound(ev.center(), Sound.ENTITY_WITHER_DEATH, 0.8f, 0.6f);
            w.spawnParticle(Particle.SQUID_INK, ev.center().clone().add(0, 1, 0),
                    80, 1.2, 1.2, 1.2, 0.1);
        }
        for (Player p : nearby(ev, ev.def().radius() + 40)) {
            p.showTitle(Title.title(
                    Component.text(ev.def().title() + " lost", Icon.BAD),
                    Component.text("the world moved on without you", Icon.DIM),
                    Title.Times.times(Duration.ofMillis(200),
                            Duration.ofMillis(1600), Duration.ofMillis(500))));
        }
        ev.fail();
    }

    /** A player picked up an event essence during a COLLECT stage. */
    public boolean onEssencePickup(Player player, ItemStack stack) {
        if (stack.getType() != Material.ECHO_SHARD) {
            return false;
        }
        for (ActiveEvent ev : active) {
            if (ev.stage().type() == StageType.COLLECT && ev.inRange(player.getLocation())) {
                ev.addProgress(stack.getAmount());
                ev.credit(player, 3 * stack.getAmount());
                player.playSound(player.getLocation(),
                        Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
                return true;
            }
        }
        return false;
    }

    private void cleanup(ActiveEvent ev) {
        clearMobs(ev);
        clearMarked(ev);
        for (Block b : ev.braziers()) {
            if (b.getType() == Material.FIRE) {
                b.setType(Material.AIR, false);
            }
        }
        ev.braziers().clear();
        ev.structure().restore();
        if (ev.chaseTarget() != null && ev.chaseTarget().isValid()) {
            ev.chaseTarget().remove();
        }
        if (ev.escortee() != null && ev.escortee().isValid()) {
            ev.escortee().remove();
        }
        if (ev.bar() != null) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                ev.bar().removeViewer(p);
            }
        }
    }

    private void clearMobs(ActiveEvent ev) {
        for (UUID id : new ArrayList<>(ev.mobs())) {
            Entity e = plugin.getServer().getEntity(id);
            if (e != null && e.isValid()) {
                e.getWorld().spawnParticle(Particle.SMOKE_NORMAL,
                        e.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.02);
                e.remove();
            }
        }
        ev.mobs().clear();
    }

    private void clearMarked(ActiveEvent ev) {
        for (Block b : ev.marked()) {
            if (b.getType() == Material.GLOWSTONE || b.getType() == Material.MAGMA_BLOCK) {
                b.setType(Material.AIR);
            }
        }
        ev.marked().clear();
    }

    /* ------------------------------------------------------------------ */
    /*  visuals                                                            */
    /* ------------------------------------------------------------------ */

    private Component barText(ActiveEvent ev) {
        EventStage stage = ev.stage();
        int secs = Math.max(0, ev.ticksLeft() / 20);
        String time = String.format("%d:%02d", secs / 60, secs % 60);
        Component mut = ev.mutator() == Mutator.NONE ? Component.empty()
                : Component.text("  \u2726" + ev.mutator().title(), ev.mutator().color());
        return Component.text(ev.def().title() + "  ", ev.def().color())
                .append(Component.text(stage.label(), Icon.TEXT))
                .append(mut)
                .append(Component.text("  " + Math.min(ev.progress(), stage.goal())
                        + "/" + stage.goal(), Icon.NEON))
                .append(Component.text("  " + time, Icon.DIM))
                .append(Component.text("  [" + (ev.stageIndex() + 1)
                        + "/" + ev.stageCount() + "]", Icon.DIM));
    }

    private void updateBar(ActiveEvent ev) {
        if (ev.bar() == null) {
            return;
        }
        ev.bar().name(barText(ev));
        ev.bar().progress((float) Math.max(0, Math.min(1, ev.fraction())));
        float timeLeft = ev.ticksLeft() / (float) Math.max(1, ev.stage().seconds() * 20);
        ev.bar().color(timeLeft < 0.25f ? BossBar.Color.RED
                : ev.stage().type() == StageType.BOSS ? BossBar.Color.YELLOW
                : BossBar.Color.PURPLE);

        // keep viewers in sync as people walk in and out
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            boolean close = p.getWorld() == ev.center().getWorld()
                    && p.getLocation().distanceSquared(ev.center())
                    <= Math.pow(ev.def().radius() + 40, 2);
            if (close) {
                ev.bar().addViewer(p);
            } else {
                ev.bar().removeViewer(p);
            }
        }
    }

    /** Draws the event boundary so players can see where the fight is. */
    private void ring(ActiveEvent ev) {
        World w = ev.center().getWorld();
        if (w == null) {
            return;
        }
        double r = ev.liveRadius();
        for (int i = 0; i < 20; i++) {
            double a = (Math.PI * 2 / 20) * i
                    + (System.currentTimeMillis() % 4000) / 4000.0 * Math.PI * 2;
            Location p = ev.center().clone().add(Math.cos(a) * r, 0.4, Math.sin(a) * r);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0, 0.3, 0, 0);
        }
        if (ev.stage().type() == StageType.DELIVER) {
            w.spawnParticle(Particle.END_ROD, ev.center().clone().add(0, 1, 0),
                    6, 0.4, 0.6, 0.4, 0.01);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  ambient + dread                                                    */
    /* ------------------------------------------------------------------ */

    private void ambient() {
        if (!enabled()) {
            return;
        }
        double chance = plugin.getConfig().getDouble("events.ambient-chance", 12.0);
        if (ThreadLocalRandom.current().nextDouble() * 100 >= chance) {
            return;
        }
        List<Player> online = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (online.isEmpty()) {
            return;
        }
        Player pick = online.get(ThreadLocalRandom.current().nextInt(online.size()));
        if (onCooldown(pick)) {
            return;
        }
        List<WorldEvent> pool = WorldEvent.forTrigger(Trigger.AMBIENT);
        WorldEvent def = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        fire(def, findSite(pick, def), pick);
    }

    private void dread() {
        if (!enabled()) {
            return;
        }
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Location l = p.getLocation();
            boolean dark = l.getBlock().getLightLevel() <= 3;
            boolean deep = l.getY() < 30;
            boolean night = l.getWorld() != null
                    && l.getWorld().getEnvironment() == World.Environment.NORMAL
                    && l.getWorld().getTime() > 13000 && l.getWorld().getTime() < 23000;
            double add = 0;
            if (dark) add += 3;
            if (deep) add += 3;
            if (night) add += 2;
            if (add > 0) {
                heat(p, Trigger.DREAD, add);
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  hooks used by the listener                                         */
    /* ------------------------------------------------------------------ */

    /** True if this entity belongs to a running event. */
    public ActiveEvent eventOf(Entity e) {
        String s = e.getPersistentDataContainer().get(keyEvent, PersistentDataType.STRING);
        if (s == null) {
            return null;
        }
        for (ActiveEvent ev : active) {
            if (ev.id().toString().equals(s)) {
                return ev;
            }
        }
        return null;
    }

    /** An event mob died. Credits the killer and advances KILL/BOSS stages. */
    public void onEventMobDeath(LivingEntity mob, Player killer) {
        ActiveEvent ev = eventOf(mob);
        if (ev == null || ev.finished()) {
            return;
        }
        ev.mobs().remove(mob.getUniqueId());
        StageType type = ev.stage().type();

        mutatorOnDeath(ev, mob);

        if (type == StageType.CHASE && ev.chaseTarget() != null
                && ev.chaseTarget().getUniqueId().equals(mob.getUniqueId())) {
            ev.setProgress(1);
            if (killer != null) {
                ev.credit(killer, 8);
            }
            return;
        }

        if (type == StageType.COLLECT) {
            // every event mob leaves an essence on the floor to be picked up
            ItemStack shard = new ItemStack(Material.ECHO_SHARD);
            org.bukkit.inventory.meta.ItemMeta meta = shard.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("\u2727 Torn Essence", ev.def().color())
                        .decoration(TextDecoration.ITALIC, false));
                shard.setItemMeta(meta);
            }
            mob.getWorld().dropItem(mob.getLocation().add(0, 0.5, 0), shard);
            mob.getWorld().spawnParticle(Particle.SOUL,
                    mob.getLocation().add(0, 1, 0), 20, 0.3, 0.4, 0.3, 0.05);
            if (killer != null) {
                ev.credit(killer, 2);
            }
            if (ev.mobs().size() < 4) {
                spawnWave(ev, 4);
            }
            return;
        }

        if (type == StageType.KILL || type == StageType.BOSS) {
            ev.addProgress(1);
            if (killer != null) {
                ev.credit(killer, type == StageType.BOSS ? 10 : 2);
            }
            // top the wave back up so a KILL stage keeps flowing
            if (type == StageType.KILL && ev.mobs().size() < 4
                    && ev.progress() + ev.mobs().size() < ev.stage().goal()) {
                spawnWave(ev, 4);
            }
        } else if (killer != null) {
            ev.credit(killer, 1);
        }
    }

    /** A marked block was broken. */
    public boolean onMarkedBreak(Block block, Player player) {
        for (ActiveEvent ev : active) {
            if (ev.stage().type() == StageType.BREAK && ev.marked().contains(block)) {
                ev.marked().remove(block);
                ev.addProgress(1);
                ev.credit(player, 3);
                block.getWorld().spawnParticle(Particle.FLAME,
                        block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.05);
                block.getWorld().playSound(block.getLocation(),
                        Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 1.3f);
                return true;
            }
        }
        return false;
    }

    /** Escort died. */
    public void onEscortDeath(LivingEntity e) {
        ActiveEvent ev = eventOf(e);
        if (ev != null && !ev.finished()) {
            failEvent(ev);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  helpers                                                            */
    /* ------------------------------------------------------------------ */

    private void tag(Entity e, ActiveEvent ev) {
        e.getPersistentDataContainer().set(keyMob, PersistentDataType.INTEGER, 1);
        e.getPersistentDataContainer().set(keyEvent, PersistentDataType.STRING,
                ev.id().toString());
        if (e instanceof LivingEntity le) {
            le.setRemoveWhenFarAway(false);
        }
    }

    private Location scatter(Location center, double radius) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double a = rng.nextDouble() * Math.PI * 2;
        double d = rng.nextDouble() * radius;
        Location l = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
        World w = l.getWorld();
        if (w != null) {
            int y = w.getHighestBlockYAt(l);
            // keep underground events underground
            if (Math.abs(y - center.getY()) > 10) {
                l.setY(center.getY());
            } else {
                l.setY(y + 1);
            }
        }
        return l;
    }

    private List<Player> nearby(ActiveEvent ev, double radius) {
        List<Player> out = new ArrayList<>();
        World w = ev.center().getWorld();
        if (w == null) {
            return out;
        }
        double r2 = radius * radius;
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(ev.center()) <= r2) {
                out.add(p);
            }
        }
        return out;
    }

    private Player nearestPlayer(Location at, double radius) {
        Player best = null;
        double bestD = radius * radius;
        World w = at.getWorld();
        if (w == null) {
            return null;
        }
        for (Player p : w.getPlayers()) {
            double d = p.getLocation().distanceSquared(at);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("events.enabled", true);
    }

    public List<ActiveEvent> active() {
        return active;
    }

    public long[] counters() {
        return new long[]{totalFired, totalCompleted, totalFailed};
    }

    /** Ends one running event early, cleaning up its site and mobs. */
    public void stop(ActiveEvent ev) {
        if (ev == null) {
            return;
        }
        cleanup(ev);
        active.remove(ev);
    }

    /** Ends everything, used on disable and reload. */
    public void shutdown() {
        for (ActiveEvent ev : new ArrayList<>(active)) {
            cleanup(ev);
        }
        active.clear();
    }
}
