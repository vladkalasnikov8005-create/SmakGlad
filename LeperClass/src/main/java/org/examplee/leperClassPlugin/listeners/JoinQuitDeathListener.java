package org.examplee.leperClassPlugin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class JoinQuitDeathListener
implements Listener {
    private final LeperClassPlugin plugin;

    public JoinQuitDeathListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (this.plugin.data.isLeper(p) && this.plugin.effects.WATER_BREATHING != null) {
            p.addPotionEffect(new PotionEffect(this.plugin.effects.WATER_BREATHING, 300, 0, false, false, false));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        this.plugin.umbrella.resetCarry(e.getPlayer());
        this.plugin.movementLock.release(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        this.plugin.umbrella.resetCarry(e.getEntity());
        this.plugin.movementLock.release(e.getEntity());
    }
}

