package org.examplee.palePlugin.util;

import java.util.Locale;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;

public final class BiomeUtil {
    private BiomeUtil() {
    }

    public static NamespacedKey parseBiomeKey(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.isEmpty()) {
            return null;
        }
        NamespacedKey direct = NamespacedKey.fromString((String)v.toLowerCase(Locale.ROOT));
        if (direct != null) {
            return direct;
        }
        return NamespacedKey.fromString((String)("minecraft:" + v.toLowerCase(Locale.ROOT)));
    }

    public static NamespacedKey biomeKeyOf(Biome biome) {
        if (biome == null) {
            return null;
        }
        if (biome instanceof Keyed) {
            Biome keyed = biome;
            return keyed.getKey();
        }
        return NamespacedKey.fromString((String)("minecraft:" + biome.toString().toLowerCase(Locale.ROOT)));
    }
}

