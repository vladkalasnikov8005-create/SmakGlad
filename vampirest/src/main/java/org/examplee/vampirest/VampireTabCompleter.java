package org.examplee.vampirest;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class VampireTabCompleter implements TabCompleter {

    private final VampireRacePlugin plugin;

    public VampireTabCompleter(VampireRacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("help", "become", "info", "bite", "ability", "combo", "give", "collect", "infect", "setleader", "setoverlord", "stage", "turn", "godmode", "admin", "nightvision", "remove"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("ability")) {
            return filter(args[1], List.of("dash", "bat", "wolf", "vision", "cutter", "blades", "mist", "shield", "mine", "heal", "veil"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("combo")) {
            return filter(args[1], List.of("on", "off", "use", "cancel", "info"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("nightvision")) {
            return filter(args[1], List.of("on", "off"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(args[1], List.of("stake", "salt", "saltblock", "serum", "artifact", "trumehat", "garlicness1", "garlicness2", "garlicness3", "garlicbook1", "garlicbook2", "garlicbook3", "blood-normal", "blood-nutritious", "blood-vampiric", "blood-lord", "blood-corrupted"));
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("setleader") || args[0].equalsIgnoreCase("setoverlord") || args[0].equalsIgnoreCase("stage") || args[0].equalsIgnoreCase("turn") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("info"))) {
            List<String> names = new ArrayList<>();
            plugin.getServer().getOnlinePlayers().forEach(player -> names.add(player.getName()));
            return filter(args[1], names);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("stage")) {
            return filter(args[2], List.of("1", "2", "3"));
        }
        return List.of();
    }

    private List<String> filter(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream().filter(option -> option.toLowerCase().startsWith(lower)).toList();
    }
}