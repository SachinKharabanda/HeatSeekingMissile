package me.sachin.heatSeekingMissile;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.CommandSender;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;


public class Msg {

    private final FileConfiguration messages;

    public Msg(FileConfiguration messages) {
        this.messages =  messages;
    }

    private static String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    public String raw(String path) {
        String val = messages.getString(path);
        return colorize(val != null ? val : path);
    }

    /** Returns prefix + message. If prefix missing, just message. */
    public String withPrefix(String path) {
        String prefix = colorize(messages.getString("prefix", ""));
        return prefix + raw(path);
    }

    /** Simple %key% -> value replacement (keeps colors). */
    public String format(String path, Map<String, String> placeholders, boolean includePrefix) {
        String out = includePrefix ? withPrefix(path) : raw(path);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("%" + e.getKey() + "%", e.getValue());
        }
        return out;
    }

    public void send(CommandSender to, String path) {
        to.sendMessage(withPrefix(path));

    }

    public List<String> sectionLines(String sectionPath) {
        // 1) If it's a YAML list, just return that
        List<String> list = messages.getStringList(sectionPath);
        if (!list.isEmpty()) {
            List<String> out = new ArrayList<>(list.size());
            for (String s : list) out.add(colorize(s));
            return out;
        }

        // 2) If it's a YAML map (your current structure), read values in insertion order
        ConfigurationSection sec = messages.getConfigurationSection(sectionPath);
        if (sec == null) return Collections.emptyList();

        List<String> out = new ArrayList<>(sec.getValues(false).size());
        // Bukkit uses LinkedHashMap under the hood, so this preserves file order
        for (Object o : sec.getValues(false).values()) {
            out.add(colorize(String.valueOf(o)));
        }
        return out;
    }

    public Map<String, String> sectionMap(String sectionPath) {
        ConfigurationSection sec = messages.getConfigurationSection(sectionPath);
        if (sec == null) return Collections.emptyMap();
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : sec.getValues(false).entrySet()) {
            out.put(e.getKey(), colorize(String.valueOf(e.getValue())));
        }
        return out;
    }

    public void sendSection(CommandSender to, String sectionPath, boolean includePrefix) {
        String prefix = includePrefix ? colorize(messages.getString("prefix", "")) : "";
        for (String line : sectionLines(sectionPath)) {
            to.sendMessage(prefix + line);
        }
    }

    // Optional: placeholders per line
    public void sendSection(CommandSender to, String sectionPath, Map<String,String> placeholders, boolean includePrefix) {
        String prefix = includePrefix ? colorize(messages.getString("prefix", "")) : "";
        for (String line : sectionLines(sectionPath)) {
            for (Map.Entry<String,String> e : placeholders.entrySet()) {
                line = line.replace("%" + e.getKey() + "%", e.getValue());
            }
            to.sendMessage(prefix + line);
        }
    }

}
