package org.examplee.leperClassPlugin.command;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.command.AddSubcommand;
import org.examplee.leperClassPlugin.command.BlessSubcommand;
import org.examplee.leperClassPlugin.command.MenuSubcommand;
import org.examplee.leperClassPlugin.command.MigrateItemsSubcommand;
import org.examplee.leperClassPlugin.command.RemoveSubcommand;
import org.examplee.leperClassPlugin.command.SneezeSubcommand;
import org.examplee.leperClassPlugin.command.StatusSubcommand;
import org.examplee.leperClassPlugin.command.Subcommand;

public final class LeperCommand
implements CommandExecutor {
    private final LeperClassPlugin plugin;
    private final Map<String, Subcommand> subs = new LinkedHashMap<String, Subcommand>();

    public LeperCommand(LeperClassPlugin plugin) {
        this.plugin = plugin;
        this.register(new MenuSubcommand(plugin));
        this.register(new AddSubcommand(plugin));
        this.register(new RemoveSubcommand(plugin));
        this.register(new BlessSubcommand(plugin, true));
        this.register(new BlessSubcommand(plugin, false));
        this.register(new SneezeSubcommand(plugin));
        this.register(new StatusSubcommand(plugin));
        this.register(new MigrateItemsSubcommand(plugin));
    }

    private void register(Subcommand s) {
        this.subs.put(s.name().toLowerCase(), s);
    }

    public Set<String> names() {
        return this.subs.keySet();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("leper")) {
            return false;
        }
        if (!sender.hasPermission("leper.admin")) {
            this.plugin.msg.error(sender, "\u041d\u0435\u0442 \u043f\u0440\u0430\u0432: leper.admin");
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                this.plugin.msg.warn(sender, "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: /leper menu|add|remove|bless|unbless|sneeze|status");
                return true;
            }
            Player p = (Player)sender;
            this.plugin.menu.open(p, p);
            return true;
        }
        Subcommand sub = this.subs.get(args[0].toLowerCase());
        if (sub == null) {
            this.plugin.msg.warn(sender, "\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u0430\u044f \u043f\u043e\u0434\u043a\u043e\u043c\u0430\u043d\u0434\u0430. \u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e: " + String.join((CharSequence)", ", this.subs.keySet()));
            return true;
        }
        return sub.execute(sender, args);
    }
}

