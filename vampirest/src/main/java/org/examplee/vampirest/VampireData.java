package org.examplee.vampirest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record VampireData(
        UUID playerId,
        int level,
        double blood,
        int hunger,
        long lastBloodDrain,
        Map<String, Long> abilityCooldowns,
        boolean leader,
        boolean nightVisionEnabled,
        boolean overlord
) {
    public VampireData(UUID playerId) {
        this(playerId, 1, 100.0, 20, System.currentTimeMillis(), new HashMap<>(), false, true, false);
    }

    public VampireData withLevel(int newLevel) {
        return new VampireData(playerId, newLevel, blood, hunger, lastBloodDrain, new HashMap<>(abilityCooldowns), leader, nightVisionEnabled, overlord);
    }

    public VampireData withBlood(double newBlood, double maxBlood) {
        return new VampireData(playerId, level, Math.max(0.0, Math.min(newBlood, maxBlood)), hunger, lastBloodDrain, new HashMap<>(abilityCooldowns), leader, nightVisionEnabled, overlord);
    }

    public VampireData withHunger(int newHunger) {
        return new VampireData(playerId, level, blood, Math.max(0, Math.min(newHunger, 20)), lastBloodDrain, new HashMap<>(abilityCooldowns), leader, nightVisionEnabled, overlord);
    }

    public VampireData withLastDrain(long time) {
        return new VampireData(playerId, level, blood, hunger, time, new HashMap<>(abilityCooldowns), leader, nightVisionEnabled, overlord);
    }

    public VampireData withCooldown(String ability, long cooldownEnd) {
        var newCooldowns = new HashMap<>(abilityCooldowns);
        newCooldowns.put(ability, cooldownEnd);
        return new VampireData(playerId, level, blood, hunger, lastBloodDrain, newCooldowns, leader, nightVisionEnabled, overlord);
    }

    public VampireData withLeader(boolean isLeader) {
        return new VampireData(playerId, level, blood, hunger, lastBloodDrain, new HashMap<>(abilityCooldowns), isLeader, nightVisionEnabled, overlord);
    }

    public VampireData withNightVision(boolean enabled) {
        return new VampireData(playerId, level, blood, hunger, lastBloodDrain, new HashMap<>(abilityCooldowns), leader, enabled, overlord);
    }

    public VampireData withOverlord(boolean enabled) {
        return new VampireData(playerId, level, blood, hunger, lastBloodDrain, new HashMap<>(abilityCooldowns), leader, nightVisionEnabled, enabled);
    }

    public boolean isOnCooldown(String ability) {
        return abilityCooldowns.getOrDefault(ability, 0L) > System.currentTimeMillis();
    }

    public long cooldownLeftMillis(String ability) {
        return Math.max(0L, abilityCooldowns.getOrDefault(ability, 0L) - System.currentTimeMillis());
    }
}