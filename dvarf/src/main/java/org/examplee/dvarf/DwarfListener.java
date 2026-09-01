package org.examplee.dvarf;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Keyed;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.examplee.dvarf.protection.BuildProtection;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class DwarfListener implements Listener {

    private static final int MAX_VEIN_BLOCKS = 2048;

    private final DwarvenCorePlugin plugin;
    private final DwarfService dwarfService;
    private final BuildProtection buildProtection;
    private final Set<String> mineGuard = new HashSet<>();
    private final Set<String> probeGuard = new HashSet<>();
    private final Set<UUID> internalBreakPlayers = new HashSet<>();
    private final Set<UUID> snotProjectiles = new HashSet<>();
    private final Map<UUID, Long> itemWarnCooldown = new HashMap<>();

    public DwarfListener(DwarvenCorePlugin plugin, DwarfService dwarfService, BuildProtection buildProtection) {
        this.plugin = plugin;
        this.dwarfService = dwarfService;
        this.buildProtection = buildProtection;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (dwarfService.isDwarf(event.getPlayer())) {
            dwarfService.applyDwarfAttributes(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!dwarfService.isDwarf(player)) {
            return;
        }

        // Respawn can reset scale/attributes, so reapply on next tick.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> dwarfService.applyDwarfAttributes(player), 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!dwarfService.isDwarf(player) && isRestrictedForNonDwarf(item)) {
            event.setCancelled(true);
            warnCannotUse(player);
            return;
        }

        if (dwarfService.hasItemTag(item, "dwarf_ale")) {
            dwarfService.makeDwarf(player);
        }

        if (!dwarfService.isDwarf(player)) {
            return;
        }

        if (item.getType() == Material.SPIDER_EYE) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.removePotionEffect(PotionEffectType.POISON), 1L);
        }

        if (dwarfService.hasItemTag(item, "mountain_elixir")) {
            dwarfService.setEndurance(player, 20 * dwarfService.getMountainElixirSeconds());
            player.sendMessage(dwarfService.color("&aЭликсир горной выносливости активирован."));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || !dwarfService.isDwarf(player)) {
            return;
        }

        if (event.getFoodLevel() > player.getFoodLevel()) {
            int gain = event.getFoodLevel() - player.getFoodLevel();
            double multiplier = dwarfService.getStage(player) >= 4 ? 1.75D : 1.25D;
            int boosted = (int) Math.ceil(gain * multiplier);
            event.setFoodLevel(Math.min(20, player.getFoodLevel() + boosted));
            player.setSaturation(Math.min(20F, player.getSaturation() + 1.0F));
            return;
        }

        if (event.getFoodLevel() < player.getFoodLevel() && dwarfService.getEnduranceTicks(player) > 0) {
            if (ThreadLocalRandom.current().nextBoolean()) {
                event.setFoodLevel(player.getFoodLevel());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (probeGuard.contains(locKey(event.getBlock()))) {
            return;
        }

        Player player = event.getPlayer();
        if (internalBreakPlayers.contains(player.getUniqueId())) {
            return;
        }

        Block block = event.getBlock();

        if (!dwarfService.isDwarf(player)) {
            return;
        }

        player.setExhaustion(player.getExhaustion() + dwarfService.getMiningExhaustionForStage(player));
        dwarfService.addMinedBlocks(player, 1);

        if (isOre(block.getType())) {
            maybeDoubleDrop(player, block);
            if (dwarfService.shouldApplyLivingOre(player)) {
                triggerLivingOre(player, block);
            }
        }

        if (player.isSneaking() && isOre(block.getType())) {
            veinMine(player, block);
            return;
        }

        if (dwarfService.isDwarfHammer(player.getInventory().getItemInMainHand())) {
            mineHammerArea(player, block);
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getHand() != null) {
            ItemStack used = event.getHand() == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

            if (!dwarfService.isDwarf(player) && isRestrictedForNonDwarf(used)) {
                event.setCancelled(true);
                warnCannotUse(player);
                return;
            }
        }

        if (!dwarfService.isDwarf(player)) {
            return;
        }

        if (event.getClickedBlock() != null
            && event.getClickedBlock().getType() == Material.CHEST
            && player.isSneaking()
            && event.getAction().isRightClick()
            && event.getHand() == EquipmentSlot.HAND
            && player.getInventory().getItemInMainHand().getType() == Material.AIR) {

            BlockState state = event.getClickedBlock().getState();
            if (state instanceof Chest chest) {
                if (chest.getInventory().getHolder() instanceof DoubleChest) {
                    player.sendMessage(dwarfService.color("&cДвойной сундук переносить нельзя. Разделите его на обычные."));
                    event.setCancelled(true);
                    return;
                }

                if (!buildProtection.canBuild(player, chest.getLocation()) || !canBreakByRegionPlugins(player, event.getClickedBlock())) {
                    player.sendMessage(dwarfService.color("&cЗдесь нельзя переносить сундук."));
                    event.setCancelled(true);
                    return;
                }

                ItemStack[] savedContents = chest.getBlockInventory().getContents().clone();

                ItemStack portableChest = new ItemStack(Material.CHEST);
                BlockStateMeta meta = (BlockStateMeta) portableChest.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(dwarfService.color("&6Переносной сундук"));
                    meta.setLore(java.util.List.of(
                        dwarfService.color("&8Артефакт дварфов"),
                        dwarfService.color("&8----------------"),
                        dwarfService.color("&7- Содержимое сохранено"),
                        dwarfService.color("&8ID: portable_chest")
                    ));
                    meta.getPersistentDataContainer().set(dwarfService.getItemIdKey(), org.bukkit.persistence.PersistentDataType.STRING, "portable_chest");
                    meta.setCustomModelData(31016);

                    BlockState portableState = meta.getBlockState();
                    if (portableState instanceof Chest portableChestState) {
                        portableChestState.getBlockInventory().setContents(savedContents);
                        meta.setBlockState(portableChestState);
                    }

                    portableChest.setItemMeta(meta);

                    // Clear first to prevent vanilla chest-break drops, then remove block.
                    chest.getBlockInventory().clear();
                    event.getClickedBlock().setType(Material.AIR);
                    player.getInventory().addItem(portableChest);
                    event.setCancelled(true);
                    player.sendMessage(dwarfService.color("&eСундук перенесен в инвентарь."));
                }
            }
        }

        if (event.getAction().isRightClick() && event.getHand() != null) {
            ItemStack used = event.getHand() == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

            if (dwarfService.hasItemTag(used, "air_checker")) {
                event.setCancelled(true);
                int seconds = dwarfService.getRemainingSurfaceAirSeconds(player);
                int minutes = seconds / 60;
                int secPart = seconds % 60;
                player.sendMessage(dwarfService.color("&bОсталось воздуха на поверхности: &f" + minutes + ":" + String.format("%02d", secPart)));
                int safeY = dwarfService.getStageSafeAltitudeY(player);
                if (player.getLocation().getBlockY() <= safeY) {
                    player.sendMessage(dwarfService.color("&aНиже или на высоте " + safeY + ": воздух полностью восстановлен."));
                }
                return;
            }

            if (dwarfService.hasItemTag(used, "dwarf_snot")) {
                event.setCancelled(true);
                Snowball projectile = player.launchProjectile(Snowball.class);
                snotProjectiles.add(projectile.getUniqueId());
                consumeOne(player.getInventory(), event.getHand());
                return;
            }

            if (dwarfService.hasItemTag(used, "cave_gas_balloon")) {
                event.setCancelled(true);
                dwarfService.useBalloonCharge(player, event.getHand());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!dwarfService.isDwarf(event.getPlayer()) && isRestrictedForNonDwarf(event.getItemInHand())) {
            event.setCancelled(true);
            warnCannotUse(event.getPlayer());
            return;
        }

        ItemStack item = event.getItemInHand();

        if (dwarfService.hasItemTag(item, "portable_chest") && event.getBlockPlaced().getState() instanceof Chest placedChest) {
            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof Chest storedChest) {
                placedChest.getBlockInventory().setContents(storedChest.getBlockInventory().getContents());
                placedChest.update();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (!dwarfService.isDwarf(event.getPlayer()) && isRestrictedForNonDwarf(event.getItemStack())) {
            event.setCancelled(true);
            warnCannotUse(event.getPlayer());
            return;
        }

        if (dwarfService.hasItemTag(event.getItemStack(), "cave_gas_balloon")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(dwarfService.color("&cЭтот баллон нельзя разлить."));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (dwarfService.isDwarf(player)) {
            return;
        }
        if (isRestrictedForNonDwarf(event.getItem().getItemStack())) {
            event.setCancelled(true);
            warnCannotUse(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(DwarfProgressMenu.TITLE)) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (dwarfService.isDwarf(player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (isRestrictedForNonDwarf(current) || isRestrictedForNonDwarf(cursor)) {
            event.setCancelled(true);
            warnCannotUse(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(DwarfProgressMenu.TITLE)) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (dwarfService.isDwarf(player)) {
            return;
        }
        if (isRestrictedForNonDwarf(event.getOldCursor())) {
            event.setCancelled(true);
            warnCannotUse(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallingGravel(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) {
            return;
        }
        Material fallingType = fallingBlock.getBlockData().getMaterial();
        if (fallingType != Material.GRAVEL) {
            return;
        }

        Block block = event.getBlock();
        for (Player player : block.getWorld().getPlayers()) {
            if (!dwarfService.isDwarf(player)) {
                continue;
            }
            if (Math.abs(player.getLocation().getX() - (block.getX() + 0.5D)) > 0.8D) {
                continue;
            }
            if (Math.abs(player.getLocation().getZ() - (block.getZ() + 0.5D)) > 0.8D) {
                continue;
            }
            if (Math.abs(player.getLocation().getY() - block.getY()) > 1.2D) {
                continue;
            }

            event.setCancelled(true);
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5D, 0.5D, 0.5D), new ItemStack(Material.GRAVEL));
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !dwarfService.isDwarf(player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setDamage(event.getDamage() * dwarfService.getFallDamageMultiplier());
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            event.setCancelled(true);
        }

        if (dwarfService.getStage(player) >= 5) {
            if (event.getCause() == EntityDamageEvent.DamageCause.LAVA
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!dwarfService.isDwarf(player)) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }

        if (dwarfService.isStoneSleeping(player)) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
            return;
        }

        if (dwarfService.getStage(player) < 5) {
            return;
        }

        if (player.getFoodLevel() > 0) {
            return;
        }
        if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player defender) {
            ItemStack shield = defender.getInventory().getItemInOffHand();
            if (dwarfService.hasItemTag(shield, "ore_shield") && defender.isBlocking()) {
                event.setDamage(event.getDamage() * dwarfService.getOreShieldDamageMultiplier());
                if (ThreadLocalRandom.current().nextDouble() < dwarfService.getOreShieldSlowChance() && event.getDamager() instanceof LivingEntity living) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, true, true, true));
                }
            }
        }

        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        if (dwarfService.hasItemTag(mainHand, "dwarf_snot") && event.getEntity() instanceof LivingEntity target) {
            applySnotEffects(target);
            consumeOne(attacker.getInventory(), EquipmentSlot.HAND);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSnotHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }
        if (!snotProjectiles.remove(snowball.getUniqueId())) {
            return;
        }
        if (event.getHitEntity() instanceof LivingEntity target) {
            applySnotEffects(target);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() instanceof Keyed keyed) {
            if (!event.getViewers().isEmpty() && event.getViewers().get(0) instanceof Player player) {
                if (!dwarfService.canCraftRecipe(player, keyed.getKey())) {
                    event.getInventory().setResult(null);
                    return;
                }
            }
        }

        if (event.getRecipe() instanceof Keyed keyed && keyed.getKey().equals(plugin.key("big_bottle_recipe"))) {
            CraftingInventory inv = event.getInventory();
            ItemStack[] m = inv.getMatrix();
            ItemStack center = m.length > 4 ? m[4] : null;
            if (!dwarfService.hasCustomId(center, "cave_gas_balloon")) {
                inv.setResult(null);
                return;
            }
        }

        CraftingInventory inventory = event.getInventory();
        ItemStack result = inventory.getResult();
        if (result == null) {
            return;
        }

        ItemStack[] matrix = inventory.getMatrix();

        if (result.getType() == Material.BLAZE_POWDER) {
            for (ItemStack item : matrix) {
                if (dwarfService.hasItemTag(item, "golden_rod")) {
                    inventory.setResult(new ItemStack(Material.AIR));
                    return;
                }
            }
        }

        // Big bottle is now a glass bottle item, so shield/banner rewrite protection is no longer required.
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getRecipe() instanceof Keyed keyed)) {
            return;
        }

        if (event.getWhoClicked() instanceof Player player && !dwarfService.canCraftRecipe(player, keyed.getKey())) {
            event.setCancelled(true);
            return;
        }

        if (!keyed.getKey().equals(plugin.key("big_bottle_recipe"))) {
            return;
        }

        CraftingInventory inv = event.getInventory();
        ItemStack[] m = inv.getMatrix();
        ItemStack center = m.length > 4 ? m[4] : null;
        if (!dwarfService.hasCustomId(center, "cave_gas_balloon")) {
            event.setCancelled(true);
        }
    }

    private void maybeDoubleDrop(Player player, Block block) {
        if (block.getType() == Material.ANCIENT_DEBRIS) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() >= dwarfService.getSkilledHandsChance()) {
            return;
        }
        for (ItemStack drop : block.getDrops(tool, player)) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
        }
    }

    private void triggerLivingOre(Player player, Block origin) {
        Material oreType = origin.getType();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Block nearby = origin.getRelative(x, y, z);
                    if (nearby.equals(origin) || nearby.getType() != oreType) {
                        continue;
                    }
                    if (ThreadLocalRandom.current().nextDouble() <= 0.15D) {
                        safeBreak(nearby, player);
                    }
                }
            }
        }
    }

    private void veinMine(Player player, Block origin) {
        Material oreType = origin.getType();
        Queue<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        queue.add(origin);

        int mined = 0;
        while (!queue.isEmpty() && mined < MAX_VEIN_BLOCKS - 1) {
            Block current = queue.poll();
            if (current == null || visited.contains(current) || current.getType() != oreType) {
                continue;
            }
            visited.add(current);
            if (current.equals(origin)) {
                continue;
            }

            mined++;
            safeBreak(current, player);

            for (BlockFace face : BlockFace.values()) {
                if (face == BlockFace.SELF) {
                    continue;
                }
                Block next = current.getRelative(face);
                if (!visited.contains(next) && next.getType() == oreType) {
                    queue.add(next);
                }
            }
        }
    }

    private void mineHammerArea(Player player, Block center) {
        // 3x3 plane based on player view: supports horizontal, up and down mining.
        BlockFace facing = getMiningFacing(player);

        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                Block target;
                if (facing == BlockFace.UP || facing == BlockFace.DOWN) {
                    target = center.getRelative(a, 0, b);
                } else if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
                    target = center.getRelative(a, b, 0);
                } else {
                    target = center.getRelative(0, b, a);
                }
                if (target.equals(center) || target.getType() == Material.AIR || target.getType() == Material.BEDROCK) {
                    continue;
                }
                safeBreak(target, player);
            }
        }
    }

    private BlockFace getMiningFacing(Player player) {
        float pitch = player.getLocation().getPitch();
        if (pitch <= -55.0F) {
            return BlockFace.UP;
        }
        if (pitch >= 55.0F) {
            return BlockFace.DOWN;
        }

        BlockFace facing = player.getFacing();
        if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH || facing == BlockFace.EAST || facing == BlockFace.WEST) {
            return facing;
        }
        float yaw = player.getLocation().getYaw();
        int rot = Math.round(yaw / 90.0F) & 3;
        return switch (rot) {
            case 0 -> BlockFace.SOUTH;
            case 1 -> BlockFace.WEST;
            case 2 -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    private void applySnotEffects(LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 10, 0, true, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 4, 5, true, true, true));
    }

    private void warnCannotUse(Player player) {
        long now = System.currentTimeMillis();
        long last = itemWarnCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 1500L) {
            return;
        }
        itemWarnCooldown.put(player.getUniqueId(), now);
        player.sendMessage(dwarfService.color("&cЯ не могу поднять это"));
    }

    private boolean isRestrictedForNonDwarf(ItemStack itemStack) {
        if (!dwarfService.isCustomDwarfItem(itemStack)) {
            return false;
        }
        return !dwarfService.hasItemTag(itemStack, "dwarf_ale")
            && !dwarfService.hasItemTag(itemStack, "big_bottle");
    }

    private void safeBreak(Block block, Player player) {
        String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        if (!mineGuard.add(key)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        internalBreakPlayers.add(playerId);

        try {
            if (!buildProtection.canBuild(player, block.getLocation()) || !canBreakByRegionPlugins(player, block)) {
                return;
            }
            player.breakBlock(block);
            dwarfService.addMinedBlocks(player, 1);
        } catch (Throwable ignored) {
            // Suppress internal mining errors to avoid console spam loops on protected blocks.
        } finally {
            internalBreakPlayers.remove(playerId);
            mineGuard.remove(key);
        }
    }

    private boolean canBreakByRegionPlugins(Player player, Block block) {
        String key = locKey(block);
        probeGuard.add(key);
        try {
            BlockBreakEvent probe = new BlockBreakEvent(block, player);
            plugin.getServer().getPluginManager().callEvent(probe);
            return !probe.isCancelled();
        } finally {
            probeGuard.remove(key);
        }
    }

    private String locKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private boolean isOre(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS") || name.equals("NETHER_GOLD_ORE") || name.equals("NETHER_QUARTZ_ORE");
    }

    private void consumeOne(PlayerInventory inventory, EquipmentSlot slot) {
        ItemStack stack = slot == EquipmentSlot.HAND ? inventory.getItemInMainHand() : inventory.getItemInOffHand();
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        int amount = stack.getAmount();
        if (amount <= 1) {
            if (slot == EquipmentSlot.HAND) {
                inventory.setItemInMainHand(null);
            } else {
                inventory.setItemInOffHand(null);
            }
        } else {
            stack.setAmount(amount - 1);
            if (slot == EquipmentSlot.HAND) {
                inventory.setItemInMainHand(stack);
            } else {
                inventory.setItemInOffHand(stack);
            }
        }
    }
}