package org.examplee.palePlugin.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.examplee.palePlugin.PalePlugin;
import org.examplee.palePlugin.util.MathUtil;

public final class PaleSpreadCommand
implements CommandExecutor,
TabCompleter {
    private final PalePlugin plugin;

    public PaleSpreadCommand(PalePlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isAdmin(CommandSender s) {
        return !(s instanceof Player) || s.hasPermission("pale.admin");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub;
        if (!cmd.getName().equalsIgnoreCase("palespread")) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "/palespread on|off|speed <1..5000>|info|map [r]|give <item> <player> [amount] [r/uses]|gui");
            return true;
        }
        switch (sub = args[0].toLowerCase(Locale.ROOT)) {
            case "on": {
                if (!this.isAdmin(sender)) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "\u041d\u0435\u0442 \u043f\u0440\u0430\u0432: pale.admin");
                    return true;
                }
                this.plugin.spread.startRunning();
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "\u0420\u0430\u0437\u0440\u0430\u0441\u0442\u0430\u043d\u0438\u0435 \u0412\u041a\u041b.");
                break;
            }
            case "off": {
                if (!this.isAdmin(sender)) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "\u041d\u0435\u0442 \u043f\u0440\u0430\u0432: pale.admin");
                    return true;
                }
                this.plugin.spread.stopRunning();
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "\u0420\u0430\u0437\u0440\u0430\u0441\u0442\u0430\u043d\u0438\u0435 \u0412\u042b\u041a\u041b.");
                break;
            }
            case "speed": {
                if (!this.isAdmin(sender)) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "\u041d\u0435\u0442 \u043f\u0440\u0430\u0432: pale.admin");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "\u041f\u0440\u0438\u043c\u0435\u0440: /palespread speed 50");
                    return true;
                }
                try {
                    int v = Integer.parseInt(args[1]);
                    this.plugin.cfg.speedPerChunk = MathUtil.clamp(v, 1, 5000);
                    this.plugin.getConfig().set("spread.speedPerChunk", (Object)this.plugin.cfg.speedPerChunk);
                    this.plugin.saveConfig();
                    sender.sendMessage(String.valueOf(ChatColor.GREEN) + "speed=" + this.plugin.cfg.speedPerChunk);
                }
                catch (NumberFormatException e) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "\u042d\u0442\u043e \u043d\u0435 \u0447\u0438\u0441\u043b\u043e.");
                }
                break;
            }
            case "info": {
                this.plugin.spread.sendInfo(sender);
                break;
            }
            case "map": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("\u0422\u043e\u043b\u044c\u043a\u043e \u0438\u0433\u0440\u043e\u043a.");
                    return true;
                }
                Player p = (Player)sender;
                int r = this.plugin.cfg.mapItemDefaultRadiusChunks;
                if (args.length >= 2) {
                    try {
                        r = Integer.parseInt(args[1]);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                r = MathUtil.clamp(r, 1, this.plugin.cfg.mapMaxRadiusChunks);
                this.plugin.engine.sendMap(p, r);
                break;
            }
            case "give": {
                ItemStack item;
                if (!this.isAdmin(sender)) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "\u041d\u0435\u0442 \u043f\u0440\u0430\u0432: pale.admin");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "\u041f\u0440\u0438\u043c\u0435\u0440: /palespread give salt Steve 2");
                    return true;
                }
                String what = args[1].toLowerCase(Locale.ROOT);
                String who = args[2];
                int amount = 1;
                if (args.length >= 4) {
                    try {
                        amount = Integer.parseInt(args[3]);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                amount = MathUtil.clamp(amount, 1, 64);
                Player target = Bukkit.getPlayerExact((String)who);
                if (target == null) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "\u0418\u0433\u0440\u043e\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + who);
                    return true;
                }
                switch (what) {
                    case "salt": {
                        item = this.plugin.items.makeSalt(amount);
                        break;
                    }
                    case "holywater": {
                        item = this.plugin.items.makeHolyWater(amount);
                        break;
                    }
                    case "ward": {
                        item = this.plugin.items.makeWard(amount);
                        break;
                    }
                    case "flint": {
                        item = this.plugin.items.makePurifierFlint(amount);
                        break;
                    }
                    case "purge": {
                        item = this.plugin.items.makeAdminPurgeWand(amount);
                        break;
                    }
                    case "map": {
                        int r = this.plugin.cfg.mapItemDefaultRadiusChunks;
                        if (args.length >= 5) {
                            try {
                                r = Integer.parseInt(args[4]);
                            }
                            catch (Exception exception) {
                                // empty catch block
                            }
                        }
                        r = MathUtil.clamp(r, 1, this.plugin.cfg.mapMaxRadiusChunks);
                        item = this.plugin.items.makeInfectionMap(amount, r);
                        break;
                    }
                    case "wand": {
                        int uses = this.plugin.cfg.infectWandUsesDefault;
                        if (args.length >= 5) {
                            try {
                                uses = Integer.parseInt(args[4]);
                            }
                            catch (Exception exception) {
                                // empty catch block
                            }
                        }
                        uses = MathUtil.clamp(uses, 1, 10000);
                        item = this.plugin.items.makeInfectWand(amount, uses);
                        break;
                    }
                    default: {
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "items: salt|holywater|ward|flint|purge|map|wand");
                        return true;
                    }
                }
                this.plugin.items.giveOrDrop(target, item);
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "\u0412\u044b\u0434\u0430\u043d\u043e " + what + " -> " + target.getName() + " x" + amount);
                break;
            }
            case "gui": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("\u0422\u043e\u043b\u044c\u043a\u043e \u0438\u0433\u0440\u043e\u043a.");
                    return true;
                }
                Player p = (Player)sender;
                if (!this.isAdmin(sender)) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "\u041d\u0435\u0442 \u043f\u0440\u0430\u0432: pale.admin");
                    return true;
                }
                p.openInventory(this.plugin.adminGui.build(p));
                break;
            }
            default: {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u043e. /palespread");
            }
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("palespread")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return this.filter(args[0], List.of("on", "off", "speed", "info", "map", "give", "gui"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return this.filter(args[1], List.of("salt", "holywater", "ward", "flint", "purge", "map", "wand"));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            ArrayList<String> names = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return this.filter(args[2], names);
        }
        return Collections.emptyList();
    }

    private List<String> filter(String prefix, List<String> opts) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<String>();
        for (String o : opts) {
            if (!o.toLowerCase(Locale.ROOT).startsWith(p)) continue;
            out.add(o);
        }
        return out;
    }
}

