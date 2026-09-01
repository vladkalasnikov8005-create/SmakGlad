package org.examplee.leperClassPlugin.core;

import org.bukkit.potion.PotionEffectType;
import org.examplee.leperClassPlugin.util.Compat;

public final class EffectRegistry {
    public final PotionEffectType FIRE_RES = Compat.effect("FIRE_RESISTANCE");
    public final PotionEffectType POISON = Compat.effect("POISON");
    public final PotionEffectType SLOW = Compat.effectFirst("SLOWNESS", "SLOW");
    public final PotionEffectType BLINDNESS = Compat.effect("BLINDNESS");
    public final PotionEffectType NAUSEA = Compat.effectFirst("NAUSEA", "CONFUSION");
    public final PotionEffectType WATER_BREATHING = Compat.effect("WATER_BREATHING");
    public final PotionEffectType WEAKNESS = Compat.effect("WEAKNESS");
    public final PotionEffectType MINING_FATIGUE = Compat.effectFirst("MINING_FATIGUE", "SLOW_DIGGING");
    public final PotionEffectType REGEN = Compat.effectFirst("REGENERATION");
    public final PotionEffectType SPEED = Compat.effect("SPEED");
    public final PotionEffectType STRENGTH = Compat.effectFirst("STRENGTH", "INCREASE_DAMAGE");
}

