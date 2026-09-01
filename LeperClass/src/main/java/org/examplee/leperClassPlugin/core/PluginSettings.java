package org.examplee.leperClassPlugin.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginSettings {
    public final long infectionPhaseMs;
    public final double contactInfectPerHp;
    public final double contactFakeScareChance;
    public final long contactResolveTicks;
    public final long knifeCooldownMs;
    public final int knifeFatigueTicks;
    public final int knifeWeakTicks;
    public final int knifeSlowTicks;
    public final long sneezeCooldownMs;
    public final double sneezeVelocity;
    public final int sneezeMaxDistance;
    public final int sneezePoisonTicks;
    public final int sneezeSlowTicks;
    public final int sneezeWeakTicks;
    public final int sneezeBlindTicks;
    public final double leperHealFromPoison;
    public final double leperHealFromHarm;
    public final double leperDamageFromHeal;

    public PluginSettings(JavaPlugin plugin) {
        FileConfiguration c = plugin.getConfig();
        this.infectionPhaseMs = c.getLong("balance.infection.phase_minutes", 20L) * 60000L;
        this.contactInfectPerHp = c.getDouble("balance.contact_infect_per_hp", 0.015);
        this.contactFakeScareChance = c.getDouble("balance.contact_fake_scare_chance", 0.2);
        this.contactResolveTicks = c.getLong("balance.contact_resolve_minutes", 20L) * 60L * 20L;
        this.knifeCooldownMs = c.getLong("balance.knife.cooldown_minutes", 60L) * 60000L;
        this.knifeFatigueTicks = (int)(c.getLong("balance.knife.fatigue_minutes", 5L) * 60L * 20L);
        this.knifeWeakTicks = (int)(c.getLong("balance.knife.weak_minutes", 10L) * 60L * 20L);
        this.knifeSlowTicks = (int)(c.getLong("balance.knife.slow_minutes", 10L) * 60L * 20L);
        this.sneezeCooldownMs = c.getLong("balance.sneeze.cooldown_seconds", 10L) * 1000L;
        this.sneezeVelocity = c.getDouble("balance.sneeze.velocity", 1.15);
        this.sneezeMaxDistance = c.getInt("balance.sneeze.max_distance", 12);
        this.sneezePoisonTicks = (int)(c.getLong("balance.sneeze.poison_seconds", 10L) * 20L);
        this.sneezeSlowTicks = (int)(c.getLong("balance.sneeze.slow_seconds", 10L) * 20L);
        this.sneezeWeakTicks = (int)(c.getLong("balance.sneeze.weak_seconds", 5L) * 20L);
        this.sneezeBlindTicks = (int)(c.getLong("balance.sneeze.blind_seconds", 10L) * 20L);
        this.leperHealFromPoison = c.getDouble("balance.potion_inversion.heal_from_poison", 6.0);
        this.leperHealFromHarm = c.getDouble("balance.potion_inversion.heal_from_harm", 8.0);
        this.leperDamageFromHeal = c.getDouble("balance.potion_inversion.damage_from_heal", 6.0);
    }
}

