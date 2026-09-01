package org.examplee.leperClassPlugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.util.TextUtil;

public final class VaccineListener
implements Listener {
    private final LeperClassPlugin plugin;

    public VaccineListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        ItemStack used = e.getItem();
        if (used == null || !this.plugin.tags.isVaccine(used)) {
            return;
        }
        if (used.getType() != Material.POTION) {
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (this.plugin.data.isLeper(p)) {
            p.sendMessage(TextUtil.ui(String.valueOf(ChatColor.RED) + "\u0412\u0430\u043c \u044d\u0442\u043e \u0443\u0436\u0435 \u043d\u0435 \u043f\u043e\u043c\u043e\u0436\u0435\u0442. \u0412\u044b - \u041f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u044b\u0439."));
            return;
        }
        int stage = this.plugin.data.getInfectionStage(p);
        if (stage == 1 || stage == 2) {
            this.plugin.infection.cure(p);
            if (p.getGameMode() != GameMode.CREATIVE) {
                int amt = used.getAmount() - 1;
                if (amt <= 0) {
                    p.getInventory().setItemInMainHand(new ItemStack(Material.GLASS_BOTTLE));
                } else {
                    used.setAmount(amt);
                }
            }
        } else {
            p.sendMessage(TextUtil.ui(String.valueOf(ChatColor.YELLOW) + "\u0412\u044b \u043d\u0435 \u0437\u0430\u0440\u0430\u0436\u0435\u043d\u044b."));
        }
    }
}

