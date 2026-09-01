package org.examplee.leperClassPlugin.util;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

public final class Compat {
    private Compat() {
    }

    public static Material materialFirst(String ... names) {
        for (String n : names) {
            Material m = Material.getMaterial((String)n);
            if (m == null) continue;
            return m;
        }
        return Material.STONE;
    }

    public static Sound soundFirst(String ... names) {
        for (String n : names) {
            try {
                Object v = Sound.class.getField(n).get(null);
                if (!(v instanceof Sound)) continue;
                Sound s = (Sound)v;
                return s;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return Sound.BLOCK_ANVIL_USE;
    }

    public static Particle particleFirst(String ... names) {
        for (String n : names) {
            try {
                return Particle.valueOf((String)n);
            }
            catch (Throwable throwable) {
            }
        }
        Particle[] all = Particle.values();
        return all.length > 0 ? all[0] : null;
    }

    public static PotionEffectType effect(String name) {
        return PotionEffectType.getByName((String)name);
    }

    public static PotionEffectType effectFirst(String ... names) {
        for (String n : names) {
            PotionEffectType t = PotionEffectType.getByName((String)n);
            if (t == null) continue;
            return t;
        }
        return null;
    }
}

