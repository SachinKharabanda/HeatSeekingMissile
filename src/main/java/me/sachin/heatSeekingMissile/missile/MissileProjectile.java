package me.sachin.heatSeekingMissile.missile;

import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.util.Vector;

public class MissileProjectile {
    public final Snowball snowball;
    public final Player shooter;
    public final Player target;
    public int ticksLeft;
    public final double speedBlocksPerSec;
    public final double damage;

    public MissileProjectile(Snowball snowball, Player shooter, Player target, int chaseTicks, double speedBps, double damage) {
        this.snowball = snowball;
        this.shooter = shooter;
        this.target = target;
        this.ticksLeft = chaseTicks;
        this.speedBlocksPerSec = speedBps;
        this.damage = damage;
    }

    public Vector speedPerTick() {
        return snowball.getVelocity().normalize().multiply(speedBlocksPerSec / 20.0);
    }
}
