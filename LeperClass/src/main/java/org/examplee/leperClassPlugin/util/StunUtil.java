package org.examplee.leperClassPlugin.util;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class StunUtil {
    private StunUtil() {
    }

    public static void stun(LeperClassPlugin plugin, Player p, int ticks) {
        plugin.movementLock.lock(p);
        UUID id = p.getUniqueId();
        Bukkit.getScheduler().runTaskLater((Plugin)plugin, () -> {
            Player pl = Bukkit.getPlayer((UUID)id);
            if (pl != null) {
                plugin.movementLock.unlock(pl);
            }
        }, (long)ticks);
    }
}

