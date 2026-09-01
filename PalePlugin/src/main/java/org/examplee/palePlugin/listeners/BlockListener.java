package org.examplee.palePlugin.listeners;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.examplee.palePlugin.PalePlugin;

public final class BlockListener
implements Listener {
    private final PalePlugin plugin;

    public BlockListener(PalePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (this.plugin.engine.infectedTypes().contains(b.getType())) {
            this.plugin.engine.sources(b.getWorld()).add(b.getX(), b.getY(), b.getZ());
        }
        if (this.plugin.items.isWard(e.getItemInHand())) {
            if (!e.getPlayer().hasPermission("pale.use") && !e.getPlayer().hasPermission("pale.admin")) {
                e.setCancelled(true);
                return;
            }
            this.plugin.engine.wards(b.getWorld()).add(b.getX(), b.getY(), b.getZ());
            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
            b.getWorld().spawnParticle(Particle.END_ROD, b.getLocation().add(0.5, 1.0, 0.5), 18, 0.35, 0.6, 0.35, 0.01);
            this.plugin.wardsStorage.save(this.plugin.engine);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        boolean removed;
        Block b = e.getBlock();
        if (this.plugin.engine.infectedTypes().contains(b.getType())) {
            this.plugin.engine.sources(b.getWorld()).remove(b.getX(), b.getY(), b.getZ());
        }
        if (removed = this.plugin.engine.wards(b.getWorld()).remove(b.getX(), b.getY(), b.getZ())) {
            this.plugin.wardsStorage.save(this.plugin.engine);
            e.setDropItems(false);
            b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), this.plugin.items.makeWard(1));
        }
    }
}

