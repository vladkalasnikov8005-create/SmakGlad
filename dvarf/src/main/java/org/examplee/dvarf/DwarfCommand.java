package org.examplee.dvarf;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DwarfCommand implements CommandExecutor, TabCompleter {

    private final DwarfService dwarfService;

    public DwarfCommand(DwarfService dwarfService) {
        this.dwarfService = dwarfService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dmenu")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(dwarfService.color("&cКоманда доступна только игроку."));
                return true;
            }
            if (!dwarfService.isDwarf(player)) {
                sender.sendMessage(dwarfService.color("&cМеню доступно только дварфам."));
                return true;
            }
            DwarfProgressMenu.open(player, dwarfService);
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(dwarfService.color("&eИспользование: /dwarf race <set|remove> [игрок] | /dwarf remove [игрок] | /dwarf give <item> [игрок] | /dwarf reload | /dwarf menu [игрок] | /dwarf stage <set|info> ... | /dwarf seismic <toggle|on|off> [игрок]"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            dwarfService.reloadRuntimeSettings();
            sender.sendMessage(dwarfService.color("&aКонфиг DwarvenCore перезагружен."));
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            Player target = resolvePlayer(sender, args.length >= 2 ? args[1] : null);
            if (target == null) {
                sender.sendMessage(dwarfService.color("&cИгрок не найден."));
                return true;
            }
            dwarfService.clearDwarf(target);
            sender.sendMessage(dwarfService.color("&7Раса дварфа снята: &f" + target.getName()));
            return true;
        }

        if (args[0].equalsIgnoreCase("menu")) {
            Player target = resolvePlayer(sender, args.length >= 2 ? args[1] : null);
            if (target == null) {
                sender.sendMessage(dwarfService.color("&cИгрок не найден."));
                return true;
            }
            DwarfProgressMenu.open(target, dwarfService);
            if (sender != target) {
                sender.sendMessage(dwarfService.color("&aОткрыто меню прогресса для: &f" + target.getName()));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stage")) {
            return handleStage(sender, args);
        }

        if (args[0].equalsIgnoreCase("seismic")) {
            return handleSeismic(sender, args);
        }

        if (args[0].equalsIgnoreCase("race")) {
            return handleRace(sender, args);
        }

        if (args[0].equalsIgnoreCase("give")) {
            return handleGive(sender, args);
        }

        sender.sendMessage(dwarfService.color("&cНеизвестная подкоманда."));
        return true;
    }

    private boolean handleRace(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(dwarfService.color("&cИспользование: /dwarf race <set|remove> [игрок]"));
            return true;
        }

        Player target = resolvePlayer(sender, args.length >= 3 ? args[2] : null);
        if (target == null) {
            sender.sendMessage(dwarfService.color("&cИгрок не найден."));
            return true;
        }

        if (args[1].equalsIgnoreCase("set")) {
            dwarfService.makeDwarf(target);
            sender.sendMessage(dwarfService.color("&aРаса дварфа выдана: &f" + target.getName()));
            return true;
        }

        if (args[1].equalsIgnoreCase("remove")) {
            dwarfService.clearDwarf(target);
            sender.sendMessage(dwarfService.color("&7Раса дварфа снята: &f" + target.getName()));
            return true;
        }

        sender.sendMessage(dwarfService.color("&cДопустимо только set/remove."));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(dwarfService.color("&cИспользование: /dwarf give <item> [игрок]"));
            sender.sendMessage(dwarfService.color("&7Доступно: " + String.join(", ", dwarfService.availableItems())));
            return true;
        }

        Player target = resolvePlayer(sender, args.length >= 3 ? args[2] : null);
        if (target == null) {
            sender.sendMessage(dwarfService.color("&cИгрок не найден."));
            return true;
        }

        ItemStack item = dwarfService.createNamedItem(args[1]);
        if (item == null) {
            sender.sendMessage(dwarfService.color("&cНеизвестный item key. Доступно: &7" + String.join(", ", dwarfService.availableItems())));
            return true;
        }

        target.getInventory().addItem(item);
        sender.sendMessage(dwarfService.color("&aПредмет выдан: &f" + args[1] + " &7-> &f" + target.getName()));
        return true;
    }

    private boolean handleStage(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(dwarfService.color("&cИспользование: /dwarf stage info [игрок] | /dwarf stage set <0-5> [игрок]"));
            return true;
        }

        if (args[1].equalsIgnoreCase("info")) {
            Player target = resolvePlayer(sender, args.length >= 3 ? args[2] : null);
            if (target == null) {
                sender.sendMessage(dwarfService.color("&cИгрок не найден."));
                return true;
            }
            int stage = dwarfService.getStage(target);
            long mined = dwarfService.getMinedBlocks(target);
            long next = dwarfService.getNextStageTargetBlocks(target);
            long left = dwarfService.getBlocksToNextStage(target);
            sender.sendMessage(dwarfService.color("&6[Эволюция] &f" + target.getName()));
            sender.sendMessage(dwarfService.color("&7Этап: &e" + stage + " &7(" + dwarfService.getStageName(stage) + ")"));
            sender.sendMessage(dwarfService.color("&7Накопано блоков: &e" + mined));
            sender.sendMessage(dwarfService.color("&7Следующая цель: &e" + next));
            sender.sendMessage(dwarfService.color("&7Осталось: &e" + left));
            return true;
        }

        if (args[1].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                sender.sendMessage(dwarfService.color("&cИспользование: /dwarf stage set <0-5> [игрок]"));
                return true;
            }

            int stage;
            try {
                stage = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(dwarfService.color("&cЭтап должен быть числом 0..5."));
                return true;
            }
            if (stage < 0 || stage > 5) {
                sender.sendMessage(dwarfService.color("&cЭтап должен быть в диапазоне 0..5."));
                return true;
            }

            Player target = resolvePlayer(sender, args.length >= 4 ? args[3] : null);
            if (target == null) {
                sender.sendMessage(dwarfService.color("&cИгрок не найден."));
                return true;
            }

            dwarfService.setStage(target, stage);
            sender.sendMessage(dwarfService.color("&aЭтап установлен: &f" + target.getName() + " &7-> &e" + stage));
            target.sendMessage(dwarfService.color("&6Ваш этап эволюции изменен: &e" + stage + " &7(" + dwarfService.getStageName(stage) + ")"));
            return true;
        }

        sender.sendMessage(dwarfService.color("&cИспользование: /dwarf stage info [игрок] | /dwarf stage set <0-5> [игрок]"));
        return true;
    }

    private boolean handleSeismic(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(dwarfService.color("&cИспользование: /dwarf seismic <toggle|on|off> [игрок]"));
            return true;
        }

        Player target = resolvePlayer(sender, args.length >= 3 ? args[2] : null);
        if (target == null) {
            sender.sendMessage(dwarfService.color("&cИгрок не найден."));
            return true;
        }

        boolean enabled;
        if (args[1].equalsIgnoreCase("toggle")) {
            enabled = dwarfService.toggleSeismicSense(target);
        } else if (args[1].equalsIgnoreCase("on")) {
            dwarfService.setSeismicSenseEnabled(target, true);
            enabled = true;
        } else if (args[1].equalsIgnoreCase("off")) {
            dwarfService.setSeismicSenseEnabled(target, false);
            enabled = false;
        } else {
            sender.sendMessage(dwarfService.color("&cИспользование: /dwarf seismic <toggle|on|off> [игрок]"));
            return true;
        }

        String stateText = enabled ? "&aВКЛ" : "&cВЫКЛ";
        sender.sendMessage(dwarfService.color("&7Сейсмическое чувство: " + stateText + " &7для &f" + target.getName()));
        if (sender != target) {
            target.sendMessage(dwarfService.color("&7Сейсмическое чувство: " + stateText));
        }
        return true;
    }

    private Player resolvePlayer(CommandSender sender, String specifiedName) {
        if (specifiedName != null && !specifiedName.isEmpty()) {
            return Bukkit.getPlayerExact(specifiedName);
        }

        if (sender instanceof Player player) {
            return player;
        }

        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("dmenu")) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(List.of("race", "remove", "give", "reload", "menu", "stage", "seismic"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("seismic")) {
            return filter(List.of("toggle", "on", "off"), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("stage")) {
            return filter(List.of("info", "set"), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("race")) {
            return filter(List.of("set", "remove"), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(dwarfService.availableItems(), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[1]);
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("stage") && args[1].equalsIgnoreCase("set")) {
                return filter(List.of("0", "1", "2", "3", "4", "5"), args[2]);
            }

            if (args[0].equalsIgnoreCase("seismic")) {
                List<String> names = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    names.add(online.getName());
                }
                return filter(names, args[2]);
            }

            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("stage") && args[1].equalsIgnoreCase("set")) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[3]);
        }

        return List.of();
    }

    private List<String> filter(List<String> values, String arg) {
        String needle = arg.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(needle)) {
                out.add(value);
            }
        }
        return out;
    }
}