package org.examplee.palePlugin.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.examplee.palePlugin.PalePlugin;

public final class AdminPurgeListener
implements Listener {
    private final PalePlugin plugin;

    public AdminPurgeListener(PalePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack it = e.getItem();
        if (!this.plugin.items.isAdminPurgeWand(it)) {
            return;
        }
        if (!e.getPlayer().hasPermission("pale.admin")) {
            e.setCancelled(true);
            return;
        }
        Location center = e.getClickedBlock() != null ? e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5) : e.getPlayer().getLocation();
        this.plugin.purge.start(e.getPlayer(), center);
        e.setCancelled(true);
    }
}

