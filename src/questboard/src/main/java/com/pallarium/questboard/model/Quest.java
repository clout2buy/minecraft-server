package com.pallarium.questboard.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Quest {
    public final String id;
    public final String name;
    public final QuestType type;
    public final Set<String> targets;
    public final int amount;
    public final int xp;
    public final List<String> commands;

    public Quest(String id, String name, QuestType type, String target, int amount, int xp, List<String> commands) {
        this.id = id;
        this.name = name;
        this.type = type;
        Set<String> t = new HashSet<>();
        for (String s : target.split(",")) t.add(s.trim().toUpperCase(Locale.ROOT));
        this.targets = Collections.unmodifiableSet(t);
        this.amount = amount;
        this.xp = xp;
        this.commands = commands;
    }

    public boolean matchesBlock(Material m) {
        return targets.contains("ANY") || targets.contains(m.name());
    }

    public boolean matchesEntity(EntityType e, boolean hostile) {
        if (targets.contains("ANY")) return true;
        if (targets.contains("ANY_HOSTILE") && hostile) return true;
        return targets.contains(e.name());
    }

    public Material icon() {
        if (targets.contains("ANY") || targets.contains("ANY_HOSTILE")) return type.icon;
        String first = targets.iterator().next();
        if (type == QuestType.KILL) {
            Material egg = Material.matchMaterial(first + "_SPAWN_EGG");
            return egg != null ? egg : type.icon;
        }
        Material m = Material.matchMaterial(first);
        if (m == null || !m.isItem()) {
            if (first.equals("WHEAT")) return Material.WHEAT;
            if (first.equals("CARROTS")) return Material.CARROT;
            if (first.equals("POTATOES")) return Material.POTATO;
            if (first.equals("BEETROOTS")) return Material.BEETROOT;
            return type.icon;
        }
        return m;
    }

    public String targetLabel() {
        if (targets.contains("ANY")) return "anything";
        if (targets.contains("ANY_HOSTILE")) return "hostile mobs";
        String first = targets.iterator().next().toLowerCase(Locale.ROOT).replace('_', ' ');
        return targets.size() > 1 ? first + " & more" : first;
    }
}
