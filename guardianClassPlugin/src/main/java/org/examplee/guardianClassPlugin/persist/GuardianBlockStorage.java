package org.examplee.guardianClassPlugin.persist;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.examplee.guardianClassPlugin.GuardianClassPlugin;
import org.examplee.guardianClassPlugin.store.GuardianBlockStore;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class GuardianBlockStorage {

    private final GuardianClassPlugin plugin;

    private File file;
    private YamlConfiguration cfg;

    public GuardianBlockStorage(GuardianClassPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(GuardianBlockStore store) {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), "guardian_blocks.yml");
        cfg = YamlConfiguration.loadConfiguration(file);

        for (World w : Bukkit.getWorlds()) {
            UUID wid = w.getUID();

            List<String> life = cfg.getStringList(wid + ".life");
            List<String> flowers = cfg.getStringList(wid + ".flowers");

            if (life != null) store.loadLife(wid, life);
            if (flowers != null) store.loadFlowers(wid, flowers);
        }
    }

    public void save(GuardianBlockStore store) {
        if (file == null) return;
        if (cfg == null) cfg = new YamlConfiguration();

        cfg.getKeys(false).forEach(k -> cfg.set(k, null));

        for (World w : Bukkit.getWorlds()) {
            UUID wid = w.getUID();
            cfg.set(wid + ".life", store.serializeLife(wid));
            cfg.set(wid + ".flowers", store.serializeFlowers(wid));
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить guardian_blocks.yml: " + e.getMessage());
        }
    }
}
