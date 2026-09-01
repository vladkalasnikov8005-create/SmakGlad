package org.examplee.leperClassPlugin.util;

import java.util.HashMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class InventoryUtil {
    private InventoryUtil() {
    }

    public static void giveOrDrop(Player p, ItemStack it) {
        HashMap<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack[]{it});
        for (ItemStack rem : left.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), rem);
        }
    }
}

