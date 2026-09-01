package org.examplee.vampirest;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class CustomItemManager {

    private final NamespacedKey aspenStakeKey;
    private final NamespacedKey saltItemKey;
    private final NamespacedKey saltProjectileKey;
    private final NamespacedKey garlicSerumKey;
    private final NamespacedKey bloodBottleKey;
    private final NamespacedKey bloodBottleTypeKey;
    private final NamespacedKey bloodArtifactKey;
    private final NamespacedKey saltBlockItemKey;
    private final NamespacedKey garlicnessKey;
    private final NamespacedKey trumeHatKey;
    private final NamespacedKey garlicBookKey;

    public CustomItemManager(VampireRacePlugin plugin) {
        this.aspenStakeKey = new NamespacedKey(plugin, "aspen_stake");
        this.saltItemKey = new NamespacedKey(plugin, "salt_item");
        this.saltProjectileKey = new NamespacedKey(plugin, "salt_projectile");
        this.garlicSerumKey = new NamespacedKey(plugin, "garlic_serum");
        this.bloodBottleKey = new NamespacedKey(plugin, "blood_bottle");
        this.bloodBottleTypeKey = new NamespacedKey(plugin, "blood_bottle_type");
        this.bloodArtifactKey = new NamespacedKey(plugin, "blood_artifact");
        this.saltBlockItemKey = new NamespacedKey(plugin, "salt_block_item");
        this.garlicnessKey = new NamespacedKey(plugin, "garlicness");
        this.trumeHatKey = new NamespacedKey(plugin, "trume_hat");
        this.garlicBookKey = new NamespacedKey(plugin, "garlic_book");
    }

    public ItemStack createAspenStake() {
        ItemStack item = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Texts.colorize("&#FFD4A3&lОсиновый &#FFA14A&lкол"));
        meta.setLore(List.of(
                Texts.colorize("&7Оружие против вампиров"),
                Texts.colorize("&7Второй удар за 5 сек &cсмертелен")
        ));
        meta.getPersistentDataContainer().set(aspenStakeKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSalt(int amount) {
        ItemStack item = new ItemStack(Material.SUGAR, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Texts.colorize("&#FFFFFF&lС&#F2F2F2&lо&#E5E5E5&lл&#D8D8D8&lь"));
        meta.setLore(List.of(
                Texts.colorize("&7ПКМ по блоку: поставить соль"),
                Texts.colorize("&7ПКМ в воздух: бросить в вампира")
        ));
        meta.getPersistentDataContainer().set(saltItemKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createGarlicSerum() {
        ItemStack item = new ItemStack(Material.HONEY_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Texts.colorize("&#EAD77E&lЧесночная &#C9B35A&lсыворотка"));
        meta.setLore(List.of(
                Texts.colorize("&7Лечит от вампиризма"),
                Texts.colorize("&7После применения: &f15 минут &7слабости")
        ));
        meta.getPersistentDataContainer().set(garlicSerumKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBloodArtifact() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Texts.colorize("&#FF5C5C&lКровавый &#C91616&lартефакт"));
        meta.setLore(List.of(
                Texts.colorize("&7ПКМ: активировать кровавый щит"),
                Texts.colorize("&7Имеет собственную перезарядку")
        ));
        meta.getPersistentDataContainer().set(bloodArtifactKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createTrumeHat() {
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Texts.colorize("&#FFB067&lШляпа &#F07A2A&lиз трюма"));
        meta.setLore(List.of(Texts.colorize("&7Защищает вампира от солнца до износа")));
        meta.getPersistentDataContainer().set(trumeHatKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createGarlicChestplate(int level) {
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        int safeLevel = Math.max(1, Math.min(3, level));
        meta.setDisplayName(Texts.colorize("&#E4D89A&lДоспех с Чесночностью &f" + safeLevel));
        meta.setLore(List.of(
                Texts.colorize("&dЧесночность " + toRoman(safeLevel)),
                Texts.colorize("&7Отпугивает вампиров поблизости")
        ));
        item.setItemMeta(meta);
        setGarlicnessLevel(item, safeLevel);
        return item;
    }

    public ItemStack createGarlicBook(int level) {
        int safeLevel = Math.max(1, Math.min(3, level));
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Texts.colorize("&#EAD77E&lКнига Чесночности &f" + safeLevel));
        meta.setLore(List.of(
                Texts.colorize("&7Используйте в руке, держа нагрудник в левой руке"),
                Texts.colorize("&7Наложит Чесночность " + safeLevel)
        ));
        meta.getPersistentDataContainer().set(garlicBookKey, PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(garlicnessKey, PersistentDataType.INTEGER, safeLevel);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSaltBlockItem(int amount) {
        ItemStack item = new ItemStack(Material.CALCITE, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Texts.colorize("&#F7F7F7&lСоляной &#E5E5E5&lблок"));
        meta.setLore(List.of(Texts.colorize("&7Создает барьер против вампиров")));
        meta.getPersistentDataContainer().set(saltBlockItemKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBloodBottle(BloodBottleType type, String donorName) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        String name = switch (type) {
            case NORMAL -> Texts.colorize("&#FF6F6F&lБутылочка обычной крови");
            case NUTRITIOUS -> Texts.colorize("&#FF4A4A&lБутылочка питательной крови");
            case VAMPIRIC -> Texts.colorize("&#D11919&lБутылочка вампирской крови");
            case LORD -> Texts.colorize("&#B95DFF&lБутылочка крови Лорда");
            case CORRUPTED -> Texts.colorize("&#63C963&lБутылочка испорченной крови");
        };
        meta.setDisplayName(name);
        if (donorName == null || donorName.isBlank()) {
            meta.setLore(List.of(
                    Texts.colorize("&7Пейте осторожно"),
                    Texts.colorize("&8Тип: &f" + type.name())
            ));
        } else {
            meta.setLore(List.of(
                    Texts.colorize("&7Пейте осторожно"),
                    Texts.colorize("&8Тип: &f" + type.name()),
                    Texts.colorize("&8Донор: &f" + donorName)
            ));
        }
        Color color = switch (type) {
            case NORMAL -> Color.fromRGB(180, 10, 10);
            case NUTRITIOUS -> Color.fromRGB(230, 30, 30);
            case VAMPIRIC -> Color.fromRGB(125, 0, 0);
            case LORD -> Color.fromRGB(120, 40, 170);
            case CORRUPTED -> Color.fromRGB(35, 160, 35);
        };
        meta.setColor(color);
        try {
            meta.getClass().getMethod("setMaxStackSize", int.class).invoke(meta, 16);
        } catch (ReflectiveOperationException ignored) {
            try {
                meta.getClass().getMethod("setMaxStackSize", Integer.class).invoke(meta, 16);
            } catch (ReflectiveOperationException ignoredAgain) {
            }
        }
        meta.getPersistentDataContainer().set(bloodBottleKey, PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(bloodBottleTypeKey, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBloodBottle(BloodBottleType type) {
        return createBloodBottle(type, null);
    }

    public boolean isAspenStake(ItemStack item) { return hasBoolTag(item, aspenStakeKey); }
    public boolean isSaltItem(ItemStack item) { return hasBoolTag(item, saltItemKey); }
    public boolean isGarlicSerum(ItemStack item) { return hasBoolTag(item, garlicSerumKey); }
    public boolean isBloodArtifact(ItemStack item) { return hasBoolTag(item, bloodArtifactKey); }
    public boolean isSaltBlockItem(ItemStack item) { return hasBoolTag(item, saltBlockItemKey); }
    public boolean isBloodBottle(ItemStack item) { return hasBoolTag(item, bloodBottleKey); }
    public boolean isTrumeHat(ItemStack item) { return hasBoolTag(item, trumeHatKey); }
    public boolean isGarlicBook(ItemStack item) { return hasBoolTag(item, garlicBookKey); }

    public int getGarlicnessLevel(ItemStack chestplate) {
        if (chestplate == null || !chestplate.hasItemMeta()) {
            return 0;
        }
        Integer level = chestplate.getItemMeta().getPersistentDataContainer().get(garlicnessKey, PersistentDataType.INTEGER);
        return level == null ? 0 : Math.max(0, Math.min(3, level));
    }

    public int getGarlicBookLevel(ItemStack book) {
        if (!isGarlicBook(book) || book == null || !book.hasItemMeta()) {
            return 0;
        }
        Integer level = book.getItemMeta().getPersistentDataContainer().get(garlicnessKey, PersistentDataType.INTEGER);
        return level == null ? 0 : Math.max(1, Math.min(3, level));
    }

    public NamespacedKey getSaltProjectileKey() {
        return saltProjectileKey;
    }

    public BloodBottleType getBloodBottleType(ItemStack item) {
        if (!isBloodBottle(item) || item == null || !item.hasItemMeta()) {
            return null;
        }
        String raw = item.getItemMeta().getPersistentDataContainer().get(bloodBottleTypeKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return BloodBottleType.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public void setGarlicnessLevel(ItemStack chestplate, int level) {
        if (chestplate == null || !chestplate.hasItemMeta()) {
            return;
        }
        ItemMeta meta = chestplate.getItemMeta();
        int safeLevel = Math.max(1, Math.min(3, level));
        meta.getPersistentDataContainer().set(garlicnessKey, PersistentDataType.INTEGER, safeLevel);

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> Texts.colorize(line).contains("Чесночность"));
        lore.add(0, Texts.colorize("&dЧесночность " + toRoman(safeLevel)));
        meta.setLore(lore);

        // Visual glint to mimic enchant-like feedback.
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        try {
            meta.getClass().getMethod("setEnchantmentGlintOverride", Boolean.class).invoke(meta, true);
        } catch (ReflectiveOperationException ignored) {
        }
        chestplate.setItemMeta(meta);
    }

    private String toRoman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            default -> "III";
        };
    }

    private boolean hasBoolTag(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }

    public enum BloodBottleType {
        NORMAL,
        NUTRITIOUS,
        VAMPIRIC,
        LORD,
        CORRUPTED
    }
}