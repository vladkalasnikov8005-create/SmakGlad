package org.examplee.dvarf;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class DwarfProgressMenu {

    public static final String TITLE = "Путь Каменного Сердца";

    private DwarfProgressMenu() {
    }

    public static void open(Player player, DwarfService service) {
        Inventory menu = Bukkit.createInventory(null, 27, TITLE);

        fill(menu, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));

        int stage = service.getStage(player);
        long mined = service.getMinedBlocks(player);
        long next = service.getNextStageTargetBlocks(player);
        long left = service.getBlocksToNextStage(player);

        menu.setItem(4, buildItem(
            Material.NETHERITE_PICKAXE,
            "Текущий этап: " + stage + " - " + service.getStageName(stage),
            List.of(
                "Накопано блоков: " + mined,
                stage >= 5 ? "Максимальный этап достигнут" : "До следующего этапа: " + left,
                stage >= 5 ? "Цель: " + next : "Следующая цель: " + next
            )
        ));

        for (int s = 0; s <= 5; s++) {
            Material mat = s <= stage ? Material.EMERALD_BLOCK : Material.DEEPSLATE_TILES;
            menu.setItem(10 + s, buildItem(
                mat,
                "Этап " + s + ": " + service.getStageName(s),
                List.of("Порог: " + service.getStageThreshold(s) + " блоков")
            ));
        }

        menu.setItem(22, buildItem(
            Material.CLOCK,
            "Подсказка",
            List.of("Чтобы расти быстрее:", "- копай руду", "- используй молот", "- ломай жилы с Shift")
        ));

        player.openInventory(menu);
    }

    private static void fill(Inventory inv, ItemStack item) {
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, item);
        }
    }

    private static ItemStack buildItem(Material material, String name, List<String> lines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(color("&6" + name));
        List<String> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(color("&7" + line));
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}