package org.examplee.leperClassPlugin.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class HungerListener
implements Listener {
    private final LeperClassPlugin plugin;

    public HungerListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onExhaust(EntityExhaustionEvent e) {
        HumanEntity humanEntity = e.getEntity();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (!this.plugin.data.isLeper(p)) {
            return;
        }
        e.setExhaustion(e.getExhaustion() * 4.0f);
    }
}

