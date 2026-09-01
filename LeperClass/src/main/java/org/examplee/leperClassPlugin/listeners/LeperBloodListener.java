package org.examplee.leperClassPlugin.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.util.InventoryUtil;

public final class LeperBloodListener
implements Listener {
    private final LeperClassPlugin plugin;
    private final Map<UUID, Long> knifeCd = new HashMap<UUID, Long>();

    public LeperBloodListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onKnifeUse(PlayerInteractEvent e) {
        long cd;
        long last;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack hand = e.getItem();
        if (hand == null || !this.plugin.tags.isSacrificialKnife(hand)) {
            return;
        }
        if (!this.plugin.data.isLeper(p)) {
            this.plugin.msg.error((CommandSender)p, "\u0422\u043e\u043b\u044c\u043a\u043e \u043f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u044b\u0439 \u043c\u043e\u0436\u0435\u0442 \u0434\u043e\u0431\u044b\u0442\u044c \u043a\u0440\u043e\u0432\u044c \u044d\u0442\u0438\u043c \u043d\u043e\u0436\u043e\u043c.");
            return;
        }
        long now = System.currentTimeMillis();
        if (now - (last = this.knifeCd.getOrDefault(p.getUniqueId(), 0L).longValue()) < (cd = this.plugin.settings.knifeCooldownMs)) {
            long sec = (cd - (now - last)) / 1000L;
            this.plugin.msg.warn((CommandSender)p, "\u041d\u043e\u0436\u0438\u043a \u0435\u0449\u0435 \u043d\u0435 \u0433\u043e\u0442\u043e\u0432. \u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: " + sec + " \u0441\u0435\u043a.");
            return;
        }
        this.knifeCd.put(p.getUniqueId(), now);
        p.damage(2.0);
        InventoryUtil.giveOrDrop(p, this.plugin.items.makeLeperBlood());
        if (this.plugin.effects.MINING_FATIGUE != null) {
            p.addPotionEffect(new PotionEffect(this.plugin.effects.MINING_FATIGUE, this.plugin.settings.knifeFatigueTicks, 0));
        }
        if (this.plugin.effects.WEAKNESS != null) {
            p.addPotionEffect(new PotionEffect(this.plugin.effects.WEAKNESS, this.plugin.settings.knifeWeakTicks, 0));
        }
        if (this.plugin.effects.SLOW != null) {
            p.addPotionEffect(new PotionEffect(this.plugin.effects.SLOW, this.plugin.settings.knifeSlowTicks, 0));
        }
        this.plugin.msg.error((CommandSender)p, "\u0412\u044b \u0434\u043e\u0431\u044b\u043b\u0438 \u043a\u0440\u043e\u0432\u044c. \u0426\u0435\u043d\u0430 \u0432\u044b\u0441\u043e\u043a\u0430.");
        this.plugin.log.info("Self blood extracted by " + p.getName());
    }

    @EventHandler
    public void onLeperKilled(EntityDeathEvent e) {
        LivingEntity livingEntity = e.getEntity();
        if (!(livingEntity instanceof Player)) {
            return;
        }
        Player victim = (Player)livingEntity;
        if (!this.plugin.data.isLeper(victim)) {
            return;
        }
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < 0.6) {
            e.getDrops().add(this.plugin.items.makeLeperBlood());
            this.plugin.log.info("Blood dropped from killed leper: " + victim.getName());
        }
        if (!this.plugin.data.isLeper(killer) && ThreadLocalRandom.current().nextDouble() < 0.3) {
            this.plugin.infection.addHit(killer);
            this.plugin.msg.warn((CommandSender)killer, "\u041a\u0440\u043e\u0432\u044c \u043f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u043e\u0433\u043e \u043f\u043e\u043f\u0430\u043b\u0430 \u0432 \u0440\u0430\u043d\u044b. \u0412\u044b \u043c\u043e\u0433\u043b\u0438 \u0437\u0430\u0440\u0430\u0437\u0438\u0442\u044c\u0441\u044f.");
        }
    }
}

