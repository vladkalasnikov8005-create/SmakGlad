package org.examplee.palePlugin.items;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.examplee.palePlugin.PalePlugin;
import org.examplee.palePlugin.util.InvUtil;
import org.examplee.palePlugin.util.Msg;

public final class PaleItems {
    private final PalePlugin plugin;

    public PaleItems(PalePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        ShapelessRecipe r;
        try {
            ItemStack salt2 = this.makeSalt(2);
            r = new ShapelessRecipe(new NamespacedKey((Plugin)this.plugin, "pale_salt_recipe"), salt2);
            r.addIngredient(Material.GLOWSTONE_DUST);
            r.addIngredient(Material.BONE_MEAL);
            Bukkit.addRecipe((Recipe)r);
        }
        catch (Exception salt2) {
            // empty catch block
        }
        try {
            ItemStack holy = this.makeHolyWater(1);
            r = new ShapelessRecipe(new NamespacedKey((Plugin)this.plugin, "pale_holywater_recipe"), holy);
            r.addIngredient((RecipeChoice)new RecipeChoice.MaterialChoice(Material.SPLASH_POTION));
            r.addIngredient(Material.GHAST_TEAR);
            Bukkit.addRecipe((Recipe)r);
        }
        catch (Exception holy) {
            // empty catch block
        }
        try {
            ItemStack ward = this.makeWard(1);
            r = new ShapedRecipe(new NamespacedKey((Plugin)this.plugin, "pale_ward_recipe"), ward);
            r.shape(new String[]{"AAA", "ANA", "AAA"});
            r.setIngredient('A', Material.AMETHYST_SHARD);
            r.setIngredient('N', Material.NETHER_STAR);
            Bukkit.addRecipe((Recipe)r);
        }
        catch (Exception ward) {
            // empty catch block
        }
        try {
            ItemStack flint = this.makePurifierFlint(1);
            r = new ShapelessRecipe(new NamespacedKey((Plugin)this.plugin, "pale_purifier_flint_recipe"), flint);
            r.addIngredient(Material.FLINT_AND_STEEL);
            r.addIngredient(Material.GHAST_TEAR);
            r.addIngredient(Material.GLOWSTONE_DUST);
            Bukkit.addRecipe((Recipe)r);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public ItemStack makeSalt(int amount) {
        ItemStack it = new ItemStack(Material.GLOWSTONE_DUST, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.g("[Pale] \u041e\u0447\u0438\u0449\u0430\u044e\u0449\u0430\u044f \u0441\u043e\u043b\u044c"));
            meta.setLore(List.of("\u041f\u041a\u041c: \u043e\u0447\u0438\u0449\u0430\u0435\u0442 \u0437\u0430\u0440\u0430\u0436\u0435\u043d\u0438\u0435 \u0432 \u0440\u0430\u0434\u0438\u0443\u0441\u0435 " + this.plugin.cfg.saltRadius));
            meta.getPersistentDataContainer().set(this.plugin.keys.KEY_SALT, PersistentDataType.BYTE, (Object)1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean isSalt(ItemStack it) {
        return this.hasByte(it, this.plugin.keys.KEY_SALT);
    }

    public ItemStack makeHolyWater(int amount) {
        ItemStack it = new ItemStack(Material.SPLASH_POTION, amount);
        ItemMeta im = it.getItemMeta();
        if (im instanceof PotionMeta) {
            PotionMeta pm = (PotionMeta)im;
            pm.setDisplayName(Msg.g("[Pale] \u0421\u0432\u044f\u0442\u0430\u044f \u0432\u043e\u0434\u0430"));
            pm.setLore(List.of("\u0411\u0440\u043e\u0441\u044c: \u043e\u0447\u0438\u0449\u0430\u0435\u0442 \u0437\u0430\u0440\u0430\u0436\u0435\u043d\u0438\u0435 \u0432 \u0440\u0430\u0434\u0438\u0443\u0441\u0435 " + this.plugin.cfg.holyWaterRadius));
            pm.getPersistentDataContainer().set(this.plugin.keys.KEY_HOLY_WATER, PersistentDataType.BYTE, (Object)1);
            it.setItemMeta((ItemMeta)pm);
        }
        return it;
    }

    public boolean isHolyWater(ItemStack it) {
        return this.hasByte(it, this.plugin.keys.KEY_HOLY_WATER);
    }

    public ItemStack makeWard(int amount) {
        ItemStack it = new ItemStack(Material.AMETHYST_BLOCK, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.g("[Pale] \u041e\u0431\u0435\u0440\u0435\u0433"));
            meta.setLore(List.of("\u0421\u0442\u0430\u0432\u044c: \u0431\u043b\u043e\u043a\u0438\u0440\u0443\u0435\u0442 \u0437\u0430\u0440\u0430\u0436\u0435\u043d\u0438\u0435 \u0432 \u0440\u0430\u0434\u0438\u0443\u0441\u0435 " + this.plugin.cfg.wardRadius));
            meta.getPersistentDataContainer().set(this.plugin.keys.KEY_WARD, PersistentDataType.BYTE, (Object)1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean isWard(ItemStack it) {
        return this.hasByte(it, this.plugin.keys.KEY_WARD);
    }

    public ItemStack makePurifierFlint(int amount) {
        ItemStack it = new ItemStack(Material.FLINT_AND_STEEL, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.g("[Pale] \u041e\u0447\u0438\u0449\u0430\u044e\u0449\u0435\u0435 \u043e\u0433\u043d\u0438\u0432\u043e"));
            meta.setLore(List.of("\u041f\u041a\u041c \u043f\u043e \u0437\u0430\u0440\u0430\u0436\u0435\u043d\u0438\u044e: \u043e\u0447\u0438\u0449\u0430\u0435\u0442 \u0440\u0430\u0434\u0438\u0443\u0441 " + this.plugin.cfg.purifierFlintRadius, "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0439: " + this.plugin.cfg.purifierFlintUsesDefault));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(this.plugin.keys.KEY_PURIFIER_FLINT, PersistentDataType.BYTE, (Object)1);
            pdc.set(this.plugin.keys.KEY_PURIFIER_FLINT_USES, PersistentDataType.INTEGER, (Object)this.plugin.cfg.purifierFlintUsesDefault);
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean isPurifierFlint(ItemStack it) {
        if (it == null || it.getType() != Material.FLINT_AND_STEEL) {
            return false;
        }
        return this.hasByte(it, this.plugin.keys.KEY_PURIFIER_FLINT);
    }

    public int getPurifierFlintUses(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) {
            return 0;
        }
        Integer v = (Integer)meta.getPersistentDataContainer().get(this.plugin.keys.KEY_PURIFIER_FLINT_USES, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    public void setPurifierFlintUses(ItemStack it, int usesLeft) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(this.plugin.keys.KEY_PURIFIER_FLINT_USES, PersistentDataType.INTEGER, (Object)usesLeft);
        meta.setLore(List.of("\u041f\u041a\u041c \u043f\u043e \u0437\u0430\u0440\u0430\u0436\u0435\u043d\u0438\u044e: \u043e\u0447\u0438\u0449\u0430\u0435\u0442 \u0440\u0430\u0434\u0438\u0443\u0441 " + this.plugin.cfg.purifierFlintRadius, "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0439: " + usesLeft));
        it.setItemMeta(meta);
    }

    public ItemStack makeInfectionMap(int amount, int radiusChunks) {
        ItemStack it = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.g("[Pale] \u041a\u0430\u0440\u0442\u0430 \u0437\u0430\u0440\u0430\u0436\u0435\u043d\u0438\u044f"));
            meta.setLore(List.of("\u041f\u041a\u041c: \u043f\u043e\u043a\u0430\u0437\u0430\u0442\u044c \u043a\u0430\u0440\u0442\u0443", "\u0420\u0430\u0434\u0438\u0443\u0441: " + radiusChunks + " \u0447\u0430\u043d\u043a\u043e\u0432"));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(this.plugin.keys.KEY_MAP_ITEM, PersistentDataType.BYTE, (Object)1);
            pdc.set(this.plugin.keys.KEY_MAP_RADIUS, PersistentDataType.INTEGER, (Object)radiusChunks);
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean isInfectionMap(ItemStack it) {
        if (it == null || it.getType() != Material.PAPER) {
            return false;
        }
        return this.hasByte(it, this.plugin.keys.KEY_MAP_ITEM);
    }

    public int getMapRadius(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) {
            return this.plugin.cfg.mapItemDefaultRadiusChunks;
        }
        Integer r = (Integer)meta.getPersistentDataContainer().get(this.plugin.keys.KEY_MAP_RADIUS, PersistentDataType.INTEGER);
        return r == null ? this.plugin.cfg.mapItemDefaultRadiusChunks : r;
    }

    public ItemStack makeInfectWand(int amount, int uses) {
        ItemStack it = new ItemStack(Material.CARROT_ON_A_STICK, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.g("[Pale] \u041f\u0430\u043b\u043e\u0447\u043a\u0430 \u0437\u0430\u0440\u0430\u0437\u044b"));
            meta.setLore(List.of("\u041f\u041a\u041c: \u0437\u0430\u0440\u0430\u0436\u0430\u0435\u0442 \u0440\u0430\u0434\u0438\u0443\u0441 " + this.plugin.cfg.infectWandRadius, "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0439: " + uses));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(this.plugin.keys.KEY_INFECT_WAND, PersistentDataType.BYTE, (Object)1);
            pdc.set(this.plugin.keys.KEY_INFECT_WAND_USES, PersistentDataType.INTEGER, (Object)uses);
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean isInfectWand(ItemStack it) {
        if (it == null || it.getType() != Material.CARROT_ON_A_STICK) {
            return false;
        }
        return this.hasByte(it, this.plugin.keys.KEY_INFECT_WAND);
    }

    public int getInfectWandUses(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) {
            return 0;
        }
        Integer v = (Integer)meta.getPersistentDataContainer().get(this.plugin.keys.KEY_INFECT_WAND_USES, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    public void setInfectWandUses(ItemStack it, int usesLeft) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(this.plugin.keys.KEY_INFECT_WAND_USES, PersistentDataType.INTEGER, (Object)usesLeft);
        meta.setLore(List.of("\u041f\u041a\u041c: \u0437\u0430\u0440\u0430\u0436\u0430\u0435\u0442 \u0440\u0430\u0434\u0438\u0443\u0441 " + this.plugin.cfg.infectWandRadius, "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0439: " + usesLeft));
        it.setItemMeta(meta);
    }

    public ItemStack makeAdminPurgeWand(int amount) {
        ItemStack it = new ItemStack(Material.BLAZE_ROD, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.g("[Pale] \u0410\u0434\u043c\u0438\u043d: \u0416\u0435\u0437\u043b \u043e\u0447\u0438\u0449\u0435\u043d\u0438\u044f"));
            meta.setLore(List.of("\u0422\u043e\u043b\u044c\u043a\u043e \u0434\u043b\u044f \u0430\u0434\u043c\u0438\u043d\u043e\u0432", "\u041f\u041a\u041c: \u043c\u0430\u0441\u0441\u043e\u0432\u0430\u044f \u043e\u0447\u0438\u0441\u0442\u043a\u0430", "\u0420\u0430\u0434\u0438\u0443\u0441: " + this.plugin.cfg.adminPurgeRadiusChunks + " \u0447\u0430\u043d\u043a\u043e\u0432", "\u0413\u043b\u0443\u0431\u0438\u043d\u0430: " + this.plugin.cfg.adminPurgeDepth));
            meta.getPersistentDataContainer().set(this.plugin.keys.KEY_ADMIN_PURGE, PersistentDataType.BYTE, (Object)1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean isAdminPurgeWand(ItemStack it) {
        if (it == null || it.getType() != Material.BLAZE_ROD) {
            return false;
        }
        return this.hasByte(it, this.plugin.keys.KEY_ADMIN_PURGE);
    }

    public void giveOrDrop(Player p, ItemStack it) {
        InvUtil.giveOrDrop(p, it);
    }

    private boolean hasByte(ItemStack it, NamespacedKey key) {
        if (it == null) {
            return false;
        }
        ItemMeta meta = it.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte v = (Byte)meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return v != null && v == 1;
    }
}

