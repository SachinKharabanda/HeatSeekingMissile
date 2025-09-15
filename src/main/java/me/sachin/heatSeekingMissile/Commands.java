package me.sachin.heatSeekingMissile;

import me.sachin.heatSeekingMissile.missile.MissileItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.Supplier;

public class Commands implements CommandExecutor {

    private final ConfigManager configs;
    private final Runnable afterReload;
    private final Supplier<Msg> msg;       // Supplier so we always read the latest Msg after reload
    private final MissileItem missileItem; // Built from config.yml

    public Commands(ConfigManager configs, Runnable afterReload, Supplier<Msg> msg, MissileItem missileItem) {
        this.configs = configs;
        this.afterReload = afterReload;
        this.msg = msg;
        this.missileItem = missileItem;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!command.getName().equalsIgnoreCase("hsm")) return false;

        // /hsm -> help
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            msg.get().sendSection(sender, "help-msgs", true);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "reload" -> {
                if (!sender.hasPermission("hsm.reload")) {
                    sender.sendMessage(msg.get().withPrefix("no-perms"));
                    return true;
                }
                configs.reloadAll(); // reload config.yml + messages.yml
                afterReload.run();   // rebuild Msg and reload MissileItem snapshot
                sender.sendMessage(msg.get().withPrefix("plugin-reload-msg"));
                return true;
            }

            case "get" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use this."); // no key in messages.yml for this
                    return true;
                }
                if (!sender.hasPermission("hsm.get")) {
                    sender.sendMessage(msg.get().withPrefix("no-perms"));
                    return true;
                }
                missileItem.give(player);
                player.sendMessage(msg.get().withPrefix("received-hsm"));
                return true;
            }

            case "give" -> {
                if (!sender.hasPermission("hsm.give")) {
                    sender.sendMessage(msg.get().withPrefix("no-perms"));
                    return true;
                }
                if (args.length < 2) {
                    msg.get().sendSection(sender, "help-msgs", true);
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found."); // add a messages.yml key later if you want
                    return true;
                }
                missileItem.give(target);
                sender.sendMessage(
                        msg.get().withPrefix("sent-hsm").replace("%target%", target.getName())
                );
                if (sender != target) {
                    target.sendMessage(msg.get().withPrefix("received-hsm"));
                }
                return true;
            }

            default -> {
                msg.get().sendSection(sender, "help-msgs", true);
                return true;
            }
        }
    }
}
