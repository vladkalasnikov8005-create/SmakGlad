package org.examplee.leperClassPlugin.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class InstantBrewingListener
implements Listener {
    private final LeperClassPlugin plugin;

    public InstantBrewingListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onClick(InventoryClickEvent e) {
        Inventory inventory = e.getInventory();
        if (!(inventory instanceof BrewerInventory)) {
            return;
        }
        BrewerInventory inv = (BrewerInventory)inventory;
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.handle(inv));
    }

    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onDrag(InventoryDragEvent e) {
        Inventory inventory = e.getInventory();
        if (!(inventory instanceof BrewerInventory)) {
            return;
        }
        BrewerInventory inv = (BrewerInventory)inventory;
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.handle(inv));
    }

    private void handle(BrewerInventory inv) {
        ItemStack ing = inv.getIngredient();
        if (ing == null || ing.getType() == Material.AIR) {
            return;
        }
        Material type = ing.getType();
        boolean changed = false;
        if (type == Material.NETHER_WART) {
            changed = this.convert(inv, 0);
        } else if (type == Material.BLAZE_POWDER) {
            changed = this.convert(inv, 1);
        } else if (type == Material.GLISTERING_MELON_SLICE) {
            changed = this.convert(inv, 2);
        }
        if (!changed) {
            return;
        }
        int amount = ing.getAmount() - 1;
        if (amount <= 0) {
            inv.setIngredient(null);
        } else {
            ing.setAmount(amount);
            inv.setIngredient(ing);
        }
    }

    private boolean convert(BrewerInventory inv, int mode) {
        boolean changed = false;
        block5: for (int slot = 0; slot < 3; ++slot) {
            ItemStack cur = inv.getItem(slot);
            if (cur == null || cur.getType() == Material.AIR) continue;
            switch (mode) {
                case 0: {
                    if (!this.plugin.tags.isLeperBlood(cur)) continue block5;
                    inv.setItem(slot, this.plugin.items.makeThickLeperBlood());
                    changed = true;
                    continue block5;
                }
                case 1: {
                    if (!this.plugin.tags.isThickLeperBlood(cur)) continue block5;
                    inv.setItem(slot, this.plugin.items.makeSterileLeperBlood());
                    changed = true;
                    continue block5;
                }
                case 2: {
                    if (!this.plugin.tags.isSterileLeperBlood(cur)) continue block5;
                    inv.setItem(slot, this.plugin.items.makeVaccine());
                    changed = true;
                }
            }
        }
        return changed;
    }
}

