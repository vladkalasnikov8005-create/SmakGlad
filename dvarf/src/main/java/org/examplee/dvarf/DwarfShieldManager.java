package org.examplee.dvarf;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class DwarfShieldManager implements Listener {

    private final DwarvenCorePlugin plugin;
    private final DwarfService dwarfService;

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> projectileBlocks = new HashMap<>();
    private final Map<UUID, Long> vexCritUntil = new HashMap<>();
    private final Map<UUID, Long> coastActiveUntil = new HashMap<>();
    private final Map<UUID, Long> stunnedUntil = new HashMap<>();

    public DwarfShieldManager(DwarvenCorePlugin plugin, DwarfService dwarfService) {
        this.plugin = plugin;
        this.dwarfService = dwarfService;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack base = inv.getItem(0);
        ItemStack addon = inv.getItem(1);
        if (base == null || addon == null) {
            return;
        }
        if (base.getType() != Material.SHIELD) {
            return;
        }

        String shieldId = dwarfService.resolveShieldAbilityFromTemplate(addon.getType());
        if (shieldId == null) {
            return;
        }

        ItemStack result = dwarfService.createTrimShield(shieldId);
        copyShieldState(base, result);
        event.setResult(result);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        // No-op; custom shields are made in anvil pipeline.
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) {
            return;
        }
        if (!event.getAction().isRightClick()) {
            return;
        }

        Player player = event.getPlayer();
        if (!dwarfService.isDwarf(player) || !player.isSneaking()) {
            return;
        }

        ItemStack item = event.getHand() == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        if (!dwarfService.isTrimShield(item)) {
            return;
        }

        String abilityId = dwarfService.getTrimShieldId(item);
        if (abilityId == null) {
            return;
        }

        if (isOnCooldown(player, abilityId)) {
            long leftMs = getCooldownLeftMillis(player, abilityId);
            player.sendMessage(dwarfService.color("&cКулдаун: " + Math.max(1, leftMs / 1000L) + " сек."));
            return;
        }

        event.setCancelled(true);
        if (!damageShield(item, player, event.getHand())) {
            return;
        }

        activateAbility(player, abilityId);
        setCooldown(player, abilityId, getCooldownSeconds(abilityId));
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Long until = stunnedUntil.get(event.getPlayer().getUniqueId());
        if (until == null || System.currentTimeMillis() >= until) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().toVector().distanceSquared(event.getTo().toVector()) > 0.0001D) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Long coastUntil = coastActiveUntil.get(player.getUniqueId());
        if (coastUntil != null && System.currentTimeMillis() <= coastUntil) {
            coastActiveUntil.remove(player.getUniqueId());
            teleportRandomSafe(player, 5);
            player.damage(2.0D);
            player.sendMessage(dwarfService.color("&bЖемчужная защита сработала!"));
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Snowball snowball
            && snowball.getScoreboardTags().contains("dwarf_host_web")
            && event.getEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4, true, false, true));
            target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 35, 0.5D, 0.8D, 0.5D, Material.COBWEB.createBlockData());
        }

        if (event.getEntity() instanceof Player target) {
            if (event.getDamager() instanceof Projectile && projectileBlocks.getOrDefault(target.getUniqueId(), 0) > 0) {
                projectileBlocks.put(target.getUniqueId(), projectileBlocks.get(target.getUniqueId()) - 1);
                event.setCancelled(true);
                target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 20, Material.COBBLESTONE.createBlockData());
                return;
            }
        }

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        Long critUntil = vexCritUntil.get(player.getUniqueId());
        if (critUntil != null && System.currentTimeMillis() <= critUntil) {
            event.setDamage(event.getDamage() + 8.0D);
            vexCritUntil.remove(player.getUniqueId());
        }
    }

    private void activateAbility(Player player, String abilityId) {
        switch (abilityId) {
            case "trim_shield_dune" -> abilityDune(player);
            case "trim_shield_ward" -> abilityWard(player);
            case "trim_shield_tide" -> abilityTide(player);
            case "trim_shield_flame" -> abilityFlame(player);
            case "trim_shield_eye" -> abilityEye(player);
            case "trim_shield_rib" -> abilityRib(player);
            case "trim_shield_snout" -> abilitySnout(player);
            case "trim_shield_shaper" -> abilityShaper(player);
            case "trim_shield_sentry" -> abilitySentry(player);
            case "trim_shield_vex" -> abilityVex(player);
            case "trim_shield_spire" -> abilitySpire(player);
            case "trim_shield_silence" -> abilitySilence(player);
            case "trim_shield_coast" -> abilityCoast(player);
            case "trim_shield_wayfinder" -> abilityWayfinder(player);
            case "trim_shield_raiser" -> abilityRaiser(player);
            case "trim_shield_host" -> abilityHost(player);
            case "trim_shield_skull" -> abilitySkull(player);
            case "trim_shield_flow" -> abilityFlow(player);
            case "trim_shield_bolt" -> abilityBolt(player);
            default -> player.sendMessage(dwarfService.color("&cНеизвестная способность щита."));
        }
    }

    private void abilityDune(Player player) {
        Material under = player.getLocation().subtract(0, 1, 0).getBlock().getType();
        Set<Material> allowed = Set.of(
            Material.SAND, Material.RED_SAND, Material.GRAVEL, Material.DIRT, Material.PODZOL,
            Material.MYCELIUM, Material.CLAY, Material.NETHERRACK, Material.BLACKSTONE, Material.BASALT
        );
        if (!allowed.contains(under)) {
            player.sendMessage(dwarfService.color("&cНужен рыхлый/каменистый блок под ногами."));
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0, true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 2, true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, true, false, true));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, true, false, true)), 80L);
        player.setFoodLevel(Math.max(0, player.getFoodLevel() - 4));
    }

    private void abilityWard(Player player) {
        List<LivingEntity> targets = getTargetsInFront(player, 18, 2.5D);
        for (LivingEntity target : targets) {
            target.damage(16.0D, player);
            Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.2D).setY(0.35D);
            target.setVelocity(push);
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 0, true, false, true));
        player.setVelocity(player.getLocation().getDirection().multiply(-1.1D).setY(0.25D));
    }

    private void abilityTide(Player player) {
        double power = player.isInWater() ? 3.8D : 2.6D;
        player.setVelocity(player.getLocation().getDirection().normalize().multiply(power).setY(0.05D));
        player.getWorld().spawnParticle(Particle.BUBBLE, player.getLocation(), 80, 0.8D, 0.4D, 0.8D, 0.02D);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, true, false, true));
    }

    private void abilityFlame(Player player) {
        for (LivingEntity entity : player.getLocation().getNearbyLivingEntities(5, 5, 5)) {
            if (entity.equals(player)) {
                continue;
            }
            entity.setFireTicks(120);
        }
        player.setFireTicks(60);
        player.damage(2.0D);
    }

    private void abilityEye(Player player) {
        Location start = player.getEyeLocation();
        Location dest = start.clone().add(start.getDirection().normalize().multiply(20));
        var hit = player.getWorld().rayTraceBlocks(start, start.getDirection(), 20);
        if (hit != null && hit.getHitPosition() != null) {
            dest = hit.getHitPosition().toLocation(player.getWorld());
        }
        Location safe = dest.getBlock().getLocation().add(0.5D, 1.0D, 0.5D);
        player.teleport(safe);
        player.damage(4.0D);
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 0, true, false, true));
    }

    private void abilityRib(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 2, true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 1, true, false, true));
    }

    private void abilitySnout(Player player) {
        boolean success = false;
        for (Piglin piglin : player.getLocation().getNearbyLivingEntities(8, 6, 8).stream().filter(Piglin.class::isInstance).map(Piglin.class::cast).toList()) {
            Material[] barter = {Material.ENDER_PEARL, Material.IRON_NUGGET, Material.OBSIDIAN, Material.FIRE_CHARGE};
            ItemStack reward = new ItemStack(barter[ThreadLocalRandom.current().nextInt(barter.length)], 1 + ThreadLocalRandom.current().nextInt(2));
            piglin.getWorld().dropItemNaturally(piglin.getLocation(), reward);
            piglin.setTarget(null);
            piglin.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 600, 0, true, false, true));
            success = true;
        }

        LivingEntity target = getNearestTargetInSight(player, 8);
        if (target != null && target.getEquipment() != null && target.getEquipment().getItemInMainHand().getType() != Material.AIR) {
            if (ThreadLocalRandom.current().nextDouble() <= 0.30D) {
                ItemStack drop = target.getEquipment().getItemInMainHand().clone();
                target.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                target.getWorld().dropItemNaturally(target.getLocation(), drop);
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, true, false, true));
                success = true;
            }
        }

        if (!success) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BAD_OMEN, 1200, 0, true, false, true));
        }
        player.setFoodLevel(Math.max(0, player.getFoodLevel() - 2));
    }

    private void abilityShaper(Player player) {
        projectileBlocks.put(player.getUniqueId(), 3);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0, true, false, true));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> projectileBlocks.remove(player.getUniqueId()), 100L);
    }

    private void abilitySentry(Player player) {
        IronGolem golem = (IronGolem) player.getWorld().spawnEntity(player.getLocation().add(1, 0, 1), EntityType.IRON_GOLEM);
        golem.setPlayerCreated(true);
        golem.setHealth(Math.min(golem.getMaxHealth(), 20.0D));
        LivingEntity target = getNearestTargetInSight(player, 20);
        if (target != null) {
            golem.setTarget(target);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!golem.isDead()) {
                golem.remove();
            }
            PotionEffectType fatigue = resolveEffectType("SLOW_DIGGING", "MINING_FATIGUE");
            if (fatigue != null) {
                player.addPotionEffect(new PotionEffect(fatigue, 100, 1, true, false, true));
            }
        }, 200L);
    }

    private void abilityVex(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0, true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 1, true, false, true));
        vexCritUntil.put(player.getUniqueId(), System.currentTimeMillis() + 8_000L);
        player.setFoodLevel(Math.max(0, player.getFoodLevel() - 3));
    }

    private void abilitySpire(Player player) {
        LivingEntity target = getNearestTargetInSight(player, 3);
        if (target != null) {
            target.damage(20.0D, player);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0, true, false, true));
        }
        player.setVelocity(player.getLocation().getDirection().multiply(-1.3D).setY(0.2D));
    }

    private void abilitySilence(Player player) {
        for (LivingEntity entity : player.getLocation().getNearbyLivingEntities(8, 8, 8)) {
            if (entity.equals(player)) {
                continue;
            }
            entity.damage(10.0D, player);
            entity.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 2, true, false, true));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, true, false, true));
        }

        breakFragileAround(player.getLocation(), 8);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0F, 1.0F);
        stunnedUntil.put(player.getUniqueId(), System.currentTimeMillis() + 3000L);
        player.damage(6.0D);
    }

    private void abilityCoast(Player player) {
        coastActiveUntil.put(player.getUniqueId(), System.currentTimeMillis() + 8000L);
    }

    private void abilityWayfinder(Player player) {
        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(player.getLocation(), 30, 30, 30)) {
            if (entity.equals(player)) {
                continue;
            }
            if (entity.getType().isAlive() && entity.getType() != EntityType.ARMOR_STAND) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, true, false, true));
            }
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0, true, false, true));
    }

    private void abilityRaiser(Player player) {
        player.setVelocity(player.getVelocity().setY(1.4D));
    }

    private void abilityHost(Player player) {
        Snowball web = player.launchProjectile(Snowball.class);
        web.setVelocity(player.getLocation().getDirection().normalize().multiply(2.0D));
        web.addScoreboardTag("dwarf_host_web");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!web.isDead()) {
                web.remove();
            }
        }, 60L);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, false, true));
    }

    private void abilitySkull(Player player) {
        for (LivingEntity entity : player.getLocation().getNearbyLivingEntities(10, 10, 10)) {
            if (entity.equals(player)) {
                continue;
            }
            entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1, true, false, true));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, true, false, true));
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, true, false, true));
    }

    private void abilityFlow(Player player) {
        Vector v = player.getLocation().getDirection().normalize();
        player.setVelocity(v.multiply(1.8D).setY(0.1D));
        player.setCooldown(Material.FIREWORK_ROCKET, 80);
        for (Entity entity : player.getNearbyEntities(5, 3, 5)) {
            if (entity instanceof LivingEntity living) {
                living.setVelocity(v.multiply(1.2D).setY(0.15D));
            }
        }
    }

    private void abilityBolt(Player player) {
        Location start = player.getEyeLocation();
        Location strike = start.clone().add(start.getDirection().normalize().multiply(10));
        var hit = player.getWorld().rayTraceBlocks(start, start.getDirection(), 10);
        if (hit != null && hit.getHitPosition() != null) {
            strike = hit.getHitPosition().toLocation(player.getWorld());
        }
        player.getWorld().strikeLightning(strike);
        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(strike, 2.2D, 2.2D, 2.2D)) {
            if (!entity.equals(player)) {
                entity.damage(10.0D, player);
                entity.setFireTicks(80);
            }
        }
        if (player.getLocation().distanceSquared(strike) < 16) {
            player.damage(4.0D);
        }
    }

    private int getCooldownSeconds(String abilityId) {
        return switch (abilityId) {
            case "trim_shield_dune" -> 50;
            case "trim_shield_ward" -> 35;
            case "trim_shield_tide" -> 18;
            case "trim_shield_flame" -> 25;
            case "trim_shield_eye" -> 20;
            case "trim_shield_rib" -> 40;
            case "trim_shield_snout" -> 25;
            case "trim_shield_shaper" -> 30;
            case "trim_shield_sentry" -> 60;
            case "trim_shield_vex" -> 25;
            case "trim_shield_spire" -> 45;
            case "trim_shield_silence" -> 90;
            case "trim_shield_coast" -> 30;
            case "trim_shield_wayfinder" -> 40;
            case "trim_shield_raiser" -> 20;
            case "trim_shield_host" -> 15;
            case "trim_shield_skull" -> 25;
            case "trim_shield_flow" -> 15;
            case "trim_shield_bolt" -> 40;
            default -> 25;
        };
    }

    private boolean damageShield(ItemStack shield, Player player, EquipmentSlot hand) {
        if (shield == null || shield.getType() != Material.SHIELD || !shield.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = shield.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return false;
        }
        int next = damageable.getDamage() + 5;
        if (next >= shield.getType().getMaxDurability()) {
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } else {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
            return false;
        }
        damageable.setDamage(next);
        shield.setItemMeta((ItemMeta) damageable);
        return true;
    }

    private void copyShieldState(ItemStack source, ItemStack target) {
        target.addUnsafeEnchantments(source.getEnchantments());

        if (source.hasItemMeta() && target.hasItemMeta()) {
            ItemMeta sourceMeta = source.getItemMeta();
            ItemMeta targetMeta = target.getItemMeta();
            if (sourceMeta instanceof Damageable srcDmg && targetMeta instanceof Damageable dstDmg) {
                dstDmg.setDamage(srcDmg.getDamage());
                target.setItemMeta((ItemMeta) dstDmg);
            }
        }
    }

    private boolean isOnCooldown(Player player, String abilityId) {
        return getCooldownLeftMillis(player, abilityId) > 0;
    }

    private long getCooldownLeftMillis(Player player, String abilityId) {
        Long until = cooldowns.getOrDefault(player.getUniqueId(), Map.of()).get(abilityId);
        if (until == null) {
            return 0L;
        }
        return Math.max(0L, until - System.currentTimeMillis());
    }

    private void setCooldown(Player player, String abilityId, int seconds) {
        cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
            .put(abilityId, System.currentTimeMillis() + (seconds * 1000L));
    }

    private List<LivingEntity> getTargetsInFront(Player player, double range, double width) {
        List<LivingEntity> targets = new ArrayList<>();
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Location start = player.getEyeLocation();

        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(player.getLocation(), range, range, range)) {
            if (entity.equals(player)) {
                continue;
            }
            Vector to = entity.getEyeLocation().toVector().subtract(start.toVector());
            double forward = to.dot(dir);
            if (forward < 0 || forward > range) {
                continue;
            }
            double side = to.clone().subtract(dir.clone().multiply(forward)).length();
            if (side <= width) {
                targets.add(entity);
            }
        }
        return targets;
    }

    private LivingEntity getNearestTargetInSight(Player player, double maxDistance) {
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(player.getLocation(), maxDistance, maxDistance, maxDistance)) {
            if (entity.equals(player)) {
                continue;
            }
            double dist = entity.getLocation().distanceSquared(player.getLocation());
            if (dist < best) {
                best = dist;
                nearest = entity;
            }
        }
        return nearest;
    }

    private void teleportRandomSafe(Player player, int radius) {
        Location base = player.getLocation();
        for (int i = 0; i < 20; i++) {
            int dx = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int dz = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            Location test = base.clone().add(dx, 0, dz);
            test.setY(test.getWorld().getHighestBlockYAt(test) + 1);
            if (test.getBlock().getType().isAir() && test.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                player.teleport(test);
                return;
            }
        }
    }

    private void breakFragileAround(Location center, int radius) {
        Set<Material> fragile = new HashSet<>(Set.of(
            Material.GLASS, Material.WHITE_STAINED_GLASS, Material.ORANGE_STAINED_GLASS, Material.MAGENTA_STAINED_GLASS,
            Material.LIGHT_BLUE_STAINED_GLASS, Material.YELLOW_STAINED_GLASS, Material.LIME_STAINED_GLASS, Material.PINK_STAINED_GLASS,
            Material.GRAY_STAINED_GLASS, Material.LIGHT_GRAY_STAINED_GLASS, Material.CYAN_STAINED_GLASS, Material.PURPLE_STAINED_GLASS,
            Material.BLUE_STAINED_GLASS, Material.BROWN_STAINED_GLASS, Material.GREEN_STAINED_GLASS, Material.RED_STAINED_GLASS,
            Material.BLACK_STAINED_GLASS, Material.GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE, Material.PINK_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
            Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE, Material.BROWN_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE
        ));

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radius * radius) {
                        continue;
                    }
                    var block = center.clone().add(x, y, z).getBlock();
                    if (fragile.contains(block.getType()) || block.getType().name().contains("LEAVES") || block.getType().name().contains("SIGN")) {
                        block.breakNaturally();
                    }
                }
            }
        }
    }

    private PotionEffectType resolveEffectType(String... names) {
        for (String name : names) {
            PotionEffectType type = PotionEffectType.getByName(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }
}