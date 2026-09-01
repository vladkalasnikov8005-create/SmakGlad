package org.examplee.palePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.examplee.palePlugin.command.PaleSpreadCommand;
import org.examplee.palePlugin.core.PaleConfig;
import org.examplee.palePlugin.core.PaleKeys;
import org.examplee.palePlugin.core.PaleMaterials;
import org.examplee.palePlugin.engine.PaleEngine;
import org.examplee.palePlugin.gui.AdminGui;
import org.examplee.palePlugin.gui.AdminGuiListener;
import org.examplee.palePlugin.items.PaleItems;
import org.examplee.palePlugin.listeners.AdminPurgeListener;
import org.examplee.palePlugin.listeners.BlockListener;
import org.examplee.palePlugin.listeners.ChunkListener;
import org.examplee.palePlugin.listeners.ItemUseListener;
import org.examplee.palePlugin.persist.BiomesStorage;
import org.examplee.palePlugin.persist.WardsStorage;
import org.examplee.palePlugin.tasks.AdminPurgeManager;
import org.examplee.palePlugin.tasks.SpreadController;

public final class PalePlugin
extends JavaPlugin {
    public PaleConfig cfg;
    public PaleKeys keys;
    public PaleMaterials mats;
    public PaleEngine engine;
    public PaleItems items;
    public SpreadController spread;
    public WardsStorage wardsStorage;
    public BiomesStorage biomesStorage;
    public AdminGui adminGui;
    public AdminPurgeManager purge;

    public void onEnable() {
        this.cfg = new PaleConfig(this);
        this.cfg.setupDefaults();
        this.cfg.load();
        this.keys = new PaleKeys((Plugin)this);
        this.mats = new PaleMaterials(this);
        if (!this.mats.resolveOrDisable()) {
            return;
        }
        this.engine = new PaleEngine(this, this.cfg, this.mats);
        this.engine.resolveInfectedBiomeOrDisableBiome();
        this.wardsStorage = new WardsStorage(this);
        this.biomesStorage = new BiomesStorage(this);
        this.wardsStorage.load(this.engine);
        this.biomesStorage.load(this.engine);
        this.items = new PaleItems(this);
        this.spread = new SpreadController(this);
        this.spread.refreshLoadedChunkCounts();
        this.spread.startAlwaysOnTasks();
        this.purge = new AdminPurgeManager(this);
        this.adminGui = new AdminGui(this);
        PluginCommand c = this.getCommand("palespread");
        if (c != null) {
            PaleSpreadCommand cmd = new PaleSpreadCommand(this);
            c.setExecutor((CommandExecutor)cmd);
            c.setTabCompleter((TabCompleter)cmd);
        } else {
            this.getLogger().warning("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 /palespread \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430 (\u043f\u0440\u043e\u0432\u0435\u0440\u044c plugin.yml).");
        }
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents((Listener)new ChunkListener(this), (Plugin)this);
        pm.registerEvents((Listener)new ItemUseListener(this), (Plugin)this);
        pm.registerEvents((Listener)new BlockListener(this), (Plugin)this);
        pm.registerEvents((Listener)new AdminGuiListener(this), (Plugin)this);
        pm.registerEvents((Listener)new AdminPurgeListener(this), (Plugin)this);
        this.items.registerRecipes();
        this.getLogger().info("[PalePlugin] Enabled (no contracts). infectedTypes=" + String.valueOf(this.mats.getInfectedTypes()));
    }

    public void onDisable() {
        try {
            if (this.spread != null) {
                this.spread.stopAllTasks();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            if (this.purge != null) {
                this.purge.stopAll();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            if (this.wardsStorage != null) {
                this.wardsStorage.save(this.engine);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            if (this.biomesStorage != null) {
                this.biomesStorage.save(this.engine);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.getLogger().info("[PalePlugin] Disabled.");
    }

    public int apiInfect(Location center, int radius, int maxBlocks) {
        if (this.engine == null) {
            return 0;
        }
        return this.engine.apiInfect(center, radius, maxBlocks);
    }
}

