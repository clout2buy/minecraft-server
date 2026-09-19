package com.pallarium.endgame.boss;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry. Every attack shape a boss can use is generated here as a list
 * of world points, so telegraphs, damage checks and particle draws all read
 * from the exact same set of coordinates. If you can see it, it hits you.
 */
public final class Shapes {

    private Shapes() {
    }

    /** Filled disc on the ground. */
    public static List<Location> circle(Location center, double radius, int density) {
        List<Location> out = new ArrayList<>();
        int rings = Math.max(1, (int) (radius * 2));
        for (int r = 1; r <= rings; r++) {
            double rad = radius * ((double) r / rings);
            int points = Math.max(6, (int) (rad * density));
            for (int i = 0; i < points; i++) {
                double a = (Math.PI * 2 / points) * i;
                out.add(center.clone().add(Math.cos(a) * rad, 0, Math.sin(a) * rad));
            }
        }
        return out;
    }

    /** Just the outline of a circle. Cheap, used for warning rings. */
    public static List<Location> ring(Location center, double radius, int points) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2 / points) * i;
            out.add(center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
        }
        return out;
    }

    /** Donut: everything between inner and outer. The safe spot is the middle. */
    public static List<Location> donut(Location center, double inner, double outer, int density) {
        List<Location> out = new ArrayList<>();
        int rings = Math.max(1, (int) ((outer - inner) * 2));
        for (int r = 0; r <= rings; r++) {
            double rad = inner + (outer - inner) * ((double) r / rings);
            int points = Math.max(8, (int) (rad * density));
            for (int i = 0; i < points; i++) {
                double a = (Math.PI * 2 / points) * i;
                out.add(center.clone().add(Math.cos(a) * rad, 0, Math.sin(a) * rad));
            }
        }
        return out;
    }

    /** Cone facing a direction. The classic breath attack. */
    public static List<Location> cone(Location origin, Vector dir, double length,
                                      double degrees, int density) {
        List<Location> out = new ArrayList<>();
        Vector f = dir.clone().setY(0).normalize();
        double half = Math.toRadians(degrees / 2.0);
        int steps = Math.max(3, (int) (length * 1.5));
        for (int s = 1; s <= steps; s++) {
            double d = length * ((double) s / steps);
            double spread = Math.tan(half) * d;
            int points = Math.max(3, (int) (spread * density));
            for (int i = -points; i <= points; i++) {
                double off = (spread / Math.max(1, points)) * i;
                Vector side = new Vector(-f.getZ(), 0, f.getX()).multiply(off);
                out.add(origin.clone().add(f.clone().multiply(d)).add(side));
            }
        }
        return out;
    }

    /** Straight beam with width. Walls, lasers, charges. */
    public static List<Location> line(Location origin, Vector dir, double length,
                                      double width, int density) {
        List<Location> out = new ArrayList<>();
        Vector f = dir.clone().setY(0).normalize();
        Vector side = new Vector(-f.getZ(), 0, f.getX());
        int steps = Math.max(3, (int) (length * density * 0.5));
        int wide = Math.max(1, (int) (width * density * 0.5));
        for (int s = 0; s <= steps; s++) {
            double d = length * ((double) s / steps);
            for (int i = -wide; i <= wide; i++) {
                double off = (width / 2.0) * ((double) i / wide);
                out.add(origin.clone().add(f.clone().multiply(d)).add(side.clone().multiply(off)));
            }
        }
        return out;
    }

    /** N beams radiating out from a point, like a star. */
    public static List<Location> star(Location center, int arms, double length,
                                      double width, double rotation) {
        List<Location> out = new ArrayList<>();
        for (int a = 0; a < arms; a++) {
            double angle = rotation + (Math.PI * 2 / arms) * a;
            Vector dir = new Vector(Math.cos(angle), 0, Math.sin(angle));
            out.addAll(line(center, dir, length, width, 3));
        }
        return out;
    }

    /** Spiral arm, the signature of a boss that is winding up. */
    public static List<Location> spiral(Location center, double maxRadius, double turns,
                                        double phase, int points) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < points; i++) {
            double t = (double) i / points;
            double a = phase + t * Math.PI * 2 * turns;
            double r = maxRadius * t;
            out.add(center.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r));
        }
        return out;
    }

    /** Cross / plus shape aligned to the given rotation. */
    public static List<Location> cross(Location center, double length, double width,
                                       double rotation) {
        return star(center, 4, length, width, rotation);
    }

    /** A hollow arena wall, used to cage players in for a phase. */
    public static List<Location> wall(Location center, double radius, double height, int points) {
        List<Location> out = new ArrayList<>();
        for (double y = 0; y <= height; y += 0.6) {
            for (int i = 0; i < points; i++) {
                double a = (Math.PI * 2 / points) * i;
                out.add(center.clone().add(Math.cos(a) * radius, y, Math.sin(a) * radius));
            }
        }
        return out;
    }

    /** Dome of points over a center, for shields and barriers. */
    public static List<Location> dome(Location center, double radius, int density) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < density; i++) {
            double phi = Math.acos(1 - (double) i / density);
            double theta = Math.PI * (1 + Math.sqrt(5)) * i;
            double x = Math.cos(theta) * Math.sin(phi) * radius;
            double y = Math.abs(Math.cos(phi)) * radius;
            double z = Math.sin(theta) * Math.sin(phi) * radius;
            out.add(center.clone().add(x, y, z));
        }
        return out;
    }

    /** Sphere shell, for explosions and orbs. */
    public static List<Location> sphere(Location center, double radius, int density) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < density; i++) {
            double phi = Math.acos(1 - 2.0 * i / density);
            double theta = Math.PI * (1 + Math.sqrt(5)) * i;
            out.add(center.clone().add(
                    Math.cos(theta) * Math.sin(phi) * radius,
                    Math.cos(phi) * radius,
                    Math.sin(theta) * Math.sin(phi) * radius));
        }
        return out;
    }

    /* ------------------------------------------------------------------ */
    /*  hit tests, matched to the shapes above                             */
    /* ------------------------------------------------------------------ */

    public static boolean inCircle(Location center, Location target, double radius) {
        if (target.getWorld() != center.getWorld()) {
            return false;
        }
        double dx = target.getX() - center.getX();
        double dz = target.getZ() - center.getZ();
        return dx * dx + dz * dz <= radius * radius
                && Math.abs(target.getY() - center.getY()) < 6;
    }

    public static boolean inDonut(Location center, Location target, double inner, double outer) {
        if (target.getWorld() != center.getWorld()) {
            return false;
        }
        double dx = target.getX() - center.getX();
        double dz = target.getZ() - center.getZ();
        double d2 = dx * dx + dz * dz;
        return d2 >= inner * inner && d2 <= outer * outer
                && Math.abs(target.getY() - center.getY()) < 6;
    }

    public static boolean inCone(Location origin, Vector dir, Location target,
                                 double length, double degrees) {
        if (target.getWorld() != origin.getWorld()) {
            return false;
        }
        Vector to = target.toVector().subtract(origin.toVector()).setY(0);
        double dist = to.length();
        if (dist > length || dist < 0.001) {
            return Math.abs(dist) < 0.001;
        }
        Vector f = dir.clone().setY(0).normalize();
        double angle = Math.toDegrees(Math.acos(
                Math.max(-1, Math.min(1, f.dot(to.clone().normalize())))));
        return angle <= degrees / 2.0 && Math.abs(target.getY() - origin.getY()) < 6;
    }

    public static boolean inLine(Location origin, Vector dir, Location target,
                                 double length, double width) {
        if (target.getWorld() != origin.getWorld()) {
            return false;
        }
        Vector f = dir.clone().setY(0).normalize();
        Vector to = target.toVector().subtract(origin.toVector()).setY(0);
        double along = to.dot(f);
        if (along < 0 || along > length) {
            return false;
        }
        Vector proj = f.clone().multiply(along);
        double perp = to.clone().subtract(proj).length();
        return perp <= width / 2.0 && Math.abs(target.getY() - origin.getY()) < 6;
    }

    public static boolean inStar(Location center, Location target, int arms, double length,
                                 double width, double rotation) {
        for (int a = 0; a < arms; a++) {
            double angle = rotation + (Math.PI * 2 / arms) * a;
            Vector dir = new Vector(Math.cos(angle), 0, Math.sin(angle));
            if (inLine(center, dir, target, length, width)) {
                return true;
            }
        }
        return false;
    }
}
