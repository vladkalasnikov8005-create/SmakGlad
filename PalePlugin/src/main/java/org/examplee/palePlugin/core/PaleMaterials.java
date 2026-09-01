package org.examplee.palePlugin.core;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaleMaterials {
    private final JavaPlugin plugin;
    public Material PALE_LOG;
    public Material PALE_WOOD;
    public Material PALE_LEAVES;
    public Material PALE_MOSS_BLOCK;
    public Material PALE_MOSS_CARPET;
    private final Set<Material> infectedTypes = new HashSet<Material>();

    public PaleMaterials(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Set<Material> getInfectedTypes() {
        return this.infectedTypes;
    }

    public boolean resolveOrDisable() {
        this.PALE_LOG = this.resolve("PALE_OAK_LOG");
        this.PALE_WOOD = this.resolve("PALE_OAK_WOOD");
        this.PALE_LEAVES = this.resolve("PALE_OAK_LEAVES");
        this.PALE_MOSS_BLOCK = this.resolve("PALE_MOSS_BLOCK");
        this.PALE_MOSS_CARPET = this.resolve("PALE_MOSS_CARPET");
        if (this.PALE_LOG == null || this.PALE_LEAVES == null) {
            this.plugin.getLogger().severe("[PalePlugin] \u041d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u044b \u043c\u0430\u0442\u0435\u0440\u0438\u0430\u043b\u044b PALE_OAK_* \u0432 \u0432\u0430\u0448\u0435\u043c \u044f\u0434\u0440\u0435.");
            this.plugin.getServer().getPluginManager().disablePlugin((Plugin)this.plugin);
            return false;
        }
        if (this.PALE_WOOD == null) {
            this.PALE_WOOD = this.PALE_LOG;
        }
        if (this.PALE_MOSS_BLOCK == null) {
            this.PALE_MOSS_BLOCK = Material.MOSS_BLOCK;
        }
        this.infectedTypes.clear();
        this.infectedTypes.add(this.PALE_LOG);
        this.infectedTypes.add(this.PALE_WOOD);
        this.infectedTypes.add(this.PALE_LEAVES);
        this.infectedTypes.add(this.PALE_MOSS_BLOCK);
        if (this.PALE_MOSS_CARPET != null) {
            this.infectedTypes.add(this.PALE_MOSS_CARPET);
        }
        return true;
    }

    private Material resolve(String name) {
        try {
            return Material.valueOf((String)name);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            Material m = Material.matchMaterial((String)name);
            if (m != null) {
                return m;
            }
            return Material.matchMaterial((String)("minecraft:" + name.toLowerCase(Locale.ROOT)));
        }
    }
}

