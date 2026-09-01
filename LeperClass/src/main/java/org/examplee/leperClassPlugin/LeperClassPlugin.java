package org.examplee.leperClassPlugin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.examplee.leperClassPlugin.command.LeperCommand;
import org.examplee.leperClassPlugin.command.LeperTabCompleter;
import org.examplee.leperClassPlugin.core.BalanceService;
import org.examplee.leperClassPlugin.core.EffectRegistry;
import org.examplee.leperClassPlugin.core.ItemMigrationService;
import org.examplee.leperClassPlugin.core.LeperKeys;
import org.examplee.leperClassPlugin.core.MovementLock;
import org.examplee.leperClassPlugin.core.PaleHook;
import org.examplee.leperClassPlugin.core.PluginSettings;
import org.examplee.leperClassPlugin.data.LeperData;
import org.examplee.leperClassPlugin.gui.LeperMenu;
import org.examplee.leperClassPlugin.gui.LeperMenuListener;
import org.examplee.leperClassPlugin.infection.InfectionManager;
import org.examplee.leperClassPlugin.items.ItemFactory;
import org.examplee.leperClassPlugin.items.ItemTags;
import org.examplee.leperClassPlugin.listeners.CombatListener;
import org.examplee.leperClassPlugin.listeners.ConsumeListener;
import org.examplee.leperClassPlugin.listeners.ContactInfectionListener;
import org.examplee.leperClassPlugin.listeners.EffectBlockListener;
import org.examplee.leperClassPlugin.listeners.HungerListener;
import org.examplee.leperClassPlugin.listeners.InstantBrewingListener;
import org.examplee.leperClassPlugin.listeners.JoinQuitDeathListener;
import org.examplee.leperClassPlugin.listeners.LeperBloodListener;
import org.examplee.leperClassPlugin.listeners.MobIgnoreListener;
import org.examplee.leperClassPlugin.listeners.PlagueBombListener;
import org.examplee.leperClassPlugin.listeners.PlagueStickListener;
import org.examplee.leperClassPlugin.listeners.SneezeListener;
import org.examplee.leperClassPlugin.listeners.UmbrellaSyncListener;
import org.examplee.leperClassPlugin.listeners.UndeadPotionInversionListener;
import org.examplee.leperClassPlugin.listeners.VaccineListener;
import org.examplee.leperClassPlugin.tasks.SunAndInfectionTask;
import org.examplee.leperClassPlugin.umbrella.UmbrellaManager;
import org.examplee.leperClassPlugin.util.LogService;
import org.examplee.leperClassPlugin.util.MessageService;

public final class LeperClassPlugin
extends JavaPlugin {
    public LeperKeys keys;
    public EffectRegistry effects;
    public LeperData data;
    public PluginSettings settings;
    public BalanceService balance;
    public MovementLock movementLock;
    public PaleHook paleHook;
    public ItemTags tags;
    public ItemFactory items;
    public InfectionManager infection;
    public UmbrellaManager umbrella;
    public LeperMenu menu;
    public MessageService msg;
    public LogService log;
    public ItemMigrationService migration;
    private SunAndInfectionTask sunTask;

    public void onEnable() {
        this.saveDefaultConfig();
        this.settings = new PluginSettings(this);
        this.msg = new MessageService();
        this.log = new LogService(this);
        this.keys = new LeperKeys((Plugin)this);
        this.effects = new EffectRegistry();
        if (this.effects.POISON == null || this.effects.SLOW == null) {
            this.getLogger().severe("Missing required effects.");
            Bukkit.getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        this.data = new LeperData(this.keys);
        this.balance = new BalanceService(this.settings);
        this.movementLock = new MovementLock(this.effects.SLOW);
        this.paleHook = new PaleHook();
        this.paleHook.hook();
        this.tags = new ItemTags(this.keys);
        this.items = new ItemFactory(this.keys);
        this.infection = new InfectionManager(this);
        this.umbrella = new UmbrellaManager(this);
        this.migration = new ItemMigrationService(this);
        this.menu = new LeperMenu(this);
        PluginCommand cmd = this.getCommand("leper");
        if (cmd != null) {
            cmd.setExecutor((CommandExecutor)new LeperCommand(this));
            cmd.setTabCompleter((TabCompleter)new LeperTabCompleter());
        }
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents((Listener)new CombatListener(this), (Plugin)this);
        pm.registerEvents((Listener)new ConsumeListener(this), (Plugin)this);
        pm.registerEvents((Listener)new HungerListener(this), (Plugin)this);
        pm.registerEvents((Listener)new VaccineListener(this), (Plugin)this);
        pm.registerEvents((Listener)new EffectBlockListener(this), (Plugin)this);
        pm.registerEvents((Listener)new JoinQuitDeathListener(this), (Plugin)this);
        pm.registerEvents((Listener)new MobIgnoreListener(this), (Plugin)this);
        pm.registerEvents((Listener)new PlagueStickListener(this), (Plugin)this);
        pm.registerEvents((Listener)new PlagueBombListener(this), (Plugin)this);
        pm.registerEvents((Listener)new LeperMenuListener(this), (Plugin)this);
        pm.registerEvents((Listener)new UmbrellaSyncListener(this), (Plugin)this);
        pm.registerEvents((Listener)new InstantBrewingListener(this), (Plugin)this);
        pm.registerEvents((Listener)new LeperBloodListener(this), (Plugin)this);
        pm.registerEvents((Listener)new ContactInfectionListener(this), (Plugin)this);
        pm.registerEvents((Listener)new SneezeListener(this), (Plugin)this);
        pm.registerEvents((Listener)new UndeadPotionInversionListener(this), (Plugin)this);
        this.sunTask = new SunAndInfectionTask(this);
        this.sunTask.start();
    }

    public void onDisable() {
        if (this.sunTask != null) {
            this.sunTask.stop();
        }
        if (this.umbrella != null) {
            this.umbrella.flushAllOnline();
        }
        Bukkit.getOnlinePlayers().forEach(p -> this.movementLock.release((Player)p));
    }
}

