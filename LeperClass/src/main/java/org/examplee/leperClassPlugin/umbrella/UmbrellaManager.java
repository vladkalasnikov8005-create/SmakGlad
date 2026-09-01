package org.examplee.leperClassPlugin.umbrella;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.util.Compat;

public final class UmbrellaManager {
    private final LeperClassPlugin plugin;
    private final Map<UUID, Integer> remainingCache = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> loreDisplayCache = new ConcurrentHashMap<UUID, Integer>();

    public UmbrellaManager(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasUmbrellaInOffhand(Player p) {
        ItemStack off = p.getInventory().getItemInOffHand();
        this.migrateUmbrellaItem(off);
        return this.plugin.tags.isUmbrella(off);
    }

    public boolean isTracking(Player p) {
        return this.remainingCache.containsKey(p.getUniqueId());
    }

    public void resetCarry(Player p) {
    }

    public void damageUmbrellaInOffhand(Player p) {
        ItemStack off = p.getInventory().getItemInOffHand();
        this.migrateUmbrellaItem(off);
        if (!this.plugin.tags.isUmbrella(off)) {
            this.remainingCache.remove(p.getUniqueId());
            return;
        }
        UUID id = p.getUniqueId();
        int remaining = this.remainingCache.computeIfAbsent(id, x -> this.readRemainingFromItem(off));
        if (--remaining <= 0) {
            this.remainingCache.remove(id);
            p.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            p.playSound(p.getLocation(), Compat.soundFirst("ENTITY_ITEM_BREAK", "BLOCK_ANVIL_BREAK"), 1.0f, 1.0f);
            return;
        }
        this.remainingCache.put(id, remaining);
        int lastShown = this.loreDisplayCache.getOrDefault(id, -1);
        if (lastShown == -1 || Math.abs(lastShown - remaining) >= 30 || remaining <= 30) {
            this.loreDisplayCache.put(id, remaining);
            this.updateUmbrellaLore(off, remaining);
        }
    }

    public void flushOffhand(Player p) {
        Integer remaining = this.remainingCache.get(p.getUniqueId());
        if (remaining == null) {
            return;
        }
        ItemStack off = p.getInventory().getItemInOffHand();
        if (!this.plugin.tags.isUmbrella(off)) {
            this.remainingCache.remove(p.getUniqueId());
            return;
        }
        ItemMeta meta = off.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int tier = (Integer)pdc.getOrDefault(this.plugin.keys.umbrellaTierKey, PersistentDataType.INTEGER, (Object)1);
        pdc.set(this.plugin.keys.umbrellaRemainingKey, PersistentDataType.INTEGER, (Object)remaining);
        meta.setLore(Arrays.asList("\u00a77\u0414\u0435\u0440\u0436\u0438 \u0432 \u043b\u0435\u0432\u043e\u0439 \u0440\u0443\u043a\u0435", "\u00a77\u0417\u0430\u0449\u0438\u0449\u0430\u0435\u0442 \u043e\u0442 \u0441\u043e\u043b\u043d\u0446\u0430", "\u00a77\u0423\u0440\u043e\u0432\u0435\u043d\u044c: " + tier, "\u00a77\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: " + this.formatSeconds(remaining)));
        off.setItemMeta(meta);
    }

    public void forget(Player p) {
        this.remainingCache.remove(p.getUniqueId());
    }

    public void flushAllOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.flushOffhand(p);
        }
    }

    private int readRemainingFromItem(ItemStack off) {
        this.migrateUmbrellaItem(off);
        ItemMeta meta = off.getItemMeta();
        if (meta == null) {
            return 0;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int lifetime = (Integer)pdc.getOrDefault(this.plugin.keys.umbrellaLifetimeKey, PersistentDataType.INTEGER, (Object)600);
        return (Integer)pdc.getOrDefault(this.plugin.keys.umbrellaRemainingKey, PersistentDataType.INTEGER, (Object)lifetime);
    }

    public boolean migrateUmbrellaItem(ItemStack it) {
        if (!this.plugin.tags.isUmbrella(it)) {
            return false;
        }
        ItemMeta meta = it.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int tier = (Integer)pdc.getOrDefault(this.plugin.keys.umbrellaTierKey, PersistentDataType.INTEGER, (Object)1);
        int lifetime = (Integer)pdc.getOrDefault(this.plugin.keys.umbrellaLifetimeKey, PersistentDataType.INTEGER, (Object)600);
        if (lifetime >= 10000) {
            tier = 3;
            lifetime = 3000;
        } else if (lifetime >= 5000) {
            tier = 2;
            lifetime = 1500;
        } else if (lifetime >= 1700 && tier <= 1) {
            tier = 1;
            lifetime = 600;
        } else if (lifetime <= 200 && tier <= 0) {
            tier = 0;
            lifetime = 150;
        } else {
            lifetime = this.lifetimeByTier(tier);
        }
        int remaining = (Integer)pdc.getOrDefault(this.plugin.keys.umbrellaRemainingKey, PersistentDataType.INTEGER, (Object)lifetime);
        remaining = Math.max(0, Math.min(remaining, lifetime));
        boolean changed = false;
        if (!"\u0417\u043e\u043d\u0442".equals(meta.getDisplayName())) {
            meta.setDisplayName("\u0417\u043e\u043d\u0442");
            changed = true;
        }
        pdc.set(this.plugin.keys.umbrellaTierKey, PersistentDataType.INTEGER, (Object)tier);
        pdc.set(this.plugin.keys.umbrellaLifetimeKey, PersistentDataType.INTEGER, (Object)lifetime);
        pdc.set(this.plugin.keys.umbrellaRemainingKey, PersistentDataType.INTEGER, (Object)remaining);
        changed = true;
        meta.setLore(Arrays.asList("\u00a77\u0414\u0435\u0440\u0436\u0438 \u0432 \u043b\u0435\u0432\u043e\u0439 \u0440\u0443\u043a\u0435", "\u00a77\u0417\u0430\u0449\u0438\u0449\u0430\u0435\u0442 \u043e\u0442 \u0441\u043e\u043b\u043d\u0446\u0430", "\u00a77\u0423\u0440\u043e\u0432\u0435\u043d\u044c: " + tier, "\u00a77\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: " + this.formatSeconds(remaining)));
        it.setItemMeta(meta);
        return changed;
    }

    private void updateUmbrellaLore(ItemStack it, int remaining) {
        if (it == null || !this.plugin.tags.isUmbrella(it)) {
            return;
        }
        ItemMeta meta = it.getItemMeta();
        if (meta == null) {
            return;
        }
        int tier = (Integer)meta.getPersistentDataContainer().getOrDefault(this.plugin.keys.umbrellaTierKey, PersistentDataType.INTEGER, (Object)1);
        meta.setLore(Arrays.asList("\u00a77\u0414\u0435\u0440\u0436\u0438 \u0432 \u043b\u0435\u0432\u043e\u0439 \u0440\u0443\u043a\u0435", "\u00a77\u0417\u0430\u0449\u0438\u0449\u0430\u0435\u0442 \u043e\u0442 \u0441\u043e\u043b\u043d\u0446\u0430", "\u00a77\u0423\u0440\u043e\u0432\u0435\u043d\u044c: " + tier, "\u00a77\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: " + this.formatSeconds(remaining)));
        it.setItemMeta(meta);
    }

    private int lifetimeByTier(int tier) {
        if (tier <= 0) {
            return 150;
        }
        if (tier == 1) {
            return 600;
        }
        if (tier == 2) {
            return 1500;
        }
        return 3000;
    }

    private String formatSeconds(int total) {
        int s = Math.max(0, total);
        int min = s / 60;
        int sec = s % 60;
        return String.format("%02d:%02d", min, sec);
    }
}

