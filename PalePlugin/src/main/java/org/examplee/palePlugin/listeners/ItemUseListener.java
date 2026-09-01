package org.examplee.palePlugin.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.examplee.palePlugin.PalePlugin;

public final class ItemUseListener
implements Listener {
    private final PalePlugin plugin;
    private final Map<UUID, Long> lastSaltUse = new HashMap<UUID, Long>();
    private final Map<UUID, Long> lastWandUse = new HashMap<UUID, Long>();

    public ItemUseListener(PalePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSaltUse(PlayerInteractEvent e) {
        long last;
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = e.getItem();
        if (!this.plugin.items.isSalt(item)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - (last = this.lastSaltUse.getOrDefault(e.getPlayer().getUniqueId(), 0L).longValue()) < this.plugin.cfg.saltCooldownMs) {
            e.setCancelled(true);
            return;
        }
        this.lastSaltUse.put(e.getPlayer().getUniqueId(), now);
        Location center = e.getClickedBlock() != null ? e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5) : e.getPlayer().getLocation();
        int cleaned = this.plugin.engine.cleanse(center, this.plugin.cfg.saltRadius);
        if (cleaned > 0) {
            this.plugin.spread.addCleansed(cleaned);
        }
        this.consumeOne(e.getPlayer(), item);
        e.setCancelled(true);
    }

    @EventHandler
    public void onHolyWaterSplash(PotionSplashEvent e) {
        ItemStack item = e.getPotion().getItem();
        if (!this.plugin.items.isHolyWater(item)) {
            return;
        }
        int cleaned = this.plugin.engine.cleanse(e.getPotion().getLocation(), this.plugin.cfg.holyWaterRadius);
        if (cleaned > 0) {
            this.plugin.spread.addCleansed(cleaned);
        }
    }

    @EventHandler
    public void onPurifierFlintUse(PlayerInteractEvent e) {
        int usesLeft;
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (e.getClickedBlock() == null) {
            return;
        }
        ItemStack item = e.getItem();
        if (!this.plugin.items.isPurifierFlint(item)) {
            return;
        }
        Block clicked = e.getClickedBlock();
        if (!this.plugin.engine.infectedTypes().contains(clicked.getType()) && !this.plugin.engine.hasInfectedNear(clicked)) {
            e.setCancelled(true);
            return;
        }
        int cleaned = this.plugin.engine.cleanse(clicked.getLocation().add(0.5, 0.5, 0.5), this.plugin.cfg.purifierFlintRadius);
        if (cleaned > 0) {
            this.plugin.spread.addCleansed(cleaned);
        }
        if ((usesLeft = this.plugin.items.getPurifierFlintUses(item) - 1) <= 0) {
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            e.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            this.plugin.items.setPurifierFlintUses(item, usesLeft);
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.2f);
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onMapUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = e.getItem();
        if (!this.plugin.items.isInfectionMap(item)) {
            return;
        }
        int r = this.plugin.items.getMapRadius(item);
        r = Math.max(1, Math.min(this.plugin.cfg.mapMaxRadiusChunks, r));
        this.plugin.engine.sendMap(e.getPlayer(), r);
        e.setCancelled(true);
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent e) {
        Location center;
        int infected;
        long last;
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = e.getItem();
        if (!this.plugin.items.isInfectWand(item)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - (last = this.lastWandUse.getOrDefault(e.getPlayer().getUniqueId(), 0L).longValue()) < this.plugin.cfg.infectWandCooldownMs) {
            e.setCancelled(true);
            return;
        }
        this.lastWandUse.put(e.getPlayer().getUniqueId(), now);
        Block centerBlock = e.getClickedBlock();
        if (centerBlock == null) {
            try {
                centerBlock = e.getPlayer().getTargetBlockExact(40);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        if ((infected = this.plugin.engine.infectAreaWand(center = centerBlock != null ? centerBlock.getLocation().add(0.5, 0.5, 0.5) : e.getPlayer().getLocation())) > 0) {
            this.plugin.spread.addInfected(infected);
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.8f, 1.15f);
        } else {
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 0.6f);
        }
        int usesLeft = this.plugin.items.getInfectWandUses(item) - 1;
        if (usesLeft <= 0) {
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            e.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            this.plugin.items.setInfectWandUses(item, usesLeft);
        }
        e.setCancelled(true);
    }

    private void consumeOne(Player p, ItemStack it) {
        if (p.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        int amt = it.getAmount() - 1;
        if (amt <= 0) {
            p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            it.setAmount(amt);
        }
    }
}

