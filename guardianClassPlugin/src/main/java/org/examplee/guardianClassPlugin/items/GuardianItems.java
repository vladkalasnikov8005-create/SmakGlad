package org.examplee.guardianClassPlugin.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.examplee.guardianClassPlugin.GuardianClassPlugin;

import java.util.List;

public final class GuardianItems {

    private final GuardianClassPlugin plugin;

    public GuardianItems(GuardianClassPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack guardianStone(int amount) {
        ItemStack it = new ItemStack(Material.EMERALD, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Камень хранителя" + ChatColor.RESET);
            meta.setLore(List.of(
                    ChatColor.GRAY + "Носи в OFFHAND 5 часов",
                    ChatColor.GRAY + "чтобы стать Приближённым."
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(plugin.keys.ITEM_GUARDIAN_STONE, PersistentDataType.BYTE, (byte) 1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public ItemStack waterStaff(int amount) {
        ItemStack it = new ItemStack(Material.STICK, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.BLUE + "Посох воды" + ChatColor.RESET);
            meta.setLore(List.of(
                    ChatColor.GRAY + "ПКМ: водяной луч (10 блоков)",
                    ChatColor.GRAY + "Shift+ПКМ: водяной щит (5 сек, КД 5 минут)",
                    ChatColor.GRAY + "ЛКМ: тушит тебя (КД 10 сек)",
                    ChatColor.DARK_GRAY + "Только Приближённым/Истинным"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(plugin.keys.ITEM_WATER_STAFF, PersistentDataType.BYTE, (byte) 1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public ItemStack lifeStoneBlock(int amount) {
        ItemStack it = new ItemStack(Material.EMERALD_BLOCK, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Камень жизни" + ChatColor.RESET);
            meta.setLore(List.of(
                    ChatColor.GRAY + "Поставь: аура 12 блоков",
                    ChatColor.GRAY + "Ускоряет рост растений",
                    ChatColor.DARK_GRAY + "Только Хранителям"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(plugin.keys.ITEM_LIFE_STONE_BLOCK, PersistentDataType.BYTE, (byte) 1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public ItemStack guardianFlowerBlock(int amount) {
        ItemStack it = new ItemStack(Material.SPORE_BLOSSOM, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Цветок хранителя" + ChatColor.RESET);
            meta.setLore(List.of(
                    ChatColor.GRAY + "Поставь: радиус 24",
                    ChatColor.GRAY + "Ускоряет Приближённых/Истинных",
                    ChatColor.DARK_GRAY + "Только Хранителям"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(plugin.keys.ITEM_GUARDIAN_FLOWER, PersistentDataType.BYTE, (byte) 1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public ItemStack divineSeed(int amount) {
        ItemStack it = new ItemStack(Material.WHEAT_SEEDS, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Божественное семя" + ChatColor.RESET);
            meta.setLore(List.of(ChatColor.GRAY + "ПКМ по земле: вырастить случайное дерево"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(plugin.keys.ITEM_DIVINE_SEED, PersistentDataType.BYTE, (byte) 1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public ItemStack purifyingLotus(int amount) {
        ItemStack it = new ItemStack(Material.LILY_PAD, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Очищающий лотос" + ChatColor.RESET);
            meta.setLore(List.of(ChatColor.GRAY + "ПКМ: снять статус Хранителя"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(plugin.keys.ITEM_PURIFYING_LOTUS, PersistentDataType.BYTE, (byte) 1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean isGuardianStone(ItemStack it) { return has(it, plugin.keys.ITEM_GUARDIAN_STONE); }
    public boolean isWaterStaff(ItemStack it) { return has(it, plugin.keys.ITEM_WATER_STAFF); }
    public boolean isDivineSeed(ItemStack it) { return has(it, plugin.keys.ITEM_DIVINE_SEED); }
    public boolean isPurifyingLotus(ItemStack it) { return has(it, plugin.keys.ITEM_PURIFYING_LOTUS); }
    public boolean isLifeStoneItem(ItemStack it) { return has(it, plugin.keys.ITEM_LIFE_STONE_BLOCK); }
    public boolean isGuardianFlowerItem(ItemStack it) { return has(it, plugin.keys.ITEM_GUARDIAN_FLOWER); }

    private boolean has(ItemStack it, org.bukkit.NamespacedKey key) {
        if (it == null || it.getType() == Material.AIR) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        Byte v = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return v != null && v == (byte) 1;
    }
}
