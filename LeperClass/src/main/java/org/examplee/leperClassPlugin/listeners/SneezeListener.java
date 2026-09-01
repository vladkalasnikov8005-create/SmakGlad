package org.examplee.leperClassPlugin.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class SneezeListener
implements Listener {
    private final LeperClassPlugin plugin;

    public SneezeListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSneezeHit(ProjectileHitEvent e) {
        Projectile pr = e.getEntity();
        Byte v = (Byte)pr.getPersistentDataContainer().get(this.plugin.keys.sneezeProjectileKey, PersistentDataType.BYTE);
        if (v == null || v != 1) {
            return;
        }
        Entity entity = e.getHitEntity();
        if (entity instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)entity;
            if (this.plugin.effects.POISON != null) {
                le.addPotionEffect(new PotionEffect(this.plugin.effects.POISON, this.plugin.settings.sneezePoisonTicks, 0));
            }
            if (this.plugin.effects.SLOW != null) {
                le.addPotionEffect(new PotionEffect(this.plugin.effects.SLOW, this.plugin.settings.sneezeSlowTicks, 2));
            }
            if (this.plugin.effects.WEAKNESS != null) {
                le.addPotionEffect(new PotionEffect(this.plugin.effects.WEAKNESS, this.plugin.settings.sneezeWeakTicks, 1));
            }
            if (this.plugin.effects.BLINDNESS != null) {
                le.addPotionEffect(new PotionEffect(this.plugin.effects.BLINDNESS, this.plugin.settings.sneezeBlindTicks, 0));
            }
        }
        pr.remove();
    }
}

