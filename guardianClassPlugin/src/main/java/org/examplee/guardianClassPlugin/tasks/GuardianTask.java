package org.examplee.guardianClassPlugin.tasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.examplee.guardianClassPlugin.GuardianClassPlugin;
import org.examplee.guardianClassPlugin.util.GuardianUtil;

import java.util.Random;

public final class GuardianTask {

    private final GuardianClassPlugin plugin;
    private BukkitRunnable task;
    private int tick = 0;
    private final Random rnd = new Random();

    public GuardianTask(GuardianClassPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick++;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    int stage = plugin.data.getStage(p);
                    if (stage == 0 && plugin.items.isGuardianStone(p.getInventory().getItemInOffHand())) {
                        plugin.data.setStage(p, 1);
                        stage = 1;
                    }
                    if (stage <= 0) continue;

                    p.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);

                    if (p.getFallDistance() > 4.0f) {
                        PotionEffect cur = p.getPotionEffect(PotionEffectType.SLOW_FALLING);
                        if (cur == null || cur.getDuration() < 20) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, true, false, true));
                        }
                    }

                    if (stage >= 2) {
                        boolean inWater = p.isInWater() || p.isInBubbleColumn();
                        boolean inRain;
                        try {
                            inRain = p.isInRain();
                        } catch (Throwable ignored) {
                            inRain = false;
                        }

                        if (inWater) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60, 0, true, false, true));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 60, 0, true, false, true));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, false, true));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, true, false, true));

                            if (tick % 10 == 0) {
                                p.getWorld().spawnParticle(Particle.SPLASH, p.getLocation().add(0, 1.0, 0), 2, 0.25, 0.4, 0.25, 0.01);
                            }
                        } else if (inRain) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false, true));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, false, true));

                            if (tick % 20 == 0) {
                                p.getWorld().spawnParticle(Particle.DRIPPING_WATER, p.getLocation().add(0, 1.4, 0), 4, 0.35, 0.25, 0.35, 0.01);
                            }
                        }
                    }

                    if (stage >= 2) {
                        if (GuardianUtil.isDay(p.getWorld())) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 80, 0, true, false, true));
                        } else {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0, true, false, true));
                        }
                    }

                    boolean nearFlower = plugin.blocks.hasFlowerNearby(p.getLocation(), 24, 10, 3);
                    if (stage >= 2 && nearFlower) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false, true));
                        if (tick % 10 == 0) {
                            p.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, p.getLocation().add(0, 1.2, 0), 2, 0.35, 0.25, 0.35, 0.01);
                        }
                    }

                    boolean holdingLifeStoneItem = plugin.items.isLifeStoneItem(p.getInventory().getItemInMainHand())
                            || plugin.items.isLifeStoneItem(p.getInventory().getItemInOffHand());

                    // Critical fix: step=1 removes movement-dependent misses and keeps effects stable while standing still.
                    boolean nearLifeStoneBlock = plugin.blocks.hasLifeStoneNearby(p.getLocation(), 12, 8, 1);

                    if (holdingLifeStoneItem || nearLifeStoneBlock) {
                        applyLifeStoneEffects(p, stage, nearLifeStoneBlock);

                        if (tick % 12 == 0) {
                            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1.0, 0), 2, 0.45, 0.35, 0.45, 0.01);
                            p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1.0, 0), 1, 0.25, 0.25, 0.25, 0.0);
                        }
                    }

                    if (nearLifeStoneBlock && tick % 20 == 0) {
                        boostPlantsNearPlayer(p);
                    }

                    if (stage == 1) {
                        ItemStack offhand = p.getInventory().getItemInOffHand();
                        boolean hasGuardianStone = plugin.items.isGuardianStone(offhand);

                        if (!hasGuardianStone) {
                            long sec = plugin.data.getStoneTimeSec(p);
                            if (sec > 0) {
                                plugin.data.setStoneTimeSec(p, 0);
                                p.sendMessage(ChatColor.RED + "Ты убрал Камень хранителя из левой руки. Прогресс сброшен.");
                            }
                            continue;
                        }

                        if (tick % 20 == 0) {
                            long sec = plugin.data.getStoneTimeSec(p) + 1;
                            plugin.data.setStoneTimeSec(p, sec);

                            if (sec >= 5L * 60L * 60L) {
                                if (p.getGameMode() != GameMode.CREATIVE) {
                                    int amount = offhand.getAmount();
                                    if (amount <= 1) {
                                        p.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                                    } else {
                                        offhand.setAmount(amount - 1);
                                        p.getInventory().setItemInOffHand(offhand);
                                    }
                                }

                                plugin.data.setStage(p, 2);
                                plugin.data.setStoneTimeSec(p, 0);
                                p.sendMessage(ChatColor.AQUA + "Ты стал Приближённым Хранителем!");
                                p.sendMessage(ChatColor.GRAY + "Камень хранителя исчезает после завершения посвящения.");
                                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
                            }
                        }
                    }
                }
            }
        };
        task.runTaskTimer(plugin, 20L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void applyLifeStoneEffects(Player p, int stage, boolean nearLifeStoneBlock) {
        if (stage == 3) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1, true, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 1, true, false, true));
            return;
        }
        if (stage == 2) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 0, true, false, true));
            return;
        }
        if (stage == 1 && nearLifeStoneBlock && plugin.items.isGuardianStone(p.getInventory().getItemInOffHand())) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false, true));
        }
    }

    private void boostPlantsNearPlayer(Player p) {
        World w = p.getWorld();
        Location c = p.getLocation();
        int cx = c.getBlockX();
        int cy = c.getBlockY();
        int cz = c.getBlockZ();

        for (int i = 0; i < 6; i++) {
            int x = cx + rnd.nextInt(25) - 12;
            int y = Math.max(w.getMinHeight(), Math.min(w.getMaxHeight() - 1, cy + rnd.nextInt(9) - 4));
            int z = cz + rnd.nextInt(25) - 12;

            Block b = w.getBlockAt(x, y, z);
            if (!(b.getBlockData() instanceof Ageable age)) continue;
            if (age.getAge() >= age.getMaximumAge()) continue;

            age.setAge(age.getAge() + 1);
            b.setBlockData(age, false);
        }
    }
}
