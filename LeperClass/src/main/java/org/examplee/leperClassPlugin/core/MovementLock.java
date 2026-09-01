package org.examplee.leperClassPlugin.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class MovementLock {
    private final Map<UUID, State> locks = new HashMap<UUID, State>();
    private final PotionEffectType slow;

    public MovementLock(PotionEffectType slow) {
        this.slow = slow;
    }

    public void lock(Player p) {
        State st = this.locks.computeIfAbsent(p.getUniqueId(), x -> new State(p.getWalkSpeed(), p.getFlySpeed()));
        ++st.locks;
        p.setSprinting(false);
        p.setWalkSpeed(0.0f);
        p.setFlySpeed(0.0f);
        if (this.slow != null) {
            p.addPotionEffect(new PotionEffect(this.slow, 40, 10, false, false, false));
        }
    }

    public void unlock(Player p) {
        State st = this.locks.get(p.getUniqueId());
        if (st == null) {
            return;
        }
        --st.locks;
        if (st.locks <= 0) {
            p.setWalkSpeed(st.walk);
            p.setFlySpeed(st.fly);
            this.locks.remove(p.getUniqueId());
        }
    }

    public void release(Player p) {
        State st = this.locks.remove(p.getUniqueId());
        if (st != null) {
            p.setWalkSpeed(st.walk);
            p.setFlySpeed(st.fly);
        }
    }

    private static final class State {
        final float walk;
        final float fly;
        int locks;

        State(float walk, float fly) {
            this.walk = walk;
            this.fly = fly;
        }
    }
}

