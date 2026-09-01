package org.examplee.leperClassPlugin.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class LeperTabCompleter
implements TabCompleter {
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("leper")) {
            return Collections.emptyList();
        }
        if (!sender.hasPermission("leper.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return this.filter(Arrays.asList("add", "remove", "menu", "bless", "unbless", "sneeze", "status", "migrateitems"), args[0]);
        }
        if (args.length == 2) {
            ArrayList<String> names = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return this.filter(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(Collection<String> options, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<String>();
        for (String s : options) {
            if (!s.toLowerCase(Locale.ROOT).startsWith(p)) continue;
            out.add(s);
        }
        return out;
    }
}

