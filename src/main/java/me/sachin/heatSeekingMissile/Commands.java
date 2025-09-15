package me.sachin.heatSeekingMissile;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.Supplier;

public class Commands implements CommandExecutor {

    private final ConfigManager configs;
    private final Runnable rebuildMsg;
    private final Supplier<Msg> msg; //supplier is used so after reload you get the new Msg

    public Commands (ConfigManager configs, Runnable rebuildMsg, Supplier<Msg> msg) {
        this.configs = configs;
        this.rebuildMsg = rebuildMsg;
        this.msg = msg;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

        if (command.getName().equalsIgnoreCase("hsm")) {

            if (!commandSender.hasPermission("hsm.cmds")) {
                commandSender.sendMessage(msg.get().withPrefix("no-perms"));
                return true;
            }

            else {

                if (args.length == 0) {
                    msg.get().sendSection(commandSender, "help-msgs", true);
                }

                else {

                    switch (args[0].toLowerCase(Locale.ROOT)) {

                        case "reload" -> {
                            configs.reloadAll();
                            rebuildMsg.run();
                            commandSender.sendMessage(msg.get().withPrefix("plugin-reload-msg"));
                        }

                        default -> {
                            msg.get().sendSection(commandSender, "help-msgs", true);
                        }

                    }

                }

            }

        }

        return true;
    }
}
