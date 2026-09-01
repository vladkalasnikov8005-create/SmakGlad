package org.examplee.guardianClassPlugin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.examplee.guardianClassPlugin.command.GuardianCommand;
import org.examplee.guardianClassPlugin.core.GuardianKeys;
import org.examplee.guardianClassPlugin.data.GuardianData;
import org.examplee.guardianClassPlugin.items.GuardianItems;
import org.examplee.guardianClassPlugin.listeners.GuardianListener;
import org.examplee.guardianClassPlugin.listeners.TreeChopListener;
import org.examplee.guardianClassPlugin.persist.GuardianBlockStorage;
import org.examplee.guardianClassPlugin.store.GuardianBlockStore;
import org.examplee.guardianClassPlugin.tasks.GuardianTask;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;

public final class GuardianClassPlugin extends JavaPlugin {

    public GuardianKeys keys;
    public GuardianData data;
    public GuardianItems items;

    public GuardianBlockStore blocks;
    public GuardianBlockStorage blocksStorage;

    private GuardianTask task;

    @Override
    public void onEnable() {
        keys = new GuardianKeys(this);
        data = new GuardianData(this);
        items = new GuardianItems(this);

        blocks = new GuardianBlockStore();
        blocksStorage = new GuardianBlockStorage(this);
        blocksStorage.load(blocks);

        GuardianCommand gc = new GuardianCommand(this);
        registerGuardianCommand(gc);

        Bukkit.getPluginManager().registerEvents(new GuardianListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TreeChopListener(this), this);

        task = new GuardianTask(this);
        task.start();

        getLogger().info("GuardianClass включен.");
    }

    @Override
    public void onDisable() {
        if (task != null) task.stop();
        try {
            blocksStorage.save(blocks);
        } catch (Throwable ignored) {
        }
        getLogger().info("GuardianClass выключен.");
    }

    private void registerGuardianCommand(GuardianCommand gc) {
        try {
            PluginCommand cmd = getCommand("guardian");
            if (cmd != null) {
                cmd.setExecutor(gc);
                cmd.setTabCompleter(gc);
                return;
            }
        } catch (UnsupportedOperationException ignored) {
            // Paper-plugin path: register through BasicCommand API below.
        }

        if (tryRegisterPaperCommand(gc)) return;
        getLogger().warning("Не удалось зарегистрировать команду /guardian.");
    }

    private boolean tryRegisterPaperCommand(GuardianCommand gc) {
        try {
            Class<?> basicCommandClass = Class.forName("io.papermc.paper.command.brigadier.BasicCommand");
            Class<?> sourceStackClass = Class.forName("io.papermc.paper.command.brigadier.CommandSourceStack");

            Object basicCommand = Proxy.newProxyInstance(
                    basicCommandClass.getClassLoader(),
                    new Class<?>[]{basicCommandClass},
                    (proxy, method, args) -> {
                        String name = method.getName();

                        if (name.equals("execute") && args != null && args.length == 2) {
                            Object source = args[0];
                            String[] commandArgs = (String[]) args[1];
                            Method getSender = sourceStackClass.getMethod("getSender");
                            CommandSender sender = (CommandSender) getSender.invoke(source);
                            gc.executeGuardian(sender, commandArgs);
                            return null;
                        }

                        if (name.equals("suggest") && args != null && args.length == 2) {
                            String[] commandArgs = (String[]) args[1];
                            return gc.tabCompleteGuardian(commandArgs);
                        }

                        if (name.equals("canUse")) return true;
                        if (name.equals("permission")) return null;
                        if (name.equals("toString")) return "GuardianBasicCommandProxy";
                        return null;
                    }
            );

            Method register;
            try {
                register = JavaPlugin.class.getMethod("registerCommand", String.class, String.class, Collection.class, basicCommandClass);
                register.invoke(this, "guardian", "Команда класса Хранителя", List.of(), basicCommand);
            } catch (NoSuchMethodException ex) {
                register = JavaPlugin.class.getMethod("registerCommand", String.class, basicCommandClass);
                register.invoke(this, "guardian", basicCommand);
            }

            getLogger().info("Команда /guardian зарегистрирована через Paper Command API.");
            return true;
        } catch (Throwable t) {
            getLogger().warning("Ошибка регистрации Paper-команды /guardian: " + t.getMessage());
            return false;
        }
    }
}
