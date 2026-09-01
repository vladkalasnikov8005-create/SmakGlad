package org.examplee.vampirest;

import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CustomItemListener implements Listener {

    private final VampireRacePlugin plugin;
    private final Map<UUID, Long> artifactCooldowns = new HashMap<>();
    private final Set<Action> rightClickActions = Set.of(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK);

    public CustomItemListener(VampireRacePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStakeHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!plugin.getVampireManager().isVampire(victim)) {
            return;
        }
        ItemStack hand = attacker.getInventory().getItemInMainHand();
        if (!plugin.getCustomItemManager().isAspenStake(hand)) {
            return;
        }

        event.setDamage(10.0);
        victim.sendMessage(Texts.prefixed("&cОсиновый кол нанес вам тяжелую рану."));
    }

    @EventHandler
    public void onSaltUse(PlayerInteractEvent event) {
        if (!rightClickActions.contains(event.getAction())) {
            return;
        }
        ItemStack hand = event.getItem();
        if (!plugin.getCustomItemManager().isSaltItem(hand)) {
            if (plugin.getCustomItemManager().isBloodArtifact(hand)) {
                useArtifact(event.getPlayer());
                event.setCancelled(true);
            }
            return;
        }

        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            if (clicked == null) {
                return;
            }
            Block placeAt = clicked.getRelative(event.getBlockFace());
            if (!placeAt.getType().isAir()) {
                return;
            }
            placeAt.setType(Material.CALCITE);
            plugin.getSaltBlockService().mark(placeAt);
            consumeOne(player, hand);
            event.setCancelled(true);
            return;
        }

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.getPersistentDataContainer().set(plugin.getCustomItemManager().getSaltProjectileKey(), PersistentDataType.BYTE, (byte) 1);
        consumeOne(player, hand);
        event.setCancelled(true);
    }

    @EventHandler
    public void onSaltProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball snowball)) {
            return;
        }
        if (!snowball.getPersistentDataContainer().has(plugin.getCustomItemManager().getSaltProjectileKey(), PersistentDataType.BYTE)) {
            return;
        }
        if (!(event.getHitEntity() instanceof Player victim) || !plugin.getVampireManager().isVampire(victim)) {
            return;
        }
        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 7, 0, true, true, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 10, 0, true, true, true));
        victim.sendMessage(Texts.prefixed("&fСоль ослепляет и сковывает вас."));
    }

    @EventHandler
    public void onDrinkBloodBottle(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (!plugin.getCustomItemManager().isBloodBottle(item)) {
            return;
        }

        Player player = event.getPlayer();
        CustomItemManager.BloodBottleType type = plugin.getCustomItemManager().getBloodBottleType(item);
        if (type == null) {
            return;
        }

        if (type == CustomItemManager.BloodBottleType.NORMAL) {
            if (plugin.getVampireManager().isVampire(player)) {
                VampireData data = plugin.getVampireManager().getVampireData(player);
                plugin.getVampireManager().setVampireData(player,
                        data.withBlood(data.blood() + 8.0, 100.0).withHunger(Math.min(20, data.hunger() + 3)));
                player.setFoodLevel(Math.min(20, data.hunger() + 3));
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 2.0));
            }
            return;
        }

        if (type == CustomItemManager.BloodBottleType.NUTRITIOUS) {
            if (plugin.getVampireManager().isVampire(player)) {
                VampireData data = plugin.getVampireManager().getVampireData(player);
                plugin.getVampireManager().setVampireData(player,
                        data.withBlood(data.blood() + 16.0, 100.0).withHunger(Math.min(20, data.hunger() + 4)));
                player.setFoodLevel(Math.min(20, data.hunger() + 4));
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 4.0));
            }
            return;
        }

        if (type == CustomItemManager.BloodBottleType.VAMPIRIC) {
            if (!plugin.getVampireManager().isVampire(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 240, 0, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 240, 0, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 240, 0, true, true, true));
                player.sendMessage(Texts.prefixed("&4Вампирская кровь отравляет вас."));
            } else {
                VampireData data = plugin.getVampireManager().getVampireData(player);
                plugin.getVampireManager().setVampireData(player, data.withBlood(data.blood() + 25.0, 100.0));
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 6.0));
            }
            return;
        }

        if (type == CustomItemManager.BloodBottleType.LORD) {
            if (!plugin.getVampireManager().isVampire(player)) {
                boolean started = plugin.getVampireManager().startLordBloodInfection(player);
                if (!started) {
                    player.sendMessage(Texts.prefixed("&eЗаражение уже запущено."));
                }
            } else {
                VampireData data = plugin.getVampireManager().getVampireData(player);
                double maxBlood = plugin.getConfig().getDouble("vampire.max-blood", 100.0);
                plugin.getVampireManager().setVampireData(player,
                        data.withBlood(maxBlood, maxBlood).withHunger(20));
                player.setFoodLevel(20);
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60, 1, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 120, 1, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 30, 1, true, true, true));
                player.sendMessage(Texts.prefixed("&4Кровь Лорда усиливает вас."));
            }
            return;
        }

        if (type == CustomItemManager.BloodBottleType.CORRUPTED) {
            if (plugin.getVampireManager().isVampire(player)) {
                VampireData data = plugin.getVampireManager().getVampireData(player);
                plugin.getVampireManager().setVampireData(player, data.withBlood(data.blood() + 5.0, 100.0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 5, 0, true, true, true));
                player.sendMessage(Texts.prefixed("&aИспорченная кровь: +5 крови, но вас мутит от нее."));
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 20, 0, true, true, true));
                player.sendMessage(Texts.prefixed("&2Испорченная кровь оказалась токсичной."));
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (plugin.getSaltBlockService().isSaltBlock(block) && block.getType() == Material.CALCITE) {
            if (plugin.getVampireManager().isVampire(event.getPlayer()) && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.getPlayer().damage(1.0);
                event.getPlayer().sendMessage(Texts.prefixed("&fСоль жжется: &c-1 HP"));
            }
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setDropItems(false);
                block.getWorld().dropItemNaturally(block.getLocation(), plugin.getCustomItemManager().createSaltBlockItem(1));
            }
        }
        plugin.getSaltBlockService().unmark(event.getBlock());
    }

    @EventHandler
    public void onSaltBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getCustomItemManager().isSaltBlockItem(event.getItemInHand())) {
            return;
        }
        plugin.getSaltBlockService().mark(event.getBlockPlaced());
    }

    @EventHandler
    public void onVampireMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getVampireManager().isVampire(player) || event.getTo() == null) {
            return;
        }
        Block feet = event.getTo().getBlock();
        Block underFeet = event.getTo().clone().subtract(0, 1, 0).getBlock();
        if (isSaltBlock(feet) || isSaltBlock(underFeet)) {
            event.setTo(event.getFrom());
            player.sendActionBar("Соль преграждает путь");
        }
    }

    private boolean isSaltBlock(Block block) {
        return plugin.getSaltBlockService().isSaltBlock(block);
    }

    private void consumeOne(Player player, ItemStack stack) {
        if (player.getGameMode().name().equals("CREATIVE")) {
            return;
        }
        stack.setAmount(stack.getAmount() - 1);
        if (stack.getAmount() <= 0 && player.getInventory().getItemInMainHand().equals(stack)) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
    }

    private void useArtifact(Player player) {
        long now = System.currentTimeMillis();
        long cooldown = 45_000L;
        long endsAt = artifactCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (!plugin.getVampireManager().isAdminMode(player) && endsAt > now) {
            player.sendMessage(Texts.prefixed("&eАртефакт перезаряжается: " + ((endsAt - now) / 1000.0) + " сек."));
            return;
        }
        if (!plugin.getVampireManager().isVampire(player)) {
            player.sendMessage(Texts.prefixed("&cАртефакт откликается только вампирам."));
            return;
        }
        plugin.getAbilityManager().activateBloodShield(player,
                plugin.getConfig().getDouble("abilities.bloodshield.shield-hp", 12.0),
                plugin.getConfig().getInt("abilities.bloodshield.duration-seconds", 12));
        if (!plugin.getVampireManager().isAdminMode(player)) {
            artifactCooldowns.put(player.getUniqueId(), now + cooldown);
        }
        player.sendMessage(Texts.prefixed("&aКровавый артефакт активировал щит."));
    }
}