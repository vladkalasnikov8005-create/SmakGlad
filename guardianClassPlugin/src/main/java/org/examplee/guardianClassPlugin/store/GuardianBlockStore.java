package org.examplee.guardianClassPlugin.store;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GuardianBlockStore {

    private final Map<UUID, HashSet<Long>> lifeStonesByWorld = new HashMap<>();
    private final Map<UUID, HashSet<Long>> flowersByWorld = new HashMap<>();

    public boolean isLifeStone(World w, int x, int y, int z) {
        return lifeStonesByWorld.getOrDefault(w.getUID(), new HashSet<>()).contains(pack(x, y, z));
    }

    public boolean isFlower(World w, int x, int y, int z) {
        return flowersByWorld.getOrDefault(w.getUID(), new HashSet<>()).contains(pack(x, y, z));
    }

    public void addLifeStone(World w, int x, int y, int z) {
        lifeStonesByWorld.computeIfAbsent(w.getUID(), k -> new HashSet<>()).add(pack(x, y, z));
    }

    public void addFlower(World w, int x, int y, int z) {
        flowersByWorld.computeIfAbsent(w.getUID(), k -> new HashSet<>()).add(pack(x, y, z));
    }

    public boolean removeLifeStone(World w, int x, int y, int z) {
        return lifeStonesByWorld.getOrDefault(w.getUID(), new HashSet<>()).remove(pack(x, y, z));
    }

    public boolean removeFlower(World w, int x, int y, int z) {
        return flowersByWorld.getOrDefault(w.getUID(), new HashSet<>()).remove(pack(x, y, z));
    }

    public List<String> serializeLife(UUID worldId) {
        HashSet<Long> set = lifeStonesByWorld.getOrDefault(worldId, new HashSet<>());
        ArrayList<String> out = new ArrayList<>(set.size());
        for (long k : set) out.add(unpackToString(k));
        return out;
    }

    public List<String> serializeFlowers(UUID worldId) {
        HashSet<Long> set = flowersByWorld.getOrDefault(worldId, new HashSet<>());
        ArrayList<String> out = new ArrayList<>(set.size());
        for (long k : set) out.add(unpackToString(k));
        return out;
    }

    public void clearWorld(UUID worldId) {
        lifeStonesByWorld.remove(worldId);
        flowersByWorld.remove(worldId);
    }

    public void loadLife(UUID worldId, List<String> lines) {
        HashSet<Long> set = lifeStonesByWorld.computeIfAbsent(worldId, k -> new HashSet<>());
        for (String s : lines) {
            Long k = parseLine(s);
            if (k != null) set.add(k);
        }
    }

    public void loadFlowers(UUID worldId, List<String> lines) {
        HashSet<Long> set = flowersByWorld.computeIfAbsent(worldId, k -> new HashSet<>());
        for (String s : lines) {
            Long k = parseLine(s);
            if (k != null) set.add(k);
        }
    }

    public boolean hasLifeStoneNearby(Location c, int r, int yR, int step) {
        return hasNearby(c, r, yR, step, true);
    }

    public boolean hasFlowerNearby(Location c, int r, int yR, int step) {
        return hasNearby(c, r, yR, step, false);
    }

    private boolean hasNearby(Location c, int r, int yR, int step, boolean life) {
        World w = c.getWorld();
        if (w == null) return false;

        HashSet<Long> set = (life ? lifeStonesByWorld : flowersByWorld).get(w.getUID());
        if (set == null || set.isEmpty()) return false;

        int cx = c.getBlockX();
        int cy = c.getBlockY();
        int cz = c.getBlockZ();

        // Fixed check: iterate saved custom blocks directly so effects do not depend on movement grid.
        for (long key : set) {
            int x = unpackX(key);
            int y = unpackY(key);
            int z = unpackZ(key);

            if (Math.abs(x - cx) <= r && Math.abs(y - cy) <= yR && Math.abs(z - cz) <= r) {
                return true;
            }
        }
        return false;
    }

    private Long parseLine(String s) {
        if (s == null) return null;
        String[] p = s.split(",");
        if (p.length != 3) return null;
        try {
            int x = Integer.parseInt(p[0]);
            int y = Integer.parseInt(p[1]);
            int z = Integer.parseInt(p[2]);
            return pack(x, y, z);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String unpackToString(long key) {
        int x = unpackX(key);
        int y = unpackY(key);
        int z = unpackZ(key);
        return x + "," + y + "," + z;
    }

    private long pack(int x, int y, int z) {
        int yEnc = y + 2048;
        long xx = ((long) x) & 0x3FFFFFFL;
        long zz = ((long) z) & 0x3FFFFFFL;
        long yy = ((long) yEnc) & 0xFFFL;
        return (xx << 38) | (zz << 12) | yy;
    }

    private int unpackX(long key) { int x = (int) (key >> 38); if ((x & (1 << 25)) != 0) x |= ~0x3FFFFFF; return x; }
    private int unpackZ(long key) { int z = (int) ((key >> 12) & 0x3FFFFFFL); if ((z & (1 << 25)) != 0) z |= ~0x3FFFFFF; return z; }
    private int unpackY(long key) { int yEnc = (int) (key & 0xFFFL); return yEnc - 2048; }
}
