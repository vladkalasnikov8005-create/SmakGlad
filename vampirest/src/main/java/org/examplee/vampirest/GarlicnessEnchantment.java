package org.examplee.vampirest;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class GarlicnessEnchantment {

    private GarlicnessEnchantment() {
    }

    public static int radiusForLevel(int level) {
        return switch (level) {
            case 1 -> 3;
            case 2 -> 5;
            default -> 7;
        };
    }

    public static void applyToVampire(Player vampire, int level) {
        if (level <= 0) {
            return;
        }

        if (level == 1) {
            vampire.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 5, 0, true, false, false));
            vampire.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 10, 0, true, false, false));
        } else if (level == 2) {
            vampire.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 6, 0, true, false, false));
            vampire.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 11, 1, true, false, false));
        } else {
            vampire.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 7, 0, true, false, false));
            vampire.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 12, 1, true, false, false));
            vampire.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 3, 0, true, false, false));
        }
    }
}