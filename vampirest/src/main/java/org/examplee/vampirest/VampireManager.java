package org.examplee.vampirest;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VampireManager {

    private final VampireRacePlugin plugin;
    private final Map<UUID, VampireData> vampireDataMap = new HashMap<>();
    private final Set<UUID> infectionInProgress = ConcurrentHashMap.newKeySet();
    private final Set<UUID> adminMode = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> infectionStage = new ConcurrentHashMap<>();
    private final Map<UUID, Long> infectionStartedAt = new ConcurrentHashMap<>();
    private final File dataFile;

    public VampireManager(VampireRacePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "vampires.yml");
    }

    public void becomeVampire(Player player) {
        double maxBlood = plugin.getConfig().getDouble("vampire.max-blood", 100.0);
        VampireData base = new VampireData(player.getUniqueId())
                .withBlood(maxBlood, maxBlood)
                .withHunger(20)
                .withLevel(3);
        vampireDataMap.put(player.getUniqueId(), base);
        infectionInProgress.remove(player.getUniqueId());
        infectionStage.remove(player.getUniqueId());
        infectionStartedAt.remove(player.getUniqueId());
        player.setFoodLevel(20);
    }

    public boolean isVampire(Player player) {
        return vampireDataMap.containsKey(player.getUniqueId());
    }

    public void removeVampire(Player player) {
        vampireDataMap.remove(player.getUniqueId());
        infectionInProgress.remove(player.getUniqueId());
        infectionStage.remove(player.getUniqueId());
        infectionStartedAt.remove(player.getUniqueId());
        player.setFoodLevel(20);
        player.setSaturation(5f);
    }

    public boolean isAdminMode(Player player) {
        return adminMode.contains(player.getUniqueId());
    }

    public void setAdminMode(Player player, boolean enabled) {
        if (enabled) {
            adminMode.add(player.getUniqueId());
        } else {
            adminMode.remove(player.getUniqueId());
        }
    }

    public VampireData getVampireData(Player player) {
        return vampireDataMap.computeIfAbsent(player.getUniqueId(), VampireData::new);
    }

    public void setVampireData(Player player, VampireData data) {
        vampireDataMap.put(player.getUniqueId(), data);
    }

    public boolean isLeader(Player player) {
        return isVampire(player) && getVampireData(player).leader();
    }

    public boolean isOverlord(Player player) {
        return isVampire(player) && getVampireData(player).overlord();
    }

    public void setLeader(Player player, boolean leader) {
        if (!isVampire(player)) {
            becomeVampire(player);
        }
        VampireData data = getVampireData(player).withLeader(leader);
        if (!leader) {
            data = data.withOverlord(false);
        }
        setVampireData(player, data);
    }

    public void setOverlord(Player player, boolean overlord) {
        if (!isVampire(player)) {
            becomeVampire(player);
        }
        VampireData data = getVampireData(player)
                .withLeader(overlord || getVampireData(player).leader())
                .withOverlord(overlord);
        setVampireData(player, data);
    }

    public boolean startInfection(Player sourceLeader, Player target) {
        if (infectionInProgress.contains(target.getUniqueId())) {
            return false;
        }
        infectionInProgress.add(target.getUniqueId());
        infectionStage.put(target.getUniqueId(), 1);
        infectionStartedAt.put(target.getUniqueId(), System.currentTimeMillis());
        target.sendMessage(Texts.prefixed("&cВас заражают. Начинается фаза 1..."));
        sourceLeader.sendMessage(Texts.prefixed("&7Фаза 1 запущена для &f" + target.getName()));

        new BukkitRunnable() {
            int phase = 1;

            @Override
            public void run() {
                if (!target.isOnline()) {
                    infectionInProgress.remove(target.getUniqueId());
                    cancel();
                    return;
                }

                if (phase == 1) {
                    infectionStage.put(target.getUniqueId(), 1);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 20, 0, true, true, true));
                    target.sendMessage(Texts.prefixed("&7Фаза 1: 20 минут начальной мутации."));
                } else if (phase == 2) {
                    infectionStage.put(target.getUniqueId(), 2);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 20, 1, true, true, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 15, 0, true, true, true));
                    target.sendMessage(Texts.prefixed("&7Фаза 2: 20 минут углубления заражения."));
                } else if (phase == 3) {
                    infectionStage.put(target.getUniqueId(), 3);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 10, 0, true, true, true));
                    target.sendMessage(Texts.prefixed("&4Фаза 3: финальная трансформация 20 минут."));
                } else {
                    becomeVampire(target);
                    sourceLeader.sendMessage(Texts.prefixed("&a" + target.getName() + " стал вампиром."));
                    infectionInProgress.remove(target.getUniqueId());
                    cancel();
                }
                phase++;
            }
        }.runTaskTimer(plugin, 0L, 20L * 60L * 20L);
        return true;
    }

    public boolean setInfectionStage(Player target, int stage) {
        if (stage < 1 || stage > 3) {
            return false;
        }
        if (stage == 3) {
            infectionStage.put(target.getUniqueId(), 3);
            becomeVampire(target);
            target.sendMessage(Texts.prefixed("&4Стадия 3: вы стали вампиром."));
            return true;
        }
        infectionStage.put(target.getUniqueId(), stage);
        infectionStartedAt.putIfAbsent(target.getUniqueId(), System.currentTimeMillis());
        target.sendMessage(Texts.prefixed("&7Стадия " + stage + " применена."));
        return true;
    }

    public void turnToVampireNow(Player target) {
        becomeVampire(target);
        target.sendMessage(Texts.prefixed("&4Вы обращены в вампира без стадий."));
    }

    public boolean startLordBloodInfection(Player target) {
        if (infectionInProgress.contains(target.getUniqueId())) {
            return false;
        }
        infectionInProgress.add(target.getUniqueId());
        infectionStage.put(target.getUniqueId(), 1);
        infectionStartedAt.put(target.getUniqueId(), System.currentTimeMillis());

        new BukkitRunnable() {
            int phase = 1;

            @Override
            public void run() {
                if (!target.isOnline()) {
                    infectionInProgress.remove(target.getUniqueId());
                    cancel();
                    return;
                }
                if (phase <= 3) {
                    infectionStage.put(target.getUniqueId(), phase);
                    target.sendMessage(Texts.prefixed("&7Фаза " + phase + " длится 20 минут."));
                } else {
                    becomeVampire(target);
                    target.sendMessage(Texts.prefixed("&4Фаза 4: вы стали вампиром."));
                    infectionInProgress.remove(target.getUniqueId());
                    cancel();
                }
                phase++;
            }
        }.runTaskTimer(plugin, 0L, 20L * 60L * 20L);
        return true;
    }

    public void drainBloodAll() {
        double maxBlood = plugin.getConfig().getDouble("vampire.max-blood", 100.0);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!isVampire(player)) {
                continue;
            }
            VampireData data = getVampireData(player);
            int newHunger = player.getFoodLevel();

            if (isAdminMode(player)) {
                newHunger = 20;
                player.setFoodLevel(20);
            }

            VampireData updated = data.withBlood(data.blood(), maxBlood).withHunger(newHunger).withLastDrain(System.currentTimeMillis());
            setVampireData(player, updated);
            player.setFoodLevel(updated.hunger());
            player.sendActionBar(Texts.colorize(Texts.BLOOD_WORD + "&8: &f" + String.format("%.1f", updated.blood())
                    + "&8/&f" + String.format("%.0f", maxBlood)
                    + " &7| &6Голод: &f" + updated.hunger() + "&8/&f20"));

        }
    }

    public void applyPassivesAll() {
        int auraRadius = plugin.getConfig().getInt("vampire.leader-aura-radius", 12);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!isVampire(player)) {
                continue;
            }
            VampireData data = getVampireData(player);
            if (data.nightVisionEnabled()) {
                applyPassiveNightVision(player);
            }
            if (isOverlord(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 40, 1, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 40, 0, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 40, 0, true, false, false));
                if (player.getFireTicks() > 0) {
                    player.setFireTicks(0);
                }
                continue;
            }
            if (isLeader(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 40, 0, true, false, false));
                if (player.getFireTicks() > 0) {
                    player.setFireTicks(0);
                }
                continue;
            }
            if (isNearLeader(player, auraRadius)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 40, 0, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 40, 0, true, false, false));
            }

            if (shouldBurnInSun(player)) {
                player.setFireTicks(Math.max(player.getFireTicks(), 60));
            }
        }
    }

    private boolean shouldBurnInSun(Player player) {
        long time = player.getWorld().getTime();
        if (time < 0 || time >= 12300) {
            return false;
        }
        if (player.getLocation().getBlock().getLightFromSky() < 14) {
            return false;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        return !plugin.getCustomItemManager().isTrumeHat(helmet);
    }

    private boolean isNearLeader(Player player, int radius) {
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) {
                continue;
            }
            if (!isLeader(other)) {
                continue;
            }
            if (other.getLocation().distanceSquared(player.getLocation()) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private void applyPassiveNightVision(Player player) {
        long time = player.getWorld().getTime();
        boolean isNight = time >= 13000 && time <= 23000;
        boolean inShade = player.getLocation().getBlock().getLightFromSky() <= 7;
        if (isNight || inShade) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 120, 0, true, false, false));
        }
    }

    public void setNightVisionEnabled(Player player, boolean enabled) {
        VampireData data = getVampireData(player);
        setVampireData(player, data.withNightVision(enabled));
    }

    public int getInfectionStage(Player player) {
        if (isVampire(player)) {
            return 3;
        }
        return infectionStage.getOrDefault(player.getUniqueId(), 0);
    }

    public long getInfectionStartedAt(Player player) {
        return infectionStartedAt.getOrDefault(player.getUniqueId(), 0L);
    }

    public void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (!cfg.isConfigurationSection("players")) {
            return;
        }
        for (String id : cfg.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(id);
                String base = "players." + id;
                Map<String, Long> cooldowns = new HashMap<>();
                if (cfg.isConfigurationSection(base + ".cooldowns")) {
                    for (String key : cfg.getConfigurationSection(base + ".cooldowns").getKeys(false)) {
                        cooldowns.put(key, cfg.getLong(base + ".cooldowns." + key));
                    }
                }
                vampireDataMap.put(uuid, new VampireData(
                        uuid,
                        cfg.getInt(base + ".level", 1),
                        cfg.getDouble(base + ".blood", 100.0),
                        cfg.getInt(base + ".hunger", 20),
                        cfg.getLong(base + ".lastDrain", System.currentTimeMillis()),
                        cooldowns,
                        cfg.getBoolean(base + ".leader", false),
                        cfg.getBoolean(base + ".nightVisionEnabled", true),
                        cfg.getBoolean(base + ".overlord", false)
                ));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveData() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, VampireData> entry : vampireDataMap.entrySet()) {
            String base = "players." + entry.getKey();
            VampireData data = entry.getValue();
            cfg.set(base + ".level", data.level());
            cfg.set(base + ".blood", data.blood());
            cfg.set(base + ".hunger", data.hunger());
            cfg.set(base + ".lastDrain", data.lastBloodDrain());
            cfg.set(base + ".leader", data.leader());
            cfg.set(base + ".nightVisionEnabled", data.nightVisionEnabled());
            cfg.set(base + ".overlord", data.overlord());
            for (Map.Entry<String, Long> cooldown : data.abilityCooldowns().entrySet()) {
                cfg.set(base + ".cooldowns." + cooldown.getKey(), cooldown.getValue());
            }
        }
        try {
            cfg.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save vampires.yml: " + exception.getMessage());
        }
    }
}