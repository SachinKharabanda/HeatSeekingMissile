package me.sachin.heatSeekingMissile.hooks;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

/**
 * Hook for Essentials plugin
 * This class will only be loaded if Essentials is present
 */
public class EssentialsHook {

    private static boolean enabled = false;
    private static Essentials essentials = null;

    static {
        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            try {
                essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
                enabled = true;
                Bukkit.getLogger().info("[HeatSeekingMissile] Essentials integration enabled");
            } catch (ClassCastException e) {
                Bukkit.getLogger().warning("[HeatSeekingMissile] Essentials found but couldn't cast to Essentials class");
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if a player has god mode enabled in Essentials
     */
    public static boolean hasGodMode(Player player) {
        if (!enabled) return false;

        // First try the Essentials API directly (most reliable)
        if (essentials != null) {
            try {
                User user = essentials.getUser(player);
                if (user != null && user.isGodModeEnabled()) {
                    return true;
                }
            } catch (Exception e) {
                // Fall back to metadata check
            }
        }

        // Check metadata as fallback (works with most Essentials versions)
        if (player.hasMetadata("godmode")) {
            for (MetadataValue meta : player.getMetadata("godmode")) {
                try {
                    if (meta.asBoolean()) return true;
                } catch (Exception ignored) {}
            }
        }

        // Alternative metadata key some versions use
        if (player.hasMetadata("essentials_godmode")) {
            for (MetadataValue meta : player.getMetadata("essentials_godmode")) {
                try {
                    if (meta.asBoolean()) return true;
                } catch (Exception ignored) {}
            }
        }

        return false;
    }
}