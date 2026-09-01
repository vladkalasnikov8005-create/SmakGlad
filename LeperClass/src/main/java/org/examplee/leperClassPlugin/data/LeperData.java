package org.examplee.leperClassPlugin.data;

import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.examplee.leperClassPlugin.core.LeperKeys;

public final class LeperData {
    private final LeperKeys keys;

    public LeperData(LeperKeys keys) {
        this.keys = keys;
    }

    public boolean isLeper(Player p) {
        Byte val = (Byte)p.getPersistentDataContainer().get(this.keys.leperKey, PersistentDataType.BYTE);
        return val != null && val == 1;
    }

    public void setLeper(Player p, boolean val) {
        if (val) {
            p.getPersistentDataContainer().set(this.keys.leperKey, PersistentDataType.BYTE, (Object)1);
        } else {
            p.getPersistentDataContainer().remove(this.keys.leperKey);
        }
    }

    public int getInfectionHits(Player p) {
        return (Integer)p.getPersistentDataContainer().getOrDefault(this.keys.infectionHitsKey, PersistentDataType.INTEGER, (Object)0);
    }

    public void setInfectionHits(Player p, int hits) {
        p.getPersistentDataContainer().set(this.keys.infectionHitsKey, PersistentDataType.INTEGER, (Object)hits);
    }

    public int getInfectionStage(Player p) {
        return (Integer)p.getPersistentDataContainer().getOrDefault(this.keys.infectionStageKey, PersistentDataType.INTEGER, (Object)0);
    }

    public void setInfectionStage(Player p, int stage) {
        p.getPersistentDataContainer().set(this.keys.infectionStageKey, PersistentDataType.INTEGER, (Object)stage);
    }

    public Long getInfectionNextPhaseMs(Player p) {
        return (Long)p.getPersistentDataContainer().get(this.keys.infectionNextPhaseKey, PersistentDataType.LONG);
    }

    public void setInfectionNextPhaseMs(Player p, long ms) {
        p.getPersistentDataContainer().set(this.keys.infectionNextPhaseKey, PersistentDataType.LONG, (Object)ms);
    }

    public void clearInfection(Player p) {
        p.getPersistentDataContainer().remove(this.keys.infectionHitsKey);
        p.getPersistentDataContainer().remove(this.keys.infectionStageKey);
        p.getPersistentDataContainer().remove(this.keys.infectionNextPhaseKey);
    }

    public boolean isDangerBlessed(Player p) {
        Byte val = (Byte)p.getPersistentDataContainer().get(this.keys.dangerBlessKey, PersistentDataType.BYTE);
        return val != null && val == 1;
    }

    public void setDangerBlessed(Player p, boolean blessed) {
        if (blessed) {
            p.getPersistentDataContainer().set(this.keys.dangerBlessKey, PersistentDataType.BYTE, (Object)1);
        } else {
            p.getPersistentDataContainer().remove(this.keys.dangerBlessKey);
        }
    }

    public long getRageUntil(Player p) {
        Long until = (Long)p.getPersistentDataContainer().get(this.keys.rageUntilKey, PersistentDataType.LONG);
        return until == null ? 0L : until;
    }

    public void setRageUntil(Player p, long ms) {
        p.getPersistentDataContainer().set(this.keys.rageUntilKey, PersistentDataType.LONG, (Object)ms);
    }
}

