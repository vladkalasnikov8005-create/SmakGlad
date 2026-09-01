package org.examplee.leperClassPlugin.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.persistence.PersistentDataType;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.command.Subcommand;
import org.examplee.leperClassPlugin.util.Compat;

public final class SneezeSubcommand
implements Subcommand {
    private final LeperClassPlugin plugin;
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<UUID, Long>();

    public SneezeSubcommand(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "sneeze";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        long last;
        long now;
        Player source;
        if (!sender.hasPermission("leper.sneeze")) {
            this.plugin.msg.error(sender, "\u041d\u0435\u0442 \u043f\u0440\u0430\u0432: leper.sneeze");
            return true;
        }
        if (args.length >= 2) {
            source = Bukkit.getPlayerExact((String)args[1]);
            if (source == null) {
                this.plugin.msg.error(sender, "\u0418\u0433\u0440\u043e\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + args[1]);
                return true;
            }
        } else if (sender instanceof Player) {
            Player p;
            source = p = (Player)sender;
        } else {
            this.plugin.msg.warn(sender, "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: /leper sneeze <player>");
            return true;
        }
        if ((now = System.currentTimeMillis()) - (last = this.cooldown.getOrDefault(source.getUniqueId(), 0L).longValue()) < this.plugin.settings.sneezeCooldownMs) {
            long sec = (this.plugin.settings.sneezeCooldownMs - (now - last)) / 1000L;
            this.plugin.msg.warn(sender, "\u0427\u0438\u0445 \u043f\u0435\u0440\u0435\u0437\u0430\u0440\u044f\u0436\u0430\u0435\u0442\u0441\u044f: " + sec + " \u0441\u0435\u043a.");
            return true;
        }
        this.cooldown.put(source.getUniqueId(), now);
        Snowball sneeze = (Snowball)source.launchProjectile(Snowball.class);
        sneeze.setVelocity(source.getEyeLocation().getDirection().normalize().multiply(this.plugin.settings.sneezeVelocity));
        sneeze.getPersistentDataContainer().set(this.plugin.keys.sneezeProjectileKey, PersistentDataType.BYTE, (Object)1);
        sneeze.setTicksLived(Math.max(1, 120 - this.plugin.settings.sneezeMaxDistance * 2));
        source.getWorld().playSound(source.getLocation(), Compat.soundFirst("ENTITY_PANDA_SNEEZE", "ENTITY_SLIME_SQUISH"), 1.0f, 1.0f);
        this.plugin.msg.ok(sender, source.getName() + " \u0447\u0438\u0445\u043d\u0443\u043b.");
        this.plugin.log.info(sender.getName() + " triggered sneeze for " + source.getName());
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<String>();
        Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        return out;
    }
}

