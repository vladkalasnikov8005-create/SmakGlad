package org.examplee.guardianClassPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.examplee.guardianClassPlugin.GuardianClassPlugin;
import org.examplee.guardianClassPlugin.util.GuardianUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class GuardianListener implements Listener {

    private final GuardianClassPlugin plugin;
    private static final double FIRE_MULT = 2.0;

    private static final Map<UUID, Long> staffRightCd = new HashMap<>();
    private static final Map<UUID, Long> staffLeftCd = new HashMap<>();
    private static final long STAFF_LEFT_CD_MS = 10_000;
    private static final long STAFF_RIGHT_CD_MS = 250;

    private static final Map<UUID, Long> shieldCd = new HashMap<>();
    private static final Map<UUID, Long> shieldActiveUntil = new HashMap<>();
    private static final long SHIELD_DURATION_MS = 5000;
    private static final long SHIELD_CD_MS = 5L * 60L * 1000L;

    private static final TreeType[] SEED_TREES = new TreeType[] {
            TreeType.TREE, TreeType.BIG_TREE, TreeType.BIRCH, TreeType.TALL_BIRCH,
            TreeType.REDWOOD, TreeType.TALL_REDWOOD, TreeType.JUNGLE, TreeType.SMALL_JUNGLE,
            TreeType.COCOA_TREE, TreeType.ACACIA, TreeType.DARK_OAK, TreeType.MANGROVE,
            TreeType.CHERRY, TreeType.AZALEA
    };

    public GuardianListener(GuardianClassPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isGuardian(Player p) {
        return plugin.data.isGuardian(p);
    }

    private void actionbar(Player p, String msg) {
        try {
            p.sendActionBar(msg);
        } catch (Throwable ignored) {
        }
    }

    private double cdLeftSec(long now, long last, long cdMs) {
        long left = cdMs - (now - last);
        if (left <= 0) return 0.0;
        return left / 1000.0;
    }

    private boolean isShieldActive(Player p) {
        long now = System.currentTimeMillis();
        long until = shieldActiveUntil.getOrDefault(p.getUniqueId(), 0L);
        return now < until;
    }

    private boolean isAllowedOmenEffect(PotionEffectType type) {
        if (type == null) return false;
        return type.getKey().getKey().toLowerCase(Locale.ROOT).contains("omen");
    }

    private boolean isAllowedGuardianBuff(PotionEffectType type) {
        if (type == null) return false;

        return type.equals(PotionEffectType.SLOW_FALLING)
                || type.equals(PotionEffectType.WATER_BREATHING)
                || type.equals(PotionEffectType.DOLPHINS_GRACE)
                || type.equals(PotionEffectType.RESISTANCE)
                || type.equals(PotionEffectType.STRENGTH)
                || type.equals(PotionEffectType.SPEED)
                || type.equals(PotionEffectType.WEAKNESS)
                || type.equals(PotionEffectType.REGENERATION)
                || type.equals(PotionEffectType.JUMP_BOOST);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        ItemStack it = e.getItemInHand();
        if (it == null) return;

        boolean lifeItem = plugin.items.isLifeStoneItem(it);
        boolean flowerItem = plugin.items.isGuardianFlowerItem(it);
        if (!(lifeItem || flowerItem)) return;

        if (!isGuardian(p)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "Ты не Хранитель и не можешь ставить этот блок.");
            return;
        }

        Block b = e.getBlockPlaced();
        World w = b.getWorld();

        if (lifeItem && b.getType() == Material.EMERALD_BLOCK) {
            plugin.blocks.addLifeStone(w, b.getX(), b.getY(), b.getZ());
            w.spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 1.2, 0.5), 20, 0.45, 0.6, 0.45, 0.01);
            w.playSound(b.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.6f);
        }

        if (flowerItem && b.getType() == Material.SPORE_BLOSSOM) {
            plugin.blocks.addFlower(w, b.getX(), b.getY(), b.getZ());
            w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, b.getLocation().add(0.5, 1.0, 0.5), 35, 0.6, 0.6, 0.6, 0.01);
            w.playSound(b.getLocation(), Sound.BLOCK_AZALEA_LEAVES_HIT, 0.8f, 1.3f);
        }

        plugin.blocksStorage.save(plugin.blocks);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        World w = b.getWorld();

        boolean isLife = plugin.blocks.isLifeStone(w, b.getX(), b.getY(), b.getZ());
        boolean isFlower = plugin.blocks.isFlower(w, b.getX(), b.getY(), b.getZ());
        if (!isLife && !isFlower) return;

        e.setDropItems(false);

        if (isLife) {
            plugin.blocks.removeLifeStone(w, b.getX(), b.getY(), b.getZ());
            w.dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), plugin.items.lifeStoneBlock(1));
            w.playSound(b.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.2f);
        }

        if (isFlower) {
            plugin.blocks.removeFlower(w, b.getX(), b.getY(), b.getZ());
            w.dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), plugin.items.guardianFlowerBlock(1));
            w.playSound(b.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.7f, 1.2f);
        }

        plugin.blocksStorage.save(plugin.blocks);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!isGuardian(p)) return;

        if (e.getCause() == EntityDamageEvent.DamageCause.FALL || e.getCause() == EntityDamageEvent.DamageCause.SONIC_BOOM) {
            e.setCancelled(true);
            return;
        }

        switch (e.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> e.setDamage(e.getDamage() * FIRE_MULT);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!isGuardian(p)) return;

        if (isShieldActive(p) && (e.getDamager() instanceof Projectile)) {
            e.setDamage(e.getDamage() * 0.5);
            p.setFireTicks(0);
            p.getWorld().spawnParticle(Particle.BUBBLE_POP, p.getLocation().add(0, 1.0, 0), 10, 0.6, 0.8, 0.6, 0.01);
        }

        EntityType type = e.getDamager().getType();
        if (type == EntityType.SLIME || type == EntityType.MAGMA_CUBE || type == EntityType.CREEPER || type == EntityType.WARDEN) {
            e.setCancelled(true);
            return;
        }
        if ("CREAKING".equalsIgnoreCase(type.name())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player p)) return;
        if (!isGuardian(p)) return;

        EntityType type = e.getEntityType();
        if (type == EntityType.CREEPER || type == EntityType.SLIME || type == EntityType.MAGMA_CUBE || type == EntityType.WARDEN) {
            e.setCancelled(true);
            return;
        }
        if ("CREAKING".equalsIgnoreCase(type.name())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotion(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!isGuardian(p)) return;

        PotionEffect ne = e.getNewEffect();
        if (ne == null) return;

        PotionEffectType t = ne.getType();
        if (isAllowedOmenEffect(t)) return;

        // Возвращаем все системные бафы Хранителя (вода, дождь, день/ночь, камень и т.д.).
        if (isAllowedGuardianBuff(t)) return;

        if (t.equals(PotionEffectType.FIRE_RESISTANCE)) {
            e.setCancelled(true);
            return;
        }

        // Разрешаем бафы, которые накладывает сам плагин (GuardianTask и другие механики).
        if (e.getCause() != EntityPotionEffectEvent.Cause.PLUGIN) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        if (!isGuardian(p)) return;

        ItemStack it = e.getItem();
        Material type = it.getType();

        if (type.isEdible() && GuardianUtil.MEAT.contains(type)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "Хранители не едят мясо.");
            return;
        }

        if (type == Material.OMINOUS_BOTTLE) return;

        if (type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.YELLOW + "Хранители могут использовать только " + ChatColor.GOLD + "Зловещую бутылку" + ChatColor.YELLOW + ".");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (plugin.data.consumeStoneInterrupted(p)) {
            p.sendMessage(ChatColor.RED + "Прогресс Камня хранителя был сбит из-за перезахода.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (plugin.data.getStage(p) != 1) return;

        long sec = plugin.data.getStoneTimeSec(p);
        if (sec <= 0) return;

        plugin.data.setStoneTimeSec(p, 0);
        plugin.data.markStoneInterrupted(p, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDivineSeed(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        ItemStack it = e.getItem();
        if (it == null || !plugin.items.isDivineSeed(it)) return;

        e.setCancelled(true);

        if (!isGuardian(p)) {
            p.sendMessage(ChatColor.RED + "Ты не Хранитель и не можешь использовать этот предмет.");
            return;
        }

        Block clicked = e.getClickedBlock();
        if (clicked == null) return;

        Material t = clicked.getType();
        if (!(t == Material.GRASS_BLOCK || t == Material.DIRT || t == Material.PODZOL || t == Material.MOSS_BLOCK || t == Material.COARSE_DIRT || t == Material.ROOTED_DIRT)) {
            p.sendMessage(ChatColor.RED + "Нужно кликнуть по земле (трава/грязь/мох).");
            return;
        }

        World w = p.getWorld();
        Location base = clicked.getLocation().add(0.5, 1.0, 0.5);

        boolean ok = false;
        Random rnd = new Random();

        for (int attempt = 0; attempt < 12; attempt++) {
            TreeType typeTree = SEED_TREES[rnd.nextInt(SEED_TREES.length)];
            int ox = rnd.nextInt(3) - 1;
            int oz = rnd.nextInt(3) - 1;

            Location place = base.clone().add(ox, 0, oz);
            if (!place.getBlock().isPassable()) continue;

            if (w.generateTree(place, typeTree)) {
                ok = true;
                break;
            }
        }

        if (!ok) {
            p.sendMessage(ChatColor.YELLOW + "Не удалось вырастить дерево здесь (мало места).");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            return;
        }

        if (p.getGameMode() != GameMode.CREATIVE) {
            int a = it.getAmount() - 1;
            if (a <= 0) p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            else it.setAmount(a);
        }

        w.spawnParticle(Particle.HAPPY_VILLAGER, base, 30, 0.8, 0.8, 0.8, 0.01);
        w.playSound(base, Sound.ITEM_BONE_MEAL_USE, 0.9f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onStaff(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player p = e.getPlayer();
        ItemStack it = e.getItem();
        if (it == null || !plugin.items.isWaterStaff(it)) return;

        int stage = plugin.data.getStage(p);
        if (stage < 2) {
            p.sendMessage(ChatColor.RED + "Посох воды могут использовать только Приближённые и Истинные.");
            e.setCancelled(true);
            return;
        }

        Action a = e.getAction();

        if (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) {
            e.setCancelled(true);

            long now = System.currentTimeMillis();
            long last = staffLeftCd.getOrDefault(p.getUniqueId(), 0L);
            double left = cdLeftSec(now, last, STAFF_LEFT_CD_MS);
            if (left > 0) {
                actionbar(p, ChatColor.RED + "КД тушения: " + String.format(Locale.US, "%.1f", left) + "s");
                return;
            }
            staffLeftCd.put(p.getUniqueId(), now);

            p.setFireTicks(0);
            p.getWorld().spawnParticle(Particle.SPLASH, p.getLocation().add(0, 1.0, 0), 18, 0.35, 0.6, 0.35, 0.02);
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.2f);
            actionbar(p, ChatColor.AQUA + "Огонь потушен. КД 10s");
            return;
        }

        if ((a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) && p.isSneaking()) {
            e.setCancelled(true);

            long now = System.currentTimeMillis();
            long last = shieldCd.getOrDefault(p.getUniqueId(), 0L);
            double left = cdLeftSec(now, last, SHIELD_CD_MS);
            if (left > 0) {
                actionbar(p, ChatColor.RED + "КД щита: " + String.format(Locale.US, "%.1f", left) + "s");
                return;
            }

            shieldCd.put(p.getUniqueId(), now);
            shieldActiveUntil.put(p.getUniqueId(), now + SHIELD_DURATION_MS);

            p.setFireTicks(0);
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.9f, 1.5f);
            actionbar(p, ChatColor.AQUA + "Водяной щит: 5s (КД 5 минут)");
            startShieldParticles(p);
            return;
        }

        if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
            if (a == Action.RIGHT_CLICK_BLOCK && !p.isSneaking()) e.setCancelled(true);

            long now = System.currentTimeMillis();
            long last = staffRightCd.getOrDefault(p.getUniqueId(), 0L);
            if (now - last < STAFF_RIGHT_CD_MS) return;
            staffRightCd.put(p.getUniqueId(), now);

            Location eye = p.getEyeLocation();
            Vector dir = eye.getDirection().normalize();

            RayTraceResult res = GuardianUtil.rayTrace(p.getWorld(), eye, dir, 10.0, p);
            Location hitLoc = (res != null && res.getHitPosition() != null)
                    ? res.getHitPosition().toLocation(p.getWorld())
                    : eye.clone().add(dir.multiply(10.0));

            GuardianUtil.spawnWaterBeam(p.getWorld(), eye, hitLoc);
            p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.6f, 1.4f);

            if (res == null) return;

            if (res.getHitEntity() instanceof LivingEntity le) {
                le.damage(3.0, p);
                le.getWorld().spawnParticle(Particle.SPLASH, le.getLocation().add(0, 1.0, 0), 14, 0.35, 0.6, 0.35, 0.02);
                return;
            }

            Block hb = res.getHitBlock();
            if (hb == null) return;
            Material m = hb.getType();

            if (m == Material.LAVA && hb.getBlockData() instanceof Levelled lv) {
                if (lv.getLevel() == 0) {
                    hb.setType(Material.OBSIDIAN, false);
                    hb.getWorld().spawnParticle(Particle.CLOUD, hb.getLocation().add(0.5, 0.7, 0.5), 10, 0.3, 0.25, 0.3, 0.02);
                    hb.getWorld().playSound(hb.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 1.0f);
                } else {
                    actionbar(p, ChatColor.GRAY + "Нужен источник лавы.");
                }
                return;
            }

            if (m == Material.FIRE || m == Material.SOUL_FIRE) {
                hb.setType(Material.AIR, false);
                hb.getWorld().playSound(hb.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.2f);
                return;
            }

            Block up = hb.getRelative(BlockFace.UP);
            if (up.getType() == Material.FIRE || up.getType() == Material.SOUL_FIRE) {
                up.setType(Material.AIR, false);
                up.getWorld().playSound(up.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.2f);
            }
        }
    }

    private void startShieldParticles(Player p) {
        UUID id = p.getUniqueId();
        World w = p.getWorld();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                Player pl = Bukkit.getPlayer(id);
                if (pl == null || !pl.isOnline()) {
                    cancel();
                    return;
                }

                ticks += 2;
                if (ticks > 20 * 5) {
                    cancel();
                    return;
                }

                Location c = pl.getLocation().add(0, 1.0, 0);
                double r = 2.0;

                for (int i = 0; i < 18; i++) {
                    double ang = (i / 18.0) * Math.PI * 2.0 + (ticks * 0.12);
                    double y = Math.sin(ticks * 0.08 + i) * 0.6;
                    double x = Math.cos(ang) * r;
                    double z = Math.sin(ang) * r;

                    w.spawnParticle(Particle.BUBBLE, c.clone().add(x, y, z), 1, 0, 0, 0, 0);
                    if (i % 3 == 0) {
                        w.spawnParticle(Particle.BUBBLE_POP, c.clone().add(x * 0.8, y * 0.8, z * 0.8), 1, 0, 0, 0, 0);
                    }
                }

                if (ticks % 10 == 0) w.spawnParticle(Particle.SPLASH, c, 6, 0.7, 0.7, 0.7, 0.02);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
