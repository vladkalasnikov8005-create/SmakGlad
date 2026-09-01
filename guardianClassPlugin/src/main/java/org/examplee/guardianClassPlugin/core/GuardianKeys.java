package org.examplee.guardianClassPlugin.core;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class GuardianKeys {
    public final NamespacedKey STAGE;
    public final NamespacedKey STONE_TIME_SEC;
    public final NamespacedKey STONE_INTERRUPTED;
    public final NamespacedKey TREECAP_ENABLED;

    public final NamespacedKey ITEM_GUARDIAN_STONE;
    public final NamespacedKey ITEM_WATER_STAFF;
    public final NamespacedKey ITEM_DIVINE_SEED;
    public final NamespacedKey ITEM_PURIFYING_LOTUS;

    public final NamespacedKey ITEM_LIFE_STONE_BLOCK;
    public final NamespacedKey ITEM_GUARDIAN_FLOWER;

    public final NamespacedKey BLOCK_LIFE_STONE;
    public final NamespacedKey BLOCK_GUARDIAN_FLOWER;

    public GuardianKeys(Plugin plugin) {
        STAGE = new NamespacedKey(plugin, "guardian_stage");
        STONE_TIME_SEC = new NamespacedKey(plugin, "guardian_stone_time_sec");
        STONE_INTERRUPTED = new NamespacedKey(plugin, "guardian_stone_interrupted");
        TREECAP_ENABLED = new NamespacedKey(plugin, "treecap_enabled");

        ITEM_GUARDIAN_STONE = new NamespacedKey(plugin, "guardian_stone");
        ITEM_WATER_STAFF = new NamespacedKey(plugin, "water_staff");
        ITEM_DIVINE_SEED = new NamespacedKey(plugin, "divine_seed");
        ITEM_PURIFYING_LOTUS = new NamespacedKey(plugin, "purifying_lotus");

        ITEM_LIFE_STONE_BLOCK = new NamespacedKey(plugin, "life_stone_item");
        ITEM_GUARDIAN_FLOWER = new NamespacedKey(plugin, "guardian_flower_item");

        BLOCK_LIFE_STONE = new NamespacedKey(plugin, "life_stone_block");
        BLOCK_GUARDIAN_FLOWER = new NamespacedKey(plugin, "guardian_flower_block");
    }
}
