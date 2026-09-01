package org.examplee.palePlugin.persist;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.examplee.palePlugin.PalePlugin;
import org.examplee.palePlugin.engine.PaleEngine;
import org.examplee.palePlugin.store.BiomeStore;

public final class BiomesStorage {
    private final PalePlugin plugin;
    private File file;
    private YamlConfiguration cfg;

    public BiomesStorage(PalePlugin plugin) {
        this.plugin = plugin;
    }

    public void load(PaleEngine engine) {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        this.file = new File(this.plugin.getDataFolder(), "biomes.yml");
        this.cfg = YamlConfiguration.loadConfiguration((File)this.file);
        for (World w : Bukkit.getWorlds()) {
            UUID wid = w.getUID();
            List<String> list = this.cfg.getStringList(wid.toString());
            if (list == null || list.isEmpty()) continue;
            BiomeStore bs = engine.biomes(w);
            for (String s : list) {
                bs.loadSerializedLine(s);
            }
        }
    }

    public void save(PaleEngine engine) {
        if (this.file == null) {
            return;
        }
        if (this.cfg == null) {
            this.cfg = new YamlConfiguration();
        }
        this.cfg.getKeys(false).forEach(k -> this.cfg.set(k, null));
        for (Map.Entry<UUID, BiomeStore> entry : engine.biomesByWorld().entrySet()) {
            this.cfg.set(entry.getKey().toString(), entry.getValue().serialize());
        }
        try {
            this.cfg.save(this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("[PalePlugin] \u041d\u0435 \u0441\u043c\u043e\u0433 \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c biomes.yml: " + e.getMessage());
        }
    }
}

