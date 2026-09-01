package org.examplee.leperClassPlugin.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.command.Subcommand;
import org.examplee.leperClassPlugin.util.EntityUtil;

public final class AddSubcommand
implements Subcommand {
    private final LeperClassPlugin plugin;

    public AddSubcommand(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "add";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            this.plugin.msg.warn(sender, "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: /leper add <player>");
            return true;
        }
        Player t = Bukkit.getPlayerExact((String)args[1]);
        if (t == null) {
            this.plugin.msg.error(sender, "\u0418\u0433\u0440\u043e\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + args[1]);
            return true;
        }
        this.plugin.data.setLeper(t, true);
        this.plugin.infection.cureDataOnly(t);
        if (this.plugin.effects.FIRE_RES != null) {
            t.removePotionEffect(this.plugin.effects.FIRE_RES);
        }
        EntityUtil.clearHostileTargets(t, 32.0);
        this.plugin.msg.ok(sender, t.getName() + " \u0442\u0435\u043f\u0435\u0440\u044c \u041f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u044b\u0439.");
        this.plugin.log.info(sender.getName() + " set leper: " + t.getName());
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

