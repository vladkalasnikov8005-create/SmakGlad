package org.examplee.dvarf.protection;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface BuildProtection {
    boolean canBuild(Player player, Location location);
}