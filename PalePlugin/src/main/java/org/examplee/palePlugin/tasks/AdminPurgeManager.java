package org.examplee.palePlugin.tasks;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.examplee.palePlugin.PalePlugin;

public final class AdminPurgeManager {
    private final PalePlugin plugin;
    private final Map<UUID, BukkitTask> tasksByPlayer = new HashMap<UUID, BukkitTask>();

    public AdminPurgeManager(PalePlugin plugin) {
        this.plugin = plugin;
    }

    public void stopAll() {
        for (BukkitTask t : this.tasksByPlayer.values()) {
            t.cancel();
        }
        this.tasksByPlayer.clear();
    }

    public void start(Player p, Location center) {
        World w = center.getWorld();
        if (w == null) {
            return;
        }
        BukkitTask old = this.tasksByPlayer.remove(p.getUniqueId());
        if (old != null) {
            old.cancel();
        }
        int cx = center.getBlockX() >> 4;
        int cz = center.getBlockZ() >> 4;
        ArrayDeque<ChunkPos> q = new ArrayDeque<ChunkPos>();
        int r = this.plugin.cfg.adminPurgeRadiusChunks;
        for (int dz = -r; dz <= r; ++dz) {
            for (int dx = -r; dx <= r; ++dx) {
                q.addLast(new ChunkPos(cx + dx, cz + dz));
            }
        }
        p.sendMessage(String.valueOf(ChatColor.GREEN) + "[Pale] \u0417\u0430\u043f\u0443\u0449\u0435\u043d\u0430 \u0430\u0434\u043c\u0438\u043d-\u043e\u0447\u0438\u0441\u0442\u043a\u0430: r=" + r + " \u0447\u0430\u043d\u043a\u043e\u0432");
        long[] cleanedTotal = new long[]{0L};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            ChunkPos pos;
            int processed = 0;
            while (processed < this.plugin.cfg.adminPurgeChunksPerTick && !q.isEmpty() && (pos = (ChunkPos)q.pollFirst()) != null) {
                if (this.plugin.cfg.adminPurgeOnlyLoadedChunks && !w.isChunkLoaded(pos.x, pos.z)) continue;
                if (!this.plugin.cfg.adminPurgeOnlyLoadedChunks) {
                    try {
                        w.getChunkAt(pos.x, pos.z);
                    }
                    catch (Exception exception) {}
                } else if (!w.isChunkLoaded(pos.x, pos.z)) continue;
                int cleaned = this.plugin.engine.purgeChunkSurface(w, pos.x, pos.z, this.plugin.cfg.adminPurgeDepth);
                cleanedTotal[0] = cleanedTotal[0] + (long)cleaned;
                if (cleaned > 0) {
                    this.plugin.spread.addCleansed(cleaned);
                }
                ++processed;
            }
            if (q.isEmpty()) {
                BukkitTask t = this.tasksByPlayer.remove(p.getUniqueId());
                if (t != null) {
                    t.cancel();
                }
                p.sendMessage(String.valueOf(ChatColor.YELLOW) + "[Pale] \u041e\u0447\u0438\u0441\u0442\u043a\u0430 \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u0430. \u0423\u0434\u0430\u043b\u0435\u043d\u043e \u0431\u043b\u043e\u043a\u043e\u0432: " + cleanedTotal[0]);
            }
        }, 1L, 1L);
        this.tasksByPlayer.put(p.getUniqueId(), task);
    }

    private static final class ChunkPos {
        final int x;
        final int z;

        ChunkPos(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
}

