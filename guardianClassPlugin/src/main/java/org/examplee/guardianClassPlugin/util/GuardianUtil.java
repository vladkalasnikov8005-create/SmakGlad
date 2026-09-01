package org.examplee.guardianClassPlugin.util;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

public final class GuardianUtil {
    private GuardianUtil() {}

    public static final Set<Material> MEAT = EnumSet.of(
            Material.BEEF, Material.COOKED_BEEF,
            Material.PORKCHOP, Material.COOKED_PORKCHOP,
            Material.CHICKEN, Material.COOKED_CHICKEN,
            Material.MUTTON, Material.COOKED_MUTTON,
            Material.RABBIT, Material.COOKED_RABBIT,
            Material.ROTTEN_FLESH,
            Material.COD, Material.COOKED_COD,
            Material.SALMON, Material.COOKED_SALMON,
            Material.TROPICAL_FISH,
            Material.PUFFERFISH,
            Material.RABBIT_STEW
    );

    public static boolean isDay(World w) {
        if (w.getEnvironment() != World.Environment.NORMAL) return false;
        long t = w.getTime();
        return t >= 0 && t <= 12300;
    }

    public static void giveOrDrop(Player p, ItemStack it) {
        HashMap<Integer, ItemStack> left = p.getInventory().addItem(it);
        for (ItemStack rem : left.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), rem);
        }
    }

    public static void spawnWaterBeam(World w, Location from, Location to) {
        Vector diff = to.toVector().subtract(from.toVector());
        double len = diff.length();
        if (len <= 0.1) return;

        int points = Math.min(40, Math.max(8, (int) (len * 6)));
        Vector step = diff.multiply(1.0 / points);

        Location cur = from.clone();
        for (int i = 0; i < points; i++) {
            w.spawnParticle(Particle.SPLASH, cur, 1, 0, 0, 0, 0.0);
            cur.add(step);
        }
    }

    public static RayTraceResult rayTrace(World w, Location eye, Vector dir, double maxDist, Player self) {
        return w.rayTrace(
                eye,
                dir,
                maxDist,
                FluidCollisionMode.ALWAYS,
                true,
                0.35,
                e -> (e instanceof LivingEntity le) && !le.getUniqueId().equals(self.getUniqueId())
        );
    }

    public static boolean hasBlockNearby(Location center, int radius, int yRadius, int step, java.util.function.Predicate<Block> pred) {
        World w = center.getWorld();
        if (w == null) return false;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radius; x <= radius; x += step) {
            for (int z = -radius; z <= radius; z += step) {
                for (int y = -yRadius; y <= yRadius; y += step) {
                    Block b = w.getBlockAt(cx + x, cy + y, cz + z);
                    if (pred.test(b)) return true;
                }
            }
        }
        return false;
    }
}
