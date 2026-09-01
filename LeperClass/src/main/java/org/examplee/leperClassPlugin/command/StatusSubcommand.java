package org.examplee.leperClassPlugin.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.command.Subcommand;

public final class StatusSubcommand
implements Subcommand {
    private final LeperClassPlugin plugin;

    public StatusSubcommand(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "status";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            this.plugin.msg.warn(sender, "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: /leper status <player>");
            return true;
        }
        Player t = Bukkit.getPlayerExact((String)args[1]);
        if (t == null) {
            this.plugin.msg.error(sender, "\u0418\u0433\u0440\u043e\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + args[1]);
            return true;
        }
        boolean leper = this.plugin.data.isLeper(t);
        int stage = this.plugin.data.getInfectionStage(t);
        int hits = this.plugin.data.getInfectionHits(t);
        boolean blessed = this.plugin.data.isDangerBlessed(t);
        long rage = Math.max(0L, this.plugin.data.getRageUntil(t) - System.currentTimeMillis());
        int umb = -1;
        ItemStack off = t.getInventory().getItemInOffHand();
        if (this.plugin.tags.isUmbrella(off) && off.getItemMeta() != null) {
            umb = (Integer)off.getItemMeta().getPersistentDataContainer().getOrDefault(this.plugin.keys.umbrellaRemainingKey, PersistentDataType.INTEGER, (Object)0);
        }
        this.plugin.msg.info(sender, "\u0421\u0442\u0430\u0442\u0443\u0441 " + t.getName() + ": class=" + (leper ? "leper" : "normal") + ", stage=" + stage + ", hits=" + hits + ", bless=" + blessed + ", rage=" + rage / 1000L + "s, umbrella=" + (String)(umb < 0 ? "none" : umb + "s"));
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

