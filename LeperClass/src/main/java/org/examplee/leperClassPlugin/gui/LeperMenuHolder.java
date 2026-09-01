package org.examplee.leperClassPlugin.gui;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class LeperMenuHolder
implements InventoryHolder {
    private final UUID target;

    public LeperMenuHolder(UUID target) {
        this.target = target;
    }

    public UUID getTarget() {
        return this.target;
    }

    public Inventory getInventory() {
        return null;
    }
}

