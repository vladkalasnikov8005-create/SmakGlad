package org.examplee.vampirest;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VampireCommand implements CommandExecutor {

    private final VampireRacePlugin plugin;

    public VampireCommand(VampireRacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Texts.prefixed("&cКоманда только для игроков."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(player);
            case "become" -> handleBecome(player);
            case "info" -> handleInfo(player, args);
            case "bite" -> handleBite(player);
            case "ability" -> handleAbility(player, args);
            case "combo" -> handleCombo(player, args);
            case "give" -> handleGive(player, args);
            case "collect" -> handleCollectBlood(player);
            case "setleader" -> handleSetLeader(player, args);
            case "setoverlord" -> handleSetOverlord(player, args);
            case "stage" -> handleSetStage(player, args);
            case "turn" -> handleTurnNow(player, args);
            case "infect" -> handleInfect(player);
            case "godmode" -> handleGodMode(player, args);
            case "admin" -> handleAdmin(player, args);
            case "nightvision" -> handleNightVision(player, args);
            case "remove" -> handleRemoveRace(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Texts.prefixed("&f/vampire become &7- стать вампиром (perm: vampire.become)"));
        player.sendMessage(Texts.prefixed("&f/vampire give <...> &7- выдать предмет (perm: vampire.give)"));
        player.sendMessage(Texts.prefixed("&f/vampire collect &7- собрать свою кровь в бутылочку"));
        player.sendMessage(Texts.prefixed("&f/vampire info [ник] &7- информация о себе или игроке"));
        player.sendMessage(Texts.prefixed("&f/vampire remove <ник> &7- убрать расу удаленно"));
        player.sendMessage(Texts.prefixed("&f/vampire setoverlord <ник> &7- назначить Владыку"));
        player.sendMessage(Texts.prefixed("&f/vampire nightvision <on|off>"));
    }

    private void handleBecome(Player player) {
        if (!player.hasPermission("vampire.become") && !player.isOp()) {
            player.sendMessage(Texts.prefixed("&cНет прав: vampire.become"));
            return;
        }
        if (plugin.getVampireManager().isVampire(player)) {
            player.sendMessage(Texts.prefixed("&eВы уже вампир."));
            return;
        }
        plugin.getVampireManager().becomeVampire(player);
        player.sendMessage(Texts.prefixed("&aВы стали вампиром."));
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            sendInfo(player, player);
            return;
        }
        if (!player.hasPermission("vampire.admin") && !player.isOp()) {
            player.sendMessage(Texts.prefixed("&cНедостаточно прав для просмотра чужой информации."));
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Texts.prefixed("&cИгрок не найден."));
            return;
        }
        sendInfo(player, target);
    }

    private void sendInfo(Player viewer, Player target) {
        int stage = plugin.getVampireManager().getInfectionStage(target);
        long stageStart = plugin.getVampireManager().getInfectionStartedAt(target);
        long elapsedMinutes = stageStart <= 0 ? 0 : (System.currentTimeMillis() - stageStart) / 60000L;

        viewer.sendMessage(Texts.prefixed("&fИгрок: &c" + target.getName()));
        viewer.sendMessage(Texts.prefixed("&fВампир: " + (plugin.getVampireManager().isVampire(target) ? "&aДА" : "&cНЕТ")));
        viewer.sendMessage(Texts.prefixed("&fЛорд: " + (plugin.getVampireManager().isLeader(target) ? "&4ДА" : "&7НЕТ")));
        viewer.sendMessage(Texts.prefixed("&fВладыка: " + (plugin.getVampireManager().isOverlord(target) ? "&5ДА" : "&7НЕТ")));
        viewer.sendMessage(Texts.prefixed("&fСтадия заражения: &e" + stage));
        viewer.sendMessage(Texts.prefixed("&fМинут с начала заражения: &e" + elapsedMinutes));

        if (!plugin.getVampireManager().isVampire(target)) {
            return;
        }
        VampireData data = plugin.getVampireManager().getVampireData(target);
        viewer.sendMessage(Texts.prefixed("&fУровень: &c" + data.level()));
        viewer.sendMessage(Texts.prefixed(Texts.BLOOD_WORD + "&8: &f" + String.format("%.1f", data.blood()) + "&8/&f100"));
        viewer.sendMessage(Texts.prefixed("&fГолод: &6" + data.hunger() + "&8/&620"));
        viewer.sendMessage(Texts.prefixed("&fПассивное NV: " + (data.nightVisionEnabled() ? "&aВКЛ" : "&cВЫКЛ")));
        viewer.sendMessage(Texts.prefixed("&fКД сбора крови: &e" + (data.cooldownLeftMillis("self_collect") / 1000.0) + " сек."));
    }

    private void handleAbility(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Texts.prefixed("&eИспользуйте /vampire ability <dash|bat|wolf|vision|cutter|blades|mist|shield|mine|heal|veil>"));
            return;
        }
        String ability = args[1].toLowerCase();
        if (plugin.getAbilityManager().isComboModeEnabled(player)) {
            plugin.getAbilityManager().queueAbilityForCombo(player, ability);
            return;
        }
        if (!plugin.getAbilityManager().castAbilityByName(player, ability)) {
            player.sendMessage(Texts.prefixed("&cНеизвестная способность."));
        }
    }

    private void handleCombo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Texts.prefixed("&eИспользуйте /vampire combo <on|off|use|cancel|info>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "on" -> plugin.getAbilityManager().setComboMode(player, true);
            case "off" -> plugin.getAbilityManager().setComboMode(player, false);
            case "use" -> plugin.getAbilityManager().useCombo(player);
            case "cancel" -> plugin.getAbilityManager().cancelCombo(player);
            case "info" -> plugin.getAbilityManager().showCombo(player);
            default -> player.sendMessage(Texts.prefixed("&cНеизвестная подкоманда комбо."));
        }
    }

    private void handleGive(Player player, String[] args) {
        if (!player.hasPermission("vampire.give") && !player.isOp()) {
            player.sendMessage(Texts.prefixed("&cНет прав: vampire.give"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Texts.prefixed("&eИспользуйте /vampire give <stake|salt|saltblock|serum|artifact|trumehat|garlicness1|garlicness2|garlicness3|garlicbook1|garlicbook2|garlicbook3>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "stake" -> player.getInventory().addItem(plugin.getCustomItemManager().createAspenStake());
            case "salt" -> player.getInventory().addItem(plugin.getCustomItemManager().createSalt(16));
            case "saltblock" -> player.getInventory().addItem(plugin.getCustomItemManager().createSaltBlockItem(16));
            case "serum" -> player.getInventory().addItem(plugin.getCustomItemManager().createGarlicSerum());
            case "artifact" -> player.getInventory().addItem(plugin.getCustomItemManager().createBloodArtifact());
            case "trumehat" -> player.getInventory().addItem(plugin.getCustomItemManager().createTrumeHat());
            case "garlicness1" -> player.getInventory().addItem(plugin.getCustomItemManager().createGarlicChestplate(1));
            case "garlicness2" -> player.getInventory().addItem(plugin.getCustomItemManager().createGarlicChestplate(2));
            case "garlicness3" -> player.getInventory().addItem(plugin.getCustomItemManager().createGarlicChestplate(3));
            case "garlicbook1" -> player.getInventory().addItem(plugin.getCustomItemManager().createGarlicBook(1));
            case "garlicbook2" -> player.getInventory().addItem(plugin.getCustomItemManager().createGarlicBook(2));
            case "garlicbook3" -> player.getInventory().addItem(plugin.getCustomItemManager().createGarlicBook(3));
            case "blood-normal" -> player.getInventory().addItem(plugin.getCustomItemManager().createBloodBottle(CustomItemManager.BloodBottleType.NORMAL));
            case "blood-nutritious" -> player.getInventory().addItem(plugin.getCustomItemManager().createBloodBottle(CustomItemManager.BloodBottleType.NUTRITIOUS));
            case "blood-vampiric" -> player.getInventory().addItem(plugin.getCustomItemManager().createBloodBottle(CustomItemManager.BloodBottleType.VAMPIRIC));
            case "blood-lord" -> player.getInventory().addItem(plugin.getCustomItemManager().createBloodBottle(CustomItemManager.BloodBottleType.LORD));
            case "blood-corrupted" -> player.getInventory().addItem(plugin.getCustomItemManager().createBloodBottle(CustomItemManager.BloodBottleType.CORRUPTED));
            default -> player.sendMessage(Texts.prefixed("&cНеизвестный предмет."));
        }
    }

    private void handleBite(Player player) {
        plugin.getBiteCommand().execute(player);
    }

    private void handleCollectBlood(Player player) {
        if (!plugin.getVampireManager().isVampire(player)) {
            player.sendMessage(Texts.prefixed("&cВы не вампир."));
            return;
        }
        VampireData data = plugin.getVampireManager().getVampireData(player);
        if (data.isOnCooldown("self_collect")) {
            player.sendMessage(Texts.prefixed("&eСбор крови на перезарядке: " + (data.cooldownLeftMillis("self_collect") / 1000.0) + " сек."));
            return;
        }
        if (data.blood() < 20.0) {
            player.sendMessage(Texts.prefixed("&cНедостаточно крови для сбора. Нужно 20."));
            return;
        }
        var offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.GLASS_BOTTLE) {
            player.sendMessage(Texts.prefixed("&eДержите пустую бутылочку в левой руке."));
            return;
        }
        offhand.setAmount(offhand.getAmount() - 1);
        if (offhand.getAmount() <= 0) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        }
        CustomItemManager.BloodBottleType type = plugin.getVampireManager().isLeader(player)
                ? CustomItemManager.BloodBottleType.LORD
                : CustomItemManager.BloodBottleType.VAMPIRIC;

        long cooldownMillis = plugin.getVampireManager().isLeader(player) ? 3_600_000L : 180_000L;
        VampireData updated = data.withBlood(data.blood() - 20.0, plugin.getConfig().getDouble("vampire.max-blood", 100.0))
                .withCooldown("self_collect", System.currentTimeMillis() + cooldownMillis);
        plugin.getVampireManager().setVampireData(player, updated);
        player.damage(2.0);

        player.getInventory().addItem(plugin.getCustomItemManager().createBloodBottle(type, player.getName()));
        player.sendMessage(Texts.prefixed("&aСобрана кровь: -20 крови, -2 HP."));
    }

    private void handleRemoveRace(Player sender, String[] args) {
        if (!sender.hasPermission("vampire.admin") && !sender.isOp()) {
            sender.sendMessage(Texts.prefixed("&cНедостаточно прав."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Texts.prefixed("&eИспользование: /vampire remove <ник>"));
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Texts.prefixed("&cИгрок не найден."));
            return;
        }
        plugin.getVampireManager().removeVampire(target);
        sender.sendMessage(Texts.prefixed("&aРасса игрока " + target.getName() + " удалена."));
        target.sendMessage(Texts.prefixed("&cВаша раса вампира удалена администратором."));
    }

    private void handleSetLeader(Player sender, String[] args) {
        if (!sender.hasPermission("vampire.admin") && !sender.isOp()) {
            sender.sendMessage(Texts.prefixed("&cНедостаточно прав."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Texts.prefixed("&eИспользуйте /vampire setleader <ник>"));
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Texts.prefixed("&cИгрок не найден."));
            return;
        }
        plugin.getVampireManager().setLeader(target, true);
    }

    private void handleSetOverlord(Player sender, String[] args) {
        if (!sender.hasPermission("vampire.admin") && !sender.isOp()) {
            sender.sendMessage(Texts.prefixed("&cНедостаточно прав."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Texts.prefixed("&eИспользуйте /vampire setoverlord <ник>"));
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Texts.prefixed("&cИгрок не найден."));
            return;
        }
        plugin.getVampireManager().setOverlord(target, true);
        sender.sendMessage(Texts.prefixed("&aИгрок " + target.getName() + " назначен Владыкой вампиров."));
        target.sendMessage(Texts.prefixed("&5Вы стали Владыкой вампиров."));
    }

    private void handleSetStage(Player sender, String[] args) {
        if (!sender.hasPermission("vampire.admin") && !sender.isOp()) {
            sender.sendMessage(Texts.prefixed("&cНедостаточно прав."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Texts.prefixed("&eИспользование: /vampire stage <ник> <1|2|3>"));
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            return;
        }
        plugin.getVampireManager().setInfectionStage(target, Integer.parseInt(args[2]));
    }

    private void handleTurnNow(Player sender, String[] args) {
        if (!sender.hasPermission("vampire.admin") && !sender.isOp()) {
            sender.sendMessage(Texts.prefixed("&cНедостаточно прав."));
            return;
        }
        if (args.length < 2) {
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            return;
        }
        plugin.getVampireManager().turnToVampireNow(target);
    }

    private void handleInfect(Player source) {
        if (!plugin.getVampireManager().isLeader(source)) {
            source.sendMessage(Texts.prefixed("&cТолько Глава может обращать других."));
            return;
        }
        Entity targetEntity = source.getTargetEntity(5);
        if (!(targetEntity instanceof Player target)) {
            return;
        }
        plugin.getVampireManager().startInfection(source, target);
    }

    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("vampire.admin") && !player.isOp()) {
            player.sendMessage(Texts.prefixed("&cНедостаточно прав."));
            return;
        }
        if (args.length < 2) {
            return;
        }
        if (args[1].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            player.sendMessage(Texts.prefixed("&aКонфиг перезагружен."));
        }
    }

    private void handleGodMode(Player player, String[] args) {
        if (!player.hasPermission("vampire.admin") && !player.isOp()) {
            player.sendMessage(Texts.prefixed("&cНедостаточно прав."));
            return;
        }
        if (args.length < 2) {
            return;
        }
        plugin.getVampireManager().setAdminMode(player, args[1].equalsIgnoreCase("on"));
    }

    private void handleNightVision(Player player, String[] args) {
        if (!plugin.getVampireManager().isVampire(player)) {
            player.sendMessage(Texts.prefixed("&cТолько вампир может переключать режим."));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Texts.prefixed("&eИспользуйте /vampire nightvision <on|off>"));
            return;
        }
        boolean enabled = args[1].equalsIgnoreCase("on");
        plugin.getVampireManager().setNightVisionEnabled(player, enabled);
        player.sendMessage(Texts.prefixed(enabled ? "&aНочное зрение включено." : "&cНочное зрение выключено."));
    }
}