package org.examplee.leperClassPlugin.listeners;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class CombatListener
implements Listener {
    private static final double NON_FIRE_DAMAGE_MULT = 0.05;
    private static final double FIRE_DAMAGE_MULT = 4.0;
    private static final Set<EntityDamageEvent.DamageCause> FIRE_CAUSES = EnumSet.of(EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK, EntityDamageEvent.DamageCause.LAVA, EntityDamageEvent.DamageCause.HOT_FLOOR);
    private final LeperClassPlugin plugin;

    public CombatListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDamage(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player p = (Player)entity;
        if (!this.plugin.data.isLeper(p)) {
            return;
        }
        if (FIRE_CAUSES.contains(e.getCause())) {
            e.setDamage(e.getDamage() * 4.0);
        } else {
            e.setDamage(e.getDamage() * 0.05);
        }
    }
}

