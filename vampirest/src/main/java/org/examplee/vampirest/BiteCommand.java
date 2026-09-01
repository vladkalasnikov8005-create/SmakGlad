package org.examplee.vampirest;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BiteCommand {

    private final VampireRacePlugin plugin;

    public BiteCommand(VampireRacePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player) {
        if (!plugin.getVampireManager().isVampire(player)) {
            player.sendMessage(Texts.prefixed("&cВы не вампир."));
            return;
        }

        VampireData data = plugin.getVampireManager().getVampireData(player);
        if (data.isOnCooldown("bite")) {
            player.sendMessage(Texts.prefixed("&eУкус на перезарядке: " + (data.cooldownLeftMillis("bite") / 1000.0) + " сек."));
            return;
        }

        var ray = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                plugin.getConfig().getInt("abilities.bite.range", 5),
                entity -> entity instanceof LivingEntity && entity != player
        );
        if (ray == null || !(ray.getHitEntity() instanceof LivingEntity target)) {
            player.sendMessage(Texts.prefixed("&eНет цели в радиусе."));
            return;
        }

        if (target instanceof Player targetPlayer && plugin.getVampireManager().isLeader(targetPlayer)) {
            player.sendMessage(Texts.prefixed("&4Кровь Лорда нельзя пить или выкачивать обычным укусом."));
            return;
        }

        target.damage(1.0, player);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 3, 0, true, true, true));

        boolean targetIsPlayer = target instanceof Player;
        double bloodGain = targetIsPlayer
                ? plugin.getConfig().getDouble("abilities.bite.blood-gain-player", 20.0)
                : plugin.getConfig().getDouble("abilities.bite.blood-gain-mob", 10.0);
        int foodGain = targetIsPlayer
                ? plugin.getConfig().getInt("abilities.bite.hunger-gain-player", 4)
                : plugin.getConfig().getInt("abilities.bite.hunger-gain-mob", 2);

        int newHunger = Math.min(20, data.hunger() + foodGain);
        VampireData updated = data.withBlood(data.blood() + bloodGain, plugin.getConfig().getDouble("vampire.max-blood", 100.0))
                .withHunger(newHunger)
                .withCooldown("bite", System.currentTimeMillis() + plugin.getConfig().getLong("abilities.bite.cooldown-seconds", 2L) * 1000L);

        if (target instanceof Monster) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 5, 0, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 5, 0, true, true, true));
            player.sendMessage(Texts.prefixed("&2Кровь агрессивного моба испорчена."));
        }

        if (target instanceof Player targetPlayer && plugin.getVampireManager().isVampire(targetPlayer)) {
            updated = updated.withBlood(updated.blood() + 10.0, plugin.getConfig().getDouble("vampire.max-blood", 100.0));
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 2.0));
            player.sendMessage(Texts.prefixed("&4Вы вкусили вампирскую кровь: усиленное насыщение."));
        }

        plugin.getVampireManager().setVampireData(player, updated);

        player.setFoodLevel(newHunger);
        player.sendMessage(Texts.prefixed("&aУкус успешен. &f+" + bloodGain + " &aкрови."));
        fillBottleIfPossible(player, target);
    }

    private void fillBottleIfPossible(Player player, LivingEntity target) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.GLASS_BOTTLE) {
            return;
        }

        boolean targetIsPlayer = target instanceof Player;
        boolean targetIsHostileMob = target instanceof Monster;
        boolean targetIsVampire = target instanceof Player targetPlayer && plugin.getVampireManager().isVampire(targetPlayer);
        boolean targetIsLeader = target instanceof Player targetPlayer && plugin.getVampireManager().isLeader(targetPlayer);

        if (targetIsLeader) {
            player.sendMessage(Texts.prefixed("&4Кровь Лорда можно получить только если Лорд сам использует /vampire collect."));
            return;
        }

        CustomItemManager.BloodBottleType bottleType = targetIsLeader
                ? CustomItemManager.BloodBottleType.LORD
                : targetIsVampire
                ? CustomItemManager.BloodBottleType.VAMPIRIC
                : (targetIsPlayer ? CustomItemManager.BloodBottleType.NUTRITIOUS
                : (targetIsHostileMob ? CustomItemManager.BloodBottleType.CORRUPTED : CustomItemManager.BloodBottleType.NORMAL));

        offhand.setAmount(offhand.getAmount() - 1);
        if (offhand.getAmount() <= 0) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        }
        player.getInventory().addItem(plugin.getCustomItemManager().createBloodBottle(bottleType, target.getName()));
    }
}