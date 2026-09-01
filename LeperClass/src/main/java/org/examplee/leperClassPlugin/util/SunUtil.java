package org.examplee.leperClassPlugin.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public final class SunUtil {
    private SunUtil() {
    }

    public static boolean isOnPaleSurface(Player p) {
        Location loc = p.getLocation();
        Block in = loc.getBlock();
        if (in.getType().name().contains("PALE")) {
            return true;
        }
        Block under = in.getRelative(BlockFace.DOWN);
        return under.getType().name().contains("PALE");
    }

    public static boolean shouldBurnInSun(Player p) {
        World w = p.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL) {
            return false;
        }
        if (w.hasStorm() || w.isThundering()) {
            return false;
        }
        long time = w.getTime();
        if (time < 0L || time > 12300L) {
            return false;
        }
        Location loc = p.getLocation();
        int highestY = w.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
        if (loc.getBlockY() + 1 < highestY) {
            return false;
        }
        return loc.getBlock().getLightFromSky() >= 14;
    }
}

