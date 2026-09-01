package org.examplee.leperClassPlugin.tasks;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.examplee.leperClassPlugin.LeperClassPlugin;
import org.examplee.leperClassPlugin.util.SunUtil;

public final class SunAndInfectionTask {
    private final LeperClassPlugin plugin;
    private BukkitRunnable task;

    public SunAndInfectionTask(LeperClassPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.stop();
        this.task = new BukkitRunnable(){

            public void run() {
                long now = System.currentTimeMillis();
                int leperCount = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!SunAndInfectionTask.this.plugin.data.isLeper(p)) continue;
                    ++leperCount;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PotionEffect cur;
                    SunAndInfectionTask.this.plugin.infection.checkProgression(p, now);
                    if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
                    boolean isLeper = SunAndInfectionTask.this.plugin.data.isLeper(p);
                    boolean isStage2 = SunAndInfectionTask.this.plugin.data.getInfectionStage(p) == 2;
                    PotionEffectType wb = SunAndInfectionTask.this.plugin.effects.WATER_BREATHING;
                    if (isLeper && wb != null && ((cur = p.getPotionEffect(wb)) == null || cur.getDuration() < 40)) {
                        p.addPotionEffect(new PotionEffect(wb, 300, 0, false, false, false));
                    }
                    if ((isLeper || isStage2) && SunAndInfectionTask.this.plugin.effects.FIRE_RES != null && p.hasPotionEffect(SunAndInfectionTask.this.plugin.effects.FIRE_RES)) {
                        p.removePotionEffect(SunAndInfectionTask.this.plugin.effects.FIRE_RES);
                    }
                    if ((isLeper || isStage2) && SunUtil.shouldBurnInSun(p)) {
                        if (SunUtil.isOnPaleSurface(p) || SunAndInfectionTask.this.plugin.umbrella.hasUmbrellaInOffhand(p)) {
                            if (p.getFireTicks() > 0) {
                                p.setFireTicks(0);
                            }
                            if (SunAndInfectionTask.this.plugin.umbrella.hasUmbrellaInOffhand(p)) {
                                SunAndInfectionTask.this.plugin.umbrella.damageUmbrellaInOffhand(p);
                            }
                        } else {
                            p.setFireTicks(Math.max(p.getFireTicks(), 60));
                        }
                    }
                    if (!isLeper) continue;
                    SunAndInfectionTask.this.applyPopulationBuffs(p, leperCount);
                    if (p.getFoodLevel() <= 4 && SunAndInfectionTask.this.plugin.data.getRageUntil(p) < now) {
                        SunAndInfectionTask.this.plugin.data.setRageUntil(p, now + 180000L);
                        p.sendMessage("\u00a7c\u042f\u0440\u043e\u0441\u0442\u044c \u043e\u0445\u0432\u0430\u0442\u0438\u043b\u0430 \u0432\u0430\u0441!");
                    }
                    if (p.getFoodLevel() > 4 && SunAndInfectionTask.this.plugin.data.getRageUntil(p) > 0L) {
                        SunAndInfectionTask.this.plugin.data.setRageUntil(p, 0L);
                    }
                    if (SunAndInfectionTask.this.plugin.data.getRageUntil(p) > now) {
                        if (SunAndInfectionTask.this.plugin.effects.STRENGTH != null) {
                            p.addPotionEffect(new PotionEffect(SunAndInfectionTask.this.plugin.effects.STRENGTH, 80, 1, false, false, true));
                        }
                        if (SunAndInfectionTask.this.plugin.effects.SPEED != null) {
                            p.addPotionEffect(new PotionEffect(SunAndInfectionTask.this.plugin.effects.SPEED, 80, 1, false, false, true));
                        }
                    }
                    if (p.getFoodLevel() <= 0) {
                        if (SunAndInfectionTask.this.plugin.effects.BLINDNESS != null) {
                            p.addPotionEffect(new PotionEffect(SunAndInfectionTask.this.plugin.effects.BLINDNESS, 80, 0, false, false, true));
                        }
                        if (SunAndInfectionTask.this.plugin.effects.SLOW != null) {
                            p.addPotionEffect(new PotionEffect(SunAndInfectionTask.this.plugin.effects.SLOW, 80, 2, false, false, true));
                        }
                    }
                    for (Entity en : p.getNearbyEntities(20.0, 12.0, 20.0)) {
                        Mob mob;
                        EntityType t;
                        if (!(en instanceof Mob) || (t = (mob = (Mob)en).getType()) != EntityType.IRON_GOLEM && t != EntityType.SNOW_GOLEM || mob.getTarget() != null && mob.getTarget().getUniqueId().equals(p.getUniqueId())) continue;
                        mob.setTarget((LivingEntity)p);
                    }
                }
            }
        };
        this.task.runTaskTimer((Plugin)this.plugin, 20L, 20L);
    }

    private void applyPopulationBuffs(Player p, int lepers) {
        int regenAmp = -1;
        int speedAmp = -1;
        int strAmp = -1;
        if (lepers >= 7) {
            regenAmp = 1;
            speedAmp = 2;
            strAmp = 2;
        } else if (lepers >= 5) {
            regenAmp = 0;
            speedAmp = 1;
            strAmp = 0;
        } else if (lepers >= 3) {
            regenAmp = 0;
            speedAmp = 0;
        }
        if (regenAmp >= 0 && this.plugin.effects.REGEN != null) {
            p.addPotionEffect(new PotionEffect(this.plugin.effects.REGEN, 80, regenAmp, false, false, true));
        }
        if (speedAmp >= 0 && this.plugin.effects.SPEED != null) {
            p.addPotionEffect(new PotionEffect(this.plugin.effects.SPEED, 80, speedAmp, false, false, true));
        }
        if (strAmp >= 0 && this.plugin.effects.STRENGTH != null) {
            p.addPotionEffect(new PotionEffect(this.plugin.effects.STRENGTH, 80, strAmp, false, false, true));
        }
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }
}

