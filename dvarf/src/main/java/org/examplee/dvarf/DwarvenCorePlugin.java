package org.examplee.dvarf;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.examplee.dvarf.protection.AllowAllProtection;
import org.examplee.dvarf.protection.BuildProtection;

public final class DwarvenCorePlugin extends JavaPlugin {

    private DwarfService dwarfService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        this.dwarfService = new DwarfService(this);
        this.dwarfService.reloadSettings();

        BuildProtection buildProtection = resolveBuildProtection();

        getServer().getPluginManager().registerEvents(new DwarfListener(this, dwarfService, buildProtection), this);
        getServer().getPluginManager().registerEvents(new DwarfShieldManager(this, dwarfService), this);

        DwarfCommand dwarfCommand = new DwarfCommand(dwarfService);
        if (getCommand("dwarf") != null) {
            getCommand("dwarf").setExecutor(dwarfCommand);
            getCommand("dwarf").setTabCompleter(dwarfCommand);
        }
        if (getCommand("dmenu") != null) {
            getCommand("dmenu").setExecutor(dwarfCommand);
            getCommand("dmenu").setTabCompleter(dwarfCommand);
        }

        dwarfService.registerRecipes();
        dwarfService.startTickLoop();

        for (Player player : getServer().getOnlinePlayers()) {
            if (dwarfService.isDwarf(player)) {
                dwarfService.applyDwarfAttributes(player);
            }
        }

        getLogger().info("DwarvenCore enabled.");
    }

    @Override
    public void onDisable() {
        if (dwarfService != null) {
            dwarfService.shutdown();
        }
        getLogger().info("DwarvenCore disabled.");
    }

    public NamespacedKey key(String value) {
        return new NamespacedKey(this, value);
    }

    private BuildProtection resolveBuildProtection() {
        getLogger().info("Using universal region-safe mode (checks cancellation from protection plugins).");
        return new AllowAllProtection();
    }
}