package org.examplee.leperClassPlugin.listeners;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.util.TextUtil;

public final class ContactInfectionListener
implements Listener {
    private final LeperClassPlugin plugin;
    private final Map<UUID, BukkitTask> pendingScare = new ConcurrentHashMap<UUID, BukkitTask>();

    public ContactInfectionListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onHitLeper(EntityDamageByEntityEvent e) {
        boolean showScare;
        Entity entity;
        Player damagerPlayer = this.resolveAttacker(e.getDamager());
        if (damagerPlayer != null && this.plugin.data.isLeper(damagerPlayer)) {
            long now = System.currentTimeMillis();
            if (this.plugin.data.getRageUntil(damagerPlayer) > now) {
                e.setDamage(e.getDamage() * 1.8);
                Entity entity2 = e.getEntity();
                if (entity2 instanceof Player) {
                    Player hit = (Player)entity2;
                    Vector dir = hit.getLocation().toVector().subtract(damagerPlayer.getLocation().toVector()).normalize();
                    hit.setVelocity(hit.getVelocity().add(dir.multiply(1.9)).setY(0.6));
                }
            }
        }
        if (!((entity = e.getEntity()) instanceof Player)) {
            return;
        }
        Player victim = (Player)entity;
        if (!this.plugin.data.isLeper(victim)) {
            return;
        }
        Player attacker = damagerPlayer;
        if (attacker == null || this.plugin.data.isLeper(attacker)) {
            return;
        }
        double hpDamage = Math.max(0.0, e.getFinalDamage());
        double chance = hpDamage * this.plugin.settings.contactInfectPerHp;
        boolean realInfection = ThreadLocalRandom.current().nextDouble() < chance;
        boolean bl = showScare = realInfection || ThreadLocalRandom.current().nextDouble() < this.plugin.settings.contactFakeScareChance;
        if (showScare && !this.pendingScare.containsKey(attacker.getUniqueId())) {
            attacker.sendMessage(TextUtil.ui(String.valueOf(ChatColor.DARK_GREEN) + "\u0412\u044b \u043c\u043e\u0433\u043b\u0438 \u0437\u0430\u0440\u0430\u0437\u0438\u0442\u044c\u0441\u044f \u043f\u043e\u0441\u043b\u0435 \u043a\u043e\u043d\u0442\u0430\u043a\u0442\u0430 \u0441 \u043f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u044b\u043c..."));
            BukkitTask task = this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                this.pendingScare.remove(attacker.getUniqueId());
                if (!attacker.isOnline() || this.plugin.data.isLeper(attacker)) {
                    return;
                }
                if (realInfection) {
                    this.plugin.infection.startInfection(attacker);
                } else {
                    attacker.sendMessage(TextUtil.ui(String.valueOf(ChatColor.GREEN) + "\u0424\u0443\u0445, \u043a\u0430\u0436\u0435\u0442\u0441\u044f \u043f\u0440\u043e\u043d\u0435\u0441\u043b\u043e. \u0412\u044b \u043d\u0435 \u0437\u0430\u0440\u0430\u0437\u0438\u043b\u0438\u0441\u044c."));
                }
            }, this.plugin.settings.contactResolveTicks);
            this.pendingScare.put(attacker.getUniqueId(), task);
        }
    }

    private Player resolveAttacker(Entity damager) {
        Projectile pr;
        ProjectileSource src;
        if (damager instanceof Player) {
            Player p = (Player)damager;
            return p;
        }
        if (damager instanceof Projectile && (src = (pr = (Projectile)damager).getShooter()) instanceof Player) {
            Player p = (Player)src;
            return p;
        }
        return null;
    }
}

