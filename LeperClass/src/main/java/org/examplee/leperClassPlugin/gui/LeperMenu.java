package org.examplee.leperClassPlugin.gui;

import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.gui.LeperMenuHolder;
import org.examplee.leperClassPlugin.util.TextUtil;

public final class LeperMenu {
    private final LeperClassPlugin plugin;

    public LeperMenu(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player admin, Player target) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)new LeperMenuHolder(target.getUniqueId()), (int)27, (String)TextUtil.gPurpleGreen("LEPER | CONTROL"));
        ItemStack bg = this.plugin.items.bgPane();
        for (int i = 0; i < inv.getSize(); ++i) {
            inv.setItem(i, bg);
        }
        inv.setItem(11, this.plugin.items.button(Material.EMERALD_BLOCK, TextUtil.gGreenGray("\u0414\u0410\u0422\u042c \u041a\u041b\u0410\u0421\u0421"), Arrays.asList(String.valueOf(ChatColor.GRAY) + "\u041a\u043b\u0438\u043a: \u0432\u044b\u0434\u0430\u0442\u044c \u041f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u043e\u0433\u043e", String.valueOf(ChatColor.DARK_GRAY) + "\u0426\u0435\u043b\u044c: " + target.getName())));
        inv.setItem(15, this.plugin.items.button(Material.BARRIER, TextUtil.gRedGray("\u0421\u041d\u042f\u0422\u042c \u041a\u041b\u0410\u0421\u0421"), Arrays.asList(String.valueOf(ChatColor.GRAY) + "\u041a\u043b\u0438\u043a: \u0443\u0431\u0440\u0430\u0442\u044c \u043a\u043b\u0430\u0441\u0441", String.valueOf(ChatColor.DARK_GRAY) + "\u0426\u0435\u043b\u044c: " + target.getName())));
        inv.setItem(17, this.plugin.items.makeUmbrellaTiny());
        inv.setItem(18, this.plugin.items.makeUmbrellaWeak());
        inv.setItem(19, this.plugin.items.makeUmbrellaNormal());
        inv.setItem(20, this.plugin.items.makeUmbrellaStrong());
        inv.setItem(22, this.plugin.items.makePlagueStick());
        inv.setItem(23, this.plugin.items.makeSacrificialKnife());
        inv.setItem(24, this.plugin.items.makePlagueBomb());
        inv.setItem(25, this.plugin.items.makeLeperBlood());
        inv.setItem(26, this.plugin.items.makeVaccine());
        inv.setItem(13, this.plugin.items.button(Material.NAME_TAG, TextUtil.gPurpleGray("\u0426\u0415\u041b\u042c: " + target.getName()), Collections.singletonList(String.valueOf(ChatColor.GRAY) + "\u0421\u0435\u0439\u0447\u0430\u0441: " + (this.plugin.data.isLeper(target) ? String.valueOf(ChatColor.DARK_RED) + "\u041f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u044b\u0439" : String.valueOf(ChatColor.GREEN) + "\u041e\u0431\u044b\u0447\u043d\u044b\u0439"))));
        admin.openInventory(inv);
    }
}

