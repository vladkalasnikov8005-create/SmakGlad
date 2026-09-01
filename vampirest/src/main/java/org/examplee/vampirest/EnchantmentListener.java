package org.examplee.vampirest;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class EnchantmentListener implements Listener {

    private final VampireRacePlugin plugin;

    public EnchantmentListener(VampireRacePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (!isChestplate(item.getType())) {
            return;
        }

        double chance = plugin.getConfig().getDouble("garlicness.table-chance", 0.02);
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }

        int level = ThreadLocalRandom.current().nextInt(1, 4);
        plugin.getCustomItemManager().setGarlicnessLevel(item, level);
        event.getEnchanter().sendMessage(Texts.prefixed("&aНаложена Чесночность " + level + "."));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player owner = event.getPlayer();
        int level = plugin.getCustomItemManager().getGarlicnessLevel(owner.getInventory().getChestplate());
        if (level <= 0) {
            return;
        }
        int radius = plugin.getConfig().getInt("garlicness.level-" + level + ".radius", GarlicnessEnchantment.radiusForLevel(level));
        for (Player near : owner.getWorld().getPlayers()) {
            if (near.equals(owner) || !plugin.getVampireManager().isVampire(near)) {
                continue;
            }
            if (near.getLocation().distanceSquared(owner.getLocation()) > radius * radius) {
                continue;
            }
            GarlicnessEnchantment.applyToVampire(near, level);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack base = event.getInventory().getFirstItem();
        ItemStack addition = event.getInventory().getSecondItem();
        if (base == null || addition == null) {
            return;
        }
        if (!isChestplate(base.getType())) {
            return;
        }
        if (!plugin.getCustomItemManager().isGarlicBook(addition)) {
            return;
        }

        int bookLevel = plugin.getCustomItemManager().getGarlicBookLevel(addition);
        if (bookLevel <= 0) {
            return;
        }

        int currentLevel = plugin.getCustomItemManager().getGarlicnessLevel(base);
        int resultLevel;
        if (currentLevel <= 0) {
            resultLevel = bookLevel;
        } else if (currentLevel == bookLevel) {
            resultLevel = Math.min(3, currentLevel + 1);
        } else {
            resultLevel = Math.max(currentLevel, bookLevel);
        }

        ItemStack result = base.clone();
        result.setAmount(1);
        plugin.getCustomItemManager().setGarlicnessLevel(result, resultLevel);
        event.setResult(result);
        event.getInventory().setRepairCost(Math.max(1, resultLevel * 2));
    }

    private boolean isChestplate(Material material) {
        return material == Material.LEATHER_CHESTPLATE
                || material == Material.CHAINMAIL_CHESTPLATE
                || material == Material.IRON_CHESTPLATE
                || material == Material.GOLDEN_CHESTPLATE
                || material == Material.DIAMOND_CHESTPLATE
                || material == Material.NETHERITE_CHESTPLATE;
    }
}