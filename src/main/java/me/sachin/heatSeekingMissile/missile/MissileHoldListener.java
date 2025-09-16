package me.sachin.heatSeekingMissile.missile;

import me.sachin.heatSeekingMissile.missile.MissileItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MissileHoldListener implements Listener {

    private final JavaPlugin plugin;
    private final MissileItem missileItem;

    public MissileHoldListener(JavaPlugin plugin, MissileItem missileItem) {
        this.plugin = plugin;
        this.missileItem = missileItem;
    }

    /** Scrolling / number keys selecting a hotbar slot. */
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        // New slot after the switch:
        var newItem = p.getInventory().getItem(e.getNewSlot());
        if (missileItem.isMissile(newItem)) {
            missileItem.showSubtitle(p);
        }
    }

    /** Swapping main/off hand with F. */
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        // run next tick so the new hand items are in place
        Bukkit.getScheduler().runTask(plugin, () -> missileItem.showSubtitleIfHolding(e.getPlayer()));
    }

    /** Optional: show once if they log in already holding it. */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> missileItem.showSubtitleIfHolding(e.getPlayer()));
    }
}
