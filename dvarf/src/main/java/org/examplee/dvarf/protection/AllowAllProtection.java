package org.examplee.dvarf.protection;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class AllowAllProtection implements BuildProtection {
    @Override
    public boolean canBuild(Player player, Location location) {
        return true;
    }
}