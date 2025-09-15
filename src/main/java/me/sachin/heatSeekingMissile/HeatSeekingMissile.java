package me.sachin.heatSeekingMissile;

import me.sachin.heatSeekingMissile.missile.MissileItem;
import org.bukkit.plugin.java.JavaPlugin;

public final class HeatSeekingMissile extends JavaPlugin {

    private ConfigManager configs;
    private Msg msg;
    private MissileItem missileItem;

    @Override
    public void onEnable() {
        // Ensure config.yml exists
        saveDefaultConfig();

        // Load messages.yml (your ConfigManager handles this)
        configs = new ConfigManager(this);
        configs.loadAll();

        // Build Msg (reads messages.yml)
        msg = new Msg(configs.messages());

        // Build the missile item snapshot (reads from config.yml)
        missileItem = new MissileItem(this, configs);

        // Register /hsm command
        if (getCommand("hsm") != null) {
            getCommand("hsm").setExecutor(
                    new Commands(configs, this::afterReload, () -> msg, missileItem)
            );
        } else {
            getLogger().severe("Command 'hsm' not found in plugin.yml");
        }
    }

    /**
     * Called after /hsm reload.
     * ConfigManager.reloadAll() already reloads config.yml and messages.yml.
     * We just rebuild the in-memory helpers to reflect new values.
     */
    private void afterReload() {
        msg = new Msg(configs.messages());
        missileItem.reload();
    }
}
