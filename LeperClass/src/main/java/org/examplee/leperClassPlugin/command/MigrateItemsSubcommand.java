package org.examplee.leperClassPlugin.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.command.Subcommand;

public final class MigrateItemsSubcommand
implements Subcommand {
    private final LeperClassPlugin plugin;

    public MigrateItemsSubcommand(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "migrateitems";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            this.plugin.msg.warn(sender, "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: /leper migrateitems <player|all>");
            return true;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int players = 0;
            int total = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                ++players;
                total += this.plugin.migration.migratePlayer(p);
            }
            this.plugin.msg.ok(sender, "\u041f\u0440\u043e\u0432\u0435\u0440\u0435\u043d\u043e \u0438\u0433\u0440\u043e\u043a\u043e\u0432: " + players + ", \u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u043e \u0441\u043b\u043e\u0442\u043e\u0432: " + total);
            return true;
        }
        Player t = Bukkit.getPlayerExact((String)args[1]);
        if (t == null) {
            this.plugin.msg.error(sender, "\u0418\u0433\u0440\u043e\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + args[1]);
            return true;
        }
        int changed = this.plugin.migration.migratePlayer(t);
        this.plugin.msg.ok(sender, "\u0418\u0433\u0440\u043e\u043a: " + t.getName() + ", \u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u043e \u0441\u043b\u043e\u0442\u043e\u0432: " + changed);
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<String>();
        out.add("all");
        Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        return out;
    }
}

