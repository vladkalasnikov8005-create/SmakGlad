package org.examplee.leperClassPlugin.infection;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.util.Compat;
import org.examplee.leperClassPlugin.util.EntityUtil;
import org.examplee.leperClassPlugin.util.TextUtil;

public final class InfectionManager {
    private final LeperClassPlugin plugin;

    public InfectionManager(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    public void addHit(Player target) {
        if (this.plugin.data.isLeper(target)) {
            return;
        }
        int hits = Math.min(3, this.plugin.data.getInfectionHits(target) + 1);
        this.plugin.data.setInfectionHits(target, hits);
        if (hits >= 3) {
            this.startInfection(target);
        }
    }

    public void startInfection(Player p) {
        if (this.plugin.data.getInfectionStage(p) > 0 || this.plugin.data.isLeper(p)) {
            return;
        }
        this.plugin.data.setInfectionStage(p, 1);
        this.plugin.data.setInfectionNextPhaseMs(p, System.currentTimeMillis() + this.plugin.settings.infectionPhaseMs);
        p.sendMessage(TextUtil.ui(String.valueOf(ChatColor.DARK_GREEN) + "\u0412\u044b \u0447\u0443\u0432\u0441\u0442\u0432\u0443\u0435\u0442\u0435 \u0441\u0435\u0431\u044f \u0441\u0442\u0440\u0430\u043d\u043d\u043e... \u041a\u0430\u0436\u0435\u0442\u0441\u044f, \u0432\u044b \u0437\u0430\u0440\u0430\u0437\u0438\u043b\u0438\u0441\u044c."));
        p.playSound(p.getLocation(), Compat.soundFirst("ENTITY_ZOMBIE_INFECT", "ENTITY_ZOMBIE_VILLAGER_CURE"), 1.0f, 0.5f);
        this.plugin.log.info("Infection stage1 started for " + p.getName());
    }

    public void checkProgression(Player p, long nowMs) {
        if (this.plugin.data.isLeper(p)) {
            return;
        }
        int stage = this.plugin.data.getInfectionStage(p);
        if (stage == 0) {
            return;
        }
        Long next = this.plugin.data.getInfectionNextPhaseMs(p);
        if (next == null || nowMs < next) {
            return;
        }
        if (stage == 1) {
            this.plugin.data.setInfectionStage(p, 2);
            this.plugin.data.setInfectionNextPhaseMs(p, nowMs + this.plugin.settings.infectionPhaseMs);
            p.sendMessage(TextUtil.ui(String.valueOf(ChatColor.RED) + "\u0412\u0430\u043c \u0441\u0442\u0430\u043b\u043e \u0445\u0443\u0436\u0435. \u0412\u0430\u0448\u0430 \u043a\u043e\u0436\u0430 \u043d\u0430\u0447\u0430\u043b\u0430 \u0433\u043e\u0440\u0435\u0442\u044c \u043d\u0430 \u0441\u043e\u043b\u043d\u0446\u0435!"));
            this.plugin.log.info("Infection stage2 started for " + p.getName());
            return;
        }
        if (stage == 2) {
            this.plugin.data.clearInfection(p);
            this.plugin.data.setLeper(p, true);
            if (this.plugin.effects.FIRE_RES != null) {
                p.removePotionEffect(this.plugin.effects.FIRE_RES);
            }
            EntityUtil.clearHostileTargets(p, 32.0);
            p.sendMessage(TextUtil.ui(String.valueOf(ChatColor.DARK_RED) + "\u0418\u043d\u0444\u0435\u043a\u0446\u0438\u044f \u043f\u043e\u0433\u043b\u043e\u0442\u0438\u043b\u0430 \u0432\u0430\u0441 \u043f\u043e\u043b\u043d\u043e\u0441\u0442\u044c\u044e. \u0412\u044b \u0441\u0442\u0430\u043b\u0438 \u041f\u0440\u043e\u043a\u0430\u0436\u0435\u043d\u043d\u044b\u043c."));
            this.plugin.log.info("Player converted to leper: " + p.getName());
        }
    }

    public void cure(Player p) {
        this.plugin.data.clearInfection(p);
        p.sendMessage(TextUtil.ui(String.valueOf(ChatColor.AQUA) + "\u0412\u044b \u043f\u0440\u0438\u043d\u044f\u043b\u0438 \u0432\u0430\u043a\u0446\u0438\u043d\u0443. \u0418\u043d\u0444\u0435\u043a\u0446\u0438\u044f \u043e\u0442\u0441\u0442\u0443\u043f\u0438\u043b\u0430!"));
        this.plugin.log.info("Infection cured for " + p.getName());
    }

    public void cureDataOnly(Player p) {
        this.plugin.data.clearInfection(p);
    }
}

