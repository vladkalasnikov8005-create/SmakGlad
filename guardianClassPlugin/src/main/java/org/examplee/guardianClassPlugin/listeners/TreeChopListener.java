package org.examplee.guardianClassPlugin.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitRunnable;
import org.examplee.guardianClassPlugin.GuardianClassPlugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class TreeChopListener implements Listener {

    private final GuardianClassPlugin plugin;
    private static final int MAX_LOGS_PER_CHOP = 200000;
    private final Set<UUID> chopping = new HashSet<>();

    public TreeChopListener(GuardianClassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) return;
        if (plugin.data.getStage(p) <= 0) return;

        if (!plugin.data.isTreeCapEnabled(p)) return;

        if (!p.isSneaking()) return;

        Block start = e.getBlock();
        if (!isLog(start.getType())) return;

        UUID id = p.getUniqueId();
        if (chopping.contains(id)) {
            p.sendMessage(org.bukkit.ChatColor.RED + "Ты уже рубишь дерево!");
            return;
        }

        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;
        if (!tool.getType().name().contains("AXE")) return;

        List<Block> blocks = collectLogs(start, MAX_LOGS_PER_CHOP);
        if (blocks.size() <= 1) return;

        e.setCancelled(true);
        chopping.add(id);

        int baseDelay = Math.min(20 * 12, 10 + blocks.size() / 6);
        int delay = baseDelay * 10;

        p.sendMessage(org.bukkit.ChatColor.GRAY + "Ты начинаешь срубать дерево... (" + blocks.size() + " брёвен, время x10)");

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!p.isOnline()) return;

                    for (Block b : blocks) {
                        if (b.getType() == Material.AIR) continue;
                        if (isLog(b.getType())) b.breakNaturally(tool);
                    }

                    damageToolExtra(p, tool, Math.max(2, blocks.size() / 4));
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_WOOD_BREAK, 0.9f, 0.9f);
                } finally {
                    chopping.remove(id);
                }
            }
        }.runTaskLater(plugin, delay);
    }

    private List<Block> collectLogs(Block start, int limit) {
        ArrayDeque<Block> q = new ArrayDeque<>();
        HashSet<Long> seen = new HashSet<>();
        ArrayList<Block> out = new ArrayList<>();
        q.add(start);

        while (!q.isEmpty() && out.size() < limit) {
            Block b = q.poll();
            if (b == null) continue;

            long key = pack(b.getX(), b.getY(), b.getZ());
            if (!seen.add(key)) continue;
            if (!isLog(b.getType())) continue;
            out.add(b);

            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        q.add(b.getRelative(dx, dy, dz));
                    }
        }
        return out;
    }

    private boolean isLog(Material m) {
        return Tag.LOGS.isTagged(m) || m.name().endsWith("_LOG") || m.name().endsWith("_WOOD");
    }

    private void damageToolExtra(Player p, ItemStack tool, int extraDamage) {
        if (!(tool.getItemMeta() instanceof Damageable dmg)) return;

        int unbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        int real = 0;
        Random rnd = new Random();

        for (int i = 0; i < extraDamage; i++) {
            if (unbreaking > 0) {
                if (rnd.nextInt(unbreaking + 1) != 0) real++;
            } else {
                real++;
            }
        }

        int newD = dmg.getDamage() + real;
        int max = tool.getType().getMaxDurability();
        if (max > 0 && newD >= max) {
            p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            return;
        }

        dmg.setDamage(newD);
        tool.setItemMeta(dmg);
    }

    private long pack(int x, int y, int z) {
        long xx = ((long) x & 0x3FFFFFFL);
        long zz = ((long) z & 0x3FFFFFFL);
        long yy = ((long) (y + 2048) & 0xFFFL);
        return (xx << 38) | (zz << 12) | yy;
    }
}
