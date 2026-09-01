package org.examplee.leperClassPlugin.listeners;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.examplee.leperClassPlugin.LeperClassPlugin;

public final class ConsumeListener
implements Listener {
    private static final Set<Material> MEAT_AND_FISH = EnumSet.of(Material.BEEF, new Material[]{Material.COOKED_BEEF, Material.PORKCHOP, Material.COOKED_PORKCHOP, Material.CHICKEN, Material.COOKED_CHICKEN, Material.MUTTON, Material.COOKED_MUTTON, Material.RABBIT, Material.COOKED_RABBIT, Material.ROTTEN_FLESH, Material.COD, Material.COOKED_COD, Material.SALMON, Material.COOKED_SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH});
    private static final Set<Material> GOLDEN_FOOD = EnumSet.of(Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_CARROT);
    private final LeperClassPlugin plugin;

    public ConsumeListener(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemMeta itemMeta;
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        if (this.plugin.tags.isLeperBlood(item)) {
            p.addPotionEffect(new PotionEffect(this.plugin.effects.POISON, 240, 1));
            if (!this.plugin.data.isLeper(p)) {
                this.plugin.infection.startInfection(p);
            }
            return;
        }
        if (this.plugin.tags.isThickLeperBlood(item)) {
            p.addPotionEffect(new PotionEffect(this.plugin.effects.POISON, 160, 1));
            if (this.plugin.effects.NAUSEA != null) {
                p.addPotionEffect(new PotionEffect(this.plugin.effects.NAUSEA, 240, 0));
            }
            return;
        }
        if (this.plugin.tags.isSterileLeperBlood(item)) {
            return;
        }
        if (!this.plugin.data.isLeper(p)) {
            return;
        }
        Material type = item.getType();
        if (type.isEdible() && !MEAT_AND_FISH.contains(type) && !GOLDEN_FOOD.contains(type)) {
            e.setCancelled(true);
            this.plugin.msg.error((CommandSender)p, "\u041f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u044b\u0435 \u043c\u043e\u0433\u0443\u0442 \u0435\u0441\u0442\u044c \u0442\u043e\u043b\u044c\u043a\u043e \u043c\u044f\u0441\u043e/\u0440\u044b\u0431\u0443 \u0438 \u0437\u043e\u043b\u043e\u0442\u0443\u044e \u0435\u0434\u0443.");
            return;
        }
        if (this.plugin.effects.FIRE_RES != null) {
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> p.removePotionEffect(this.plugin.effects.FIRE_RES));
        }
        if (type == Material.ROTTEN_FLESH) {
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> p.removePotionEffect(PotionEffectType.HUNGER));
        }
        if ((itemMeta = item.getItemMeta()) instanceof PotionMeta) {
            boolean healingPotion;
            PotionMeta pm = (PotionMeta)itemMeta;
            boolean poisonPotion = this.potionHas(pm, "POISON");
            boolean harmPotion = this.potionHas(pm, "HARM") || this.potionHas(pm, "INSTANT_DAMAGE");
            boolean bl = healingPotion = this.potionHas(pm, "HEAL") || this.potionHas(pm, "INSTANT_HEALTH") || this.potionHas(pm, "REGEN");
            if (poisonPotion) {
                this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
                    this.plugin.balance.heal(p, this.plugin.settings.leperHealFromPoison);
                    p.removePotionEffect(PotionEffectType.POISON);
                });
            }
            if (harmPotion) {
                this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.plugin.balance.heal(p, this.plugin.settings.leperHealFromHarm));
            }
            if (healingPotion) {
                this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
                    this.plugin.balance.hurt(p, this.plugin.settings.leperDamageFromHeal);
                    this.plugin.msg.error((CommandSender)p, "\u0414\u043b\u044f \u043f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u043e\u0433\u043e \u044d\u0442\u043e \u0437\u0435\u043b\u044c\u0435 \u043e\u0431\u0435\u0440\u043d\u0443\u043b\u043e\u0441\u044c \u0431\u043e\u043b\u044c\u044e.");
                });
            }
            if (this.plugin.effects.FIRE_RES != null && this.potionHas(pm, "FIRE_RES")) {
                e.setCancelled(true);
                this.plugin.msg.error((CommandSender)p, "\u041f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u044b\u0435 \u043d\u0435 \u043c\u043e\u0433\u0443\u0442 \u043f\u0438\u0442\u044c \u043e\u0433\u043d\u0435\u0441\u0442\u043e\u0439\u043a\u043e\u0441\u0442\u044c.");
            }
        }
    }

    private boolean potionHas(PotionMeta meta, String token) {
        token = token.toUpperCase();
        for (PotionEffect pe : meta.getCustomEffects()) {
            if (!pe.getType().getName().toUpperCase().contains(token)) continue;
            return true;
        }
        try {
            Method m = meta.getClass().getMethod("getBasePotionType", new Class[0]);
            Object base = m.invoke(meta, new Object[0]);
            if (base != null && base.toString().toUpperCase().contains(token)) {
                return true;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return false;
    }
}

