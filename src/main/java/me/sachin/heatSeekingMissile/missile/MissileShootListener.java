package me.sachin.heatSeekingMissile.missile;

import me.sachin.heatSeekingMissile.Msg;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class MissileShootListener implements Listener {

    private final MissileItem item;
    private final MissileManager manager;

    public MissileShootListener(MissileItem item, MissileManager manager, Msg msg) {
        this.item = item; this.manager = manager;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        var a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        var p = e.getPlayer();
        var held = p.getInventory().getItemInMainHand();
        if (!item.isMissile(held)) held = p.getInventory().getItemInOffHand();
        if (!item.isMissile(held)) return;

        e.setCancelled(true);
        manager.tryShoot(p);
    }
}
