package org.examplee.vampirest;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VampireListener implements Listener {

    private static final EnumSet<Material> RAW_MEAT = EnumSet.of(
            Material.BEEF,
            Material.PORKCHOP,
            Material.CHICKEN,
            Material.MUTTON,
            Material.RABBIT
    );
    private static final EnumSet<Material> COOKED_MEAT = EnumSet.of(
            Material.COOKED_BEEF,
            Material.COOKED_PORKCHOP,
            Material.COOKED_CHICKEN,
            Material.COOKED_MUTTON,
            Material.COOKED_RABBIT
    );
    private static final EnumSet<EntityType> UNDEAD_IGNORE = EnumSet.of(
            EntityType.ZOMBIE,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.HUSK,
            EntityType.DROWNED,
            EntityType.SKELETON,
            EntityType.STRAY,
            EntityType.BOGGED,
            EntityType.WITHER_SKELETON,
            EntityType.ZOMBIFIED_PIGLIN,
            EntityType.PHANTOM
    );

    private final VampireRacePlugin plugin;
    private final Map<UUID, Long> trumeHatExposureSeconds = new HashMap<>();
    private final Map<UUID, Long> trumeHatLastTick = new HashMap<>();
    private final Map<UUID, Integer> bleedingTasks = new HashMap<>();

    public VampireListener(VampireRacePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getVampireManager().isVampire(event.getPlayer())) {
            event.getPlayer().sendMessage(Texts.prefixed("&8Ночь приветствует вас."));
            event.getPlayer().setSaturation(0f);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || !plugin.getVampireManager().isVampire(player)) {
            return;
        }
        if (event.getFoodLevel() < 0) {
            return;
        }
        int targetFood = event.getFoodLevel();
        VampireData data = plugin.getVampireManager().getVampireData(player);
        plugin.getVampireManager().setVampireData(player, data.withHunger(targetFood));
    }

    @EventHandler
    public void onConsumeFood(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getVampireManager().isVampire(player)) {
            return;
        }
        if (plugin.getCustomItemManager().isGarlicSerum(event.getItem())
                || plugin.getCustomItemManager().isBloodBottle(event.getItem())
                || event.getItem().getType().name().contains("POTION")) {
            return;
        }

        Material material = event.getItem().getType();
        VampireData data = plugin.getVampireManager().getVampireData(player);
        double maxBlood = plugin.getConfig().getDouble("vampire.max-blood", 100.0);

        if (RAW_MEAT.contains(material)) {
            event.setCancelled(true);
            consumeHandledFood(player, event.getHand(), material);
            plugin.getVampireManager().setVampireData(player, data.withBlood(data.blood() + 7.5, maxBlood));
            keepVampireHunger(player, data.hunger());
            player.sendMessage(Texts.prefixed("&aСырая плоть: &f+7.5 &aкрови."));
            return;
        }

        if (COOKED_MEAT.contains(material)) {
            event.setCancelled(true);
            consumeHandledFood(player, event.getHand(), material);
            plugin.getVampireManager().setVampireData(player, data.withBlood(data.blood() + 5.0, maxBlood));
            keepVampireHunger(player, data.hunger());
            player.sendMessage(Texts.prefixed("&aЖареное мясо: &f+5 &aкрови."));
            return;
        }

        if (material == Material.GOLDEN_CARROT) {
            event.setCancelled(true);
            consumeHandledFood(player, event.getHand(), material);
            int newHunger = Math.min(20, data.hunger() + 1);
            plugin.getVampireManager().setVampireData(player,
                    data.withBlood(data.blood() + 10.0, maxBlood).withHunger(newHunger));
            setHungerNextTick(player, newHunger);
            player.sendMessage(Texts.prefixed("&6Золотая морковь: +10 крови, +1 голода."));
            return;
        }

        if (material == Material.GOLDEN_APPLE) {
            event.setCancelled(true);
            consumeHandledFood(player, event.getHand(), material);
            int newHunger = Math.min(20, data.hunger() + 2);
            plugin.getVampireManager().setVampireData(player,
                    data.withBlood(data.blood() + 10.0, maxBlood).withHunger(newHunger));
            setHungerNextTick(player, newHunger);
            player.sendMessage(Texts.prefixed("&6Золотое яблоко: +10 крови, +2 голода."));
            return;
        }

        if (material == Material.ENCHANTED_GOLDEN_APPLE) {
            event.setCancelled(true);
            consumeHandledFood(player, event.getHand(), material);
            int newHunger = Math.min(20, data.hunger() + 4);
            plugin.getVampireManager().setVampireData(player,
                    data.withBlood(data.blood() + 20.0, maxBlood).withHunger(newHunger));
            setHungerNextTick(player, newHunger);
            player.sendMessage(Texts.prefixed("&6Зачарованное яблоко: +20 крови, +4 голода."));
            return;
        }

        if (material.isEdible()) {
            event.setCancelled(true);
            consumeHandledFood(player, event.getHand(), material);
            double gain = plugin.getConfig().getDouble("vampire.blood-from-food", 3.0);
            plugin.getVampireManager().setVampireData(player, data.withBlood(data.blood() + gain, maxBlood));
            keepVampireHunger(player, data.hunger());
            player.sendMessage(Texts.prefixed("&aПища дает только кровь: &f+" + gain));
            return;
        }

        event.setCancelled(true);
        player.sendMessage(Texts.prefixed("&cЭта еда вам не подходит. Вампир питается только кровью и редкой золотой пищей."));
    }

    private void keepVampireHunger(Player player, int hunger) {
        plugin.getVampireManager().setVampireData(player,
                plugin.getVampireManager().getVampireData(player).withHunger(hunger));
        setHungerNextTick(player, hunger);
    }

    private void consumeHandledFood(Player player, EquipmentSlot hand, Material material) {
        EquipmentSlot actualHand = hand == null ? EquipmentSlot.HAND : hand;
        ItemStack stack = actualHand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (stack.getType() != material) {
            return;
        }
        stack.setAmount(stack.getAmount() - 1);
        if (stack.getAmount() <= 0) {
            if (actualHand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
        }
    }

    private void setHungerNextTick(Player player, int hunger) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.setFoodLevel(Math.max(0, Math.min(20, hunger)));
        });
    }

    @EventHandler
    public void onSunCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Player player) || !plugin.getVampireManager().isVampire(player)) {
            return;
        }
        if (plugin.getVampireManager().isLeader(player)) {
            event.setCancelled(true);
            return;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        if (plugin.getCustomItemManager().isTrumeHat(helmet)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVampireMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().distanceSquared(event.getTo()) < 0.0001) {
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.getVampireManager().isVampire(player) || plugin.getVampireManager().isLeader(player)) {
            trumeHatLastTick.remove(player.getUniqueId());
            return;
        }

        ItemStack helmet = player.getInventory().getHelmet();
        if (!plugin.getCustomItemManager().isTrumeHat(helmet)) {
            trumeHatLastTick.remove(player.getUniqueId());
            return;
        }
        if (!isUnderSun(player)) {
            trumeHatLastTick.remove(player.getUniqueId());
            return;
        }

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long previous = trumeHatLastTick.getOrDefault(id, now);
        trumeHatLastTick.put(id, now);

        long gained = Math.max(0L, (now - previous) / 1000L);
        if (gained <= 0L) {
            return;
        }

        long total = trumeHatExposureSeconds.getOrDefault(id, 0L) + gained;
        trumeHatExposureSeconds.put(id, total);

        long maxSeconds = plugin.getConfig().getLong("items.trume-hat.sun-seconds", 3600L);
        if (total < maxSeconds) {
            return;
        }

        player.getInventory().setHelmet(new ItemStack(Material.AIR));
        trumeHatExposureSeconds.remove(id);
        trumeHatLastTick.remove(id);
        player.sendMessage(Texts.prefixed("&cШляпа из трюма рассыпалась после 1 часа под солнцем."));
    }

    @EventHandler
    public void onVampirePresence(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().distanceSquared(event.getTo()) < 0.0001) {
            return;
        }
        Player vampire = event.getPlayer();
        if (!plugin.getVampireManager().isVampire(vampire)) {
            return;
        }

        int radius = plugin.getConfig().getInt("vampire.villagers-fear-radius", 10);
        double force = plugin.getConfig().getDouble("vampire.villagers-fear-force", 0.35);
        for (Villager villager : vampire.getWorld().getEntitiesByClass(Villager.class)) {
            if (villager.getLocation().distanceSquared(vampire.getLocation()) > radius * radius) {
                continue;
            }
            var away = villager.getLocation().toVector().subtract(vampire.getLocation().toVector());
            if (away.lengthSquared() < 0.001) {
                away = villager.getLocation().getDirection().multiply(-1);
            }
            villager.setVelocity(away.normalize().multiply(force).setY(0.2));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUndeadTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }
        if (!plugin.getVampireManager().isVampire(player)) {
            return;
        }
        if (UNDEAD_IGNORE.contains(event.getEntityType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUndeadDamageVampire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!plugin.getVampireManager().isVampire(victim)) {
            return;
        }

        EntityType damagerType = event.getDamager().getType();
        if (UNDEAD_IGNORE.contains(damagerType)) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getDamager() instanceof Projectile projectile)) {
            return;
        }
        if (!(projectile.getShooter() instanceof LivingEntity shooter)) {
            return;
        }
        if (UNDEAD_IGNORE.contains(shooter.getType())) {
            event.setCancelled(true);
        }
    }

    private boolean isUnderSun(Player player) {
        long time = player.getWorld().getTime();
        boolean isDay = time >= 0 && time < 12300;
        boolean openSky = player.getLocation().getBlock().getLightFromSky() >= 14;
        return isDay && openSky;
    }

    @EventHandler
    public void onVampireDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !plugin.getVampireManager().isVampire(attacker)) {
            return;
        }
        double multiplier = plugin.getConfig().getDouble("vampire.fist-damage-multiplier", 3.0);
        event.setDamage(event.getDamage() * multiplier);

        if (event.getEntity() instanceof LivingEntity victim) {
            applyBleeding(attacker, victim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVampireLifeSteal(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (!plugin.getVampireManager().isVampire(attacker)) {
            return;
        }
        if (event.getFinalDamage() <= 0) {
            return;
        }

        double percent = plugin.getVampireManager().isLeader(attacker) ? 0.25 : 0.10;
        double heal = event.getFinalDamage() * percent;
        attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal));
    }

    private void applyBleeding(Player attacker, LivingEntity victim) {
        if (victim instanceof Player victimPlayer && victimPlayer.equals(attacker)) {
            return;
        }

        // Do not refresh bleeding while it is already active.
        if (bleedingTasks.containsKey(victim.getUniqueId())) {
            return;
        }

        int taskId = new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 3 || victim.isDead() || !victim.isValid()) {
                    bleedingTasks.remove(victim.getUniqueId());
                    cancel();
                    return;
                }
                victim.damage(1.0, attacker);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 0, true, false, false));
                ticks++;
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();

        bleedingTasks.put(victim.getUniqueId(), taskId);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !plugin.getVampireManager().isVampire(player)) {
            return;
        }
        double afterShield = plugin.getAbilityManager().absorbWithBloodShield(player, event.getDamage());
        double divisor = 5.0;
        if (plugin.getVampireManager().isOverlord(player)) {
            divisor = 25.0;
        } else if (plugin.getVampireManager().isLeader(player)) {
            divisor = 10.0;
        }
        event.setDamage(Math.max(0.0, afterShield / divisor));
    }
}