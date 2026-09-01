package org.examplee.leperClassPlugin.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.command.Subcommand;

public final class BlessSubcommand
implements Subcommand {
    private final LeperClassPlugin plugin;
    private final boolean bless;

    public BlessSubcommand(LeperClassPlugin plugin, boolean bless) {
        this.plugin = plugin;
        this.bless = bless;
    }

    @Override
    public String name() {
        return this.bless ? "bless" : "unbless";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            this.plugin.msg.warn(sender, "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: /leper " + this.name() + " <player>");
            return true;
        }
        Player t = Bukkit.getPlayerExact((String)args[1]);
        if (t == null) {
            this.plugin.msg.error(sender, "\u0418\u0433\u0440\u043e\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + args[1]);
            return true;
        }
        this.plugin.data.setDangerBlessed(t, this.bless);
        if (this.bless) {
            this.plugin.msg.ok(sender, t.getName() + " \u043f\u043e\u043b\u0443\u0447\u0438\u043b \u0431\u043b\u0430\u0433\u043e\u0441\u043b\u043e\u0432\u043b\u0435\u043d\u0438\u0435 \u0414\u0435\u043d\u0436\u0435\u0440.");
            this.plugin.msg.info((CommandSender)t, "\u0412\u044b \u043f\u043e\u043b\u0443\u0447\u0438\u043b\u0438 \u0431\u043b\u0430\u0433\u043e\u0441\u043b\u043e\u0432\u043b\u0435\u043d\u0438\u0435 \u0414\u0435\u043d\u0436\u0435\u0440.");
        } else {
            this.plugin.msg.ok(sender, t.getName() + " \u043b\u0438\u0448\u0435\u043d \u0431\u043b\u0430\u0433\u043e\u0441\u043b\u043e\u0432\u043b\u0435\u043d\u0438\u044f \u0414\u0435\u043d\u0436\u0435\u0440.");
            this.plugin.msg.info((CommandSender)t, "\u0411\u043b\u0430\u0433\u043e\u0441\u043b\u043e\u0432\u043b\u0435\u043d\u0438\u0435 \u0414\u0435\u043d\u0436\u0435\u0440 \u0441\u043d\u044f\u0442\u043e.");
        }
        this.plugin.log.info(sender.getName() + " " + this.name() + " " + t.getName());
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

