package org.examplee.leperClassPlugin.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.examplee.leperClassPlugin.util.Compat;
import org.examplee.leperClassPlugin.util.TextUtil;

public final class ParticlesUtil {
    private ParticlesUtil() {
    }

    public static void greenDust(World w, Location loc, int count, double ox, double oy, double oz, float size) {
        block3: {
            if (w == null || loc == null) {
                return;
            }
            Particle dust = Compat.particleFirst("DUST", "REDSTONE", "ENTITY_EFFECT", "SPELL_MOB", "CRIT");
            try {
                w.spawnParticle(dust, loc, count, ox, oy, oz, 0.0, (Object)new Particle.DustOptions(TextUtil.C_GREEN, size));
            }
            catch (Throwable ignored) {
                Particle fallback = Compat.particleFirst("ENTITY_EFFECT", "SPELL_MOB", "CLOUD", "CRIT");
                if (fallback == null) break block3;
                w.spawnParticle(fallback, loc, Math.max(1, count / 2), ox, oy, oz, 0.0);
            }
        }
    }
}

