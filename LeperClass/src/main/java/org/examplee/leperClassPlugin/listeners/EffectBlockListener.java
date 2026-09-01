package org.examplee.leperClassPlugin.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class EffectBlockListener
implements Listener {
    private final LeperClassPlugin plugin;

    public EffectBlockListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEffect(EntityPotionEffectEvent e) {
        boolean isStage2;
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player p = (Player)entity;
        boolean isLeper = this.plugin.data.isLeper(p);
        boolean bl = isStage2 = this.plugin.data.getInfectionStage(p) == 2;
        if (!isLeper && !isStage2) {
            return;
        }
        PotionEffect ne = e.getNewEffect();
        if (ne != null && this.plugin.effects.FIRE_RES != null && ne.getType().equals(this.plugin.effects.FIRE_RES)) {
            e.setCancelled(true);
        }
    }
}

