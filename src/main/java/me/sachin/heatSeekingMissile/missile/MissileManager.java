package me.sachin.heatSeekingMissile.missile;

import me.sachin.heatSeekingMissile.ConfigManager;
import me.sachin.heatSeekingMissile.Msg;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class MissileManager implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager cfgMgr;
    private Msg msg;  // CHANGED: Removed 'final' so it can be updated
    private final MissileItem item;

    private final Map<UUID, MissileProjectile> active = new HashMap<>();
    private final Map<UUID, Long> nextShotAt = new HashMap<>();

    // --- reloading state per player (for cancel on switch & sound cancellation) ---
    private static final class ReloadState {
        BukkitTask completeTask;
        final List<BukkitTask> soundTasks = new ArrayList<>();
        ItemStack boundItem;     // the specific item stack being reloaded
        boolean inHandRequired;  // true => cancel if player switches away
    }
    private final Map<UUID, ReloadState> reloads = new HashMap<>();

    // config snapshot
    private double projDmg, projSpeed, radius;
    private int chaseTicks, equipDelay, shotDelay, reloadTime;
    private boolean reloadInHand;
    private String[] shootNoises, hitNoises, reloadNoises;
    private PotionEffectType potionType;
    private int potionDur, potionLvl;

    private BukkitTask ticker;
    private final String PROJ_META = "hsm_projectile";

    public MissileManager(JavaPlugin plugin, ConfigManager cfgMgr, Msg msg, MissileItem item) {
        this.plugin = plugin; this.cfgMgr = cfgMgr; this.msg = msg; this.item = item;
        reloadFromConfig();
        startTicker();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ------------------ CONFIG ------------------

    public void reloadFromConfig() {
        var c = plugin.getConfig();
        String s = "Missile-settings.Shoot.";
        projDmg    = c.getDouble(s + "Projectile-dmg", 25);
        projSpeed  = c.getDouble(s + "Projectile-speed", 15);
        radius     = c.getDouble(s + "Homing-radius", 5);
        equipDelay = c.getInt   (s + "Equip-delay", 0);
        shotDelay  = c.getInt   (s + "Delay-between-shots", 100);
        chaseTicks = c.getInt   (s + "Chase-time", 100);
        shootNoises= c.getString(s + "Shoot-noise", "").split("\\s*,\\s*");
        hitNoises  = c.getString(s + "Hit-noise",   "").split("\\s*,\\s*");

        String r = "Missile-settings.Reload.";
        reloadTime   = c.getInt   (r + "Reload-time", 300);
        reloadNoises = c.getString(r + "Reload-noise", "").split("\\s*,\\s*");
        reloadInHand = c.getBoolean(r + "Reload-in-hand", false);

        String pot = c.getString(s + "Potion-affects", "NONE-0-0");
        try {
            String[] parts = pot.split("-", 3);
            potionType = parts[0].equalsIgnoreCase("NONE") ? null : org.bukkit.potion.PotionEffectType.getByName(parts[0].toUpperCase());
            potionDur  = Integer.parseInt(parts[1]);
            potionLvl  = Integer.parseInt(parts[2]);
        } catch (Exception e) {
            potionType = null; potionDur = 0; potionLvl = 0;
        }

        plugin.getLogger().info("[HSM] Reloaded Shoot/Reload snapshot: radius=" + radius + ", chase=" + chaseTicks + ", reloadTime=" + reloadTime);
    }

    // NEW METHOD: Update the Msg instance after reload
    public void updateMsg(Msg newMsg) {
        this.msg = newMsg;
    }

    public boolean isReloading(Player p) { return reloads.containsKey(p.getUniqueId()); }
    public boolean requiresInHand() { return reloadInHand; }

    // ------------------ SHOOT ------------------

    public void tryShoot(Player shooter) {
        long now = plugin.getServer().getCurrentTick();
        long next = nextShotAt.getOrDefault(shooter.getUniqueId(), 0L);
        if (now < next) return;

        ItemStack held = shooter.getInventory().getItemInMainHand();
        if (!item.isMissile(held)) held = shooter.getInventory().getItemInOffHand();
        if (!item.isMissile(held)) return;

        // Make a final reference for use in lambda
        final ItemStack missileItem = held;

        if (isReloading(shooter)) return;

        // Check if player has too many missiles in inventory
        int missileCount = countMissilesInInventory(shooter);
        int maxAllowed = item.getMaxAmount();
        if (missileCount > maxAllowed) {
            shooter.sendMessage(msg.format("limit-hsm", Map.of("max", String.valueOf(maxAllowed)), true));
            return;
        }

        if (item.getAmmo(missileItem) <= 0) {
            startReload(shooter, missileItem);
            return;
        }

        var targetOpt = TargetFinder.find(shooter, radius);
        if (targetOpt.isEmpty()) {
            shooter.sendMessage(msg.withPrefix("no-target"));
            return;
        }
        Player target = targetOpt.get();

        // consume ammo (capacity 1)
        item.setAmmo(missileItem, 0);
        item.showSubtitleFor(shooter, missileItem);

        // fire projectile
        launch(shooter, target);
        playSounds(shooter.getLocation(), shootNoises);

        // messages
        shooter.sendMessage(msg.format("shoot-at-target", Map.of("target", target.getName()), true));
        target.sendMessage(msg.format("shot-at-by-shooter", Map.of("shooter", shooter.getName()), true));

        nextShotAt.put(shooter.getUniqueId(), now + Math.max(shotDelay, equipDelay));

        // Automatic reload after shooting (weapon now at 0 ammo)
        // Start reload immediately after firing
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Only start reload if still at 0 ammo (in case something else modified it)
            if (item.getAmmo(missileItem) <= 0 && !isReloading(shooter)) {
                startReload(shooter, missileItem);
            }
        }, 1L); // Small delay to ensure shot completes first
    }

    private void launch(Player shooter, Player target) {
        Snowball s = shooter.launchProjectile(Snowball.class);
        s.setGravity(false);
        s.setVelocity(shooter.getEyeLocation().getDirection().normalize().multiply(projSpeed / 20.0));
        s.setShooter(shooter);
        s.setMetadata(PROJ_META, new FixedMetadataValue(plugin, shooter.getUniqueId().toString()));
        active.put(s.getUniqueId(), new MissileProjectile(s, shooter, target, chaseTicks, projSpeed, projDmg));
    }

    // ------------------ RELOAD ------------------

    public void startReload(Player p, ItemStack held) {
        if (isReloading(p)) return;

        // Check if weapon is already fully loaded (capacity is 1)
        if (item.getAmmo(held) >= 1) {
            // Weapon is already at max ammo, don't reload
            return;
        }

        ReloadState rs = new ReloadState();
        rs.boundItem = held;
        rs.inHandRequired = reloadInHand;
        reloads.put(p.getUniqueId(), rs);

        item.setReloadingFlag(held, true);
        item.showSubtitleFor(p, held);

        // schedule reload completion
        rs.completeTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // if cancelled, state may already be gone
            if (!isReloading(p)) return;
            finishReload(p);
        }, reloadTime);

        // schedule reload noises (cancel if reload cancelled)
        scheduleSounds(at(p.getLocation()), reloadNoises, rs.soundTasks);
    }

    public void cancelReload(Player p) {
        ReloadState rs = reloads.remove(p.getUniqueId());
        if (rs == null) return;

        // cancel tasks
        if (rs.completeTask != null) rs.completeTask.cancel();
        for (BukkitTask t : rs.soundTasks) if (t != null) t.cancel();

        // clear reloading flag & reset action bar to default
        item.setReloadingFlag(rs.boundItem, false);

        // Check if player is still holding the missile and update subtitle
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();
        if (item.isMissile(main) || item.isMissile(off)) {
            // Update subtitle to show current state without reload indicator
            ItemStack held = item.isMissile(main) ? main : off;
            item.showSubtitleFor(p, held);
        }
    }

    private void finishReload(Player p) {
        ReloadState rs = reloads.remove(p.getUniqueId());
        if (rs == null) return;

        item.setReloadingFlag(rs.boundItem, false);
        item.setAmmo(rs.boundItem, 1);
        item.showSubtitleFor(p, rs.boundItem);
        p.sendMessage(msg.withPrefix("missile-reloaded"));
        // (Optional) play an extra completion sound here if you want, but
        // config already lets you time a sound near completion via delay.
    }

    // ------------------ TICKER (homing + particles) ------------------

    private void startTicker() {
        if (ticker != null) ticker.cancel();
        ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<UUID, MissileProjectile>> it = active.entrySet().iterator();
            while (it.hasNext()) {
                MissileProjectile mp = it.next().getValue();
                if (mp.snowball.isDead() || mp.target == null || !mp.target.isOnline() || mp.ticksLeft-- <= 0) {
                    it.remove();
                    continue;
                }
                tickHoming(mp);
                spawnStarburst(mp.snowball.getLocation(), mp.snowball.getVelocity());
            }
        }, 1L, 1L);
    }

    private void tickHoming(MissileProjectile mp) {
        Snowball sb = mp.snowball;
        Vector to = mp.target.getEyeLocation().toVector().subtract(sb.getLocation().toVector());
        if (to.lengthSquared() < 0.25) return;

        Vector desired = to.normalize();
        Vector current = sb.getVelocity().normalize();
        double steer = 0.35; // 0..1
        Vector dir = current.multiply(1.0 - steer).add(desired.multiply(steer)).normalize();
        sb.setVelocity(dir.multiply(mp.speedBlocksPerSec / 20.0));
    }

    private void spawnStarburst(Location loc, Vector vel) {
        World w = loc.getWorld();
        for (int i = 0; i < 8; i++) {
            double theta = (Math.random() * Math.PI * 2);
            double r = 0.35 + Math.random() * 0.25;
            double x = Math.cos(theta) * r;
            double y = (Math.random() - 0.5) * 0.3;
            double z = Math.sin(theta) * r;
            w.spawnParticle(Particle.CLOUD,
                    loc.getX() - vel.getX() * 0.1,
                    loc.getY() - vel.getY() * 0.1,
                    loc.getZ() - vel.getZ() * 0.1,
                    0, x, y, z, 0.0);
        }
    }

    // ------------------ HELPER METHODS ------------------

    private int countMissilesInInventory(Player p) {
        int count = 0;
        for (ItemStack stack : p.getInventory().getContents()) {
            if (item.isMissile(stack)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    // ------------------ SOUND SCHEDULING ------------------

    private record SoundAt(Location loc, Sound sound, float vol, float pitch, long delay) {}
    private Location at(Location l) { return l.clone(); }

    private void scheduleSounds(Location at, String[] specs, List<BukkitTask> outTasks) {
        if (specs == null) return;
        for (String spec : specs) {
            if (spec.isBlank()) continue;
            String[] p = spec.split("-", 4);
            try {
                Sound s = Sound.valueOf(p[0].trim().toUpperCase());
                float vol = p.length > 1 ? Float.parseFloat(p[1]) : 1f;
                float pit = p.length > 2 ? Float.parseFloat(p[2]) : 1f;
                long delay = p.length > 3 ? Long.parseLong(p[3]) : 0L;
                BukkitTask t = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    at.getWorld().playSound(at, s, vol, pit);
                }, delay);
                if (outTasks != null) outTasks.add(t);
            } catch (Exception ignored) {}
        }
    }

    private void playSounds(Location at, String[] specs) {
        scheduleSounds(at, specs, null);
    }

    // ------------------ EVENTS ------------------

    // Q key drop => cancel and start reload for the dropped missile item
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack dropped = e.getItemDrop().getItemStack();
        if (!item.isMissile(dropped)) return;
        e.setCancelled(true); // prevent the drop

        Player p = e.getPlayer();

        // Check if already reloading - if so, don't start another reload
        if (isReloading(p)) {
            return;
        }

        // Only start reload if not at max ammo
        if (item.getAmmo(dropped) < 1) {
            startReload(p, dropped);
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball s)) return;
        if (!s.hasMetadata(PROJ_META)) return;

        MissileProjectile mp = active.remove(s.getUniqueId());
        if (mp == null) return;

        Location impactLoc = s.getLocation();
        Player hit = null;
        if (e.getHitEntity() instanceof Player hp) hit = hp;
        if (hit == null) {
            var near = impactLoc.getNearbyPlayers(1.2).stream().findFirst().orElse(null);
            if (near != null) hit = near;
        }

        // Create explosion at impact location
        // Parameters: location, power, set fire, break blocks
        // Using moderate explosion that doesn't destroy blocks by default
        // Attribute the explosion to the shooter so combat-tag plugins register it
        impactLoc.getWorld().createExplosion(mp.shooter, impactLoc, 3.0f, false, false);


        // Add visual effects for more dramatic impact
        World w = impactLoc.getWorld();
        w.spawnParticle(Particle.EXPLOSION, impactLoc, 1);
        w.spawnParticle(Particle.LARGE_SMOKE, impactLoc, 20, 0.5, 0.5, 0.5, 0.1);
        w.spawnParticle(Particle.FLAME, impactLoc, 30, 0.8, 0.8, 0.8, 0.05);

        if (hit != null) {
            hit.damage(mp.damage, mp.shooter);
            if (potionType != null && potionDur > 0 && potionLvl > 0) {
                hit.addPotionEffect(new PotionEffect(potionType, potionDur, potionLvl - 1));
            }
            playSounds(impactLoc, hitNoises);
            mp.shooter.sendMessage(msg.format("target-hit", Map.of("target", hit.getName()), true));
        }
        s.remove();
    }
}