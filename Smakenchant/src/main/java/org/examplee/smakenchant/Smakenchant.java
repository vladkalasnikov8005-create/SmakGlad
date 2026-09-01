package org.examplee.smakenchant;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings({"deprecation", "removal", "unused"})
public class Smakenchant extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private NamespacedKey keyMultiJump;

    private boolean mjEnabled;
    private double mjChance1, mjChance2;
    private float mjSndVol, mjSndPitch;
    private String mjSound;
    private boolean mjDisableElytra;
    private double mjPowerUp;
    private double mjForward;
    private final int mjMaxLevel = 2;

    // ============== КРАСИВЫЙ ГРАДИЕНТ ==============
    private static final int GRAD_R1 = 201, GRAD_G1 = 168, GRAD_B1 = 232;
    private static final int GRAD_R2 = 184, GRAD_G2 = 176, GRAD_B2 = 192;

    private String gradient(String text) {
        if (text == null || text.isEmpty()) return "";
        text = ChatColor.stripColor(text);
        int len = text.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            if (ch == '§') { i++; continue; }
            double t = len <= 1 ? 0 : (double) i / (len - 1);
            int r = (int) Math.round(GRAD_R1 + (GRAD_R2 - GRAD_R1) * t);
            int g = (int) Math.round(GRAD_G1 + (GRAD_G2 - GRAD_G1) * t);
            int b = (int) Math.round(GRAD_B1 + (GRAD_B2 - GRAD_B1) * t);
            String hex = String.format("%02x%02x%02x", r, g, b);
            sb.append("§x");
            for (char c : hex.toCharArray()) sb.append('§').append(c);
            sb.append(ch);
        }
        return sb.toString();
    }

    private String mjText(int level) {
        String[] roman = {"0", "I", "II", "III"};
        return "Многоуровневый прыжок " + (level >= 0 && level <= mjMaxLevel ? roman[level] : level);
    }

    private String mjLoreLine(int level) { return gradient(mjText(level)); }

    private static class OverrideCfg {
        boolean enabled;
        int vanillaMax;
        int newMax;
        int costPerExtra;
    }

    private final Map<Enchantment, OverrideCfg> overrides = new HashMap<>();

    // ============== СОСТОЯНИЕ ПРЫЖКОВ ==============
    // Сколько доп прыжков ОСТАЛОСЬ в текущем воздухе
    private final Map<UUID, Integer> airJumpsLeft = new HashMap<>();
    // Флаг "мы сейчас обрабатываем двойной прыжок" — защита от рекурсии
    private final Set<UUID> doingJump = new HashSet<>();
    // Сколько тиков подряд игрок в воздухе (для детекта отрыва)
    private final Map<UUID, Integer> airTime = new HashMap<>();

    @Override
    public void onEnable() {
        keyMultiJump = new NamespacedKey(this, "multi_jump");
        saveDefaultConfig();
        reloadCfg();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("smakenchant").setExecutor(this);
        getCommand("smakenchant").setTabCompleter(this);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                p.setAllowFlight(false);
                p.setFlying(false);
            }
            airTime.put(p.getUniqueId(), p.isOnGround() ? 0 : 20);
            airJumpsLeft.put(p.getUniqueId(), 0);
        }
        getLogger().info("Smakenchant v1.4 включен (26.2)");
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                    p.setAllowFlight(false);
                    p.setFlying(false);
                }
            } catch (Throwable ignored) {}
        }
        airJumpsLeft.clear();
        doingJump.clear();
        airTime.clear();
        Bukkit.getScheduler().cancelTasks(this);
    }

    // ============== ФИКС БАГНУТЫХ КНИГ ==============
    private boolean fixBugEnchants(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (stack.getType() != Material.ENCHANTED_BOOK) return false;
        if (!stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta esm)) return false;
        Map<Enchantment, Integer> bad = meta.getEnchants();
        if (bad.isEmpty()) return false;
        boolean changed = false;
        for (Map.Entry<Enchantment, Integer> en : bad.entrySet()) {
            Enchantment ench = en.getKey();
            int lvl = en.getValue();
            int existing = esm.getStoredEnchants().getOrDefault(ench, 0);
            if (existing < lvl) {
                try { esm.addStoredEnchant(ench, lvl, true); changed = true; } catch (Throwable ignored) {}
            }
            meta.removeEnchant(ench);
            changed = true;
        }
        if (changed) stack.setItemMeta(esm);
        return changed;
    }

    private void fixPlayerInvOnce(Player p) {
        try {
            boolean ch = false;
            for (ItemStack it : p.getInventory().getContents()) ch |= fixBugEnchants(it);
            for (ItemStack it : p.getInventory().getArmorContents()) ch |= fixBugEnchants(it);
            ch |= fixBugEnchants(p.getInventory().getItemInOffHand());
            ch |= fixBugEnchants(p.getItemOnCursor());
            if (p.getOpenInventory().getTopInventory() != null)
                for (ItemStack it : p.getOpenInventory().getTopInventory().getContents()) ch |= fixBugEnchants(it);
            if (ch) p.updateInventory();
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        airTime.put(p.getUniqueId(), p.isOnGround() ? 0 : 20);
        airJumpsLeft.put(p.getUniqueId(), 0);
        if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) p.setAllowFlight(false);
        Bukkit.getScheduler().runTaskLater(this, () -> fixPlayerInvOnce(p), 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInvOpen(org.bukkit.event.inventory.InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p)
            Bukkit.getScheduler().runTaskLater(this, () -> fixPlayerInvOnce(p), 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p)
            Bukkit.getScheduler().runTaskLater(this, () -> fixPlayerInvOnce(p), 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        airJumpsLeft.remove(id);
        doingJump.remove(id);
        airTime.remove(id);
    }

    @EventHandler
    public void onTp(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        UUID uid = p.getUniqueId();
        airJumpsLeft.put(uid, 0);
        doingJump.remove(uid);
        airTime.put(uid, 0);
        if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) p.setAllowFlight(false);
    }

    // ============== КОНФИГ ==============
    private void reloadCfg() {
        reloadConfig();
        mjEnabled = getConfig().getBoolean("multi-jump.enabled", true);
        mjChance1 = getConfig().getDouble("multi-jump.chance-level-1", 0.08);
        mjChance2 = getConfig().getDouble("multi-jump.chance-level-2", 0.03);
        mjSound = getConfig().getString("multi-jump.sound", "ENTITY_BREEZE_JUMP");
        mjSndVol = (float) getConfig().getDouble("multi-jump.sound-volume", 0.6);
        mjSndPitch = (float) getConfig().getDouble("multi-jump.sound-pitch", 1.4);
        mjDisableElytra = getConfig().getBoolean("multi-jump.disable-when-wearing-elytra", true);
        // Сила подбрасывания вверх (ванильный прыжок ≈ 0.42)
        mjPowerUp = getConfig().getDouble("multi-jump.jump-power", 0.5);
        // Дополнительный импульс ВПЕРЁД по взгляду (0 = не добавляем, сохраняем скорость бега)
        mjForward = getConfig().getDouble("multi-jump.forward-boost", 0.0);

        overrides.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("overrides");
        if (sec != null) {
            for (String keyName : sec.getKeys(false)) {
                Enchantment ench = resolveEnch(keyName);
                if (ench == null) { getLogger().warning("Не найден энчант: " + keyName); continue; }
                OverrideCfg c = new OverrideCfg();
                c.enabled = sec.getBoolean(keyName + ".enabled", false);
                c.vanillaMax = sec.getInt(keyName + ".vanilla-max", ench.getMaxLevel());
                c.newMax = sec.getInt(keyName + ".new-max", ench.getMaxLevel());
                c.costPerExtra = sec.getInt(keyName + ".cost-per-extra-level", 0);
                if (c.newMax < c.vanillaMax) c.newMax = c.vanillaMax;
                overrides.put(ench, c);
            }
        }
        getLogger().info("Загружено " + overrides.size() + " оверрайдов энчантов.");
    }

    private Enchantment resolveEnch(String name) {
        try {
            Enchantment e = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
            if (e != null) return e;
        } catch (Throwable ignored) {}
        try { return Enchantment.getByName(name.toUpperCase(Locale.ROOT)); }
        catch (Throwable ignored) { return null; }
    }

    private void playSnd(World w, Location loc, String name, float vol, float pitch) {
        Sound s = null;
        try { s = Sound.valueOf(name.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ignored) {}
        if (s == null) {
            try { s = Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT).replace('_', '.'))); }
            catch (Throwable ignored) {}
        }
        w.playSound(loc, s != null ? s : Sound.ENTITY_BREEZE_JUMP, vol, pitch);
    }

    // ============== ПРЫЖОК: ОСНОВНАЯ ЛОГИКА ==============

    private int getMjLevel(ItemStack boots) {
        if (boots == null || !boots.hasItemMeta()) return 0;
        PersistentDataContainer pdc = boots.getItemMeta().getPersistentDataContainer();
        Integer lvl = pdc.get(keyMultiJump, PersistentDataType.INTEGER);
        return lvl == null ? 0 : Math.max(0, Math.min(mjMaxLevel, lvl));
    }

    private boolean hasMjBoots(Player p) {
        if (!mjEnabled) return false;
        GameMode gm = p.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return false;
        if (p.isGliding()) return false;
        if (mjDisableElytra) {
            ItemStack chest = p.getInventory().getChestplate();
            if (chest != null && chest.getType() == Material.ELYTRA) return false;
        }
        return getMjLevel(p.getInventory().getBoots()) > 0;
    }

    private boolean isMjBook(ItemStack s) {
        return s != null && s.getType() == Material.ENCHANTED_BOOK && s.hasItemMeta()
                && s.getItemMeta().getPersistentDataContainer().has(keyMultiJump, PersistentDataType.INTEGER);
    }

    private int getMjBookLvl(ItemStack book) {
        if (!isMjBook(book)) return 0;
        Integer lvl = book.getItemMeta().getPersistentDataContainer().get(keyMultiJump, PersistentDataType.INTEGER);
        return lvl == null ? 0 : lvl;
    }

    private ItemStack setMjOnItem(ItemStack stack, int level) {
        if (stack == null) return null;
        ItemMeta m = stack.getItemMeta();
        if (m == null) return stack;
        List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
        lore.removeIf(line -> ChatColor.stripColor(line).startsWith("Многоуровневый прыжок"));
        if (level > 0) {
            m.getPersistentDataContainer().set(keyMultiJump, PersistentDataType.INTEGER, level);
            lore.add(0, mjLoreLine(level));
            try { m.setEnchantmentGlintOverride(true); } catch (Throwable ignored) {}
        } else {
            m.getPersistentDataContainer().remove(keyMultiJump);
            if (m.getEnchants().isEmpty()) {
                try { m.setEnchantmentGlintOverride(null); } catch (Throwable ignored) {}
            }
        }
        m.setLore(lore.isEmpty() ? null : lore);
        stack.setItemMeta(m);
        return stack;
    }

    /**
     * Отслеживаем приземление и прыжок с земли по onGround и Y-скорости.
     * Также держим allowFlight пока в воздухе и есть прыжки.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID uid = p.getUniqueId();
        GameMode gm = p.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;

        boolean mj = hasMjBoots(p);
        boolean onGround = p.isOnGround();
        boolean wasOnGround = airTime.getOrDefault(uid, 0) == 0;

        if (onGround) {
            // === ПРИЗЕМЛИЛИСЬ ===
            airTime.put(uid, 0);
            airJumpsLeft.put(uid, 0);
            if (p.getAllowFlight()) p.setAllowFlight(false);
            doingJump.remove(uid);
        } else {
            // === В ВОЗДУХЕ ===
            int t = airTime.getOrDefault(uid, 0) + 1;
            airTime.put(uid, t);

            if (wasOnGround && t == 1) {
                // === ТОЛЬКО ЧТО ОТОРВАЛИСЬ ОТ ЗЕМЛИ = прыжок с земли ===
                // Это срабатывает в первом же тике после прыжка, мгновенно.
                if (mj) {
                    int maxJumps = getMjLevel(p.getInventory().getBoots());
                    airJumpsLeft.put(uid, maxJumps);
                    p.setAllowFlight(true);
                }
            } else if (mj) {
                // Уже в воздухе — держим allowFlight если прыжки ещё есть
                int left = airJumpsLeft.getOrDefault(uid, 0);
                if (left > 0) {
                    if (!p.getAllowFlight() && !p.isFlying()) p.setAllowFlight(true);
                } else {
                    if (p.getAllowFlight()) p.setAllowFlight(false);
                }
            } else {
                if (p.getAllowFlight()) p.setAllowFlight(false);
                airJumpsLeft.put(uid, 0);
                doingJump.remove(uid);
            }
        }
    }

    /**
     * Двойной прыжок: игрок нажал пробел в воздухе (событие "попытка начать лететь").
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAirJump(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        GameMode gm = p.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;
        if (!e.isFlying()) return; // Игрок ВЫКЛЮЧАЕТ полёт — не наше
        if (p.isOnGround()) return; // На земле это не двойной прыжок
        if (!hasMjBoots(p)) return;

        // Отменяем попытку включить режим полёта
        e.setCancelled(true);

        UUID uid = p.getUniqueId();
        if (doingJump.contains(uid)) return; // Уже в обработке — антирекурсия

        int left = airJumpsLeft.getOrDefault(uid, 0);
        if (left <= 0) {
            // Прыжки кончились — отключаем полёт до приземления
            p.setAllowFlight(false);
            return;
        }

        doingJump.add(uid);
        airJumpsLeft.put(uid, left - 1);

        // Применяем импульс: ТОЛЬКО вверх, горизонтальную скорость НЕ МЕНЯЕМ —
        // это убирает замедление при беге!
        Vector vel = p.getVelocity();
        vel.setY(mjPowerUp);
        // Если настроен форвард-буст — добавляем немного вперёд по взгляду
        if (mjForward > 0.01) {
            Vector dir = p.getLocation().getDirection().setY(0).normalize();
            vel.add(dir.multiply(mjForward));
        }
        p.setVelocity(vel);

        // Звук и эффекты
        playSnd(p.getWorld(), p.getLocation(), mjSound, mjSndVol, mjSndPitch);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 0.2, 0),
                10, 0.25, 0.15, 0.25, 0.03);

        // Сбрасываем флаг через 3 тика (не 4 — быстрее реакция на следующий прыжок)
        Bukkit.getScheduler().runTaskLater(this, () -> {
            doingJump.remove(uid);
            // Если прыжки кончились — отключаем allowFlight
            if (airJumpsLeft.getOrDefault(uid, 0) <= 0 && !p.isOnGround()) {
                p.setAllowFlight(false);
            }
        }, 3L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(e.getEntity() instanceof Player p)) return;
        int lvl = getMjLevel(p.getInventory().getBoots());
        if (lvl > 0) e.setDamage(e.getDamage() * (1.0 - (lvl == 2 ? 0.7 : 0.4)));
    }

    // ============== СТОЛ ЗАЧАРОВАНИЙ ==============
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantTable(EnchantItemEvent e) {
        if (!mjEnabled) return;
        ItemStack item = e.getItem();
        if (item == null) return;
        if (!item.getType().name().endsWith("_BOOTS")) return;
        if (getMjLevel(item) > 0) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double roll = rnd.nextDouble();
        int lvl = 0;
        if (roll < mjChance2) lvl = 2;
        else if (roll < mjChance2 + mjChance1) lvl = 1;
        if (lvl > 0) {
            int finalLvl = lvl;
            Bukkit.getScheduler().runTask(this, () -> setMjOnItem(item, finalLvl));
        }
    }

    // ============== ЭНЧАНТЫ: наковальня ==============
    private Map<Enchantment, Integer> enchantsOf(ItemStack stack) {
        if (stack == null) return new HashMap<>();
        if (stack.hasItemMeta() && stack.getItemMeta() instanceof EnchantmentStorageMeta esm)
            return new HashMap<>(esm.getStoredEnchants());
        return new HashMap<>(stack.getEnchantments());
    }

    private void setEnchantOnStack(ItemStack stack, Enchantment ench, int level) {
        if (stack == null || ench == null) return;
        if (stack.getType() == Material.ENCHANTED_BOOK) {
            if (!(stack.getItemMeta() instanceof EnchantmentStorageMeta esm)) return;
            if (esm.hasStoredEnchant(ench)) esm.removeStoredEnchant(ench);
            if (level > 0) { try { esm.addStoredEnchant(ench, level, true); } catch (Throwable ignored) {} }
            stack.setItemMeta(esm);
        } else {
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return;
            if (meta.hasEnchant(ench)) meta.removeEnchant(ench);
            if (level > 0) {
                try { meta.addEnchant(ench, level, true); } catch (Throwable ignored) {}
                try { meta.setEnchantmentGlintOverride(true); } catch (Throwable ignored) {}
            }
            stack.setItemMeta(meta);
        }
    }

    private int combineLvl(int l, int r, int cap) {
        if (l <= 0) return Math.min(r, cap);
        if (r <= 0) return Math.min(l, cap);
        if (l == r) return Math.min(l + 1, cap);
        return Math.min(Math.max(l, r), cap);
    }

    private int applyOverCapEnchants(ItemStack result, ItemStack left, ItemStack right) {
        if (result == null || left == null) return 0;
        Map<Enchantment, Integer> leftEnch = enchantsOf(left);
        Map<Enchantment, Integer> rightEnch = enchantsOf(right == null ? new ItemStack(Material.AIR) : right);
        int addedCost = 0;
        boolean isBook = result.getType() == Material.ENCHANTED_BOOK;

        for (Map.Entry<Enchantment, Integer> re : rightEnch.entrySet()) {
            Enchantment ench = re.getKey();
            int rLvl = re.getValue();
            int lLvl = leftEnch.getOrDefault(ench, 0);
            int vanillaCap = ench.getMaxLevel();
            int cap = vanillaCap;
            OverrideCfg cfg = overrides.get(ench);
            if (cfg != null && cfg.enabled) {
                cap = cfg.newMax;
                if (cfg.vanillaMax > 0) vanillaCap = cfg.vanillaMax;
            }
            int target = combineLvl(lLvl, rLvl, cap);
            if (target <= 0) continue;
            if (!isBook && !ench.canEnchantItem(result)) {
                int cur = enchantsOf(result).getOrDefault(ench, 0);
                if (cur <= 0) continue;
                target = Math.min(cur, cap);
            }
            int current = enchantsOf(result).getOrDefault(ench, 0);
            if (target > current) {
                setEnchantOnStack(result, ench, target);
                if (target > vanillaCap)
                    addedCost += (target - vanillaCap) * (cfg != null && cfg.costPerExtra > 0 ? cfg.costPerExtra : 5);
            }
        }
        for (Map.Entry<Enchantment, Integer> en : enchantsOf(result).entrySet()) {
            Enchantment ench = en.getKey();
            int lvl = en.getValue();
            OverrideCfg cfg = overrides.get(ench);
            if (cfg == null || !cfg.enabled) continue;
            int vanillaCap = ench.getMaxLevel();
            if (cfg.vanillaMax > 0) vanillaCap = cfg.vanillaMax;
            if (lvl > vanillaCap) {
                int capped = Math.min(lvl, cfg.newMax);
                if (capped != lvl) setEnchantOnStack(result, ench, capped);
            }
        }

        int leftMj = getMjLevel(left);
        int rightMj = isMjBook(right) ? getMjBookLvl(right) : getMjLevel(right);
        int newMj = Math.max(leftMj, rightMj);
        if (leftMj > 0 && rightMj > 0)
            newMj = (leftMj == rightMj) ? Math.min(mjMaxLevel, leftMj + 1) : Math.max(leftMj, rightMj);
        boolean canTakeMj = isBook || result.getType().name().endsWith("_BOOTS");
        if (canTakeMj && newMj > 0) {
            setMjOnItem(result, newMj);
            if (rightMj > leftMj && newMj > leftMj) addedCost += 3;
            else if (leftMj > 0 && rightMj > 0 && newMj > leftMj) addedCost += 4;
        }
        return addedCost;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilPrepare(PrepareAnvilEvent e) {
        ItemStack left = e.getInventory().getItem(0);
        if (left == null) return;
        ItemStack right = e.getInventory().getItem(1);
        ItemStack result = e.getResult();
        if (result == null) return;
        ItemStack working = result.clone();
        int extra = applyOverCapEnchants(working, left, right);
        e.setResult(working);
        if (extra > 0) {
            int base = 1;
            try {
                AnvilView view = e.getView();
                int rc = view.getRepairCost();
                if (rc > 0) base = rc;
            } catch (Throwable ignored) {}
            int newCost = Math.min(39, base + extra);
            try { e.getView().setRepairCost(newCost); } catch (Throwable ignored) {}
            try { e.getInventory().setRepairCost(newCost); } catch (Throwable ignored) {}
        }
    }

    // ============== КОМАНДЫ ==============
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("smakenchant.admin")) { sender.sendMessage("§cНет прав"); return true; }
            reloadCfg();
            sender.sendMessage("§a[Smakenchant] Перезагружено. Оверрайдов: " + overrides.size());
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("smakenchant.admin")) { sender.sendMessage("§cНет прав"); return true; }
            if (args.length < 2) { sender.sendMessage("§e/smakenchant give <ench> [lvl] [player]"); return true; }
            String enchName = args[1].toLowerCase();
            int lvl = 1;
            if (args.length >= 3) { try { lvl = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {} }
            Player target;
            if (args.length >= 4) {
                target = Bukkit.getPlayerExact(args[3]);
                if (target == null) { sender.sendMessage("§cИгрок не найден"); return true; }
            } else {
                if (!(sender instanceof Player)) { sender.sendMessage("§cУкажите игрока"); return true; }
                target = (Player) sender;
            }
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            String display;
            if (enchName.equals("multijump") || enchName.equals("multi_jump") || enchName.equals("mj") || enchName.equals("прыжок")) {
                lvl = Math.max(1, Math.min(mjMaxLevel, lvl));
                setMjOnItem(book, lvl);
                display = mjLoreLine(lvl);
            } else {
                Enchantment ench = resolveEnch(enchName);
                if (ench == null) { sender.sendMessage("§cНе найден: " + enchName); return true; }
                OverrideCfg cfg = overrides.get(ench);
                int max = (cfg != null && cfg.enabled) ? cfg.newMax : ench.getMaxLevel();
                lvl = Math.max(1, Math.min(max, lvl));
                setEnchantOnStack(book, ench, lvl);
                display = gradient(ench.getKey().getKey() + " " + lvl);
            }
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(book);
            for (ItemStack o : overflow.values()) target.getWorld().dropItemNaturally(target.getLocation(), o);
            target.sendMessage("§aВы получили: " + display);
            if (sender != target) sender.sendMessage("§aВыдано " + target.getName() + ": " + display);
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("enchant")) {
            if (!sender.hasPermission("smakenchant.admin")) { sender.sendMessage("§cНет прав"); return true; }
            if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько для игроков"); return true; }
            if (args.length < 2) { sender.sendMessage("§e/smakenchant enchant <ench> [lvl]"); return true; }
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) { p.sendMessage("§cВозьмите предмет в руку"); return true; }
            String enchName = args[1].toLowerCase();
            int lvl = 1;
            if (args.length >= 3) { try { lvl = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {} }
            if (enchName.equals("multijump") || enchName.equals("multi_jump") || enchName.equals("mj") || enchName.equals("прыжок")) {
                if (!hand.getType().name().endsWith("_BOOTS")) { p.sendMessage("§cМногоуровневый прыжок только на ботинки"); return true; }
                lvl = Math.max(1, Math.min(mjMaxLevel, lvl));
                setMjOnItem(hand, lvl);
                p.sendMessage("§aНаложено: " + mjLoreLine(lvl));
            } else {
                Enchantment ench = resolveEnch(enchName);
                if (ench == null) { p.sendMessage("§cНе найден: " + enchName); return true; }
                OverrideCfg cfg = overrides.get(ench);
                int max = (cfg != null && cfg.enabled) ? cfg.newMax : ench.getMaxLevel();
                lvl = Math.max(1, Math.min(max, lvl));
                if (hand.getType() != Material.ENCHANTED_BOOK && !ench.canEnchantItem(hand)) {
                    p.sendMessage("§cНе подходит для этого предмета"); return true;
                }
                setEnchantOnStack(hand, ench, lvl);
                p.sendMessage("§aНаложено: " + gradient(ench.getKey().getKey() + " " + lvl));
            }
            return true;
        }
        sender.sendMessage("§e==== Smakenchant v1.4 ====");
        sender.sendMessage("§f/smakenchant reload §7- перезагрузить конфиг");
        sender.sendMessage("§f/smakenchant give <ench> [lvl] [player] §7- выдать книгу");
        sender.sendMessage("§f/smakenchant enchant <ench> [lvl] §7- зачаровать предмет в руке");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("reload", "give", "enchant"))
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("enchant"))) {
            List<String> enchs = new ArrayList<>();
            enchs.add("multijump");
            for (Enchantment e : Registry.ENCHANTMENT) enchs.add(e.getKey().getKey());
            for (String s : enchs)
                if (s.startsWith(args[1].toLowerCase())) out.add(s);
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("enchant"))) {
            for (String n : List.of("1","2","3","4","5","6","7","8","9","10"))
                if (n.startsWith(args[2])) out.add(n);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[3].toLowerCase())) out.add(p.getName());
        }
        return out;
    }
}
