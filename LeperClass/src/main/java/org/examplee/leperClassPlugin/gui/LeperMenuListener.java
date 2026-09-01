package org.examplee.leperClassPlugin.gui;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.gui.LeperMenuHolder;
import org.examplee.leperClassPlugin.util.EntityUtil;
import org.examplee.leperClassPlugin.util.InventoryUtil;

public final class LeperMenuListener
implements Listener {
    private final LeperClassPlugin plugin;

    public LeperMenuListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent e) {
        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (!(inventoryHolder instanceof LeperMenuHolder)) {
            return;
        }
        LeperMenuHolder holder = (LeperMenuHolder)inventoryHolder;
        e.setCancelled(true);
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player admin = (Player)humanEntity;
        Player target = Bukkit.getPlayer((UUID)holder.getTarget());
        if (target == null) {
            return;
        }
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize()) {
            return;
        }
        if (slot == 11) {
            this.plugin.data.setLeper(target, true);
            this.plugin.infection.cureDataOnly(target);
            EntityUtil.clearHostileTargets(target, 32.0);
            admin.sendMessage(String.valueOf(ChatColor.GREEN) + target.getName() + " \u0442\u0435\u043f\u0435\u0440\u044c \u041f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u044b\u0439.");
            this.plugin.menu.open(admin, target);
            return;
        }
        if (slot == 15) {
            this.plugin.data.setLeper(target, false);
            this.plugin.infection.cureDataOnly(target);
            admin.sendMessage(String.valueOf(ChatColor.GREEN) + target.getName() + " \u0431\u043e\u043b\u044c\u0448\u0435 \u043d\u0435 \u041f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u044b\u0439.");
            this.plugin.menu.open(admin, target);
            return;
        }
        switch (slot) {
            case 17: {
                InventoryUtil.giveOrDrop(target, this.plugin.items.makeUmbrellaTiny());
                break;
            }
            case 18: {
                InventoryUtil.giveOrDrop(target, this.plugin.items.makeUmbrellaWeak());
                break;
            }
            case 19: {
                InventoryUtil.giveOrDrop(target, this.plugin.items.makeUmbrellaNormal());
                break;
            }
            case 20: {
                InventoryUtil.giveOrDrop(target, this.plugin.items.makeUmbrellaStrong());
                break;
            }
            case 22: {
                InventoryUtil.giveOrDrop(target, this.plugin.items.makePlagueStick());
                break;
            }
            case 23: {
                InventoryUtil.giveOrDrop(target, this.plugin.items.makeSacrificialKnife());
                break;
            }
            case 24: {
                InventoryUtil.giveOrDrop(target, this.plugin.items.makePlagueBomb());
                break;
            }
            case 25: {
                InventoryUtil.giveOrDrop(target, this.plugin.items.makeLeperBlood());
                break;
            }
            case 26: {
                InventoryUtil.giveOrDrop(target, this.plugin.items.makeVaccine());
            }
        }
    }
}

