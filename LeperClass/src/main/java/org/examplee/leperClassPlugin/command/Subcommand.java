package org.examplee.leperClassPlugin.command;

import java.util.List;
import org.bukkit.command.CommandSender;

public interface Subcommand {
    public String name();

    public boolean execute(CommandSender sender, String[] args);

    default public List<String> tab(CommandSender sender, String[] args) {
        return List.of();
    }
}

