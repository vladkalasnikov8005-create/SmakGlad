package org.examplee.leperClassPlugin.umbrella;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class UmbrellaListener
implements Listener {
    private final LeperClassPlugin plugin;

    public UmbrellaListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        this.plugin.umbrella.flushOffhand(p);
        this.plugin.umbrella.forget(p);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        this.plugin.umbrella.flushOffhand(p);
        this.plugin.umbrella.forget(p);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onInvClick(InventoryClickEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        if (!this.plugin.umbrella.isTracking(p)) {
            return;
        }
        this.plugin.umbrella.flushOffhand(p);
        this.plugin.umbrella.forget(p);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onInvDrag(InventoryDragEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        if (!this.plugin.umbrella.isTracking(p)) {
            return;
        }
        this.plugin.umbrella.flushOffhand(p);
        this.plugin.umbrella.forget(p);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (!this.plugin.umbrella.isTracking(p)) {
            return;
        }
        this.plugin.umbrella.flushOffhand(p);
        this.plugin.umbrella.forget(p);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        this.plugin.umbrella.flushOffhand(p);
        this.plugin.umbrella.forget(p);
    }
}

