package org.examplee.leperClassPlugin.core;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class ItemMigrationService {
    private final LeperClassPlugin plugin;

    public ItemMigrationService(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    public int migratePlayer(Player p) {
        if (p == null) {
            return 0;
        }
        PlayerInventory inv = p.getInventory();
        int changed = 0;
        ItemStack[] storage = inv.getStorageContents();
        for (int i = 0; i < storage.length; ++i) {
            ItemStack old = storage[i];
            storage[i] = this.migrateItem(storage[i]);
            if (storage[i] == old) continue;
            ++changed;
        }
        inv.setStorageContents(storage);
        ItemStack oldOff = inv.getItemInOffHand();
        ItemStack off = this.migrateItem(inv.getItemInOffHand());
        inv.setItemInOffHand(off);
        if (off != oldOff) {
            ++changed;
        }
        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; ++i) {
            ItemStack old = armor[i];
            armor[i] = this.migrateItem(armor[i]);
            if (armor[i] == old) continue;
            ++changed;
        }
        inv.setArmorContents(armor);
        return changed;
    }

    private ItemStack migrateItem(ItemStack it) {
        if (it == null) {
            return null;
        }
        int amount = it.getAmount();
        int version = this.itemVersion(it);
        if (version >= 2) {
            this.plugin.umbrella.migrateUmbrellaItem(it);
            return it;
        }
        if (this.plugin.tags.isVaccine(it) && it.getType() != Material.POTION) {
            ItemStack upgraded = this.plugin.items.makeVaccine();
            upgraded.setAmount(amount);
            return upgraded;
        }
        if (this.plugin.tags.isPlagueStick(it)) {
            ItemStack upgraded = this.plugin.items.makePlagueStick();
            upgraded.setAmount(amount);
            return upgraded;
        }
        if (this.plugin.tags.isPlagueBomb(it)) {
            ItemStack upgraded = this.plugin.items.makePlagueBomb();
            upgraded.setAmount(amount);
            return upgraded;
        }
        if (this.plugin.tags.isLeperBlood(it)) {
            ItemStack upgraded = this.plugin.items.makeLeperBlood();
            upgraded.setAmount(amount);
            return upgraded;
        }
        if (this.plugin.tags.isThickLeperBlood(it)) {
            ItemStack upgraded = this.plugin.items.makeThickLeperBlood();
            upgraded.setAmount(amount);
            return upgraded;
        }
        if (this.plugin.tags.isSterileLeperBlood(it)) {
            ItemStack upgraded = this.plugin.items.makeSterileLeperBlood();
            upgraded.setAmount(amount);
            return upgraded;
        }
        if (this.plugin.tags.isSacrificialKnife(it)) {
            ItemStack upgraded = this.plugin.items.makeSacrificialKnife();
            upgraded.setAmount(amount);
            return upgraded;
        }
        this.plugin.umbrella.migrateUmbrellaItem(it);
        return it;
    }

    private int itemVersion(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) {
            return 0;
        }
        return (Integer)meta.getPersistentDataContainer().getOrDefault(this.plugin.keys.itemVersionKey, PersistentDataType.INTEGER, (Object)0);
    }
}

