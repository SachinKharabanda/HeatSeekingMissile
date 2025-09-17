package me.sachin.heatSeekingMissile.missile;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class MissileHoldListener implements Listener {

    private final JavaPlugin plugin;
    private final MissileItem missileItem;
    private final MissileManager manager;

    public MissileHoldListener(JavaPlugin plugin, MissileItem missileItem, MissileManager manager) {
        this.plugin = plugin;
        this.missileItem = missileItem;
        this.manager = manager;
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();

        // If reloading and Reload-in-hand = true, cancel when switching OFF the missile
        if (manager.requiresInHand() && manager.isReloading(p)) {
            ItemStack oldItem = p.getInventory().getItem(e.getPreviousSlot());
            if (missileItem.isMissile(oldItem)) {
                // switching away
                manager.cancelReload(p);
            }
        }

        ItemStack newItem = p.getInventory().getItem(e.getNewSlot());
        if (missileItem.isMissile(newItem)) {
            missileItem.showSubtitleFor(p, newItem);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        // Run next tick so inventory reflects the swapped state.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = e.getPlayer();

            // Cancel reload on swap-away if required in hand
            if (manager.requiresInHand() && manager.isReloading(p)) {
                ItemStack main = p.getInventory().getItemInMainHand();
                ItemStack off  = p.getInventory().getItemInOffHand();
                // if neither hand now holds the missile we were reloading, cancel
                if (!missileItem.isMissile(main) && !missileItem.isMissile(off)) {
                    manager.cancelReload(p);
                }
            }

            // show subtitle if now holding
            showIfHolding(p);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> showIfHolding(e.getPlayer()));
    }

    private void showIfHolding(Player p) {
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off  = p.getInventory().getItemInOffHand();
        ItemStack held = missileItem.isMissile(main) ? main : (missileItem.isMissile(off) ? off : null);
        if (held != null) missileItem.showSubtitleFor(p, held);
    }
}
