package org.examplee.vampirest;

import org.bukkit.plugin.java.JavaPlugin;

public final class VampireRacePlugin extends JavaPlugin {

    private VampireManager vampireManager;
    private AbilityManager abilityManager;
    private CustomItemManager customItemManager;
    private SaltBlockService saltBlockService;
    private BiteCommand biteCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        vampireManager = new VampireManager(this);
        abilityManager = new AbilityManager(this);
        customItemManager = new CustomItemManager(this);
        saltBlockService = new SaltBlockService(this);
        biteCommand = new BiteCommand(this);

        vampireManager.loadData();
        saltBlockService.load();

        getServer().getPluginManager().registerEvents(new VampireListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomItemListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantmentListener(this), this);

        var command = getCommand("vampire");
        if (command != null) {
            command.setExecutor(new VampireCommand(this));
            command.setTabCompleter(new VampireTabCompleter(this));
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new VampirePlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI integration enabled");
        }

        getServer().getScheduler().runTaskTimer(this, vampireManager::drainBloodAll, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, vampireManager::applyPassivesAll, 20L, 20L);
        getLogger().info("VampireRace enabled");
    }

    @Override
    public void onDisable() {
        saltBlockService.save();
        vampireManager.saveData();
        getLogger().info("VampireRace disabled");
    }

    public VampireManager getVampireManager() { return vampireManager; }
    public AbilityManager getAbilityManager() { return abilityManager; }
    public CustomItemManager getCustomItemManager() { return customItemManager; }
    public SaltBlockService getSaltBlockService() { return saltBlockService; }
    public BiteCommand getBiteCommand() { return biteCommand; }
}