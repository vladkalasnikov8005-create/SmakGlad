package org.examplee.vampirest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SaltBlockService {

    private final VampireRacePlugin plugin;
    private final Set<String> saltBlocks = new HashSet<>();
    private final File file;

    public SaltBlockService(VampireRacePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "salt-blocks.yml");
    }

    public void mark(Block block) {
        saltBlocks.add(key(block.getLocation()));
    }

    public void unmark(Block block) {
        saltBlocks.remove(key(block.getLocation()));
    }

    public boolean isSaltBlock(Block block) {
        if (block.getType() == Material.CALCITE && saltBlocks.contains(key(block.getLocation()))) {
            return true;
        }
        for (int y = -25; y <= 25; y++) {
            Location check = block.getLocation().clone().add(0, y, 0);
            if (saltBlocks.contains(key(check))) {
                return true;
            }
        }
        return false;
    }

    public void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        saltBlocks.clear();
        saltBlocks.addAll(cfg.getStringList("blocks"));
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("blocks", saltBlocks.stream().toList());
        try {
            cfg.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save salt-blocks.yml: " + exception.getMessage());
        }
    }

    private String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}