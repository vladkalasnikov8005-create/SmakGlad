package org.examplee.vampirest;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Cat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AbilityManager {

    private static final Set<String> KNOWN_ABILITIES = Set.of(
            "dash", "bat", "wolf", "vision", "cutter", "blades", "mist", "shield", "mine", "heal", "veil"
    );

    private final VampireRacePlugin plugin;
    private final Map<UUID, List<String>> queuedAbilities = new HashMap<>();
    private final Set<UUID> comboModeEnabled = new HashSet<>();
    private final Set<UUID> comboCastingBypass = new HashSet<>();
    private final Map<UUID, Double> bloodShield = new HashMap<>();
    private final Map<UUID, Bat> batVisualMap = new HashMap<>();
    private final Map<UUID, Integer> batFollowTasks = new HashMap<>();
    private final List<BloodMine> mines = new ArrayList<>();

    public AbilityManager(VampireRacePlugin plugin) {
        this.plugin = plugin;
        startMineLoop();
    }

    public void dash(Player player) {
        if (!checkVampire(player) || !checkCooldown(player, "dash")) {
            return;
        }
        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(1.4).setY(Math.max(0.12, direction.getY() * 0.45)));
        player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation(), 1, 0, 0, 0, 0);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 24, 0.6, 0.6, 0.6, 0.02);

        // Track victims for a short window while dashing so pass-through hits are consistent.
        Set<UUID> hitVictims = new HashSet<>();
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 8) {
                    cancel();
                    return;
                }
                Vector look = player.getLocation().getDirection().normalize();
                for (LivingEntity victim : player.getWorld().getLivingEntities()) {
                    if (victim == player) {
                        continue;
                    }
                    if (victim instanceof Cat || victim instanceof Wolf) {
                        continue;
                    }
                    if (hitVictims.contains(victim.getUniqueId())) {
                        continue;
                    }
                    if (!isInBeam(player, victim, look, 2.2, 1.35)) {
                        continue;
                    }
                    victim.damage(4.0, player);
                    hitVictims.add(victim.getUniqueId());
                    player.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 26, 0.25, 0.4, 0.25, 0,
                            new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.2f));
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            var trace = player.getWorld().rayTraceBlocks(player.getLocation(), direction,
                    plugin.getConfig().getDouble("abilities.dash.distance", 10.0),
                    FluidCollisionMode.NEVER, true);
            if (trace == null) {
                player.setVelocity(player.getVelocity().add(direction.clone().multiply(0.35)));
            }
        }, 2L);
        setCooldown(player, "dash", plugin.getConfig().getLong("abilities.dash.cooldown-seconds", 5L) * 1000L);
    }

    private void enhancedDash(Player player) {
        if (!checkVampire(player) || !checkCooldown(player, "combo_dash_dash") || !consumeBlood(player, 22)) {
            return;
        }
        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(2.2).setY(0.35));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 4, 2, true, false, false));
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 1, 0.1, 0.1, 0.1, 0);
        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity == player) {
                continue;
            }
            if (entity.getLocation().distanceSquared(player.getLocation()) <= 12.25) {
                entity.damage(6.0, player);
            }
        }
        setCooldown(player, "combo_dash_dash", 30_000L);
        player.sendMessage(Texts.prefixed("&4Комбо: Усиленный рывок"));
    }

    public void batForm(Player player) {
        if (!checkVampire(player) || !checkCooldown(player, "bat")) {
            return;
        }
        int duration = plugin.getConfig().getInt("abilities.bat.duration-seconds", 30);
        UUID id = player.getUniqueId();
        Integer oldTask = batFollowTasks.remove(id);
        if (oldTask != null) {
            plugin.getServer().getScheduler().cancelTask(oldTask);
        }
        Bat oldBat = batVisualMap.remove(id);
        if (oldBat != null && !oldBat.isDead()) {
            oldBat.remove();
        }

        Bat visualBat = player.getWorld().spawn(player.getLocation(), Bat.class);
        visualBat.setInvulnerable(true);
        visualBat.setSilent(true);
        visualBat.setPersistent(false);
        visualBat.setAware(true);
        batVisualMap.put(id, visualBat);

        int followTaskId = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            Bat bat = batVisualMap.get(player.getUniqueId());
            if (bat == null || bat.isDead()) {
                return;
            }
            bat.teleport(player.getLocation());
        }, 0L, 1L).getTaskId();
        batFollowTasks.put(id, followTaskId);

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvisible(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, 1, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration * 20, 0, true, false, false));
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 50, 0.8, 0.5, 0.8, 0.03);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 45, 0.7, 0.5, 0.7, 0,
                new Particle.DustOptions(Color.fromRGB(90, 0, 0), 1.2f));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            Integer followTask = batFollowTasks.remove(player.getUniqueId());
            if (followTask != null) {
                plugin.getServer().getScheduler().cancelTask(followTask);
            }
            Bat bat = batVisualMap.remove(player.getUniqueId());
            if (bat != null && !bat.isDead()) {
                bat.remove();
            }
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setInvisible(false);
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        }, duration * 20L);
        setCooldown(player, "bat", plugin.getConfig().getLong("abilities.bat.cooldown-seconds", 680L) * 1000L);
    }

    public void summonWolves(Player player) {
        if (!checkVampire(player) || !checkCooldown(player, "wolf")) {
            return;
        }
        int count = plugin.getConfig().getInt("abilities.wolf.count", 3);
        int duration = plugin.getConfig().getInt("abilities.wolf.duration-seconds", 20);
        List<Wolf> wolves = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Wolf wolf = player.getWorld().spawn(player.getLocation().add((i - 1) * 1.5, 0, 1.5), Wolf.class);
            wolf.setOwner(player);
            wolf.setTamed(true);
            var wolfHealth = wolf.getAttribute(Attribute.MAX_HEALTH);
            if (wolfHealth != null) {
                wolfHealth.setBaseValue(15.0);
            }
            wolf.setHealth(15.0);
            wolf.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration * 20, 0, true, false, false));
            wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, 1, true, false, false));
            wolves.add(wolf);
        }
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.8, 0), 55, 1.0, 0.6, 1.0, 0,
                new Particle.DustOptions(Color.fromRGB(130, 0, 0), 1.2f));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Wolf wolf : wolves) {
                if (!wolf.isDead()) {
                    wolf.remove();
                }
            }
        }, duration * 20L);
        setCooldown(player, "wolf", plugin.getConfig().getLong("abilities.wolf.cooldown-seconds", 120L) * 1000L);
    }

    public void vampireVision(Player player) {
        if (!checkVampire(player) || !checkCooldown(player, "vision")) {
            return;
        }
        int duration = plugin.getConfig().getInt("abilities.vision.duration-seconds", 10);
        double radius = plugin.getConfig().getDouble("abilities.vision.radius", 20.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration * 20, 0, true, false, false));
        player.getWorld().spawnParticle(Particle.DUST, player.getEyeLocation(), 30, 0.5, 0.3, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(175, 0, 40), 1.0f));
        for (Player target : player.getWorld().getPlayers()) {
            if (!target.getUniqueId().equals(player.getUniqueId())
                    && target.getLocation().distanceSquared(player.getLocation()) <= radius * radius) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration * 20, 0, true, false, false));
            }
        }
        setCooldown(player, "vision", plugin.getConfig().getLong("abilities.vision.cooldown-seconds", 30L) * 1000L);
    }

    public void bloodCutter(Player player) {
        if (!checkVampire(player) || !checkCooldown(player, "bloodcutter") || !consumeBlood(player, 15)) {
            return;
        }
        World world = player.getWorld();
        long durationTicks = 20L * 5;
        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || elapsed >= durationTicks) {
                    cancel();
                    return;
                }

                Vector direction = player.getLocation().getDirection().normalize();

                for (double d = 0.5; d <= 5.0; d += 0.3) {
                    var point = player.getEyeLocation().clone().add(direction.clone().multiply(d));
                    world.spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(Color.fromRGB(194, 0, 0), 1.1f));
                }

                if (elapsed % 20 == 0) {
                    for (LivingEntity target : world.getLivingEntities()) {
                        if (target == player) {
                            continue;
                        }
                        if (target.getLocation().distanceSquared(player.getLocation()) > 25) {
                            continue;
                        }
                        if (!isInBeam(player, target, direction, 5.0, 1.1)) {
                            continue;
                        }
                        target.damage(2.0, player);
                    }
                }
                elapsed += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        setCooldown(player, "bloodcutter", plugin.getConfig().getLong("abilities.bloodcutter.cooldown-seconds", 35L) * 1000L);
    }

    public void bloodHeal(Player player) {
        double bloodCost = plugin.getConfig().getDouble("abilities.bloodheal.blood-cost", 50.0);
        if (!checkVampire(player) || !checkCooldown(player, "bloodheal") || !consumeBlood(player, bloodCost)) {
            return;
        }
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 5.0));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.0, 0), 8, 0.3, 0.3, 0.3, 0.0);
        setCooldown(player, "bloodheal", plugin.getConfig().getLong("abilities.bloodheal.cooldown-seconds", 30L) * 1000L);
    }

    public void shadowVeil(Player player) {
        if (!checkVampire(player)) {
            return;
        }
        if (!plugin.getVampireManager().isOverlord(player)) {
            player.sendMessage(Texts.prefixed("&cПелена Сумрака доступна только Владыке."));
            return;
        }
        double bloodCost = plugin.getConfig().getDouble("abilities.shadowveil.blood-cost", 45.0);
        if (!checkCooldown(player, "shadowveil") || !consumeBlood(player, bloodCost)) {
            return;
        }

        int durationSeconds = plugin.getConfig().getInt("abilities.shadowveil.duration-seconds", 12);
        double radius = plugin.getConfig().getDouble("abilities.shadowveil.radius", 10.0);

        BukkitRunnable veilTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                Location center = player.getLocation().clone().add(0, 1.0, 0);
                player.getWorld().spawnParticle(Particle.SMOKE, center, 50, radius * 0.25, 1.0, radius * 0.25, 0.02);
                player.getWorld().spawnParticle(Particle.DUST, center, 24, radius * 0.2, 0.8, radius * 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(80, 0, 80), 1.2f));

                for (Player near : player.getWorld().getPlayers()) {
                    if (near.getLocation().distanceSquared(player.getLocation()) > radius * radius) {
                        continue;
                    }
                    if (plugin.getVampireManager().isVampire(near)) {
                        continue;
                    }
                    near.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, true, true, true));
                }

                for (LivingEntity entity : player.getWorld().getLivingEntities()) {
                    if (entity == player) {
                        continue;
                    }
                    if (entity.getLocation().distanceSquared(player.getLocation()) > radius * radius) {
                        continue;
                    }
                    if (entity instanceof Player nearPlayer && plugin.getVampireManager().isVampire(nearPlayer)) {
                        continue;
                    }
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, true, false, false));
                }
            }
        };

        veilTask.runTaskTimer(plugin, 0L, 10L);
        plugin.getServer().getScheduler().runTaskLater(plugin, veilTask::cancel, durationSeconds * 20L);
        setCooldown(player, "shadowveil", plugin.getConfig().getLong("abilities.shadowveil.cooldown-seconds", 90L) * 1000L);
        player.sendMessage(Texts.prefixed("&5Пелена Сумрака окутала местность."));
    }

    public void bloodBlades(Player player) {
        if (!checkVampire(player) || !checkCooldown(player, "bloodblades") || !consumeBlood(player, 10)) {
            return;
        }
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        launchBloodBlade(player, right.clone().multiply(0.25));
        launchBloodBlade(player, right.clone().multiply(-0.25));
        setCooldown(player, "bloodblades", plugin.getConfig().getLong("abilities.bloodblades.cooldown-seconds", 12L) * 1000L);
    }

    public void bloodMist(Player player) {
        if (!checkVampire(player) || !checkCooldown(player, "bloodmist") || !consumeBlood(player, 50)) {
            return;
        }
        int radius = 10;
        long duration = 20L * 20;
        Set<UUID> damaged = new HashSet<>();

        BukkitRunnable mistTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 80; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * radius;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    player.getWorld().spawnParticle(Particle.DUST,
                            player.getLocation().clone().add(x, 0.3 + Math.random() * 2.0, z),
                            1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(120, 0, 0), 1.2f));
                }

                for (LivingEntity target : player.getWorld().getLivingEntities()) {
                    if (target == player) {
                        continue;
                    }
                    if (target.getLocation().distanceSquared(player.getLocation()) > radius * radius) {
                        continue;
                    }
                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 3, 0, true, false, false));
                    if (damaged.add(target.getUniqueId())) {
                        target.damage(4.0, player);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 10, 1, true, true, true));
                    }
                }
            }
        };
        mistTask.runTaskTimer(plugin, 0L, 10L);
        plugin.getServer().getScheduler().runTaskLater(plugin, mistTask::cancel, duration);
        setCooldown(player, "bloodmist", plugin.getConfig().getLong("abilities.bloodmist.cooldown-seconds", 600L) * 1000L);
    }

    public boolean consumeAndCooldownShield(Player player) {
        if (!checkVampire(player) || !checkCooldown(player, "bloodshield") || !consumeBlood(player, 20)) {
            return false;
        }
        activateBloodShield(player,
                plugin.getConfig().getDouble("abilities.bloodshield.shield-hp", 12.0),
                plugin.getConfig().getInt("abilities.bloodshield.duration-seconds", 12));
        setCooldown(player, "bloodshield", plugin.getConfig().getLong("abilities.bloodshield.cooldown-seconds", 50L) * 1000L);
        return true;
    }

    public void bloodMine(Player player) {
        if (!checkVampire(player) || !checkCooldown(player, "bloodmine") || !consumeBlood(player, 20)) {
            return;
        }
        mines.add(new BloodMine(player.getUniqueId(), player.getLocation().clone(), System.currentTimeMillis() + 60_000L));
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.2, 0), 24, 0.4, 0.2, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(130, 0, 0), 1.1f));
        player.sendMessage(Texts.prefixed("&aКровавая мина установлена."));
        setCooldown(player, "bloodmine", plugin.getConfig().getLong("abilities.bloodmine.cooldown-seconds", 45L) * 1000L);
    }

    public boolean isComboModeEnabled(Player player) {
        return comboModeEnabled.contains(player.getUniqueId());
    }

    public void setComboMode(Player player, boolean enabled) {
        UUID id = player.getUniqueId();
        if (enabled) {
            comboModeEnabled.add(id);
            queuedAbilities.remove(id);
            player.sendMessage(Texts.prefixed("&aРежим комбо включен. Выберите 1-2 способности."));
        } else {
            comboModeEnabled.remove(id);
            queuedAbilities.remove(id);
            player.sendMessage(Texts.prefixed("&cРежим комбо выключен."));
        }
    }

    public boolean queueAbilityForCombo(Player player, String ability) {
        if (!KNOWN_ABILITIES.contains(ability)) {
            player.sendMessage(Texts.prefixed("&cНеизвестная способность."));
            return false;
        }
        List<String> queue = queuedAbilities.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayList<>());
        if (queue.size() >= 2) {
            player.sendMessage(Texts.prefixed("&eУже выбраны 2 способности."));
            return false;
        }
        queue.add(ability);
        player.sendMessage(Texts.prefixed("&aДобавлено в комбо: &f" + ability + " &7(" + queue.size() + "/2)"));
        return true;
    }

    public boolean castAbilityByName(Player player, String ability) {
        if (!KNOWN_ABILITIES.contains(ability)) {
            return false;
        }
        switch (ability) {
            case "dash" -> dash(player);
            case "bat" -> batForm(player);
            case "wolf" -> summonWolves(player);
            case "vision" -> vampireVision(player);
            case "cutter" -> bloodCutter(player);
            case "blades" -> bloodBlades(player);
            case "mist" -> bloodMist(player);
            case "shield" -> consumeAndCooldownShield(player);
            case "mine" -> bloodMine(player);
            case "heal" -> bloodHeal(player);
            case "veil" -> shadowVeil(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    public void cancelCombo(Player player) {
        queuedAbilities.remove(player.getUniqueId());
        player.sendMessage(Texts.prefixed("&cКомбо отменено."));
    }

    public void showCombo(Player player) {
        List<String> combo = queuedAbilities.get(player.getUniqueId());
        if (combo == null || combo.isEmpty()) {
            player.sendMessage(Texts.prefixed("&eСпособности для комбо не выбраны."));
            return;
        }
        player.sendMessage(Texts.prefixed("&fТекущий выбор: &c" + String.join(" &7+ &c", combo)));
    }

    public void useCombo(Player player) {
        if (!checkVampire(player)) {
            return;
        }
        List<String> combo = queuedAbilities.get(player.getUniqueId());
        if (combo == null || combo.isEmpty()) {
            player.sendMessage(Texts.prefixed("&eСначала выберите 1-2 способности."));
            return;
        }

        if (combo.size() == 1) {
            castAbilityByName(player, combo.get(0));
            queuedAbilities.remove(player.getUniqueId());
            return;
        }

        String first = combo.get(0);
        String second = combo.get(1);
        if (first.equals("dash") && second.equals("dash")) {
            enhancedDash(player);
            queuedAbilities.remove(player.getUniqueId());
            return;
        }

        executeUniqueCombo(player, first, second);
        queuedAbilities.remove(player.getUniqueId());
    }

    private void executeUniqueCombo(Player player, String first, String second) {
        String key = first + "+" + second;
        if (!checkCooldown(player, "combo_" + key) || !consumeBlood(player, 25 + Math.abs(key.hashCode() % 15))) {
            return;
        }

        withComboBypass(player, () -> castAbilityByName(player, first));
        withComboBypass(player, () -> castAbilityByName(player, second));

        int hash = Math.abs(key.hashCode());
        double radius = 3.5 + (hash % 6);
        double damage = 3.0 + (hash % 5);
        int r = 80 + (hash % 170);
        int g = 5 + ((hash / 2) % 120);
        int b = 5 + ((hash / 3) % 120);

        Location center = player.getLocation().clone().add(0, 0.8, 0);
        player.getWorld().spawnParticle(Particle.DUST, center, 160, 1.4, 1.0, 1.4, 0,
                new Particle.DustOptions(Color.fromRGB(r, g, b), 1.3f));

        for (LivingEntity target : player.getWorld().getLivingEntities()) {
            if (target == player) {
                continue;
            }
            if (target.getLocation().distanceSquared(center) <= radius * radius) {
                target.damage(damage, player);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 3, hash % 2, true, true, true));
            }
        }
        setCooldown(player, "combo_" + key, 40_000L + (hash % 50_000L));
        player.sendMessage(Texts.prefixed("&5Комбо: " + first + " + " + second + " активировано"));
    }

    public void activateBloodShield(Player player, double shieldHp, int durationSeconds) {
        bloodShield.put(player.getUniqueId(), shieldHp);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(140, 0, 0), 1.3f));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> bloodShield.remove(player.getUniqueId()), durationSeconds * 20L);
    }

    public double absorbWithBloodShield(Player player, double incomingDamage) {
        Double shield = bloodShield.get(player.getUniqueId());
        if (shield == null || shield <= 0) {
            return incomingDamage;
        }
        double remaining = incomingDamage - shield;
        if (remaining <= 0) {
            bloodShield.put(player.getUniqueId(), shield - incomingDamage);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(210, 30, 30), 1.0f));
            return 0;
        }
        bloodShield.remove(player.getUniqueId());
        player.sendMessage(Texts.prefixed("&cКровавый щит разрушен."));
        return remaining;
    }

    private void launchBloodBlade(Player caster, Vector sideOffset) {
        Vector direction = caster.getEyeLocation().getDirection().normalize();
        BukkitRunnable bladeTask = new BukkitRunnable() {
            double traveled = 0;
            final Location location = caster.getEyeLocation().clone().add(sideOffset);

            @Override
            public void run() {
                if (!caster.isOnline() || traveled >= 20) {
                    cancel();
                    return;
                }
                location.add(direction.clone().multiply(0.7));
                traveled += 0.7;

                caster.getWorld().spawnParticle(Particle.DUST, location, 8, 0.08, 0.08, 0.08, 0,
                        new Particle.DustOptions(Color.fromRGB(220, 30, 30), 1.1f));
                for (LivingEntity target : caster.getWorld().getLivingEntities()) {
                    if (target == caster) {
                        continue;
                    }
                    if (target.getLocation().distanceSquared(location) <= 1.2) {
                        target.damage(4.0, caster);
                        explodeBloodBlades(caster, target.getLocation());
                        cancel();
                        return;
                    }
                }
            }
        };
        bladeTask.runTaskTimer(plugin, 0L, 1L);
        plugin.getServer().getScheduler().runTaskLater(plugin, bladeTask::cancel, 80L);
    }

    private void explodeBloodBlades(Player caster, Location center) {
        caster.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 0.6, 0), 60, 0.4, 0.4, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(255, 55, 55), 1.2f));
        for (LivingEntity nearby : caster.getWorld().getLivingEntities()) {
            if (nearby == caster) {
                continue;
            }
            if (nearby.getLocation().distanceSquared(center) <= 6.25) {
                nearby.damage(2.0, caster);
            }
        }
    }

    private void startMineLoop() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            Iterator<BloodMine> iterator = mines.iterator();
            while (iterator.hasNext()) {
                BloodMine mine = iterator.next();
                if (now > mine.expiresAt || mine.location.getWorld() == null) {
                    iterator.remove();
                    continue;
                }

                mine.location.getWorld().spawnParticle(Particle.DUST, mine.location.clone().add(0, 0.2, 0), 4, 0.12, 0.05, 0.12, 0,
                        new Particle.DustOptions(Color.fromRGB(140, 0, 0), 0.9f));

                for (LivingEntity target : mine.location.getWorld().getLivingEntities()) {
                    if (target.getUniqueId().equals(mine.owner)) {
                        continue;
                    }
                    if (target.getLocation().distanceSquared(mine.location) <= 2.25) {
                        Player owner = plugin.getServer().getPlayer(mine.owner);
                        if (owner != null) {
                            target.damage(6.0, owner);
                        } else {
                            target.damage(6.0);
                        }
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 5, 1, true, true, true));
                        mine.location.getWorld().spawnParticle(Particle.EXPLOSION, mine.location.clone().add(0, 0.3, 0), 1, 0, 0, 0, 0);
                        mine.location.getWorld().spawnParticle(Particle.DUST, mine.location.clone().add(0, 0.3, 0), 80, 0.8, 0.3, 0.8, 0,
                                new Particle.DustOptions(Color.fromRGB(255, 30, 30), 1.2f));
                        iterator.remove();
                        break;
                    }
                }
            }
        }, 10L, 5L);
    }

    private boolean isInBeam(Player source, LivingEntity target, Vector direction, double length, double width) {
        Vector sourceToTarget = target.getEyeLocation().toVector().subtract(source.getEyeLocation().toVector());
        double forward = sourceToTarget.dot(direction);
        if (forward < 0 || forward > length) {
            return false;
        }
        Vector closestPoint = source.getEyeLocation().toVector().add(direction.clone().multiply(forward));
        return closestPoint.distance(target.getEyeLocation().toVector()) <= width;
    }

    private boolean isNearLeader(Player player, int radius) {
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) {
                continue;
            }
            if (!plugin.getVampireManager().isLeader(other)) {
                continue;
            }
            if (other.getLocation().distanceSquared(player.getLocation()) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private static final class BloodMine {
        private final UUID owner;
        private final Location location;
        private final long expiresAt;

        private BloodMine(UUID owner, Location location, long expiresAt) {
            this.owner = owner;
            this.location = location;
            this.expiresAt = expiresAt;
        }
    }

    private void withComboBypass(Player player, Runnable runnable) {
        UUID id = player.getUniqueId();
        comboCastingBypass.add(id);
        try {
            runnable.run();
        } finally {
            comboCastingBypass.remove(id);
        }
    }

    private boolean checkVampire(Player player) {
        if (plugin.getVampireManager().isVampire(player)) {
            return true;
        }
        player.sendMessage(Texts.prefixed("&cВы не вампир."));
        return false;
    }

    private boolean checkCooldown(Player player, String ability) {
        if (plugin.getVampireManager().isAdminMode(player) || comboCastingBypass.contains(player.getUniqueId())) {
            return true;
        }
        VampireData data = plugin.getVampireManager().getVampireData(player);
        if (!data.isOnCooldown(ability)) {
            return true;
        }
        player.sendMessage(Texts.prefixed("&eПерезарядка: " + (data.cooldownLeftMillis(ability) / 1000.0) + " сек."));
        return false;
    }

    private void setCooldown(Player player, String ability, long cooldownMillis) {
        if (plugin.getVampireManager().isAdminMode(player) || comboCastingBypass.contains(player.getUniqueId())) {
            return;
        }
        VampireData data = plugin.getVampireManager().getVampireData(player);
        plugin.getVampireManager().setVampireData(player, data.withCooldown(ability, System.currentTimeMillis() + cooldownMillis));
    }

    private boolean consumeBlood(Player player, double cost) {
        if (plugin.getVampireManager().isAdminMode(player) || comboCastingBypass.contains(player.getUniqueId())) {
            return true;
        }
        VampireData data = plugin.getVampireManager().getVampireData(player);
        if (data.blood() < cost) {
            player.sendMessage(Texts.prefixed("&cНедостаточно крови. Нужно: " + cost));
            return false;
        }
        plugin.getVampireManager().setVampireData(player, data.withBlood(data.blood() - cost,
                plugin.getConfig().getDouble("vampire.max-blood", 100.0)));
        return true;
    }
}