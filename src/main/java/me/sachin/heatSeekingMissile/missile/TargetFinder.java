package me.sachin.heatSeekingMissile.missile;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.Optional;

public class TargetFinder {

    public static Optional<Player> find(Player shooter, double radius) {
        Location eye = shooter.getEyeLocation();
        Vector look = eye.getDirection().setY(0).normalize();
        double cosMax = Math.cos(Math.toRadians(35)); // 70° wedge (±35°)

        return shooter.getWorld().getPlayers().stream()
                .filter(p -> p != shooter)
                .filter(p -> canDamage(shooter, p))
                .filter(p -> p.getWorld().equals(eye.getWorld()))
                .filter(p -> eye.distanceSquared(p.getEyeLocation()) <= radius * radius)
                .filter(p -> { // angle check in XZ
                    Vector to = p.getEyeLocation().toVector().subtract(eye.toVector()).setY(0);
                    if (to.lengthSquared() < 0.0001) return false;
                    double cos = look.dot(to.normalize());
                    return cos >= cosMax;
                })
                .filter(p -> hasClearLOS(eye, p.getEyeLocation(), radius))
                .min(Comparator.comparingDouble(p -> eye.distanceSquared(p.getEyeLocation())));
    }

    private static boolean canDamage(Player a, Player b) {
        if (!a.getWorld().getPVP()) return false;
        if (!b.isValid() || b.isDead() || !b.isOnline()) return false;
        GameMode gm = b.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return false;
        if (b.isInvulnerable()) return false; // catches many /god implementations

        // scoreboard team ally with no friendly fire
        Team ta = a.getScoreboard().getEntryTeam(a.getName());
        Team tb = b.getScoreboard().getEntryTeam(b.getName());
        if (ta != null && ta == tb && !ta.allowFriendlyFire()) return false;

        return true;
    }

    private static boolean hasClearLOS(Location from, Location to, double maxDist) {
        Vector dir = to.toVector().subtract(from.toVector()).normalize();
        var result = from.getWorld().rayTraceBlocks(from, dir, maxDist, FluidCollisionMode.ALWAYS, true);
        if (result == null) return true;
        return result.getHitPosition().distanceSquared(from.toVector()) >= from.distanceSquared(to);
    }
}
