package org.examplee.palePlugin.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;

public final class BiomeStore {
    private final HashMap<Long, NamespacedKey> oldByCell = new HashMap();

    public int size() {
        return this.oldByCell.size();
    }

    public boolean contains(long key) {
        return this.oldByCell.containsKey(key);
    }

    public void put(long key, NamespacedKey biomeKey) {
        if (biomeKey != null) {
            this.oldByCell.putIfAbsent(key, biomeKey);
        }
    }

    public long cellKeyFromBlock(int x, int y, int z) {
        return this.cellKey(x >> 2, y >> 2, z >> 2);
    }

    public long cellKey(int xCell, int yCell, int zCell) {
        int yEnc = yCell + 2048;
        long xx = (long)xCell & 0x3FFFFFFL;
        long zz = (long)zCell & 0x3FFFFFFL;
        long yy = (long)yEnc & 0xFFFL;
        return xx << 38 | zz << 12 | yy;
    }

    private int unpackX(long key) {
        int x = (int)(key >> 38);
        if ((x & 0x2000000) != 0) {
            x |= 0xFC000000;
        }
        return x;
    }

    private int unpackZ(long key) {
        int z = (int)(key >> 12 & 0x3FFFFFFL);
        if ((z & 0x2000000) != 0) {
            z |= 0xFC000000;
        }
        return z;
    }

    private int unpackY(long key) {
        int yEnc = (int)(key & 0xFFFL);
        return yEnc - 2048;
    }

    public void restoreCellsIfClean(World world, Set<Long> cellKeys, Set<Material> infectedTypes, Biome infectedBiome) {
        for (long key : cellKeys) {
            int zCell;
            int yCell;
            int xCell;
            NamespacedKey oldKey = this.oldByCell.get(key);
            if (oldKey == null || this.cellHasInfected(world, xCell = this.unpackX(key), yCell = this.unpackY(key), zCell = this.unpackZ(key), infectedTypes)) continue;
            Biome oldBiome = (Biome)Registry.BIOME.get(oldKey);
            if (oldBiome == null || infectedBiome != null && oldBiome.equals((Object)infectedBiome)) {
                oldBiome = (Biome)Registry.BIOME.get(NamespacedKey.fromString((String)"minecraft:plains"));
            }
            if (oldBiome == null) continue;
            int bx = (xCell << 2) + 1;
            int by = (yCell << 2) + 1;
            int bz = (zCell << 2) + 1;
            by = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, by));
            try {
                world.getBlockAt(bx, by, bz).setBiome(oldBiome);
                this.oldByCell.remove(key);
            }
            catch (Exception exception) {}
        }
    }

    private boolean cellHasInfected(World world, int xCell, int yCell, int zCell, Set<Material> infectedTypes) {
        int baseX = xCell << 2;
        int baseY = yCell << 2;
        int baseZ = zCell << 2;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        for (int ox = 0; ox < 4; ++ox) {
            int x = baseX + ox;
            for (int oz = 0; oz < 4; ++oz) {
                int z = baseZ + oz;
                for (int oy = 0; oy < 4; ++oy) {
                    int y = baseY + oy;
                    if (y < minY || y > maxY || !infectedTypes.contains(world.getBlockAt(x, y, z).getType())) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> serialize() {
        ArrayList<String> out = new ArrayList<String>(this.oldByCell.size());
        for (Map.Entry<Long, NamespacedKey> e : this.oldByCell.entrySet()) {
            long key = e.getKey();
            NamespacedKey biomeKey = e.getValue();
            int xCell = this.unpackX(key);
            int yCell = this.unpackY(key);
            int zCell = this.unpackZ(key);
            out.add(xCell + "," + yCell + "," + zCell + "," + biomeKey.toString());
        }
        return out;
    }

    public void loadSerializedLine(String s) {
        String[] p = s.split(",");
        if (p.length != 4) {
            return;
        }
        try {
            int xCell = Integer.parseInt(p[0]);
            int yCell = Integer.parseInt(p[1]);
            int zCell = Integer.parseInt(p[2]);
            NamespacedKey key = NamespacedKey.fromString((String)p[3].toLowerCase(Locale.ROOT));
            if (key == null) {
                return;
            }
            this.put(this.cellKey(xCell, yCell, zCell), key);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

