package org.examplee.leperClassPlugin.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.examplee.leperClassPlugin.core.LeperKeys;

public final class ItemTags {
    private final LeperKeys keys;

    public ItemTags(LeperKeys keys) {
        this.keys = keys;
    }

    private boolean has(ItemStack it, NamespacedKey key) {
        if (it == null || it.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = it.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte v = (Byte)meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return v != null && v == 1;
    }

    public boolean isPlagueStick(ItemStack it) {
        return this.has(it, this.keys.plagueStickKey);
    }

    public boolean isPlagueBomb(ItemStack it) {
        return this.has(it, this.keys.plagueBombKey);
    }

    public boolean isUmbrella(ItemStack it) {
        return this.has(it, this.keys.umbrellaKey);
    }

    public boolean isVaccine(ItemStack it) {
        return this.has(it, this.keys.vaccineKey);
    }

    public boolean isLeperBlood(ItemStack it) {
        return this.has(it, this.keys.leperBloodKey);
    }

    public boolean isThickLeperBlood(ItemStack it) {
        return this.has(it, this.keys.thickBloodKey);
    }

    public boolean isSterileLeperBlood(ItemStack it) {
        return this.has(it, this.keys.sterileBloodKey);
    }

    public boolean isSacrificialKnife(ItemStack it) {
        return this.has(it, this.keys.sacrificialKnifeKey);
    }
}

