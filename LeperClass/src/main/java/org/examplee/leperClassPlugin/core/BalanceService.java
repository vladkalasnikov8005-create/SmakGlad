package org.examplee.leperClassPlugin.core;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.examplee.leperClassPlugin.core.PluginSettings;

public final class BalanceService {
    private final PluginSettings settings;

    public BalanceService(PluginSettings settings) {
        this.settings = settings;
    }

    public void heal(Player p, double amount) {
        if (p == null || amount <= 0.0) {
            return;
        }
        double max = 20.0;
        try {
            AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) {
                max = attr.getValue();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        p.setHealth(Math.min(max, p.getHealth() + amount));
    }

    public void hurt(Player p, double amount) {
        if (p == null || amount <= 0.0) {
            return;
        }
        p.damage(amount);
    }

    public PluginSettings settings() {
        return this.settings;
    }
}

