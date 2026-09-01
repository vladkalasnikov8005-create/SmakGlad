package org.examplee.leperClassPlugin.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.command.Subcommand;

public final class MenuSubcommand
implements Subcommand {
    private final LeperClassPlugin plugin;

    public MenuSubcommand(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "menu";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            this.plugin.msg.error(sender, "\u041c\u0435\u043d\u044e \u043c\u043e\u0436\u043d\u043e \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u0442\u043e\u043b\u044c\u043a\u043e \u0438\u0433\u0440\u043e\u043a\u043e\u043c.");
            return true;
        }
        Player admin = (Player)sender;
        Player target = admin;
        if (args.length >= 2 && (target = Bukkit.getPlayerExact((String)args[1])) == null) {
            this.plugin.msg.error(sender, "\u0418\u0433\u0440\u043e\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + args[1]);
            return true;
        }
        this.plugin.menu.open(admin, target);
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<String>();
        Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        return out;
    }
}

