package org.examplee.leperClassPlugin.listeners;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class MobIgnoreListener
implements Listener {
    private final LeperClassPlugin plugin;

    public MobIgnoreListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        LivingEntity livingEntity = e.getTarget();
        if (!(livingEntity instanceof Player)) {
            return;
        }
        Player p = (Player)livingEntity;
        if (!this.plugin.data.isLeper(p)) {
            return;
        }
        EntityType type = e.getEntity().getType();
        if (type == EntityType.IRON_GOLEM || type == EntityType.SNOW_GOLEM) {
            return;
        }
        if (e.getEntity() instanceof Enemy) {
            e.setCancelled(true);
        }
    }
}

