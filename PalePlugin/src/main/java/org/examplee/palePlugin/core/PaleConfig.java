package org.examplee.palePlugin.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.examplee.palePlugin.util.MathUtil;

public final class PaleConfig {
    private final JavaPlugin plugin;
    public int speedPerChunk = 40;
    public int maxAttemptsPerTickGlobal = 3000;
    public int maxAttemptsPerTickPerWorld = 1800;
    public int turboMaxAttemptsPerTickGlobal = 3000;
    public int turboMaxAttemptsPerTickPerWorld = 1800;
    public int maxSourcesPerWorld = 250000;
    public int sourceChanceDivider = 6;
    public double rateMinPerSecPerChunk = 2.0E-4;
    public double rateMaxPerSecPerChunk = 0.25;
    public double rateCurvePower = 2.0;
    public int spreadTriesPerAttempt = 3;
    public int logSourceChanceDivider = 4;
    public int indexChunksPerTickPerWorld = 1;
    public int indexDepth = 28;
    public int saltRadius = 8;
    public long saltCooldownMs = 1500L;
    public int holyWaterRadius = 10;
    public int purifierFlintRadius = 10;
    public int purifierFlintUsesDefault = 2;
    public int wardRadius = 24;
    public int mapMaxRadiusChunks = 8;
    public int mapItemDefaultRadiusChunks = 6;
    public int infectWandRadius = 6;
    public long infectWandCooldownMs = 800L;
    public int infectWandUsesDefault = 16;
    public int infectWandMaxBlocksPerUse = 900;
    public int infectWandBonusSourceChanceDivider = 2;
    public boolean biomeEnabled = true;
    public String infectedBiomeName = "minecraft:pale_garden";
    public boolean stagesEnabled = true;
    public int stage1Sources = 5;
    public int stage2Sources = 15;
    public int stage3Sources = 40;
    public int stage4Sources = 90;
    public int stage5Sources = 180;
    public boolean effectsEnabled = true;
    public int effectsCheckPeriodTicks = 20;
    public int effectsMinStage = 2;
    public int effectsSlownessAmpStage2 = 0;
    public int effectsWeaknessAmpStage3 = 0;
    public int effectsMiningFatigueAmpStage4 = 0;
    public boolean effectsDarknessStage5 = true;
    public int stepEffectsCheckPeriodTicks = 5;
    public int stepEffectsDurationTicks = 60;
    public int adminPurgeRadiusChunks = 8;
    public int adminPurgeDepth = 64;
    public int adminPurgeChunksPerTick = 2;
    public boolean adminPurgeOnlyLoadedChunks = true;

    public PaleConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setupDefaults() {
        FileConfiguration cfg = this.plugin.getConfig();
        cfg.addDefault("spread.speedPerChunk", (Object)40);
        cfg.addDefault("spread.maxAttemptsPerTickGlobal", (Object)3000);
        cfg.addDefault("spread.maxAttemptsPerTickPerWorld", (Object)1800);
        cfg.addDefault("spread.turboMaxAttemptsPerTickGlobal", (Object)3000);
        cfg.addDefault("spread.turboMaxAttemptsPerTickPerWorld", (Object)1800);
        cfg.addDefault("spread.maxSourcesPerWorld", (Object)250000);
        cfg.addDefault("spread.sourceChanceDivider", (Object)6);
        cfg.addDefault("spread.rateMinPerSecPerChunk", (Object)2.0E-4);
        cfg.addDefault("spread.rateMaxPerSecPerChunk", (Object)0.25);
        cfg.addDefault("spread.rateCurvePower", (Object)2.0);
        cfg.addDefault("spread.spreadTriesPerAttempt", (Object)3);
        cfg.addDefault("spread.logSourceChanceDivider", (Object)4);
        cfg.addDefault("index.indexChunksPerTickPerWorld", (Object)1);
        cfg.addDefault("index.indexDepth", (Object)28);
        cfg.addDefault("cleanse.saltRadius", (Object)8);
        cfg.addDefault("cleanse.saltCooldownMs", (Object)1500);
        cfg.addDefault("cleanse.holyWaterRadius", (Object)10);
        cfg.addDefault("cleanse.purifierFlintRadius", (Object)10);
        cfg.addDefault("cleanse.purifierFlintUses", (Object)2);
        cfg.addDefault("ward.radius", (Object)24);
        cfg.addDefault("map.maxRadiusChunks", (Object)8);
        cfg.addDefault("map.itemDefaultRadiusChunks", (Object)6);
        cfg.addDefault("wand.radius", (Object)6);
        cfg.addDefault("wand.cooldownMs", (Object)800);
        cfg.addDefault("wand.uses", (Object)16);
        cfg.addDefault("wand.maxBlocksPerUse", (Object)900);
        cfg.addDefault("wand.bonusSourceChanceDivider", (Object)2);
        cfg.addDefault("biome.enabled", (Object)true);
        cfg.addDefault("biome.infected", (Object)"minecraft:pale_garden");
        cfg.addDefault("stages.enabled", (Object)true);
        cfg.addDefault("stages.stage1Sources", (Object)5);
        cfg.addDefault("stages.stage2Sources", (Object)15);
        cfg.addDefault("stages.stage3Sources", (Object)40);
        cfg.addDefault("stages.stage4Sources", (Object)90);
        cfg.addDefault("stages.stage5Sources", (Object)180);
        cfg.addDefault("effects.enabled", (Object)true);
        cfg.addDefault("effects.checkPeriodTicks", (Object)20);
        cfg.addDefault("effects.minStage", (Object)2);
        cfg.addDefault("effects.slownessAmpStage2", (Object)0);
        cfg.addDefault("effects.weaknessAmpStage3", (Object)0);
        cfg.addDefault("effects.miningFatigueAmpStage4", (Object)0);
        cfg.addDefault("effects.darknessStage5", (Object)true);
        cfg.addDefault("stepEffects.checkPeriodTicks", (Object)5);
        cfg.addDefault("stepEffects.durationTicks", (Object)60);
        cfg.addDefault("adminPurge.radiusChunks", (Object)8);
        cfg.addDefault("adminPurge.depth", (Object)64);
        cfg.addDefault("adminPurge.chunksPerTick", (Object)2);
        cfg.addDefault("adminPurge.onlyLoadedChunks", (Object)true);
        cfg.options().copyDefaults(true);
        this.plugin.saveConfig();
    }

    public void load() {
        FileConfiguration cfg = this.plugin.getConfig();
        this.speedPerChunk = MathUtil.clamp(cfg.getInt("spread.speedPerChunk", 40), 1, 5000);
        this.maxAttemptsPerTickGlobal = MathUtil.clamp(cfg.getInt("spread.maxAttemptsPerTickGlobal", 3000), 50, 500000);
        this.maxAttemptsPerTickPerWorld = MathUtil.clamp(cfg.getInt("spread.maxAttemptsPerTickPerWorld", 1800), 50, 500000);
        this.turboMaxAttemptsPerTickGlobal = MathUtil.clamp(cfg.getInt("spread.turboMaxAttemptsPerTickGlobal", 3000), 1000, 500000);
        this.turboMaxAttemptsPerTickPerWorld = MathUtil.clamp(cfg.getInt("spread.turboMaxAttemptsPerTickPerWorld", 1800), 1000, 500000);
        this.maxSourcesPerWorld = MathUtil.clamp(cfg.getInt("spread.maxSourcesPerWorld", 250000), 1000, 5000000);
        this.sourceChanceDivider = MathUtil.clamp(cfg.getInt("spread.sourceChanceDivider", 6), 1, 50);
        this.rateMinPerSecPerChunk = Math.max(1.0E-12, cfg.getDouble("spread.rateMinPerSecPerChunk", 2.0E-4));
        this.rateMaxPerSecPerChunk = Math.max(this.rateMinPerSecPerChunk, cfg.getDouble("spread.rateMaxPerSecPerChunk", 0.25));
        this.rateCurvePower = Math.max(1.0, cfg.getDouble("spread.rateCurvePower", 2.0));
        this.spreadTriesPerAttempt = MathUtil.clamp(cfg.getInt("spread.spreadTriesPerAttempt", 3), 1, 50);
        this.logSourceChanceDivider = MathUtil.clamp(cfg.getInt("spread.logSourceChanceDivider", 4), 1, 50);
        this.indexChunksPerTickPerWorld = MathUtil.clamp(cfg.getInt("index.indexChunksPerTickPerWorld", 1), 0, 200);
        this.indexDepth = MathUtil.clamp(cfg.getInt("index.indexDepth", 28), 1, 128);
        this.saltRadius = MathUtil.clamp(cfg.getInt("cleanse.saltRadius", 8), 1, 64);
        this.saltCooldownMs = Math.max(0L, cfg.getLong("cleanse.saltCooldownMs", 1500L));
        this.holyWaterRadius = MathUtil.clamp(cfg.getInt("cleanse.holyWaterRadius", 10), 1, 64);
        this.purifierFlintRadius = MathUtil.clamp(cfg.getInt("cleanse.purifierFlintRadius", 10), 1, 64);
        this.purifierFlintUsesDefault = MathUtil.clamp(cfg.getInt("cleanse.purifierFlintUses", 2), 1, 64);
        this.wardRadius = MathUtil.clamp(cfg.getInt("ward.radius", 24), 4, 128);
        this.mapMaxRadiusChunks = MathUtil.clamp(cfg.getInt("map.maxRadiusChunks", 8), 1, 32);
        this.mapItemDefaultRadiusChunks = MathUtil.clamp(cfg.getInt("map.itemDefaultRadiusChunks", 6), 1, this.mapMaxRadiusChunks);
        this.infectWandRadius = MathUtil.clamp(cfg.getInt("wand.radius", 6), 1, 64);
        this.infectWandCooldownMs = Math.max(0L, cfg.getLong("wand.cooldownMs", 800L));
        this.infectWandUsesDefault = MathUtil.clamp(cfg.getInt("wand.uses", 16), 1, 10000);
        this.infectWandMaxBlocksPerUse = MathUtil.clamp(cfg.getInt("wand.maxBlocksPerUse", 900), 10, 50000);
        this.infectWandBonusSourceChanceDivider = MathUtil.clamp(cfg.getInt("wand.bonusSourceChanceDivider", 2), 1, 50);
        this.biomeEnabled = cfg.getBoolean("biome.enabled", true);
        this.infectedBiomeName = cfg.getString("biome.infected", "minecraft:pale_garden");
        if (this.infectedBiomeName == null || this.infectedBiomeName.isBlank()) {
            this.infectedBiomeName = "minecraft:pale_garden";
        }
        this.stagesEnabled = cfg.getBoolean("stages.enabled", true);
        this.stage1Sources = MathUtil.clamp(cfg.getInt("stages.stage1Sources", 5), 1, 1000000);
        this.stage2Sources = MathUtil.clamp(cfg.getInt("stages.stage2Sources", 15), this.stage1Sources + 1, 1000000);
        this.stage3Sources = MathUtil.clamp(cfg.getInt("stages.stage3Sources", 40), this.stage2Sources + 1, 1000000);
        this.stage4Sources = MathUtil.clamp(cfg.getInt("stages.stage4Sources", 90), this.stage3Sources + 1, 1000000);
        this.stage5Sources = MathUtil.clamp(cfg.getInt("stages.stage5Sources", 180), this.stage4Sources + 1, 1000000);
        this.effectsEnabled = cfg.getBoolean("effects.enabled", true);
        this.effectsCheckPeriodTicks = MathUtil.clamp(cfg.getInt("effects.checkPeriodTicks", 20), 5, 200);
        this.effectsMinStage = MathUtil.clamp(cfg.getInt("effects.minStage", 2), 0, 5);
        this.effectsSlownessAmpStage2 = MathUtil.clamp(cfg.getInt("effects.slownessAmpStage2", 0), 0, 5);
        this.effectsWeaknessAmpStage3 = MathUtil.clamp(cfg.getInt("effects.weaknessAmpStage3", 0), 0, 5);
        this.effectsMiningFatigueAmpStage4 = MathUtil.clamp(cfg.getInt("effects.miningFatigueAmpStage4", 0), 0, 5);
        this.effectsDarknessStage5 = cfg.getBoolean("effects.darknessStage5", true);
        this.stepEffectsCheckPeriodTicks = MathUtil.clamp(cfg.getInt("stepEffects.checkPeriodTicks", 5), 1, 200);
        this.stepEffectsDurationTicks = MathUtil.clamp(cfg.getInt("stepEffects.durationTicks", 60), 20, 1200);
        this.adminPurgeRadiusChunks = MathUtil.clamp(cfg.getInt("adminPurge.radiusChunks", 8), 1, 32);
        this.adminPurgeDepth = MathUtil.clamp(cfg.getInt("adminPurge.depth", 64), 1, 256);
        this.adminPurgeChunksPerTick = MathUtil.clamp(cfg.getInt("adminPurge.chunksPerTick", 2), 1, 50);
        this.adminPurgeOnlyLoadedChunks = cfg.getBoolean("adminPurge.onlyLoadedChunks", true);
    }

    public double effectiveAttemptsPerSecondPerChunk() {
        double s = Math.max(1, Math.min(5000, this.speedPerChunk));
        double t = (s - 1.0) / 4999.0;
        double shaped = Math.pow(t, this.rateCurvePower);
        double min = Math.max(1.0E-12, this.rateMinPerSecPerChunk);
        double max = Math.max(min, this.rateMaxPerSecPerChunk);
        return min * Math.pow(max / min, shaped);
    }

    public static double lerp(double a, double b, double t) {
        if (t <= 0.0) {
            return a;
        }
        if (t >= 1.0) {
            return b;
        }
        return a + (b - a) * t;
    }
}

