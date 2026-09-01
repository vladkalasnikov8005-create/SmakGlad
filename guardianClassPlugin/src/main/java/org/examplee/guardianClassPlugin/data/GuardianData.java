package org.examplee.guardianClassPlugin.data;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.examplee.guardianClassPlugin.GuardianClassPlugin;

public final class GuardianData {

    private final GuardianClassPlugin plugin;

    public GuardianData(GuardianClassPlugin plugin) {
        this.plugin = plugin;
    }

    public int getStage(Player p) {
        return p.getPersistentDataContainer().getOrDefault(plugin.keys.STAGE, PersistentDataType.INTEGER, 0);
    }

    public void setStage(Player p, int stage) {
        stage = Math.max(0, Math.min(3, stage));
        if (stage == 0) {
            p.getPersistentDataContainer().remove(plugin.keys.STAGE);
            p.getPersistentDataContainer().remove(plugin.keys.STONE_TIME_SEC);
            p.sendMessage(ChatColor.YELLOW + "Статус Хранителя снят.");
            return;
        }
        p.getPersistentDataContainer().set(plugin.keys.STAGE, PersistentDataType.INTEGER, stage);

        String name = stageName(stage);
        p.sendMessage(ChatColor.GREEN + "Твоя стадия Хранителя: " + ChatColor.WHITE + name);
    }

    public boolean isGuardian(Player p) {
        return getStage(p) > 0;
    }

    public boolean isNearOrTrue(Player p) {
        int s = getStage(p);
        return s == 2 || s == 3;
    }

    public boolean isTrue(Player p) {
        return getStage(p) == 3;
    }

    public long getStoneTimeSec(Player p) {
        Long v = p.getPersistentDataContainer().get(plugin.keys.STONE_TIME_SEC, PersistentDataType.LONG);
        return v == null ? 0L : Math.max(0L, v);
    }

    public void setStoneTimeSec(Player p, long sec) {
        if (sec <= 0) p.getPersistentDataContainer().remove(plugin.keys.STONE_TIME_SEC);
        else p.getPersistentDataContainer().set(plugin.keys.STONE_TIME_SEC, PersistentDataType.LONG, sec);
    }

    public void markStoneInterrupted(Player p, boolean interrupted) {
        if (interrupted) {
            p.getPersistentDataContainer().set(plugin.keys.STONE_INTERRUPTED, PersistentDataType.BYTE, (byte) 1);
            return;
        }
        p.getPersistentDataContainer().remove(plugin.keys.STONE_INTERRUPTED);
    }

    public boolean consumeStoneInterrupted(Player p) {
        Byte v = p.getPersistentDataContainer().get(plugin.keys.STONE_INTERRUPTED, PersistentDataType.BYTE);
        if (v == null || v != (byte) 1) return false;
        p.getPersistentDataContainer().remove(plugin.keys.STONE_INTERRUPTED);
        return true;
    }

    public boolean isTreeCapEnabled(Player p) {
        Byte v = p.getPersistentDataContainer().get(plugin.keys.TREECAP_ENABLED, PersistentDataType.BYTE);
        return v == null || v == (byte) 1;
    }

    public void setTreeCapEnabled(Player p, boolean enabled) {
        if (enabled) {
            // true by default, remove custom flag to keep PDC clean
            p.getPersistentDataContainer().remove(plugin.keys.TREECAP_ENABLED);
            return;
        }
        p.getPersistentDataContainer().set(plugin.keys.TREECAP_ENABLED, PersistentDataType.BYTE, (byte) 0);
    }

    public String stageName(int stage) {
        return switch (stage) {
            case 1 -> "Неистинный";
            case 2 -> "Приближённый";
            case 3 -> "Истинный";
            default -> "Нет";
        };
    }
}
