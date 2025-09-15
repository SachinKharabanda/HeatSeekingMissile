package me.sachin.heatSeekingMissile;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private final JavaPlugin plugin;
    private File messageFile;
    private FileConfiguration messagesCfg;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        loadMessages();
    }

    public void reloadAll() {
        plugin.reloadConfig();
        loadMessages();
    }

    //Messages.yml
    private void loadMessages() {

        if (messageFile==null) {
            messageFile = new File(plugin.getDataFolder(),"messages.yml");
        }

        if (!messageFile.exists()) {
            plugin.saveResource("messages.yml", false); //copies default messages/yml from jar to data folder
        }

        messagesCfg = YamlConfiguration.loadConfiguration(messageFile);

        //merge defaults from jar so missing keys get filled on updates
        try (InputStreamReader defReader = new InputStreamReader(
                plugin.getResource("messages.yml"), StandardCharsets.UTF_8
        )) {
            YamlConfiguration defCfg = YamlConfiguration.loadConfiguration(defReader);
            messagesCfg.setDefaults(defCfg);
            messagesCfg.options().copyDefaults(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public FileConfiguration messages() {
        return messagesCfg;
    }

}
