package org.examplee.leperClassPlugin.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.util.Compat;
import org.examplee.leperClassPlugin.util.ParticlesUtil;
import org.examplee.leperClassPlugin.util.StunUtil;
import org.examplee.leperClassPlugin.util.TextUtil;

public final class PlagueStickListener
implements Listener {
    private final LeperClassPlugin plugin;
    private final Map<UUID, Long> rcCooldown = new HashMap<UUID, Long>();

    public PlagueStickListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onRightClick(PlayerInteractEvent e) {
        long now;
        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack used = e.getItem();
        if (used == null || !this.plugin.tags.isPlagueStick(used)) {
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (a == Action.RIGHT_CLICK_BLOCK && !p.isSneaking()) {
            e.setCancelled(true);
        }
        if ((now = System.currentTimeMillis()) - this.rcCooldown.getOrDefault(p.getUniqueId(), 0L) < 700L) {
            return;
        }
        this.rcCooldown.put(p.getUniqueId(), now);
        Sound s = Compat.soundFirst("ENTITY_PANDA_SNEEZE", "ENTITY_SLIME_SQUISH", "ENTITY_SLIME_SQUISH_SMALL");
        p.getWorld().playSound(p.getLocation(), s, 0.85f, 0.95f);
        Location eye = p.getEyeLocation();
        ParticlesUtil.greenDust(p.getWorld(), eye, 10, 0.18, 0.18, 0.18, 1.6f);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onHit(EntityDamageByEntityEvent e) {
        Entity entity = e.getDamager();
        if (!(entity instanceof Player)) {
            return;
        }
        Player damager = (Player)entity;
        Entity entity2 = e.getEntity();
        if (!(entity2 instanceof LivingEntity)) {
            return;
        }
        LivingEntity target = (LivingEntity)entity2;
        ItemStack hand = damager.getInventory().getItemInMainHand();
        if (!this.plugin.tags.isPlagueStick(hand)) {
            return;
        }
        target.addPotionEffect(new PotionEffect(this.plugin.effects.POISON, 100, 1));
        if (target instanceof Player) {
            Player tp = (Player)target;
            StunUtil.stun(this.plugin, tp, 40);
            tp.sendMessage(TextUtil.ui(String.valueOf(ChatColor.DARK_GREEN) + "\u0422\u0435\u0431\u044f \u043e\u0433\u043b\u0443\u0448\u0438\u043b\u0430 \u043f\u0440\u043e\u043a\u0430\u0437\u0430!"));
            if (this.plugin.data.isDangerBlessed(damager) && !this.plugin.data.isLeper(tp)) {
                this.plugin.infection.addHit(tp);
            }
        }
        ParticlesUtil.greenDust(target.getWorld(), target.getLocation().add(0.0, 1.0, 0.0), 20, 0.4, 0.6, 0.4, 1.6f);
    }
}

