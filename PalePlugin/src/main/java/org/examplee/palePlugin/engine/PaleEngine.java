package org.examplee.palePlugin.engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.examplee.palePlugin.core.PaleConfig;
import org.examplee.palePlugin.core.PaleMaterials;
import org.examplee.palePlugin.store.BiomeStore;
import org.examplee.palePlugin.store.SourceStore;
import org.examplee.palePlugin.store.WardStore;
import org.examplee.palePlugin.util.BiomeUtil;
import org.examplee.palePlugin.util.MathUtil;

public final class PaleEngine {
    private final JavaPlugin plugin;
    public final PaleConfig cfg;
    public final PaleMaterials mats;
    private final Random random = new Random();
    private final Map<UUID, SourceStore> sourcesByWorld = new HashMap<UUID, SourceStore>();
    private final Map<UUID, WardStore> wardsByWorld = new HashMap<UUID, WardStore>();
    private final Map<UUID, BiomeStore> biomeStoreByWorld = new HashMap<UUID, BiomeStore>();
    private NamespacedKey infectedBiomeKey;
    private Biome infectedBiome;

    public PaleEngine(JavaPlugin plugin, PaleConfig cfg, PaleMaterials mats) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.mats = mats;
    }

    public Set<Material> infectedTypes() {
        return this.mats.getInfectedTypes();
    }

    public SourceStore sources(World w) {
        return this.sourcesByWorld.computeIfAbsent(w.getUID(), k -> new SourceStore());
    }

    public WardStore wards(World w) {
        return this.wardsByWorld.computeIfAbsent(w.getUID(), k -> new WardStore());
    }

    public BiomeStore biomes(World w) {
        return this.biomeStoreByWorld.computeIfAbsent(w.getUID(), k -> new BiomeStore());
    }

    public Map<UUID, WardStore> wardsByWorld() {
        return this.wardsByWorld;
    }

    public Map<UUID, BiomeStore> biomesByWorld() {
        return this.biomeStoreByWorld;
    }

    public void resolveInfectedBiomeOrDisableBiome() {
        this.infectedBiome = null;
        this.infectedBiomeKey = null;
        if (!this.cfg.biomeEnabled) {
            return;
        }
        this.infectedBiomeKey = BiomeUtil.parseBiomeKey(this.cfg.infectedBiomeName);
        if (this.infectedBiomeKey == null) {
            this.cfg.biomeEnabled = false;
            this.plugin.getLogger().warning("[PalePlugin] biome.infected \u043d\u0435\u0432\u0435\u0440\u043d\u044b\u0439: " + this.cfg.infectedBiomeName + ". \u0411\u0438\u043e\u043c\u044b \u043e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u044b.");
            return;
        }
        try {
            this.infectedBiome = (Biome)Registry.BIOME.get(this.infectedBiomeKey);
        }
        catch (Throwable t) {
            this.infectedBiome = null;
        }
        if (this.infectedBiome == null) {
            this.cfg.biomeEnabled = false;
            this.plugin.getLogger().warning("[PalePlugin] \u0411\u0438\u043e\u043c \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + String.valueOf(this.infectedBiomeKey) + ". \u0411\u0438\u043e\u043c\u044b \u043e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u044b.");
        }
    }

    public int apiInfect(Location center, int radius, int maxBlocks) {
        return this.infectAreaExternal(center, radius, maxBlocks);
    }

    public int infectAreaExternal(Location center, int radius, int maxBlocks) {
        World world;
        World world2 = world = center == null ? null : center.getWorld();
        if (world == null) {
            return 0;
        }
        radius = MathUtil.clamp(radius, 1, 64);
        maxBlocks = MathUtil.clamp(maxBlocks, 10, 200000);
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int rSq = radius * radius;
        SourceStore store = this.sources(world);
        WardStore wards = this.wards(world);
        BiomeStore bs = this.biomes(world);
        int infected = 0;
        int processed = 0;
        block0: for (int x = -radius; x <= radius; ++x) {
            int xx = x * x;
            for (int y = -radius; y <= radius; ++y) {
                int xxyy = xx + y * y;
                for (int z = -radius; z <= radius; ++z) {
                    if (xxyy + z * z > rSq) continue;
                    if (processed++ >= maxBlocks) break block0;
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (wards.isProtected(b.getX(), b.getY(), b.getZ(), this.cfg.wardRadius) || !this.tryInfect(b, store, bs)) continue;
                    ++infected;
                }
            }
        }
        return infected;
    }

    public int infectAreaWand(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return 0;
        }
        int radius = this.cfg.infectWandRadius;
        int maxBlocks = this.cfg.infectWandMaxBlocksPerUse;
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int rSq = radius * radius;
        SourceStore store = this.sources(world);
        WardStore wards = this.wards(world);
        BiomeStore bs = this.biomes(world);
        int infected = 0;
        int processed = 0;
        block0: for (int x = -radius; x <= radius; ++x) {
            int xx = x * x;
            for (int y = -radius; y <= radius; ++y) {
                int xxyy = xx + y * y;
                for (int z = -radius; z <= radius; ++z) {
                    if (xxyy + z * z > rSq) continue;
                    if (processed++ >= maxBlocks) break block0;
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (wards.isProtected(b.getX(), b.getY(), b.getZ(), this.cfg.wardRadius) || !this.tryInfect(b, store, bs)) continue;
                    ++infected;
                    if (store.size() >= this.cfg.maxSourcesPerWorld || this.cfg.infectWandBonusSourceChanceDivider <= 0 || this.random.nextInt(this.cfg.infectWandBonusSourceChanceDivider) != 0) continue;
                    store.add(b.getX(), b.getY(), b.getZ());
                }
            }
        }
        return infected;
    }

    public int cleanse(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            return 0;
        }
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int rSq = radius * radius;
        SourceStore store = this.sources(world);
        BiomeStore bs = this.biomeStoreByWorld.get(world.getUID());
        int cleaned = 0;
        HashSet<Long> touchedCells = new HashSet<Long>();
        HashSet<Long> touchedChunks = new HashSet<Long>();
        for (int x = -radius; x <= radius; ++x) {
            int xx = x * x;
            for (int y = -radius; y <= radius; ++y) {
                int xxyy = xx + y * y;
                for (int z = -radius; z <= radius; ++z) {
                    if (xxyy + z * z > rSq) continue;
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    Material t = b.getType();
                    if (!this.infectedTypes().contains(t)) continue;
                    touchedChunks.add(SourceStore.packChunk(b.getX() >> 4, b.getZ() >> 4));
                    if (bs != null) {
                        touchedCells.add(bs.cellKeyFromBlock(b.getX(), b.getY(), b.getZ()));
                    }
                    cleaned += this.cleanseSingleBlock(b, store);
                }
            }
        }
        if (this.cfg.biomeEnabled && this.infectedBiome != null && bs != null && !touchedCells.isEmpty()) {
            bs.restoreCellsIfClean(world, touchedCells, this.infectedTypes(), this.infectedBiome);
        }
        Iterator iterator = touchedChunks.iterator();
        while (iterator.hasNext()) {
            long ck = (Long)iterator.next();
            int ccx = (int)(ck >> 32);
            int ccz = (int)ck;
            try {
                world.refreshChunk(ccx, ccz);
            }
            catch (Exception exception) {}
        }
        return cleaned;
    }

    public int cleanseSingleBlock(Block b, SourceStore store) {
        Material t = b.getType();
        if (!this.infectedTypes().contains(t)) {
            return 0;
        }
        String n = t.name();
        if (n.contains("LOG") || n.contains("WOOD") || n.contains("LEAVES")) {
            b.setType(Material.AIR, false);
        } else if (n.contains("MOSS")) {
            if (n.contains("CARPET")) {
                b.setType(Material.AIR, false);
            } else {
                b.setType(Material.DIRT, false);
            }
        } else {
            b.setType(Material.AIR, false);
        }
        store.remove(b.getX(), b.getY(), b.getZ());
        return 1;
    }

    public boolean tryInfect(Block b, SourceStore store, BiomeStore bs) {
        Material t = b.getType();
        if (t.isAir()) {
            return false;
        }
        if (this.infectedTypes().contains(t)) {
            return false;
        }
        Material newType = null;
        String n = t.name();
        if (n.contains("LOG")) {
            newType = this.mats.PALE_LOG;
        } else if (n.contains("WOOD")) {
            newType = this.mats.PALE_WOOD;
        } else if (n.contains("LEAVES")) {
            newType = this.mats.PALE_LEAVES;
        } else if ((t == Material.GRASS_BLOCK || t == Material.DIRT || t == Material.MOSS_BLOCK) && this.random.nextInt(10) == 0) {
            newType = this.mats.PALE_MOSS_BLOCK;
        }
        if (newType == null) {
            return false;
        }
        b.setType(newType, false);
        this.applyInfectedBiome(b, bs);
        if (newType == this.mats.PALE_LOG) {
            this.infectLogUpwards(b, bs);
        }
        if (store.size() < this.cfg.maxSourcesPerWorld) {
            boolean makeSource;
            boolean isWood = newType == this.mats.PALE_LOG || newType == this.mats.PALE_WOOD;
            boolean bl = makeSource = isWood && this.random.nextInt(this.cfg.logSourceChanceDivider) == 0 || !isWood && this.random.nextInt(this.cfg.sourceChanceDivider) == 0;
            if (makeSource) {
                store.add(b.getX(), b.getY(), b.getZ());
            }
        }
        return true;
    }

    private void infectLogUpwards(Block base, BiomeStore bs) {
        String n;
        Material t;
        Block cur = base.getRelative(BlockFace.UP);
        for (int i = 0; i < 12 && !(t = cur.getType()).isAir() && (n = t.name()).contains("LOG") && !this.infectedTypes().contains(t); ++i) {
            cur.setType(this.mats.PALE_LOG, false);
            this.applyInfectedBiome(cur, bs);
            cur = cur.getRelative(BlockFace.UP);
        }
    }

    private void applyInfectedBiome(Block b, BiomeStore bs) {
        if (!this.cfg.biomeEnabled || this.infectedBiome == null) {
            return;
        }
        try {
            Biome cur = b.getBiome();
            if (!cur.equals((Object)this.infectedBiome)) {
                NamespacedKey oldKey;
                long cellKey = bs.cellKeyFromBlock(b.getX(), b.getY(), b.getZ());
                if (!bs.contains(cellKey) && (oldKey = BiomeUtil.biomeKeyOf(cur)) != null && !oldKey.equals((Object)this.infectedBiomeKey)) {
                    bs.put(cellKey, oldKey);
                }
                b.setBiome(this.infectedBiome);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void indexChunkSurface(World world, int chunkX, int chunkZ) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        SourceStore store = this.sources(world);
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int y;
                int wx = baseX + lx;
                int wz = baseZ + lz;
                int topY = world.getHighestBlockYAt(wx, wz);
                for (int dy = 0; dy < this.cfg.indexDepth && (y = topY - dy) >= world.getMinHeight(); ++dy) {
                    Block b = world.getBlockAt(wx, y, wz);
                    if (!this.infectedTypes().contains(b.getType())) continue;
                    store.add(wx, y, wz);
                }
            }
        }
    }

    public boolean trySpreadFromSource(Block source) {
        SourceStore store = this.sources(source.getWorld());
        WardStore wards = this.wards(source.getWorld());
        BiomeStore bs = this.biomes(source.getWorld());
        int jumpRadius = 4;
        int jumpUpDown = 2;
        int probeDown = 6;
        int tries = this.cfg.spreadTriesPerAttempt;
        for (int attempt = 0; attempt < tries; ++attempt) {
            int i;
            int dx = this.random.nextInt(9) - 4;
            int dz = this.random.nextInt(9) - 4;
            int dy = this.random.nextInt(5) - 2;
            if (dx == 0 && dy == 0 && dz == 0) continue;
            Block target = source.getRelative(dx, dy, dz);
            if (target.getType().isAir()) {
                Block down;
                Block t = target;
                for (i = 0; i < 6 && (down = t.getRelative(BlockFace.DOWN)).getY() > down.getWorld().getMinHeight() && (t = down).getType().isAir(); ++i) {
                }
                target = t;
            }
            if (wards.isProtected(target.getX(), target.getY(), target.getZ(), this.cfg.wardRadius)) continue;
            int bonus = this.hasInfectedNear(target) ? 2 : 0;
            for (i = 0; i < 1 + bonus; ++i) {
                if (!this.tryInfect(target, store, bs)) continue;
                if (this.random.nextInt(3) == 0) {
                    source.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, target.getLocation().add(0.5, 0.5, 0.5), 4, 0.3, 0.3, 0.3, 0.01);
                }
                return true;
            }
        }
        return false;
    }

    public int getChunkStage(World w, int chunkX, int chunkZ) {
        if (!this.cfg.stagesEnabled) {
            return 0;
        }
        int c = this.sources(w).getChunkCount(chunkX, chunkZ);
        int stage = 0;
        if (c >= this.cfg.stage1Sources) {
            stage = 1;
        }
        if (c >= this.cfg.stage2Sources) {
            stage = 2;
        }
        if (c >= this.cfg.stage3Sources) {
            stage = 3;
        }
        if (c >= this.cfg.stage4Sources) {
            stage = 4;
        }
        if (c >= this.cfg.stage5Sources) {
            stage = 5;
        }
        return stage;
    }

    public void sendMap(Player p, int radiusChunks) {
        int dz;
        World w = p.getWorld();
        SourceStore store = this.sources(w);
        int cx = p.getLocation().getBlockX() >> 4;
        int cz = p.getLocation().getBlockZ() >> 4;
        int stageHere = this.getChunkStage(w, cx, cz);
        int max = 0;
        for (dz = -radiusChunks; dz <= radiusChunks; ++dz) {
            for (int dx = -radiusChunks; dx <= radiusChunks; ++dx) {
                max = Math.max(max, store.getChunkCount(cx + dx, cz + dz));
            }
        }
        p.sendMessage(String.valueOf(ChatColor.GRAY) + "[Pale] \u041a\u0430\u0440\u0442\u0430 \u0437\u0430\u0440\u0430\u0436\u0435\u043d\u0438\u044f (0..9), r=" + radiusChunks + " \u0447\u0430\u043d\u043a\u043e\u0432, max=" + max + ", stage=" + stageHere);
        for (dz = -radiusChunks; dz <= radiusChunks; ++dz) {
            StringBuilder line = new StringBuilder();
            for (int dx = -radiusChunks; dx <= radiusChunks; ++dx) {
                int count = store.getChunkCount(cx + dx, cz + dz);
                if (dx == 0 && dz == 0) {
                    line.append(ChatColor.WHITE).append('X');
                    continue;
                }
                int level = max <= 0 ? 0 : (int)Math.round((double)count / (double)max * 9.0);
                level = MathUtil.clamp(level, 0, 9);
                line.append(this.mapColorForLevel(level)).append((char)(48 + level));
            }
            line.append(ChatColor.RESET);
            p.sendMessage(line.toString());
        }
        p.sendMessage(String.valueOf(ChatColor.DARK_GRAY) + "\u041b\u0435\u0433\u0435\u043d\u0434\u0430: 0=\u043d\u0435\u0442, 9=\u043c\u0430\u043a\u0441\u0438\u043c\u0443\u043c, X=\u0442\u044b.");
    }

    private String rgb(String hex) {
        try {
            return net.md_5.bungee.api.ChatColor.of((String)hex).toString();
        }
        catch (Throwable t) {
            return ChatColor.GREEN.toString();
        }
    }

    private String mapColorForLevel(int level) {
        if (level <= 0) {
            return this.rgb("#1a1a1a");
        }
        if (level <= 2) {
            return this.rgb("#145214");
        }
        if (level <= 4) {
            return this.rgb("#1aff1a");
        }
        if (level <= 6) {
            return this.rgb("#39FF14");
        }
        if (level <= 8) {
            return this.rgb("#66ff00");
        }
        return this.rgb("#ccff00");
    }

    public boolean hasInfectedNear(Block b) {
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    if (dx == 0 && dy == 0 && dz == 0 || !this.infectedTypes().contains(b.getRelative(dx, dy, dz).getType())) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public int purgeChunkSurface(World world, int chunkX, int chunkZ, int depth) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int minY = world.getMinHeight();
        int cleaned = 0;
        SourceStore store = this.sources(world);
        BiomeStore bs = this.biomeStoreByWorld.get(world.getUID());
        HashSet<Long> touchedCells = new HashSet<Long>();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int y;
                int wx = baseX + lx;
                int wz = baseZ + lz;
                int topY = world.getHighestBlockYAt(wx, wz);
                for (int dy = 0; dy < depth && (y = topY - dy) >= minY; ++dy) {
                    Block b = world.getBlockAt(wx, y, wz);
                    if (!this.infectedTypes().contains(b.getType())) continue;
                    if (bs != null) {
                        touchedCells.add(bs.cellKeyFromBlock(b.getX(), b.getY(), b.getZ()));
                    }
                    cleaned += this.cleanseSingleBlock(b, store);
                }
            }
        }
        if (this.cfg.biomeEnabled && this.infectedBiome != null && bs != null && !touchedCells.isEmpty()) {
            bs.restoreCellsIfClean(world, touchedCells, this.infectedTypes(), this.infectedBiome);
        }
        try {
            world.refreshChunk(chunkX, chunkZ);
        }
        catch (Exception exception) {
            // empty catch block
        }
        return cleaned;
    }
}

