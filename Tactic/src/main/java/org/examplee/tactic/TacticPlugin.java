package org.examplee.tactic;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;
import java.util.*;
import java.util.ArrayDeque;

public class TacticPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final String TEAM_NAME = "tactic_mask_hide";
    private static final String MENU_TITLE = ChatColor.GOLD + "Выдача предметов Tactic";
    private static final int MENU_SIZE = 54;

    // === Режимы КАССЕТНОЙ ГРАНАТЫ (только дальность броска) ===
    private static final int SMOKE_SHORT  = 0;  // 2-4 блока
    private static final int SMOKE_MEDIUM = 1;  // 5-7
    private static final int SMOKE_LONG   = 2;  // 10-15
    private static final int SMOKE_HUGE   = 3;  // 20-30
    private static final int SMOKE_GIANT  = 4;  // 40-50

    // === Режимы динамита ===
    private static final int DYN_SHORT  = 0;
    private static final int DYN_MEDIUM = 1;
    private static final int DYN_LONG   = 2;

    // === Параметры дымовой гранаты ===
    private static final int    CLOUD_RADIUS        = 4;      // 8 блоков ширина (радиус 4)
    private static final int    CLOUD_HEIGHT        = 5;      // 5 блоков высота
    private static final int    CLOUD_DURATION      = 300;    // 15 секунд
    private static final int    CLOUD_TICK_PERIOD   = 1;
    private static final int    PARTICLES_PER_TICK  = 45;
    private static final int    BLINDNESS_SEC       = 4;
    private static final int    SLOWNESS_SEC        = 5;
    private static final int    SLOWNESS_LVL        = 1;
    private static final double SMOKE_RANGE         = 25.0;   // макс. дистанция постановки дыма по лучу взгляда

    // === Липкая бомба (одна мини-TNT, прилипает и взрывается через 10 сек) ===
    private static final int    STICKY_SPEED_SHORT = 0;
    private static final int    STICKY_SPEED_MED   = 1;
    private static final int    STICKY_SPEED_LONG  = 2;
    private static final int    STICKY_FUSE_TICKS  = 200;   // 10 сек после прилипания
    private static final float  STICKY_POWER       = 2.8F;  // слабее обычного TNT (4.0F)
    private static final double STICKY_ATTACH_DIST = 0.9;
    private static final int    STICKY_GRACE_TICKS = 5;

    // === Плуг ===
    private static final int    PLOW_RADIUS        = 1;    // строго 3×3 (радиус 1)
    private static final int    PLOW_TICK_PERIOD   = 2;    // проверка каждые 2 тика (~10 раз/сек) — отзывчиво
    private static final int    HOOKAH_PUFFS        = 5;     // 5 тяжек на одну забивку
    private static final int    HOOKAH_COOLDOWN     = 30;    // кулдаун между тяжками (1.5 сек)

    // === Сигареты ===
    private static final int    CIG_SMOKE_TICKS     = 60;    // 3 сек на сам процесс курения
    // Типы сигарет
    private static final int CIG_DIRT     = 0;   // Дешёвка/Самокрутка
    private static final int CIG_CLASSIC  = 1;   // Classic Red
    private static final int CIG_MENTHOL  = 2;   // Menthol Light
    private static final int CIG_GOLD     = 3;   // Gold Filter
    private static final int CIG_CIGAR    = 4;   // Cigar Strong

    // === Кальянные табаки (новая сетка по ценности) ===
    private static final int    TOBA_GARBAGE    = 1;   // Мусорная смесь (дешёвый)
    private static final int    TOBA_BURNT      = 2;   // Жженые листья (дешёвый)
    private static final int    TOBA_CHEMICAL   = 3;   // Химическое яблоко (дешёвый)
    private static final int    TOBA_DOUBLE_APPLE=4;  // Двойное яблоко (средний)
    private static final int    TOBA_GRAPE_MINT = 5;   // Виноград-мята (средний)
    private static final int    TOBA_BLUEBERRY  = 6;   // Черника-сода (средний)
    private static final int    TOBA_PEACH      = 7;   // Премиум персик (дорогой)
    private static final int    TOBA_TANGIERS   = 8;   // Tangiers Noir (дорогой)
    private static final int    TOBA_DIAMOND    = 9;   // Diamond Haze (элита)
    private static final int    TOBA_GODS       =10;   // God's Breath (макс)
    // === ДЕШЁВЫЕ / БРЕДОВЫЕ: галлюциногенные грибные табаки ===
    private static final int    TOBA_SHROOM     =11;   // Грибная смесь (красный мухомор)
    private static final int    TOBA_WARPED     =12;   // Искажающий (эндер/варп)

    // === Передозировка никотином ===
    private static final long   TOXICITY_WINDOW_MS    = 60_000L; // окно 60 сек
    private static final int    TOXIC_THRESH_NAUSEA   = 3;
    private static final int    TOXIC_THRESH_POISON   = 6;
    private static final int    TOXIC_THRESH_WITHER   = 10;
    private static final int    TOXIC_THRESH_DEATH    = 15;

    // === Вирус Мяуканья ===
    private static final double MEOW_INFECT_RADIUS  = 3.0;
    private static final double MEOW_INFECT_CHANCE  = 0.15;   // 15% шанс заражения рядом
    private static final double MEOW_MASK_CHANCE    = 0.01;   // 1% если на игроке надета маска 🎭
    private static final long   MEOW_CHECK_PERIOD   = 90 * 20L; // проверка заражения раз в 90 секунд
    private static final long   MEOW_PROGRESS_PERIOD= 8 * 60 * 20L; // стадия растёт на +1 раз в 8 минут
    private static final long   MEOW_VACCINE_IMMUNITY= 5 * 60 * 1000L; // 5 мин иммунитета после вакцины
    private static final int    MEOW_MAX_STAGE      = 10;
    // Частота звуков и фраз: чем выше стадия, тем чаще, но с добавкой +10 секунд,
    // чтобы не слишком спамило на ранних и поздних стадиях.
    private static final long   MEOW_SOUND_BASE_TICKS  = 500;  // 25 сек база (на 1 стадии = 25+10=35 сек)
    private static final long   MEOW_CHAT_BASE_TICKS   = 1800; // 90 сек база (на 1 стадии = 90+10=100 сек)
    private static final long   MEOW_COUGH_BASE_TICKS  = 1200; // 60 сек база (покашливание)
    // Добавка в тиках к каждому интервалу (10 секунд = 200 тиков)
    private static final long   MEOW_INTERVAL_ADD_TICKS = 10 * 20L;
    // Фразы которые случайно выскакивают в чат на низких стадиях
    private static final String[] MEOW_RANDOM_MESSAGES = {
            "§7Мурррррр.....",
            "§7Мяу....",
            "§7Мурррр...",
            "§7Мяу.....",
            "§7*тихо мяукнул*",
            "§7*мурчание*",
            "§7Мряу?",
            "§7Мрр..мяу..",
    };
    private static final String[] CAT_SOUNDS = {
            "ENTITY_CAT_AMBIENT", "ENTITY_CAT_PURR", "ENTITY_CAT_PURREOW",
            "ENTITY_CAT_STRAY_AMBIENT", "ENTITY_CAT_HURT"
    };
    // Разные варианты вставок-мяуканий (разная длина и пунктуация, для разнообразия)
    private static final String[] MEOW_INSERTS = {
            "муррр", "мяу", "мррр", "мяуу", "мррмяу",
            "мяяу", "мурр", "мур-мур", "мяу-мяу", "мрррр",
            "мурррр", "мяяяу", "мряу", "мияу"
    };
    private static final String[] MEOW_SUFFIXES = {
            "..", "...", ".....", " мяу...", " муррр...",
            " мяяяу...", " мррр..", ""
    };
    private static final String[] MEOW_PREFIXES = {
            "Муррр.. ", "Мяу... ", "Мррр... ", "Мяу... ", "Мряу.. "
    };

    private static final int    CLUSTER_COUNT       = 8;
    private static final double CLUSTER_TRIGGER_Y   = 20.0;   // срабатывает в 20 блоках от земли
    private static final float  CLUSTER_TNT_POWER   = 2.5F;
    // Маленькие TNT из кассеты взрываются ТОЛЬКО при ударе о землю (не в воздухе).
    // Fuse ставим большим «страховочным», а землю детектим в тик-цикле.
    private static final int    CLUSTER_TNT_FUSE    = 200;    // 10 сек — страховка, по факту взрывается раньше о землю
    private static final int    CLUSTER_GRACE_TICKS = 10;     // не срабатывает первые 10 тиков после броска

    // === Самонаводящийся лук ===
    private static final int    HOMING_MODE_PLAYERS = 0;
    private static final int    HOMING_MODE_ALL     = 1;
    private static final int    HOMING_MODE_MOBS    = 2;
    private static final double HOMING_LOCK_RANGE   = 70.0;  // макс дистанция взятия цели (шире — проще захватить)
    private static final double HOMING_LOCK_ANGLE   = 22.0;  // угол в градусах (ещё шире конус)
    private static final int    HOMING_LOCK_TICKS   = 30;    // 1.5 секунды удержания = захват
    private static final double HOMING_TURN_RATE    = 0.75;  // сильно доворачивает
    private static final double HOMING_TURN_RATE_NEAR = 0.95;// почти стопроцентное доведение вблизи
    private static final double HOMING_MAX_DIST     = 160.0; // дольше летит
    private static final float  HOMING_SPEED        = 3.6f;  // чуть быстрее
    private static final double HOMING_GRAVITY_COMP = 0.045; // компенсация гравитации на летящей стреле

    // === Поводок для игроков ===
    private static final int    LEASH_RANGE       = 4;      // дистанция связывания (чуть больше для удобства)
    // Поводок вяжется МГНОВЕННО по ПКМ по игроку (как ванильный поводок на мобов) —
    // удерживать ПКМ 3 сек НЕЛЬЗЯ, потому что Material.LEAD не поднимает руку (isHandRaised не работает).
    private static final int    LEASH_TETHER      = 6;      // максимальное расстояние на поводке от хозяина/блока
    private static final long   LEASH_ESCAPE_AFTER= 3*60*1000L; // через 3 минуты можно вырываться
    private static final int    LEASH_ESCAPE_CLICKS=250;    // нужно ПКМ 250 раз чтобы вырваться
    private static final double LEASH_PULL_STRENGTH = 0.55; // сильнее притяжение
    private static final double LEASH_PULL_STRENGTH_BLOCK = 0.75; // к блоку тянет сильнее
    private static final double LEASH_DAMAGE_PER_PULL = 0.5; // урон при натяжении до предела (пол-бусинки)

    // PDC
    private NamespacedKey keyMask, keyShears, keyDynamite, keyFireball, keySmoke, keyGrenade, keySticky, keyHookah, keyTobacco, keyPlow;
    private NamespacedKey keyCigPack, keyCig, keyVaccine, keySlobber, keyHomingBow, keyLeash;
    private NamespacedKey keyDynMode, keyGrenadeMode, keyStickyMode, keyTobaccoType, keyCigType, keyCigPackType, keyHomingMode;
    private NamespacedKey keyMeowStage;
    private NamespacedKey keyTntEntity, keyTntPower;
    private NamespacedKey keyFireballEntity;
    private NamespacedKey keySmokeEntity, keyGrenadeEntity, keyStickyEntity, keyStunEntity, keyFreezeEntity;
    private NamespacedKey keyClusterTnt;
    private NamespacedKey keyOwner;
    private NamespacedKey keySpawnTick;
    // PDC для прилипших бомб (TNTPrimed), кальянной стойки (BrewingStand)
    private NamespacedKey keyStickyPlanted, keyStickyFuseStart;
    private NamespacedKey keyHookahBlock, keyHookahPuffs, keyHookahTobacco;
    // PDC для самонаводящегося лука
    private NamespacedKey keyHomingArrow, keyHomingTarget;
    // PDC для поводка игроков
    private NamespacedKey keyLeashTarget; // UUID того, кого держим на поводке; "block@x,y,z,world" если привязан к блоку
    private NamespacedKey keyLeashBlock;
    private NamespacedKey keyLeashClicks;
    private NamespacedKey keyLeashSince;

    // === Ритуальный костёр ===
    private NamespacedKey keyBonfire;          // Предмет в руке
    private NamespacedKey keyBonfireFuel;      // Кол-во топлива (0-100) в блоке
    private NamespacedKey keyBonfireBroken;    // Счётчик сломаний (0..3)
    private NamespacedKey keyBonfireSoul;      // 1 если синий костёр (soul)
    private NamespacedKey keyBonfireDeadUntil;
    private NamespacedKey keyRegionTool; // unix-ms до которого костёр потушен и ждёт перезаправки
    static final int BONFIRE_MAX_FUEL   = 100;
    private int BONFIRE_FUEL_PER_TICK = 1;    // сколько единиц снимается за период
    private int BONFIRE_TICK_PERIOD   = 100;   // тиков между сжиганиями (20 тиков = 1 сек; 100 = 5 сек по умолчанию)
    private int BONFIRE_LOG_FUEL = 20;         // поинтов за бревно
    private int BONFIRE_PLANK_FUEL = 5;        // поинтов за доски
    private int BONFIRE_STICK_FUEL = 1;        // поинтов за палку
    private boolean BONFIRE_SPAWN_MOBS = true; // спавн охранников/орды/Вардена
    private static final int BONFIRE_DEAD_GRACE_SEC = 120;    // 2 минуты на перезаправку
    private static final int BONFIRE_WARDEN_FUEL = 10;
    private int bonfireTaskId = -1;
    private final Map<Long, BonfireData> bonfires = new HashMap<>();
    // ArmorStand-голограммы над кострами (показывают остаток топлива)
    private final Map<Long, org.bukkit.entity.ArmorStand> bonfireHolograms = new HashMap<>();
    // ==================== ТЕРРИТОРИИ (жезл регионов) ====================
    private int regionTaskId = -1;
    private final Map<String, RegionData> regions = new HashMap<>();         // имя(нижний регистр) -> данные
    private final Map<UUID, long[]> regionSelections = new HashMap<>();     // игрок -> [x1,z1,x2,z2,worldMsbs] выделение (y игнорируем, 3D столб)
    private final Map<UUID, String> lastEnteredRegion = new HashMap<>();    // игрок -> последний регион в actionbar (для избежания спама)
    // cooldown кальяна и текущая курящаяся сигарета (чтобы нельзя было закурить две сразу)
    private final Map<UUID, Long> hookahCooldown = new HashMap<>();
    private final Map<UUID, Long> smokingTask = new HashMap<>();
    // Состояние захвата цели самонаводящимся луком:
    //   targetId — UUID цели (игрок) или int entityId (моб), progress 0..HOMING_LOCK_TICKS, mode
    private final Map<UUID, HomingLock> homingLocks = new HashMap<>();
    // Состояние связывания игроков поводком:
    //   leashTies  — кого на поводке у кого (хозяин → привязанный игрок + к чему привязан)
    //   leashBind  — устарело (оставлено на случай обратной совместимости); поводок вяжется МГНОВЕННО по ПКМ
    //   leashEscapeClicks — счётчик кликов ПКМ для вырывания
    private final Map<UUID, LeashTie> leashTies = new HashMap<>();
    private final Map<UUID, Object[]> leashBind = new HashMap<>();
    private final Map<UUID, Integer> leashEscapeClicks = new HashMap<>();
    private int leashTaskId = -1;
    // Передозировка: uuid -> список временных меток затяжек (в мс)
    private final Map<UUID, ArrayDeque<Long>> nicotineHits = new HashMap<>();
    // Флаг что эта смерть — от передозировки (чтобы подменить сообщение)
    private final Set<UUID> overdoseDeaths = new HashSet<>();

    private final List<SmokeCloud> smokeClouds = new ArrayList<>();
    private final List<FreezeCloud> freezeClouds = new ArrayList<>();
    // Замороженные блоки (лёд/обсидиан) которые потом растают
    private final Map<Long, FrozenBlock> frozenBlocks = new HashMap<>();
    // Фейковые мобы-галлюцинации: моб -> время исчезновения + владелец
    private final Map<UUID, Hallucination> hallucinations = new HashMap<>();
    private int freezeTaskId = -1;
    private int hallucinationTaskId = -1;
    private final Map<UUID, MaskData> masked = new HashMap<>();
    private final Map<UUID, Integer> expiryTasks = new HashMap<>();
    private final Map<UUID, Inventory> openMenus = new HashMap<>();
    // Флаг: мы сейчас сами открываем меню — не удалять игрока из openMenus/openCategory
    // при синхронном срабатывании InventoryCloseEvent внутри openInventory()
    private final Set<UUID> menuSwitching = Collections.newSetFromMap(new WeakHashMap<>());
    // Кальяны: ключ — long (xz chunk + y+world), значение — тип табака и оставшиеся тяжки
    private final Map<Long, Hookah> hookahs = new HashMap<>();
    private long tickCounter = 0;

    private String maskName, maskColored;
    private long durationTicks;
    private int maskModelData, shearsModelData, dynModelData, fbModelData, smokeModelData, grenadeModelData,
                stickyModelData, hookahModelData, tobaccoModelData, plowModelData,
                cigPackModelData, cigModelData, vaccineModelData, slobberModelData,
                hbowModelData, leashModelData, bonfireModelData, regionModelData;

    // Состояние вируса Мяуканья
    private boolean meowEnabled = true;
    // Карта uuid -> стадия заражения (1..10)
    private final Map<UUID, Integer> meowStage = new HashMap<>();
    // Карта uuid -> тик последнего звука/фразы (для темпа на разных стадиях)
    private final Map<UUID, Long> meowLastSound = new HashMap<>();
    private final Map<UUID, Long> meowLastChat  = new HashMap<>();
    private final Map<UUID, Long> meowLastCough = new HashMap<>();
    // Иммунитет после вакцины (мс до которого действует)
    private final Map<UUID, Long> meowImmunityUntil = new HashMap<>();
    // Ссылка на GUI карантина (игрок -> инвентарь)
    private final Map<UUID, Inventory> openQuarantine = new HashMap<>();
    private int quarantineTaskId = -1;
    // === Оглушающая граната (отдельный предмет, не режим динамита) ===
    // PDC для оглушающей гранаты, крио-гранаты и фейковых мобов-галлюцинаций
    private NamespacedKey keyStunGrenade;
    private NamespacedKey keyFreezeGrenade;
    private NamespacedKey keyHallucination;

    @Override
    public void onEnable() {
        keyMask           = new NamespacedKey(this, "tactic_mask");
        keyShears         = new NamespacedKey(this, "tactic_shears");
        keyDynamite       = new NamespacedKey(this, "tactic_dynamite");
        keyFireball       = new NamespacedKey(this, "tactic_fireball");
        keySmoke          = new NamespacedKey(this, "tactic_smoke");
        keyGrenade        = new NamespacedKey(this, "tactic_grenade");
        keySticky         = new NamespacedKey(this, "tactic_sticky");
        keyHookah         = new NamespacedKey(this, "tactic_hookah");
        keyTobacco        = new NamespacedKey(this, "tactic_tobacco");
        keyCigPack        = new NamespacedKey(this, "tactic_cig_pack");
        keyCig            = new NamespacedKey(this, "tactic_cig");
        keyVaccine        = new NamespacedKey(this, "tactic_vaccine");
        keySlobber        = new NamespacedKey(this, "tactic_slobber");
        keyHomingBow      = new NamespacedKey(this, "tactic_hbow");
        keyHomingMode     = new NamespacedKey(this, "tactic_hbow_mode");
        keyLeash          = new NamespacedKey(this, "tactic_leash");
        keyMeowStage      = new NamespacedKey(this, "tactic_meow_stage");
        keyStunGrenade    = new NamespacedKey(this, "tactic_stun_gren");
        keyDynMode        = new NamespacedKey(this, "tactic_dyn_mode");
        keyGrenadeMode    = new NamespacedKey(this, "tactic_grenade_mode");
        keyStickyMode     = new NamespacedKey(this, "tactic_sticky_mode");
        keyTobaccoType    = new NamespacedKey(this, "tactic_tobacco_type");
        keyCigType        = new NamespacedKey(this, "tactic_cig_type");
        keyCigPackType    = new NamespacedKey(this, "tactic_cig_pack_type");
        keyTntEntity      = new NamespacedKey(this, "tactic_tnt_ent");
        keyTntPower       = new NamespacedKey(this, "tactic_tnt_power");
        keyFireballEntity = new NamespacedKey(this, "tactic_fb_ent");
        keySmokeEntity    = new NamespacedKey(this, "tactic_smoke_ent");
        keyGrenadeEntity  = new NamespacedKey(this, "tactic_grenade_ent");
        keyStickyEntity   = new NamespacedKey(this, "tactic_sticky_ent");
        keyStunEntity     = new NamespacedKey(this, "tactic_stun_ent");
        keyFreezeGrenade  = new NamespacedKey(this, "tactic_freeze");
        keyFreezeEntity   = new NamespacedKey(this, "tactic_freeze_ent");
        keyHallucination  = new NamespacedKey(this, "hallucination");
        keyClusterTnt     = new NamespacedKey(this, "tactic_cluster_tnt");
        keyOwner          = new NamespacedKey(this, "tactic_owner");
        keySpawnTick      = new NamespacedKey(this, "tactic_spawn_tick");
        keyStickyPlanted  = new NamespacedKey(this, "tactic_sticky_planted");
        keyStickyFuseStart= new NamespacedKey(this, "tactic_sticky_fuse_start");
        keyHookahBlock    = new NamespacedKey(this, "tactic_hookah_block");
        keyHookahPuffs    = new NamespacedKey(this, "tactic_hookah_puffs");
        keyHookahTobacco  = new NamespacedKey(this, "tactic_hookah_tobacco");
        keyHomingArrow    = new NamespacedKey(this, "tactic_harrow");
        keyHomingTarget   = new NamespacedKey(this, "tactic_htarget");
        keyLeashTarget    = new NamespacedKey(this, "tactic_ltarget");
        keyLeashBlock     = new NamespacedKey(this, "tactic_lblock");
        keyLeashClicks    = new NamespacedKey(this, "tactic_lclicks");
        keyLeashSince     = new NamespacedKey(this, "tactic_lsince");
        keyPlow           = new NamespacedKey(this, "tactic_plow");
        keyBonfire        = new NamespacedKey(this, "tactic_bonfire");
        keyBonfireFuel    = new NamespacedKey(this, "tactic_bf_fuel");
        keyBonfireBroken  = new NamespacedKey(this, "tactic_bf_broken");
        keyBonfireSoul    = new NamespacedKey(this, "tactic_bf_soul");
        keyBonfireDeadUntil = new NamespacedKey(this, "tactic_bf_dead");
        keyRegionTool      = new NamespacedKey(this, "tactic_region");

        saveDefaultConfig();
        reloadCfg();

        getCommand("tactic").setExecutor(this);
        getCommand("tactic").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        setupHideTeam();

        Bukkit.getScheduler().runTaskTimer(this, this::enforceMask, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(this, () -> tickCounter++, 1L, 1L);
        // Тик снарядов (кас-граната + липучка), прилипших бомб, плуга и дыма
        Bukkit.getScheduler().runTaskTimer(this, this::tickProjectiles, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(this, this::tickStickyPlanted, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(this, this::tickPlows, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(this, this::tickSmokeClouds, CLOUD_TICK_PERIOD, CLOUD_TICK_PERIOD);
        Bukkit.getScheduler().runTaskTimer(this, this::tickMeowInfect, MEOW_CHECK_PERIOD, MEOW_CHECK_PERIOD);
        Bukkit.getScheduler().runTaskTimer(this, this::tickMeowProgress, MEOW_PROGRESS_PERIOD, MEOW_PROGRESS_PERIOD);
        Bukkit.getScheduler().runTaskTimer(this, this::tickMeowEffects, 20L, 20L);
        // Тик самонаводящегося лука: удержание цели + полёт стрел
        Bukkit.getScheduler().runTaskTimer(this, this::tickHoming, 1L, 1L);
        // Тик поводка для игроков
        leashTaskId = Bukkit.getScheduler().runTaskTimer(this, this::tickLeash, 1L, 1L).getTaskId();
        // Тик ритуальных костров запускается/перезапускается в reloadCfg() с настраиваемым периодом
        // Тик крио-облаков (заморозка воды/лавы и эффекты игрокам)
        freezeTaskId = Bukkit.getScheduler().runTaskTimer(this, this::tickFreezeClouds, FREEZE_TICK_PERIOD, FREEZE_TICK_PERIOD).getTaskId();
        // Тик таяния замороженных блоков (каждую секунду)
        Bukkit.getScheduler().runTaskTimer(this, this::tickFrozenBlocks, 20L, 20L);
        // Тик галлюцинаций (фейковые мобы)
        hallucinationTaskId = Bukkit.getScheduler().runTaskTimer(this, this::tickHallucinations, 5L, 5L).getTaskId();
        // Загружаем стадию вируса из PDC игроков онлайн (при релоге)
        Bukkit.getScheduler().runTaskLater(this, () -> { for (Player p : Bukkit.getOnlinePlayers()) loadMeowStage(p); }, 1L);
        // Обновление карантинного меню каждые 20 тиков
        quarantineTaskId = Bukkit.getScheduler().runTaskTimer(this, this::refreshQuarantineMenu, 20L, 20L).getTaskId();
        // Тик территорий (actionbar при входе + чат с правилами)
        loadRegionsFromConfig();
        regionTaskId = Bukkit.getScheduler().runTaskTimer(this, this::tickRegions, 20L, 20L).getTaskId();

        getLogger().info("Tactic включён (26.2)");
    }

    @Override
    public void onDisable() {
        // Отменяем таск обновления карантинного меню
        if (quarantineTaskId != -1) {
            Bukkit.getScheduler().cancelTask(quarantineTaskId);
            quarantineTaskId = -1;
        }
        for (Player p : Bukkit.getOnlinePlayers()) if (isMasked(p)) removeMask(p, false, null);
        masked.clear();
        expiryTasks.values().forEach(Bukkit.getScheduler()::cancelTask);
        expiryTasks.clear();
        smokeClouds.clear();
        // Убираем все наши снаряды и прилипшие липучки
        for (World w : Bukkit.getWorlds()) {
            for (Entity ent : w.getEntities()) {
                if (ent instanceof FallingBlock fb) {
                    var pdc = fb.getPersistentDataContainer();
                    if (pdc.has(keyGrenadeEntity, PersistentDataType.BYTE)
                     || pdc.has(keySmokeEntity, PersistentDataType.BYTE)
                     || pdc.has(keyStickyEntity, PersistentDataType.BYTE))
                        fb.remove();
                }
                if (ent instanceof TNTPrimed tnt) {
                    if (tnt.getPersistentDataContainer().has(keyStickyPlanted, PersistentDataType.BYTE)
                     || tnt.getPersistentDataContainer().has(keyClusterTnt, PersistentDataType.BYTE)
                     || tnt.getPersistentDataContainer().has(keyTntEntity, PersistentDataType.BYTE))
                        tnt.remove();
                }
                if (ent instanceof Snowball sb) {
                    var pdc = sb.getPersistentDataContainer();
                    if (pdc.has(keyStunEntity, PersistentDataType.BYTE)
                     || pdc.has(keyFreezeEntity, PersistentDataType.BYTE))
                        sb.remove();
                }
            }
        }
        hookahs.clear();
        for (Long tid : smokingTask.values()) Bukkit.getScheduler().cancelTask(tid.intValue());
        smokingTask.clear();
        nicotineHits.clear();
        overdoseDeaths.clear();
        hookahCooldown.clear();
        homingLocks.clear();
        // Сбрасываем всех привязанных на поводке (чтоб не висели частицы/скорость после рестарта)
        for (UUID uid : new ArrayList<>(leashTies.keySet())) {
            Player tied = Bukkit.getPlayer(uid);
            if (tied != null) tied.sendMessage(ChatColor.GRAY + "🪢 Поводок отвязался (перезапуск сервера).");
        }
        leashTies.clear();
        leashBind.clear();
        leashEscapeClicks.clear();
        if (leashTaskId != -1) { Bukkit.getScheduler().cancelTask(leashTaskId); leashTaskId = -1; }
        // Вирус Мяуканья: стадия хранится в PDC игрока (сохраняется персистентно между рестартами),
        // поэтому на выключении просто очищаем in-memory кэши, не трогая PDC.
        meowStage.clear();
        meowLastSound.clear();
        meowLastChat.clear();
        meowLastCough.clear();
        meowImmunityUntil.clear();
        // Закрываем все наши меню и очищаем трекеры
        for (Inventory inv : openMenus.values()) {
            try { inv.close(); } catch (Exception ignored) {}
        }
        openMenus.clear();
        openQuarantine.clear();
        openCategory.clear();
        plowLastPos.clear();
        // Отменяем тик костров и убираем все голограммы
        if (bonfireTaskId != -1) { Bukkit.getScheduler().cancelTask(bonfireTaskId); bonfireTaskId = -1; }
        removeAllBonfireHolograms();
        bonfires.clear();
        // Сохраняем регионы и выключаем тик
        if (regionTaskId != -1) { Bukkit.getScheduler().cancelTask(regionTaskId); regionTaskId = -1; }
        saveRegionsToConfig();
        regions.clear();
        regionSelections.clear();
        lastEnteredRegion.clear();
        // Убираем крио-облака и откатываем замороженные блоки
        if (freezeTaskId != -1) { Bukkit.getScheduler().cancelTask(freezeTaskId); freezeTaskId = -1; }
        freezeClouds.clear();
        for (FrozenBlock fb : frozenBlocks.values()) {
            if (fb.world != null && fb.world.getBlockAt(fb.x, fb.y, fb.z).getType() == fb.frozenAs) {
                fb.world.getBlockAt(fb.x, fb.y, fb.z).setType(fb.original, true);
            }
        }
        frozenBlocks.clear();
        // Убираем фейковых мобов-галлюцинации
        if (hallucinationTaskId != -1) { Bukkit.getScheduler().cancelTask(hallucinationTaskId); hallucinationTaskId = -1; }
        for (Hallucination h : hallucinations.values()) {
            try {
                Entity e = Bukkit.getEntity(h.entityUid);
                if (e != null && !e.isDead()) e.remove();
            } catch (Exception ignored) {}
        }
        hallucinations.clear();
        hallucinationUntil.clear();
        freezeClouds.clear();
        menuSwitching.clear();
    }

    private void reloadCfg() {
        reloadConfig();
        maskName = getConfig().getString("mask-name", "????");
        maskColored = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("mask-name-color", "§7") + maskName);
        durationTicks = getConfig().getLong("mask-duration-seconds", 1800L) * 20L;
        maskModelData    = getConfig().getInt("mask-item-model-data",     1001);
        shearsModelData  = getConfig().getInt("shears-item-model-data",   1002);
        dynModelData     = getConfig().getInt("dynamite-item-model-data", 1003);
        fbModelData      = getConfig().getInt("fireball-item-model-data", 1004);
        smokeModelData   = getConfig().getInt("smoke-item-model-data",    1005);
        grenadeModelData = getConfig().getInt("grenade-item-model-data",  1006);
        stickyModelData  = getConfig().getInt("sticky-item-model-data",   1007);
        hookahModelData  = getConfig().getInt("hookah-item-model-data",   1008);
        tobaccoModelData = getConfig().getInt("tobacco-item-model-data",  1009);
        plowModelData    = getConfig().getInt("plow-item-model-data",     1010);
        cigPackModelData = getConfig().getInt("cigpack-item-model-data",  1011);
        cigModelData     = getConfig().getInt("cig-item-model-data",      1012);
        vaccineModelData= getConfig().getInt("vaccine-item-model-data",  1013);
        slobberModelData= getConfig().getInt("slobber-item-model-data",  1014);
        hbowModelData   = getConfig().getInt("hbow-item-model-data",     1016);
        leashModelData  = getConfig().getInt("leash-item-model-data",    1017);
        bonfireModelData= getConfig().getInt("bonfire-item-model-data",  1018);
        regionModelData = getConfig().getInt("region-tool-model-data", 1019);
        BONFIRE_FUEL_PER_TICK = getConfig().getInt("bonfire-fuel-per-tick", 1);
        BONFIRE_TICK_PERIOD   = Math.max(1, getConfig().getInt("bonfire-tick-period-ticks", 100)); // 100 тиков = 5 сек
        BONFIRE_LOG_FUEL      = getConfig().getInt("bonfire-fuel-log", 20);
        BONFIRE_PLANK_FUEL    = getConfig().getInt("bonfire-fuel-plank", 5);
        BONFIRE_STICK_FUEL    = getConfig().getInt("bonfire-fuel-stick", 1);
        BONFIRE_SPAWN_MOBS    = getConfig().getBoolean("bonfire-spawn-mobs", true);
        meowEnabled      = getConfig().getBoolean("meow-virus-enabled", true);
        // Перезагружаем регионы из конфига
        regions.clear();
        loadRegionsFromConfig();
        // Табаков много — CMD начинается с tobaccoModelData и прибавляем тип (1..10) — без конфига

        // Перезапускаем тик костров с новым периодом
        if (bonfireTaskId != -1) Bukkit.getScheduler().cancelTask(bonfireTaskId);
        bonfireTaskId = Bukkit.getScheduler().runTaskTimer(this, this::tickBonfires, BONFIRE_TICK_PERIOD, BONFIRE_TICK_PERIOD).getTaskId();
    }

    private String msg(String path) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages." + path, ""));
    }

    private void setupHideTeam() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = sb.getTeam(TEAM_NAME);
        if (t == null) t = sb.registerNewTeam(TEAM_NAME);
        t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        t.setCanSeeFriendlyInvisibles(false);
    }

    private Team getHideTeam() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = sb.getTeam(TEAM_NAME);
        if (t == null) { setupHideTeam(); t = sb.getTeam(TEAM_NAME); }
        return t;
    }

    // ==================== МАСКА ====================
    public boolean isMasked(Player p) { return p != null && masked.containsKey(p.getUniqueId()); }

    public void applyMask(Player p) { applyMask(p, durationTicks); }

    public void applyMask(Player p, long ticks) {
        UUID uid = p.getUniqueId();
        if (!masked.containsKey(uid)) masked.put(uid, new MaskData(p.getDisplayName(), p.getPlayerListName()));
        else { Integer old = expiryTasks.remove(uid); if (old != null) Bukkit.getScheduler().cancelTask(old); }
        p.setDisplayName(maskColored);
        p.setPlayerListName(maskColored);
        p.setCustomName(maskColored);
        p.setCustomNameVisible(false);
        getHideTeam().addEntry(p.getName());
        int id = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (p.isOnline()) removeMask(p, true, RemoveReason.EXPIRED);
            else cleanupOffline(uid);
        }, ticks).getTaskId();
        expiryTasks.put(uid, id);
        enforceMask();
    }

    public void removeMask(Player p) { removeMask(p, true, RemoveReason.ADMIN); }

    private void removeMask(Player p, boolean cancelTask, RemoveReason reason) {
        UUID uid = p.getUniqueId();
        MaskData d = masked.remove(uid);
        if (d == null) return;
        if (cancelTask) { Integer t = expiryTasks.remove(uid); if (t != null) Bukkit.getScheduler().cancelTask(t); }
        else expiryTasks.remove(uid);
        p.setDisplayName(d.origDisplay);
        p.setPlayerListName(d.origList);
        p.setCustomName(null);
        p.setCustomNameVisible(false);
        Team tm = getHideTeam();
        if (tm != null) tm.removeEntry(p.getName());
        if (reason != null) {
            switch (reason) {
                case EXPIRED -> p.sendMessage(msg("mask-expired"));
                case ADMIN   -> p.sendMessage(msg("mask-removed-by-admin"));
                case SHEARS  -> p.sendMessage(msg("mask-torn-by-shears"));
            }
        }
    }

    private void cleanupOffline(UUID uid) {
        masked.remove(uid);
        Integer t = expiryTasks.remove(uid);
        if (t != null) Bukkit.getScheduler().cancelTask(t);
    }

    private void enforceMask() {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (!isMasked(pl)) {
                if (maskColored.equals(pl.getPlayerListName())) pl.setPlayerListName(pl.getName());
                if (getHideTeam().hasEntry(pl.getName())) getHideTeam().removeEntry(pl.getName());
                continue;
            }
            if (!maskColored.equals(pl.getDisplayName())) pl.setDisplayName(maskColored);
            if (!maskColored.equals(pl.getPlayerListName())) pl.setPlayerListName(maskColored);
            pl.setCustomNameVisible(false);
            if (!getHideTeam().hasEntry(pl.getName())) getHideTeam().addEntry(pl.getName());
        }
    }

    // ==================== ITEMS ====================
    private ItemStack buildMask(int amount) {
        ItemStack it = new ItemStack(Material.PAPER, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.DARK_PURPLE + "🎭 Маска");
        m.setLore(List.of(
                ChatColor.GRAY + "Скрывает ник на 30 минут.",
                ChatColor.DARK_GRAY + "ПКМ — надеть"));
        m.setCustomModelData(maskModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyMask, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack buildShears(int amount) {
        ItemStack it = new ItemStack(Material.SHEARS, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.RED + "✂ Ножницы разоблачения");
        m.setLore(List.of(
                ChatColor.GRAY + "Срывает маску с игрока.",
                ChatColor.DARK_GRAY + "Удар / ПКМ по замаскированному"));
        m.setCustomModelData(shearsModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyShears, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack buildDynamite(int amount, int mode) {
        ItemStack it = new ItemStack(Material.TNT, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        String name, color;
        switch (mode) {
            case DYN_MEDIUM -> { name = "средняя"; color = ChatColor.YELLOW.toString(); }
            case DYN_LONG   -> { name = "дальняя";  color = ChatColor.RED.toString(); }
            default         -> { name = "короткая"; color = ChatColor.GREEN.toString(); mode = DYN_SHORT; }
        }
        m.setDisplayName(ChatColor.RED + "🧨 Динамит");
        m.setLore(List.of(
                ChatColor.GRAY + "Взрыв ломает блоки и наносит урон.",
                ChatColor.DARK_GRAY + "Shift+ЛКМ — режим  ·  ПКМ — бросить",
                ChatColor.DARK_GRAY + "Дальность: " + color + name));
        m.setCustomModelData(dynModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyDynamite, PersistentDataType.BYTE, (byte)1);
        m.getPersistentDataContainer().set(keyDynMode, PersistentDataType.INTEGER, mode);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack buildFireball(int amount) {
        ItemStack it = new ItemStack(Material.FIRE_CHARGE, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.GOLD + "🔥 Фаербол");
        m.setLore(List.of(
                ChatColor.GRAY + "Взрыв подкидывает тебя вверх.",
                ChatColor.DARK_GRAY + "ПКМ — запустить"));
        m.setCustomModelData(fbModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyFireball, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack buildSmoke(int amount) {
        ItemStack it = new ItemStack(Material.GUNPOWDER, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.DARK_GRAY + "💨 Дымовая шашка");
        m.setLore(List.of(
                ChatColor.GRAY + "Облако дыма 8×8×5 на 15 сек.",
                ChatColor.GRAY + "Слепота и медлительность внутри.",
                ChatColor.DARK_GRAY + "ПКМ — поставить перед собой"));
        m.setCustomModelData(smokeModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keySmoke, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack buildClusterGrenade(int amount, int mode) {
        ItemStack it = new ItemStack(Material.CLAY_BALL, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        String name, color;
        switch (mode) {
            case SMOKE_MEDIUM -> { name = "средняя";   color = ChatColor.YELLOW.toString(); }
            case SMOKE_LONG   -> { name = "дальняя";   color = ChatColor.RED.toString(); }
            case SMOKE_HUGE   -> { name = "огромная";  color = ChatColor.DARK_RED.toString(); }
            case SMOKE_GIANT  -> { name = "гигантская"; color = ChatColor.DARK_PURPLE.toString(); }
            default           -> { name = "короткая";  color = ChatColor.GREEN.toString(); mode = SMOKE_SHORT; }
        }
        m.setDisplayName(ChatColor.DARK_GRAY + "💣 Кассетная граната");
        m.setLore(List.of(
                ChatColor.GRAY + "Распадается на 8 зарядов TNT.",
                ChatColor.DARK_GRAY + "Shift+ЛКМ — режим  ·  ПКМ — бросить",
                ChatColor.DARK_GRAY + "Дальность: " + color + name));
        m.setCustomModelData(grenadeModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyGrenade, PersistentDataType.BYTE, (byte)1);
        m.getPersistentDataContainer().set(keyGrenadeMode, PersistentDataType.INTEGER, mode);
        it.setItemMeta(m);
        return it;
    }

    // ==================== ОГЛУШАЮЩИЙ ДИНАМИТ ====================
    private ItemStack buildStunGrenade(int amount) {
        ItemStack it = new ItemStack(Material.FIREWORK_STAR, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.WHITE + "💥 Оглушающая граната");
        m.setLore(List.of(
                ChatColor.GRAY + "Ослепляет и оглушает противников.",
                ChatColor.GRAY + "Радиус 6 блоков, 5 секунд эффектов.",
                ChatColor.RED + "Блоки не ломает.",
                ChatColor.DARK_GRAY + "ПКМ — бросить"));
        m.setCustomModelData(slobberModelData + 1); // 1015 — следующий после 1014, чтобы не пересекаться с vaccine(1013)/slobber(1014)
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyStunGrenade, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private static final double STUN_RADIUS = 6.0;
    private static final float  STUN_DURATION_SEC = 5f;
    private static final double STUN_SPEED = 0.9;
    private static final int    STUN_FUSE = 50;

    // === КРИО-ГРАНАТА (заморозка) ===
    private static final int    FREEZE_RADIUS       = 3;      // 6 блоков ширина (радиус 3)
    private static final int    FREEZE_HEIGHT       = 4;      // 4 блока высота
    private static final int    FREEZE_DURATION     = 240;    // 12 секунд
    private static final int    FREEZE_TICK_PERIOD  = 2;
    private static final int    FREEZE_PARTICLES    = 35;
    private static final int    FREEZE_FUSE         = 45;     // ~2.25 сек полёта
    private static final double FREEZE_SPEED        = 0.9;
    private static final int    FREEZE_ICE_TIME     = 100;    // лёд стоит 5 сек потом тает

    // ==================== КРИО-ГРАНАТА ====================
    private ItemStack buildFreezeGrenade(int amount) {
        ItemStack it = new ItemStack(Material.ICE, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.AQUA + "🧊 Крио-граната");
        m.setLore(List.of(
                ChatColor.GRAY + "Облако холода 6×6×4 на 12 сек.",
                ChatColor.GRAY + "Замедление IV внутри.",
                ChatColor.GRAY + "Вода → лёд · лава → обсидиан (временно).",
                ChatColor.RED + "Блоки не ломает.",
                ChatColor.DARK_GRAY + "ПКМ — бросить"
        ));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyFreezeGrenade, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private boolean isFreezeGrenade(ItemStack s) {
        return s != null && s.getType() == Material.ICE && s.hasItemMeta()
                && s.getItemMeta().getPersistentDataContainer().has(keyFreezeGrenade, PersistentDataType.BYTE);
    }

    // ==================== ЛИПКАЯ БОМБА ====================
    private ItemStack buildSticky(int amount, int mode) {
        ItemStack it = new ItemStack(Material.SLIME_BALL, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        String name, color;
        switch (mode) {
            case STICKY_SPEED_MED  -> { name = "средняя"; color = ChatColor.YELLOW.toString(); }
            case STICKY_SPEED_LONG -> { name = "дальняя";  color = ChatColor.RED.toString(); }
            default                -> { name = "короткая"; color = ChatColor.GREEN.toString(); mode = STICKY_SPEED_SHORT; }
        }
        m.setDisplayName(ChatColor.GREEN + "🟢 Липкая бомба");
        m.setLore(List.of(
                ChatColor.GRAY + "Прилипает к полу/стенам/потолку.",
                ChatColor.GRAY + "Взрыв через 10 сек после прилипания.",
                ChatColor.GRAY + "Взрыв чуть слабее TNT.",
                ChatColor.DARK_GRAY + "Shift+ЛКМ — режим  ·  ПКМ — бросить",
                ChatColor.DARK_GRAY + "Дальность: " + color + name));
        m.setCustomModelData(stickyModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keySticky, PersistentDataType.BYTE, (byte)1);
        m.getPersistentDataContainer().set(keyStickyMode, PersistentDataType.INTEGER, mode);
        it.setItemMeta(m);
        return it;
    }

    // ==================== СИГАРЕТЫ И ПАЧКИ ====================
    private ItemStack buildCigPack(int amount, int type) {
        ItemStack it = new ItemStack(Material.PAPER, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        String name, lore;
        switch (type) {
            case CIG_CLASSIC -> { name = "🚬 Classic Red";
                lore = ChatColor.GRAY + "Классика. Сбивает голод и силы."; }
            case CIG_MENTHOL -> { name = "❄ Menthol Light";
                lore = ChatColor.GRAY + "Ментол. Слепит, но бодрит."; }
            case CIG_GOLD    -> { name = "✨ Gold Filter";
                lore = ChatColor.GRAY + "Дорогие. Лечат, но травят."; }
            case CIG_CIGAR   -> { name = "🟫 Cigar Strong";
                lore = ChatColor.GRAY + "Крепкая сигара. Сила и иссушение."; }
            default          -> { type = CIG_DIRT; name = "📜 Дешёвка";
                lore = ChatColor.GRAY + "Самокрутка. Яд в чистом виде."; }
        }
        m.setDisplayName(ChatColor.WHITE + name);
        m.setLore(List.of(
                lore,
                ChatColor.DARK_GRAY + "ПКМ — достать сигарету"));
        m.setCustomModelData(cigPackModelData + type);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyCigPack, PersistentDataType.BYTE, (byte)1);
        m.getPersistentDataContainer().set(keyCigPackType, PersistentDataType.INTEGER, type);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack buildCigarette(int amount, int type) {
        ItemStack it = new ItemStack(Material.STICK, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        String name;
        String lore;
        switch (type) {
            case CIG_CLASSIC -> { name = "🚬 Classic Red";
                lore = ChatColor.GRAY + "Обычная сигарета. Hunger + Weakness."; }
            case CIG_MENTHOL -> { name = "❄ Menthol Light";
                lore = ChatColor.GRAY + "Ментоловая. Слепота + Скорость."; }
            case CIG_GOLD    -> { name = "✨ Gold Filter";
                lore = ChatColor.GRAY + "Золотой фильтр. Регенерация + лёгкий яд."; }
            case CIG_CIGAR   -> { name = "🟫 Cigar Strong";
                lore = ChatColor.GRAY + "Крепкая сигара. Сила + Иссушение."; }
            default          -> { type = CIG_DIRT; name = "📜 Дешёвка";
                lore = ChatColor.GRAY + "Самокрутка. Яд, тошнота, медлительность."; }
        }
        m.setDisplayName(ChatColor.WHITE + name);
        m.setLore(List.of(
                lore,
                ChatColor.DARK_GRAY + "ПКМ — выкурить (3 сек)"));
        m.setCustomModelData(cigModelData + type);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyCig, PersistentDataType.BYTE, (byte)1);
        m.getPersistentDataContainer().set(keyCigType, PersistentDataType.INTEGER, type);
        it.setItemMeta(m);
        return it;
    }

    // ==================== КАЛЬЯН + ТАБАК (НОВАЯ СЕТКА) ====================
    private ItemStack buildHookahItem(int amount) {
        ItemStack it = new ItemStack(Material.BREWING_STAND, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.DARK_AQUA + "💨 Кальян");
        m.setLore(List.of(
                ChatColor.GRAY + "ПКМ по блоку — поставить кальян.",
                ChatColor.GRAY + "ПКМ с табаком — забить чашу.",
                ChatColor.GRAY + "ПКМ пустой рукой — затяжка (5 тяжек)."));
        m.setCustomModelData(hookahModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyHookah, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack buildTobacco(int amount, int type) {
        ItemStack it;
        String disp;
        List<String> lore;
        int cmd = tobaccoModelData + type;
        switch (type) {
            // === ДЕШЁВЫЕ / ВРЕДНЫЕ ===
            case TOBA_GARBAGE -> {
                it = new ItemStack(Material.BROWN_DYE, Math.max(1, amount));
                disp = "" + ChatColor.DARK_GRAY + ChatColor.BOLD + "Garbage Mix";
                lore = List.of(
                        ChatColor.GRAY + "Мусорная смесь.",
                        ChatColor.RED + "Poison II 10сек · Hunger III 5сек",
                        ChatColor.RED + "Mining Fatigue 10сек");
            }
            case TOBA_BURNT -> {
                it = new ItemStack(Material.GRAY_DYE, Math.max(1, amount));
                disp = "" + ChatColor.DARK_GRAY + ChatColor.BOLD + "Burnt Leaves";
                lore = List.of(
                        ChatColor.GRAY + "Жжёные листья.",
                        ChatColor.RED + "Wither I 8сек · Slowness II 5сек",
                        ChatColor.RED + "Blindness 3сек");
            }
            case TOBA_CHEMICAL -> {
                it = new ItemStack(Material.LIME_DYE, Math.max(1, amount));
                disp = "" + ChatColor.GREEN + ChatColor.BOLD + "Chemical Apple";
                lore = List.of(
                        ChatColor.GRAY + "Химическое яблоко.",
                        ChatColor.RED + "Poison I 15сек · Nausea 10сек");
            }
            // === СРЕДНИЕ ===
            case TOBA_DOUBLE_APPLE -> {
                it = new ItemStack(Material.RED_DYE, Math.max(1, amount));
                disp = "" + ChatColor.RED + ChatColor.BOLD + "Double Apple Classic";
                lore = List.of(
                        ChatColor.GRAY + "Двойное яблоко.",
                        ChatColor.GREEN + "Strength I 30сек",
                        ChatColor.RED + "Poison I 5сек");
            }
            case TOBA_GRAPE_MINT -> {
                it = new ItemStack(Material.PURPLE_DYE, Math.max(1, amount));
                disp = "" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "Grape Mint";
                lore = List.of(
                        ChatColor.GRAY + "Виноградная мята.",
                        ChatColor.GREEN + "Jump Boost II 20сек",
                        ChatColor.RED + "Hunger II 10сек");
            }
            case TOBA_BLUEBERRY -> {
                it = new ItemStack(Material.BLUE_DYE, Math.max(1, amount));
                disp = "" + ChatColor.BLUE + ChatColor.BOLD + "Blueberry Soda";
                lore = List.of(
                        ChatColor.GRAY + "Черничная сода.",
                        ChatColor.GREEN + "Speed II 20сек",
                        ChatColor.RED + "Wither I 3сек");
            }
            // === ДОРОГИЕ / ЧИСТЫЕ ===
            case TOBA_PEACH -> {
                it = new ItemStack(Material.PINK_DYE, Math.max(1, amount));
                disp = "" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "Premium Peach";
                lore = List.of(
                        ChatColor.GRAY + "Премиум персик.",
                        ChatColor.GREEN + "Regeneration II 10сек",
                        ChatColor.GREEN + "Saturation 5сек");
            }
            case TOBA_TANGIERS -> {
                it = new ItemStack(Material.ORANGE_DYE, Math.max(1, amount));
                disp = "" + ChatColor.GOLD + ChatColor.BOLD + "Tangiers Noir";
                lore = List.of(
                        ChatColor.GRAY + "Тёмный сорт.",
                        ChatColor.GREEN + "Strength II 45сек · Resistance I 30сек",
                        ChatColor.RED + "Poison I 2сек");
            }
            case TOBA_DIAMOND -> {
                it = new ItemStack(Material.LIGHT_BLUE_DYE, Math.max(1, amount));
                disp = "" + ChatColor.AQUA + ChatColor.BOLD + "Diamond Haze";
                lore = List.of(
                        ChatColor.GRAY + "Алмазный туман.",
                        ChatColor.GREEN + "Absorption III 30сек · Regen II 15сек",
                        ChatColor.GREEN + "Fire Resistance 30сек");
            }
            case TOBA_GODS -> {
                it = new ItemStack(Material.WHITE_DYE, Math.max(1, amount));
                disp = "" + ChatColor.YELLOW + ChatColor.BOLD + "God's Breath";
                lore = List.of(
                        ChatColor.GRAY + "Дыхание бога.",
                        ChatColor.GREEN + "Strength II · Speed II · Regen III · Night Vision",
                        ChatColor.GREEN + "Длительность 60сек · без негатива");
            }
            // === ДЕШЁВЫЕ / БРЕДОВЫЕ: галлюциногенные ===
            case TOBA_SHROOM -> {
                it = new ItemStack(Material.RED_DYE, Math.max(1, amount));
                disp = "" + ChatColor.RED + ChatColor.BOLD + "🍄 Mushroom Trip";
                lore = List.of(
                        ChatColor.GRAY + "Грибная смесь с мухоморами.",
                        ChatColor.LIGHT_PURPLE + "20 сек вокруг бегают фейковые",
                        ChatColor.LIGHT_PURPLE + "криперы и призраки (не настоящие).",
                        ChatColor.RED + "Nausea 20сек · Confusion · Weakness");
            }
            case TOBA_WARPED -> {
                it = new ItemStack(Material.PURPLE_DYE, Math.max(1, amount));
                disp = "" + ChatColor.DARK_PURPLE + ChatColor.BOLD + "🌀 Warped Fungi";
                lore = List.of(
                        ChatColor.GRAY + "Искажённые грибы Незера.",
                        ChatColor.LIGHT_PURPLE + "Телепортирует на 5-10 блоков в случайную сторону",
                        ChatColor.LIGHT_PURPLE + "каждые 3 секунды в течение 15 сек.",
                        ChatColor.RED + "Nausea 20сек · Slow Falling 15сек");
            }
            default -> {
                type = TOBA_GARBAGE;
                it = new ItemStack(Material.BROWN_DYE, Math.max(1, amount));
                disp = "" + ChatColor.DARK_GRAY + ChatColor.BOLD + "Garbage Mix";
                lore = List.of(ChatColor.GRAY + "Мусорная смесь.");
                cmd = tobaccoModelData + type;
            }
        }
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(disp);
        m.setLore(lore);
        m.setCustomModelData(cmd);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyTobacco, PersistentDataType.BYTE, (byte)1);
        m.getPersistentDataContainer().set(keyTobaccoType, PersistentDataType.INTEGER, type);
        it.setItemMeta(m);
        return it;
    }

    // ==================== ПЛУГ ====================
    private ItemStack buildPlow(int amount) {
        ItemStack it = new ItemStack(Material.DIAMOND_HOE, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.AQUA + "⛏ Плуг");
        m.setLore(List.of(
                ChatColor.GRAY + "Наденьте на голову (ПКМ в воздухе).",
                ChatColor.GRAY + "Вспахивает и пересаживает зрелый",
                ChatColor.GRAY + "урожай при ходьбе в радиусе 3×3.",
                ChatColor.DARK_GRAY + "Прочность: как у алмазной мотыги."));
        m.setCustomModelData(plowModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyPlow, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private boolean isPlow(ItemStack s) {
        return s != null && s.getType() == Material.DIAMOND_HOE && s.hasItemMeta()
                && s.getItemMeta().getPersistentDataContainer().has(keyPlow, PersistentDataType.BYTE);
    }
    private boolean isWearingPlow(Player p) {
        ItemStack helmet = p.getInventory().getHelmet();
        return isPlow(helmet);
    }

    private ItemStack buildPane(Material mat, ChatColor color, String name) {
        ItemStack it = new ItemStack(mat, 1);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(color + name);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }

    private boolean isMask(ItemStack s)         { return s != null && s.getType() == Material.PAPER && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyMask, PersistentDataType.BYTE); }
    private boolean isShears(ItemStack s)       { return s != null && s.getType() == Material.SHEARS && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyShears, PersistentDataType.BYTE); }
    private boolean isDynamite(ItemStack s)     { return s != null && s.getType() == Material.TNT && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyDynamite, PersistentDataType.BYTE); }
    private boolean isFireballItem(ItemStack s) { return s != null && s.getType() == Material.FIRE_CHARGE && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyFireball, PersistentDataType.BYTE); }
    private boolean isSmokeItem(ItemStack s)    { return s != null && s.getType() == Material.GUNPOWDER && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keySmoke, PersistentDataType.BYTE); }
    private boolean isGrenadeItem(ItemStack s)  { return s != null && s.getType() == Material.CLAY_BALL && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyGrenade, PersistentDataType.BYTE); }
    private boolean isStickyItem(ItemStack s)   { return s != null && s.getType() == Material.SLIME_BALL && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keySticky, PersistentDataType.BYTE); }
    private boolean isStunGrenade(ItemStack s)  { return s != null && s.getType() == Material.FIREWORK_STAR && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyStunGrenade, PersistentDataType.BYTE); }
    private boolean isHookahItem(ItemStack s)   { return s != null && s.getType() == Material.BREWING_STAND && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyHookah, PersistentDataType.BYTE); }
    // Табаки: используем разные красители по категориям (дешёвый=коричневый, средний=жёлтый/оранж/фиолет, дорогой=розовый/чёрный/голубой/белый)
    private boolean isTobaccoItem(ItemStack s)  { return s != null && isTobaccoMaterial(s.getType()) && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyTobacco, PersistentDataType.BYTE); }
    private boolean isTobaccoMaterial(Material m) {
        return m == Material.BROWN_DYE || m == Material.GRAY_DYE || m == Material.BLACK_DYE
                || m == Material.RED_DYE || m == Material.PURPLE_DYE || m == Material.BLUE_DYE
                || m == Material.PINK_DYE || m == Material.ORANGE_DYE || m == Material.LIGHT_BLUE_DYE || m == Material.WHITE_DYE;
    }
    private boolean isCigPack(ItemStack s)      { return s != null && s.getType() == Material.PAPER && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyCigPack, PersistentDataType.BYTE); }
    private boolean isCigarette(ItemStack s)    { return s != null && s.getType() == Material.STICK && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyCig, PersistentDataType.BYTE); }
    private boolean isVaccine(ItemStack s)      { return s != null && s.getType() == Material.POTION && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyVaccine, PersistentDataType.BYTE); }
    private boolean isSlobber(ItemStack s)      { return s != null && s.getType() == Material.FERMENTED_SPIDER_EYE && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keySlobber, PersistentDataType.BYTE); }
    private boolean isHomingBow(ItemStack s)    { return s != null && s.getType() == Material.BOW && s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(keyHomingBow, PersistentDataType.BYTE); }

    private int getHomingMode(ItemStack s) {
        if (!isHomingBow(s)) return HOMING_MODE_PLAYERS;
        return s.getItemMeta().getPersistentDataContainer().getOrDefault(keyHomingMode, PersistentDataType.INTEGER, HOMING_MODE_PLAYERS);
    }

    private int getDynMode(ItemStack s)        { return isDynamite(s) ? s.getItemMeta().getPersistentDataContainer().getOrDefault(keyDynMode, PersistentDataType.INTEGER, DYN_SHORT) : DYN_SHORT; }
    private int getGrenadeItemMode(ItemStack s){ return isGrenadeItem(s) ? s.getItemMeta().getPersistentDataContainer().getOrDefault(keyGrenadeMode, PersistentDataType.INTEGER, SMOKE_SHORT) : SMOKE_SHORT; }
    private int getStickyMode(ItemStack s)     { return isStickyItem(s) ? s.getItemMeta().getPersistentDataContainer().getOrDefault(keyStickyMode, PersistentDataType.INTEGER, STICKY_SPEED_SHORT) : STICKY_SPEED_SHORT; }
    private int getTobaccoType(ItemStack s)    { return isTobaccoItem(s) ? s.getItemMeta().getPersistentDataContainer().getOrDefault(keyTobaccoType, PersistentDataType.INTEGER, TOBA_GARBAGE) : TOBA_GARBAGE; }

    private boolean isTacticItem(ItemStack s) {
        return isMask(s) || isShears(s) || isDynamite(s) || isFireballItem(s) || isSmokeItem(s)
                || isGrenadeItem(s) || isStickyItem(s) || isHookahItem(s) || isTobaccoItem(s)
                || isPlow(s) || isCigPack(s) || isCigarette(s) || isVaccine(s) || isSlobber(s)
                || isStunGrenade(s) || isFreezeGrenade(s) || isHomingBow(s) || isLeashItem(s) || isBonfireItem(s) || isRegionTool(s);
    }

    private ItemStack buildHomingBow(int amount, int mode) {
        ItemStack it = new ItemStack(Material.BOW, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        String name, color;
        switch (mode) {
            case HOMING_MODE_ALL ->  { name = "все"; color = ChatColor.GOLD.toString(); }
            case HOMING_MODE_MOBS -> { name = "мобы"; color = ChatColor.RED.toString(); }
            default               -> { name = "игроки"; color = ChatColor.AQUA.toString(); mode = HOMING_MODE_PLAYERS; }
        }
        m.setDisplayName(ChatColor.LIGHT_PURPLE + "🏹 Лук-самонавод");
        m.setLore(List.of(
                ChatColor.GRAY + "Стрела сама летит в захваченную цель.",
                ChatColor.GRAY + "Зажми ПКМ, наведи на цель 1.5 сек — цель захвачена.",
                ChatColor.GRAY + "Захват держится постоянно. Shift+ПКМ — сбросить.",
                ChatColor.DARK_GRAY + "Shift+ЛКМ — режим цели",
                ChatColor.DARK_GRAY + "Цели: " + color + name));
        m.setCustomModelData(hbowModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyHomingBow, PersistentDataType.BYTE, (byte)1);
        m.getPersistentDataContainer().set(keyHomingMode, PersistentDataType.INTEGER, mode);
        if (m instanceof org.bukkit.inventory.meta.Damageable d) d.setDamage(0);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack buildLeash(int amount) {
        ItemStack it = new ItemStack(Material.LEAD, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.GOLD + "🪢 Поводок для игроков");
        m.setLore(List.of(
                ChatColor.GRAY + "ПКМ по игроку (в 4 блоках) — мгновенно связать.",
                ChatColor.GRAY + "ЛКМ по блоку — привязать к блоку.",
                ChatColor.GRAY + "Shift+ПКМ — отпустить поводок.",
                ChatColor.GRAY + "Жертва может вырваться после 3 мин через 250 ПКМ."));
        m.setCustomModelData(leashModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyLeash, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private boolean isLeashItem(ItemStack s) {
        return s != null && s.getType() == Material.LEAD && s.hasItemMeta()
                && s.getItemMeta().getPersistentDataContainer().has(keyLeash, PersistentDataType.BYTE);
    }

    // ==================== РИТУАЛЬНЫЙ КОСТЁР ====================
    private ItemStack buildBonfire(int amount) {
        ItemStack it = new ItemStack(Material.CAMPFIRE, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.GOLD + "🔥 Ритуальный костёр");
        m.setLore(List.of(
                ChatColor.GRAY + "Поставьте и подпитывайте древесиной.",
                ChatColor.GRAY + "При угасании налетит орда нежити!",
                ChatColor.DARK_GRAY + "Топливо: любые брёвна/доски/палки"
        ));
        m.setCustomModelData(bonfireModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyBonfire, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private boolean isBonfireItem(ItemStack s) {
        return s != null && s.getType() == Material.CAMPFIRE && s.hasItemMeta()
                && s.getItemMeta().getPersistentDataContainer().has(keyBonfire, PersistentDataType.BYTE);
    }

    // ==================== ЖЕЗЛ ТЕРРИТОРИЙ ====================
    private ItemStack buildRegionTool(int amount) {
        ItemStack it = new ItemStack(Material.GRAY_DYE, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.DARK_AQUA + "🗺 Жезл территорий");
        m.setLore(List.of(
                ChatColor.GRAY + "ЛКМ по блоку — точка 1",
                ChatColor.GRAY + "ПКМ по блоку — точка 2",
                ChatColor.GRAY + "/region create <название> — создать регион",
                ChatColor.GRAY + "Поддерживает цвета §x§R§R§G§G§B§B и градиенты."
        ));
        m.setCustomModelData(regionModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyRegionTool, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private boolean isRegionTool(ItemStack s) {
        return s != null && s.getType() == Material.GRAY_DYE && s.hasItemMeta()
                && s.getItemMeta().getPersistentDataContainer().has(keyRegionTool, PersistentDataType.BYTE);
    }

    private ItemStack buildHomingArrow(int amount) {
        return new ItemStack(Material.ARROW, Math.max(1, amount));
    }

    // ==================== ВИРУС МЯУКАНЬЯ: БИЛДЕРЫ ПРЕДМЕТОВ ====================
    private ItemStack buildVaccine(int amount) {
        ItemStack it = new ItemStack(Material.POTION, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.AQUA + "💉 Вакцина от мяуканья");
        m.setLore(List.of(
                ChatColor.GRAY + "Полностью излечивает вирус мяуканья.",
                ChatColor.DARK_GRAY + "ПКМ — вколоть себе вакцину."));
        m.setCustomModelData(vaccineModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keyVaccine, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack buildSlobber(int amount) {
        ItemStack it = new ItemStack(Material.FERMENTED_SPIDER_EYE, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.LIGHT_PURPLE + "🐱 Слюни кота");
        m.setLore(List.of(
                ChatColor.GRAY + "Если выпить — заражаешься 1-й стадией мяуканья.",
                ChatColor.DARK_GRAY + "ПКМ — съесть."));
        m.setCustomModelData(slobberModelData);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(keySlobber, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m);
        return it;
    }

    private int getCigType(ItemStack s) {
        if (!isCigarette(s) && !isCigPack(s)) return CIG_DIRT;
        var pdc = s.getItemMeta().getPersistentDataContainer();
        NamespacedKey k = isCigarette(s) ? keyCigType : keyCigPackType;
        return pdc.getOrDefault(k, PersistentDataType.INTEGER, CIG_DIRT);
    }

    private void consumeHand(Player p) {
        if (p.getGameMode() == GameMode.CREATIVE) return;
        ItemStack m = p.getInventory().getItemInMainHand();
        if (isTacticItem(m)) {
            if (m.getAmount() > 1) m.setAmount(m.getAmount() - 1);
            else p.getInventory().setItemInMainHand(null);
            return;
        }
        ItemStack o = p.getInventory().getItemInOffHand();
        if (isTacticItem(o)) {
            if (o.getAmount() > 1) o.setAmount(o.getAmount() - 1);
            else p.getInventory().setItemInOffHand(null);
        }
    }

    private void giveToPlayer(CommandSender sender, Player target, ItemStack item, int amount, String itemName) {
        target.getInventory().addItem(item).values().forEach(lo -> target.getWorld().dropItemNaturally(target.getLocation(), lo));
        if (sender == target) sender.sendMessage(ChatColor.GREEN + "Вы получили x" + amount + " §r" + itemName);
        else {
            sender.sendMessage(ChatColor.GREEN + "Выдано x" + amount + " §r" + itemName + " §aигроку " + target.getName());
            target.sendMessage(ChatColor.GREEN + "Вам выдали x" + amount + " §r" + itemName);
        }
    }

    // ==================== МЕНЮ (КАТЕГОРИИ) ====================
    private static final String MAIN_MENU = "main";
    private static final String CAT_COMBAT = "combat";
    private static final String CAT_GRENADES = "grenades";
    private static final String CAT_TOOLS = "tools";
    private static final String CAT_HOOKAH = "hookah";
    private static final String CAT_CIGS = "cigs";
    private static final String CAT_MEOW = "meow";
    private static final String CAT_QUARANTINE = "quarantine";
    // Трекер открытой категории по игроку
    private final Map<UUID, String> openCategory = new HashMap<>();

    private void openMenu(Player p) { openCategory(p, MAIN_MENU); }

    private void openCategory(Player p, String cat) {
        Inventory inv = Bukkit.createInventory(null, MENU_SIZE, MENU_TITLE);
        for (int i = 0; i < MENU_SIZE; i++) inv.setItem(i, buildPane(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY, " "));
        // Кнопки категорий в нижнем ряду (слоты 45-53)
        inv.setItem(45, buildCategoryIcon(Material.IRON_SWORD, ChatColor.RED, "⚔ Боевые", CAT_COMBAT.equals(cat)));
        inv.setItem(46, buildCategoryIcon(Material.TNT, ChatColor.DARK_RED, "💣 Гранаты", CAT_GRENADES.equals(cat)));
        inv.setItem(47, buildCategoryIcon(Material.DIAMOND_HOE, ChatColor.AQUA, "⛏ Инструменты", CAT_TOOLS.equals(cat)));
        inv.setItem(48, buildCategoryIcon(Material.BREWING_STAND, ChatColor.DARK_AQUA, "💨 Кальян", CAT_HOOKAH.equals(cat)));
        inv.setItem(49, buildPane(Material.BARRIER, ChatColor.RED, "Закрыть"));
        inv.setItem(50, buildCategoryIcon(Material.PAPER, ChatColor.GRAY, "🚬 Сигареты", CAT_CIGS.equals(cat)));
        inv.setItem(51, buildCategoryIcon(Material.WHITE_WOOL, ChatColor.LIGHT_PURPLE, "🐱 Мяу-вирус", CAT_MEOW.equals(cat)));
        inv.setItem(53, buildCategoryIcon(Material.PLAYER_HEAD, ChatColor.DARK_PURPLE, "☣ Карантин", CAT_QUARANTINE.equals(cat)));
        inv.setItem(52, buildPane(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY, " "));

        // Заполняем содержимое выбранной категории
        switch (cat) {
            case CAT_COMBAT -> {
                inv.setItem(4, buildPane(Material.NAME_TAG, ChatColor.RED, "Боевые предметы"));
                inv.setItem(19, buildMask(1));
                inv.setItem(20, buildShears(1));
                inv.setItem(21, buildFireball(1));
                inv.setItem(23, buildHomingBow(1, HOMING_MODE_PLAYERS));
                inv.setItem(24, buildLeash(1));
            }
            case CAT_GRENADES -> {
                inv.setItem(4, buildPane(Material.NAME_TAG, ChatColor.DARK_RED, "Гранаты (режимы — Shift+ЛКМ в руках)"));
                inv.setItem(19, buildDynamite(1, DYN_SHORT));
                inv.setItem(20, buildSticky(1, STICKY_SPEED_SHORT));
                inv.setItem(21, buildSmoke(1));
                inv.setItem(23, buildClusterGrenade(1, SMOKE_SHORT));
                inv.setItem(24, buildStunGrenade(1));
                inv.setItem(25, buildFreezeGrenade(1));
            }
            case CAT_TOOLS -> {
                inv.setItem(4, buildPane(Material.NAME_TAG, ChatColor.AQUA, "Инструменты"));
                inv.setItem(19, buildBonfire(1));
                inv.setItem(21, buildPlow(1));
                inv.setItem(23, buildRegionTool(1));
            }
            case CAT_HOOKAH -> {
                inv.setItem(4, buildPane(Material.NAME_TAG, ChatColor.DARK_AQUA, "Кальян и табаки"));
                inv.setItem(22, buildHookahItem(1));
                // === 10 табаков по кругу вокруг кальяна ===
                // Дешёвые (3 шт)
                inv.setItem(19, buildTobacco(1, TOBA_GARBAGE));
                inv.setItem(20, buildTobacco(1, TOBA_BURNT));
                inv.setItem(21, buildTobacco(1, TOBA_CHEMICAL));
                // Средние (3 шт)
                inv.setItem(23, buildTobacco(1, TOBA_DOUBLE_APPLE));
                inv.setItem(24, buildTobacco(1, TOBA_GRAPE_MINT));
                inv.setItem(25, buildTobacco(1, TOBA_BLUEBERRY));
                // Дорогие / элитные (4 шт)
                inv.setItem(29, buildTobacco(1, TOBA_PEACH));
                inv.setItem(30, buildTobacco(1, TOBA_TANGIERS));
                inv.setItem(31, buildTobacco(1, TOBA_DIAMOND));
                inv.setItem(32, buildTobacco(1, TOBA_GODS));
                // Бредовые / галлюциногенные (2 шт)
                inv.setItem(38, buildTobacco(1, TOBA_SHROOM));
                inv.setItem(42, buildTobacco(1, TOBA_WARPED));
            }
            case CAT_CIGS -> {
                inv.setItem(4, buildPane(Material.NAME_TAG, ChatColor.GRAY, "Сигареты (пачки)"));
                inv.setItem(20, buildCigPack(1, CIG_DIRT));
                inv.setItem(22, buildCigPack(1, CIG_CLASSIC));
                inv.setItem(24, buildCigPack(1, CIG_MENTHOL));
                inv.setItem(30, buildCigPack(1, CIG_GOLD));
                inv.setItem(32, buildCigPack(1, CIG_CIGAR));
            }
            case CAT_MEOW -> {
                inv.setItem(4, buildPane(Material.NAME_TAG, ChatColor.LIGHT_PURPLE, "🐱 Вирус мяуканья"));
                inv.setItem(22, buildVaccine(1));
                inv.setItem(24, buildSlobber(1));
                // Описание
                inv.setItem(31, buildInfoHead(ChatColor.LIGHT_PURPLE + "Как заразиться?",
                        List.of(ChatColor.GRAY + "Стоять в 3 блоках от больного.",
                                ChatColor.GRAY + "Маска 🎭 снижает шанс до 1%.",
                                ChatColor.GRAY + "10 стадий — мяуканье в речи.")));
            }
            case CAT_QUARANTINE -> {
                fillQuarantineMenu(inv);
            }
            default -> {
                // Главная — приветственное окно
                inv.setItem(4, buildPane(Material.NAME_TAG, ChatColor.GOLD, "Tactic · Выберите категорию"));
                inv.setItem(22, buildInfoHead(ChatColor.GOLD + "Tactic 26.2",
                        List.of(ChatColor.GRAY + "Нажимайте иконки внизу,",
                                ChatColor.GRAY + "чтобы открыть раздел.",
                                ChatColor.GRAY + "Все предметы выдаются по 1 шт.")));
            }
        }
        // Помечаем игрока: сейчас сами переключаем меню — onMenuClose не должен сбрасывать записи
        menuSwitching.add(p.getUniqueId());
        try {
            p.openInventory(inv);
        } finally {
            Bukkit.getScheduler().runTaskLater(this, () -> menuSwitching.remove(p.getUniqueId()), 1L);
        }
        // Регистрируем ПОСЛЕ openInventory, чтобы InventoryCloseEvent от ПРЕДЫДУЩЕГО меню
        // не стёр только что записанные значения
        openCategory.put(p.getUniqueId(), cat);
        openMenus.put(p.getUniqueId(), inv);
        if (cat.equals(CAT_QUARANTINE)) openQuarantine.put(p.getUniqueId(), inv);
    }

    private ItemStack buildCategoryIcon(Material mat, ChatColor color, String name, boolean selected) {
        ItemStack it = new ItemStack(mat, 1);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName((selected ? ChatColor.WHITE + "▶ " : "") + color + name);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(m);
        return it;
    }
    private ItemStack buildInfoHead(String title, List<String> lore) {
        ItemStack it = new ItemStack(Material.PAPER, 1);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(title);
        List<String> loreFmt = new ArrayList<>();
        for (String s : lore) loreFmt.add(s);
        m.setLore(loreFmt);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }
    private ItemStack buildQuarantineHead(Player target) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        if (head.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta sm) {
            sm.setOwningPlayer(target);
            int st = getMeowStage(target);
            StringBuilder bar = new StringBuilder("§7[§r");
            for (int i = 0; i < MEOW_MAX_STAGE; i++) bar.append(i < st ? "§c▮§r" : "§7▯§r");
            bar.append("§7]§r");
            sm.setDisplayName(ChatColor.LIGHT_PURPLE + "🐱 " + target.getName());
            sm.setLore(List.of(
                    ChatColor.GRAY + "Стадия: " + ChatColor.RED + st + "/10",
                    bar.toString(),
                    ChatColor.GRAY + (meowEnabled ? "§aВирус активен" : "§cВирус выключен"),
                    ChatColor.DARK_GRAY + "ЛКМ — вылечить",
                    ChatColor.DARK_GRAY + "ПКМ — заразить на 10 стадию"));
            head.setItemMeta(sm);
        }
        return head;
    }

    /** Заполняет карантинное меню головами заражённых онлайн-игроков */
    private void fillQuarantineMenu(Inventory inv) {
        inv.setItem(4, buildPane(Material.PLAYER_HEAD, ChatColor.DARK_PURPLE, "☣ Карантин · Заражено: " + meowStage.size()));
        int slot = 19;
        List<Player> sick = new ArrayList<>(Bukkit.getOnlinePlayers());
        sick.removeIf(pl -> getMeowStage(pl) <= 0);
        sick.sort(Comparator.comparingInt((Player pl) -> -getMeowStage(pl)));
        for (Player pl : sick) {
            if (slot > 43) break;
            inv.setItem(slot++, buildQuarantineHead(pl));
        }
        // Кнопка вылечить всех
        inv.setItem(49, buildPane(Material.SPLASH_POTION, ChatColor.GREEN, "💉 ВЫЛЕЧИТЬ ВСЕХ"));
    }

    /** Периодическое обновление карантинного меню (чтобы стадии менялись в реальном времени) */
    private void refreshQuarantineMenu() {
        for (Map.Entry<UUID, Inventory> e : openQuarantine.entrySet()) {
            Player viewer = Bukkit.getPlayer(e.getKey());
            if (viewer == null || !viewer.isOnline()) { openQuarantine.remove(e.getKey()); continue; }
            fillQuarantineMenu(e.getValue());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory top = p.getOpenInventory().getTopInventory();
        if (top == null) return;
        // Проверяем "наше" меню: либо инвентарь отслеживается в openMenus/openQuarantine,
        // либо тайтл/размер совпадает с MENU_TITLE (на случай если ссылка в мапе устарела).
        boolean ours = openMenus.containsValue(top) || openQuarantine.containsValue(top);
        if (!ours && top.getSize() == MENU_SIZE && top.getHolder() == null) {
            String title = "";
            try {
                title = ChatColor.stripColor(
                        net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                                .serialize(p.getOpenInventory().title()));
            } catch (Throwable ignored) {}
            if (title.startsWith("Выдача предметов Tactic")) ours = true;
        }
        if (!ours) return;
        // ОТМЕНЯЕМ любые действия с нашим меню ВСЕГДА — включая клики по нижнему инвентарю
        // (чтобы нельзя было переложить вещи через Shift/цифру)
        e.setCancelled(true);
        // Клики по СВОЕМУ инвентарю (нижняя часть) — игнорируем после отмены
        if (e.getClickedInventory() == null || e.getClickedInventory() != top) return;
        int slot = e.getSlot();
        if (slot < 0 || slot >= MENU_SIZE) return;
        String cat = openCategory.getOrDefault(p.getUniqueId(), MAIN_MENU);

        // ВСЕГДА синхронизируем openMenus с актуальным инвентарём
        openMenus.put(p.getUniqueId(), top);

        // Обработка карантинного меню (должно быть открыто в CAT_QUARANTINE)
        if (cat.equals(CAT_QUARANTINE)) {
            if (slot == 49) {
                if (!p.hasPermission("tactic.meow.admin")) { p.sendMessage(msg("no-permission")); return; }
                clearAllMeow();
                p.playSound(p.getLocation(), Sound.ITEM_BOTTLE_EMPTY, 1f, 1f);
                p.sendMessage(ChatColor.GREEN + "💉 Все вылечены.");
                openCategory(p, CAT_QUARANTINE);
                return;
            }
            ItemStack clicked = top.getItem(slot);
            if (clicked != null && clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta sm && sm.getOwningPlayer() != null) {
                if (!p.hasPermission("tactic.meow.admin")) { p.sendMessage(msg("no-permission")); return; }
                Player target = (Player) sm.getOwningPlayer();
                if (!target.isOnline()) return;
                if (e.isLeftClick()) {
                    cureMeow(target, true);
                    target.playSound(target.getLocation(), Sound.ITEM_BOTTLE_EMPTY, 1f, 1f);
                    p.sendMessage(ChatColor.GREEN + "💉 " + target.getName() + " вылечен.");
                } else if (e.isRightClick()) {
                    setMeowStage(target, MEOW_MAX_STAGE);
                    target.sendMessage(ChatColor.LIGHT_PURPLE + "😿 Вам выставили 10 стадию мяуканья.");
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "🐱 " + target.getName() + " теперь на 10 стадии.");
                }
                openCategory(p, CAT_QUARANTINE);
                return;
            }
        }

        // Нижний ряд — переключение категорий
        switch (slot) {
            case 45 -> { openCategory(p, CAT_COMBAT); return; }
            case 46 -> { openCategory(p, CAT_GRENADES); return; }
            case 47 -> { openCategory(p, CAT_TOOLS); return; }
            case 48 -> { openCategory(p, CAT_HOOKAH); return; }
            case 49 -> { p.closeInventory(); return; }
            case 50 -> { openCategory(p, CAT_CIGS); return; }
            case 51 -> { openCategory(p, CAT_MEOW); return; }
            case 53 -> {
                if (p.hasPermission("tactic.meow.admin")) {
                    openCategory(p, CAT_QUARANTINE);
                    openQuarantine.put(p.getUniqueId(), top);
                } else {
                    p.sendMessage(ChatColor.RED + "Нужны права tactic.meow.admin");
                }
                return;
            }
        }

        // Выдача предметов по слотам
        if (cat.equals(CAT_COMBAT)) {
            switch (slot) {
                case 19 -> giveItem(p, buildMask(1), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.5f, "🎭 Маску");
                case 20 -> giveItem(p, buildShears(1), Sound.ENTITY_SHEEP_SHEAR, 1.5f, "✂ Ножницы");
                case 21 -> giveItem(p, buildFireball(1), Sound.ITEM_FIRECHARGE_USE, 1.2f, "🔥 Фаербол");
                case 23 -> giveItem(p, buildHomingBow(1, HOMING_MODE_PLAYERS), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 1.3f, "🏹 Лук-самонавод");
                case 24 -> { p.getInventory().addItem(buildLeash(1));
                        playSoundSafe(p.getWorld(), p.getLocation(), 1.0f, 1.0f,
                                "ITEM_LEAD_PLACE", "BLOCK_LEASH_KNOT_PLACE", "ENTITY_LEASH_KNOT_PLACE");
                        p.sendMessage(ChatColor.GREEN + "Вы получили 🪢 Поводок"); }
            }
        } else if (cat.equals(CAT_GRENADES)) {
            switch (slot) {
                case 19 -> giveItem(p, buildDynamite(1, DYN_SHORT), Sound.ENTITY_TNT_PRIMED, 1.2f, "🧨 Динамит");
                case 20 -> giveItem(p, buildSticky(1, STICKY_SPEED_SHORT), Sound.BLOCK_SLIME_BLOCK_PLACE, 1.2f, "🟢 Липучку");
                case 21 -> giveItem(p, buildSmoke(1), Sound.BLOCK_FIRE_EXTINGUISH, 1.2f, "💨 Дымовую шашку");
                case 23 -> giveItem(p, buildClusterGrenade(1, SMOKE_SHORT), Sound.ENTITY_SPLASH_POTION_BREAK, 0.8f, "💣 Кассетную гранату");
                case 24 -> giveItem(p, buildStunGrenade(1), Sound.ENTITY_GENERIC_EXPLODE, 1.3f, "💥 Оглушающую гранату");
                case 25 -> giveItem(p, buildFreezeGrenade(1), Sound.BLOCK_GLASS_BREAK, 1.2f, "🧊 Крио-гранату");
            }
        } else if (cat.equals(CAT_TOOLS)) {
            if (slot == 19) giveItem(p, buildBonfire(1), Sound.ITEM_FIRECHARGE_USE, 1.0f, "🔥 Ритуальный костёр");
            if (slot == 21) giveItem(p, buildPlow(1), Sound.ITEM_HOE_TILL, 1.2f, "⛏ Плуг");
            if (slot == 23) giveItem(p, buildRegionTool(1), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, "🗺 Жезл территорий");
        } else if (cat.equals(CAT_HOOKAH)) {
            if (slot == 22) giveItem(p, buildHookahItem(1), Sound.BLOCK_BREWING_STAND_BREW, 1.0f, "💨 Кальян");
            Map<Integer, Integer> tob = new HashMap<>();
            tob.put(19, TOBA_GARBAGE);
            tob.put(20, TOBA_BURNT);
            tob.put(21, TOBA_CHEMICAL);
            tob.put(23, TOBA_DOUBLE_APPLE);
            tob.put(24, TOBA_GRAPE_MINT);
            tob.put(25, TOBA_BLUEBERRY);
            tob.put(29, TOBA_PEACH);
            tob.put(30, TOBA_TANGIERS);
            tob.put(31, TOBA_DIAMOND);
            tob.put(32, TOBA_GODS);
            tob.put(38, TOBA_SHROOM);
            tob.put(42, TOBA_WARPED);
            if (tob.containsKey(slot)) giveItem(p, buildTobacco(1, tob.get(slot)), Sound.ITEM_CROP_PLANT, 1.0f, "табак " + tobaccoName(tob.get(slot)));
        } else if (cat.equals(CAT_CIGS)) {
            Map<Integer, Integer> cigs = new HashMap<>();
            String[] names = {"📜 Дешёвку", "🚬 Classic Red", "❄ Menthol Light", "✨ Gold Filter", "🟫 Cigar Strong"};
            cigs.put(20, CIG_DIRT);
            cigs.put(22, CIG_CLASSIC);
            cigs.put(24, CIG_MENTHOL);
            cigs.put(30, CIG_GOLD);
            cigs.put(32, CIG_CIGAR);
            if (cigs.containsKey(slot)) {
                int t = cigs.get(slot);
                int idx = t; // 0..4 совпадает с порядком
                giveItem(p, buildCigPack(1, t), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, names[idx]);
            }
        } else if (cat.equals(CAT_MEOW)) {
            if (slot == 22) giveItem(p, buildVaccine(1), Sound.ITEM_BOTTLE_FILL, 1.2f, "💉 Вакцину");
            if (slot == 24) giveItem(p, buildSlobber(1), Sound.ENTITY_CAT_AMBIENT, 0.8f, "🐱 Слюни кота");
        }
    }
    private void giveItem(Player p, ItemStack stack, Sound sound, float pitch, String name) {
        p.getInventory().addItem(stack).values().forEach(lo -> p.getWorld().dropItemNaturally(p.getLocation(), lo));
        p.playSound(p.getLocation(), sound, 0.8f, pitch);
        p.sendMessage(ChatColor.GREEN + "Вы получили " + name);
    }

    @EventHandler public void onMenuClose(InventoryCloseEvent e) {
        // Если мы сами сейчас переключаем меню — не сбрасываем записи
        // (InventoryCloseEvent приходит внутри openInventory() синхронно от старого меню)
        if (menuSwitching.contains(e.getPlayer().getUniqueId())) return;
        openMenus.remove(e.getPlayer().getUniqueId());
        openQuarantine.remove(e.getPlayer().getUniqueId());
        openCategory.remove(e.getPlayer().getUniqueId());
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void onMenuDrag(InventoryDragEvent e)  {
        // Отменяем любое перетаскивание В НАШЕМ меню (верхний инвентарь размером MENU_SIZE)
        Inventory top = e.getView().getTopInventory();
        boolean ours = openMenus.containsValue(e.getInventory()) || openQuarantine.containsValue(e.getInventory())
                || (top != null && top.getSize() == MENU_SIZE && top.getHolder() == null);
        if (ours) e.setCancelled(true);
    }

    // ==================== МАСКА: ИСПОЛЬЗОВАНИЕ ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMaskUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack main = p.getInventory().getItemInMainHand();
        if (!isMask(main)) return;
        if (!e.getAction().name().startsWith("RIGHT_CLICK")) return;
        e.setCancelled(true);
        if (isMasked(p)) { p.sendMessage(msg("already-masked")); return; }
        applyMask(p);
        consumeHand(p);
        p.sendMessage(msg("mask-activated"));
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
    }

    // ==================== НОЖНИЦЫ ====================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShearsHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player target)) return;
        Player attacker = null;
        if (e.getDamager() instanceof Player) attacker = (Player) e.getDamager();
        else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player) attacker = (Player) pr.getShooter();
        if (attacker == null) return;
        if (!isShears(attacker.getInventory().getItemInMainHand())) return;
        e.setCancelled(true);
        if (tearMask(attacker, target)) {
            consumeHand(attacker);
            target.playSound(target.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1f, 1f);
            attacker.playSound(target.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1f, 1f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShearsRightClick(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player target)) return;
        Player p = e.getPlayer();
        if (isShears(p.getInventory().getItemInMainHand())) {
            e.setCancelled(true);
            if (tearMask(p, target)) {
                consumeHand(p);
                target.playSound(target.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1f, 1f);
                p.playSound(target.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1f, 1f);
            }
            return;
        }
        if (isMasked(target)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void blockRightClickOnMasked(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player target)) return;
        if (e.getPlayer().equals(target)) return;
        if (isMasked(target) && !isShears(e.getPlayer().getInventory().getItemInMainHand())) e.setCancelled(true);
    }

    private boolean tearMask(Player attacker, Player target) {
        if (!isMasked(target)) { attacker.sendMessage(msg("shears-on-non-masked")); return false; }
        if (target.hasPermission("tactic.mask.protected")) { attacker.sendMessage(msg("protected")); return false; }
        removeMask(target, true, RemoveReason.SHEARS);
        attacker.sendMessage(msg("mask-torn-by-you").replace("%player%", maskName));
        return true;
    }

    // ==================== ЧАТ (объединённый рендерер: маска + искажение мяу-вирусом) ====================
    // Ник НЕ перекрашиваем — оставляем как есть (displayName), с префиксами/цветами из других плагинов.
    // Только когда на игроке маска — вместо его displayName показываем серый "????".
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        boolean masked = isMasked(p);
        int meow = getMeowStage(p);
        final Component nickComponent = masked
                ? Component.text(maskName, NamedTextColor.GRAY)
                : p.displayName();
        ChatRenderer r = (source, dn, msg, viewer) -> {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(msg);
            String distorted = (meow > 0 && meowEnabled) ? distortMessage(plain, meow) : plain;
            // Скобки и сообщение — обычные БЕЛЫЕ (как в дефолтном чате). Ник перекрашиваем только при маске.
            return Component.text("<", NamedTextColor.WHITE)
                    .append(nickComponent)
                    .append(Component.text("> ", NamedTextColor.WHITE))
                    .append(Component.text(distorted, NamedTextColor.WHITE));
        };
        e.renderer(r);
    }

    @SuppressWarnings({"deprecation", "removal"})
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChatLegacy(org.bukkit.event.player.AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        boolean masked = isMasked(p);
        int meow = getMeowStage(p);
        String text = e.getMessage();
        if (meow > 0 && meowEnabled) text = distortMessage(text, meow);
        e.setMessage(text);
        if (masked) {
            // При маске — серый "????", скобки и сообщение БЕЛЫЕ (стандартный вид чата)
            e.setFormat(ChatColor.WHITE + "<" + ChatColor.GRAY + maskName.replace("%","%%") + ChatColor.WHITE + "> " + ChatColor.WHITE + "%2$s");
        } else {
            // Без маски — белые скобки, родной ник и белое сообщение (как обычно)
            e.setFormat(ChatColor.WHITE + "<%1$s" + ChatColor.WHITE + "> " + ChatColor.WHITE + "%2$s");
        }
    }

    // ==================== ВХОД/ВЫХОД/КИК/АДВАНС ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        // Загружаем сохранённую в PDC стадию мяу-вируса при входе
        loadMeowStage(p);
        if (isMasked(p)) {
            p.setDisplayName(maskColored); p.setPlayerListName(maskColored);
            getHideTeam().addEntry(p.getName());
            if (e.getJoinMessage() != null) e.joinMessage(Component.text(maskColored + "§e вошёл в игру"));
        }
        enforceMask();
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uid = p.getUniqueId();
        stopSmoke(uid);
        nicotineHits.remove(uid);
        overdoseDeaths.remove(uid);
        hookahCooldown.remove(uid);
        plowLastPos.remove(uid);
        openMenus.remove(uid);
        openQuarantine.remove(uid);
        openCategory.remove(uid);
        // In-memory состояние вируса сбрасываем при выходе; при новом входе стадия
        // подгрузится из PDC в loadMeowStage(). Иммунитет тоже сбрасываем (5-минутный
        // иммунитет после вакцины не переживает релог, чтобы не хранить вечный мусор).
        meowStage.remove(uid);
        meowLastSound.remove(uid);
        meowLastChat.remove(uid);
        meowLastCough.remove(uid);
        meowImmunityUntil.remove(uid);
        // Поводок: если вышедший был на поводке у кого-то — обрываем; если у вышедшего кто-то на поводке — тоже
        LeashTie own = leashTies.remove(uid);
        if (own != null) leashEscapeClicks.remove(own.victim);
        LeashTie onUs = findLeashOnVictim(uid);
        if (onUs != null) {
            Player owner = Bukkit.getPlayer(onUs.owner);
            if (owner != null && owner.isOnline())
                owner.sendMessage(ChatColor.GRAY + "🪢 " + p.getName() + " вышел — поводок отвязался.");
            leashTies.remove(onUs.owner);
            leashEscapeClicks.remove(uid);
        }
        leashBind.remove(uid);
        homingLocks.remove(uid);
        regionSelections.remove(uid);
        lastEnteredRegion.remove(uid);
        if (isMasked(p) && e.getQuitMessage() != null) e.quitMessage(Component.text(maskColored + "§e вышел из игры"));
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKick(PlayerKickEvent e) {
        Player p = e.getPlayer();
        if (isMasked(p) && e.getLeaveMessage() != null) e.leaveMessage(Component.text(maskColored + "§e был кикнут"));
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdvance(PlayerAdvancementDoneEvent e) { if (isMasked(e.getPlayer())) e.message(null); }

    // ==================== СМЕРТЬ ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player v = e.getEntity();
        String m = e.getDeathMessage();
        if (m == null) return;
        if (isMasked(v)) m = m.replace(v.getName(), maskColored);
        Player k = v.getKiller();
        if (k != null && isMasked(k)) m = m.replace(k.getName(), maskColored);
        if (v.getLastDamageCause() != null) {
            Entity dmg = v.getLastDamageCause().getEntity();
            if (dmg instanceof Projectile pr && pr.getShooter() instanceof Player s && isMasked(s))
                m = m.replace(s.getName(), maskColored);
        }
        e.setDeathMessage(m);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Player p = e.getPlayer();
            if (isMasked(p)) { p.setDisplayName(maskColored); p.setPlayerListName(maskColored); getHideTeam().addEntry(p.getName()); }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorld(PlayerChangedWorldEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Player p = e.getPlayer();
            if (isMasked(p)) { p.setDisplayName(maskColored); p.setPlayerListName(maskColored); getHideTeam().addEntry(p.getName()); }
        }, 2L);
    }

    // ==================== ДИНАМИТ (убран буст игрокам, только ломание + урон) ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDynamiteInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isDynamite(hand)) return;

        if (p.isSneaking() && e.getAction().name().startsWith("LEFT_CLICK")) {
            e.setCancelled(true);
            int mode = (getDynMode(hand) + 1) % 3;
            int amount = hand.getAmount();
            p.getInventory().setItemInMainHand(buildDynamite(amount, mode));
            String t = switch (mode) {
                case DYN_MEDIUM -> ChatColor.YELLOW + "средняя";
                case DYN_LONG   -> ChatColor.RED + "дальняя";
                default         -> ChatColor.GREEN + "короткая";
            };
            p.sendActionBar(ChatColor.GRAY + "Динамит: " + t);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, mode == DYN_SHORT ? 0.8f : mode == DYN_MEDIUM ? 1.2f : 1.6f);
            return;
        }
        if (e.getAction().name().startsWith("RIGHT_CLICK")) {
            e.setCancelled(true);
            throwDynamite(p, getDynMode(hand));
            consumeHand(p);
        }
    }

    private void throwDynamite(Player p, int mode) {
        double speed; float power; int fuse;
        switch (mode) {
            case DYN_MEDIUM -> { speed = 0.9;  power = 3.2F; fuse = 70; }
            case DYN_LONG   -> { speed = 1.5;  power = 4.0F; fuse = 90; }
            default         -> { speed = 0.55; power = 2.5F; fuse = 50; }
        }
        TNTPrimed tnt = p.getWorld().spawn(p.getEyeLocation().subtract(0, 0.2, 0), TNTPrimed.class);
        tnt.setFuseTicks(fuse);
        tnt.setSource(p);
        tnt.setYield(power);
        tnt.setIsIncendiary(false);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        tnt.setVelocity(dir.multiply(speed).add(new Vector(0, 0.15, 0)));
        tnt.getPersistentDataContainer().set(keyTntEntity, PersistentDataType.BYTE, (byte)1);
        tnt.getPersistentDataContainer().set(keyTntPower,  PersistentDataType.FLOAT, power);
        p.playSound(p.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1f);
    }

    // НЕТ БУСТА у динамита. Для мелких cluster-TNT — отменяем ванильный взрыв по таймеру,
    // т.к. они должны взрываться ТОЛЬКО при ударе о землю (детект в tickProjectiles).
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDynamiteExplode(EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof TNTPrimed tnt)) return;
        if (tnt.getPersistentDataContainer().has(keyClusterTnt, PersistentDataType.BYTE)) {
            // Если TNT ещё в воздухе (не на земле) — отменяем взрыв, ждём приземления
            boolean onGround = tnt.isOnGround()
                    || (Math.abs(tnt.getVelocity().getY()) < 0.08
                        && tnt.getLocation().subtract(0, 0.2, 0).getBlock().getType().isSolid());
            if (!onGround) {
                e.setCancelled(true);
                // Продлеваем fuse на ещё несколько тиков, чтобы не сработал в следующий тик
                tnt.setFuseTicks(Math.max(tnt.getFuseTicks(), 10));
            }
            return;
        }
        if (!tnt.getPersistentDataContainer().has(keyTntEntity, PersistentDataType.BYTE)) return;
        // Обычный динамит игрока — ванильный взрыв ломает блоки и наносит урон сам.
        // Буст игрокам НЕ добавляем! Буст есть ТОЛЬКО у фаербола.
    }

    // ==================== ОГЛУШАЮЩАЯ ГРАНАТА (исправленная) ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStunUse(PlayerInteractEvent e) {
        if (!e.getAction().name().startsWith("RIGHT_CLICK")) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isStunGrenade(hand)) return;
        e.setCancelled(true);
        throwStun(p);
        consumeHand(p);
    }

    private void throwStun(Player p) {
        // Спавним чуть впереди глаза, чтобы не было детонации об самого себя
        Location spawn = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(1.0));
        Snowball sb = p.getWorld().spawn(spawn, Snowball.class);
        sb.setShooter(p);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        sb.setVelocity(dir.multiply(STUN_SPEED).add(new Vector(0, 0.15, 0)));
        // Метка — что это именно наша стан-граната (чтобы не ловить обычные снежки)
        sb.getPersistentDataContainer().set(keyStunEntity, PersistentDataType.BYTE, (byte)1);
        sb.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING, p.getUniqueId().toString());
        sb.getPersistentDataContainer().set(keySpawnTick, PersistentDataType.LONG, tickCounter);
        p.playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.7f, 0.7f);
        // Взрыв по таймеру (если ни в кого не врезалась — бах в воздухе/на земле)
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!sb.isValid() || sb.isDead()) return;
            var pdc = sb.getPersistentDataContainer();
            if (pdc.has(keyStickyPlanted, PersistentDataType.BYTE)) return;
            pdc.set(keyStickyPlanted, PersistentDataType.BYTE, (byte)1);
            detonateStun(sb.getLocation(), p);
            sb.remove();
        }, STUN_FUSE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStunHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball sb)) return;
        var pdc = sb.getPersistentDataContainer();
        // Только наши снаряды стан-гранаты (обычные снежки игнорируем)
        if (!pdc.has(keyStunEntity, PersistentDataType.BYTE)) return;
        if (!(sb.getShooter() instanceof Player owner)) return;
        // Грейс: первые 3 тика снаряд не детонирует (чтобы не взрываться об игрока сразу после броска)
        Long spawnTick = pdc.get(keySpawnTick, PersistentDataType.LONG);
        if (spawnTick != null && tickCounter - spawnTick < 3) return;
        if (pdc.has(keyStickyPlanted, PersistentDataType.BYTE)) return;
        pdc.set(keyStickyPlanted, PersistentDataType.BYTE, (byte)1);
        detonateStun(sb.getLocation(), owner);
        sb.remove();
    }

    private void detonateStun(Location loc, Player owner) {
        World w = loc.getWorld();
        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 1.6f);
        playSoundSafe(w, loc, 2.0f, 2.0f, "ENTITY_LIGHTNING_BOLT_THUNDER", "ENTITY_GENERIC_EXPLODE");
        try { w.spawnParticle(Particle.FLASH, loc, 1); } catch (Throwable ignored) {}
        w.spawnParticle(Particle.CLOUD, loc, 60, 1.5, 1.0, 1.5, 0.15);
        w.spawnParticle(Particle.POOF, loc, 20, 1.0, 0.7, 1.0, 0.05);
        for (Entity ent : w.getNearbyEntities(loc, STUN_RADIUS, STUN_RADIUS, STUN_RADIUS)) {
            if (!(ent instanceof LivingEntity living)) continue;
            living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int)(STUN_DURATION_SEC*20), 0, false, false, true));
            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(STUN_DURATION_SEC*20), 3, false, false, false));
            living.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, (int)(STUN_DURATION_SEC*20), 0, false, false, false));
            living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (int)(STUN_DURATION_SEC*20), 1, false, false, false));
            // Кратковременный микростан (высоко поднимаем в воздух и тормозим скорость)
            living.setVelocity(new Vector(
                    ThreadLocalRandom.current().nextDouble()*0.8-0.4,
                    0.35,
                    ThreadLocalRandom.current().nextDouble()*0.8-0.4
            ));
            if (living instanceof Player pl) {
                // Лёгкий поворот камеры для дезориентации
                float yawDelta = ThreadLocalRandom.current().nextFloat(120f) - 60f;
                Location ploc = pl.getLocation();
                float newYaw = ploc.getYaw() + yawDelta;
                float newPitch = Math.max(-90f, Math.min(90f,
                        ploc.getPitch() + (ThreadLocalRandom.current().nextFloat(40f) - 20f)));
                pl.teleport(new Location(pl.getWorld(), ploc.getX(), ploc.getY(), ploc.getZ(), newYaw, newPitch));
                pl.sendTitle(ChatColor.WHITE + "💥", ChatColor.GRAY + "Оглушение!", 0, (int)(STUN_DURATION_SEC*20), 5);
            }
        }
    }

    // ==================== КРИО-ГРАНАТА (бросок + детонация) ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFreezeUse(PlayerInteractEvent e) {
        if (!e.getAction().name().startsWith("RIGHT_CLICK")) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isFreezeGrenade(hand)) return;
        e.setCancelled(true);
        throwFreeze(p);
        consumeHand(p);
    }

    private void throwFreeze(Player p) {
        // Снежок со спрайтом — обёртка; детонация создаёт морозное облако
        Location spawn = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(1.0));
        Snowball sb = p.getWorld().spawn(spawn, Snowball.class);
        sb.setShooter(p);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        sb.setVelocity(dir.multiply(FREEZE_SPEED).add(new Vector(0, 0.15, 0)));
        sb.getPersistentDataContainer().set(keyFreezeEntity, PersistentDataType.BYTE, (byte)1);
        sb.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING, p.getUniqueId().toString());
        sb.getPersistentDataContainer().set(keySpawnTick, PersistentDataType.LONG, tickCounter);
        playSoundSafe(p.getWorld(), p.getLocation(), 0.8f, 0.9f,
                "ENTITY_SNOWBALL_THROW", "BLOCK_POWDER_SNOW_PLACE", "BLOCK_SNOW_BREAK");
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!sb.isValid() || sb.isDead()) return;
            var pdc = sb.getPersistentDataContainer();
            if (pdc.has(keyStickyPlanted, PersistentDataType.BYTE)) return;
            pdc.set(keyStickyPlanted, PersistentDataType.BYTE, (byte)1);
            detonateFreeze(sb.getLocation());
            sb.remove();
        }, FREEZE_FUSE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFreezeHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball sb)) return;
        var pdc = sb.getPersistentDataContainer();
        if (!pdc.has(keyFreezeEntity, PersistentDataType.BYTE)) return;
        Long spawnTick = pdc.get(keySpawnTick, PersistentDataType.LONG);
        if (spawnTick != null && tickCounter - spawnTick < 3) return;
        if (pdc.has(keyStickyPlanted, PersistentDataType.BYTE)) return;
        pdc.set(keyStickyPlanted, PersistentDataType.BYTE, (byte)1);
        detonateFreeze(sb.getLocation());
        sb.remove();
    }

    private void detonateFreeze(Location loc) {
        World w = loc.getWorld();
        Location c = loc.clone();
        freezeClouds.add(new FreezeCloud(c, FREEZE_RADIUS, FREEZE_HEIGHT, tickCounter + FREEZE_DURATION));
        // Звуки: стекло/лёд/снег
        playSoundSafe(w, c, 2.0f, 0.7f,
                "BLOCK_GLASS_BREAK", "BLOCK_ICE_BREAK", "ENTITY_PLAYER_HURT_FREEZE");
        w.playSound(c, Sound.BLOCK_SNOW_BREAK, 1.5f, 0.6f);
        // Замораживаем воду/лаву вокруг
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int dx = -FREEZE_RADIUS; dx <= FREEZE_RADIUS; dx++) {
            for (int dz = -FREEZE_RADIUS; dz <= FREEZE_RADIUS; dz++) {
                if (dx*dx + dz*dz > FREEZE_RADIUS*FREEZE_RADIUS) continue;
                for (int dy = -1; dy < FREEZE_HEIGHT-1; dy++) {
                    Block b = w.getBlockAt(c.getBlockX()+dx, c.getBlockY()+dy, c.getBlockZ()+dz);
                    Material m = b.getType();
                    if (m == Material.WATER || m == Material.BUBBLE_COLUMN) {
                        freezeBlock(b, Material.ICE);
                    } else if (m == Material.LAVA) {
                        freezeBlock(b, Material.OBSIDIAN);
                    }
                }
            }
        }
        // Визуал: снежинки и облако
        for (int i = 0; i < 80; i++) {
            double ox = (rnd.nextDouble()-0.5)*2*FREEZE_RADIUS;
            double oy = rnd.nextDouble()*FREEZE_HEIGHT;
            double oz = (rnd.nextDouble()-0.5)*2*FREEZE_RADIUS;
            if (ox*ox + oz*oz > FREEZE_RADIUS*FREEZE_RADIUS) continue;
            try { w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(ox, oy, oz), 1, 0, 0, 0, 0); } catch (Throwable ignored) {}
        }
        w.spawnParticle(Particle.CLOUD, c, 40, FREEZE_RADIUS, 0.5, FREEZE_RADIUS, 0.1);
        try { w.spawnParticle(Particle.SPLASH, c.clone().add(0,0.5,0), 30, FREEZE_RADIUS*0.8, 0.6, FREEZE_RADIUS*0.8, 0.05); } catch (Throwable ignored) {}
        try { w.spawnParticle(Particle.CRIT, c, 20, FREEZE_RADIUS*0.7, 0.4, FREEZE_RADIUS*0.7, 0.1); } catch (Throwable ignored) {}
    }

    /** Заморозить блок во временный материал; запомнить исходный для обратного отката */
    private void freezeBlock(Block b, Material frozenAs) {
        long key = blockKey(b);
        if (frozenBlocks.containsKey(key)) return;
        frozenBlocks.put(key, new FrozenBlock(b.getWorld(), b.getX(), b.getY(), b.getZ(), b.getType(), frozenAs, tickCounter + FREEZE_ICE_TIME));
        b.setType(frozenAs, false);
    }

    private long blockKey(Block b) {
        return locKey(b.getLocation());
    }

    /** Тик крио-облаков: частицы холода + эффект Slowness игрокам внутри */
    private void tickFreezeClouds() {
        long now = tickCounter;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Iterator<FreezeCloud> it = freezeClouds.iterator();
        while (it.hasNext()) {
            FreezeCloud cl = it.next();
            if (now > cl.expireTick) { it.remove(); continue; }
            World w = cl.center.getWorld();
            double r2 = cl.radius * cl.radius;
            // Частицы снега
            for (int i = 0; i < FREEZE_PARTICLES; i++) {
                double ox = (rnd.nextDouble()-0.5)*2*cl.radius;
                double oy = rnd.nextDouble()*cl.height;
                double oz = (rnd.nextDouble()-0.5)*2*cl.radius;
                if (ox*ox + oz*oz > r2) continue;
                Location p = cl.center.clone().add(ox, oy, oz);
                try { w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0, 0.02, 0, 0.005); } catch (Throwable ignored) {}
                if (rnd.nextDouble() < 0.15)
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.05, 0.02, 0.05, 0.01);
            }
            // Эффекты игрокам
            for (Player pl : w.getPlayers()) {
                Location ploc = pl.getLocation();
                double dy = ploc.getY() + pl.getHeight()*0.5 - cl.center.getY();
                if (dy < -0.5 || dy > cl.height) continue;
                double dx = ploc.getX() - cl.center.getX();
                double dz = ploc.getZ() - cl.center.getZ();
                if (dx*dx + dz*dz > r2) continue;
                pl.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3, false, false, false));
                if (rnd.nextInt(10) == 0) {
                    // FROZEN_SLOWNESS есть в новых версиях; если нет — просто медлительность посильнее
                    PotionEffectType freezeType = null;
                    try {
                        org.bukkit.Registry<PotionEffectType> reg = org.bukkit.Registry.EFFECT;
                        if (reg != null) freezeType = reg.get(NamespacedKey.minecraft("frozen_slowness"));
                    } catch (Throwable ignored) {}
                    if (freezeType != null) {
                        pl.addPotionEffect(new PotionEffect(freezeType, 40, 0, false, false, false));
                    } else {
                        pl.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 4, false, false, false));
                    }
                }
                // Звук "дрожи" иногда
                if (rnd.nextInt(25) == 0)
                    playSoundSafe(w, pl.getLocation(), 0.3f, 1.5f + rnd.nextFloat()*0.4f,
                            "BLOCK_POWDER_SNOW_STEP", "BLOCK_SNOW_STEP", "ENTITY_PLAYER_HURT_FREEZE");
            }
        }
    }

    /** Тик таяния замороженных блоков */
    private void tickFrozenBlocks() {
        List<Long> toThaw = new ArrayList<>();
        for (Map.Entry<Long, FrozenBlock> e : frozenBlocks.entrySet()) {
            if (tickCounter >= e.getValue().thawAt) toThaw.add(e.getKey());
        }
        for (Long k : toThaw) {
            FrozenBlock fb = frozenBlocks.remove(k);
            if (fb.world == null) continue;
            Block b = fb.world.getBlockAt(fb.x, fb.y, fb.z);
            // Откатываем ТОЛЬКО если там ещё наш лёд/обсидиан (игрок не заменил блок)
            if (b.getType() == fb.frozenAs) {
                b.setType(fb.original, true);
                Location loc = b.getLocation().add(0.5, 0.5, 0.5);
                try { b.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 5, 0.2,0.2,0.2,0.01); } catch (Throwable ignored) {}
            }
        }
    }

    // ==================== ФАЕРБОЛ (ЕДИНСТВЕННЫЙ с rocket boost) ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFireballUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isFireballItem(hand) || !e.getAction().name().startsWith("RIGHT_CLICK")) return;
        e.setCancelled(true);
        throwFireball(p);
        consumeHand(p);
    }

    private void throwFireball(Player p) {
        Fireball fb = p.getWorld().spawn(p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(1.2)), Fireball.class);
        fb.setShooter(p);
        fb.setIsIncendiary(true);
        fb.setYield(2.5F);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        fb.setVelocity(dir.multiply(1.4));
        fb.getPersistentDataContainer().set(keyFireballEntity, PersistentDataType.BYTE, (byte)1);
        p.playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
        p.setCooldown(Material.FIRE_CHARGE, 15);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFireballHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Fireball fb)) return;
        if (!fb.getPersistentDataContainer().has(keyFireballEntity, PersistentDataType.BYTE)) return;
        Location hit = fb.getLocation();
        float power = 2.0F; double radius = 4.0;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Entity near : fb.getWorld().getNearbyEntities(hit, radius, radius, radius)) {
                if (!(near instanceof Player pl)) continue;
                Vector diff = pl.getLocation().toVector().subtract(hit.toVector());
                double len = diff.length();
                if (len < 0.3) {
                    pl.setVelocity(pl.getVelocity().add(new Vector(0, power * 1.2, 0)));
                } else {
                    Vector boost = diff.normalize().multiply(power * (1.0 - len / radius) * 0.9).setY(power * 0.6);
                    pl.setVelocity(pl.getVelocity().add(boost));
                }
                pl.playSound(pl.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireballExplode(EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof Fireball fb)) return;
        if (!fb.getPersistentDataContainer().has(keyFireballEntity, PersistentDataType.BYTE)) return;
        // Ломает блоки как ванильный — ничего не трогаем
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFireballDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Fireball fb && fb.getPersistentDataContainer().has(keyFireballEntity, PersistentDataType.BYTE))
            e.setCancelled(true);
    }

    // ==================== ЛИПКАЯ БОМБА ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStickyInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isStickyItem(hand)) return;

        if (p.isSneaking() && e.getAction().name().startsWith("LEFT_CLICK")) {
            e.setCancelled(true);
            int mode = (getStickyMode(hand) + 1) % 3;
            int amount = hand.getAmount();
            p.getInventory().setItemInMainHand(buildSticky(amount, mode));
            String t = switch (mode) {
                case STICKY_SPEED_MED  -> ChatColor.YELLOW + "средняя";
                case STICKY_SPEED_LONG -> ChatColor.RED + "дальняя";
                default                -> ChatColor.GREEN + "короткая";
            };
            p.sendActionBar(ChatColor.GRAY + "Липучка: " + t);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, mode == STICKY_SPEED_SHORT ? 0.8f : mode == STICKY_SPEED_MED ? 1.2f : 1.6f);
            return;
        }
        if (e.getAction().name().startsWith("RIGHT_CLICK")) {
            e.setCancelled(true);
            throwSticky(p, getStickyMode(hand));
            consumeHand(p);
        }
    }

    private void throwSticky(Player p, int mode) {
        double speed, velY;
        switch (mode) {
            case STICKY_SPEED_MED  -> { speed = 1.0; velY = 0.2; }
            case STICKY_SPEED_LONG -> { speed = 1.6; velY = 0.3; }
            default                -> { speed = 0.7; velY = 0.2; }
        }
        // Снаряд — падающий блок слизи (визуально липкий)
        FallingBlock fb = p.getWorld().spawnFallingBlock(
                p.getEyeLocation().subtract(0, 0.2, 0),
                Material.SLIME_BLOCK.createBlockData()
        );
        fb.setDropItem(false);
        fb.setHurtEntities(false);
        fb.setPersistent(false);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        fb.setVelocity(dir.multiply(speed).add(new Vector(0, velY, 0)));
        fb.getPersistentDataContainer().set(keyStickyEntity, PersistentDataType.BYTE, (byte)1);
        fb.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING, p.getUniqueId().toString());
        fb.getPersistentDataContainer().set(keySpawnTick, PersistentDataType.LONG, tickCounter);
        p.playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.6f, 0.9f);
    }

    /** Прилепить бомбу: вместо падающего блока спавним мини-TNTPrimed с 10-сек фитилём и держим её на месте */
    private void stickSticky(FallingBlock fb, Location place) {
        World w = place.getWorld();
        TNTPrimed tnt = w.spawn(place, TNTPrimed.class);
        tnt.setFuseTicks(STICKY_FUSE_TICKS);
        tnt.setYield(STICKY_POWER);
        tnt.setIsIncendiary(true);
        tnt.setGravity(false);
        tnt.setVelocity(new Vector(0, 0, 0));
        tnt.getPersistentDataContainer().set(keyStickyPlanted, PersistentDataType.BYTE, (byte)1);
        tnt.getPersistentDataContainer().set(keyStickyFuseStart, PersistentDataType.LONG, tickCounter);
        String ownerStr = fb.getPersistentDataContainer().get(keyOwner, PersistentDataType.STRING);
        if (ownerStr != null) {
            tnt.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING, ownerStr);
            try { tnt.setSource(Bukkit.getPlayer(UUID.fromString(ownerStr))); } catch (Exception ignored) {}
        }
        w.playSound(place, Sound.BLOCK_SLIME_BLOCK_PLACE, 0.5f, 1.0f);
        w.spawnParticle(Particle.ITEM_SLIME, place, 6, 0.25, 0.15, 0.25, 0.03, new ItemStack(Material.SLIME_BALL));
        fb.remove();
    }

    /** Тик прилипших бомб: держим на месте, звуки отсчёта */
    private void tickStickyPlanted() {
        for (World w : Bukkit.getWorlds()) {
            for (TNTPrimed tnt : w.getEntitiesByClass(TNTPrimed.class)) {
                var pdc = tnt.getPersistentDataContainer();
                if (!pdc.has(keyStickyPlanted, PersistentDataType.BYTE)) continue;

                // Не даём упасть/отскочить
                tnt.setVelocity(new Vector(0, 0, 0));
                tnt.setGravity(false);

                Long start = pdc.get(keyStickyFuseStart, PersistentDataType.LONG);
                if (start == null) continue;
                long passed = tickCounter - start;
                long left = STICKY_FUSE_TICKS - passed;
                Location loc = tnt.getLocation();

                if (left > 0 && (passed % 20 == 0 || (left <= 40 && passed % 10 == 0) || (left <= 20 && passed % 4 == 0))) {
                    float pitch = left > 40 ? 0.7f : left > 20 ? 1.0f : 1.5f;
                    w.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, pitch);
                    w.spawnParticle(Particle.LARGE_SMOKE, loc, 2, 0.15, 0.1, 0.15, 0.01);
                }
            }
        }
    }



    // ==================== ДЫМОВАЯ ШАШКА (только дым, без взрывов, без летающего блока) ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSmokeInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isSmokeItem(hand)) return;
        if (!e.getAction().name().startsWith("RIGHT_CLICK")) return;

        e.setCancelled(true);

        // Ставим дым сразу в точке, куда смотрит игрок (рейкаст до SMOKE_RANGE блоков)
        Location target = getSmokeTargetLocation(p);
        spawnSmokeCloud(target);
        p.getWorld().playSound(target, Sound.BLOCK_FIRE_EXTINGUISH, 1.4f, 0.5f);
        p.getWorld().spawnParticle(Particle.CLOUD, target, 15, 0.6, 0.2, 0.6, 0.03);

        consumeHand(p);
    }

    /** Ищет точку на земле, куда смотрит игрок (для мгновенной постановки дыма) */
    private Location getSmokeTargetLocation(Player p) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        World w = p.getWorld();
        Location last = eye.clone();
        for (double d = 0; d <= SMOKE_RANGE; d += 0.5) {
            Location step = eye.clone().add(dir.clone().multiply(d));
            Block b = step.getBlock();
            if (b.getType().isSolid() && !b.isPassable()) {
                // ставим на верх этого блока
                return new Location(w, step.getBlockX() + 0.5, b.getY() + 1, step.getBlockZ() + 0.5);
            }
            last = step;
        }
        // если ничего не встретили — конечная точка луча, опускаем до земли
        for (int y = 0; y < 40; y++) {
            Block b = last.getBlock();
            if (b.getType().isSolid() && !b.isPassable()) {
                return new Location(w, last.getBlockX() + 0.5, b.getY() + 1, last.getBlockZ() + 0.5);
            }
            last.subtract(0, 1, 0);
            if (last.getY() < w.getMinHeight()) break;
        }
        return eye.clone().add(dir.multiply(5));
    }

    // ==================== КАССЕТНАЯ ГРАНАТА (только 8 TNT, БЕЗ ДЫМА) ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGrenadeInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isGrenadeItem(hand)) return;

        if (p.isSneaking() && e.getAction().name().startsWith("LEFT_CLICK")) {
            e.setCancelled(true);
            int mode = (getGrenadeItemMode(hand) + 1) % 5;
            int amount = hand.getAmount();
            p.getInventory().setItemInMainHand(buildClusterGrenade(amount, mode));
            String t = switch (mode) {
                case SMOKE_MEDIUM -> ChatColor.YELLOW + "средняя";
                case SMOKE_LONG   -> ChatColor.RED + "дальняя";
                case SMOKE_HUGE   -> ChatColor.DARK_RED + "огромная";
                case SMOKE_GIANT  -> ChatColor.DARK_PURPLE + "гигантская";
                default           -> ChatColor.GREEN + "короткая";
            };
            p.sendActionBar(ChatColor.GRAY + "Граната: " + t);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 0.7f + mode * 0.2f);
            return;
        }

        if (e.getAction().name().startsWith("RIGHT_CLICK")) {
            e.setCancelled(true);
            throwGrenade(p, getGrenadeItemMode(hand));
            consumeHand(p);
        }
    }

    private void throwGrenade(Player p, int mode) {
        double speed = switch (mode) {
            case SMOKE_MEDIUM -> 1.1;
            case SMOKE_LONG   -> 1.5;
            case SMOKE_HUGE   -> 1.9;
            case SMOKE_GIANT  -> 2.3;
            default           -> 0.7;
        };
        // ЧЁРНО-бетонный блок как снаряд кассетной гранаты
        FallingBlock fb = p.getWorld().spawnFallingBlock(
                p.getEyeLocation().subtract(0, 0.2, 0),
                Material.BLACK_CONCRETE.createBlockData()
        );
        fb.setDropItem(false);
        fb.setHurtEntities(false);
        fb.setPersistent(false);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        fb.setVelocity(dir.multiply(speed).add(new Vector(0, 0.15, 0)));
        fb.getPersistentDataContainer().set(keyGrenadeEntity, PersistentDataType.BYTE, (byte)1);
        fb.getPersistentDataContainer().set(keyGrenadeMode, PersistentDataType.INTEGER, mode);
        fb.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING, p.getUniqueId().toString());
        fb.getPersistentDataContainer().set(keySpawnTick, PersistentDataType.LONG, tickCounter);
        p.playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.7f, 0.5f);
    }

    /** Тик снарядов (кас-граната + её мелкая TNT — только детон о землю) */
    private void tickProjectiles() {
        for (World w : Bukkit.getWorlds()) {
            for (Entity ent : w.getEntities()) {

                // ----- 1) Мелкая TNT из кассетной гранаты: взрывается ТОЛЬКО о землю -----
                if (ent instanceof TNTPrimed tnt
                        && tnt.getPersistentDataContainer().has(keyClusterTnt, PersistentDataType.BYTE)) {
                    boolean onGround = tnt.isOnGround()
                            || (Math.abs(tnt.getVelocity().getY()) < 0.08 && tnt.getLocation().subtract(0, 0.2, 0).getBlock().getType().isSolid());
                    if (onGround || tnt.getFuseTicks() <= 1) {
                        Location loc = tnt.getLocation();
                        float power = tnt.getPersistentDataContainer().getOrDefault(keyTntPower, PersistentDataType.FLOAT, CLUSTER_TNT_POWER);
                        tnt.remove();
                        w.createExplosion(loc, power, true, true);
                    }
                    continue;
                }

                if (!(ent instanceof FallingBlock fb)) continue;
                var pdc = fb.getPersistentDataContainer();

                // Любые «старые» FallingBlock от дыма — сразу удалить
                if (pdc.has(keySmokeEntity, PersistentDataType.BYTE)) {
                    fb.remove();
                    continue;
                }

                // -------- ЛИПКАЯ БОМБА в полёте: прилипает к ЛЮБОЙ твердой поверхности (пол/стена/потолок) --------
                if (pdc.has(keyStickyEntity, PersistentDataType.BYTE)) {
                    Long spawned = pdc.get(keySpawnTick, PersistentDataType.LONG);
                    long age = spawned == null ? Long.MAX_VALUE : (tickCounter - spawned);
                    Location loc = fb.getLocation();
                    boolean hit = false;
                    if (age >= STICKY_GRACE_TICKS) {
                        // Проверяем 6 направлений вокруг снаряда и саму позицию на солидный блок
                        double r = STICKY_ATTACH_DIST;
                        Location[] checks = new Location[] {
                                loc.clone(),
                                loc.clone().add(r, 0, 0), loc.clone().add(-r, 0, 0),
                                loc.clone().add(0, r, 0), loc.clone().add(0, -r, 0),
                                loc.clone().add(0, 0, r), loc.clone().add(0, 0, -r)
                        };
                        for (Location ch : checks) {
                            if (ch.getBlock().getType().isSolid() && !ch.getBlock().isPassable()) { hit = true; break; }
                        }
                        // Также прилипаем если практически остановилась (на что-то наткнулась)
                        if (!hit && fb.isOnGround()) hit = true;
                    }
                    if (hit) {
                        stickSticky(fb, loc);
                        continue;
                    }
                    continue;
                }

                // -------- КАССЕТНАЯ граната: в 20 блоках от земли (при падении вниз) — распад на 8 TNT --------
                if (pdc.has(keyGrenadeEntity, PersistentDataType.BYTE)) {
                    Location loc = fb.getLocation();
                    boolean onGround = fb.isOnGround() || (fb.getVelocity().getY() > -0.01 && fb.getVelocity().length() < 0.08);

                    Long spawned = pdc.get(keySpawnTick, PersistentDataType.LONG);
                    long age = spawned == null ? Long.MAX_VALUE : (tickCounter - spawned);
                    boolean graceOver = age >= CLUSTER_GRACE_TICKS;
                    boolean falling = fb.getVelocity().getY() < 0;

                    boolean triggerAir = false;
                    if (graceOver && falling) {
                        Block ground = findGround(loc);
                        if (ground != null) {
                            double dY = loc.getY() - (ground.getY() + 1);
                            if (dY >= 0 && dY <= CLUSTER_TRIGGER_Y) triggerAir = true;
                        }
                    }
                    if (onGround || triggerAir) {
                        triggerCluster(loc, fb);
                        fb.remove();
                    }
                }
            }
        }
    }

    /** Распад кассетной гранаты на 8 TNT (БЕЗ создания дымового облака!) */
    private void triggerCluster(Location loc, FallingBlock source) {
        World w = loc.getWorld();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Player owner = null;
        String ownerStr = source.getPersistentDataContainer().get(keyOwner, PersistentDataType.STRING);
        if (ownerStr != null) {
            try { owner = Bukkit.getPlayer(UUID.fromString(ownerStr)); } catch (Exception ignored) {}
        }

        w.playSound(loc, Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 0.8f);
        w.spawnParticle(Particle.CLOUD, loc, 20, 0.5, 0.2, 0.5, 0.05);
        w.spawnParticle(Particle.POOF, loc, 8, 0.4, 0.3, 0.4, 0.1);

        for (int i = 0; i < CLUSTER_COUNT; i++) {
            double ox = (rnd.nextDouble() - 0.5) * 3.0;
            double oz = (rnd.nextDouble() - 0.5) * 3.0;
            Location tntLoc = loc.clone().add(ox, 1.8, oz);
            TNTPrimed tnt = w.spawn(tntLoc, TNTPrimed.class);
            tnt.setFuseTicks(CLUSTER_TNT_FUSE);
            tnt.setYield(CLUSTER_TNT_POWER);
            tnt.setIsIncendiary(true);
            if (owner != null) tnt.setSource(owner);
            tnt.setVelocity(new Vector(ox * 0.28, 0.35 + rnd.nextDouble() * 0.3, oz * 0.28));
            tnt.getPersistentDataContainer().set(keyClusterTnt, PersistentDataType.BYTE, (byte)1);
            tnt.getPersistentDataContainer().set(keyTntPower, PersistentDataType.FLOAT, CLUSTER_TNT_POWER);
        }
        // ДЫМ ПОСЛЕ ВЗРЫВОВ НЕ СОЗДАЁМ! Только в дымшашке — дым.
    }

    /** Найти первый сплошной блок под ногами (для срабатывания кассеты) */
    private Block findGround(Location loc) {
        Location l = loc.clone();
        for (int y = 0; y < 40; y++) {
            Block b = l.getBlock();
            if (b.getType().isSolid() && !b.isPassable()) return b;
            l.subtract(0, 1, 0);
            if (l.getY() < loc.getWorld().getMinHeight()) return null;
        }
        return null;
    }

    /** Создать дымовое облако в указанной точке */
    private void spawnSmokeCloud(Location center) {
        Location c = center.clone();
        smokeClouds.add(new SmokeCloud(c, CLOUD_RADIUS, CLOUD_HEIGHT, tickCounter + CLOUD_DURATION));
    }

    /** Тик дымовых облаков — частицы и эффекты */
    private void tickSmokeClouds() {
        long now = tickCounter;
        Random rnd = ThreadLocalRandom.current();
        Iterator<SmokeCloud> it = smokeClouds.iterator();
        while (it.hasNext()) {
            SmokeCloud cloud = it.next();
            if (now > cloud.expireTick) { it.remove(); continue; }
            World w = cloud.center.getWorld();

            // === Облако 8×8×5: частицы равномерно в цилиндре ===
            for (int i = 0; i < PARTICLES_PER_TICK; i++) {
                double ox = (rnd.nextDouble() - 0.5) * 2.0 * cloud.radius;
                double oy = rnd.nextDouble() * CLOUD_HEIGHT;
                double oz = (rnd.nextDouble() - 0.5) * 2.0 * cloud.radius;
                if (ox * ox + oz * oz > cloud.radius * cloud.radius) continue;
                Location p = cloud.center.clone().add(ox, oy, oz);
                if (rnd.nextDouble() < 0.6)
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p, 1, 0.04, 0.06, 0.04, 0.002);
                else
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.04, 0.06, 0.04, 0.008);
                if (rnd.nextDouble() < 0.05)
                    w.spawnParticle(Particle.CLOUD, p, 1, 0, 0.03, 0, 0);
            }

            // === Эффекты игрокам внутри облака ===
            double r2 = cloud.radius * cloud.radius;
            for (Player pl : w.getPlayers()) {
                Location ploc = pl.getLocation();
                double dy = ploc.getY() + pl.getHeight() * 0.5 - cloud.center.getY();
                if (dy < -0.5 || dy > CLOUD_HEIGHT) continue;
                double dx = ploc.getX() - cloud.center.getX();
                double dz = ploc.getZ() - cloud.center.getZ();
                if (dx * dx + dz * dz > r2) continue;

                pl.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, BLINDNESS_SEC * 20, 0, false, false, false));
                pl.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_SEC * 20, SLOWNESS_LVL, false, false, false));
            }
        }
    }

    // Если кассетная граната была удалена — запускаем распад чтобы не пропала
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGrenadeDie(EntityRemoveEvent e) {
        if (!(e.getEntity() instanceof FallingBlock fb)) return;
        var pdc = fb.getPersistentDataContainer();
        if (!pdc.has(keyGrenadeEntity, PersistentDataType.BYTE)) return;
        if (fb.isValid() && !fb.isDead()) return;
        triggerCluster(fb.getLocation(), fb);
    }

    // ==================== СИГАРЕТЫ ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCigInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!e.getAction().name().startsWith("RIGHT_CLICK")) return;
        ItemStack hand = p.getInventory().getItemInMainHand();

        // ПКМ пачкой — вытащить одну сигарету (работает в воздухе и по неинтерактивным блокам)
        if (isCigPack(hand) && (e.getClickedBlock() == null || !e.getClickedBlock().getType().isInteractable())) {
            e.setCancelled(true);
            int t = getCigType(hand);
            ItemStack cig = buildCigarette(1, t);
            Map<Integer, ItemStack> left = p.getInventory().addItem(cig);
            for (ItemStack lo : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), lo);
            consumeHand(p);
            p.getWorld().playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.7f, 1.0f);
            p.sendActionBar(ChatColor.GRAY + "Вы достали сигарету");
            return;
        }

        // ПКМ сигаретой — начать курить (3 сек, нельзя двигаться/получать урон)
        if (isCigarette(hand) && (e.getClickedBlock() == null || !e.getClickedBlock().getType().isInteractable())) {
            e.setCancelled(true);
            UUID uid = p.getUniqueId();
            if (smokingTask.containsKey(uid)) {
                p.sendActionBar(ChatColor.RED + "Ты уже куришь!");
                return;
            }
            startSmoking(p, hand);
        }
    }

    /** Начать процесс курения сигареты: 3 секунд (60 тиков), затем эффекты + никотиновый удар */
    private void startSmoking(Player p, ItemStack cig) {
        UUID uid = p.getUniqueId();
        Location start = p.getLocation();
        int type = getCigType(cig);
        // Запрещаем курить в креативе без траты предмета? Нет — в креативе тоже забираем (как указано consumeHand)
        // Но фактически consumeHand вернёт предмет.
        final Location[] lastSafe = { start };
        long[] ticks = { 0 };
        int taskId = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override public void run() {
                if (!p.isOnline() || !p.isValid()) { stopSmoke(uid); return; }
                // Отмена если игрок начал бежать (переместился больше чем на 0.5 блока)
                Location now = p.getLocation();
                if (now.distanceSquared(lastSafe[0]) > 0.25) {
                    p.sendMessage(ChatColor.RED + "Курение прервано (движение).");
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 0.8f);
                    stopSmoke(uid);
                    return;
                }
                // Частицы дыма перед игроком
                Location mouth = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(0.4));
                p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, mouth, 3, 0.05, 0.05, 0.05, 0.008);
                ticks[0] += 2;
                if (ticks[0] % 10 == 0)
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.3f, 0.6f);
                if (ticks[0] >= CIG_SMOKE_TICKS) {
                    finishSmoke(p, type);
                    stopSmoke(uid);
                }
            }
        }, 0L, 2L).getTaskId();
        smokingTask.put(uid, (long) taskId);
        // Сохраняем старт: тратим сигарету сразу — если прервали — не возвращаем (как сигарету бросили)
        consumeHand(p);
        p.sendTitle(" ", ChatColor.GRAY + "Куришь...", 0, 60, 0);
    }

    private void stopSmoke(UUID uid) {
        Long tid = smokingTask.remove(uid);
        if (tid != null) Bukkit.getScheduler().cancelTask(tid.intValue());
    }

    /** Сигарета докурена — наложить эффекты и записать никотиновый удар */
    private void finishSmoke(Player p, int type) {
        switch (type) {
            case CIG_DIRT -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 5*20, 0, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 3*20, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2*20, 0, false, false, false));
            }
            case CIG_CLASSIC -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 10*20, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 5*20, 0, false, false, false));
            }
            case CIG_MENTHOL -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2*20, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5*20, 0, false, false, true));
            }
            case CIG_GOLD -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 3*20, 0, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 2*20, 0, false, false, false));
            }
            case CIG_CIGAR -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 15*20, 0, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 3*20, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 5*20, 0, false, false, false));
            }
        }
        p.getWorld().spawnParticle(Particle.LARGE_SMOKE, p.getEyeLocation(), 12, 0.25, 0.2, 0.25, 0.02);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 0.8f);
        registerNicotinePuff(p);
    }

    /** Принудительно прерываем курение если игрок получил урон */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmokeDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        UUID uid = p.getUniqueId();
        if (smokingTask.containsKey(uid)) {
            p.sendMessage(ChatColor.RED + "Курение прервано (получен урон).");
            stopSmoke(uid);
        }
    }

    // ==================== ВИРУС МЯУКАНЬЯ: использование вакцины и слюней ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVaccineUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!e.getAction().name().startsWith("RIGHT_CLICK")) return;
        if (isVaccine(hand) && (e.getClickedBlock() == null || !e.getClickedBlock().getType().isInteractable())) {
            e.setCancelled(true);
            int st = getMeowStage(p);
            if (st == 0) {
                p.sendMessage(ChatColor.GRAY + "Вы здоровы, вакцина не нужна.");
                return;
            }
            cureMeow(p, true);
            consumeHand(p);
            p.playSound(p.getLocation(), Sound.ITEM_BOTTLE_EMPTY, 1f, 1f);
            p.sendMessage(ChatColor.GREEN + "💉 Вакцина сработала! Вы полностью излечены от мяуканья.");
            p.spawnParticle(Particle.HEART, p.getLocation().add(0, 1, 0), 8, 0.4, 0.3, 0.4, 0.02);
            return;
        }
        if (isSlobber(hand) && (e.getClickedBlock() == null || !e.getClickedBlock().getType().isInteractable())) {
            e.setCancelled(true);
            infectPlayer(p, 1);
            consumeHand(p);
            p.playSound(p.getLocation(), Sound.ENTITY_CAT_PURREOW, 0.9f, 0.9f);
            p.sendMessage(ChatColor.LIGHT_PURPLE + "🐱 Фу... вы проглотили слюни кота. В горле першит...");
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 5*20, 0, false, false, false));
        }
    }

    // ==================== ПЕРЕДОЗИРОВКА НИКОТИНОМ ====================
    /** Записать одну затяжку и применить эффекты передозировки */
    private void registerNicotinePuff(Player p) {
        UUID uid = p.getUniqueId();
        long now = System.currentTimeMillis();
        ArrayDeque<Long> dq = nicotineHits.computeIfAbsent(uid, u -> new ArrayDeque<>());
        dq.addLast(now);
        // Вычистить затяжки старше окна
        while (!dq.isEmpty() && now - dq.peekFirst() > TOXICITY_WINDOW_MS) dq.pollFirst();
        int cnt = dq.size();
        String msg = null;
        ChatColor col = ChatColor.GRAY;
        // Сбрасываем предыдущие никотиновые эффекты и вешаем по уровню
        // Уровни
        if (cnt >= TOXIC_THRESH_DEATH) {
            msg = ChatColor.DARK_RED + "⚠ ПЕРЕДОЗИРОВКА! Смертельная доза...";
            col = ChatColor.DARK_RED;
            overdoseDeaths.add(uid);
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 5*20, 4, false, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 5*20, 0, false, false, false));
            // Гарантированная смерть с кастомным сообщением через 1 секунду
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!p.isOnline() || !p.isValid() || p.isDead()) return;
                ArrayDeque<Long> cur = nicotineHits.get(uid);
                if (cur == null) return;
                long nn = System.currentTimeMillis();
                while (!cur.isEmpty() && nn - cur.peekFirst() > TOXICITY_WINDOW_MS) cur.pollFirst();
                if (cur.size() >= TOXIC_THRESH_DEATH) {
                    overdoseDeaths.add(uid);
                    p.setHealth(0);
                }
            }, 20L);
        } else if (cnt >= TOXIC_THRESH_WITHER) {
            msg = ChatColor.RED + "⚠ Передозировка: сильное отравление!";
            col = ChatColor.RED;
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 10*20, 2, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 8*20, 0, false, false, false));
        } else if (cnt >= TOXIC_THRESH_POISON) {
            msg = ChatColor.GOLD + "⚠ Слишком много дыма... тошнит!";
            col = ChatColor.GOLD;
            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 15*20, 1, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15*20, 1, false, false, false));
        } else if (cnt >= TOXIC_THRESH_NAUSEA) {
            msg = ChatColor.YELLOW + "Голова кругом от дыма...";
            col = ChatColor.YELLOW;
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 10*20, 0, false, false, false));
        }
        if (msg != null) {
            p.sendMessage(col + msg);
            p.sendActionBar(col + "Токсичность: " + cnt + "/" + TOXIC_THRESH_DEATH);
        }
    }

    // Сброс токсичности через 60 секунд после последней затяжки (тиками чистим) + убираем флаг смерти
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeathClear(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        UUID uid = p.getUniqueId();
        nicotineHits.remove(uid);
        overdoseDeaths.remove(uid);
        stopSmoke(uid);
    }

    // Подмена сообщения о смерти от передозировки
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOverdoseDeath(PlayerDeathEvent e) {
        Player v = e.getEntity();
        if (overdoseDeaths.remove(v.getUniqueId())) {
            e.setDeathMessage(ChatColor.RED + v.getName() + ChatColor.DARK_RED + " умер от передозировки никотином.");
        }
    }

    // ==================== ВИРУС МЯУКАНЬЯ: ядро логики ====================
    private int getMeowStage(Player p) {
        return meowStage.getOrDefault(p.getUniqueId(), 0);
    }
    private void setMeowStage(Player p, int s) {
        UUID uid = p.getUniqueId();
        s = Math.max(0, Math.min(MEOW_MAX_STAGE, s));
        if (s <= 0) {
            meowStage.remove(uid);
            meowLastSound.remove(uid);
            meowLastChat.remove(uid);
            meowLastCough.remove(uid);
            p.getPersistentDataContainer().remove(keyMeowStage);
        } else {
            meowStage.put(uid, s);
            p.getPersistentDataContainer().set(keyMeowStage, PersistentDataType.INTEGER, s);
        }
    }
    private void loadMeowStage(Player p) {
        Integer s = p.getPersistentDataContainer().get(keyMeowStage, PersistentDataType.INTEGER);
        if (s != null && s > 0 && s <= MEOW_MAX_STAGE) meowStage.put(p.getUniqueId(), s);
    }

    private boolean isImmune(Player p) {
        Long until = meowImmunityUntil.get(p.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    private void infectPlayer(Player p, int startStage) {
        if (isImmune(p)) {
            p.sendMessage(ChatColor.AQUA + "💉 Иммунитет после вакцины защищает вас от заражения!");
            return;
        }
        int cur = getMeowStage(p);
        if (cur == 0) {
            setMeowStage(p, Math.max(1, startStage));
            p.sendMessage(ChatColor.LIGHT_PURPLE + "🐱 Вы чувствуете странное першение в горле... кажется, вы заболели.");
        } else {
            setMeowStage(p, Math.min(MEOW_MAX_STAGE, cur + 1));
            p.sendMessage(ChatColor.LIGHT_PURPLE + "🐱 Болезнь прогрессирует... (стадия " + getMeowStage(p) + "/10)");
        }
    }
    private void cureMeow(Player p, boolean announce) {
        setMeowStage(p, 0);
        // Даём 5 мин иммунитета
        meowImmunityUntil.put(p.getUniqueId(), System.currentTimeMillis() + MEOW_VACCINE_IMMUNITY);
        if (announce)
            p.sendMessage(ChatColor.GREEN + "💉 Симптомы мяуканья отступили. Иммунитет действует 5 минут.");
    }
    private void clearAllMeow() {
        for (UUID uid : new ArrayList<>(meowStage.keySet())) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                p.getPersistentDataContainer().remove(keyMeowStage);
                p.sendMessage(ChatColor.GREEN + "💉 Глобальная вакцинация — вирус мяуканья излечен.");
                meowImmunityUntil.put(uid, System.currentTimeMillis() + MEOW_VACCINE_IMMUNITY);
            }
        }
        meowStage.clear();
        meowLastSound.clear();
        meowLastChat.clear();
        meowLastCough.clear();
    }

    /** Прогрессия болезни: раз в 8 минут у всех заражённых стадия +1 */
    private void tickMeowProgress() {
        if (!meowEnabled) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            int st = getMeowStage(p);
            if (st == 0 || st >= MEOW_MAX_STAGE) continue;
            setMeowStage(p, st + 1);
            p.sendMessage(ChatColor.LIGHT_PURPLE + "🐱 Болезнь усиливается... (стадия " + (st + 1) + "/10)");
            p.playSound(p.getLocation(), Sound.ENTITY_CAT_HISS, 0.5f, 1.0f);
            if (st + 1 == MEOW_MAX_STAGE) {
                p.sendMessage(ChatColor.RED + "😿 Ваша речь окончательно превратилась в мяуканье...");
                // На 10 стадии даём кошачьи баффы сразу
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 0, false, false, false));
            }
        }
    }

    /** Проверка заражения: здоровые игроки в радиусе 3 блоков от заражённых получают вирус */
    private void tickMeowInfect() {
        if (!meowEnabled) return;
        for (World w : Bukkit.getWorlds()) {
            for (Player healthy : w.getPlayers()) {
                if (getMeowStage(healthy) > 0) continue;
                boolean masked = isWearingMeowMask(healthy);
                double chance = masked ? MEOW_MASK_CHANCE : MEOW_INFECT_CHANCE;
                boolean inRange = false;
                for (Player sick : w.getPlayers()) {
                    if (sick.equals(healthy)) continue;
                    if (getMeowStage(sick) == 0) continue;
                    if (healthy.getLocation().distanceSquared(sick.getLocation()) <= MEOW_INFECT_RADIUS * MEOW_INFECT_RADIUS) {
                        inRange = true;
                        break;
                    }
                }
                if (!inRange) continue;
                if (ThreadLocalRandom.current().nextDouble() < chance) {
                    infectPlayer(healthy, 1);
                    w.playSound(healthy.getLocation(), Sound.ENTITY_CAT_HURT, 0.6f, 1.1f);
                }
            }
        }
    }

    /** Маска 🎭 — это кастомная маска plugin'а (PAPER c keyMask) надетая в слоте шлема */
    private boolean isWearingMeowMask(Player p) {
        ItemStack helmet = p.getInventory().getHelmet();
        return helmet != null && isMask(helmet);
    }

    /** Тиканье эффектов (звуки, фразы, прогрессия, кашель) раз в секунду */
    private void tickMeowEffects() {
        if (!meowEnabled) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            int stage = getMeowStage(p);
            if (stage == 0) continue;
            UUID uid = p.getUniqueId();
            long now = tickCounter;
            // Чем выше стадия, тем короче интервалы, но с добавкой +10 секунд на каждой стадии,
            // чтобы не слишком спамило. Минимум — 3 секунды, чтобы не было лага от звуков.
            long soundEvery = Math.max(60,  MEOW_SOUND_BASE_TICKS  / Math.max(1, stage) + MEOW_INTERVAL_ADD_TICKS);
            long chatEvery  = Math.max(100, MEOW_CHAT_BASE_TICKS   / Math.max(1, stage) + MEOW_INTERVAL_ADD_TICKS);
            long coughEvery = Math.max(80,  MEOW_COUGH_BASE_TICKS  / Math.max(1, stage-1) + MEOW_INTERVAL_ADD_TICKS/2);
            Long lastS = meowLastSound.getOrDefault(uid, 0L);
            Long lastC = meowLastChat.getOrDefault(uid, 0L);
            Long lastK = meowLastCough.getOrDefault(uid, 0L);

            // Звуки котов
            if (now - lastS >= soundEvery) {
                meowLastSound.put(uid, now);
                String snd = CAT_SOUNDS[ThreadLocalRandom.current().nextInt(CAT_SOUNDS.length)];
                playSoundSafe(p.getWorld(), p.getLocation(), 1.0f,
                        0.8f + ThreadLocalRandom.current().nextFloat() * 0.6f,
                        snd, "ENTITY_CAT_AMBIENT");
            }

            // Кашель/чихание (частицы CLOUD + звук, начиная со 2 стадии)
            if (stage >= 2 && now - lastK >= coughEvery) {
                meowLastCough.put(uid, now);
                Location loc = p.getEyeLocation();
                Vector dir = p.getEyeLocation().getDirection();
                Location cough = loc.clone().add(dir.multiply(0.5));
                p.getWorld().spawnParticle(Particle.CLOUD, cough, stage >= 8 ? 20 : 8, 0.15, 0.1, 0.15, 0.02);
                // Звук чихания. В новых версиях Paper это ENTITY_PANDA_SNEEZE; на случай
                // если в сборке его нет — есть фоллбэк на лису/ламу.
                playSoundSafe(p.getWorld(), p.getLocation(), 0.7f,
                        0.8f + ThreadLocalRandom.current().nextFloat()*0.4f,
                        "ENTITY_PANDA_SNEEZE", "ENTITY_FOX_SPIT", "ENTITY_LLAMA_SPIT");
                // На 8+ стадии кашель накладывает слепоту на 1.5 сек
                if (stage >= 8 && ThreadLocalRandom.current().nextFloat() < 0.4f) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false, false));
                }
            }

            // Негативные эффекты по стадиям
            if (stage >= 7)
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, false, false, false));
            if (stage >= 9)
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false, false));
            // Кошачьи баффы на 10 стадии (взамен потерянной речи)
            if (stage == MEOW_MAX_STAGE) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, false, false, false));
                // Кошки идут за игроком: приручаем ближайших котов в радиусе 6
                for (Entity e : p.getNearbyEntities(6, 4, 6)) {
                    if (e instanceof Cat cat && !cat.isSitting()) {
                        cat.setOwner(p);
                        cat.setTarget(null);
                    }
                }
            }

            // Случайные фразы в чат (автоматически вырываются у заражённого, как обычное сообщение)
            boolean speak = false;
            if (stage == MEOW_MAX_STAGE && now - lastC >= Math.max(60, chatEvery/2)) speak = true;
            else if (stage < MEOW_MAX_STAGE && now - lastC >= chatEvery && ThreadLocalRandom.current().nextFloat() < 0.6f) speak = true;
            if (speak) {
                meowLastChat.put(uid, now);
                String mew = MEOW_RANDOM_MESSAGES[ThreadLocalRandom.current().nextInt(MEOW_RANDOM_MESSAGES.length)];
                // Ник оставляем как есть (displayName с цветами/префиксами), при маске — ???? серым
                Component nick = isMasked(p)
                        ? Component.text(maskName, NamedTextColor.GRAY)
                        : p.displayName();
                Component out = Component.text("<", NamedTextColor.WHITE)
                        .append(nick)
                        .append(Component.text("> ", NamedTextColor.WHITE))
                        // Убираем §7 из случайной фразы; цвет сообщения стандартный (белый как обычный чат)
                        .append(Component.text(ChatColor.stripColor(mew), NamedTextColor.WHITE));
                for (Player viewer : p.getWorld().getPlayers()) viewer.sendMessage(out);
                Bukkit.getConsoleSender().sendMessage(out);
            }
        }
    }

    /** Криперы/фантомы не трогают игроков на 10 стадии (кошки их пугают) */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMeowEntityTarget(EntityTargetEvent e) {
        if (!(e.getTarget() instanceof Player p)) return;
        if (getMeowStage(p) != MEOW_MAX_STAGE) return;
        if (e.getEntity() instanceof Creeper || e.getEntity() instanceof Phantom) {
            e.setCancelled(true);
            // Крипер убегает
            if (e.getEntity() instanceof Creeper c && c.getPathfinder() != null) {
                c.getPathfinder().stopPathfinding();
                Vector away = c.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.3).setY(0.3);
                c.setVelocity(c.getVelocity().add(away));
            }
        }
    }

    /**
     * Искажает фразу игрока в зависимости от стадии:
     *  - шанс срабатывания искажения линейно растёт: 10% на 1 стадии → 100% на 10.
     *  - если решило искажать: вставляет случайные "муррр/мяу/мррр" между словами,
     *    иногда добавляет префикс/суффикс. На ранних стадиях только лёгкий суффикс,
     *    на поздних — полный хаос со множеством вставок.
     */
    private String distortMessage(String msg, int stage) {
        if (stage <= 0) return msg;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        // Линейный шанс: 10% на 1 стадии, +10% на каждую следующую → 100% на 10
        float chance = Math.min(1.0f, 0.10f + 0.10f * (stage - 1));
        if (rnd.nextFloat() > chance) return msg;

        String[] words = msg.split("\\s+");
        StringBuilder sb = new StringBuilder();
        // Шанс вставить префикс (выше на поздних стадиях)
        float prefixChance = stage >= 8 ? 0.55f : stage >= 5 ? 0.30f : 0.10f;
        if (rnd.nextFloat() < prefixChance)
            sb.append(MEOW_PREFIXES[rnd.nextInt(MEOW_PREFIXES.length)]);

        // Шанс вставить мяуканье между двумя словами (выше на поздних стадиях)
        float insertChance = 0.15f + 0.08f * (stage - 1); // 0.15 → 0.87
        for (int i = 0; i < words.length; i++) {
            sb.append(words[i]);
            if (i < words.length - 1) {
                if (rnd.nextFloat() < insertChance) {
                    String ins = MEOW_INSERTS[rnd.nextInt(MEOW_INSERTS.length)];
                    String sep = MEOW_SUFFIXES[rnd.nextInt(MEOW_SUFFIXES.length)];
                    sb.append(" ").append(ins).append(sep).append(" ");
                } else {
                    sb.append(" ");
                }
            }
        }
        // Суффикс в конце — тем чаще, чем выше стадия
        float suffixChance = 0.25f + 0.075f * stage; // 0.325 → 1.0
        if (rnd.nextFloat() < suffixChance) {
            String suf = MEOW_SUFFIXES[rnd.nextInt(MEOW_SUFFIXES.length)];
            // Иногда добавляем полноценное мяу в конце
            if (stage >= 7 && rnd.nextFloat() < 0.4f) {
                sb.append(" ").append(MEOW_INSERTS[rnd.nextInt(MEOW_INSERTS.length)]).append(suf);
            } else {
                sb.append(suf);
            }
        }
        return sb.toString();
    }

    // Рендерер чата и искажение сообщений теперь обработаны в onChat/onChatLegacy выше.

    // Сохраняем стадию при выходе (и при логауте не сбрасываем — вирус остаётся!)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMeowQuit(PlayerQuitEvent e) {
        // Состояние вируса хранится в meowStage, не очищаем при выходе — игрок останется заражённым
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMeowDeath(PlayerDeathEvent e) {
        // Смерть сбрасывает вирус? Оставим как есть — вирус после возрождения сохраняется.
        // Если нужно сбрасывать при смерти — раскомментировать cureMeow.
    }

    // ==================== САМОНАВОДЯЩИЙСЯ ЛУК ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHomingBowUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isHomingBow(hand)) return;

        // Shift+ЛКМ — переключить режим (игроки / все / мобы)
        if (p.isSneaking() && e.getAction().name().startsWith("LEFT_CLICK")) {
            e.setCancelled(true);
            int mode = (getHomingMode(hand) + 1) % 3;
            int amount = hand.getAmount();
            p.getInventory().setItemInMainHand(buildHomingBow(amount, mode));
            String t = switch (mode) {
                case HOMING_MODE_ALL  -> ChatColor.GOLD + "игроки + мобы";
                case HOMING_MODE_MOBS -> ChatColor.RED + "только мобы";
                default              -> ChatColor.AQUA + "только игроки";
            };
            p.sendActionBar(ChatColor.LIGHT_PURPLE + "🏹 Лук-самонавод: " + t);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f,
                    mode == HOMING_MODE_PLAYERS ? 0.9f : mode == HOMING_MODE_ALL ? 1.2f : 1.5f);
            return;
        }
    }

    /** Тик удержания цели (когда натягивается тетива) и полёт самонаводящихся стрел */
    private void tickHoming() {
        // ---- 1) Захват цели / перманентное удержание ----
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uid = p.getUniqueId();
            ItemStack hand = p.getInventory().getItemInMainHand();
            boolean bowInHand = isHomingBow(hand);
            boolean isCharging = bowInHand
                    && p.getActiveItem() != null
                    && p.getActiveItem().getType() == Material.BOW
                    && p.isHandRaised();
            HomingLock cur = homingLocks.get(uid);

            if (!isCharging) {
                // Захват ПЕРМАНЕНТНЫЙ: не сбрасываем cur когда игрок убирает лук/перестаёт заряжать;
                // сбрасывается только: смерть, выход, отпускание без выстрела НЕ сбрасывает.
                // Но если цели уже нет — сбрасываем.
                if (cur != null && (cur.target == null || cur.target.isDead() || !cur.target.isValid()
                        || cur.target instanceof Player tp && (!tp.isOnline() || tp.getWorld() != p.getWorld()))) {
                    homingLocks.remove(uid);
                    p.sendActionBar("");
                }
                if (cur != null && cur.progress >= HOMING_LOCK_TICKS) {
                    // показываем "цель захвачена" когда держим лук
                    String tn = (cur.target instanceof Player pl) ? pl.getName() : formatEntityName(cur.target.getType());
                    p.sendActionBar(ChatColor.LIGHT_PURPLE + "🏹 §a✓ Цель: §f" + tn);
                }
                continue;
            }

            int mode = getHomingMode(hand);
            // Если уже есть захваченная цель — проверяем что она валидна; если да — держим, не перезахватываем
            if (cur != null && cur.progress >= HOMING_LOCK_TICKS && cur.target != null
                    && !cur.target.isDead() && cur.target.isValid()
                    && p.getWorld().equals(cur.target.getWorld())
                    && p.getLocation().distanceSquared(cur.target.getLocation()) <= HOMING_LOCK_RANGE*HOMING_LOCK_RANGE
                    && isValidTarget(cur.target, mode, p)) {
                String tn = (cur.target instanceof Player pl) ? pl.getName() : formatEntityName(cur.target.getType());
                p.sendActionBar(ChatColor.LIGHT_PURPLE + "🏹 §a▮▮▮▮▮▮▮▮▮▮ §f" + tn + " §a✓");
                // Магические кольца-частицы вокруг захваченной цели (визуал наводки)
                if (tickCounter % 5 == 0) {
                    Location c = cur.target.getLocation().add(0, cur.target.getHeight()*0.6, 0);
                    try { p.spawnParticle(Particle.ENCHANTED_HIT, c, 4, 0.35, 0.5, 0.35, 0.05); }
                    catch (Throwable ignored) {}
                    try { p.spawnParticle(Particle.END_ROD, c, 2, 0.3, 0.4, 0.3, 0.02); }
                    catch (Throwable ignored) {}
                }
                // Редкий звук биения прицела
                if (tickCounter % 30 == 0) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 1.8f);
                continue;
            }

            // Ищем новую цель (без требования line-of-sight — проще захват; но угол шире)
            Entity target = findHomingTarget(p, mode);
            if (target == null) {
                if (cur != null && cur.progress < HOMING_LOCK_TICKS) { homingLocks.remove(uid); p.sendActionBar(""); }
                continue;
            }
            if (cur == null || cur.target == null || cur.target.getEntityId() != target.getEntityId()) {
                cur = new HomingLock(target, mode);
                homingLocks.put(uid, cur);
            }
            cur.target = target; cur.mode = mode;
            cur.progress = Math.min(HOMING_LOCK_TICKS, cur.progress + 1);
            String targetName = (target instanceof Player pl) ? pl.getName() : formatEntityName(target.getType());
            int bars = (int) Math.round(cur.progress * 10.0 / HOMING_LOCK_TICKS);
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < 10; i++) bar.append(i < bars ? "§a▮" : "§7▯");
            p.sendActionBar(ChatColor.LIGHT_PURPLE + "🏹 " + bar + " §f" + targetName
                    + (cur.progress >= HOMING_LOCK_TICKS ? " §a✓ ЗАХВАЧЕНО" : ""));
        }

        // ---- 2) Полёт самонаводящихся стрел (с упреждением и гравитационной компенсацией) ----
        for (World w : Bukkit.getWorlds()) {
            for (Arrow arrow : w.getEntitiesByClass(Arrow.class)) {
                var pdc = arrow.getPersistentDataContainer();
                if (!pdc.has(keyHomingArrow, PersistentDataType.BYTE)) continue;
                String targetStr = pdc.get(keyHomingTarget, PersistentDataType.STRING);
                if (targetStr == null) { pdc.remove(keyHomingArrow); pdc.remove(keyHomingTarget); continue; }

                Entity target = null;
                try {
                    UUID tu = UUID.fromString(targetStr);
                    target = Bukkit.getEntity(tu);
                } catch (Exception ignored) {}

                if (target == null || target.isDead() || !target.isValid()
                        || arrow.isInBlock() || arrow.isOnGround()
                        || arrow.getLocation().distanceSquared(target.getLocation()) > HOMING_MAX_DIST*HOMING_MAX_DIST) {
                    pdc.remove(keyHomingArrow);
                    pdc.remove(keyHomingTarget);
                    continue;
                }

                Location aloc = arrow.getLocation();
                // Точка прицеливания = центр hitbox'а цели
                Location tloc = target.getLocation().add(0, target.getHeight()*0.55, 0);

                // Итеративное упреждение (Ньютон): считаем примерное время подлёта и двигаем
                // точку прицеливания вперёд по скорости цели, учитывая и скорость стрелы и гравитацию.
                Vector tVel = target.getVelocity();
                double dist = aloc.distance(tloc);
                // Итерации уточнения упреждения (2 прохода)
                Location aim = tloc.clone();
                for (int iter = 0; iter < 2; iter++) {
                    double pd = aloc.distance(aim);
                    double t = pd / HOMING_SPEED / 20.0;
                    aim = tloc.clone().add(tVel.clone().multiply(t * 20.0));
                }

                // Вектор желательного направления — на упреждённую точку
                Vector wantDir = aim.toVector().subtract(aloc.toVector());
                if (wantDir.lengthSquared() < 0.05) continue;
                wantDir = wantDir.normalize();

                Vector vel = arrow.getVelocity();
                double curSpeed = vel.length();
                if (curSpeed < 0.3) { pdc.remove(keyHomingArrow); pdc.remove(keyHomingTarget); continue; }
                Vector curDir = vel.clone().normalize();

                // Доля поворота: чем ближе — тем резче доворачиваем
                double distFactor = Math.max(0.0, Math.min(1.0, (20.0 - dist) / 20.0));
                double turn = HOMING_TURN_RATE + (HOMING_TURN_RATE_NEAR - HOMING_TURN_RATE) * distFactor;

                // Плавная интерполяция направления (SLERP-аппроксимация)
                Vector newDir = curDir.multiply(1.0 - turn).add(wantDir.multiply(turn)).normalize();

                // Компенсация гравитации: поднимаем нос стрелы чуть вверх, чтобы не падала
                Vector newVel = newDir.clone().multiply(HOMING_SPEED);
                newVel.setY(newVel.getY() + HOMING_GRAVITY_COMP);

                arrow.setVelocity(newVel);

                // Частицы следа: смесь END_ROD + ENCHANTED_HIT — магическо-светящийся хвост
                if (tickCounter % 2 == 0) {
                    w.spawnParticle(Particle.END_ROD, aloc, 2, 0.04, 0.04, 0.04, 0.0);
                    try { w.spawnParticle(Particle.ENCHANTED_HIT, aloc, 3, 0.08,0.08,0.08,0.05); }
                    catch (Throwable ignored) {
                        try { w.spawnParticle(Particle.valueOf("CRIT_MAGIC"), aloc, 3, 0.08,0.08,0.08,0.05); }
                        catch (Throwable ignored2) {}
                    }
                }
                // Звук свиста редко
                if (tickCounter % 8 == 0) w.playSound(aloc, Sound.ENTITY_ARROW_SHOOT, 0.25f, 1.6f);

                // Авопопадание: если мы вплотную к цели — наносим урон вручную и гасим стрелу,
                // чтобы не пролетала мимо из-за тик-рейта
                if (dist < 1.2 && target instanceof LivingEntity living) {
                    double dmg = 7.0; // урон как полноценный выстрел
                    if (target instanceof Player) dmg = 6.0;
                    living.damage(dmg, (arrow.getShooter() instanceof LivingEntity sh) ? sh : null);
                    w.spawnParticle(Particle.CRIT, aim, 10, 0.2,0.2,0.2,0.15);
                    playSoundSafe(w, aim, 1.0f, 1.3f, "ENTITY_ARROW_HIT", "ITEM_CROSSBOW_HIT");
                    arrow.remove();
                    pdc.remove(keyHomingArrow);
                    pdc.remove(keyHomingTarget);
                }
            }
        }
    }

    private boolean isValidTarget(Entity ent, int mode, Player owner) {
        boolean isPlayer = ent instanceof Player;
        if (ent.equals(owner)) return false;
        if (isPlayer) {
            if (mode == HOMING_MODE_MOBS) return false;
            Player tp = (Player) ent;
            return tp.isOnline() && !tp.isDead() && tp.isValid()
                    && tp.getGameMode() != GameMode.CREATIVE
                    && tp.getGameMode() != GameMode.SPECTATOR;
        } else {
            if (mode == HOMING_MODE_PLAYERS) return false;
            if (!(ent instanceof LivingEntity)) return false;
            if (ent instanceof ArmorStand) return false;
            return !((LivingEntity) ent).isDead() && ent.isValid();
        }
    }

    private String formatEntityName(EntityType t) {
        String n = t.getKey().getKey(); // minecraft:zombie_piglin
        return n.replace('_', ' ');
    }

    /** Найти лучшую цель для самонаведения в пределах угла/дальности, подходящую под режим */
    private Entity findHomingTarget(Player p, int mode) {
        Location eye = p.getEyeLocation();
        Vector look = eye.getDirection().normalize();
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        double cosLock = Math.cos(Math.toRadians(HOMING_LOCK_ANGLE));
        boolean wantPlayers = (mode == HOMING_MODE_PLAYERS || mode == HOMING_MODE_ALL);
        boolean wantMobs    = (mode == HOMING_MODE_MOBS    || mode == HOMING_MODE_ALL);

        Collection<Entity> nearby = p.getWorld().getNearbyEntities(eye, HOMING_LOCK_RANGE, HOMING_LOCK_RANGE, HOMING_LOCK_RANGE);
        for (Entity ent : nearby) {
            if (ent.equals(p)) continue;
            if (ent instanceof Item || ent instanceof ExperienceOrb || ent instanceof AbstractArrow
                    || ent instanceof FallingBlock || ent instanceof Fireball) continue;
            if (!isValidTarget(ent, mode, p)) continue;
            Vector to = ent.getLocation().add(0, ent.getHeight()*0.5, 0).toVector().subtract(eye.toVector());
            double dist = to.length();
            if (dist < 1.5 || dist > HOMING_LOCK_RANGE) continue;
            to = to.normalize();
            double dot = look.dot(to);
            if (dot < cosLock) continue;
            // Требование line-of-sight убираем, чтобы захватывало и за тонкими стенами/стеклом;
            // но если сплошная стена 2+ блока — hasLineOfSight всё же учитываем с пониженным штрафом
            boolean sees = p.hasLineOfSight(ent);
            if (!sees) {
                // За стеной назначаем штраф по расстоянию, чтобы приоритет был у видимых целей
                dist *= 1.6;
            }
            double score = dist + (1.0 - dot) * 6.0;
            if (score < bestScore) { bestScore = score; best = ent; }
        }
        return best;
    }

    /** При выстреле из самонаводящегося лука — помечаем стрелу и вешаем на неё цель (если успели захватить) */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHomingShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getBow() == null || !isHomingBow(e.getBow())) return;
        if (!(e.getProjectile() instanceof Arrow arrow)) return;
        UUID uid = p.getUniqueId();
        HomingLock lock = homingLocks.get(uid);

        // Если нет захваченной цели — стрела летит как обычная (без самонаведения)
        if (lock == null || lock.progress < HOMING_LOCK_TICKS || lock.target == null
                || lock.target.isDead() || !lock.target.isValid()) {
            return;
        }

        Entity tgt = lock.target;
        // Стрела с самонаведением
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setCritical(false);
        resetArrowKnockback(arrow);
        var pdc = arrow.getPersistentDataContainer();
        pdc.set(keyHomingArrow, PersistentDataType.BYTE, (byte)1);
        // Используем UUID цели — работает и для игроков, и для мобов в современном Bukkit
        pdc.set(keyHomingTarget, PersistentDataType.STRING, tgt.getUniqueId().toString());
        try { arrow.setShooter(p); } catch (Exception ignored) {}

        // Задаём НАЧАЛЬНОЕ направление прямо на цель (с лёгким упреждением),
        // остальную доводку стрела сделает в полёте сама
        Location aloc = arrow.getLocation();
        Location aim = tgt.getLocation().add(0, tgt.getHeight()*0.5, 0).add(tgt.getVelocity().multiply(0.15));
        Vector dir = aim.toVector().subtract(aloc.toVector());
        if (dir.lengthSquared() > 0.01) {
            arrow.setVelocity(dir.normalize().multiply(HOMING_SPEED * 0.95));
        }
        // НЕ сбрасываем homingLocks — захват ПЕРМАНЕНТНЫЙ (до Shift+ПКМ/смерти/выхода).
        // Это значит следующий выстрел тоже полетит в ту же цель — удобно для боя.

        p.playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.7f, 1.4f);
        playSoundSafe(p.getWorld(), p.getLocation(), 0.6f, 1.8f, "BLOCK_AMETHYST_BLOCK_RESONATE", "BLOCK_NOTE_BLOCK_PLING");
        // Фейерверк частиц при магическом выстреле
        try { p.spawnParticle(Particle.ENCHANTED_HIT, p.getEyeLocation(), 15, 0.3, 0.2, 0.3, 0.1); }
        catch (Throwable t) {
            try { p.spawnParticle(Particle.valueOf("CRIT_MAGIC"), p.getEyeLocation(), 15, 0.3, 0.2, 0.3, 0.1); }
            catch (Throwable ignored) {}
        }
        try { p.spawnParticle(Particle.END_ROD, p.getEyeLocation(), 6, 0.2,0.15,0.2,0.05); }
        catch (Throwable ignored) {}
    }

    @SuppressWarnings({"deprecation", "removal"})
    private void resetArrowKnockback(AbstractArrow arrow) {
        try { arrow.setKnockbackStrength(0); } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onHomingItemSwap(PlayerItemHeldEvent e) {
        // Не сбрасываем при смене предмета — захват перманентный (до Shift+ПКМ / смерти / выхода)
    }

    /** Shift+ПКМ с луком сбрасывает захват */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHomingRelease(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHomingBow(p.getInventory().getItemInMainHand())) return;
        if (!p.isSneaking()) return;
        if (!e.getAction().name().startsWith("RIGHT_CLICK")) return;
        UUID uid = p.getUniqueId();
        if (homingLocks.remove(uid) != null) {
            e.setCancelled(true);
            p.sendActionBar(ChatColor.RED + "🏹 Захват сброшен");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 0.6f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHomingDeath(PlayerDeathEvent e) {
        homingLocks.remove(e.getEntity().getUniqueId());
        // Если смерть цели — сбрасываем и у того, кто её держал на прицеле
        UUID dead = e.getEntity().getUniqueId();
        homingLocks.entrySet().removeIf(en -> en.getValue() != null
                && en.getValue().target != null
                && en.getValue().target.getUniqueId().equals(dead));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHomingQuit(PlayerQuitEvent e) {
        homingLocks.remove(e.getPlayer().getUniqueId());
    }

    // ==================== ПОВОДОК ДЛЯ ИГРОКОВ ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLeashUse(PlayerInteractAtEntityEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isLeashItem(hand)) return;
        // Отменяем ванильное поведение поводка на мобах
        e.setCancelled(true);
        if (!(e.getRightClicked() instanceof Player target)) return;
        UUID uid = p.getUniqueId();
        UUID tid = target.getUniqueId();

        // Нельзя связать самого себя
        if (tid.equals(uid)) return;

        // Сброс: Shift+ПКМ с поводком — отпустить кого держим
        if (p.isSneaking()) {
            LeashTie tie = leashTies.remove(uid);
            if (tie != null) {
                leashEscapeClicks.remove(tie.victim);
                Player vic = Bukkit.getPlayer(tie.victim);
                if (vic != null) vic.sendMessage(ChatColor.YELLOW + "🪢 " + p.getName() + " отпустил(а) вас.");
                p.sendMessage(ChatColor.GREEN + "🪢 Вы отпустили поводок.");
                playSoundSafe(p.getWorld(), p.getLocation(), 0.8f, 1.0f,
                        "ENTITY_LEASH_KNOT_BREAK", "ITEM_LEAD_BREAK", "BLOCK_WOOD_BREAK");
            }
            return;
        }
        if (leashTies.containsKey(uid)) {
            p.sendMessage(ChatColor.RED + "🪢 У вас уже есть кто-то на поводке (Shift+ПКМ чтобы отпустить).");
            return;
        }
        if (p.getLocation().distanceSquared(target.getLocation()) > LEASH_RANGE*LEASH_RANGE) {
            p.sendMessage(ChatColor.RED + "Подойдите ближе (в пределах " + LEASH_RANGE + " блоков).");
            return;
        }
        if (target.isDead() || target.getGameMode() == GameMode.CREATIVE
                || target.getGameMode() == GameMode.SPECTATOR) {
            p.sendMessage(ChatColor.RED + "🪢 Этого игрока нельзя связать.");
            return;
        }
        // Нельзя привязать того, кто УЖЕ у кого-то на поводке (чтоб не цеплялись паровозиком)
        if (findLeashOnVictim(tid) != null) {
            p.sendMessage(ChatColor.RED + "🪢 Этот игрок уже на поводке.");
            return;
        }

        // === МГНОВЕННОЕ связывание (как ванильный поводок на мобах) ===
        leashTies.put(uid, new LeashTie(uid, tid, System.currentTimeMillis()));
        consumeHand(p);
        leashEscapeClicks.remove(tid);
        leashBind.remove(uid); // на всякий случай

        p.sendMessage(ChatColor.GREEN + "🪢 Вы накинули поводок на " + target.getName() + "!");
        target.sendMessage(ChatColor.RED + "🪢 " + p.getName() + " накинул(а) на вас поводок! " +
                "Вы сможете вырваться через 3 минуты (250 ПКМ).");
        // Визуал
        playSoundSafe(p.getWorld(), target.getLocation(), 1.0f, 1.0f,
                "ITEM_LEAD_PLACE", "BLOCK_LEASH_KNOT_PLACE", "ENTITY_LEASH_KNOT_PLACE",
                "ENTITY_HORSE_SADDLE");
        for (int i = 0; i < 12; i++) {
            target.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                    target.getLocation().add(0, 1 + ThreadLocalRandom.current().nextDouble(0.8), 0),
                    1, 0.3, 0.3, 0.3, 0.02);
        }
        try { target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 4, 0.3, 0.2, 0.3, 0.02); }
        catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLeashClickAir(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        UUID uid = p.getUniqueId();

        // ПКМ — попытка вырваться с поводка. Срабатывает ВСЕГДА если на игроке поводок,
        // неважно что у него в руке (жертва не обязана держать поводок).
        if (e.getAction().name().startsWith("RIGHT_CLICK")) {
            LeashTie selfTie = findLeashOnVictim(uid);
            if (selfTie != null) {
                e.setCancelled(true);
                long now = System.currentTimeMillis();
                if (now - selfTie.tiedAt < LEASH_ESCAPE_AFTER) {
                    long left = (LEASH_ESCAPE_AFTER - (now - selfTie.tiedAt)) / 1000;
                    long min = left/60, sec = left%60;
                    p.sendActionBar(ChatColor.RED + "🪢 Поводок держит крепко. Вырваться можно через " + min + "м " + sec + "с");
                    return;
                }
                int clicks = leashEscapeClicks.getOrDefault(uid, 0) + 1;
                leashEscapeClicks.put(uid, clicks);
                p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0,1,0), 2, 0.2,0.2,0.2,0.0);
                if (clicks >= LEASH_ESCAPE_CLICKS) {
                    leashTies.remove(selfTie.owner);
                    leashEscapeClicks.remove(uid);
                    Player owner = Bukkit.getPlayer(selfTie.owner);
                    p.sendMessage(ChatColor.GREEN + "🪢 Вы вырвались с поводка!");
                    if (owner != null) owner.sendMessage(ChatColor.RED + "🪢 " + p.getName() + " вырвался(лась) с поводка!");
                    playSoundSafe(p.getWorld(), p.getLocation(), 1.0f, 1.0f,
                            "ENTITY_LEASH_KNOT_BREAK", "ITEM_LEAD_BREAK", "BLOCK_WOOD_BREAK");
                } else {
                    int pct = (100*clicks)/LEASH_ESCAPE_CLICKS;
                    p.sendActionBar(ChatColor.GOLD + "🪢 Вырываетесь... " + pct + "% (" + clicks + "/" + LEASH_ESCAPE_CLICKS + ")");
                }
                return;
            }
        }

        // Дальше логика для ДЕРЖАТЕЛЯ поводка (нужно держать предмет в руке)
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isLeashItem(hand)) return;

        // ЛКМ по блоку — привязать жертву к блоку (только у кого в руке поводок)
        if (e.getAction().name().startsWith("LEFT_CLICK_BLOCK")) {
            LeashTie tie = leashTies.get(uid);
            if (tie == null) return;
            Block b = e.getClickedBlock();
            if (b == null || !b.getType().isSolid()) return;
            e.setCancelled(true);
            // Снимаем крепление с блока если повторно ЛКМ по тому же блоку
            if (tie.blockLoc != null && tie.blockWorld != null
                    && tie.blockWorld.equals(b.getWorld())
                    && tie.blockLoc.getBlockX() == b.getRelative(e.getBlockFace()).getX()
                    && tie.blockLoc.getBlockY() == b.getRelative(e.getBlockFace()).getY()
                    && tie.blockLoc.getBlockZ() == b.getRelative(e.getBlockFace()).getZ()) {
                tie.blockLoc = null;
                tie.blockWorld = null;
                p.sendMessage(ChatColor.YELLOW + "🪢 Вы отвязали игрока от блока.");
                Player vic = Bukkit.getPlayer(tie.victim);
                if (vic != null) vic.sendMessage(ChatColor.YELLOW + "🪢 Вас отвязали от блока — теперь вас тащит за собой " + p.getName() + ".");
                return;
            }
            tie.blockLoc = b.getRelative(e.getBlockFace()).getLocation();
            tie.blockWorld = b.getWorld();
            Player vic = Bukkit.getPlayer(tie.victim);
            if (vic != null) {
                vic.sendMessage(ChatColor.GOLD + "🪢 Вас привязали к блоку!");
            }
            p.sendMessage(ChatColor.GREEN + "🪢 Вы привязали игрока к блоку.");
            playSoundSafe(p.getWorld(), b.getLocation(), 0.6f, 0.9f,
                    "BLOCK_FENCE_GATE_CLOSE", "BLOCK_FENCE_GATE_OPEN",
                    "BLOCK_WOOD_PLACE", "ITEM_LEAD_PLACE");
        }
    }

    private LeashTie findLeashOnVictim(UUID victim) {
        for (LeashTie t : leashTies.values()) if (t.victim.equals(victim)) return t;
        return null;
    }

    /** Тик поводка: натягивает верёвку, тянет жертву к хозяину/блоку, рендер верёвки */
    private void tickLeash() {
        long now = System.currentTimeMillis();
        // Прогресс удержания больше не нужен — поводок вяжется моментально по ПКМ.
        // Очищаем случайно зависшие записи.
        leashBind.entrySet().removeIf(e -> {
            Player o = Bukkit.getPlayer(e.getKey());
            return o == null || !o.isOnline();
        });

        Iterator<Map.Entry<UUID, LeashTie>> it = leashTies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LeashTie> en = it.next();
            LeashTie tie = en.getValue();
            Player owner = Bukkit.getPlayer(tie.owner);
            Player victim = Bukkit.getPlayer(tie.victim);

            // Если владелец вышел/умер или жертва вышла/умерла — сбрасываем
            boolean broken = false;
            if (owner == null || victim == null || !owner.isOnline() || !victim.isOnline() || victim.isDead()) {
                broken = true;
            }
            // Если поводок не к блоку и у владельца больше нет предмета-поводка в руке (он его выбросил/передал) —
            // НЕ сбрасываем сразу (пусть держится пока держит); но если сменил мир — обрываем
            if (!broken && owner.getWorld() != victim.getWorld()) broken = true;
            // Блок-якорь остался в другом мире — обрываем
            if (!broken && tie.blockLoc != null && tie.blockWorld != null
                    && !tie.blockWorld.equals(victim.getWorld())) broken = true;

            if (broken) {
                if (victim != null && victim.isOnline())
                    victim.sendMessage(ChatColor.GRAY + "🪢 Поводок отвязался.");
                if (owner != null && owner.isOnline())
                    owner.sendMessage(ChatColor.GRAY + "🪢 Поводок отвязался.");
                leashEscapeClicks.remove(tie.victim);
                playSoundSafe(victim != null ? victim.getWorld() : Bukkit.getWorlds().get(0),
                        victim != null ? victim.getLocation() : new Location(Bukkit.getWorlds().get(0),0,64,0),
                        0.8f, 1.0f, "ENTITY_LEASH_KNOT_BREAK", "ITEM_LEAD_BREAK", "BLOCK_WOOD_BREAK");
                it.remove();
                continue;
            }

            // Anchor = точка к которой тянем (либо блок, либо пояс владельца)
            Location anchor;
            boolean toBlock = (tie.blockLoc != null && tie.blockWorld != null);
            if (toBlock) {
                anchor = tie.blockLoc.clone().add(0.5, 0.5, 0.5);
            } else {
                anchor = owner.getLocation().add(0, 0.9, 0);
            }
            Location vic = victim.getLocation().add(0, 0.9, 0);
            double dist = vic.distance(anchor);
            double overshoot = dist - LEASH_TETHER;

            if (overshoot > 0) {
                // Сильнее тянуть когда дальше ушли; привязан к блоку — ещё сильнее
                double strength = toBlock ? LEASH_PULL_STRENGTH_BLOCK : LEASH_PULL_STRENGTH;
                double factor = Math.min(1.0, overshoot / 4.0);
                Vector dir = anchor.toVector().subtract(vic.toVector()).normalize();
                Vector pull = dir.multiply(strength * factor);
                // Чтоб притягивало и в воздухе и с земли
                Vector vVel = victim.getVelocity();
                // Обнуляем скорость ВДОЛЬ от анкера (тормозим убегание), но сохраняем падение/прыжок
                Vector away = dir.clone().multiply(-1);
                double awaySpeed = vVel.dot(away);
                if (awaySpeed > 0) vVel = vVel.subtract(away.multiply(awaySpeed * 0.6));
                vVel.add(pull);
                // Слегка приподнимаем если тянут по земле, чтобы не тормозило о блоки
                if (victim.isOnGround()) vVel.setY(Math.max(vVel.getY(), 0.2));
                victim.setVelocity(vVel);

                // Звук натяжения и урон только когда натяг сильный (дёргаем сильно)
                if (overshoot > 2.0 && tickCounter % 20 == 0) {
                    victim.damage(LEASH_DAMAGE_PER_PULL, owner);
                    victim.getWorld().spawnParticle(Particle.CRIT,
                            victim.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.02);
                }
            }

            // Рендер верёвки-частиц: гуще и ярче
            if (tickCounter % 2 == 0) {
                Location a = anchor.clone();
                Location b = vic.clone();
                int steps = Math.max(2, (int) Math.ceil(a.distance(b)) * 2);
                World w = a.getWorld();
                for (int i = 1; i < steps; i++) {
                    double t = (double) i / steps;
                    // Небольшое провисание посередине (чтоб выглядело как реальная верёвка)
                    double sag = Math.sin(Math.PI * t) * 0.2;
                    Location pp = a.clone().add(b.toVector().subtract(a.toVector()).multiply(t)).add(0, -sag, 0);
                    if (i % 3 == 0) {
                        try { w.spawnParticle(Particle.ENCHANTED_HIT, pp, 1, 0.01, 0.01, 0.01, 0.0); }
                        catch (Throwable err) {
                            try { w.spawnParticle(Particle.valueOf("CRIT_MAGIC"), pp, 1, 0.01,0.01,0.01,0.0); }
                            catch (Throwable ignored) {}
                        }
                    }
                }
                // Узелок у блока/игрока
                if (toBlock) {
                    try { w.spawnParticle(Particle.ENCHANTED_HIT, a, 3, 0.15,0.15,0.15,0.02); }
                    catch (Throwable ignored) {}
                }
            }

            // ActionBar для жертвы
            if (now - tie.tiedAt < LEASH_ESCAPE_AFTER) {
                long left = (LEASH_ESCAPE_AFTER - (now - tie.tiedAt))/1000;
                victim.sendActionBar(ChatColor.RED + "🪢 На вас поводок" + (toBlock ? " (к блоку)" : "") + "! Вырваться через " + (left/60) + "м " + (left%60) + "с");
            } else {
                int clicks = leashEscapeClicks.getOrDefault(tie.victim, 0);
                int pct = (100*clicks)/LEASH_ESCAPE_CLICKS;
                victim.sendActionBar(ChatColor.GOLD + "🪢 ПКМ чтобы вырваться — " + pct + "% (" + clicks + "/" + LEASH_ESCAPE_CLICKS + ")" + (toBlock ? " (к блоку)" : ""));
            }
            // Владельцу тоже показываем если он держит поводок
            if (!toBlock) {
                owner.sendActionBar(ChatColor.GOLD + "🪢 На поводке: §f" + victim.getName());
            }
        }
    }

    // ==================== ПЛУГ ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlowInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isPlow(hand)) return;

        // ПКМ в воздухе/не по интерактивному блоку — надеть на голову
        if (e.getAction().name().startsWith("RIGHT_CLICK")
                && (e.getClickedBlock() == null || !e.getClickedBlock().getType().isInteractable())) {
            ItemStack helmet = p.getInventory().getHelmet();
            if (helmet != null && helmet.getType() != Material.AIR) {
                p.sendMessage(ChatColor.RED + "Снимите шлем, чтобы надеть плуг.");
                return;
            }
            ItemStack worn = hand.clone();
            worn.setAmount(1);
            p.getInventory().setHelmet(worn);
            consumeHand(p);
            e.setCancelled(true);
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.8f, 1.0f);
        }
    }

    /** Список блоков под ногами игрока, которые мы уже обработали (чтобы не тратить прочность повторно) */
    private final Map<UUID, Long> plowLastPos = new HashMap<>();

    // Тик обработки плуга
    private void tickPlows() {
        if (tickCounter % PLOW_TICK_PERIOD != 0) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isWearingPlow(p)) continue;
            if (p.isFlying() || !p.isOnGround()) continue;

            ItemStack helmet = p.getInventory().getHelmet();
            if (helmet == null || !isPlow(helmet)) continue;

            Location ploc = p.getLocation();
            int px = ploc.getBlockX();
            int py = ploc.getBlockY();
            int pz = ploc.getBlockZ();

            // Ключ позиции (для защиты от двойной обработки)
            long posKey = ((long) px & 0x7FFFFFFL) << 38 | ((long) pz & 0x7FFFFFFL) << 7 | (py & 0x7FL);
            Long prev = plowLastPos.get(p.getUniqueId());
            if (prev != null && prev == posKey) continue;

            int actions = 0;
            for (int dx = -PLOW_RADIUS; dx <= PLOW_RADIUS; dx++) {
                for (int dz = -PLOW_RADIUS; dz <= PLOW_RADIUS; dz++) {
                    int bx = px + dx;
                    int bz = pz + dz;
                    // Ищем опорный блок в колонке: из-за того что пашня/тропинка имеют
                    // высоту 15/16, игрок «проваливается» на 1/16 и его стопы оказываются
                    // ВНУТРИ блока, а не над ним — поэтому проверяем стопы и 2 блока вниз.
                    Block b = findPlowGround(p.getWorld(), bx, bz, py);
                    if (b == null) continue;
                    Material t = b.getType();
                    Block above = b.getRelative(0, 1, 0);

                    // 1) Пашем землю в FARMLAND (если сверху воздух / трава / снег)
                    boolean canTill = (t == Material.GRASS_BLOCK || t == Material.DIRT
                                    || t == Material.DIRT_PATH || t == Material.ROOTED_DIRT
                                    || t == Material.COARSE_DIRT);
                    boolean aboveClear = above.getType().isAir()
                                    || above.getType() == Material.SNOW
                                    || above.getType() == Material.SHORT_GRASS;
                    if (canTill && aboveClear) {
                        b.setType(Material.FARMLAND);
                        if (above.getType() != Material.AIR) above.setType(Material.AIR);
                        actions++;
                        continue;
                    }

                    // 2) Созревшая культура над пашней — собрать и пересадить
                    if (isMatureCrop(above) && t == Material.FARMLAND) {
                        if (harvestAndReplant(p, above)) actions++;
                    }
                }
            }
            if (actions > 0) {
                // Тратим прочность шлема = количеству обработанных блоков
                damagePlow(helmet, p, actions);
                p.playSound(p.getLocation(), Sound.ITEM_HOE_TILL, 0.25f, 1.1f);
            }
            plowLastPos.put(p.getUniqueId(), posKey);
        }
    }

    /**
     * Найти опорный блок в колонке (bx, ?, bz) под игроком:
     * проверяем блок на уровне стоп и 2 блока вниз.
     * Пашня (FARMLAND) и земляная тропинка (DIRT_PATH) имеют высоту 15/16,
     * поэтому стопы игрока оказываются внутри самого блока — py, а не py-1.
     */
    private Block findPlowGround(World w, int bx, int bz, int startY) {
        for (int dy = 0; dy >= -2; dy--) {
            Block b = w.getBlockAt(bx, startY + dy, bz);
            Material t = b.getType();
            if (t == Material.AIR) continue;
            // Если это культура — пропускаем, ищем дальше вниз (случай, когда игрок стоит в пшенице)
            if (isMatureCrop(b) || t == Material.WHEAT || t == Material.CARROTS
                    || t == Material.POTATOES || t == Material.BEETROOTS || t == Material.NETHER_WART) {
                continue;
            }
            // Подходящий грунт (то что можно пахать, или уже пашня)
            if (t == Material.GRASS_BLOCK || t == Material.DIRT || t == Material.DIRT_PATH
                    || t == Material.ROOTED_DIRT || t == Material.COARSE_DIRT || t == Material.FARMLAND) {
                return b;
            }
            // Любой другой твёрдый блок — останавливаемся, пахать/собирать нечего
            if (t.isSolid()) return null;
        }
        return null;
    }

    private boolean isMatureCrop(Block b) {
        Material t = b.getType();
        if (t == Material.WHEAT || t == Material.CARROTS || t == Material.POTATOES
                || t == Material.BEETROOTS || t == Material.NETHER_WART) {
            if (b.getBlockData() instanceof org.bukkit.block.data.Ageable age) {
                return age.getAge() >= age.getMaximumAge();
            }
        }
        return false;
    }

    /** Наносит урон плугу, ломает его когда кончается прочность */
    private void damagePlow(ItemStack plow, Player owner, int amount) {
        if (owner.getGameMode() == GameMode.CREATIVE) return;
        if (!(plow.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg)) return;
        short maxDmg = plow.getType().getMaxDurability();
        int cur = dmg.getDamage() + amount;
        if (cur >= maxDmg - 1) {
            owner.getInventory().setHelmet(null);
            owner.playSound(owner.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
            return;
        }
        dmg.setDamage(cur);
        plow.setItemMeta((ItemMeta) dmg);
    }

    /** Собрать урожай, выдать дроп игроку и посадить заново (если есть семена). Возвращает true если собрал. */
    private boolean harvestAndReplant(Player p, Block crop) {
        Material type = crop.getType();
        Material seedMat;
        ItemStack produce;
        ItemStack seedsDrop = null;

        switch (type) {
            case WHEAT -> {
                produce = new ItemStack(Material.WHEAT, 1);
                seedsDrop = new ItemStack(Material.WHEAT_SEEDS, 1 + ThreadLocalRandom.current().nextInt(2));
                seedMat = Material.WHEAT_SEEDS;
            }
            case CARROTS -> {
                produce = new ItemStack(Material.CARROT, 2 + ThreadLocalRandom.current().nextInt(2));
                seedMat = Material.CARROT;
            }
            case POTATOES -> {
                int n = 2 + ThreadLocalRandom.current().nextInt(2);
                produce = new ItemStack(Material.POTATO, n);
                if (ThreadLocalRandom.current().nextFloat() < 0.02f)
                    seedsDrop = new ItemStack(Material.POISONOUS_POTATO, 1);
                seedMat = Material.POTATO;
            }
            case BEETROOTS -> {
                produce = new ItemStack(Material.BEETROOT, 1);
                seedsDrop = new ItemStack(Material.BEETROOT_SEEDS, 1 + ThreadLocalRandom.current().nextInt(2));
                seedMat = Material.BEETROOT_SEEDS;
            }
            case NETHER_WART -> {
                produce = new ItemStack(Material.NETHER_WART, 2 + ThreadLocalRandom.current().nextInt(2));
                seedMat = Material.NETHER_WART;
            }
            default -> { return false; }
        }

        // Даём основной продукт
        giveOrDrop(p, crop.getLocation(), produce);
        if (seedsDrop != null) giveOrDrop(p, crop.getLocation(), seedsDrop);

        // Пересадка: забираем 1 семя; в креативе сажаем всегда, без траты
        boolean planted = false;
        if (p.getGameMode() == GameMode.CREATIVE || consumeMaterial(p, seedMat, 1)) {
            var data = crop.getBlockData();
            if (data instanceof org.bukkit.block.data.Ageable age) {
                age.setAge(0);
                crop.setBlockData(data, true);
                planted = true;
            }
        }
        if (!planted) {
            crop.setType(Material.AIR, true);
        }
        p.spawnParticle(Particle.HAPPY_VILLAGER, crop.getLocation().add(0.5, 0.3, 0.5), 4, 0.2, 0.1, 0.2, 0.02);
        return true;
    }

    private void giveOrDrop(Player p, Location dropAt, ItemStack stack) {
        Map<Integer, ItemStack> leftover = p.getInventory().addItem(stack);
        for (ItemStack lo : leftover.values()) {
            p.getWorld().dropItemNaturally(dropAt, lo);
        }
    }

    /** Отнять amount предметов матерала из инвентаря (не ломает PDC-айтемы) */
    private boolean consumeMaterial(Player p, Material mat, int amount) {
        if (p.getGameMode() == GameMode.CREATIVE) return true;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (it == null || it.getType() != mat) continue;
            // не трогаем именные / кастомные предметы (плуг и т.п.)
            if (it.hasItemMeta() && it.getItemMeta().hasDisplayName()) continue;
            if (it.getAmount() > amount) {
                it.setAmount(it.getAmount() - amount);
                return true;
            }
            int take = it.getAmount();
            p.getInventory().setItem(i, null);
            amount -= take;
            if (amount <= 0) return true;
        }
        return amount <= 0;
    }

    // ==================== КАЛЬЯН ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHookahUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!e.getAction().name().startsWith("RIGHT_CLICK")) return;

        // 1) Установка кальяна — ПКМ предметом-кальяном по блоку
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (isHookahItem(hand)) {
            Block against = e.getClickedBlock();
            if (against == null) return;
            Block place = against.getRelative(e.getBlockFace());
            if (!place.getType().isAir() && !place.isReplaceable()) {
                p.sendMessage(ChatColor.RED + "Здесь нельзя поставить кальян.");
                return;
            }
            place.setType(Material.BREWING_STAND);
            long key = locKey(place.getLocation());
            hookahs.put(key, new Hookah(0, 0));
            consumeHand(p);
            p.getWorld().playSound(place.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.0f);
            p.sendMessage(ChatColor.GREEN + "Кальян поставлен. Забей табак ПКМ с табаком в руке.");
            return;
        }

        // 2) Взаимодействие с уже поставленным кальяном
        Block clicked = e.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.BREWING_STAND) return;
        long key = locKey(clicked.getLocation());
        if (!hookahs.containsKey(key)) return;
        e.setCancelled(true);
        Hookah h = hookahs.get(key);

        // 2а) ПКМ с табаком — забить чашу
        if (isTobaccoItem(hand)) {
            if (h.tobaccoType != 0 && h.puffsLeft > 0) {
                p.sendMessage(ChatColor.RED + "Чаша ещё не закончилась (тяжек осталось: " + h.puffsLeft + ").");
                return;
            }
            int type = getTobaccoType(hand);
            h.tobaccoType = type;
            h.puffsLeft = HOOKAH_PUFFS;
            consumeHand(p);
            String name = tobaccoName(type);
            clicked.getWorld().playSound(clicked.getLocation(), Sound.ITEM_CROP_PLANT, 0.7f, 1.2f);
            p.sendMessage(ChatColor.GREEN + "Чаша забита: " + name + ". ПКМ по кальяну — тяга.");
            return;
        }

        // 2б) ПКМ пустой рукой — тяга
        // Кулдаун
        Long cd = hookahCooldown.get(p.getUniqueId());
        long now = System.currentTimeMillis();
        if (cd != null && now < cd) {
            long wait = ((cd - now) / 1000L) + 1;
            p.sendMessage(ChatColor.GRAY + "Тяни помедленнее... (" + wait + " сек)");
            return;
        }
        if (h.tobaccoType == 0 || h.puffsLeft <= 0) {
            p.sendMessage(ChatColor.RED + "Чаша пуста — забей табак.");
            return;
        }
        h.puffsLeft--;
        hookahCooldown.put(p.getUniqueId(), now + HOOKAH_COOLDOWN * 50L);

        applyTobaccoEffects(p, h.tobaccoType);
        // Кальян тоже считается затяжкой для передозировки
        registerNicotinePuff(p);
        p.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.7f, 1.4f);
        for (int i = 0; i < 10; i++) {
            p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                    clicked.getLocation().add(0.5, 1.1, 0.5), 1,
                    0.15, 0.05, 0.15, 0.01);
        }

        if (h.puffsLeft <= 0) {
            h.tobaccoType = 0;
            p.sendMessage(ChatColor.GRAY + "Чаша закончилась — забей заново.");
        } else {
            p.sendMessage(ChatColor.GREEN + "Тяжка! Осталось: " + h.puffsLeft + "/" + HOOKAH_PUFFS);
        }
    }

    // Дроп кальяна при ломании
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHookahBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.BREWING_STAND) return;
        long key = locKey(b.getLocation());
        if (!hookahs.containsKey(key)) return;
        Hookah h = hookahs.remove(key);
        e.setDropItems(false);
        b.getWorld().dropItemNaturally(b.getLocation(), buildHookahItem(1));
        if (h.tobaccoType != 0) b.getWorld().dropItemNaturally(b.getLocation(), buildTobacco(1, h.tobaccoType));
    }

    private void applyTobaccoEffects(Player p, int type) {
        switch (type) {
            // === ДЕШЁВЫЕ / ВРЕДНЫЕ ===
            case TOBA_GARBAGE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 10*20, 1, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 5*20, 2, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 10*20, 0, false, false, false));
            }
            case TOBA_BURNT -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 8*20, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5*20, 1, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 3*20, 0, false, false, false));
            }
            case TOBA_CHEMICAL -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 15*20, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 10*20, 0, false, false, false));
            }
            // === СРЕДНИЕ ===
            case TOBA_DOUBLE_APPLE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 30*20, 0, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 5*20, 0, false, false, false));
            }
            case TOBA_GRAPE_MINT -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20*20, 1, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 10*20, 1, false, false, false));
            }
            case TOBA_BLUEBERRY -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20*20, 1, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 3*20, 0, false, false, false));
            }
            // === ДОРОГИЕ / ЧИСТЫЕ ===
            case TOBA_PEACH -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10*20, 1, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 5*20, 0, false, false, true));
            }
            case TOBA_TANGIERS -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 45*20, 1, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 30*20, 0, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 2*20, 0, false, false, false));
            }
            case TOBA_DIAMOND -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 30*20, 2, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 15*20, 1, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 30*20, 0, false, false, true));
            }
            case TOBA_GODS -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60*20, 1, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60*20, 1, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60*20, 2, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60*20, 0, false, false, true));
            }
            case TOBA_SHROOM -> {
                // Грибы: тошнота + слабость + фейковые мобы вокруг игрока 20 сек
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20*20, 1, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20*20, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20*20, 1, false, false, false));
                triggerHallucinations(p, 20*20L);
            }
            case TOBA_WARPED -> {
                // Искажённые: тошнота + плавное падение + 15 сек случайных коротких телепортов
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20*20, 1, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 15*20, 0, false, false, false));
                scheduleWarpedTeleports(p, 15*20L);
            }
        }
    }

    private String tobaccoName(int type) {
        return switch (type) {
            case TOBA_BURNT        -> "Burnt Leaves";
            case TOBA_CHEMICAL     -> "Chemical Apple";
            case TOBA_DOUBLE_APPLE -> "Double Apple Classic";
            case TOBA_GRAPE_MINT   -> "Grape Mint";
            case TOBA_BLUEBERRY    -> "Blueberry Soda";
            case TOBA_PEACH        -> "Premium Peach";
            case TOBA_TANGIERS     -> "Tangiers Noir";
            case TOBA_DIAMOND      -> "Diamond Haze";
            case TOBA_GODS         -> "God's Breath";
            case TOBA_SHROOM       -> "Mushroom Trip";
            case TOBA_WARPED       -> "Warped Fungi";
            default                -> "Garbage Mix";
        };
    }

    // ==================== ГАЛЛЮЦИНАЦИИ: фейковые мобы вокруг игрока ====================
    private final Map<UUID, Long> hallucinationUntil = new HashMap<>();

    private void triggerHallucinations(Player p, long durationTicks) {
        UUID uid = p.getUniqueId();
        long endTick = tickCounter + durationTicks;
        hallucinationUntil.put(uid, endTick);
        // При старте сразу спавним парочку фейков вокруг
        spawnHallucinationsFor(p, 5);
    }

    private void scheduleWarpedTeleports(Player p, long durationTicks) {
        UUID uid = p.getUniqueId();
        // Каждые 3 секунды в течение duration — короткий рэнром-телепорт (5-10 блоков)
        int hops = (int)(durationTicks / 60L);
        for (int i = 1; i <= hops; i++) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!p.isOnline() || p.isDead()) return;
                Long end = hallucinationUntil.get(uid);
                // Не телепортируем если трип уже закончился (или игрок вышел/умер)
                if (end == null || tickCounter > end) return;
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                double dist = 5 + rnd.nextDouble()*5;
                double angle = rnd.nextDouble()*2*Math.PI;
                double tx = p.getLocation().getX() + Math.cos(angle)*dist;
                double tz = p.getLocation().getZ() + Math.sin(angle)*dist;
                double ty = p.getLocation().getY();
                // Ищем безопасную точку (не в стене)
                Location to = new Location(p.getWorld(), tx, ty, tz);
                for (int dy = 0; dy < 8; dy++) {
                    Location c = new Location(p.getWorld(), tx, ty+dy, tz);
                    if (c.getBlock().isPassable() && c.clone().add(0,1,0).getBlock().isPassable()) {
                        to = c;
                        break;
                    }
                }
                p.teleport(to);
                try { p.getWorld().spawnParticle(Particle.PORTAL, to, 30, 0.5, 0.5, 0.5, 0.2); } catch (Throwable ignored) {}
                try { p.getWorld().spawnParticle(Particle.DRAGON_BREATH, to, 15, 0.3,0.3,0.3,0.05); } catch (Throwable ignored) {}
                playSoundSafe(p.getWorld(), to, 1.0f, 1.2f,
                        "ENTITY_ENDERMAN_TELEPORT", "ITEM_CHORUS_FRUIT_TELEPORT");
            }, i * 60L);
        }
        // По окончании снимаем тошноту
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!p.isOnline()) return;
            Long end = hallucinationUntil.get(uid);
            if (end == null) return;
            // Не убираем если игрок покурил ещё раз
            if (end > tickCounter + 5) return;
            hallucinationUntil.remove(uid);
        }, durationTicks + 5);
    }

    private void spawnHallucinationsFor(Player p, int count) {
        World w = p.getWorld();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Location eye = p.getEyeLocation();
        for (int i = 0; i < count; i++) {
            double angle = rnd.nextDouble()*2*Math.PI;
            double dist = 6 + rnd.nextDouble()*8;
            double ox = Math.cos(angle)*dist;
            double oz = Math.sin(angle)*dist;
            Location loc = eye.clone().add(ox, -1 + rnd.nextDouble()*2, oz);
            // Корректируем Y к земле
            Block ground = w.getHighestBlockAt(loc);
            loc.setY(ground.getY()+1);
            EntityType[] types = { EntityType.CREEPER, EntityType.PHANTOM, EntityType.ZOMBIE, EntityType.SKELETON };
            EntityType type = types[rnd.nextInt(types.length)];
            Entity fake = null;
            try { fake = w.spawnEntity(loc, type); } catch (Exception ignored) { continue; }
            // Делаем "фейковым": бессмертен, не двигается, не агрится, удаляется через несколько секунд
            if (fake instanceof LivingEntity le) {
                try { le.setAI(false); } catch (Throwable ignored) {}
                try { le.setInvulnerable(true); } catch (Throwable ignored) {}
                try { le.setSilent(true); } catch (Throwable ignored) {}
                try { le.setCollidable(false); } catch (Throwable ignored) {}
                try {
                    if (le instanceof Mob m) m.setTarget(null);
                } catch (Throwable ignored) {}
                // Не дропает лут
                try {
                    le.getPersistentDataContainer().set(keyHallucination, PersistentDataType.BYTE, (byte)1);
                } catch (Throwable ignored) {}
            }
            // Удалим через 3-5 сек
            long life = 60 + rnd.nextInt(60);
            hallucinations.put(fake.getUniqueId(), new Hallucination(fake.getUniqueId(), p.getUniqueId(), tickCounter + life));
            // Звук шипения/рыка при появлении фейка
            if (rnd.nextBoolean()) {
                try {
                    if (type == EntityType.CREEPER) w.playSound(loc, Sound.ENTITY_CREEPER_HURT, 0.6f, 1.0f);
                    else if (type == EntityType.PHANTOM) w.playSound(loc, Sound.ENTITY_PHANTOM_AMBIENT, 0.6f, 1.0f);
                    else w.playSound(loc, Sound.ENTITY_ZOMBIE_AMBIENT, 0.6f, 1.0f);
                } catch (Throwable ignored) {}
            }
        }
    }

    /** Тик галлюцинаций: обновляем фейковых мобов, удаляем истёкшие, иногда спавним новых */
    private void tickHallucinations() {
        List<UUID> toRemove = new ArrayList<>();
        // Считаем активных фейков на игрока
        Map<UUID, Integer> perPlayer = new HashMap<>();
        for (Map.Entry<UUID, Hallucination> en : hallucinations.entrySet()) {
            Hallucination h = en.getValue();
            Entity ent = null;
            try { ent = Bukkit.getEntity(h.entityUid); } catch (Throwable ignored) {}
            if (ent == null || ent.isDead() || !ent.isValid() || tickCounter > h.expireTick) {
                if (ent != null && !ent.isDead()) {
                    // Пшик!
                    try { ent.getWorld().spawnParticle(Particle.CLOUD, ent.getLocation().add(0,1,0), 10, 0.3,0.3,0.3,0.05); } catch (Throwable ignored) {}
                    try { ent.remove(); } catch (Throwable ignored) {}
                }
                toRemove.add(en.getKey());
                continue;
            }
            perPlayer.merge(h.ownerUid, 1, Integer::sum);
            // Подталкиваем фейка в сторону игрока (чтобы выглядело как "идут к тебе"), но без вреда
            Player owner = Bukkit.getPlayer(h.ownerUid);
            if (owner != null && owner.isOnline() && ent instanceof LivingEntity le) {
                Location to = owner.getLocation();
                Location from = ent.getLocation();
                if (from.distanceSquared(to) < 25*25) {
                    Vector dir = to.toVector().subtract(from.toVector()).normalize().multiply(0.15);
                    le.setVelocity(le.getVelocity().add(dir).multiply(0.6));
                }
                // Случайный звук фейка
                if (ThreadLocalRandom.current().nextInt(80) == 0) {
                    try {
                        if (ent instanceof Creeper) le.getWorld().playSound(ent.getLocation(), Sound.ENTITY_CREEPER_HURT, 0.5f, 1.0f);
                        else if (ent instanceof Phantom) le.getWorld().playSound(ent.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 0.5f, 1.0f);
                        else if (ent instanceof Zombie) le.getWorld().playSound(ent.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 0.5f, 1.0f);
                        else if (ent instanceof Skeleton) le.getWorld().playSound(ent.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 0.5f, 1.0f);
                    } catch (Throwable ignored) {}
                }
            }
        }
        for (UUID u : toRemove) hallucinations.remove(u);

        // Доспавниваем новых фейков для активных "трипующих" игроков
        for (Map.Entry<UUID, Long> en : hallucinationUntil.entrySet()) {
            UUID uid = en.getKey();
            if (tickCounter > en.getValue()) {
                hallucinationUntil.remove(uid);
                continue;
            }
            Player p = Bukkit.getPlayer(uid);
            if (p == null || !p.isOnline() || p.isDead()) continue;
            int current = perPlayer.getOrDefault(uid, 0);
            if (current < 6 && ThreadLocalRandom.current().nextInt(20) == 0) {
                spawnHallucinationsFor(p, 2);
            }
        }
    }

    // Убираем урон от фейковых мобов (они чисто косметические)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHallucinationDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof LivingEntity le
                && le.getPersistentDataContainer().has(keyHallucination, PersistentDataType.BYTE)) {
            e.setCancelled(true);
            // Фейк исчезает при попытке атаковать игрока
            try {
                le.getWorld().spawnParticle(Particle.CLOUD, le.getLocation().add(0,1,0), 10, 0.3,0.3,0.3,0.05);
            } catch (Throwable ignored) {}
            le.remove();
            hallucinations.remove(le.getUniqueId());
            return;
        }
        // Также: игрок не может нанести урон фейку (они исчезают сами) — чтобы не спамило уроном
        if (e.getEntity() instanceof LivingEntity le
                && le.getPersistentDataContainer().has(keyHallucination, PersistentDataType.BYTE)) {
            e.setCancelled(true);
            try {
                le.getWorld().spawnParticle(Particle.CLOUD, le.getLocation().add(0,1,0), 10, 0.3,0.3,0.3,0.05);
            } catch (Throwable ignored) {}
            le.remove();
            hallucinations.remove(le.getUniqueId());
        }
    }

    // При смерти/выходе сбрасываем трип
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitClearHallucinations(PlayerQuitEvent e) {
        UUID uid = e.getPlayer().getUniqueId();
        hallucinationUntil.remove(uid);
        // Удаляем всех фейков этого игрока
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, Hallucination> en : hallucinations.entrySet()) {
            if (en.getValue().ownerUid.equals(uid)) {
                try {
                    Entity ent = Bukkit.getEntity(en.getKey());
                    if (ent != null && !ent.isDead()) ent.remove();
                } catch (Throwable ignored) {}
                toRemove.add(en.getKey());
            }
        }
        for (UUID u : toRemove) hallucinations.remove(u);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeathClearHallucinations(PlayerDeathEvent e) {
        UUID uid = e.getEntity().getUniqueId();
        hallucinationUntil.remove(uid);
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, Hallucination> en : hallucinations.entrySet()) {
            if (en.getValue().ownerUid.equals(uid)) {
                try {
                    Entity ent = Bukkit.getEntity(en.getKey());
                    if (ent != null && !ent.isDead()) ent.remove();
                } catch (Throwable ignored) {}
                toRemove.add(en.getKey());
            }
        }
        for (UUID u : toRemove) hallucinations.remove(u);
    }

    private long locKey(Location l) {
        // Упаковываем world-хэш и координаты блока в один long
        // x/z/y — по ~9 бит, этого достаточно
        int x = l.getBlockX();
        int y = l.getBlockY();
        int z = l.getBlockZ();
        long wid = l.getWorld().getUID().getMostSignificantBits() & 0xFFFFL;
        long key = (wid << 48);
        key |= ((long)(x & 0xFFFF)) << 32;
        key |= ((long)(y & 0xFFFF)) << 16;
        key |= (z & 0xFFFFL);
        return key;
    }

    // ==================== РИТУАЛЬНЫЙ КОСТЁР: ЛОГИКА ====================

    /** Проверяет, является ли блок нашим ритуальным костром (есть в карте bonfires) */
    private boolean isOurBonfire(Block b) {
        if (b == null) return false;
        Material t = b.getType();
        if (t != Material.CAMPFIRE && t != Material.SOUL_CAMPFIRE) return false;
        return bonfires.containsKey(locKey(b.getLocation()));
    }

    /** Включает/выключает огонь в блоке костра (campfire blockdata lit) */
    private void setCampfireLit(Block b, boolean lit) {
        if (b == null) return;
        try {
            if (b.getBlockData() instanceof Campfire cf) {
                cf.setLit(lit);
                b.setBlockData(cf, true);
            }
        } catch (Throwable ignored) {}
    }

    /** Превратить блок костра в SOUL_CAMPFIRE (синий) или обратно в обычный */
    private void setSoulCampfire(Block b, boolean soul) {
        if (b == null) return;
        try {
            boolean wasLit = b.getBlockData() instanceof Campfire c && c.isLit();
            b.setType(soul ? Material.SOUL_CAMPFIRE : Material.CAMPFIRE, false);
            setCampfireLit(b, wasLit);
        } catch (Throwable ignored) {}
    }

    // ==================== ГОЛОГРАММА НАД КОСТРОМ ====================

    /** Создаёт или обновляет hologram над костром, показывающую остаток топлива. */
    private void updateBonfireHologram(Block b, BonfireData d) {
        if (b == null || d == null) return;
        long key = locKey(b.getLocation());
        org.bukkit.entity.ArmorStand stand = bonfireHolograms.get(key);
        // Если костёр потух окончательно (grace истёк и варден вызван) — голограмму не показываем
        boolean dead = d.deadUntil > 0L && System.currentTimeMillis() >= d.deadUntil;
        if (d.fuel <= 0 && d.deadUntil == 0L) {
            removeBonfireHologram(key);
            return;
        }
        Location loc = b.getLocation().add(0.5, 1.4, 0.5);
        String text;
        if (d.deadUntil > 0L && System.currentTimeMillis() < d.deadUntil) {
            long secLeft = (d.deadUntil - System.currentTimeMillis()) / 1000L;
            text = "§c⚠ Потух! " + secLeft + "с";
        } else if (dead) {
            removeBonfireHologram(key);
            return;
        } else {
            int pct = (int) Math.round(d.fuel * 100.0 / BONFIRE_MAX_FUEL);
            // Статус в зависимости от процента
            String color = pct > 60 ? "§a" : pct > 30 ? "§e" : "§c";
            String bar = buildBar(pct, 20);
            text = color + "🔥 " + d.fuel + "/" + BONFIRE_MAX_FUEL + " §7" + bar;
        }
        if (stand == null || stand.isDead()) {
            try {
                stand = b.getWorld().spawn(loc, org.bukkit.entity.ArmorStand.class, as -> {
                    try {
                        as.customName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(text));
                    } catch (Throwable ignored) {
                        try { as.setCustomName(text); } catch (Throwable ignored2) {}
                    }
                    as.setCustomNameVisible(true);
                    as.setGravity(false);
                    as.setVisible(false);
                    as.setMarker(true);
                    as.setSmall(true);
                    as.setInvulnerable(true);
                    as.setPersistent(false);
                });
                bonfireHolograms.put(key, stand);
            } catch (Throwable ignored) { return; }
        } else {
            stand.teleport(loc);
            try {
                // Пытаемся Adventure, не вышло — legacy
                stand.customName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(text));
            } catch (Throwable ignored) {
                try { stand.setCustomName(text); } catch (Throwable ignored2) {}
            }
        }
    }

    /** Убирает hologram над костром. */
    private void removeBonfireHologram(long key) {
        org.bukkit.entity.ArmorStand as = bonfireHolograms.remove(key);
        if (as != null && !as.isDead()) {
            try { as.remove(); } catch (Throwable ignored) {}
        }
    }

    /** Убирает все hologram-костры (вызов при выключении). */
    private void removeAllBonfireHolograms() {
        for (org.bukkit.entity.ArmorStand as : bonfireHolograms.values()) {
            try { if (as != null && !as.isDead()) as.remove(); } catch (Throwable ignored) {}
        }
        bonfireHolograms.clear();
    }

    /** Простой индикатор-прогрессбар из символов █/░. */
    private String buildBar(int pct, int width) {
        int filled = Math.max(0, Math.min(width, (int) Math.round(pct * width / 100.0)));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) sb.append(i < filled ? "█" : "░");
        return sb.toString();
    }

    /**
     * Плавный серый градиент текста (тёмно-серый → светло-серый).
     * Через hex-коды §x§R§R§G§G§B§B в legacy-формате.
     * startPrefix — код/префикс для первого символа, endSuffix — код/суффикс для последнего (жирный/курсив и т.п.).
     */
    private String gradient(String text, String startPrefix, String endSuffix) {
        if (text == null || text.isEmpty()) return "";
        // Палитра серого: тёмный #303030 → светло-серый #B0B0B0
        int[] startRGB = {0x30, 0x30, 0x30};
        int[] endRGB   = {0xB0, 0xB0, 0xB0};
        int n = text.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c == ' ') { sb.append(c); continue; }
            double t = (n == 1) ? 0.5 : (double) i / (n - 1);
            int r = (int) Math.round(startRGB[0] + (endRGB[0] - startRGB[0]) * t);
            int g = (int) Math.round(startRGB[1] + (endRGB[1] - startRGB[1]) * t);
            int b = (int) Math.round(startRGB[2] + (endRGB[2] - startRGB[2]) * t);
            if (i == 0 && startPrefix != null) sb.append(startPrefix);
            sb.append(String.format("§x§%s§%s§%s§%s§%s§%s",
                    hex(r >> 4), hex(r & 0xF),
                    hex(g >> 4), hex(g & 0xF),
                    hex(b >> 4), hex(b & 0xF)));
            sb.append(c);
        }
        if (endSuffix != null) sb.append(endSuffix);
        sb.append("§r");
        return sb.toString();
    }
    private String hex(int nibble) { return Integer.toHexString(nibble & 0xF).toUpperCase(java.util.Locale.ROOT); }

    /** Сообщение всем игрокам в заданном радиусе от локации */
    private void broadcastNearby(Location loc, double radius, String message) {
        double r2 = radius * radius;
        for (Player pl : loc.getWorld().getPlayers()) {
            if (pl.getLocation().distanceSquared(loc) <= r2) {
                pl.sendMessage(message);
            }
        }
    }

    /** Сколько единиц топлива даёт материал (0 = не топливо) */
    /** Сколько единиц топлива даёт предмет (0 = не топливо). Значения из конфига: брёвна/доски/палки. */
    private int fuelValue(Material m) {
        String name = m.name();
        // Палка
        if (m == Material.STICK) return BONFIRE_STICK_FUEL;
        // Доски: любые *_PLANKS (включая bamboo/crimson/warped)
        if (name.endsWith("_PLANKS")) return BONFIRE_PLANK_FUEL;
        // Брёвна и стволы (log, stem, wood, hyphae — любые варианты, включая обтёсанные)
        if (name.endsWith("_LOG") || name.endsWith("_STEM")
                || name.endsWith("_WOOD") || name.endsWith("_HYPHAE")) {
            return BONFIRE_LOG_FUEL;
        }
        return 0;
    }

    /** Зажечь потухший костёр (во время grace period) с добавлением топлива */
    private void relightBonfire(Block b, BonfireData data, int addedFuel) {
        long key = locKey(b.getLocation());
        data.fuel = Math.min(BONFIRE_MAX_FUEL, data.fuel + addedFuel);
        data.deadUntil = 0L;
        setSoulCampfire(b, data.soul);
        setCampfireLit(b, true);
        bonfires.put(key, data);
        Location loc = b.getLocation().add(0.5, 0.5, 0.5);
        b.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.2f);
        b.getWorld().spawnParticle(Particle.FLAME, loc, 20, 0.5, 0.4, 0.5, 0.03);
        broadcastNearby(loc, 50, ChatColor.GREEN + "🔥 §aКостёр снова разгорелся!");
        updateBonfireHologram(b, data);
    }

    /** Потушить костёр: убрать огонь, выставить grace period, заспавнить орду */
    private void extinguishBonfire(Block b, String reasonMsg) {
        long key = locKey(b.getLocation());
        BonfireData data = bonfires.get(key);
        if (data == null) return;
        if (data.deadUntil != 0L && data.deadUntil > System.currentTimeMillis()) return; // уже потушен
        data.fuel = 0;
        data.deadUntil = System.currentTimeMillis() + BONFIRE_DEAD_GRACE_SEC * 1000L;
        setCampfireLit(b, false);
        bonfires.put(key, data);
        Location loc = b.getLocation().add(0.5, 0.5, 0.5);
        b.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 2.0f, 0.8f);
        b.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 30, 0.6, 0.5, 0.6, 0.05);
        spawnMobHorde(loc);
        if (reasonMsg != null) broadcastNearby(loc, 50, reasonMsg);
        String warn = BONFIRE_SPAWN_MOBS
                ? ChatColor.RED + "\n🔥 §cПодпитайте его деревом за 2 минуты, иначе явится Варден!"
                : ChatColor.GRAY + "\n🔥 Подпитайте его деревом за 2 минуты.";
        broadcastNearby(loc, 50, gradient("Ритуальный костёр угас...", "§8§l", "§7§o") + warn);
        updateBonfireHologram(b, data);
    }

    /** Заспавнить орду нежити вокруг костра (R=40) */
    private void spawnMobHorde(Location center) {
        if (!BONFIRE_SPAWN_MOBS) return;
        World w = center.getWorld();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        // 20 зомби, 10 маленьких зомби, 30 скелетов, 15 криперов
        spawnGroup(w, center, 20, EntityType.ZOMBIE, rnd);
        spawnGroup(w, center, 10, EntityType.ZOMBIE, rnd, true);
        spawnGroup(w, center, 30, EntityType.SKELETON, rnd);
        spawnGroup(w, center, 15, EntityType.CREEPER, rnd);
        w.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 3.0f, 0.5f);
    }

    private void spawnGroup(World w, Location center, int count, EntityType type, ThreadLocalRandom rnd) {
        spawnGroup(w, center, count, type, rnd, false);
    }

    private void spawnGroup(World w, Location center, int count, EntityType type, ThreadLocalRandom rnd, boolean baby) {
        for (int i = 0; i < count; i++) {
            double angle = rnd.nextDouble() * 2 * Math.PI;
            double dist = 4 + rnd.nextDouble() * 36; // 4..40
            double ox = Math.cos(angle) * dist;
            double oz = Math.sin(angle) * dist;
            Location loc = center.clone().add(ox, 0, oz);
            // Найти твёрдую землю, не считая листву/траву/снег
            int sy = w.getHighestBlockYAt(loc);
            int cy = Math.max(w.getMinHeight(), Math.min(sy, loc.getBlockY() + 5));
            Block b = w.getBlockAt(loc.getBlockX(), cy, loc.getBlockZ());
            while (cy > w.getMinHeight() && (!b.getType().isSolid() || b.getType().isAir()
                    || b.getType() == Material.SNOW || b.getType() == Material.SHORT_GRASS
                    || b.getType() == Material.TALL_GRASS || b.getType() == Material.FERN)) {
                cy--;
                b = w.getBlockAt(loc.getBlockX(), cy, loc.getBlockZ());
            }
            loc.setY(cy + 1);
            Entity ent = w.spawnEntity(loc, type);
            if (baby && ent instanceof Ageable ag) ag.setBaby();
            // Разозлить на ближайшего игрока если возможно
            if (ent instanceof Monster mon) {
                Player nearest = null;
                double best = 60*60;
                for (Player pl : w.getPlayers()) {
                    double d = pl.getLocation().distanceSquared(loc);
                    if (d < best) { best = d; nearest = pl; }
                }
                if (nearest != null) mon.setTarget(nearest);
            }
        }
    }

    /** Спавн 2-х мобов-охранников у горящего костра (каждые bonfire-tick-period-ticks) */
    private void spawnBonfireGuards(Location center) {
        World w = center.getWorld();
        if (BONFIRE_SPAWN_MOBS) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            EntityType[] guards = { EntityType.ZOMBIE, EntityType.CREEPER, EntityType.SKELETON, EntityType.SPIDER };
            EntityType pick = guards[rnd.nextInt(guards.length)];
            // Спавним близко (3..10 блоков) — охранники у костра, а не орда на 40 блоков
            for (int i = 0; i < 2; i++) {
                double angle = rnd.nextDouble() * 2 * Math.PI;
                double dist = 3 + rnd.nextDouble() * 7;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                Location loc = center.clone().add(ox, 0, oz);
                int sy = w.getHighestBlockYAt(loc);
                int cy = Math.max(w.getMinHeight(), Math.min(sy, loc.getBlockY() + 5));
                Block b = w.getBlockAt(loc.getBlockX(), cy, loc.getBlockZ());
                while (cy > w.getMinHeight() && (!b.getType().isSolid() || b.getType().isAir()
                        || b.getType() == Material.SNOW || b.getType() == Material.SHORT_GRASS
                        || b.getType() == Material.TALL_GRASS || b.getType() == Material.FERN)) {
                    cy--;
                    b = w.getBlockAt(loc.getBlockX(), cy, loc.getBlockZ());
                }
                loc.setY(cy + 1);
                try {
                    Entity ent = w.spawnEntity(loc, pick);
                    if (ent instanceof Monster mon) {
                        Player nearest = null;
                        double best = 30 * 30;
                        for (Player pl : w.getPlayers()) {
                            double d = pl.getLocation().distanceSquared(loc);
                            if (d < best) { best = d; nearest = pl; }
                        }
                        if (nearest != null) mon.setTarget(nearest);
                    }
                } catch (Throwable ignored) {}
            }
        }
        // Зловещий дымок из костра при "спавне" (всегда, даже если мобы отключены)
        try { w.spawnParticle(Particle.LARGE_SMOKE, center, 6, 0.4, 0.3, 0.4, 0.02); } catch (Throwable ignored) {}
    }

    /** Заспавнить Вардена с рыком и эффектами */
    private void spawnWarden(Location loc, Player target) {
        World w = loc.getWorld();
        Location spawn = loc.clone().add(0, 1, 0);
        Warden warden = null;
        if (BONFIRE_SPAWN_MOBS) {
            warden = (Warden) w.spawnEntity(spawn, EntityType.WARDEN);
        }
        try { w.spawnParticle(Particle.SONIC_BOOM, spawn.clone().add(0, 1, 0), 5, 0.4, 0.5, 0.4, 0.1); } catch (Throwable ignored) {}
        playSoundSafe(w, loc, 3.0f, 1.0f, "ENTITY_WARDEN_EMERGE");
        playSoundSafe(w, loc, 3.0f, 0.8f, "ENTITY_WARDEN_ROAR");
        playSoundSafe(w, loc, 2.0f, 1.0f, "ENTITY_WARDEN_ANGRY", "ENTITY_GENERIC_EXPLODE");
        try { w.spawnParticle(Particle.POOF, spawn.clone().add(0,1,0), 30, 0.6, 0.5, 0.6, 0.1); } catch (Throwable ignored) {}
        if (warden != null && target != null && target.isOnline() && target.getWorld().equals(w)) {
            warden.setTarget(target);
        }
    }

    // ---- Установка костра из предмета ----
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBonfirePlace(BlockPlaceEvent e) {
        ItemStack hand = e.getItemInHand();
        if (!isBonfireItem(hand)) return;
        Block b = e.getBlockPlaced();
        // Ставится как обычный CAMPFIRE уже ванильно — нам нужно только зарегистрировать и зажечь
        setSoulCampfire(b, false);
        setCampfireLit(b, true);
        BonfireData data = new BonfireData();
        data.fuel = BONFIRE_MAX_FUEL;
        data.breakCount = 0;
        data.soul = false;
        data.deadUntil = 0L;
        long bfKey = locKey(b.getLocation());
        bonfires.put(bfKey, data);
        Location loc = b.getLocation().add(0.5, 0.5, 0.5);
        b.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
        b.getWorld().spawnParticle(Particle.FLAME, loc, 15, 0.4, 0.3, 0.4, 0.02);
        updateBonfireHologram(b, data);
        e.getPlayer().sendMessage(ChatColor.GOLD + "🔥 Ритуальный костёр установлен. Топливо: 100/100");
    }

    // ---- ПКМ по костру: подпитка или проверка топлива ----
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBonfireInteract(PlayerInteractEvent e) {
        if (!e.getAction().name().startsWith("RIGHT_CLICK")) return;
        // Обрабатываем только главную руку, чтобы не было двойных срабатываний
        if (e.getHand() != EquipmentSlot.HAND) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        long key = locKey(b.getLocation());
        if (!bonfires.containsKey(key)) return;
        // Это наш костёр — отменяем ванильное поведение
        e.setCancelled(true);
        Player p = e.getPlayer();
        BonfireData data = bonfires.get(key);
        ItemStack hand = p.getInventory().getItemInMainHand();
        Material hm = hand.getType();

        // Попытка подпитать
        int fv = fuelValue(hm);
        if (fv > 0) {
            // Если потух (grace period) — поджигаем заново; иначе добавляем топлива
            if (data.deadUntil != 0L && data.deadUntil > System.currentTimeMillis()) {
                // Возрождение из мёртвого
                relightBonfire(b, data, fv);
                p.sendMessage(ChatColor.GREEN + "🔥 Вы подожгли костёр заново! Топливо: " + data.fuel + "/100");
                if (p.getGameMode() != GameMode.CREATIVE) consumeHand(p);
                return;
            }
            // Обычное добавление топлива
            if (data.fuel >= BONFIRE_MAX_FUEL) {
                p.sendMessage(ChatColor.GRAY + "Костёр и так полон дров (" + data.fuel + "/100).");
                return;
            }
            data.fuel = Math.min(BONFIRE_MAX_FUEL, data.fuel + fv);
            if (p.getGameMode() != GameMode.CREATIVE) consumeHand(p);
            Location loc = b.getLocation().add(0.5, 0.8, 0.5);
            b.getWorld().spawnParticle(Particle.FLAME, loc, 6, 0.3, 0.2, 0.3, 0.02);
            b.getWorld().playSound(b.getLocation(), Sound.ENTITY_GENERIC_BURN, 0.5f, 1.0f);
            p.sendActionBar(ChatColor.GOLD + "🔥 Топливо: " + data.fuel + "/" + BONFIRE_MAX_FUEL);
            bonfires.put(key, data);
            updateBonfireHologram(b, data);
            return;
        }

        // Без дров — просто показываем статус
        boolean lit = data.deadUntil == 0L || data.deadUntil <= System.currentTimeMillis();
        if (data.deadUntil > System.currentTimeMillis()) {
            long secLeft = (data.deadUntil - System.currentTimeMillis()) / 1000L;
            p.sendMessage(ChatColor.RED + "🔥 §cКостёр потух! Осталось " + secLeft + " сек, чтобы подпитать, иначе явится Варден!");
        } else {
            String soul = data.soul ? " §5(синий)" : "";
            p.sendMessage(ChatColor.GOLD + "🔥 Ритуальный костёр" + soul + " §f— топливо: " + data.fuel + "/" + BONFIRE_MAX_FUEL);
        }
    }

    // ---- Ломание костра ----
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBonfireBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        long key = locKey(b.getLocation());
        if (!bonfires.containsKey(key)) return;
        Player p = e.getPlayer();

        // Креатив: убрать костёр полностью
        if (p.getGameMode() == GameMode.CREATIVE) {
            bonfires.remove(key);
            removeBonfireHologram(key);
            return;
        }

        // Выживание: ломание ВСЕГДА только гасит костёр (блок не исчезает!)
        e.setCancelled(true);
        BonfireData data = bonfires.get(key);
        // Если уже потух — не вызываем повторное угасание
        if (data.deadUntil != 0L && data.deadUntil > System.currentTimeMillis()) return;
        Location loc = b.getLocation().add(0.5, 0.5, 0.5);
        b.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 0.9f);
        b.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 12, 0.4, 0.3, 0.4, 0.03);
        p.sendMessage(gradient("Огонь потух от ваших ударов...", "§8§l", "§7"));
        extinguishBonfire(b, gradient("Кто-то загасил ритуальный костёр!", "§8", "§7"));
    }

    // ---- Взрывы: не дают разрушить костёр, но тушат его ----
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBonfireExplodeBlock(BlockExplodeEvent e) {
        List<Block> keep = new ArrayList<>();
        boolean anyBonfire = false;
        for (Block bl : e.blockList()) {
            if (isOurBonfire(bl)) {
                anyBonfire = true;
                keep.add(bl);
            }
        }
        if (anyBonfire) {
            e.blockList().removeAll(keep);
            for (Block bl : keep) {
                // Потушить каждый затронутый костёр
                extinguishBonfire(bl, null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBonfireExplodeEntity(EntityExplodeEvent e) {
        // Для EntityExplodeEvent у нас нет списка блоков в том же формате,
        // но Paper даёт blockList — так же чистим наши костры из него
        List<Block> keep = new ArrayList<>();
        boolean anyBonfire = false;
        for (Block bl : e.blockList()) {
            if (isOurBonfire(bl)) {
                anyBonfire = true;
                keep.add(bl);
            }
        }
        if (anyBonfire) {
            e.blockList().removeAll(keep);
            for (Block bl : keep) {
                extinguishBonfire(bl, null);
            }
        }
    }

    /** Тик костров (каждые bonfire-tick-period-ticks): трата топлива, grace period, частицы, спавн охранников */
    private void tickBonfires() {
        if (bonfires.isEmpty()) return;
        long now = System.currentTimeMillis();
        List<Long> toRemove = new ArrayList<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (Map.Entry<Long, BonfireData> en : bonfires.entrySet()) {
            Long key = en.getKey();
            BonfireData d = en.getValue();
            Block b = keyToBlock(key);
            if (b == null) { toRemove.add(key); continue; }

            Material t = b.getType();
            boolean valid = (d.soul && t == Material.SOUL_CAMPFIRE) || (!d.soul && t == Material.CAMPFIRE);
            if (!valid) {
                // Блок сменился (не нами) — убираем из карты
                toRemove.add(key);
                continue;
            }

            Location loc = b.getLocation().add(0.5, 0.7, 0.5);
            boolean lit = b.getBlockData() instanceof Campfire c && c.isLit();

            // Если сейчас в grace period (потух)
            if (d.deadUntil > 0L) {
                if (now >= d.deadUntil) {
                    // Grace истёк — приходит Варден (или просто костёр загорается, если мобы отключены)
                    if (BONFIRE_SPAWN_MOBS) {
                        broadcastNearby(loc, 50, ChatColor.DARK_RED + "💀 §4Древнее проклятие настигло вас — Варден явился!");
                    } else {
                        broadcastNearby(loc, 50, ChatColor.GRAY + "🔥 Время на перезаправку вышло. Костёр загорелся снова.");
                    }
                    spawnWarden(loc, null);
                    d.deadUntil = 0L;
                    d.fuel = BONFIRE_WARDEN_FUEL;
                    setCampfireLit(b, true);
                    bonfires.put(key, d);
                    b.getWorld().spawnParticle(Particle.FLAME, loc, 30, 0.5, 0.5, 0.5, 0.03);
                    // Обновляем голограмму
                    updateBonfireHologram(b, d);
                } else {
                    // Частицы дыма потухшего костра + звук
                    if (rnd.nextInt(3) == 0) {
                        b.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 2, 0.3, 0.2, 0.3, 0.02);
                    }
                    if (lit) setCampfireLit(b, false);
                    // Обновляем таймер на голограмме (⚠ Потух! Xс)
                    updateBonfireHologram(b, d);
                }
                continue;
            }

            // Горит — тратим топливо
            d.fuel -= BONFIRE_FUEL_PER_TICK;
            if (d.fuel <= 0) {
                d.fuel = 0;
                bonfires.put(key, d);
                extinguishBonfire(b, null);
                continue;
            }

            // Частицы огня
            Particle flameParticle = d.soul ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
            if (rnd.nextInt(2) == 0) {
                b.getWorld().spawnParticle(flameParticle, loc, 1, 0.15, 0.1, 0.15, 0.015);
            }
            if (d.soul && rnd.nextInt(4) == 0) {
                try { b.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(rnd.nextDouble()*0.4-0.2, 0.5 + rnd.nextDouble()*0.5, rnd.nextDouble()*0.4-0.2), 1, 0, 0, 0, 0); }
                catch (Throwable ignored) {}
            }

            // Каждый период тика — спавним 2-х мобов рядом с горящим костром
            spawnBonfireGuards(loc);

            bonfires.put(key, d);
            // Обновляем полоску топлива на голограмме
            updateBonfireHologram(b, d);
        }

        for (Long k : toRemove) {
            bonfires.remove(k);
            removeBonfireHologram(k);
        }
    }

    /** Обратное преобразование locKey → блок (x/y/z/world) — ищем по всем загруженным мирам */
    private Block keyToBlock(long key) {
        // Наш locKey: worldUuidMsb (16bit) << 48 | x(16)<<32 | y(16)<<16 | z(16)
        long widPart = (key >> 48) & 0xFFFFL;
        int x = (int)((key >> 32) & 0xFFFF);
        int y = (int)((key >> 16) & 0xFFFF);
        int z = (int)(key & 0xFFFF);
        // sign-extend 16-bit signed ints
        if ((x & 0x8000) != 0) x |= 0xFFFF0000;
        if ((y & 0x8000) != 0) y |= 0xFFFF0000;
        if ((z & 0x8000) != 0) z |= 0xFFFF0000;
        for (World w : Bukkit.getWorlds()) {
            if ((w.getUID().getMostSignificantBits() & 0xFFFFL) == widPart) {
                return w.getBlockAt(x, y, z);
            }
        }
        return null;
    }


    // ==================== СИСТЕМА ТЕРРИТОРИЙ ====================

    /** Вспомогательный: парсит цветовые коды § и hex §x§R§R§G§G§B§B в Component (поддержка градиентов через §x). */
    private Component colorize(String s) {
        if (s == null) return Component.empty();
        try {
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(s);
        } catch (Throwable t) {
            return net.kyori.adventure.text.Component.text(ChatColor.stripColor(s));
        }
    }

    /** Упаковка локации в long-ключ (как для костра) */
    private long locKey(World w, int x, int y, int z) {
        long wid = w.getUID().getMostSignificantBits() & 0xFFFFL;
        long k = (wid << 48);
        k |= ((long)(x & 0xFFFF)) << 32;
        k |= ((long)(y & 0xFFFF)) << 16;
        k |= (z & 0xFFFFL);
        return k;
    }

    /** Тик территорий (каждую секунду): actionbar + уведомление о входе */
    private void tickRegions() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            RegionData r = regionAt(p.getLocation());
            String key = r == null ? null : r.name;
            UUID uid = p.getUniqueId();
            String prev = lastEnteredRegion.get(uid);
            if (r == null) {
                if (prev != null) {
                    lastEnteredRegion.remove(uid);
                    p.sendActionBar(Component.empty());
                }
                continue;
            }
            // ActionBar всегда с именем региона
            p.sendActionBar(colorize(r.title));
            // При входе показываем правила в чат
            if (!r.name.equals(prev)) {
                lastEnteredRegion.put(uid, r.name);
                // Приветственный звук при входе в регион
                if (r.enterSound != null) {
                    if (r.enterSound.isEmpty()) {
                        // "none" — без звука
                    } else {
                        playSoundSafe(p.getWorld(), p.getLocation(),
                                Math.max(0.1f, r.soundVol), Math.max(0.1f, r.soundPitch),
                                r.enterSound, "BLOCK_BEACON_ACTIVATE", "ENTITY_EXPERIENCE_ORB_PICKUP");
                    }
                } else {
                    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.2f);
                }
                p.sendMessage(net.kyori.adventure.text.Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
                p.sendMessage(colorize(r.title));
                if (r.rules != null && !r.rules.isEmpty()) {
                    for (String line : r.rules) {
                        p.sendMessage(colorize(line));
                    }
                }
                p.sendMessage(net.kyori.adventure.text.Component.text("Владелец: ", NamedTextColor.GRAY)
                        .append(Component.text(r.ownerName, NamedTextColor.WHITE)));
                p.sendMessage(net.kyori.adventure.text.Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
            }
        }
    }

    /** Найти регион в точке локации */
    private RegionData regionAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        long wm = loc.getWorld().getUID().getMostSignificantBits() & 0xFFFFL;
        int px = loc.getBlockX(), pz = loc.getBlockZ();
        // Приоритет: самый маленький по площади — чтобы внутри большого региона можно было создать маленький и он был виден
        RegionData best = null;
        long bestArea = Long.MAX_VALUE;
        for (RegionData r : regions.values()) {
            if (r.worldMsb != wm) continue;
            if (px < r.minX || px > r.maxX || pz < r.minZ || pz > r.maxZ) continue;
            long area = (long)(r.maxX - r.minX + 1) * (long)(r.maxZ - r.minZ + 1);
            if (area < bestArea) { bestArea = area; best = r; }
        }
        return best;
    }

    private boolean isAdmin(Player p) { return p.hasPermission("tactic.region.admin") || p.isOp(); }
    private boolean canManage(Player p, RegionData r) {
        if (isAdmin(p)) return true;
        UUID u = p.getUniqueId();
        if (r.owner.equals(u)) return true;
        return r.coowners.contains(u);
    }

    private String playername(UUID uid) {
        Player pl = Bukkit.getPlayer(uid);
        if (pl != null) return pl.getName();
        // Оффлайн — ищем в оффлайне
        try {
            var off = Bukkit.getOfflinePlayer(uid);
            if (off.getName() != null) return off.getName();
        } catch (Throwable ignored) {}
        return uid.toString().substring(0, 8);
    }
    private UUID resolvePlayer(String name) {
        Player pl = Bukkit.getPlayerExact(name);
        if (pl != null) return pl.getUniqueId();
        try {
            var off = Bukkit.getOfflinePlayer(name);
            if (off != null && off.hasPlayedBefore()) return off.getUniqueId();
        } catch (Throwable ignored) {}
        return null;
    }

    /** ЛКМ/ПКМ жезлом по блоку — выделение региона */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegionInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isRegionTool(hand)) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        e.setCancelled(true);
        UUID uid = p.getUniqueId();
        long[] sel = regionSelections.computeIfAbsent(uid, u -> new long[] {0,0,0,0,0});
        int bx = b.getX(), bz = b.getZ();
        long wm = b.getWorld().getUID().getMostSignificantBits() & 0xFFFFL;
        if (e.getAction().name().startsWith("LEFT_CLICK")) {
            sel[0] = bx; sel[1] = bz; sel[4] = wm;
            p.sendMessage(ChatColor.AQUA + "🗺 Точка 1: §fx=" + bx + " z=" + bz);
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.7f, 1.2f);
        } else if (e.getAction().name().startsWith("RIGHT_CLICK")) {
            sel[2] = bx; sel[3] = bz; sel[4] = wm;
            p.sendMessage(ChatColor.AQUA + "🗺 Точка 2: §fx=" + bx + " z=" + bz);
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.7f, 0.8f);
        }
    }

    /** Сохранить/загрузить регионы в config.yml */
    private void saveRegionsToConfig() {
        for (Map.Entry<String, RegionData> en : regions.entrySet()) {
            String base = "regions." + en.getKey();
            RegionData r = en.getValue();
            getConfig().set(base + ".owner", r.owner.toString());
            getConfig().set(base + ".owner-name", r.ownerName);
            getConfig().set(base + ".world-msb", r.worldMsb);
            getConfig().set(base + ".min-x", r.minX);
            getConfig().set(base + ".min-z", r.minZ);
            getConfig().set(base + ".max-x", r.maxX);
            getConfig().set(base + ".max-z", r.maxZ);
            getConfig().set(base + ".title", r.titleRaw);
            getConfig().set(base + ".rules", new ArrayList<>(r.rulesRaw));
            getConfig().set(base + ".enter-sound", r.enterSound);
            getConfig().set(base + ".sound-volume", (double) r.soundVol);
            getConfig().set(base + ".sound-pitch", (double) r.soundPitch);
            List<String> co = new ArrayList<>();
            for (UUID u : r.coowners) co.add(u.toString());
            getConfig().set(base + ".coowners", co);
            List<String> mb = new ArrayList<>();
            for (UUID u : r.members) mb.add(u.toString());
            getConfig().set(base + ".members", mb);
        }
        saveConfig();
    }
    private void loadRegionsFromConfig() {
        regions.clear();
        var sec = getConfig().getConfigurationSection("regions");
        if (sec == null) return;
        for (String name : sec.getKeys(false)) {
            try {
                String base = "regions." + name;
                String ownerStr = getConfig().getString(base + ".owner");
                if (ownerStr == null) continue;
                RegionData r = new RegionData();
                r.name = name.toLowerCase(java.util.Locale.ROOT);
                r.owner = UUID.fromString(ownerStr);
                r.ownerName = getConfig().getString(base + ".owner-name", playername(r.owner));
                r.worldMsb = getConfig().getLong(base + ".world-msb", 0L);
                r.minX = getConfig().getInt(base + ".min-x");
                r.minZ = getConfig().getInt(base + ".min-z");
                r.maxX = getConfig().getInt(base + ".max-x");
                r.maxZ = getConfig().getInt(base + ".max-z");
                r.titleRaw = getConfig().getString(base + ".title", "§7Регион §f" + name);
                r.title = r.titleRaw;
                r.rulesRaw = getConfig().getStringList(base + ".rules");
                r.rules = new ArrayList<>(r.rulesRaw);
                r.enterSound = getConfig().getString(base + ".enter-sound", null);
                r.soundVol = (float) getConfig().getDouble(base + ".sound-volume", 1.0);
                r.soundPitch = (float) getConfig().getDouble(base + ".sound-pitch", 1.0);
                var co = getConfig().getStringList(base + ".coowners");
                for (String s : co) try { r.coowners.add(UUID.fromString(s)); } catch (Exception ignored) {}
                var mb = getConfig().getStringList(base + ".members");
                for (String s : mb) try { r.members.add(UUID.fromString(s)); } catch (Exception ignored) {}
                regions.put(r.name, r);
            } catch (Exception ignored) {}
        }
    }

    private String joinList(List<UUID> list) {
        if (list.isEmpty()) return "§7(пусто)";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (UUID u : list) {
            if (!first) sb.append("§7, ");
            sb.append("§f").append(playername(u));
            first = false;
        }
        return sb.toString();
    }

    private void sendRegionInfo(CommandSender s, RegionData r) {
        s.sendMessage(ChatColor.DARK_AQUA + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        s.sendMessage(ChatColor.DARK_AQUA + "🗺 ");
        s.sendMessage(colorize(r.title));
        s.sendMessage(ChatColor.GRAY + "Имя: §f" + r.name);
        s.sendMessage(ChatColor.GRAY + "Владелец: §f" + r.ownerName);
        s.sendMessage(ChatColor.GRAY + "Совладельцы: " + joinList(r.coowners));
        s.sendMessage(ChatColor.GRAY + "Участники: " + joinList(r.members));
        s.sendMessage(ChatColor.GRAY + "Координаты: §fx=" + r.minX + ".." + r.maxX + " z=" + r.minZ + ".." + r.maxZ);
        s.sendMessage(ChatColor.GRAY + "Звук при входе: §f"
                + (r.enterSound == null ? "стандартный" : r.enterSound.isEmpty() ? "выключен"
                : r.enterSound + " (vol " + String.format(java.util.Locale.US,"%.1f",r.soundVol) + ", pitch " + String.format(java.util.Locale.US,"%.1f",r.soundPitch) + ")"));
        if (!r.rules.isEmpty()) {
            s.sendMessage(ChatColor.GRAY + "Правила:");
            for (String line : r.rules) s.sendMessage(colorize(line));
        }
        s.sendMessage(ChatColor.DARK_AQUA + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private boolean regionCmd(CommandSender sender, String[] args) {
        Player p = sender instanceof Player ? (Player) sender : null;
        // Без подкоманд: если игрок стоит в регионе — показать инфо
        if (args.length < 2) {
            if (p == null) { sender.sendMessage(ChatColor.RED + "Из консоли: /region info <название>"); return true; }
            RegionData here = regionAt(p.getLocation());
            if (here == null) { p.sendMessage(ChatColor.GRAY + "Вы не в каком-либо регионе."); return true; }
            sendRegionInfo(p, here);
            return true;
        }
        String sub = args[1].toLowerCase(java.util.Locale.ROOT);
        // ===== /region create <name> [title] =====
        if (sub.equals("create") || sub.equals("создать")) {
            if (p == null) { sender.sendMessage(ChatColor.RED + "Только игрокам."); return true; }
            if (args.length < 3) { p.sendMessage(ChatColor.RED + "Использование: /region create <название> [заголовок]"); return true; }
            String name = args[2].toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-zа-я0-9_\\-]", "_");
            if (name.isEmpty()) { p.sendMessage(ChatColor.RED + "Некорректное имя."); return true; }
            if (regions.containsKey(name)) { p.sendMessage(ChatColor.RED + "Регион с таким именем уже есть."); return true; }
            long[] sel = regionSelections.get(p.getUniqueId());
            if (sel == null || sel[4] == 0L) { p.sendMessage(ChatColor.RED + "Выделите регион жезлом: ЛКМ — точка 1, ПКМ — точка 2."); return true; }
            int x1 = (int) sel[0], z1 = (int) sel[1], x2 = (int) sel[2], z2 = (int) sel[3];
            long wm = sel[4];
            String title = args.length >= 4 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)) : "§7Регион §f" + name;
            RegionData r = new RegionData();
            r.name = name;
            r.owner = p.getUniqueId();
            r.ownerName = p.getName();
            r.worldMsb = wm;
            r.minX = Math.min(x1, x2); r.maxX = Math.max(x1, x2);
            r.minZ = Math.min(z1, z2); r.maxZ = Math.max(z1, z2);
            r.titleRaw = title;
            r.title = title;
            r.rulesRaw = new ArrayList<>();
            r.rules = new ArrayList<>();
            regions.put(name, r);
            saveRegionsToConfig();
            p.sendMessage(ChatColor.GREEN + "🗺 Регион §f" + name + " §aсоздан! Используйте /region rules add <текст> чтобы добавить правила.");
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.0f);
            return true;
        }
        // ===== /region delete <name> =====
        if (sub.equals("delete") || sub.equals("remove") || sub.equals("удалить")) {
            if (args.length < 3) { sender.sendMessage(ChatColor.RED + "Использование: /region delete <название>"); return true; }
            String name = args[2].toLowerCase(java.util.Locale.ROOT);
            RegionData r = regions.get(name);
            if (r == null) { sender.sendMessage(ChatColor.RED + "Регион не найден."); return true; }
            if (p != null && !canManage(p, r)) { sender.sendMessage(ChatColor.RED + "Нет прав управлять этим регионом."); return true; }
            regions.remove(name);
            getConfig().set("regions." + name, null);
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "🗺 Регион §f" + name + " §aудалён.");
            return true;
        }
        // ===== /region list =====
        if (sub.equals("list") || sub.equals("список")) {
            if (regions.isEmpty()) { sender.sendMessage(ChatColor.GRAY + "Регионов пока нет."); return true; }
            sender.sendMessage(ChatColor.DARK_AQUA + "🗺 Список регионов (" + regions.size() + "):");
            for (RegionData r : regions.values()) {
                sender.sendMessage(ChatColor.GRAY + " • §f" + r.name + " §7— " + r.ownerName);
            }
            return true;
        }
        String name = args.length >= 3 ? args[2].toLowerCase(java.util.Locale.ROOT) : (p != null ? (regionAt(p.getLocation()) == null ? null : regionAt(p.getLocation()).name) : null);
        if (name == null) { sender.sendMessage(ChatColor.RED + "Укажите название региона."); return true; }
        RegionData r = regions.get(name);
        if (r == null) { sender.sendMessage(ChatColor.RED + "Регион §f" + name + " §cне найден."); return true; }

        // ===== /region info <name> =====
        if (sub.equals("info") || sub.equals("инфо")) {
            sendRegionInfo(sender, r);
            return true;
        }

        // Все следующие команды требуют прав управления
        if (p != null && !canManage(p, r)) { sender.sendMessage(ChatColor.RED + "Нет прав управлять этим регионом."); return true; }

        // ===== /region title <name> <текст> =====
        if (sub.equals("title") || sub.equals("название") || sub.equals("заголовок")) {
            if (args.length < 4) { sender.sendMessage(ChatColor.RED + "Использование: /region title <имя> <текст (с цветами § и §x)>"); return true; }
            String text = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
            r.titleRaw = text;
            r.title = text;
            saveRegionsToConfig();
            sender.sendMessage(ChatColor.GREEN + "🗺 Заголовок региона обновлён:");
            sender.sendMessage(colorize(text));
            return true;
        }
        // ===== /region rules add <name> <линия> =====
        if (sub.equals("rules") || sub.equals("правила")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.GRAY + "Правила региона §f" + r.name + "§7:");
                if (r.rules.isEmpty()) sender.sendMessage(ChatColor.GRAY + "  (нет правил)");
                else for (String line : r.rules) sender.sendMessage(colorize(line));
                sender.sendMessage(ChatColor.YELLOW + "/region rules add <name> <текст> — добавить");
                sender.sendMessage(ChatColor.YELLOW + "/region rules clear <name> — очистить");
                sender.sendMessage(ChatColor.YELLOW + "/region rules set <name> <№> <текст> — изменить");
                return true;
            }
            String act = args[3].toLowerCase(java.util.Locale.ROOT);
            if (act.equals("add") || act.equals("добавить")) {
                if (args.length < 5) { sender.sendMessage(ChatColor.RED + "Введите текст правила."); return true; }
                String line = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
                r.rulesRaw.add(line);
                r.rules.add(line);
                saveRegionsToConfig();
                sender.sendMessage(ChatColor.GREEN + "Добавлено правило:");
                sender.sendMessage(colorize(line));
                return true;
            }
            if (act.equals("clear") || act.equals("очистить")) {
                r.rulesRaw.clear();
                r.rules.clear();
                saveRegionsToConfig();
                sender.sendMessage(ChatColor.GREEN + "Правила очищены.");
                return true;
            }
            if (act.equals("set") || act.equals("изменить")) {
                if (args.length < 6) { sender.sendMessage(ChatColor.RED + "/region rules set <name> <№> <текст>"); return true; }
                int idx;
                try { idx = Integer.parseInt(args[4]) - 1; } catch (Exception e) { sender.sendMessage(ChatColor.RED + "Номер должен быть числом (1-based)."); return true; }
                if (idx < 0 || idx >= r.rules.size()) { sender.sendMessage(ChatColor.RED + "Такого номера нет."); return true; }
                String line = String.join(" ", java.util.Arrays.copyOfRange(args, 5, args.length));
                r.rulesRaw.set(idx, line);
                r.rules.set(idx, line);
                saveRegionsToConfig();
                sender.sendMessage(ChatColor.GREEN + "Правило #" + (idx+1) + " обновлено.");
                return true;
            }
            if (act.equals("remove") || act.equals("del") || act.equals("удалить")) {
                if (args.length < 5) { sender.sendMessage(ChatColor.RED + "/region rules remove <name> <№>"); return true; }
                int idx;
                try { idx = Integer.parseInt(args[4]) - 1; } catch (Exception e) { sender.sendMessage(ChatColor.RED + "Номер должен быть числом."); return true; }
                if (idx < 0 || idx >= r.rules.size()) { sender.sendMessage(ChatColor.RED + "Такого номера нет."); return true; }
                r.rulesRaw.remove(idx);
                r.rules.remove(idx);
                saveRegionsToConfig();
                sender.sendMessage(ChatColor.GREEN + "Правило удалено.");
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Неизвестное действие: " + act + ". add/set/remove/clear.");
            return true;
        }
        // ===== /region coowner add/remove/list =====
        if (sub.equals("coowner") || sub.equals("coowners") || sub.equals("совладелец")) {
            if (args.length < 4 || args[3].equalsIgnoreCase("list")) {
                sender.sendMessage(ChatColor.GRAY + "Совладельцы: " + joinList(r.coowners));
                return true;
            }
            String act = args[3].toLowerCase(java.util.Locale.ROOT);
            if (act.equals("add") || act.equals("добавить")) {
                if (args.length < 5) { sender.sendMessage(ChatColor.RED + "/region coowner add <название> <игрок>"); return true; }
                UUID tgt = resolvePlayer(args[4]);
                if (tgt == null) { sender.sendMessage(ChatColor.RED + "Игрок не найден."); return true; }
                if (r.coowners.contains(tgt)) { sender.sendMessage(ChatColor.RED + "Этот игрок уже совладелец."); return true; }
                if (r.owner.equals(tgt)) { sender.sendMessage(ChatColor.RED + "Это владелец региона."); return true; }
                r.coowners.add(tgt);
                saveRegionsToConfig();
                sender.sendMessage(ChatColor.GREEN + "§f" + playername(tgt) + " §aдобавлен как совладелец.");
                return true;
            }
            if (act.equals("remove") || act.equals("del") || act.equals("удалить")) {
                if (args.length < 5) { sender.sendMessage(ChatColor.RED + "/region coowner remove <название> <игрок>"); return true; }
                UUID tgt = resolvePlayer(args[4]);
                if (tgt == null) { sender.sendMessage(ChatColor.RED + "Игрок не найден."); return true; }
                if (!r.coowners.remove(tgt)) { sender.sendMessage(ChatColor.RED + "Этого игрока нет в совладельцах."); return true; }
                saveRegionsToConfig();
                sender.sendMessage(ChatColor.GREEN + "Совладелец удалён.");
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Доступно: add/remove/list");
            return true;
        }
        // ===== /region member add/remove/list =====
        if (sub.equals("member") || sub.equals("members") || sub.equals("участник")) {
            if (args.length < 4 || args[3].equalsIgnoreCase("list")) {
                sender.sendMessage(ChatColor.GRAY + "Участники: " + joinList(r.members));
                return true;
            }
            String act = args[3].toLowerCase(java.util.Locale.ROOT);
            if (act.equals("add") || act.equals("добавить")) {
                if (args.length < 5) { sender.sendMessage(ChatColor.RED + "/region member add <название> <игрок>"); return true; }
                UUID tgt = resolvePlayer(args[4]);
                if (tgt == null) { sender.sendMessage(ChatColor.RED + "Игрок не найден."); return true; }
                if (r.members.contains(tgt)) { sender.sendMessage(ChatColor.RED + "Этот игрок уже участник."); return true; }
                if (r.coowners.contains(tgt) || r.owner.equals(tgt)) { sender.sendMessage(ChatColor.RED + "Это владелец/совладелец."); return true; }
                r.members.add(tgt);
                saveRegionsToConfig();
                sender.sendMessage(ChatColor.GREEN + "§f" + playername(tgt) + " §aдобавлен как участник.");
                return true;
            }
            if (act.equals("remove") || act.equals("del") || act.equals("удалить")) {
                if (args.length < 5) { sender.sendMessage(ChatColor.RED + "/region member remove <название> <игрок>"); return true; }
                UUID tgt = resolvePlayer(args[4]);
                if (tgt == null) { sender.sendMessage(ChatColor.RED + "Игрок не найден."); return true; }
                if (!r.members.remove(tgt)) { sender.sendMessage(ChatColor.RED + "Этого игрока нет в участниках."); return true; }
                saveRegionsToConfig();
                sender.sendMessage(ChatColor.GREEN + "Участник удалён.");
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Доступно: add/remove/list");
            return true;
        }
        // ===== /region sound <name> [sound] [vol] [pitch] =====
        if (sub.equals("sound") || sub.equals("звук")) {
            if (args.length < 4 || args[3].equalsIgnoreCase("info") || args[3].equalsIgnoreCase("list")) {
                sender.sendMessage(ChatColor.GRAY + "Звук входа в регион §f" + r.name + "§7:");
                sender.sendMessage(ChatColor.GRAY + "  Звук: §f" + (r.enterSound == null ? "(по умолчанию — BEACON_ACTIVATE)" : r.enterSound));
                sender.sendMessage(ChatColor.GRAY + "  Громкость: §f" + String.format(java.util.Locale.US, "%.1f", r.soundVol));
                sender.sendMessage(ChatColor.GRAY + "  Тон: §f" + String.format(java.util.Locale.US, "%.1f", r.soundPitch));
                sender.sendMessage(ChatColor.YELLOW + "/region sound <имя> <sound> [vol] [pitch] — установить");
                sender.sendMessage(ChatColor.YELLOW + "/region sound <имя> default — звук по умолчанию");
                sender.sendMessage(ChatColor.YELLOW + "/region sound <имя> none — без звука");
                sender.sendMessage(ChatColor.YELLOW + "Примеры звуков: ENTITY_EXPERIENCE_ORB_PICKUP, BLOCK_BEACON_ACTIVATE, UI_TOAST_CHALLENGE_COMPLETE, ENTITY_PLAYER_LEVELUP, ENTITY_CAT_AMBIENT");
                if (p != null) {
                    p.sendMessage(ChatColor.GRAY + "Проверка звука...");
                    if (r.enterSound != null && !r.enterSound.isEmpty()) {
                        playSoundSafe(p.getWorld(), p.getLocation(), Math.max(0.1f, r.soundVol), Math.max(0.1f, r.soundPitch),
                                r.enterSound, "BLOCK_BEACON_ACTIVATE");
                    } else {
                        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.2f);
                    }
                }
                return true;
            }
            String snd = args[3];
            if (snd.equalsIgnoreCase("default") || snd.equalsIgnoreCase("поумолчанию")) {
                r.enterSound = null;
                r.soundVol = 1.0f;
                r.soundPitch = 1.0f;
                saveRegionsToConfig();
                sender.sendMessage(ChatColor.GREEN + "Установлен звук по умолчанию.");
                return true;
            }
            if (snd.equalsIgnoreCase("none") || snd.equalsIgnoreCase("off") || snd.equalsIgnoreCase("выкл") || snd.equalsIgnoreCase("тишина")) {
                r.enterSound = "";
                saveRegionsToConfig();
                sender.sendMessage(ChatColor.GREEN + "Звук при входе отключён.");
                return true;
            }
            // Проверяем что звук существует
            Sound test = resolveSound(snd);
            if (test == null) {
                sender.sendMessage(ChatColor.RED + "Звук §f" + snd + " §cне найден. Проверьте правильность имени (пример: ENTITY_PLAYER_LEVELUP).");
                return true;
            }
            r.enterSound = snd;
            if (args.length >= 5) {
                try { r.soundVol = Math.max(0.1f, Math.min(10.0f, Float.parseFloat(args[4]))); }
                catch (Exception e) { sender.sendMessage(ChatColor.RED + "Громкость должна быть числом (0.1..10)."); return true; }
            } else { r.soundVol = 1.0f; }
            if (args.length >= 6) {
                try { r.soundPitch = Math.max(0.1f, Math.min(2.0f, Float.parseFloat(args[5]))); }
                catch (Exception e) { sender.sendMessage(ChatColor.RED + "Тон должен быть числом (0.1..2.0)."); return true; }
            } else { r.soundPitch = 1.0f; }
            saveRegionsToConfig();
            sender.sendMessage(ChatColor.GREEN + "🗺 Звук §f" + snd + " §aустановлен (громкость " + String.format(java.util.Locale.US,"%.1f",r.soundVol) + ", тон " + String.format(java.util.Locale.US,"%.1f",r.soundPitch) + ").");
            if (p != null) playSoundSafe(p.getWorld(), p.getLocation(), r.soundVol, r.soundPitch, snd, "BLOCK_BEACON_ACTIVATE");
            return true;
        }
        // ===== /region wand =====
        if (sub.equals("wand") || sub.equals("tool") || sub.equals("жезл")) {
            if (p == null) { sender.sendMessage(ChatColor.RED + "Только игрокам."); return true; }
            p.getInventory().addItem(buildRegionTool(1)).values().forEach(lo -> p.getWorld().dropItemNaturally(p.getLocation(), lo));
            p.sendMessage(ChatColor.GREEN + "🗺 Вы получили жезл территорий.");
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Неизвестная подкоманда. create/delete/info/list/title/rules/coowner/member/sound/wand");
        return true;
    }

    /** Данные региона */
    private static class RegionData {
        String name;
        UUID owner;
        String ownerName;
        long worldMsb;
        int minX, minZ, maxX, maxZ;
        String titleRaw;    // с §-кодами и hex
        String title;       // то же самое (для удобства)
        String enterSound;  // кастомный звук при входе (null = по умолчанию)
        float soundVol = 1.0f;
        float soundPitch = 1.0f;
        List<String> rulesRaw = new ArrayList<>();
        List<String> rules = new ArrayList<>();
        Set<UUID> coowners = new HashSet<>();
        Set<UUID> members = new HashSet<>();
    }

    // ==================== КОМАНДА ====================
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Обработка /region (с алиасами rg/территория)
        String cmdName = cmd.getName().toLowerCase(java.util.Locale.ROOT);
        if (cmdName.equals("region")) {
            if (!sender.hasPermission("tactic.region.use")) { sender.sendMessage(ChatColor.RED + "У вас нет прав."); return true; }
            return regionCmd(sender, args);
        }
        if (args.length == 0) {
            if (sender instanceof Player pl) {
                if (!sender.hasPermission("tactic.menu")) { sender.sendMessage(msg("no-permission")); return true; }
                openMenu(pl);
                return true;
            }
            // Из консоли — справка
            sender.sendMessage(ChatColor.GOLD + "=== Tactic (26.2) ===");
            sender.sendMessage(ChatColor.YELLOW + "/tactic menu — открыть меню выдачи");
            sender.sendMessage(ChatColor.YELLOW + "/tactic give <предмет> [игрок] [N]");
            sender.sendMessage(ChatColor.GRAY + "   mask shears dynamite fireball smoke sticky grenade plow hookah");
            sender.sendMessage(ChatColor.GRAY + "   hbow harrow (лук-самонавод / стрелы)");
            sender.sendMessage(ChatColor.GRAY + "   leash (поводок для игроков)");
            sender.sendMessage(ChatColor.GRAY + "   stun (оглушающая граната)");
            sender.sendMessage(ChatColor.GRAY + "   bonfire/костер/bf (ритуальный костёр)");
            sender.sendMessage(ChatColor.YELLOW + "/tactic bonfire mobs <on|off> — спавн мобов от костра");
            sender.sendMessage(ChatColor.YELLOW + "/tactic unmask <игрок>");
            sender.sendMessage(ChatColor.YELLOW + "/tactic reload");
            return true;
        }
        String sub = args[0].toLowerCase();

        if (sub.equals("menu") || sub.equals("items")) {
            if (!sender.hasPermission("tactic.menu")) { sender.sendMessage(msg("no-permission")); return true; }
            if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Только игрокам. Используйте /tactic give ..."); return true; }
            openMenu(p);
            return true;
        }

        if (sub.equals("give")) {
            if (!sender.hasPermission("tactic.give")) { sender.sendMessage(msg("no-permission")); return true; }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Использование: /tactic give <предмет> [игрок] [N]");
                return true;
            }
            String type = args[1].toLowerCase();
            String regex = "mask|shears|dynamite|fireball|smoke|sticky|grenade|stun|freeze|крио|cryo|plow|hookah|hbow|harrow|leash|bonfire|костер|bf"
                    + "|dirtpack|classicpack|mentholpack|goldpack|cigarpack"
                    + "|vaccine|slobber"
                    + "|garbage|burnt|chemical|apple|grape|blueberry|peach|tangiers|diamond|gods|shroom|mushroom|грибы|warped|искаж";
            if (!type.matches(regex)) {
                sender.sendMessage(ChatColor.RED + "Неизвестный предмет. Доступные: " + regex.replace("|"," | "));
                return true;
            }
            int amount = 1; Player target = null;
            if (args.length >= 3) {
                Integer num = parseInt(args[2]);
                if (num != null) {
                    amount = Math.max(1, num);
                    if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Из консоли укажите игрока."); return true; }
                    target = (Player) sender;
                } else {
                    target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) { sender.sendMessage(msg("player-not-found").replace("%player%", args[2])); return true; }
                    if (args.length >= 4) { Integer n2 = parseInt(args[3]); if (n2 != null) amount = Math.max(1, n2); }
                }
            } else {
                if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Из консоли укажите игрока."); return true; }
                target = (Player) sender;
            }
            ItemStack item = switch (type) {
                case "mask"        -> buildMask(amount);
                case "shears"      -> buildShears(amount);
                case "dynamite"    -> buildDynamite(amount, DYN_SHORT);
                case "fireball"    -> buildFireball(amount);
                case "smoke"       -> buildSmoke(amount);
                case "sticky"      -> buildSticky(amount, STICKY_SPEED_SHORT);
                case "grenade"     -> buildClusterGrenade(amount, SMOKE_SHORT);
                case "stun"        -> buildStunGrenade(amount);
                case "freeze", "крио", "cryo" -> buildFreezeGrenade(amount);
                case "plow"        -> buildPlow(amount);
                case "hookah"      -> buildHookahItem(amount);
                case "hbow"        -> buildHomingBow(amount, HOMING_MODE_PLAYERS);
                case "harrow"      -> buildHomingArrow(amount);
                case "leash"       -> buildLeash(amount);
                // пачки сигарет
                case "dirtpack"    -> buildCigPack(amount, CIG_DIRT);
                case "classicpack" -> buildCigPack(amount, CIG_CLASSIC);
                case "mentholpack" -> buildCigPack(amount, CIG_MENTHOL);
                case "goldpack"    -> buildCigPack(amount, CIG_GOLD);
                case "cigarpack"   -> buildCigPack(amount, CIG_CIGAR);
                // табаки (дешёвые / средние / дорогие)
                case "garbage"     -> buildTobacco(amount, TOBA_GARBAGE);
                case "burnt"       -> buildTobacco(amount, TOBA_BURNT);
                case "chemical"    -> buildTobacco(amount, TOBA_CHEMICAL);
                case "apple"       -> buildTobacco(amount, TOBA_DOUBLE_APPLE);
                case "grape"       -> buildTobacco(amount, TOBA_GRAPE_MINT);
                case "blueberry"   -> buildTobacco(amount, TOBA_BLUEBERRY);
                case "peach"       -> buildTobacco(amount, TOBA_PEACH);
                case "tangiers"    -> buildTobacco(amount, TOBA_TANGIERS);
                case "diamond"     -> buildTobacco(amount, TOBA_DIAMOND);
                case "gods"        -> buildTobacco(amount, TOBA_GODS);
                case "shroom", "mushroom", "грибы" -> buildTobacco(amount, TOBA_SHROOM);
                case "warped", "искаж" -> buildTobacco(amount, TOBA_WARPED);
                case "vaccine"     -> buildVaccine(amount);
                case "slobber"     -> buildSlobber(amount);
                case "bonfire", "костер", "bf" -> buildBonfire(amount);
                case "region", "территория", "rg" -> buildRegionTool(amount);
                default            -> buildMask(1);
            };
            giveToPlayer(sender, target, item, amount, item.getItemMeta().getDisplayName());
            return true;
        }

        // === Вирус Мяуканья ===
        if (sub.equals("meowinfo")) {
            if (!sender.hasPermission("tactic.meow.admin")) { sender.sendMessage(msg("no-permission")); return true; }
            Player t;
            if (args.length >= 2) {
                t = Bukkit.getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(msg("player-not-found").replace("%player%", args[1])); return true; }
            } else {
                if (!(sender instanceof Player pl)) { sender.sendMessage(ChatColor.RED + "Укажите игрока."); return true; }
                t = pl;
            }
            int st = getMeowStage(t);
            if (st == 0) sender.sendMessage(ChatColor.GREEN + "Игрок " + t.getName() + " здоров.");
            else {
                StringBuilder bar = new StringBuilder("§7[§r");
                for (int i = 0; i < MEOW_MAX_STAGE; i++) bar.append(i < st ? "§c▮§r" : "§7▯§r");
                bar.append("§7]§r");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "🐱 " + t.getName() + " — стадия " + st + "/10 " + bar);
            }
            return true;
        }
        if (sub.equals("infect")) {
            if (!sender.hasPermission("tactic.meow.admin")) { sender.sendMessage(msg("no-permission")); return true; }
            if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Использование: /tactic infect <игрок> [стадия 1-10]"); return true; }
            Player t = Bukkit.getPlayerExact(args[1]);
            if (t == null) { sender.sendMessage(msg("player-not-found").replace("%player%", args[1])); return true; }
            int stage = 1;
            if (args.length >= 3) { Integer n = parseInt(args[2]); if (n != null) stage = Math.max(1, Math.min(MEOW_MAX_STAGE, n)); }
            int cur = getMeowStage(t);
            setMeowStage(t, stage);
            if (cur == 0) t.sendMessage(ChatColor.LIGHT_PURPLE + "🐱 Вы чувствуете странное першение в горле...");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "🐱 Игрок " + t.getName() + " заражён на стадию " + stage + "/10.");
            t.playSound(t.getLocation(), Sound.ENTITY_CAT_PURREOW, 0.8f, 0.7f);
            return true;
        }
        if (sub.equals("cure")) {
            if (!sender.hasPermission("tactic.meow.admin")) { sender.sendMessage(msg("no-permission")); return true; }
            if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Использование: /tactic cure <игрок>"); return true; }
            Player t = Bukkit.getPlayerExact(args[1]);
            if (t == null) { sender.sendMessage(msg("player-not-found").replace("%player%", args[1])); return true; }
            if (getMeowStage(t) == 0) { sender.sendMessage(ChatColor.GRAY + "Игрок и так здоров."); return true; }
            cureMeow(t, true);
            t.playSound(t.getLocation(), Sound.ITEM_BOTTLE_EMPTY, 1f, 1f);
            sender.sendMessage(ChatColor.GREEN + "💉 Игрок " + t.getName() + " излечен от мяуканья.");
            return true;
        }
        if (sub.equals("cureall")) {
            if (!sender.hasPermission("tactic.meow.admin")) { sender.sendMessage(msg("no-permission")); return true; }
            int n = meowStage.size();
            clearAllMeow();
            for (Player pl : Bukkit.getOnlinePlayers())
                pl.playSound(pl.getLocation(), Sound.ITEM_BOTTLE_EMPTY, 0.7f, 1.1f);
            sender.sendMessage(ChatColor.GREEN + "💉 Глобальная вакцинация! Излечено игроков: " + n);
            return true;
        }
        if (sub.equals("meow")) {
            if (!sender.hasPermission("tactic.meow.admin")) { sender.sendMessage(msg("no-permission")); return true; }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Вирус мяуканья сейчас: " + (meowEnabled ? "§aВКЛ" : "§cВЫКЛ"));
                sender.sendMessage(ChatColor.GRAY + "Заражено игроков: " + meowStage.size());
                return true;
            }
            String mode = args[1].toLowerCase();
            if (mode.equals("on") || mode.equals("enable") || mode.equals("true")) {
                meowEnabled = true;
                getConfig().set("meow-virus-enabled", true); saveConfig();
                sender.sendMessage(ChatColor.GREEN + "🐱 Вирус мяуканья ВКЛЮЧЁН. Заражённые остались: " + meowStage.size());
                Bukkit.broadcast(ChatColor.LIGHT_PURPLE + "🐱 Вирус мяуканья снова распространяется...", "tactic.meow.admin");
            } else if (mode.equals("off") || mode.equals("disable") || mode.equals("false")) {
                meowEnabled = false;
                getConfig().set("meow-virus-enabled", false); saveConfig();
                sender.sendMessage(ChatColor.RED + "🐱 Вирус мяуканья ВЫКЛЮЧЕН. Симптомы у заражённых пропали, но стадии сохранены.");
                Bukkit.broadcast(ChatColor.GRAY + "Вирус мяуканья временно обезврежен.", "tactic.meow.admin");
            } else {
                sender.sendMessage(ChatColor.RED + "Использование: /tactic meow <on|off>");
            }
            return true;
        }

        if (sub.equals("bonfire") || sub.equals("костер") || sub.equals("bf")) {
            if (!sender.hasPermission("tactic.reload")) { sender.sendMessage(msg("no-permission")); return true; }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.GOLD + "🔥 Ритуальный костёр:");
                sender.sendMessage(ChatColor.GRAY + "Спавн мобов: " + (BONFIRE_SPAWN_MOBS ? "§aВКЛ" : "§cВЫКЛ"));
                sender.sendMessage(ChatColor.GRAY + "Топливо за тик: §f" + BONFIRE_FUEL_PER_TICK);
                sender.sendMessage(ChatColor.GRAY + "Период тика: §f" + BONFIRE_TICK_PERIOD + " тиков (" + (BONFIRE_TICK_PERIOD/20f) + " сек)");
                sender.sendMessage(ChatColor.YELLOW + "/tactic bonfire mobs <on|off> — вкл/выкл спавн мобов");
                return true;
            }
            String opt = args[1].toLowerCase();
            if (opt.equals("mobs")) {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.GRAY + "Спавн мобов от костра: " + (BONFIRE_SPAWN_MOBS ? "§aВКЛ" : "§cВЫКЛ"));
                    sender.sendMessage(ChatColor.YELLOW + "Использование: /tactic bonfire mobs <on|off>");
                    return true;
                }
                String v = args[2].toLowerCase();
                if (v.equals("on") || v.equals("enable") || v.equals("true") || v.equals("вкл")) {
                    BONFIRE_SPAWN_MOBS = true;
                    getConfig().set("bonfire-spawn-mobs", true); saveConfig();
                    sender.sendMessage(ChatColor.GREEN + "🔥 Спавн мобов от костра ВКЛЮЧЁН (охранники, орда, Варден).");
                } else if (v.equals("off") || v.equals("disable") || v.equals("false") || v.equals("выкл")) {
                    BONFIRE_SPAWN_MOBS = false;
                    getConfig().set("bonfire-spawn-mobs", false); saveConfig();
                    sender.sendMessage(ChatColor.RED + "🔥 Спавн мобов от костра ВЫКЛЮЧЕН. Только огонь и топливо, без мобов.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Использование: /tactic bonfire mobs <on|off>");
                }
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Неизвестный параметр. Доступно: mobs");
            return true;
        }

        if (sub.equals("unmask")) {
            if (!sender.hasPermission("tactic.unmask")) { sender.sendMessage(msg("no-permission")); return true; }
            if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Использование: /tactic unmask <игрок>"); return true; }
            Player t = Bukkit.getPlayerExact(args[1]);
            if (t == null) { sender.sendMessage(msg("player-not-found").replace("%player%", args[1])); return true; }
            if (!isMasked(t)) { sender.sendMessage(msg("target-not-masked")); return true; }
            removeMask(t, true, RemoveReason.ADMIN);
            sender.sendMessage(ChatColor.GREEN + "Вы сняли маску с игрока " + maskName);
            return true;
        }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("tactic.reload")) { sender.sendMessage(msg("no-permission")); return true; }
            reloadCfg(); setupHideTeam(); sender.sendMessage(msg("reloaded")); return true;
        }

        sender.sendMessage(ChatColor.RED + "Неизвестная подкоманда. Доступно: menu, give, bonfire, unmask, meow, reload");
        return true;
    }

    private Integer parseInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return null; } }

    /**
     * Безопасно получить Sound по имени: сперва через Registry.SOUNDS (новый способ),
     * затем через Sound.valueOf как фоллбэк, если в старой версии реестр пуст.
     * Имена-константы вида "ENTITY_CAT_AMBIENT" конвертируются в "entity.cat.ambient".
     */
    private Sound resolveSound(String name) {
        if (name == null) return null;
        // 1) Пробуем через Registry.SOUNDS с ключом minecraft:... (актуальный способ в 1.21+)
        try {
            String key = name.toLowerCase(java.util.Locale.ROOT).replace('_', '.');
            Sound s = org.bukkit.Registry.SOUNDS.get(NamespacedKey.minecraft(key));
            if (s != null) return s;
        } catch (Throwable ignored) {}
        // 2) Фоллбэк через enum valueOf (deprecated for removal, но работает как запас)
        return soundValueOfFallback(name);
    }

    @SuppressWarnings({"deprecation", "removal"})
    private Sound soundValueOfFallback(String name) {
        try { return Sound.valueOf(name); } catch (IllegalArgumentException e) { return null; }
    }

    /** Безопасно проигрываем звук по имени (с фоллбэком). */
    private void playSoundSafe(World w, Location loc, float volume, float pitch, String... candidates) {
        for (String name : candidates) {
            Sound s = resolveSound(name);
            if (s != null) {
                w.playSound(loc, s, volume, pitch);
                return;
            }
        }
        w.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, volume, pitch);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String cmdName = cmd.getName().toLowerCase(java.util.Locale.ROOT);
        if (cmdName.equals("region")) {
            if (!sender.hasPermission("tactic.region.use")) return out;
            if (args.length == 1) {
                for (String s : List.of("create","delete","info","list","title","rules","coowner","member","sound","wand"))
                    if (s.startsWith(args[0].toLowerCase())) out.add(s);
            } else {
                String sub = args[0].toLowerCase();
                String regionName = args.length >= 3 ? args[2] : null;
                if (args.length == 2 && (sub.equals("delete") || sub.equals("info") || sub.equals("title")
                        || sub.equals("rules") || sub.equals("coowner") || sub.equals("member") || sub.equals("sound"))) {
                    for (RegionData r : regions.values()) {
                        if (r.name.startsWith(args[1].toLowerCase())) {
                            boolean admin = sender.hasPermission("tactic.region.admin");
                            if (admin || (sender instanceof Player p && (r.owner.equals(p.getUniqueId()) || r.coowners.contains(p.getUniqueId()))))
                                out.add(r.name);
                        }
                    }
                } else if (args.length == 3 && sub.equals("sound")) {
                    for (String s : List.of("default","none","ENTITY_EXPERIENCE_ORB_PICKUP","BLOCK_BEACON_ACTIVATE",
                            "UI_TOAST_CHALLENGE_COMPLETE","ENTITY_PLAYER_LEVELUP","ENTITY_CAT_AMBIENT",
                            "ENTITY_WOLF_AMBIENT","ENTITY_VILLAGER_AMBIENT","BLOCK_ENCHANTMENT_TABLE_USE",
                            "ITEM_TOTEM_USE","BLOCK_END_PORTAL_SPAWN","ENTITY_ENDER_DRAGON_GROWL"))
                        if (s.toLowerCase().startsWith(args[2].toLowerCase())) out.add(s);
                } else if (args.length == 3 && sub.equals("rules")) {
                    for (String s : List.of("add","set","remove","clear","list"))
                        if (s.startsWith(args[2].toLowerCase())) out.add(s);
                } else if (args.length == 3 && (sub.equals("coowner") || sub.equals("member"))) {
                    for (String s : List.of("add","remove","list"))
                        if (s.startsWith(args[2].toLowerCase())) out.add(s);
                } else if (args.length == 4 && (sub.equals("coowner") || sub.equals("member"))
                        && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
                    for (Player pl : Bukkit.getOnlinePlayers())
                        if (pl.getName().toLowerCase().startsWith(args[3].toLowerCase())) out.add(pl.getName());
                } else if (args.length == 4 && sub.equals("rules") && args[2].equalsIgnoreCase("remove")) {
                    RegionData r = regionName != null ? regions.get(regionName.toLowerCase()) : null;
                    if (r != null) for (int i = 1; i <= r.rules.size(); i++) out.add(String.valueOf(i));
                } else if (args.length == 4 && sub.equals("rules") && args[2].equalsIgnoreCase("set")) {
                    RegionData r = regionName != null ? regions.get(regionName.toLowerCase()) : null;
                    if (r != null) for (int i = 1; i <= r.rules.size(); i++) out.add(String.valueOf(i));
                }
            }
            return out;
        }
        if (args.length == 1)
            for (String sub : List.of("menu","give","unmask","infect","cure","cureall","meowinfo","meow","bonfire","костер","bf","reload"))
                if (sub.startsWith(args[0].toLowerCase())) out.add(sub);
        else if (args.length == 2 && args[0].equalsIgnoreCase("give"))
            for (String t : List.of("mask","shears","dynamite","fireball","smoke","sticky","grenade","stun","freeze","крио","plow","hookah","hbow","harrow","leash","bonfire","костер","bf",
                    "dirtpack","classicpack","mentholpack","goldpack","cigarpack",
                    "vaccine","slobber",
                    "garbage","burnt","chemical","apple","grape","blueberry","peach","tangiers","diamond","gods",
                    "shroom","mushroom","грибы","warped","искаж","region","территория","rg"))
                if (t.startsWith(args[1].toLowerCase())) out.add(t);
        else if (args.length == 2 && (args[0].equalsIgnoreCase("unmask")
                                  || args[0].equalsIgnoreCase("cure")
                                  || args[0].equalsIgnoreCase("infect")
                                  || args[0].equalsIgnoreCase("meowinfo")))
            for (Player pu : Bukkit.getOnlinePlayers())
                if (pu.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(pu.getName());
        else if (args.length == 2 && args[0].equalsIgnoreCase("meow"))
            for (String s : List.of("on","off"))
                if (s.startsWith(args[1].toLowerCase())) out.add(s);
        else if (args.length == 2 && bfCmd(args[0])) {
            for (String opt : List.of("mobs"))
                if (opt.startsWith(args[1].toLowerCase())) out.add(opt);
        }
        else if (args.length == 3 && bfCmd(args[0]) && args[1].equalsIgnoreCase("mobs")) {
            for (String v : List.of("on","off"))
                if (v.startsWith(args[2].toLowerCase())) out.add(v);
        }
        else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player pg : Bukkit.getOnlinePlayers())
                if (pg.getName().toLowerCase().startsWith(args[2].toLowerCase())) out.add(pg.getName());
            for (String n : List.of("1","8","16","32","64"))
                if (n.startsWith(args[2])) out.add(n);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give"))
            for (String n : List.of("1","8","16","32","64"))
                if (n.startsWith(args[3])) out.add(n);
        return out;
    }
    private boolean bfCmd(String s) {
        String l = s.toLowerCase();
        return l.equals("bonfire") || l.equals("костер") || l.equals("bf");
    }

    private enum RemoveReason { EXPIRED, ADMIN, SHEARS }
    private static class MaskData { final String origDisplay, origList; MaskData(String d, String l) { origDisplay = d; origList = l; } }
    private static class SmokeCloud { final Location center; final int radius; final int height; final long expireTick; SmokeCloud(Location c, int r, int h, long e) { center = c; radius = r; height = h; expireTick = e; } }
    private static class Hookah { int tobaccoType; int puffsLeft; Hookah(int t, int p) { tobaccoType = t; puffsLeft = p; } }
    /** Состояние захвата цели самонаводящимся луком */
    private static class HomingLock {
        Entity target;
        int progress;
        int mode;
        HomingLock(Entity t, int m) { target = t; progress = 0; mode = m; }
    }
    /** Состояние поводка: кто держит, кого, с какого времени, к какому блоку привязан (если есть) */
    private static class LeashTie {
        UUID owner;
        UUID victim;
        long tiedAt;
        Location blockLoc;
        World blockWorld;
        LeashTie(UUID o, UUID v, long t) { owner = o; victim = v; tiedAt = t; }
    }

    /** Состояние ритуального костра */
    private static class BonfireData {
        int fuel = BONFIRE_MAX_FUEL;
        int breakCount = 0;
        boolean soul = false;
        long deadUntil = 0L; // unix-ms до которого потушен (grace period)
    }
    /** Морозное облако от крио-гранаты */
    private static class FreezeCloud {
        final Location center;
        final int radius;
        final int height;
        final long expireTick;
        FreezeCloud(Location c, int r, int h, long e) { center = c; radius = r; height = h; expireTick = e; }
    }
    /** Временно замороженный блок (вода→лёд, лава→обсидиан) */
    private static class FrozenBlock {
        final World world;
        final int x, y, z;
        final Material original;
        final Material frozenAs;
        final long thawAt;
        FrozenBlock(World w, int x, int y, int z, Material o, Material f, long t) {
            world = w; this.x = x; this.y = y; this.z = z;
            original = o; frozenAs = f; thawAt = t;
        }
    }
    /** Фейковый моб-галлюцинация */
    private static class Hallucination {
        final UUID entityUid;
        final UUID ownerUid;
        final long expireTick;
        Hallucination(UUID e, UUID o, long t) { entityUid = e; ownerUid = o; expireTick = t; }
    }
}
