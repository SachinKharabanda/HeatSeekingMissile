package me.sachin.heatSeekingMissile;

import org.bukkit.plugin.java.JavaPlugin;

public final class HeatSeekingMissile extends JavaPlugin {

    private ConfigManager configs;
    private Msg msg;

    @Override
    public void onEnable() {
        // config.yml -> plugins/YourPlugin/config.yml (only if missing)
        saveDefaultConfig();

        // load messages.yml (and any others you add later)
        configs = new ConfigManager(this);
        configs.loadAll();

        // build Msg from messages.yml
        msg = new Msg(configs.messages());

        // register /hsm command
        if (getCommand("hsm") != null) {
            getCommand("hsm").setExecutor(new Commands(configs, this::rebuildMsg, this::msg));
            // If you add a TabCompleter later:
            // getCommand("hsm").setTabCompleter(new HsmTab(msg));
        } else {
            getLogger().severe("Command 'hsm' not found. Did you add it to plugin.yml?");
        }
    }

    /** Rebuild Msg after /hsm reload so it sees updated messages.yml */
    private void rebuildMsg() {
        msg = new Msg(configs.messages());
    }

    /** Supplier target used in Commands(.., this::msg) */
    public Msg msg() {
        return msg;
    }
}
