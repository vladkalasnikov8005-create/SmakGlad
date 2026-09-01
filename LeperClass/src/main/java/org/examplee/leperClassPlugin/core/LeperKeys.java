package org.examplee.leperClassPlugin.core;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class LeperKeys {
    public final NamespacedKey leperKey;
    public final NamespacedKey plagueStickKey;
    public final NamespacedKey plagueBombKey;
    public final NamespacedKey umbrellaKey;
    public final NamespacedKey umbrellaTierKey;
    public final NamespacedKey umbrellaLifetimeKey;
    public final NamespacedKey umbrellaRemainingKey;
    public final NamespacedKey vaccineKey;
    public final NamespacedKey infectionHitsKey;
    public final NamespacedKey infectionStageKey;
    public final NamespacedKey infectionNextPhaseKey;
    public final NamespacedKey dangerBlessKey;
    public final NamespacedKey rageUntilKey;
    public final NamespacedKey leperBloodKey;
    public final NamespacedKey thickBloodKey;
    public final NamespacedKey sterileBloodKey;
    public final NamespacedKey sacrificialKnifeKey;
    public final NamespacedKey sneezeProjectileKey;
    public final NamespacedKey itemVersionKey;

    public LeperKeys(Plugin plugin) {
        this.leperKey = new NamespacedKey(plugin, "class_leper");
        this.plagueStickKey = new NamespacedKey(plugin, "plague_stick");
        this.plagueBombKey = new NamespacedKey(plugin, "plague_bomb");
        this.umbrellaKey = new NamespacedKey(plugin, "umbrella");
        this.umbrellaTierKey = new NamespacedKey(plugin, "umbrella_tier");
        this.umbrellaLifetimeKey = new NamespacedKey(plugin, "umbrella_lifetime_sec");
        this.umbrellaRemainingKey = new NamespacedKey(plugin, "umbrella_remaining_sec");
        this.vaccineKey = new NamespacedKey(plugin, "vaccine_shot");
        this.infectionHitsKey = new NamespacedKey(plugin, "infection_hits");
        this.infectionStageKey = new NamespacedKey(plugin, "infection_stage");
        this.infectionNextPhaseKey = new NamespacedKey(plugin, "infection_time");
        this.dangerBlessKey = new NamespacedKey(plugin, "danger_blessing");
        this.rageUntilKey = new NamespacedKey(plugin, "rage_until_ms");
        this.leperBloodKey = new NamespacedKey(plugin, "leper_blood");
        this.thickBloodKey = new NamespacedKey(plugin, "thick_leper_blood");
        this.sterileBloodKey = new NamespacedKey(plugin, "sterile_leper_blood");
        this.sacrificialKnifeKey = new NamespacedKey(plugin, "sacrificial_knife");
        this.sneezeProjectileKey = new NamespacedKey(plugin, "sneeze_projectile");
        this.itemVersionKey = new NamespacedKey(plugin, "item_version");
    }
}

