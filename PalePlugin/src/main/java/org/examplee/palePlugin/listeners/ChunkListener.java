package org.examplee.palePlugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.examplee.palePlugin.PalePlugin;

public final class ChunkListener
implements Listener {
    private final PalePlugin plugin;

    public ChunkListener(PalePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLoad(ChunkLoadEvent e) {
        this.plugin.spread.onChunkLoad(e.getWorld(), e.getChunk().getX(), e.getChunk().getZ());
    }

    @EventHandler
    public void onUnload(ChunkUnloadEvent e) {
        this.plugin.spread.onChunkUnload(e.getWorld());
    }
}

