package org.examplee.leperClassPlugin.core;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public final class PaleHook {
    private Object pluginRef;
    private Method apiInfect;

    public void hook() {
        try {
            Plugin pl = Bukkit.getPluginManager().getPlugin("PalePlugin");
            if (pl == null || !pl.isEnabled()) {
                this.pluginRef = null;
                this.apiInfect = null;
                return;
            }
            this.apiInfect = pl.getClass().getMethod("apiInfect", Location.class, Integer.TYPE, Integer.TYPE);
            this.pluginRef = pl;
        }
        catch (Throwable t) {
            this.pluginRef = null;
            this.apiInfect = null;
        }
    }

    public int infect(Location loc, int radius, int maxBlocks) {
        if (this.pluginRef == null || this.apiInfect == null || loc == null) {
            return 0;
        }
        try {
            int n;
            Object res = this.apiInfect.invoke(this.pluginRef, loc, radius, maxBlocks);
            if (res instanceof Integer) {
                Integer i = (Integer)res;
                n = i;
            } else {
                n = 0;
            }
            return n;
        }
        catch (Throwable ignored) {
            return 0;
        }
    }
}

