package org.examplee.leperClassPlugin.items;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.examplee.leperClassPlugin.core.LeperKeys;
import org.examplee.leperClassPlugin.util.Compat;
import org.examplee.leperClassPlugin.util.TextUtil;

public final class ItemFactory {
    public static final int ITEM_VERSION = 2;
    private final LeperKeys keys;

    public ItemFactory(LeperKeys keys) {
        this.keys = keys;
    }

    public ItemStack makePlagueStick() {
        ItemStack it = new ItemStack(Material.STICK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.gPurpleGreen("\u041f\u0410\u041b\u041a\u0410 \u041f\u0420\u041e\u041a\u0410\u0417\u042b"));
            meta.setLore(List.of(TextUtil.loreHint("\u0423\u0434\u0430\u0440: \u044f\u0434 \u0438 \u043a\u043e\u0440\u043e\u0442\u043a\u043e\u0435 \u043e\u0433\u043b\u0443\u0448\u0435\u043d\u0438\u0435"), TextUtil.loreWarn("\u0417\u0430\u0440\u0430\u0436\u0435\u043d\u0438\u0435 \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442 \u0442\u043e\u043b\u044c\u043a\u043e \u0441 \u0431\u043b\u0430\u0433\u043e\u0441\u043b\u043e\u0432\u043b\u0435\u043d\u0438\u0435\u043c \u0414\u0435\u043d\u0436\u0435\u0440")));
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            this.markVersion(meta);
            meta.getPersistentDataContainer().set(this.keys.plagueStickKey, PersistentDataType.BYTE, (Object)1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public ItemStack makePlagueBomb() {
        Material mat = Compat.materialFirst("SLIME_BALL", "FERMENTED_SPIDER_EYE", "SPIDER_EYE");
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.gGreenGray("\u041e\u0411\u041b\u0410\u041a\u041e \u041f\u0420\u041e\u041a\u0410\u0417\u042b"));
            meta.setLore(List.of(TextUtil.loreHint("\u041f\u041a\u041c: \u044f\u0434\u043e\u0432\u0438\u0442\u043e\u0435 \u043e\u0431\u043b\u0430\u043a\u043e \u0438 \u043e\u0433\u043b\u0443\u0448\u0435\u043d\u0438\u0435"), TextUtil.loreWarn("\u041e\u0447\u0430\u0433 \u0438 \u0437\u0430\u0440\u0430\u0436\u0435\u043d\u0438\u0435 \u0442\u043e\u043b\u044c\u043a\u043e \u0441 \u0431\u043b\u0430\u0433\u043e\u0441\u043b\u043e\u0432\u043b\u0435\u043d\u0438\u0435\u043c \u0414\u0435\u043d\u0436\u0435\u0440")));
            this.markVersion(meta);
            meta.getPersistentDataContainer().set(this.keys.plagueBombKey, PersistentDataType.BYTE, (Object)1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public ItemStack makeVaccine() {
        ItemStack it = this.makePotion(String.valueOf(ChatColor.AQUA) + "\u0412\u0410\u041a\u0426\u0418\u041d\u0410", List.of(TextUtil.loreHint("\u041f\u043e\u043b\u043d\u043e\u0441\u0442\u044c\u044e \u0438\u0437\u043b\u0435\u0447\u0438\u0432\u0430\u0435\u0442 \u043f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u0438\u0435 \u043d\u0430 \u0440\u0430\u043d\u043d\u0438\u0445 \u0441\u0442\u0430\u0434\u0438\u044f\u0445 (1-2)")), Color.fromRGB((int)150, (int)255, (int)255), this.keys.vaccineKey, "INSTANT_HEAL");
        return it;
    }

    public ItemStack makeLeperBlood() {
        return this.makePotion(String.valueOf(ChatColor.DARK_RED) + "\u041a\u0440\u043e\u0432\u044c \u043f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u043e\u0433\u043e", List.of(TextUtil.loreWarn("\u041d\u0435 \u043f\u0435\u0439 \u044d\u0442\u0443 \u0434\u0440\u044f\u043d\u044c (\u043f\u043e\u043b\u0443\u0447\u0438\u0448\u044c \u043f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u0438\u0435)")), Color.fromRGB((int)180, (int)0, (int)0), this.keys.leperBloodKey, "WATER");
    }

    public ItemStack makeThickLeperBlood() {
        return this.makePotion(String.valueOf(ChatColor.GRAY) + "\u0413\u0443\u0441\u0442\u0430\u044f \u043a\u0440\u043e\u0432\u044c \u043f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u043e\u0433\u043e", List.of(TextUtil.loreHint("\u041a\u0430\u0436\u0435\u0442\u0441\u044f, \u044d\u0442\u043e \u043b\u0443\u0447\u0448\u0435 \u043d\u0435 \u043f\u0438\u0442\u044c")), Color.fromRGB((int)90, (int)90, (int)90), this.keys.thickBloodKey, "AWKWARD");
    }

    public ItemStack makeSterileLeperBlood() {
        return this.makePotion(String.valueOf(ChatColor.GOLD) + "\u0421\u0442\u0435\u0440\u0438\u043b\u044c\u043d\u0430\u044f \u043a\u0440\u043e\u0432\u044c \u043f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u043e\u0433\u043e", List.of(TextUtil.loreHint("\u0411\u0435\u0441\u043f\u043e\u043b\u0435\u0437\u043d\u0430")), Color.fromRGB((int)120, (int)70, (int)35), this.keys.sterileBloodKey, "AWKWARD");
    }

    public ItemStack makeSacrificialKnife() {
        ItemStack it = new ItemStack(Compat.materialFirst("IRON_SWORD", "STONE_SWORD"));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.gRedGray("\u0416\u0415\u0420\u0422\u0412\u0415\u041d\u041d\u042b\u0419 \u041d\u041e\u0416\u0418\u041a"));
            meta.setLore(List.of(TextUtil.loreHint("\u041f\u041a\u041c: \u0434\u043e\u0431\u044b\u0442\u044c \u043a\u0440\u043e\u0432\u044c (\u0442\u043e\u043b\u044c\u043a\u043e \u0434\u043b\u044f \u043f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u043e\u0433\u043e)"), String.valueOf(ChatColor.DARK_GRAY) + "\u2022 \u041a\u0414: 1 \u0447\u0430\u0441"));
            this.markVersion(meta);
            meta.getPersistentDataContainer().set(this.keys.sacrificialKnifeKey, PersistentDataType.BYTE, (Object)1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public ItemStack makeUmbrellaTiny() {
        return this.makeUmbrella(0, 150);
    }

    public ItemStack makeUmbrellaWeak() {
        return this.makeUmbrella(1, 600);
    }

    public ItemStack makeUmbrellaNormal() {
        return this.makeUmbrella(2, 1500);
    }

    public ItemStack makeUmbrellaStrong() {
        return this.makeUmbrella(3, 3000);
    }

    private ItemStack makeUmbrella(int tier, int lifetimeSeconds) {
        ItemStack it = new ItemStack(Material.STICK, 1);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u0417\u043e\u043d\u0442");
            int remaining = lifetimeSeconds;
            meta.setLore(Arrays.asList(String.valueOf(ChatColor.GRAY) + "\u0414\u0435\u0440\u0436\u0438 \u0432 \u043b\u0435\u0432\u043e\u0439 \u0440\u0443\u043a\u0435", String.valueOf(ChatColor.GRAY) + "\u0417\u0430\u0449\u0438\u0449\u0430\u0435\u0442 \u043e\u0442 \u0441\u043e\u043b\u043d\u0446\u0430", String.valueOf(ChatColor.GRAY) + "\u0423\u0440\u043e\u0432\u0435\u043d\u044c: " + tier, String.valueOf(ChatColor.GRAY) + "\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: " + this.formatSeconds(remaining)));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            this.markVersion(meta);
            pdc.set(this.keys.umbrellaKey, PersistentDataType.BYTE, (Object)1);
            pdc.set(this.keys.umbrellaTierKey, PersistentDataType.INTEGER, (Object)tier);
            pdc.set(this.keys.umbrellaLifetimeKey, PersistentDataType.INTEGER, (Object)lifetimeSeconds);
            pdc.set(this.keys.umbrellaRemainingKey, PersistentDataType.INTEGER, (Object)remaining);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String formatSeconds(int total) {
        int s = Math.max(0, total);
        int min = s / 60;
        int sec = s % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public ItemStack bgPane() {
        ItemStack bg = new ItemStack(Compat.materialFirst("PURPLE_STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", "GLASS_PANE"));
        ItemMeta m = bg.getItemMeta();
        if (m != null) {
            m.setDisplayName(String.valueOf(ChatColor.DARK_GRAY) + "\u2022");
            bg.setItemMeta(m);
        }
        return bg;
    }

    public ItemStack button(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack makePotion(String name, List<String> lore, Color color, NamespacedKey key, String baseTypeName) {
        ItemStack it = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta)it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.setColor(color);
            this.setBasePotionCompat(meta, baseTypeName);
            try {
                meta.addItemFlags(new ItemFlag[]{ItemFlag.valueOf((String)"HIDE_POTION_EFFECTS")});
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
            this.markVersion((ItemMeta)meta);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (Object)1);
            it.setItemMeta((ItemMeta)meta);
        }
        return it;
    }

    private void markVersion(ItemMeta meta) {
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(this.keys.itemVersionKey, PersistentDataType.INTEGER, (Object)2);
    }

    private void setBasePotionCompat(PotionMeta meta, String potionTypeName) {
        if (meta == null || potionTypeName == null) {
            return;
        }
        try {
            Class<?> potionTypeClass = Class.forName("org.bukkit.potion.PotionType");
            Enum potionType = Enum.valueOf(potionTypeClass.asSubclass(Enum.class), potionTypeName);
            try {
                Method setBasePotionType = meta.getClass().getMethod("setBasePotionType", potionTypeClass);
                setBasePotionType.invoke(meta, potionType);
                return;
            }
            catch (Throwable setBasePotionType) {
                Class<?> potionDataClass = Class.forName("org.bukkit.potion.PotionData");
                Object potionData = potionDataClass.getConstructor(potionTypeClass).newInstance(potionType);
                Method setBasePotionData = meta.getClass().getMethod("setBasePotionData", potionDataClass);
                setBasePotionData.invoke(meta, potionData);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}

