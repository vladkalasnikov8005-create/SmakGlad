package org.examplee.guardianClassPlugin.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.examplee.guardianClassPlugin.GuardianClassPlugin;
import org.examplee.guardianClassPlugin.util.GuardianUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class GuardianCommand implements CommandExecutor, TabCompleter {

    private final GuardianClassPlugin plugin;

    public GuardianCommand(GuardianClassPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isAdmin(CommandSender s) {
        return !(s instanceof Player) || s.hasPermission("guardian.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("guardian")) return true;
        return executeGuardian(sender, args);
    }

    public boolean executeGuardian(CommandSender sender, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GRAY + "/guardian info");
            sender.sendMessage(ChatColor.GRAY + "/guardian stage [player]");
            sender.sendMessage(ChatColor.GRAY + "/guardian set <player> <0|1|2|3> (admin)");
            sender.sendMessage(ChatColor.GRAY + "/guardian give <stone|staff|seed|lotus|life|flower> <player> [amount] (admin)");
            sender.sendMessage(ChatColor.GRAY + "/guardian treecap [on|off|toggle|status] [player]");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("info") || sub.equals("help")) {
            sendRaceInfo(sender);
            return true;
        }

        if (sub.equals("stage")) {
            Player t;
            if (args.length >= 2) {
                if (!isAdmin(sender)) {
                    sender.sendMessage(ChatColor.RED + "Нет прав: guardian.admin");
                    return true;
                }
                t = Bukkit.getPlayerExact(args[1]);
            } else {
                t = (sender instanceof Player p) ? p : null;
            }
            if (t == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден.");
                return true;
            }
            int st = plugin.data.getStage(t);
            sender.sendMessage(ChatColor.GREEN + "Стадия " + t.getName() + ": " + ChatColor.WHITE + plugin.data.stageName(st));
            return true;
        }

        if (sub.equals("set")) {
            if (!isAdmin(sender)) {
                sender.sendMessage(ChatColor.RED + "Нет прав: guardian.admin");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.YELLOW + "Пример: /guardian set Steve 2");
                return true;
            }
            Player t = Bukkit.getPlayerExact(args[1]);
            if (t == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден онлайн: " + args[1]);
                return true;
            }
            int stage;
            try {
                stage = Integer.parseInt(args[2]);
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Нужно число 0..3");
                return true;
            }
            plugin.data.setStage(t, stage);
            sender.sendMessage(ChatColor.GREEN + "Готово.");
            return true;
        }

        if (sub.equals("give")) {
            if (!isAdmin(sender)) {
                sender.sendMessage(ChatColor.RED + "Нет прав: guardian.admin");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.YELLOW + "Пример: /guardian give staff Steve 1");
                return true;
            }

            String what = args[1].toLowerCase(Locale.ROOT);
            Player t = Bukkit.getPlayerExact(args[2]);
            if (t == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден онлайн: " + args[2]);
                return true;
            }

            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (Exception ignored) {
                }
            }
            amount = Math.max(1, Math.min(64, amount));

            switch (what) {
                case "stone" -> GuardianUtil.giveOrDrop(t, plugin.items.guardianStone(amount));
                case "staff" -> GuardianUtil.giveOrDrop(t, plugin.items.waterStaff(amount));
                case "seed" -> GuardianUtil.giveOrDrop(t, plugin.items.divineSeed(amount));
                case "lotus" -> GuardianUtil.giveOrDrop(t, plugin.items.purifyingLotus(amount));
                case "life" -> GuardianUtil.giveOrDrop(t, plugin.items.lifeStoneBlock(amount));
                case "flower" -> GuardianUtil.giveOrDrop(t, plugin.items.guardianFlowerBlock(amount));
                default -> {
                    sender.sendMessage(ChatColor.YELLOW + "Предметы: stone|staff|seed|lotus|life|flower");
                    return true;
                }
            }

            sender.sendMessage(ChatColor.GREEN + "Выдано " + what + " -> " + t.getName() + " x" + amount);
            return true;
        }

        if (sub.equals("treecap")) {
            String mode = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "status";

            Player target;
            if (args.length >= 3) {
                if (!isAdmin(sender)) {
                    sender.sendMessage(ChatColor.RED + "Нет прав: guardian.admin");
                    return true;
                }
                target = Bukkit.getPlayerExact(args[2]);
            } else {
                target = (sender instanceof Player p) ? p : null;
            }

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден.");
                return true;
            }

            if (!plugin.data.isGuardian(target)) {
                sender.sendMessage(ChatColor.YELLOW + "Трикапитейтер доступен только Хранителям.");
                return true;
            }

            boolean current = plugin.data.isTreeCapEnabled(target);

            switch (mode) {
                case "on", "enable", "1" -> {
                    plugin.data.setTreeCapEnabled(target, true);
                    sender.sendMessage(ChatColor.GREEN + "Трикапитейтер включен для " + target.getName() + ".");
                    if (!target.equals(sender)) {
                        target.sendMessage(ChatColor.GREEN + "Твой трикапитейтер включен администратором.");
                    }
                }
                case "off", "disable", "0" -> {
                    plugin.data.setTreeCapEnabled(target, false);
                    sender.sendMessage(ChatColor.YELLOW + "Трикапитейтер выключен для " + target.getName() + ".");
                    if (!target.equals(sender)) {
                        target.sendMessage(ChatColor.YELLOW + "Твой трикапитейтер выключен администратором.");
                    }
                }
                case "toggle", "switch" -> {
                    boolean newState = !current;
                    plugin.data.setTreeCapEnabled(target, newState);
                    sender.sendMessage((newState ? ChatColor.GREEN : ChatColor.YELLOW)
                            + "Трикапитейтер " + (newState ? "включен" : "выключен") + " для " + target.getName() + ".");
                    if (!target.equals(sender)) {
                        target.sendMessage((newState ? ChatColor.GREEN : ChatColor.YELLOW)
                                + "Твой трикапитейтер " + (newState ? "включен" : "выключен") + " администратором.");
                    }
                }
                case "status", "state" -> sender.sendMessage(ChatColor.AQUA + "Трикапитейтер " + target.getName() + ": "
                        + (current ? ChatColor.GREEN + "включен" : ChatColor.RED + "выключен"));
                default -> sender.sendMessage(ChatColor.YELLOW + "Использование: /guardian treecap [on|off|toggle|status] [player]");
            }

            return true;
        }

        sender.sendMessage(ChatColor.RED + "Неизвестная команда. /guardian");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("guardian")) return Collections.emptyList();
        return tabCompleteGuardian(args);
    }

    public List<String> tabCompleteGuardian(String[] args) {

        if (args.length == 1) return filter(args[0], List.of("info", "stage", "set", "give", "treecap"));

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(args[1], List.of("stone", "staff", "seed", "lotus", "life", "flower"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("treecap")) {
            return filter(args[1], List.of("on", "off", "toggle", "status"));
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("stage") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return filter(args[1], names);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("treecap")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return filter(args[2], names);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) return filter(args[2], List.of("0", "1", "2", "3"));

        return Collections.emptyList();
    }

    private List<String> filter(String prefix, List<String> opts) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : opts) if (o.toLowerCase(Locale.ROOT).startsWith(p)) out.add(o);
        return out;
    }

    private void sendRaceInfo(CommandSender s) {
        s.sendMessage(ChatColor.DARK_AQUA + "====== " + ChatColor.AQUA + "Раса: Хранитель" + ChatColor.DARK_AQUA + " ======");
        s.sendMessage(ChatColor.GRAY + "Стадии:");
        s.sendMessage(ChatColor.WHITE + " 0) Нет статуса");
        s.sendMessage(ChatColor.WHITE + " 1) Неистинный");
        s.sendMessage(ChatColor.WHITE + " 2) Приближённый");
        s.sendMessage(ChatColor.WHITE + " 3) Истинный");

        s.sendMessage(ChatColor.GRAY + "Как повысить стадию:");
        s.sendMessage(ChatColor.WHITE + " - Носи Камень хранителя в OFFHAND 5 часов: 1 -> 2");

        s.sendMessage(ChatColor.GRAY + "Ограничения:");
        s.sendMessage(ChatColor.WHITE + " - Нельзя есть мясо.");
        s.sendMessage(ChatColor.WHITE + " - Нельзя пить обычные зелья (разрешены только эффекты omen).");
        s.sendMessage(ChatColor.WHITE + " - Огнестойкость не работает.");

        s.sendMessage(ChatColor.GRAY + "Пассивные эффекты:");
        s.sendMessage(ChatColor.WHITE + " - Иммунитет к падению и sonic boom.");
        s.sendMessage(ChatColor.WHITE + " - В воде усиленные бафы, в дождь - ослабленные бафы (стадии 2-3).");
        s.sendMessage(ChatColor.WHITE + " - Днём усиление, ночью слабость (стадии 2-3).");
        s.sendMessage(ChatColor.WHITE + " - Камень жизни и Цветок хранителя дают дополнительные бонусы.");

        s.sendMessage(ChatColor.GRAY + "Предметы Хранителя:");
        s.sendMessage(ChatColor.WHITE + " - Камень хранителя, Посох воды, Божественное семя,");
        s.sendMessage(ChatColor.WHITE + "   Очищающий лотос, Камень жизни, Цветок хранителя.");

        s.sendMessage(ChatColor.GRAY + "Команды:");
        s.sendMessage(ChatColor.WHITE + " - /guardian stage [player]");
        s.sendMessage(ChatColor.WHITE + " - /guardian treecap [on|off|toggle|status] [player]");
        s.sendMessage(ChatColor.WHITE + " - /guardian info");
    }
}
