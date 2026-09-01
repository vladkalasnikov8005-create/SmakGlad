package org.examplee.palePlugin.gui;

import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.examplee.palePlugin.PalePlugin;

public final class AdminGui {
    private final PalePlugin plugin;

    public AdminGui(PalePlugin plugin) {
        this.plugin = plugin;
    }

    public Inventory build(Player p) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)new Holder(p.getUniqueId()), (int)27, (String)"Pale: Admin");
        inv.setItem(11, this.btn(Material.LEVER, this.plugin.spread.isRunning() ? "\u0420\u0430\u0437\u0440\u0430\u0441\u0442\u0430\u043d\u0438\u0435: \u0412\u041a\u041b" : "\u0420\u0430\u0437\u0440\u0430\u0441\u0442\u0430\u043d\u0438\u0435: \u0412\u042b\u041a\u041b", List.of("\u041a\u043b\u0438\u043a: \u043f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0438\u0442\u044c"), "toggle"));
        inv.setItem(13, this.btn(Material.CLOCK, "Speed: " + this.plugin.cfg.speedPerChunk, List.of("\u041b\u041a\u041c: -50", "\u041f\u041a\u041c: +50", "Shift+\u041b\u041a\u041c: -500", "Shift+\u041f\u041a\u041c: +500"), "speed"));
        inv.setItem(15, this.btn(Material.PAPER, "\u0412\u044b\u0434\u0430\u0442\u044c \u0441\u0435\u0431\u0435: \u041a\u0430\u0440\u0442\u0430 \u0437\u0430\u0440\u0430\u0436\u0435\u043d\u0438\u044f", List.of("r=" + this.plugin.cfg.mapItemDefaultRadiusChunks), "map_self"));
        inv.setItem(23, this.btn(Material.BLAZE_ROD, "\u0412\u044b\u0434\u0430\u0442\u044c \u0441\u0435\u0431\u0435: \u0416\u0435\u0437\u043b purge", List.of("\u041f\u041a\u041c: \u0430\u0434\u043c\u0438\u043d-\u043e\u0447\u0438\u0441\u0442\u043a\u0430"), "purge_self"));
        inv.setItem(25, this.btn(Material.CARROT_ON_A_STICK, "\u0412\u044b\u0434\u0430\u0442\u044c \u0441\u0435\u0431\u0435: \u041f\u0430\u043b\u043e\u0447\u043a\u0430 \u0437\u0430\u0440\u0430\u0437\u044b", List.of("uses=" + this.plugin.cfg.infectWandUsesDefault), "wand_self"));
        return inv;
    }

    private ItemStack btn(Material mat, String name, List<String> lore, String action) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(this.plugin.keys.KEY_GUI_ACTION, PersistentDataType.STRING, (Object)action);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static final class Holder
    implements InventoryHolder {
        final UUID owner;

        Holder(UUID owner) {
            this.owner = owner;
        }

        public Inventory getInventory() {
            return null;
        }
    }
}

