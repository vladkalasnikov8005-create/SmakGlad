package org.examplee.palePlugin.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.examplee.palePlugin.store.SourceStore;

public final class WardStore {
    private final HashMap<Long, ArrayList<Int3>> byChunk = new HashMap();
    private int size = 0;

    public int size() {
        return this.size;
    }

    public void add(int x, int y, int z) {
        long ck = SourceStore.packChunk(x >> 4, z >> 4);
        this.byChunk.computeIfAbsent(ck, k -> new ArrayList()).add(new Int3(x, y, z));
        ++this.size;
    }

    public boolean remove(int x, int y, int z) {
        long ck = SourceStore.packChunk(x >> 4, z >> 4);
        ArrayList<Int3> list = this.byChunk.get(ck);
        if (list == null) {
            return false;
        }
        for (int i = 0; i < list.size(); ++i) {
            Int3 p = list.get(i);
            if (p.x != x || p.y != y || p.z != z) continue;
            int last = list.size() - 1;
            list.set(i, list.get(last));
            list.remove(last);
            --this.size;
            if (list.isEmpty()) {
                this.byChunk.remove(ck);
            }
            return true;
        }
        return false;
    }

    public boolean isProtected(int x, int y, int z, int radius) {
        int rSq = radius * radius;
        int cRad = (radius >> 4) + 1;
        int cx = x >> 4;
        int cz = z >> 4;
        for (int dx = -cRad; dx <= cRad; ++dx) {
            for (int dz = -cRad; dz <= cRad; ++dz) {
                long ck = SourceStore.packChunk(cx + dx, cz + dz);
                ArrayList<Int3> list = this.byChunk.get(ck);
                if (list == null) continue;
                for (Int3 p : list) {
                    int ox = p.x - x;
                    int oy = p.y - y;
                    int oz = p.z - z;
                    if (ox * ox + oy * oy + oz * oz > rSq) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> serialize() {
        ArrayList<String> out = new ArrayList<String>(this.size);
        for (ArrayList<Int3> list : this.byChunk.values()) {
            for (Int3 p : list) {
                out.add(p.x + "," + p.y + "," + p.z);
            }
        }
        return out;
    }

    private static final class Int3 {
        final int x;
        final int y;
        final int z;

        Int3(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}

