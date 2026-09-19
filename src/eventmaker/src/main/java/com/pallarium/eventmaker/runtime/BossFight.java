package com.pallarium.eventmaker.runtime;

import com.pallarium.eventmaker.EventMakerPlugin;
import com.pallarium.eventmaker.model.GameEvent;
import com.pallarium.eventmaker.util.Text;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

public class BossFight extends ActiveEvent {
    public static final String META = "eventmaker_boss";
    private LivingEntity boss;
    private UUID bossId;
    private boolean slain = false;

    public BossFight(EventMakerPlugin plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    protected void onStart() {
        Location loc = event.center();
        loc.getWorld().strikeLightningEffect(loc);
        EntityType[] pool = {EntityType.RAVAGER, EntityType.WITHER_SKELETON, EntityType.IRON_GOLEM, EntityType.ZOMBIE, EntityType.SPIDER};
        EntityType chosen = pool[random.nextInt(pool.length)];
        boss = (LivingEntity) loc.getWorld().spawnEntity(loc, chosen);
        bossId = boss.getUniqueId();
        double hp = 300;
        if (boss.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
        }
        boss.setHealth(hp);
        boss.setCustomName(Text.color("&4&l" + event.name + " Boss"));
        boss.setCustomNameVisible(true);
        boss.setRemoveWhenFarAway(false);
        boss.setGlowing(true);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        boss.setMetadata(META, new FixedMetadataValue(plugin, event.id));
        if (boss instanceof Mob) ((Mob) boss).setAware(true);
        plugin.broadcast("&cA boss has appeared at &f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "&c!");
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (boss == null || boss.isDead() || !boss.isValid()) {
            if (!slain) { slain = true; stop(false); }
            return;
        }
        if (bar != null) {
            double max = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            bar.setProgress(Math.max(0, Math.min(1, boss.getHealth() / max)));
            bar.setTitle(Text.color("&4&l" + event.name + " Boss &7- &c" + (int) boss.getHealth() + " HP &8| &7" + Text.time(Math.max(0, remaining))));
        }
        Location loc = boss.getLocation();
        List<Player> near = loc.getWorld().getPlayers();
        near.removeIf(p -> p.getLocation().distanceSquared(loc) > 20 * 20);
        if (elapsed % 8 == 0 && !near.isEmpty()) {
            loc.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
            loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 5, 1, 0.5, 1);
            for (Player p : near) {
                Vector push = p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.6).setY(0.7);
                p.setVelocity(push);
                p.damage(4, boss);
            }
        }
        if (elapsed % 15 == 0 && !near.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                Location s = loc.clone().add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
                LivingEntity minion = (LivingEntity) loc.getWorld().spawnEntity(s, EntityType.ZOMBIE);
                minion.setCustomName(Text.color("&cMinion"));
                minion.setMetadata(META + "_minion", new FixedMetadataValue(plugin, event.id));
            }
            plugin.broadcast("&cThe boss summons minions!");
        }
        if (elapsed % 30 == 0 && !near.isEmpty()) {
            Player target = near.get(random.nextInt(near.size()));
            Fireball fb = loc.getWorld().spawn(loc.clone().add(0, 2, 0), Fireball.class);
            Vector dir = target.getLocation().toVector().subtract(loc.toVector()).normalize();
            fb.setDirection(dir);
            fb.setVelocity(dir.multiply(1.2));
            fb.setYield(1.5f);
            fb.setIsIncendiary(false);
        }
    }

    public boolean isBoss(Entity e) {
        return bossId != null && e.getUniqueId().equals(bossId);
    }

    public void onDamaged(Player by, double amount) {
        addScore(by, amount);
    }

    public void onKilled() {
        if (slain) return;
        slain = true;
        plugin.broadcast("&aThe boss has been slain!");
        stop(false);
    }

    @Override
    protected void onEnd(boolean forced) {
        if (boss != null && boss.isValid()) {
            boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 40, 1, 1, 1);
            boss.remove();
        }
        for (Entity e : event.center().getWorld().getEntities()) {
            if (e.hasMetadata(META + "_minion")) e.remove();
        }
        if (!forced) announceWinnersAndReward();
    }

    @Override
    protected String scoreLabel() {
        return "damage dealt";
    }
}
