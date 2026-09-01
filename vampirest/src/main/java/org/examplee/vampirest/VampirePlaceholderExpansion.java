package org.examplee.vampirest;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class VampirePlaceholderExpansion extends PlaceholderExpansion {

    private final VampireRacePlugin plugin;

    public VampirePlaceholderExpansion(VampireRacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "vampire";
    }

    @Override
    public String getAuthor() {
        return "examplee";
    }

    @Override
    public String getVersion() {
        return "2.1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getPlayer() == null) {
            return "";
        }
        var online = player.getPlayer();
        if (!plugin.getVampireManager().isVampire(online)) {
            return "0";
        }
        VampireData data = plugin.getVampireManager().getVampireData(online);
        return switch (params.toLowerCase()) {
            case "blood" -> String.format("%.1f", data.blood());
            case "hunger" -> String.valueOf(data.hunger());
            case "level" -> String.valueOf(data.level());
            case "leader" -> String.valueOf(data.leader());
            case "nightvision" -> String.valueOf(data.nightVisionEnabled());
            default -> "";
        };
    }
}