package org.examplee.leperClassPlugin.listeners;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class ItemMigrationListener
implements Listener {
    private static final long RESCAN_COOLDOWN_MS = 3000L;
    private final LeperClassPlugin plugin;
    private final Map<UUID, Long> lastScan = new ConcurrentHashMap<UUID, Long>();

    public ItemMigrationListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        this.migratePlayerNow(e.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        this.lastScan.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onClick(InventoryClickEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (humanEntity instanceof Player) {
            Player p = (Player)humanEntity;
            this.queueMigrate(p);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onDrag(InventoryDragEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (humanEntity instanceof Player) {
            Player p = (Player)humanEntity;
            this.queueMigrate(p);
        }
    }

    private void queueMigrate(Player p) {
        long last;
        long now = System.currentTimeMillis();
        if (now - (last = this.lastScan.getOrDefault(p.getUniqueId(), 0L).longValue()) < 3000L) {
            return;
        }
        this.lastScan.put(p.getUniqueId(), now);
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.migratePlayerNow(p));
    }

    private void migratePlayerNow(Player p) {
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
        if (changed > 0) {
            this.plugin.log.info("Item migration updated slots=" + changed + " player=" + p.getName());
        }
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
        if (it == null) {
            return 0;
        }
        ItemMeta meta = it.getItemMeta();
        if (meta == null) {
            return 0;
        }
        return (Integer)meta.getPersistentDataContainer().getOrDefault(this.plugin.keys.itemVersionKey, PersistentDataType.INTEGER, (Object)0);
    }
}

