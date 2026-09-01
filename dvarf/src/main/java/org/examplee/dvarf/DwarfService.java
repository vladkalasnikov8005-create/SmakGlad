package org.examplee.dvarf;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class DwarfService {

    private static final Map<String, Integer> MODEL_DATA = new LinkedHashMap<>();

    static {
        MODEL_DATA.put("dwarf_ale", 31001);
        MODEL_DATA.put("miner_helmet", 31002);
        MODEL_DATA.put("dwarf_hammer", 31003);
        MODEL_DATA.put("sunglasses", 31004);
        MODEL_DATA.put("dwarf_snot", 31005);
        MODEL_DATA.put("golden_rod", 31006);
        MODEL_DATA.put("tinted_plate", 31007);
        MODEL_DATA.put("cave_gas_balloon", 31008);
        MODEL_DATA.put("empty_balloon", 31009);
        MODEL_DATA.put("cave_gas", 31010);
        MODEL_DATA.put("ore_shield", 31011);
        MODEL_DATA.put("mountain_elixir", 31012);
        MODEL_DATA.put("big_bottle", 31013);
        MODEL_DATA.put("air_checker", 31014);
        MODEL_DATA.put("dwarf_race_book", 31015);
        MODEL_DATA.put("portable_chest", 31016);

        MODEL_DATA.put("trim_shield_dune", 32001);
        MODEL_DATA.put("trim_shield_ward", 32002);
        MODEL_DATA.put("trim_shield_tide", 32003);
        MODEL_DATA.put("trim_shield_flame", 32004);
        MODEL_DATA.put("trim_shield_eye", 32005);
        MODEL_DATA.put("trim_shield_rib", 32006);
        MODEL_DATA.put("trim_shield_snout", 32007);
        MODEL_DATA.put("trim_shield_shaper", 32008);
        MODEL_DATA.put("trim_shield_sentry", 32009);
        MODEL_DATA.put("trim_shield_vex", 32010);
        MODEL_DATA.put("trim_shield_spire", 32011);
        MODEL_DATA.put("trim_shield_silence", 32012);
        MODEL_DATA.put("trim_shield_coast", 32013);
        MODEL_DATA.put("trim_shield_wayfinder", 32014);
        MODEL_DATA.put("trim_shield_raiser", 32015);
        MODEL_DATA.put("trim_shield_host", 32016);
        MODEL_DATA.put("trim_shield_skull", 32017);
        MODEL_DATA.put("trim_shield_flow", 32018);
        MODEL_DATA.put("trim_shield_bolt", 32019);
    }

    private static final Map<Material, String> TEMPLATE_TO_SHIELD = new HashMap<>();

    static {
        registerTemplate("DUNE_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_dune");
        registerTemplate("WARD_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_ward");
        registerTemplate("TIDE_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_tide");
        registerTemplate("SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_sentry");
        registerTemplate("VEX_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_vex");
        registerTemplate("RIB_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_rib");
        registerTemplate("SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_snout");
        registerTemplate("EYE_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_eye");
        registerTemplate("SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_spire");
        registerTemplate("SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_silence");
        registerTemplate("WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_wayfinder");
        registerTemplate("RAISER_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_raiser");
        registerTemplate("HOST_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_host");
        registerTemplate("SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_shaper");
        registerTemplate("FLOW_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_flow");
        registerTemplate("BOLT_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_bolt");
        registerTemplate("COAST_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_coast");
        registerTemplate("WILD_ARMOR_TRIM_SMITHING_TEMPLATE", "trim_shield_flame");
        registerTemplate("NETHERITE_UPGRADE_SMITHING_TEMPLATE", "trim_shield_flame");
    }

    private static void registerTemplate(String materialName, String shieldId) {
        Material material = Material.matchMaterial(materialName);
        if (material != null) {
            TEMPLATE_TO_SHIELD.put(material, shieldId);
        }
    }

    private final DwarvenCorePlugin plugin;

    private final NamespacedKey dwarfRaceKey;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey enduranceTicksKey;
    private final NamespacedKey balloonChargesKey;
    private final NamespacedKey minedBlocksKey;
    private final NamespacedKey seismicToggleKey;
    private static final int SURFACE_AIR_SECONDS = 600;
    private static final int BASE_SAFE_ALTITUDE_Y = 63;
    private static final int STAGE3_SAFE_ALTITUDE_Y = 100;
    private static final int MAX_STAGE = 5;

    private static final long STAGE1_BLOCKS = 50_000L;
    private static final long STAGE2_BLOCKS = 100_000L;
    private static final long STAGE3_BLOCKS = 250_000L;
    private static final long STAGE4_BLOCKS = 500_000L;
    private static final long STAGE5_BLOCKS = 1_000_000L;

    private final Map<UUID, Integer> surfaceTicks = new HashMap<>();
    private final Map<UUID, Integer> zeroHungerTicks = new HashMap<>();
    private final Map<UUID, Integer> skyHungerTicks = new HashMap<>();
    private final Map<UUID, Integer> snotTimerTicks = new HashMap<>();
    private final Map<UUID, Integer> highAltitudeTicks = new HashMap<>();
    private final Map<UUID, Integer> stageCache = new HashMap<>();
    private final Map<UUID, Integer> furnaceRageCooldownTicks = new HashMap<>();
    private final Map<UUID, Integer> stoneSleepTicks = new HashMap<>();
    private final Map<UUID, Integer> stoneSleepCooldownTicks = new HashMap<>();

    private double skilledHandsChance;
    private double snotDropChance;
    private double oreShieldSlowChance;
    private int surfaceSuffocationSeconds;
    private int zeroHungerDeathTicks;
    private int skyHungerIntervalSeconds;
    private int snotRollTicks;
    private int snotGuaranteedTicks;
    private int mountainElixirSeconds;
    private int skyHungerLoss;
    private float miningExtraExhaustion;
    private double oreShieldDamageMultiplier;
    private double fallDamageMultiplier;
    private int furnaceRegenRadius;
    private int tickTaskId = -1;

    public DwarfService(DwarvenCorePlugin plugin) {
        this.plugin = plugin;
        this.dwarfRaceKey = plugin.key("race_dwarf");
        this.itemIdKey = plugin.key("item_id");
        this.enduranceTicksKey = plugin.key("endurance_ticks");
        this.balloonChargesKey = plugin.key("balloon_charges");
        this.minedBlocksKey = plugin.key("blocks_mined");
        this.seismicToggleKey = plugin.key("seismic_enabled");
    }

    public void startTickLoop() {
        stopTickLoop();
        tickTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                normalizeHammerVisibility(player);

                if (!isDwarf(player)) {
                    clearTrackers(player);
                    continue;
                }

                refreshStage(player);

                applyPassiveEffects(player);
                processSurfaceDebuffs(player);
                processAltitudeDebuffs(player);
                processHungerDeath(player);
                processSnotDrop(player);
                tickEndurance(player);
                processStoneSleep(player);
            }
        }, 20L, 20L).getTaskId();
    }

    public void stopTickLoop() {
        if (tickTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
    }

    public void shutdown() {
        stopTickLoop();
        unregisterRecipes();
    }

    public void reloadRuntimeSettings() {
        plugin.reloadConfig();
        reloadSettings();
    }

    public void reloadSettings() {
        this.skilledHandsChance = plugin.getConfig().getDouble("chances.skilled_hands_ore_double", 0.20D);
        this.snotDropChance = plugin.getConfig().getDouble("chances.snot_drop_every_roll", 0.05D);
        this.oreShieldSlowChance = plugin.getConfig().getDouble("chances.ore_shield_slow_on_block", 0.10D);
        this.surfaceSuffocationSeconds = Math.max(1, plugin.getConfig().getInt("timers.surface_suffocation_seconds", 3600));
        this.zeroHungerDeathTicks = Math.max(1, plugin.getConfig().getInt("timers.zero_hunger_death_ticks", 2400));
        this.skyHungerIntervalSeconds = Math.max(1, plugin.getConfig().getInt("timers.sky_hunger_interval_seconds", 10));
        this.snotRollTicks = Math.max(20, plugin.getConfig().getInt("timers.snot_roll_ticks", 6000));
        this.snotGuaranteedTicks = Math.max(20, plugin.getConfig().getInt("timers.snot_guaranteed_ticks", 36000));
        this.mountainElixirSeconds = Math.max(1, plugin.getConfig().getInt("timers.mountain_elixir_seconds", 120));
        this.skyHungerLoss = Math.max(0, plugin.getConfig().getInt("values.sky_hunger_loss", 2));
        this.miningExtraExhaustion = (float) Math.min(0.01D, plugin.getConfig().getDouble("values.mining_extra_exhaustion", 0.005D));
        this.oreShieldDamageMultiplier = plugin.getConfig().getDouble("values.ore_shield_damage_multiplier", 0.20D);
        this.fallDamageMultiplier = plugin.getConfig().getDouble("values.fall_damage_multiplier", 0.70D);
        this.furnaceRegenRadius = Math.max(1, plugin.getConfig().getInt("values.furnace_regen_radius", 3));
    }

    public void registerRecipes() {
        unregisterRecipes();

        ItemStack tintedPlate = createItem("tinted_plate", Material.BLACK_STAINED_GLASS_PANE, "Пластина тонированного стекла", List.of("Черная пластина из тонированного стекла"), false);
        StonecuttingRecipe stonecuttingRecipe = new StonecuttingRecipe(plugin.key("tinted_plate_recipe"), tintedPlate, Material.TINTED_GLASS);
        Bukkit.addRecipe(stonecuttingRecipe);

        ItemStack balloon = createItem("cave_gas_balloon", Material.POWDER_SNOW_BUCKET, "Баллон пещерного газа", List.of("Позволяет дышать под водой", "Нельзя случайно разлить"), true);
        setBalloonCharges(balloon, 10);
        ShapedRecipe balloonRecipe = new ShapedRecipe(plugin.key("cave_gas_balloon_recipe"), balloon);
        balloonRecipe.shape(" G ", " B ", "   ");
        balloonRecipe.setIngredient('G', Material.CLAY_BALL);
        balloonRecipe.setIngredient('B', Material.BUCKET);
        Bukkit.addRecipe(balloonRecipe);

        ItemStack bigBottle = createItem("big_bottle", Material.GLASS_BOTTLE, "Большой бутыль", List.of("Кастомная пустая бутылочка"), true);
        ShapedRecipe bigBottleRecipe = new ShapedRecipe(plugin.key("big_bottle_recipe"), bigBottle);
        bigBottleRecipe.shape(" G ", " B ", "   ");
        bigBottleRecipe.setIngredient('G', Material.GLASS_BOTTLE);
        bigBottleRecipe.setIngredient('B', new RecipeChoice.ExactChoice(createNamedItem("cave_gas_balloon")));
        Bukkit.addRecipe(bigBottleRecipe);

        ItemStack hammer = createNamedItem("dwarf_hammer");
        ShapedRecipe hammerRecipe = new ShapedRecipe(plugin.key("dwarf_hammer_recipe"), hammer);
        hammerRecipe.shape("III", " P ", " R ");
        hammerRecipe.setIngredient('I', Material.IRON_INGOT);
        hammerRecipe.setIngredient('P', Material.DIAMOND_PICKAXE);
        hammerRecipe.setIngredient('R', new RecipeChoice.ExactChoice(createNamedItem("golden_rod")));
        Bukkit.addRecipe(hammerRecipe);

        ItemStack oreShield = createNamedItem("ore_shield");
        ShapedRecipe oreShieldRecipe = new ShapedRecipe(plugin.key("ore_shield_recipe"), oreShield);
        oreShieldRecipe.shape(" I ", "ISI", " B ");
        oreShieldRecipe.setIngredient('I', Material.IRON_INGOT);
        oreShieldRecipe.setIngredient('S', Material.SHIELD);
        oreShieldRecipe.setIngredient('B', new RecipeChoice.ExactChoice(createNamedItem("cave_gas_balloon")));
        Bukkit.addRecipe(oreShieldRecipe);

        ItemStack endurance = createItem("mountain_elixir", Material.POTION, "Эликсир горной выносливости", List.of("Минус 50% расхода голода на 2 минуты"), true);
        ShapelessRecipe enduranceRecipe = new ShapelessRecipe(plugin.key("mountain_elixir_recipe"), endurance);
        enduranceRecipe.addIngredient(Material.HONEY_BOTTLE);
        enduranceRecipe.addIngredient(Material.GLOW_BERRIES);
        enduranceRecipe.addIngredient(Material.IRON_INGOT);
        Bukkit.addRecipe(enduranceRecipe);
    }

    public void unregisterRecipes() {
        Bukkit.removeRecipe(plugin.key("tinted_plate_recipe"));
        Bukkit.removeRecipe(plugin.key("cave_gas_balloon_recipe"));
        Bukkit.removeRecipe(plugin.key("big_bottle_recipe"));
        Bukkit.removeRecipe(plugin.key("dwarf_hammer_recipe"));
        Bukkit.removeRecipe(plugin.key("ore_shield_recipe"));
        Bukkit.removeRecipe(plugin.key("mountain_elixir_recipe"));
    }

    public void makeDwarf(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(dwarfRaceKey, PersistentDataType.BYTE, (byte) 1);
        applyDwarfAttributes(player);
        player.sendMessage(color("&6Вы выбрали расу дварфа."));
    }

    public void clearDwarf(Player player) {
        player.getPersistentDataContainer().remove(dwarfRaceKey);
        resetAttributes(player);
        clearTrackers(player);
        player.sendMessage(color("&7Раса дварфа снята."));
    }

    public boolean isDwarf(Player player) {
        Byte val = player.getPersistentDataContainer().get(dwarfRaceKey, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    public void applyDwarfAttributes(Player player) {
        setAttribute(player, resolveAttribute("SCALE", "GENERIC_SCALE"), 0.66D);
        setAttribute(player, resolveAttribute("KNOCKBACK_RESISTANCE", "GENERIC_KNOCKBACK_RESISTANCE"), 0.45D);
        setAttribute(player, resolveAttribute("ARMOR", "GENERIC_ARMOR"), getStage(player) >= 2 ? 2.0D : 0.0D);
    }

    public void resetAttributes(Player player) {
        setAttribute(player, resolveAttribute("SCALE", "GENERIC_SCALE"), 1.0D);
        setAttribute(player, resolveAttribute("KNOCKBACK_RESISTANCE", "GENERIC_KNOCKBACK_RESISTANCE"), 0.0D);
        setAttribute(player, resolveAttribute("ARMOR", "GENERIC_ARMOR"), 0.0D);
    }

    public boolean hasItemTag(ItemStack itemStack, String tag) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return false;
        }

        String val = itemStack.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        return tag.equalsIgnoreCase(val);
    }

    public boolean isCustomDwarfItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return false;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().has(itemIdKey, PersistentDataType.STRING);
    }

    public boolean hasCustomId(ItemStack itemStack, String expected) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return false;
        }
        String val = itemStack.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        return expected.equals(val);
    }

    public boolean isDwarfHammer(ItemStack itemStack) {
        if (hasItemTag(itemStack, "dwarf_hammer")) {
            return true;
        }
        if (itemStack == null || itemStack.getType() != Material.DIAMOND_PICKAXE || !itemStack.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (!meta.hasCustomModelData() || meta.getCustomModelData() != MODEL_DATA.get("dwarf_hammer")) {
            return false;
        }
        if (meta.getDisplayName() == null || !ChatColor.stripColor(meta.getDisplayName()).toLowerCase(Locale.ROOT).contains("молот")) {
            return false;
        }

        // Restore lost PDC tag after anvil/enchant operations so hammer mechanics keep working.
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, "dwarf_hammer");
        itemStack.setItemMeta(meta);
        return true;
    }

    public NamespacedKey getItemIdKey() {
        return itemIdKey;
    }

    public String resolveShieldAbilityFromTemplate(Material material) {
        return TEMPLATE_TO_SHIELD.get(material);
    }

    public boolean isTrimShield(ItemStack itemStack) {
        if (!isCustomDwarfItem(itemStack)) {
            return false;
        }
        String id = itemStack.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        return id != null && id.startsWith("trim_shield_");
    }

    public String getTrimShieldId(ItemStack itemStack) {
        if (!isTrimShield(itemStack)) {
            return null;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    public ItemStack createTrimShield(String trimShieldId) {
        String shortName = trimShieldId.replace("trim_shield_", "").toUpperCase(Locale.ROOT);
        return createItem(trimShieldId, Material.SHIELD, "Щит шаблона " + shortName, List.of("Активная способность: " + shortName, "Shift + ПКМ для активации"), true);
    }

    public void addMinedBlocks(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        if (!isDwarf(player)) {
            return;
        }
        long current = getMinedBlocks(player);
        player.getPersistentDataContainer().set(minedBlocksKey, PersistentDataType.LONG, current + amount);
        refreshStage(player);
    }

    public long getMinedBlocks(Player player) {
        Long val = player.getPersistentDataContainer().get(minedBlocksKey, PersistentDataType.LONG);
        return val == null ? 0L : Math.max(0L, val);
    }

    public void setMinedBlocks(Player player, long value) {
        player.getPersistentDataContainer().set(minedBlocksKey, PersistentDataType.LONG, Math.max(0L, value));
        refreshStage(player);
    }

    public int getStage(Player player) {
        long mined = getMinedBlocks(player);
        if (mined >= STAGE5_BLOCKS) {
            return 5;
        }
        if (mined >= STAGE4_BLOCKS) {
            return 4;
        }
        if (mined >= STAGE3_BLOCKS) {
            return 3;
        }
        if (mined >= STAGE2_BLOCKS) {
            return 2;
        }
        if (mined >= STAGE1_BLOCKS) {
            return 1;
        }
        return 0;
    }

    public String getStageName(int stage) {
        return switch (stage) {
            case 0 -> "Новичок-копатель";
            case 1 -> "Опытный шахтер";
            case 2 -> "Подземный страж";
            case 3 -> "Кузнец глубин";
            case 4 -> "Лорд Каменных Чертогов";
            case 5 -> "Древний Дух Горы";
            default -> "Неизвестно";
        };
    }

    public long getStageThreshold(int stage) {
        return switch (Math.max(0, Math.min(MAX_STAGE, stage))) {
            case 0 -> 0L;
            case 1 -> STAGE1_BLOCKS;
            case 2 -> STAGE2_BLOCKS;
            case 3 -> STAGE3_BLOCKS;
            case 4 -> STAGE4_BLOCKS;
            case 5 -> STAGE5_BLOCKS;
            default -> 0L;
        };
    }

    public long getNextStageTargetBlocks(Player player) {
        int stage = getStage(player);
        if (stage >= MAX_STAGE) {
            return STAGE5_BLOCKS;
        }
        return getStageThreshold(stage + 1);
    }

    public long getBlocksToNextStage(Player player) {
        int stage = getStage(player);
        if (stage >= MAX_STAGE) {
            return 0L;
        }
        return Math.max(0L, getNextStageTargetBlocks(player) - getMinedBlocks(player));
    }

    public void setStage(Player player, int stage) {
        int safeStage = Math.max(0, Math.min(MAX_STAGE, stage));
        setMinedBlocks(player, getStageThreshold(safeStage));
    }

    public boolean isSeismicSenseEnabled(Player player) {
        Byte val = player.getPersistentDataContainer().get(seismicToggleKey, PersistentDataType.BYTE);
        return val == null || val == (byte) 1;
    }

    public void setSeismicSenseEnabled(Player player, boolean enabled) {
        player.getPersistentDataContainer().set(seismicToggleKey, PersistentDataType.BYTE, enabled ? (byte) 1 : (byte) 0);
    }

    public boolean toggleSeismicSense(Player player) {
        boolean next = !isSeismicSenseEnabled(player);
        setSeismicSenseEnabled(player, next);
        return next;
    }

    public boolean isStoneSleeping(Player player) {
        return stoneSleepTicks.getOrDefault(player.getUniqueId(), 0) > 0;
    }

    public boolean canCraftRecipe(Player player, NamespacedKey recipeKey) {
        int stage = getStage(player);
        if (recipeKey.equals(plugin.key("dwarf_hammer_recipe"))) {
            return stage >= 3;
        }
        if (recipeKey.equals(plugin.key("ore_shield_recipe"))) {
            return stage >= 2;
        }
        if (recipeKey.equals(plugin.key("mountain_elixir_recipe"))) {
            return stage >= 4;
        }
        return true;
    }

    public ItemStack createNamedItem(String keyName) {
        return switch (keyName.toLowerCase(Locale.ROOT)) {
            case "dwarf_ale" -> createItem("dwarf_ale", Material.HONEY_BOTTLE, "Эль дворфа", List.of("Выбирает расу дварфа"), true);
            case "miner_helmet" -> createItem("miner_helmet", Material.GOLDEN_HELMET, "Каска шахтера", List.of("Постоянное ночное зрение"), true);
            case "dwarf_hammer" -> createItem("dwarf_hammer", Material.DIAMOND_PICKAXE, "Молот дварфа", List.of("Копает 3x3x1"), false);
            case "sunglasses" -> createItem("sunglasses", Material.CHAINMAIL_HELMET, "Солнцезащитные очки", List.of("Снимают ограничение зрения на поверхности"), true);
            case "dwarf_snot" -> createItem("dwarf_snot", Material.SLIME_BALL, "Сопли дварфа", List.of("Эффект по цели: тошнота и медлительность"), true);
            case "golden_rod" -> createItem("golden_rod", Material.BLAZE_ROD, "Золотой стержень", List.of("Нельзя переработать в порошок"), true);
            case "tinted_plate" -> createItem("tinted_plate", Material.BLACK_STAINED_GLASS_PANE, "Пластина тонированного стекла", List.of("Черное стекло"), false);
            case "cave_gas_balloon" -> createItem("cave_gas_balloon", Material.POWDER_SNOW_BUCKET, "Баллон пещерного газа", List.of("Дает дыхание", "Нельзя случайно разлить"), true);
            case "empty_balloon" -> createItem("empty_balloon", Material.BUCKET, "Пустой баллон", List.of("Основа для баллона пещерного газа"), true);
            case "cave_gas" -> createItem("cave_gas", Material.CLAY_BALL, "Пещерный газ", List.of("Материал для крафта баллона"), true);
            case "ore_shield" -> createItem("ore_shield", Material.SHIELD, "Рудный щит", List.of("Блокирует 80% урона", "10% шанс замедлить атакующего"), true);
            case "mountain_elixir" -> createItem("mountain_elixir", Material.POTION, "Эликсир горной выносливости", List.of("Минус 50% расхода голода на 2 минуты"), true);
            case "big_bottle" -> createItem("big_bottle", Material.GLASS_BOTTLE, "Большой бутыль", List.of("Кастомная пустая бутылочка"), true);
            case "air_checker" -> createItem("air_checker", Material.CLOCK, "Чекер воздуха", List.of("Показывает, сколько воздуха осталось на поверхности"), true);
            case "dwarf_race_book" -> createRaceBook();
            case "trim_shield_dune", "trim_shield_ward", "trim_shield_tide", "trim_shield_flame", "trim_shield_eye",
                "trim_shield_rib", "trim_shield_snout", "trim_shield_shaper", "trim_shield_sentry", "trim_shield_vex",
                "trim_shield_spire", "trim_shield_silence", "trim_shield_coast", "trim_shield_wayfinder", "trim_shield_raiser",
                "trim_shield_host", "trim_shield_skull", "trim_shield_flow", "trim_shield_bolt" -> createTrimShield(keyName.toLowerCase(Locale.ROOT));
            default -> null;
        };
    }

    public List<String> availableItems() {
        return List.of(
            "dwarf_ale",
            "miner_helmet",
            "dwarf_hammer",
            "sunglasses",
            "dwarf_snot",
            "golden_rod",
            "tinted_plate",
            "cave_gas_balloon",
            "empty_balloon",
            "cave_gas",
            "ore_shield",
            "mountain_elixir",
            "big_bottle",
            "air_checker",
            "dwarf_race_book",
            "trim_shield_dune",
            "trim_shield_ward",
            "trim_shield_tide",
            "trim_shield_flame",
            "trim_shield_eye",
            "trim_shield_rib",
            "trim_shield_snout",
            "trim_shield_shaper",
            "trim_shield_sentry",
            "trim_shield_vex",
            "trim_shield_spire",
            "trim_shield_silence",
            "trim_shield_coast",
            "trim_shield_wayfinder",
            "trim_shield_raiser",
            "trim_shield_host",
            "trim_shield_skull",
            "trim_shield_flow",
            "trim_shield_bolt"
        );
    }

    public boolean isSurface(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        return world.getHighestBlockYAt(location) <= location.getBlockY();
    }

    public boolean isDay(Player player) {
        World world = player.getWorld();
        long time = world.getTime();
        return time >= 0 && time <= 12300;
    }

    public void setEndurance(Player player, int ticks) {
        player.getPersistentDataContainer().set(enduranceTicksKey, PersistentDataType.INTEGER, ticks);
    }

    public int getEnduranceTicks(Player player) {
        Integer val = player.getPersistentDataContainer().get(enduranceTicksKey, PersistentDataType.INTEGER);
        return val == null ? 0 : val;
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void processSurfaceDebuffs(Player player) {
        UUID id = player.getUniqueId();
        boolean onSurface = isSurface(player);

        if (onSurface) {
            int surface = surfaceTicks.getOrDefault(id, 0) + 20;
            surfaceTicks.put(id, surface);

            if (surface >= surfaceSuffocationSeconds * 20) {
                player.damage(1.0D);
            }

            int skyTicks = skyHungerTicks.getOrDefault(id, 0) + 20;
            if (skyTicks >= skyHungerIntervalSeconds * 20) {
                player.setFoodLevel(Math.max(0, player.getFoodLevel() - skyHungerLoss));
                skyTicks = 0;
            }
            skyHungerTicks.put(id, skyTicks);
        } else {
            surfaceTicks.put(id, 0);
            skyHungerTicks.put(id, 0);
        }
    }

    private void processHungerDeath(Player player) {
        int stage = getStage(player);
        UUID id = player.getUniqueId();
        if (stage >= 5) {
            if (player.getFoodLevel() <= 0) {
                addEffect(player, PotionEffectType.SLOWNESS, 60, 10);
                addEffect(player, PotionEffectType.REGENERATION, 60, 2);
            }
            zeroHungerTicks.put(id, 0);
            return;
        }

        if (player.getFoodLevel() <= 0) {
            int ticks = zeroHungerTicks.getOrDefault(id, 0) + 20;
            zeroHungerTicks.put(id, ticks);
            if (ticks >= zeroHungerDeathTicks) {
                player.setHealth(0.0D);
                zeroHungerTicks.put(id, 0);
            }
        } else {
            zeroHungerTicks.put(id, 0);
        }
    }

    private void processAltitudeDebuffs(Player player) {
        int stage = getStage(player);
        int safeY = stage >= 3 ? STAGE3_SAFE_ALTITUDE_Y : BASE_SAFE_ALTITUDE_Y;
        int limitSeconds = stage >= 3 ? 300 : SURFACE_AIR_SECONDS;

        UUID id = player.getUniqueId();
        if (player.getLocation().getBlockY() > safeY) {
            int ticks = highAltitudeTicks.getOrDefault(id, 0) + 20;
            highAltitudeTicks.put(id, ticks);

            int remaining = Math.max(0, limitSeconds - (ticks / 20));
            int minutes = remaining / 60;
            int seconds = remaining % 60;
            player.sendActionBar(color("&cВоздух на поверхности: " + minutes + ":" + String.format("%02d", seconds)));

            if (remaining <= Math.max(1, limitSeconds / 2)) {
                addEffect(player, PotionEffectType.WEAKNESS, 60, 0);
            }
            if (remaining <= Math.max(1, limitSeconds / 5)) {
                addEffect(player, PotionEffectType.NAUSEA, 60, 0);
            }
            if (remaining <= Math.max(1, limitSeconds / 10)) {
                addEffect(player, PotionEffectType.BLINDNESS, 60, 0);
            }

            if (ticks >= 20 * limitSeconds) {
                player.damage(1.0D);
            }
            return;
        }

        int ticks = Math.max(0, highAltitudeTicks.getOrDefault(id, 0) - 20);
        highAltitudeTicks.put(id, ticks);

        if (ticks <= 0) {
            return;
        }

        int remaining = Math.max(0, limitSeconds - (ticks / 20));
        int minutes = remaining / 60;
        int seconds = remaining % 60;
        player.sendActionBar(color("&aВосстановление воздуха: " + minutes + ":" + String.format("%02d", seconds)));
    }

    public void useBalloonCharge(Player player, EquipmentSlot hand) {
        ItemStack item = hand == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        if (!hasItemTag(item, "cave_gas_balloon")) {
            return;
        }

        int charges = getBalloonCharges(item);
        if (charges <= 0) {
            replaceWithEmptyBalloon(player, hand == EquipmentSlot.HAND);
            player.sendMessage(color("&cБаллон пуст."));
            return;
        }

        int ticks = highAltitudeTicks.getOrDefault(player.getUniqueId(), 0);
        if (ticks <= 0) {
            player.sendMessage(color("&7Воздух уже на максимуме (10:00)."));
            return;
        }

        int nextCharges = charges - 1;
        setBalloonCharges(item, nextCharges);
        highAltitudeTicks.put(player.getUniqueId(), Math.max(0, ticks - 1200));

        if (nextCharges <= 0) {
            replaceWithEmptyBalloon(player, hand == EquipmentSlot.HAND);
            player.sendMessage(color("&cБаллон опустел."));
            return;
        }

        if (hand == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.getInventory().setItemInOffHand(item);
        }

        int remainingSeconds = getRemainingSurfaceAirSeconds(player);
        player.sendMessage(color("&bБаллон использован. Осталось зарядов: &f" + nextCharges + " &7| Воздух: &f" + (remainingSeconds / 60) + ":" + String.format("%02d", remainingSeconds % 60)));
    }

    private void processSnotDrop(Player player) {
        UUID id = player.getUniqueId();
        int ticks = snotTimerTicks.getOrDefault(id, 0) + 20;

        if (ticks % snotRollTicks == 0 && ThreadLocalRandom.current().nextDouble() < snotDropChance) {
            player.getInventory().addItem(createNamedItem("dwarf_snot"));
            player.sendMessage(color("&aВы чихнули и получили сопли дварфа."));
        }

        if (ticks >= snotGuaranteedTicks) {
            player.getInventory().addItem(createNamedItem("dwarf_snot"));
            player.sendMessage(color("&eГарантированный чих: сопли дварфа добавлены."));
            ticks = 0;
        }

        snotTimerTicks.put(id, ticks);
    }

    private void tickEndurance(Player player) {
        int val = getEnduranceTicks(player);
        if (val <= 0) {
            return;
        }
        val -= 20;
        if (val <= 0) {
            player.getPersistentDataContainer().remove(enduranceTicksKey);
            player.sendMessage(color("&7Эффект горной выносливости закончился."));
        } else {
            player.getPersistentDataContainer().set(enduranceTicksKey, PersistentDataType.INTEGER, val);
        }
    }

    private void applyPassiveEffects(Player player) {
        int stage = getStage(player);
        boolean surface = isSurface(player);

        if (stage >= 5) {
            addEffect(player, PotionEffectType.SPEED, 60, 2);
            addEffect(player, PotionEffectType.HASTE, 60, 2);
            if (player.getLocation().getBlockY() < BASE_SAFE_ALTITUDE_Y) {
                addEffect(player, PotionEffectType.JUMP_BOOST, 60, 1);
            }
        }

        if (!surface && stage < 5) {
            addEffect(player, PotionEffectType.SPEED, 60, 0);
            addEffect(player, PotionEffectType.JUMP_BOOST, 60, 1);
            addEffect(player, PotionEffectType.HASTE, 60, 2);
        } else {
            if (stage == 0) {
                addEffect(player, PotionEffectType.SLOWNESS, 60, 0);
            } else if (player.isSprinting()) {
                addEffect(player, PotionEffectType.SLOWNESS, 60, 0);
            }

            if (isDay(player) && !isWearing(player, "sunglasses")) {
                addEffect(player, PotionEffectType.BLINDNESS, 60, 0);
            }
        }

        if (stage >= 1 && player.getLocation().getBlockY() < BASE_SAFE_ALTITUDE_Y) {
            addEffect(player, PotionEffectType.NIGHT_VISION, 220, 0);
        }

        if (isWearing(player, "miner_helmet")) {
            addEffect(player, PotionEffectType.NIGHT_VISION, 220, 0);
        }

        if (isWearingShield(player, "ore_shield")) {
            addEffect(player, PotionEffectType.SLOWNESS, 60, 0);
        }

        if (isHoldingUsableBalloon(player)) {
            addEffect(player, PotionEffectType.WATER_BREATHING, 60, 0);
            player.setRemainingAir(player.getMaximumAir());
        }

        if (player.getHealth() < player.getMaxHealth() && hasFurnaceNearby(player.getLocation(), furnaceRegenRadius)) {
            addEffect(player, PotionEffectType.REGENERATION, 60, stage >= 3 ? 1 : 0);
        }

        if (stage >= 3) {
            processFurnaceRage(player);
        }

        if (stage >= 5 && player.getLocation().getBlockY() < BASE_SAFE_ALTITUDE_Y) {
            if (isSeismicSenseEnabled(player)) {
                applySeismicSense(player);
            }
        }

        if (stage >= 4 && player.getLocation().getBlockY() < BASE_SAFE_ALTITUDE_Y && isTouchingStoneWall(player)) {
            addEffect(player, PotionEffectType.SLOWNESS, 60, 0);
            // Spider-like climb on cave walls.
            if (player.getVelocity().getY() < 0.18D) {
                player.setVelocity(player.getVelocity().setY(0.18D));
            }
        }

        if (stage >= 5 && player.getLocation().getBlock().isLiquid()) {
            addEffect(player, PotionEffectType.FIRE_RESISTANCE, 60, 0);
            addEffect(player, PotionEffectType.SLOW_FALLING, 60, 0);
        }
    }

    public int getMountainElixirSeconds() {
        return mountainElixirSeconds;
    }

    public int getRemainingSurfaceAirSeconds(Player player) {
        int stage = getStage(player);
        int limitSeconds = stage >= 3 ? 300 : SURFACE_AIR_SECONDS;
        int safeY = stage >= 3 ? STAGE3_SAFE_ALTITUDE_Y : BASE_SAFE_ALTITUDE_Y;
        if (player.getLocation().getBlockY() <= safeY) {
            return limitSeconds;
        }
        int ticks = highAltitudeTicks.getOrDefault(player.getUniqueId(), 0);
        return Math.max(0, limitSeconds - (ticks / 20));
    }

    public int getSafeAltitudeY() {
        return BASE_SAFE_ALTITUDE_Y;
    }

    public int getStageSafeAltitudeY(Player player) {
        return getStage(player) >= 3 ? STAGE3_SAFE_ALTITUDE_Y : BASE_SAFE_ALTITUDE_Y;
    }

    public double getSkilledHandsChance() {
        return 0.15D;
    }

    public float getMiningExtraExhaustion() {
        return miningExtraExhaustion;
    }

    public float getMiningExhaustionForStage(Player player) {
        return miningExtraExhaustion * 1.5F;
    }

    public double getOreShieldDamageMultiplier() {
        return oreShieldDamageMultiplier;
    }

    public double getOreShieldSlowChance() {
        return oreShieldSlowChance;
    }

    public double getFallDamageMultiplier() {
        return fallDamageMultiplier;
    }

    public boolean shouldApplyLivingOre(Player player) {
        return getStage(player) >= 5;
    }

    private boolean isWearing(Player player, String key) {
        ItemStack helmet = player.getInventory().getHelmet();
        if ("miner_helmet".equals(key)) {
            return isMinerHelmet(helmet);
        }
        return hasItemTag(helmet, key);
    }

    private boolean isWearingShield(Player player, String key) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        return hasItemTag(offHand, key);
    }

    private boolean isHolding(Player player, String key) {
        return hasItemTag(player.getInventory().getItemInMainHand(), key) || hasItemTag(player.getInventory().getItemInOffHand(), key);
    }

    private boolean isHoldingUsableBalloon(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (hasItemTag(main, "cave_gas_balloon") && getBalloonCharges(main) > 0) {
            return true;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        return hasItemTag(off, "cave_gas_balloon") && getBalloonCharges(off) > 0;
    }

    private void addEffect(Player player, PotionEffectType type, int duration, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, true));
    }

    private boolean hasFurnaceNearby(Location origin, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Material type = origin.clone().add(x, y, z).getBlock().getType();
                    if (type == Material.FURNACE || type == Material.BLAST_FURNACE || type == Material.SMOKER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void clearTrackers(Player player) {
        UUID id = player.getUniqueId();
        surfaceTicks.remove(id);
        zeroHungerTicks.remove(id);
        skyHungerTicks.remove(id);
        snotTimerTicks.remove(id);
        highAltitudeTicks.remove(id);
        stageCache.remove(id);
        furnaceRageCooldownTicks.remove(id);
        stoneSleepTicks.remove(id);
        stoneSleepCooldownTicks.remove(id);
    }

    private ItemStack createItem(String itemTag, Material material, String name, List<String> lore, boolean enchantedHint) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color("&6" + name));
        List<String> formattedLore = new ArrayList<>();
        formattedLore.add(color("&8Артефакт дварфов"));
        formattedLore.add(color("&8----------------"));
        for (String line : lore) {
            formattedLore.add(color("&7- " + line));
        }
        formattedLore.add(color("&8ID: " + itemTag));
        meta.setLore(formattedLore);
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, itemTag);
        meta.setCustomModelData(MODEL_DATA.getOrDefault(itemTag, 39999));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        if (enchantedHint) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);

        if ("cave_gas_balloon".equalsIgnoreCase(itemTag)) {
            setBalloonCharges(item, 10);
        }

        return item;
    }

    private int getBalloonCharges(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return 0;
        }
        ItemMeta meta = itemStack.getItemMeta();
        Integer val = meta.getPersistentDataContainer().get(balloonChargesKey, PersistentDataType.INTEGER);
        if (val == null) {
            setBalloonCharges(itemStack, 10);
            return 10;
        }
        return Math.max(0, val);
    }

    private void setBalloonCharges(ItemStack itemStack, int charges) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        int safeCharges = Math.max(0, Math.min(10, charges));
        meta.getPersistentDataContainer().set(balloonChargesKey, PersistentDataType.INTEGER, safeCharges);

        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
        lore.removeIf(line -> ChatColor.stripColor(line) != null && ChatColor.stripColor(line).startsWith("Заряды:"));
        lore.add(color("&bЗаряды: &f" + safeCharges + "/10"));
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
    }

    private void replaceWithEmptyBalloon(Player player, boolean mainHand) {
        ItemStack empty = createNamedItem("empty_balloon");
        if (mainHand) {
            player.getInventory().setItemInMainHand(empty);
        } else {
            player.getInventory().setItemInOffHand(empty);
        }
    }

    private ItemStack createRaceBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {
            return book;
        }

        meta.setTitle("Летопись Дварфов");
        meta.setAuthor("Совет Горных Кланов");
        meta.setDisplayName(color("&6Летопись Дварфов"));
        meta.setCustomModelData(MODEL_DATA.getOrDefault("dwarf_race_book", 31015));
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, "dwarf_race_book");
        meta.addPage(
            "§6§lЛетопись Дварфов\n\n"
                + "§0Дварфы рождены в камне и огне.\n"
                + "Они сильны под землей,\n"
                + "стойки в бою и мастера\n"
                + "горного ремесла."
        );
        meta.addPage(
            "§8§lБлагословения\n\n"
                + "§0- Умелые руки: x2 руда\n"
                + "- Молот 3x3 туннель\n"
                + "- Реген у горна\n"
                + "- Сопротивление падению\n"
                + "- Каска и ночное зрение"
        );
        meta.addPage(
            "§4§lПроклятие Неба\n\n"
                + "§0Выше безопасной высоты\n"
                + "дварф начинает терять\n"
                + "запас воздуха.\n\n"
                + "Чтобы восстановиться,\n"
                + "спустись ниже."
        );
        meta.addPage(
            "§2§lПамятка\n\n"
                + "§0- Пользуйся Чекером воздуха\n"
                + "- Носи баллон пещерного газа\n"
                + "- Не задерживайся наверху\n"
                + "- Береги клан"
        );

        book.setItemMeta(meta);
        return book;
    }

    private void refreshStage(Player player) {
        int stage = getStage(player);
        UUID id = player.getUniqueId();
        Integer prev = stageCache.get(id);
        if (prev == null || prev != stage) {
            stageCache.put(id, stage);
            applyDwarfAttributes(player);
            if (prev != null) {
                player.sendMessage(color("&6Эволюция дварфа: этап " + prev + " -> " + stage));
            }
        }
    }

    private void processFurnaceRage(Player player) {
        UUID id = player.getUniqueId();
        int cooldown = Math.max(0, furnaceRageCooldownTicks.getOrDefault(id, 0) - 20);
        furnaceRageCooldownTicks.put(id, cooldown);

        if (cooldown > 0) {
            return;
        }
        if (player.getHealth() > player.getMaxHealth() * 0.5D) {
            return;
        }
        if (player.getFoodLevel() < 3) {
            return;
        }

        player.setFoodLevel(Math.max(0, player.getFoodLevel() - 3));
        addEffect(player, PotionEffectType.RESISTANCE, 200, 1);
        addEffect(player, PotionEffectType.STRENGTH, 200, 0);
        furnaceRageCooldownTicks.put(id, 3600);
        player.sendMessage(color("&cЯрость горна активирована!"));
    }

    private void applySeismicSense(Player player) {
        // Living entities and players glow through blocks in a short periodic pulse.
        for (Player near : player.getWorld().getPlayers()) {
            if (near.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            if (near.getLocation().distanceSquared(player.getLocation()) <= 30 * 30) {
                near.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, true, false, true));
            }
        }
        player.getWorld().getNearbyLivingEntities(player.getLocation(), 30, 30, 30)
            .stream()
            .filter(entity -> !entity.getUniqueId().equals(player.getUniqueId()))
            .forEach(entity -> entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, true, false, true)));
    }

    private void processStoneSleep(Player player) {
        int stage = getStage(player);
        UUID id = player.getUniqueId();

        int cooldown = Math.max(0, stoneSleepCooldownTicks.getOrDefault(id, 0) - 20);
        stoneSleepCooldownTicks.put(id, cooldown);

        int sleepTicks = stoneSleepTicks.getOrDefault(id, 0);
        if (sleepTicks > 0) {
            sleepTicks = Math.max(0, sleepTicks - 20);
            stoneSleepTicks.put(id, sleepTicks);

            addEffect(player, PotionEffectType.BLINDNESS, 60, 0);
            addEffect(player, PotionEffectType.SLOWNESS, 60, 10);
            addEffect(player, PotionEffectType.REGENERATION, 60, 2);
            addEffect(player, PotionEffectType.SATURATION, 20, 0);
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
            return;
        }

        if (stage < 4 || cooldown > 0) {
            return;
        }
        if (player.getHealth() > player.getMaxHealth() * 0.10D) {
            return;
        }

        stoneSleepTicks.put(id, 200);
        stoneSleepCooldownTicks.put(id, 6000);
        player.sendMessage(color("&8Каменная спячка активирована на 10% HP."));
    }

    private boolean isTouchingStoneWall(Player player) {
        Location loc = player.getLocation();
        Material n = loc.clone().add(1, 0, 0).getBlock().getType();
        Material s = loc.clone().add(-1, 0, 0).getBlock().getType();
        Material e = loc.clone().add(0, 0, 1).getBlock().getType();
        Material w = loc.clone().add(0, 0, -1).getBlock().getType();
        return isStoneLike(n) || isStoneLike(s) || isStoneLike(e) || isStoneLike(w);
    }

    private boolean isStoneLike(Material material) {
        return Tag.BASE_STONE_OVERWORLD.isTagged(material)
            || Tag.BASE_STONE_NETHER.isTagged(material)
            || material == Material.DEEPSLATE;
    }

    private void normalizeHammerVisibility(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (!isDwarfHammer(item)) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }

        // Keep miner helmet tag stable after anvil/enchant operations.
        ItemStack helmet = player.getInventory().getHelmet();
        isMinerHelmet(helmet);
    }

    private boolean isMinerHelmet(ItemStack itemStack) {
        if (hasItemTag(itemStack, "miner_helmet")) {
            return true;
        }
        if (itemStack == null || itemStack.getType() != Material.GOLDEN_HELMET || !itemStack.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (!meta.hasCustomModelData() || meta.getCustomModelData() != MODEL_DATA.get("miner_helmet")) {
            return false;
        }

        String plain = meta.getDisplayName() == null ? "" : ChatColor.stripColor(meta.getDisplayName());
        if (!plain.toLowerCase(Locale.ROOT).contains("каска")) {
            return false;
        }

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, "miner_helmet");
        itemStack.setItemMeta(meta);
        return true;
    }

    private Attribute resolveAttribute(String... candidates) {
        for (String candidate : candidates) {
            try {
                return Attribute.valueOf(candidate);
            } catch (IllegalArgumentException ignored) {
                // Try next fallback name for different API mappings.
            }
        }
        return null;
    }

    private void setAttribute(Player player, Attribute attribute, double value) {
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}