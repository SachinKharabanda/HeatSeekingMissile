package me.sachin.heatSeekingMissile.hooks;

import net.brcdev.gangs.GangsPlusApi;
import net.brcdev.gangs.gang.Gang;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Hook for GangsPlus plugin
 * This class will only be loaded if GangsPlus is present
 */
public class GangsPlusHook {

    private static boolean enabled = false;

    static {
        if (Bukkit.getPluginManager().getPlugin("GangsPlus") != null) {
            enabled = true;
            Bukkit.getLogger().info("[HeatSeekingMissile] GangsPlus integration enabled");
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if two players are in the same gang
     */
    public static boolean areInSameGang(Player p1, Player p2) {
        if (!enabled) return false;

        try {
            // Check if both players are in gangs
            if (!GangsPlusApi.isInGang(p1) || !GangsPlusApi.isInGang(p2)) {
                return false;
            }

            // Get their gangs
            Gang gang1 = GangsPlusApi.getPlayersGang(p1);
            Gang gang2 = GangsPlusApi.getPlayersGang(p2);

            if (gang1 == null || gang2 == null) {
                return false;
            }

            // Check if same gang (comparing IDs is most reliable)
            return gang1.getId() == gang2.getId();

        } catch (Exception e) {
            Bukkit.getLogger().warning("[HeatSeekingMissile] Error checking gang membership: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if two players' gangs are allied
     */
    public static boolean areGangsAllied(Player p1, Player p2) {
        if (!enabled) return false;

        try {
            // Check if both players are in gangs
            if (!GangsPlusApi.isInGang(p1) || !GangsPlusApi.isInGang(p2)) {
                return false;
            }

            // Get their gangs
            Gang gang1 = GangsPlusApi.getPlayersGang(p1);
            Gang gang2 = GangsPlusApi.getPlayersGang(p2);

            if (gang1 == null || gang2 == null) {
                return false;
            }

            // Same gang check
            if (gang1.getId() == gang2.getId()) {
                return true;
            }

            // Check if gang2 is in gang1's allies
            if (gang1.getAllyGangs() != null && gang1.getAllyGangs().contains(gang2)) {
                return true;
            }

            // Check reverse (if gang1 is in gang2's allies)
            if (gang2.getAllyGangs() != null && gang2.getAllyGangs().contains(gang1)) {
                return true;
            }

            return false;

        } catch (Exception e) {
            Bukkit.getLogger().warning("[HeatSeekingMissile] Error checking gang alliance: " + e.getMessage());
            return false;
        }
    }
}