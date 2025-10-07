package me.sachin.heatSeekingMissile;

import me.sachin.heatSeekingMissile.missile.MissileItem;
import me.sachin.heatSeekingMissile.missile.MissileManager;
import me.sachin.heatSeekingMissile.missile.MissileShootListener;
import org.bukkit.plugin.java.JavaPlugin;
import me.sachin.heatSeekingMissile.missile.MissileHoldListener;

public final class HeatSeekingMissile extends JavaPlugin {

    private ConfigManager configs;
    private Msg msg;
    private MissileItem missileItem;
    private me.sachin.heatSeekingMissile.missile.MissileManager missileManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configs = new ConfigManager(this);
        configs.loadAll();

        msg = new Msg(configs.messages());
        missileItem = new MissileItem(this, configs);

        // create + keep the manager (one instance!)
        missileManager = new me.sachin.heatSeekingMissile.missile.MissileManager(this, configs, msg, missileItem);

        // listener that shows the action bar when you hold the item (pass manager so it can cancel reload)
        getServer().getPluginManager().registerEvents(
                new me.sachin.heatSeekingMissile.missile.MissileHoldListener(this, missileItem, missileManager), this);

        // right-click to fire
        getServer().getPluginManager().registerEvents(
                new me.sachin.heatSeekingMissile.missile.MissileShootListener(missileItem, missileManager, msg), this);

        if (getCommand("hsm") != null) {
            getCommand("hsm").setExecutor(
                    new Commands(configs, this::afterReload, () -> msg, missileItem)
            );
        } else {
            getLogger().severe("Command 'hsm' not found in plugin.yml");
        }
    }

    private void afterReload() {
        // messages.yml + config.yml have already been reloaded by ConfigManager.reloadAll()
        msg = new Msg(configs.messages());
        missileItem.reload();
        if (missileManager != null) {
            missileManager.reloadFromConfig();  // <-- this updates Shoot.* and Reload.*
            missileManager.updateMsg(msg);      // <-- NEW: Update the Msg instance in MissileManager
        }
    }

}