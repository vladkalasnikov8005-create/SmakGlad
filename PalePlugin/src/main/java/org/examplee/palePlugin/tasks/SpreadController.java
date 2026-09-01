package org.examplee.palePlugin.tasks;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.examplee.palePlugin.PalePlugin;
import org.examplee.palePlugin.store.SourceStore;

public final class SpreadController {
    private final PalePlugin plugin;
    private boolean running = false;
    private BukkitTask spreadTask;
    private BukkitTask indexTask;
    private BukkitTask statsSecondTask;
    private BukkitTask playerEffectsTask;
    private BukkitTask stepEffectsTask;
    private final Map<UUID, Double> carryAttemptsByWorld = new HashMap<UUID, Double>();
    private final Map<UUID, ArrayDeque<ChunkPos>> indexQueueByWorld = new HashMap<UUID, ArrayDeque<ChunkPos>>();
    private final Map<UUID, Integer> loadedChunkCountByWorld = new HashMap<UUID, Integer>();
    private long totalAttempts = 0L;
    private long totalInfected = 0L;
    private long totalSkippedProtected = 0L;
    private long totalCleansed = 0L;
    private final int[] infectedPerSec = new int[60];
    private int infectedSecIndex = 0;
    private long lastEpochSecond = -1L;
    private final Random rnd = new Random();
    private static final String PERM_ADMIN = "pale.admin";
    private static final String PERM_EFFECT_IMMUNE = "pale.effect.immune";
    private final NamespacedKey KEY_LEPER_CLASS = NamespacedKey.fromString((String)"leperclass:class_leper");

    public SpreadController(PalePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void setRunning(boolean val) {
        if (val) {
            this.startRunning();
        } else {
            this.stopRunning();
        }
    }

    public void startAlwaysOnTasks() {
        this.startStatsSecondTask();
        this.startPlayerEffectsTask();
        this.startStepEffectsTask();
    }

    public void stopAllTasks() {
        this.stopRunning();
        this.stopStatsSecondTask();
        this.stopPlayerEffectsTask();
        this.stopStepEffectsTask();
    }

    public void startRunning() {
        if (this.running) {
            return;
        }
        this.running = true;
        this.refreshLoadedChunkCounts();
        this.enqueueLoadedChunksForIndex();
        this.startIndexTask();
        this.startSpreadTask();
    }

    public void stopRunning() {
        this.running = false;
        this.stopSpreadTask();
        this.stopIndexTask();
    }

    public void addCleansed(int n) {
        this.totalCleansed += (long)Math.max(0, n);
    }

    public void addInfected(int n) {
        if (n <= 0) {
            return;
        }
        this.totalInfected += (long)n;
        int n2 = this.infectedSecIndex;
        this.infectedPerSec[n2] = this.infectedPerSec[n2] + n;
    }

    public void refreshLoadedChunkCounts() {
        this.loadedChunkCountByWorld.clear();
        for (World w : Bukkit.getWorlds()) {
            this.loadedChunkCountByWorld.put(w.getUID(), w.getLoadedChunks().length);
        }
    }

    public void onChunkLoad(World w, int cx, int cz) {
        UUID wid = w.getUID();
        this.loadedChunkCountByWorld.put(wid, this.loadedChunkCountByWorld.getOrDefault(wid, 0) + 1);
        if (this.running) {
            this.indexQueueByWorld.computeIfAbsent(wid, k -> new ArrayDeque()).addLast(new ChunkPos(cx, cz));
        }
    }

    public void onChunkUnload(World w) {
        UUID wid = w.getUID();
        this.loadedChunkCountByWorld.put(wid, Math.max(0, this.loadedChunkCountByWorld.getOrDefault(wid, 0) - 1));
    }

    private void enqueueLoadedChunksForIndex() {
        for (World world : Bukkit.getWorlds()) {
            UUID wid = world.getUID();
            ArrayDeque q = this.indexQueueByWorld.computeIfAbsent(wid, k -> new ArrayDeque());
            for (Chunk ch : world.getLoadedChunks()) {
                q.addLast(new ChunkPos(ch.getX(), ch.getZ()));
            }
        }
    }

    private void startSpreadTask() {
        this.stopSpreadTask();
        this.spreadTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!this.running) {
                return;
            }
            this.rollStatsSecond();
            double s = Math.max(1, Math.min(5000, this.plugin.cfg.speedPerChunk));
            double tSpeed = (s - 1.0) / 4999.0;
            double turboT = Math.pow(tSpeed, 8.0);
            int effectiveGlobalCap = (int)Math.round(PaleConfig.lerp(this.plugin.cfg.maxAttemptsPerTickGlobal, this.plugin.cfg.turboMaxAttemptsPerTickGlobal, turboT));
            int effectivePerWorldCap = (int)Math.round(PaleConfig.lerp(this.plugin.cfg.maxAttemptsPerTickPerWorld, this.plugin.cfg.turboMaxAttemptsPerTickPerWorld, turboT));
            int globalBudget = effectiveGlobalCap;
            double perSecPerChunk = this.plugin.cfg.effectiveAttemptsPerSecondPerChunk();
            for (World world : Bukkit.getWorlds()) {
                SourceStore store;
                if (globalBudget <= 0) break;
                UUID wid = world.getUID();
                int loadedChunks = this.loadedChunkCountByWorld.getOrDefault(wid, 0);
                if (loadedChunks <= 0 || (store = this.plugin.engine.sources(world)).size() == 0) continue;
                double carry = this.carryAttemptsByWorld.getOrDefault(wid, 0.0);
                double want = (double)loadedChunks * (perSecPerChunk / 20.0) + carry;
                int attempts = (int)want;
                this.carryAttemptsByWorld.put(wid, want - (double)attempts);
                if (attempts <= 0) continue;
                if (store.size() > 5000 && this.plugin.cfg.speedPerChunk > 2500) {
                    attempts = (int)Math.ceil((double)attempts * 1.2);
                }
                attempts = Math.min(attempts, effectivePerWorldCap);
                if ((attempts = Math.min(attempts, globalBudget)) <= 0) continue;
                int perWorldRemain = Math.max(0, effectivePerWorldCap - attempts);
                int globalRemain = Math.max(0, globalBudget - attempts);
                int burstMax = this.plugin.cfg.speedPerChunk > 3000 ? Math.max(0, attempts / 2) : 0;
                int extraBurstBudget = Math.min(burstMax, Math.min(perWorldRemain, globalRemain));
                int extraSpent = 0;
                for (int i = 0; i < attempts; ++i) {
                    ++this.totalAttempts;
                    Block source = store.getRandomLiveSource(world, this.rnd, this.plugin.engine.infectedTypes());
                    if (source == null) break;
                    if (this.plugin.engine.wards(world).isProtected(source.getX(), source.getY(), source.getZ(), this.plugin.cfg.wardRadius)) {
                        ++this.totalSkippedProtected;
                        continue;
                    }
                    if (!this.plugin.engine.trySpreadFromSource(source)) continue;
                    this.addInfected(1);
                    if (this.plugin.cfg.speedPerChunk <= 3000 || extraBurstBudget <= 0 || this.rnd.nextInt(100) >= 35) continue;
                    --extraBurstBudget;
                    ++extraSpent;
                    ++this.totalAttempts;
                    if (!this.plugin.engine.trySpreadFromSource(source)) continue;
                    this.addInfected(1);
                }
                globalBudget -= attempts + extraSpent;
                if (this.rnd.nextInt(200) != 0) continue;
                store.compactIfNeeded();
            }
        }, 1L, 1L);
    }

    private void stopSpreadTask() {
        if (this.spreadTask != null) {
            this.spreadTask.cancel();
            this.spreadTask = null;
        }
    }

    private void startIndexTask() {
        this.stopIndexTask();
        if (this.plugin.cfg.indexChunksPerTickPerWorld <= 0) {
            return;
        }
        this.indexTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!this.running) {
                return;
            }
            for (World world : Bukkit.getWorlds()) {
                ChunkPos pos;
                UUID wid = world.getUID();
                ArrayDeque<ChunkPos> q = this.indexQueueByWorld.get(wid);
                if (q == null || q.isEmpty()) continue;
                int processed = 0;
                while (processed < this.plugin.cfg.indexChunksPerTickPerWorld && !q.isEmpty() && (pos = q.pollFirst()) != null) {
                    if (!world.isChunkLoaded(pos.x, pos.z)) continue;
                    this.plugin.engine.indexChunkSurface(world, pos.x, pos.z);
                    ++processed;
                }
            }
        }, 1L, 1L);
    }

    private void stopIndexTask() {
        if (this.indexTask != null) {
            this.indexTask.cancel();
            this.indexTask = null;
        }
    }

    private void startStatsSecondTask() {
        this.stopStatsSecondTask();
        this.statsSecondTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::rollStatsSecond, 20L, 20L);
    }

    private void stopStatsSecondTask() {
        if (this.statsSecondTask != null) {
            this.statsSecondTask.cancel();
            this.statsSecondTask = null;
        }
    }

    private void rollStatsSecond() {
        long sec = System.currentTimeMillis() / 1000L;
        if (this.lastEpochSecond == -1L) {
            this.lastEpochSecond = sec;
            return;
        }
        long diff = sec - this.lastEpochSecond;
        if (diff <= 0L) {
            return;
        }
        int steps = (int)Math.min(diff, 60L);
        for (int i = 0; i < steps; ++i) {
            this.infectedSecIndex = (this.infectedSecIndex + 1) % 60;
            this.infectedPerSec[this.infectedSecIndex] = 0;
        }
        this.lastEpochSecond = sec;
    }

    private int infectedLastMinute() {
        int sum = 0;
        for (int v : this.infectedPerSec) {
            sum += v;
        }
        return sum;
    }

    private void startPlayerEffectsTask() {
        this.stopPlayerEffectsTask();
        this.playerEffectsTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!this.plugin.cfg.effectsEnabled || !this.plugin.cfg.stagesEnabled) {
                return;
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                int cz;
                int cx;
                World w;
                int stage;
                if (p.hasPermission(PERM_EFFECT_IMMUNE) || p.hasPermission(PERM_ADMIN) || this.isLeper(p) || (stage = this.plugin.engine.getChunkStage(w = p.getWorld(), cx = p.getLocation().getBlockX() >> 4, cz = p.getLocation().getBlockZ() >> 4)) < this.plugin.cfg.effectsMinStage) continue;
                int dur = this.plugin.cfg.effectsCheckPeriodTicks + 40;
                if (stage >= 2) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, this.plugin.cfg.effectsSlownessAmpStage2, true, false, true));
                }
                if (stage >= 3) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, dur, this.plugin.cfg.effectsWeaknessAmpStage3, true, false, true));
                }
                if (stage >= 4) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, dur, this.plugin.cfg.effectsMiningFatigueAmpStage4, true, false, true));
                }
                if (stage < 5 || !this.plugin.cfg.effectsDarknessStage5) continue;
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, dur, 0, true, false, true));
            }
        }, 20L, (long)this.plugin.cfg.effectsCheckPeriodTicks);
    }

    private void stopPlayerEffectsTask() {
        if (this.playerEffectsTask != null) {
            this.playerEffectsTask.cancel();
            this.playerEffectsTask = null;
        }
    }

    private void startStepEffectsTask() {
        this.stopStepEffectsTask();
        this.stepEffectsTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                GameMode gm = p.getGameMode();
                if (gm == GameMode.SPECTATOR || gm == GameMode.CREATIVE) continue;
                Block under = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
                if (!this.plugin.engine.infectedTypes().contains(under.getType())) continue;
                int dur = this.plugin.cfg.stepEffectsDurationTicks;
                if (this.isLeper(p)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, 0, true, false, true));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, dur, 0, true, false, true));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, dur, 0, true, false, true));
                    continue;
                }
                if (p.hasPermission(PERM_ADMIN) || p.hasPermission(PERM_EFFECT_IMMUNE)) continue;
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, 0, true, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, dur, 0, true, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, dur, 0, true, false, true));
            }
        }, 10L, (long)this.plugin.cfg.stepEffectsCheckPeriodTicks);
    }

    private void stopStepEffectsTask() {
        if (this.stepEffectsTask != null) {
            this.stepEffectsTask.cancel();
            this.stepEffectsTask = null;
        }
    }

    private boolean isLeper(Player p) {
        if (this.KEY_LEPER_CLASS == null) {
            return false;
        }
        Byte v = (Byte)p.getPersistentDataContainer().get(this.KEY_LEPER_CLASS, PersistentDataType.BYTE);
        return v != null && v == 1;
    }

    public void sendInfo(CommandSender sender) {
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "[Pale] running=" + this.running + " speed=" + this.plugin.cfg.speedPerChunk + " rate\u2248" + String.format(Locale.US, "%.6f", this.plugin.cfg.effectiveAttemptsPerSecondPerChunk()));
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "[Pale] infected total=" + this.totalInfected + " attempts total=" + this.totalAttempts + " skippedProtected=" + this.totalSkippedProtected + " cleansed total=" + this.totalCleansed + " infected/min=" + this.infectedLastMinute());
        for (World w : Bukkit.getWorlds()) {
            UUID wid = w.getUID();
            int loaded = this.loadedChunkCountByWorld.getOrDefault(wid, 0);
            int sources = this.plugin.engine.sources(w).size();
            int wards = this.plugin.engine.wards(w).size();
            int biomeCells = this.plugin.engine.biomes(w).size();
            sender.sendMessage(String.valueOf(ChatColor.DARK_GRAY) + "world=" + w.getName() + " loaded=" + loaded + " sources=" + sources + " wards=" + wards + " biomeCells=" + biomeCells);
        }
    }

    private static final class ChunkPos {
        final int x;
        final int z;

        ChunkPos(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    private static final class PaleConfig {
        private PaleConfig() {
        }

        static double lerp(double a, double b, double t) {
            if (t <= 0.0) {
                return a;
            }
            if (t >= 1.0) {
                return b;
            }
            return a + (b - a) * t;
        }
    }
}

