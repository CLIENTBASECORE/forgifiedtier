package dev.forgified.practice;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scoreboard.Team;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.block.Chest;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class ForgifiedPracticePlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final String MENU_TITLE = ChatColor.BLUE + "" + ChatColor.BOLD + "Practice Queue";
    private static final String SETTINGS_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Player Settings";
    private static final String LEADERBOARDS_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Leaderboards";
    private static final String EVENTS_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Frost Events";
    private static final String PARTY_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Party";
    private static final String DUEL_KIT_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Select Duel Kit";
    private static final String DUEL_MAP_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Select Duel Map";
    private static final String KIT_EDITOR_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Select A Kit";

    private final Map<String, KitData> kits = new LinkedHashMap<>();
    private final List<ArenaSpawns> arenas = new ArrayList<>();
    private final Set<ArenaSpawns> busyArenas = new HashSet<>();
    private final Map<String, Queue<UUID>> queues = new HashMap<>();
    private final Map<UUID, String> queuedKit = new HashMap<>();
    private final Map<UUID, String> queuedType = new HashMap<>();
    private final Map<UUID, Long> queuedAt = new HashMap<>();
    private final Map<UUID, Match> matches = new HashMap<>();
    private final Map<UUID, Match> lastPlayedMatch = new HashMap<>();
    private final Map<UUID, DuelRequest> duelRequests = new HashMap<>();
    private final Map<UUID, UUID> duelMenuTargets = new HashMap<>();
    private final Map<UUID, KitData> duelMenuKits = new HashMap<>();
    private final Map<UUID, String> partyMenuModes = new HashMap<>();
    private final Map<UUID, UUID> partyMenuTargets = new HashMap<>();
    private final Map<UUID, KitData> partyMenuKits = new HashMap<>();
    private final Map<UUID, KitData> layoutEditorKits = new HashMap<>();
    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, PartyInvite> partyInvites = new HashMap<>();
    private final Map<UUID, PlayerSettings> playerSettings = new HashMap<>();
    private final Map<UUID, PlayerStats> statsCache = new HashMap<>();
    private final Map<UUID, EventSession> activeEvents = new HashMap<>();
    private final Map<String, List<HotbarItem>> hotbars = new HashMap<>();
    private final Set<UUID> pluginTeleports = new HashSet<>();
    private final Set<String> filledEventChests = new HashSet<>();
    private final Set<String> bedRespawnKits = new HashSet<>();
    private final Set<UUID> externalScoreboardPlayers = new HashSet<>();
    // Sidebar scoreboard state (use stable teams to avoid flicker)
    private final Map<UUID, List<String>> scoreboardEntries = new HashMap<>();
    private final Map<UUID, Integer> scoreboardLineCount = new HashMap<>();
    private final Map<UUID, Scoreboard> practiceScoreboards = new HashMap<>();
    private final Map<UUID, Boolean> scoreboardHadHealth = new HashMap<>();
    private final Set<UUID> scoreboardDirty = new HashSet<>();
    private BukkitTask scoreboardDirtyRefreshTask;
    private final Map<UUID, String> scoreboardContext = new HashMap<>();
    private final Map<UUID, Integer> lastBelowNameHealth = new HashMap<>();
    private long scoreboardPulse = 0L;
    // Cache Fastbridge assignment checks to avoid intermittent reflection failures causing flicker
    private final Map<UUID, Boolean> cachedFastbridgeAssigned = new HashMap<>();
    private final Map<UUID, Long> cachedFastbridgeAssignedUntil = new HashMap<>();
    private final Set<UUID> respawningPlayers = new HashSet<>();
    private final Map<UUID, Long> spawnProtectionUntil = new HashMap<>();
    private final Map<UUID, UUID> primedTntOwners = new HashMap<>();
    private final Map<UUID, String> arenaEditors = new HashMap<>();
    private final Set<UUID> spawnEditors = new HashSet<>();
    private final Map<UUID, Match> spectatingMatch = new HashMap<>();
    private final Random random = new Random();
    private final Map<String, String> lastArenaNamePerKit = new HashMap<>();
    private final Map<UUID, Long> pearlCooldown = new HashMap<>();
    private final Map<UUID, Long> bowCooldown = new HashMap<>();
    private final Map<UUID, UUID> lastDamager = new HashMap<>();
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private final Map<String, List<UUID>> airLeaderboardEntities = new HashMap<>();
    private final Map<UUID, ArmorStand> airLeaderboardStandById = new HashMap<>();
    private final List<String> frostResourceNames = Arrays.asList(
            "arenas.yml", "chest.yml", "config.yml", "event-scoreboard.yml", "hotbar.yml",
            "kits.yml", "menus.yml", "messages.yml", "scoreboard.yml", "settings.yml", "tablist.yml",
            "void.yml", "sumo-void.yml", "spleef-void.yml", "heightlimit.yml", "bridge-voidtime.yml", "bridgeportal.yml", "Fireball.yml", "sound.yml", "divisions.yml", "bed.yml", "anticheat.yml", "duel.yml", "leavecommand.yml", "width.yml", "length.yml", "hostrank.yml");

    private FileConfiguration hostrankConfig;
    private final Set<UUID> publicParties = new HashSet<>();

    private FileConfiguration frostConfig;
    private FileConfiguration frostKits;
    private FileConfiguration frostArenas;
    private FileConfiguration frostHotbar;
    private FileConfiguration frostMenus;
    private FileConfiguration frostMessages;
    private FileConfiguration frostScoreboard;
    private FileConfiguration frostEventScoreboard;
    private FileConfiguration frostSettings;
    private FileConfiguration frostChest;
    private FileConfiguration voidConfig;
    private FileConfiguration sumoVoidConfig;
    private FileConfiguration spleefVoidConfig;
    private FileConfiguration heightLimitConfig;
    private FileConfiguration bridgeVoidConfig;
    private FileConfiguration bridgePortalConfig;
    private FileConfiguration fireballConfig;
    private FileConfiguration soundConfig;
    private FileConfiguration divisionsConfig;
    private FileConfiguration bedConfig;
    private FileConfiguration anticheatConfig;
    private FileConfiguration duelConfig;
    private FileConfiguration leaveCommandConfig;
    private FileConfiguration widthConfig;
    private FileConfiguration lengthConfig;
    private final Map<UUID, Integer> flightVL = new HashMap<>();
    private final Map<UUID, Integer> reachVL = new HashMap<>();
    private final Map<UUID, Integer> speedVL = new HashMap<>();
    private final Set<UUID> alertsEnabled = new HashSet<>();
    private File kitLayoutsFile;
    private FileConfiguration kitLayoutsConfig;
    private File statsFile;
    private FileConfiguration statsConfig;
    private File airLeaderboardFile;
    private FileConfiguration airLeaderboardConfig;
    private Location spawnLocation;
    private Location spawnMin;
    private Location spawnMax;
    private int countdownSeconds;
    private int voidY;
    private int voidBlocksUnderSpawn;
    private int sumoVoidBlocksUnderSpawn;
    private boolean sumoUseAbsoluteY;
    private int sumoVoidAbsoluteY;
    private int spleefVoidBlocksUnderSpawn;
    private boolean spleefUseAbsoluteY;
    private int spleefVoidAbsoluteY;
    private int respawnCountdownSeconds;
    private int heightLimitBlocksAboveSpawn;
    private int bridgeRespawnCountdownSeconds;
    private int bridgeVoidBlocksUnderSpawn;
    private int bridgeVoidAbsoluteY;
    private boolean bridgeUseAbsoluteVoidY;
    private boolean bridgeVoidInstantRespawn;
    private int spawnProtectionSeconds;
    private int widthOverall;
    private int widthBedFight;
    private int widthFireballFight;
    private int widthBridge;
    private int widthBattleRush;
    private int lengthOverall;
    private int lengthBedFight;
    private int lengthFireballFight;
    private int lengthBridge;
    private int lengthBattleRush;
    private boolean useFrostHotbar;
    private boolean scoreboardEnabled;
    private boolean fireballThrowEnabled;
    private boolean fireballKbEnabled;
    private double fireballSpeed;
    private double fireballKbMultiplier;
    private String scoreboardTitle;
    private String prefix;
    private BukkitTask scoreboardTask;
    private BukkitTask airLeaderboardTask;
    private final Map<UUID, Long> fireballCooldown = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadStats();
        loadAirLeaderboards();
        reloadPractice();
        refreshLeaderboards();
        // Remove any old/orphaned ArmorStand leaderboards that may still exist from previous versions/restarts
        purgeOrphanAirLeaderboards();
        spawnAirLeaderboards();
        Bukkit.getPluginManager().registerEvents(this, this);
        // Refresh scoreboards every tick (match + lobby). Flicker is prevented by only sending changes.
        scoreboardTask = Bukkit.getScheduler().runTaskTimer(this, this::refreshScoreboardsTick, 1L, 1L);
        // Keep leaderboards synced (every 20 ticks = 1 second)
        airLeaderboardTask = Bukkit.getScheduler().runTaskTimer(this, this::tickLeaderboards, 20L, 20L);
        registerCommand("forgifiedpractice");
        registerCommand("practice");
        registerCommand("queue");
        registerCommand("duel");
        registerCommand("party");
        registerCommand("settings");
        registerCommand("stats");
        registerCommand("unqueue");
        registerCommand("lobby");
        registerCommand("setlobby");
        registerCommand("map");
        registerCommand("spectate");
        registerCommand("friend");
        registerCommand("leaderboard");
        registerCommand("divisions");
        registerCommand("alerts");
    }

    /**
     * Runs every 20 ticks (1 second):
     * - rebuild leaderboard cache
     * - update ArmorStand leaderboard text
     * - refresh any open leaderboard inventories
     */
    private void tickLeaderboards() {
        refreshLeaderboards();
        tickAirLeaderboards();
        refreshOpenLeaderboardInventories();
    }

    @Override
    public void onDisable() {
        for (Match match : new HashSet<>(matches.values())) {
            finishMatch(match, null, "Plugin disabled");
        }
        queues.clear();
        queuedKit.clear();
        queuedType.clear();
        queuedAt.clear();
        activeEvents.clear();
        filledEventChests.clear();
        removeAirLeaderboardEntities();
        saveStats();
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }
        if (airLeaderboardTask != null) {
            airLeaderboardTask.cancel();
            airLeaderboardTask = null;
        }
        if (scoreboardDirtyRefreshTask != null) {
            scoreboardDirtyRefreshTask.cancel();
            scoreboardDirtyRefreshTask = null;
        }
    }

    private void registerCommand(String name) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(this);
            getCommand(name).setTabCompleter(this);
            if (name.equalsIgnoreCase("friend")) {
                getCommand(name).setPermission(null);
                getCommand(name).setPermissionMessage(null);
            }
        }
    }

    private void reloadPractice() {
        countdownSeconds = 5;
        voidY = 0;
        voidBlocksUnderSpawn = 25;
        sumoVoidBlocksUnderSpawn = 25;
        sumoUseAbsoluteY = false;
        sumoVoidAbsoluteY = 0;
        respawnCountdownSeconds = 3;
        heightLimitBlocksAboveSpawn = 50;
        bridgeRespawnCountdownSeconds = 3;
        bridgeVoidBlocksUnderSpawn = 25;
        bridgeVoidAbsoluteY = 0;
        bridgeUseAbsoluteVoidY = false;
        bridgeVoidInstantRespawn = true;
        spawnProtectionSeconds = 3;
        useFrostHotbar = true;
        scoreboardEnabled = true;
        fireballThrowEnabled = true;
        fireballKbEnabled = true;
        fireballSpeed = 1.5D;
        fireballKbMultiplier = 1.8D;
        bedRespawnKits.clear();
        Collections.addAll(bedRespawnKits, normalize("BedFight"), normalize("FireballFight"), normalize("BattleRush"), normalize("MLGRush"), normalize("Bridge"));

        copyBundledFrostFiles();
        File frostFolder = resolveFrostFolder();
        frostConfig = YamlConfiguration.loadConfiguration(new File(frostFolder, "config.yml"));
        frostKits = YamlConfiguration.loadConfiguration(new File(frostFolder, "kits.yml"));
        frostArenas = YamlConfiguration.loadConfiguration(new File(frostFolder, "arenas.yml"));
        frostHotbar = YamlConfiguration.loadConfiguration(new File(frostFolder, "hotbar.yml"));
        frostMenus = YamlConfiguration.loadConfiguration(new File(frostFolder, "menus.yml"));
        frostMessages = YamlConfiguration.loadConfiguration(new File(frostFolder, "messages.yml"));
        frostScoreboard = YamlConfiguration.loadConfiguration(new File(frostFolder, "scoreboard.yml"));
        frostEventScoreboard = YamlConfiguration.loadConfiguration(new File(frostFolder, "event-scoreboard.yml"));
        frostSettings = YamlConfiguration.loadConfiguration(new File(frostFolder, "settings.yml"));
        frostChest = YamlConfiguration.loadConfiguration(new File(frostFolder, "chest.yml"));
        voidConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "void.yml"));
        sumoVoidConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "sumo-void.yml"));
        spleefVoidConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "spleef-void.yml"));
        heightLimitConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "heightlimit.yml"));
        bridgeVoidConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "bridge-voidtime.yml"));
        bridgePortalConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "bridgeportal.yml"));
        fireballConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "Fireball.yml"));
        soundConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "sound.yml"));
        divisionsConfig = YamlConfiguration.loadConfiguration(new File(frostFolder, "divisions.yml"));
        bedConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "bed.yml"));
        anticheatConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "anticheat.yml"));
        duelConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "duel.yml"));
        leaveCommandConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "leavecommand.yml"));
        widthConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "width.yml"));
        lengthConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "length.yml"));
        hostrankConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "hostrank.yml"));


        scoreboardTitle = color(frostScoreboard.getString("SCOREBOARD.TITLE", "&6&lPractice"));
        prefix = "";

        World fallbackWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        spawnLocation = parseLocation(frostConfig.getString("spawnLocation"), fallbackWorld);
        if (spawnLocation == null && fallbackWorld != null) {
            spawnLocation = fallbackWorld.getSpawnLocation();
        }
        spawnMin = parseLocation(frostConfig.getString("spawnMin"), spawnLocation == null ? fallbackWorld : spawnLocation.getWorld());
        spawnMax = parseLocation(frostConfig.getString("spawnMax"), spawnLocation == null ? fallbackWorld : spawnLocation.getWorld());
        voidBlocksUnderSpawn = Math.max(1, voidConfig.getInt("void.blocks-under-spawn", 25));
        sumoVoidBlocksUnderSpawn = Math.max(1, sumoVoidConfig.getInt("void.blocks-under-spawn", voidBlocksUnderSpawn));
        sumoUseAbsoluteY = sumoVoidConfig.getBoolean("void.use-absolute-y", false);
        sumoVoidAbsoluteY = sumoVoidConfig.getInt("void.absolute-y", 0);
        spleefVoidBlocksUnderSpawn = Math.max(1, spleefVoidConfig.getInt("void.blocks-under-spawn", voidBlocksUnderSpawn));
        spleefUseAbsoluteY = spleefVoidConfig.getBoolean("void.use-absolute-y", false);
        spleefVoidAbsoluteY = spleefVoidConfig.getInt("void.absolute-y", 0);
        respawnCountdownSeconds = Math.max(1, voidConfig.getInt("void.respawn-countdown-seconds", 3));
        heightLimitBlocksAboveSpawn = Math.max(0, heightLimitConfig.getInt("height-limit.blocks-above-spawn", 50));
        bridgeRespawnCountdownSeconds = Math.max(0, bridgeVoidConfig.getInt("bridge.respawn-countdown-seconds",
                bridgeVoidConfig.getInt("bridge.portal-reset-countdown-seconds", 3)));
        bridgeVoidBlocksUnderSpawn = Math.max(1, bridgeVoidConfig.getInt("bridge.void.blocks-under-spawn",
                bridgeVoidConfig.getInt("bridge.void-depth-under-spawn", voidBlocksUnderSpawn)));
        bridgeVoidAbsoluteY = bridgeVoidConfig.getInt("bridge.void.absolute-y", voidY);
        bridgeUseAbsoluteVoidY = bridgeVoidConfig.getBoolean("bridge.void.use-absolute-y", false);
        bridgeVoidInstantRespawn = bridgeVoidConfig.getBoolean("bridge.void-instant-respawn", true);
        spawnProtectionSeconds = Math.max(0, bridgeVoidConfig.getInt("bridge.spawn-protection-seconds", 3));
        widthOverall = Math.max(0, widthConfig.getInt("width.blocks-from-center", 0));
        widthBedFight = Math.max(0, widthConfig.getInt("width.bedfight", 0));
        widthFireballFight = Math.max(0, widthConfig.getInt("width.fireballfight", 0));
        widthBridge = Math.max(0, widthConfig.getInt("width.bridge", 0));
        widthBattleRush = Math.max(0, widthConfig.getInt("width.battlerush", 0));
        lengthOverall = Math.max(0, lengthConfig.getInt("length.blocks-from-center", 0));
        lengthBedFight = Math.max(0, lengthConfig.getInt("length.bedfight", 0));
        lengthFireballFight = Math.max(0, lengthConfig.getInt("length.fireballfight", 0));
        lengthBridge = Math.max(0, lengthConfig.getInt("length.bridge", 0));
        lengthBattleRush = Math.max(0, lengthConfig.getInt("length.battlerush", 0));
        if (spawnLocation != null) {
            voidY = spawnLocation.getBlockY() - voidBlocksUnderSpawn;
        } else {
            voidY = voidConfig.getInt("void.absolute-y", 0);
        }
        fireballThrowEnabled = fireballConfig.getBoolean("enable trow fireball", fireballConfig.getBoolean("fireball.enable-throw-fireball", true));
        fireballKbEnabled = fireballConfig.getBoolean("enable KB", fireballConfig.getBoolean("fireball.enable-kb", true));
        fireballSpeed = fireballConfig.getDouble("fireball.speed", 1.5D);
        fireballKbMultiplier = fireballConfig.getDouble("fireball.kb-multiplier", 1.8D);

        loadKits(frostKits);
        loadArenas(frostArenas, fallbackWorld);
        loadHotbars(frostHotbar);
        getLogger().info("Loaded practice files from " + frostFolder.getName() + ": kits=" + kits.size() + ", arenas=" + arenas.size()
                + ", hotbars=" + hotbars.size() + ", chestPresets=" + frostChest.getConfigurationSection("CHESTS"));
    }

    private File resolveFrostFolder() {
        return getDataFolder();
    }

    private void copyBundledFrostFiles() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        for (String resourceName : frostResourceNames) {
            File target = new File(getDataFolder(), resourceName);
            if (!target.isFile()) {
                saveResource(resourceName, false);
            }
        }
    }

    private void loadKits(FileConfiguration frostKits) {
        kits.clear();
        ConfigurationSection section = frostKits.getConfigurationSection("kits");
        if (section == null) {
            return;
        }
        for (String name : section.getKeys(false)) {
            String path = "kits." + name;
            ItemStack icon = frostKits.getItemStack(path + ".icon");
            ItemStack[] contents = readItems(frostKits.getList(path + ".contents"), 36);
            ItemStack[] armor = readItems(frostKits.getList(path + ".armor"), 4);
            String display = color(frostKits.getString(path + ".displayName", name));
            int unrankedPos = frostKits.getInt(path + ".unrankedPos", -1);
            int rankedPos = frostKits.getInt(path + ".rankedPos", -1);
            int editorPos = frostKits.getInt(path + ".editorPos", -1);
            int spawnFfaPos = frostKits.getInt(path + ".spawnFfaPos", -1);
            Set<String> arenaWhitelist = new HashSet<>();
            for (String arenaName : frostKits.getStringList(path + ".arenaWhitelist")) {
                arenaWhitelist.add(normalize(arenaName));
            }
            boolean ranked = frostKits.getBoolean(path + ".ranked", true);
            boolean combo = frostKits.getBoolean(path + ".combo", normalize(name).contains("combo"));
            boolean sumo = frostKits.getBoolean(path + ".sumo", normalize(name).contains("sumo"));
            boolean boxing = frostKits.getBoolean(path + ".boxing", normalize(name).contains("boxing"));
            boolean build = frostKits.getBoolean(path + ".build", false);
            boolean bridges = frostKits.getBoolean(path + ".bridges", normalize(name).contains("bridge"));
            boolean skyWars = frostKits.getBoolean(path + ".skyWars", normalize(name).contains("skywars"));
            boolean bedWars = frostKits.getBoolean(path + ".bedWars", normalize(name).contains("bedfight") || normalize(name).contains("fireball"));
            boolean noFall = frostKits.getBoolean(path + ".noFall", false);
            boolean noHunger = frostKits.getBoolean(path + ".noHunger", false);
            boolean noRegen = frostKits.getBoolean(path + ".noRegen", false);
            int damageTicks = frostKits.getInt(path + ".damageTicks", combo ? 0 : 20);
            int goals = frostKits.getInt(path + ".goals", bridges ? 5 : 0);
            kits.put(normalize(name), new KitData(name, display, icon, contents, armor, unrankedPos, rankedPos, editorPos,
                    spawnFfaPos, arenaWhitelist, ranked, combo, sumo, boxing, build, bridges, skyWars, bedWars, noFall, noHunger, noRegen,
                    damageTicks, goals));
        }
    }

    private void loadArenas(FileConfiguration frostArenas, World fallbackWorld) {
        arenas.clear();
        busyArenas.clear();
        ConfigurationSection section = frostArenas.getConfigurationSection("arenas");
        if (section == null) {
            return;
        }
        for (String arena : section.getKeys(false)) {
            String base = "arenas." + arena;
            addArena(arena, base, frostArenas, fallbackWorld);
            ConfigurationSection standalone = frostArenas.getConfigurationSection(base + ".standaloneArenas");
            if (standalone == null) {
                continue;
            }
            for (String id : standalone.getKeys(false)) {
                addArena(arena + "-" + id, base + ".standaloneArenas." + id, frostArenas, fallbackWorld);
            }
        }
    }

    private void addArena(String name, String path, FileConfiguration file, World fallbackWorld) {
        if (!file.getBoolean(path + ".enabled", true)) {
            return;
        }
        Location first = parseLocation(file.getString(path + ".a"), fallbackWorld);
        Location second = parseLocation(file.getString(path + ".b"), fallbackWorld);
        if (first != null && second != null) {
            arenas.add(new ArenaSpawns(name, findSafe(first), findSafe(second)));
        }
    }

    private void loadHotbars(FileConfiguration frostHotbar) {
        hotbars.clear();
        if (!useFrostHotbar) {
            loadFallbackHotbars();
            return;
        }
        loadHotbarSection(frostHotbar, "IN-SPAWN");
        loadHotbarSection(frostHotbar, "IN-QUEUE");
        loadHotbarSection(frostHotbar, "IN-PARTY");
        if (hotbars.isEmpty()) {
            loadFallbackHotbars();
        }
    }

    private void loadHotbarSection(FileConfiguration file, String sectionName) {
        ConfigurationSection section = file.getConfigurationSection(sectionName);
        if (section == null) {
            return;
        }
        List<HotbarItem> items = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            String path = sectionName + "." + key;
            if (!file.getBoolean(path + ".ENABLED", true)) {
                continue;
            }
            Material material = Material.matchMaterial(file.getString(path + ".MATERIAL", "STONE"));
            if (material == null) {
                material = Material.STONE;
            }
            String action = file.getString(path + ".ACTION", key);
            String name = color(file.getString(path + ".NAME", key));

            // Force old "Host Events" hotbar item to show as "Public Parties (Right Click)"
            // (only changes display name; action stays the same)
            if (material == Material.EYE_OF_ENDER && "EVENTS_MENU".equalsIgnoreCase(action)) {
                name = color("&6Public Parties &7(Right Click)");
            }
            items.add(new HotbarItem(
                    file.getInt(path + ".SLOT", 0),
                    material,
                    (short) file.getInt(path + ".DATA", 0),
                    Math.max(1, file.getInt(path + ".AMOUNT", 1)),
                    name,
                    action,
                    file.getString(path + ".COMMAND", "")));
        }
        hotbars.put(sectionName, items);
    }

    private void loadFallbackHotbars() {
        hotbars.put("IN-SPAWN", Arrays.asList(
                new HotbarItem(0, Material.IRON_SWORD, (short) 0, 1, ChatColor.GOLD + "Unranked Queue " + ChatColor.GRAY + "(Right Click)", "JOIN_UNRANKED", ""),
                new HotbarItem(1, Material.DIAMOND_SWORD, (short) 0, 1, ChatColor.GOLD + "Duel " + ChatColor.GRAY + "(Right Click)", "DUEL_MENU", ""),
                new HotbarItem(4, Material.NAME_TAG, (short) 0, 1, ChatColor.GOLD + "Create Party " + ChatColor.GRAY + "(Right Click)", "CREATE_PARTY", ""),
                new HotbarItem(5, Material.SKULL_ITEM, (short) 3, 1, ChatColor.GOLD + "Settings " + ChatColor.GRAY + "(Right Click)", "SETTINGS_MENU", ""),
                new HotbarItem(6, Material.EMERALD, (short) 0, 1, ChatColor.GOLD + "Leaderboards " + ChatColor.GRAY + "(Right Click)", "LEADERBOARDS_MENU", ""),
                new HotbarItem(7, Material.EYE_OF_ENDER, (short) 0, 1, ChatColor.GOLD + "Public Parties " + ChatColor.GRAY + "(Right Click)", "OPEN_PUBLIC_PARTIES", ""),
                new HotbarItem(8, Material.SKULL_ITEM, (short) 3, 1, ChatColor.GOLD + "Profile " + ChatColor.GRAY + "(Right Click)", "OPEN_STATS", "")));
        hotbars.put("IN-QUEUE", Collections.singletonList(
                new HotbarItem(8, Material.INK_SACK, (short) 1, 1, ChatColor.RED + "Leave Queue " + ChatColor.GRAY + "(Right Click)", "LEAVE_QUEUE", "")));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (!(sender instanceof Player)) {
            if ((name.equals("practice") || name.equals("forgifiedpractice")) && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadPractice();
                sender.sendMessage("ForgifiedPractice reloaded.");
                return true;
            }
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;
        if (name.equals("friend") || label.equalsIgnoreCase("friend") || label.equalsIgnoreCase("f")) {
            handleFriendCommand(player, args);
            return true;
        }
        if (name.equals("queue")) {
            if (args.length == 0) {
                openQueueMenu(player, "unranked");
                return true;
            }
            queue(player, args[0], args.length > 1 ? args[1] : "unranked");
            return true;
        }
        if (name.equals("duel")) {
            handleDuelCommand(player, args);
            return true;
        }
        if (name.equals("party")) {
            handlePartyCommand(player, args);
            return true;
        }
        if (name.equals("event") || name.equals("events") || name.equals("hostevent")) {
            player.sendMessage(prefix + ChatColor.RED + "Events are disabled for now.");
            return true;
        }
        if (name.equals("settings")) {
            openSettingsMenu(player);
            return true;
        }
        if (name.equals("stats")) {
            openStatsMenu(player);
            return true;
        }
        if (name.equals("unqueue")) {
            leaveQueue(player, true);
            return true;
        }
        if (name.equals("lobby")) {
            if (Boolean.TRUE.equals(fastbridgeAssignedState(player))) {
                String cmd = leaveCommandConfig.getString("external-leave-command", "fp leave");
                player.performCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
                return true;
            }
            if (spectatingMatch.containsKey(player.getUniqueId())) {
                spectatingMatch.remove(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Stopped spectating.");
            }
            returnToSpawn(player, true);
            return true;
        }
        if (name.equals("setlobby")) {
            handleSetLobbyCommand(player);
            return true;
        }
        if (name.equals("map")) {
            sendCurrentMap(player);
            return true;
        }
        if (name.equals("spectate")) {
            handleSpectateCommand(player, args);
            return true;
        }
        if ((name.equals("practice") || name.equals("forgifiedpractice")) && args.length >= 1 && args[0].equalsIgnoreCase("leave")) {
            if (spectatingMatch.containsKey(player.getUniqueId())) {
                spectatingMatch.remove(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Stopped spectating.");
            }
            returnToSpawn(player, true);
            return true;
        }
        if ((name.equals("practice") || name.equals("forgifiedpractice")) && args.length >= 2
                && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("lobby"))) {
            handleFastBuilderCommand(player, args);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload") && isAdmin(player)) {
            reloadPractice();
            player.sendMessage(prefix + ChatColor.GREEN + "Reloaded Frost kits and arenas.");
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("addwins"))) {
            if (!isAdmin(player)) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            boolean hasWinsArg = args.length >= 2 && args[1].equalsIgnoreCase("wins");
            int startIdx = hasWinsArg ? 2 : 1;
            OfflinePlayer target = null;
            int amount = 0;
            if (args.length >= startIdx + 2) {
                target = Bukkit.getOfflinePlayer(args[startIdx]);
                try {
                    amount = Integer.parseInt(args[startIdx + 1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid wins amount: " + args[startIdx + 1]);
                    return true;
                }
            } else if (args.length == startIdx + 1) {
                target = player;
                try {
                    amount = Integer.parseInt(args[startIdx]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid wins amount: " + args[startIdx]);
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /fp add wins <player> <amount>");
                return true;
            }

            PlayerStats tStats = stats(target.getUniqueId());
            tStats.wins += amount;
            writeStats(target.getUniqueId(), tStats);
            saveStats();
            player.sendMessage(ChatColor.GREEN + "Added " + amount + " wins to " + target.getName() + ". Total wins: " + tStats.wins);
            Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
            if (onlineTarget != null) {
                updateLuckPermsPrefix(onlineTarget);
            }
            refreshLeaderboards();
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("removewins"))) {
            if (!isAdmin(player)) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            boolean hasWinsArg = args.length >= 2 && args[1].equalsIgnoreCase("wins");
            int startIdx = hasWinsArg ? 2 : 1;
            OfflinePlayer target = null;
            int amount = 0;
            if (args.length >= startIdx + 2) {
                target = Bukkit.getOfflinePlayer(args[startIdx]);
                try {
                    amount = Integer.parseInt(args[startIdx + 1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid wins amount: " + args[startIdx + 1]);
                    return true;
                }
            } else if (args.length == startIdx + 1) {
                target = player;
                try {
                    amount = Integer.parseInt(args[startIdx]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid wins amount: " + args[startIdx]);
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /fp remove wins <player> <amount>");
                return true;
            }

            PlayerStats tStats = stats(target.getUniqueId());
            tStats.wins = Math.max(0, tStats.wins - amount);
            writeStats(target.getUniqueId(), tStats);
            saveStats();
            player.sendMessage(ChatColor.GREEN + "Removed " + amount + " wins from " + target.getName() + ". Total wins: " + tStats.wins);
            Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
            if (onlineTarget != null) {
                updateLuckPermsPrefix(onlineTarget);
            }
            refreshLeaderboards();
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("change") && args.length >= 3 && args[1].equalsIgnoreCase("division")) {
            if (!isAdmin(player)) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            OfflinePlayer target;
            String division;
            if (args.length >= 4) {
                target = Bukkit.getOfflinePlayer(args[2]);
                division = args[3];
            } else {
                target = player;
                division = args[2];
            }

            if (!division.equalsIgnoreCase("reset") && !division.equalsIgnoreCase("none")) {
                if (divisionsConfig == null || !divisionsConfig.contains("divisions." + division.toLowerCase(Locale.ROOT))) {
                    Set<String> validKeys = divisionsConfig != null && divisionsConfig.contains("divisions")
                            ? divisionsConfig.getConfigurationSection("divisions").getKeys(false)
                            : Collections.emptySet();
                    player.sendMessage(ChatColor.RED + "Invalid division: " + division + ". Valid divisions: " + validKeys);
                    return true;
                }
            }

            PlayerStats tStats = stats(target.getUniqueId());
            if (division.equalsIgnoreCase("reset") || division.equalsIgnoreCase("none")) {
                tStats.customDivision = null;
                player.sendMessage(ChatColor.GREEN + "Reset " + target.getName() + "'s division to follow their wins.");
            } else {
                tStats.customDivision = division.toLowerCase(Locale.ROOT);
                player.sendMessage(ChatColor.GREEN + "Changed " + target.getName() + "'s division to " + division + ".");
            }
            writeStats(target.getUniqueId(), tStats);
            saveStats();
            Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
            if (onlineTarget != null) {
                updateLuckPermsPrefix(onlineTarget);
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("finish")) {
            finishArenaEdit(player);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("kit")) {
            handleKitAdminCommand(player, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("arena")) {
            handleArenaAdminCommand(player, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("queue") && args.length > 1) {
            queue(player, args[1], args.length > 2 ? args[2] : "unranked");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("duel")) {
            handleDuelCommand(player, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("party")) {
            handlePartyCommand(player, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("settings")) {
            openSettingsMenu(player);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("leaderboard")) {
            handleAirLeaderboardCommand(player, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }
        if (label.equalsIgnoreCase("stats")) {
            openStatsMenu(player);
            return true;
        }
        if (label.equalsIgnoreCase("leaderboard") || label.equalsIgnoreCase("lb")) {
            if (args.length > 0 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("delete"))) {
                handleAirLeaderboardCommand(player, args);
            } else {
                refreshLeaderboards();
                openLeaderboardsMenu(player);
            }
            return true;
        }
        if (label.equalsIgnoreCase("friend")) {
            handleFriendCommand(player, args);
            return true;
        }
        if (label.equalsIgnoreCase("divisions")) {
            handleDivisionsCommand(player);
            return true;
        }
        if (label.equalsIgnoreCase("alerts")) {
            if (!isAdmin(player)) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (alertsEnabled.contains(player.getUniqueId())) {
                alertsEnabled.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "ForgifiedAC alerts disabled.");
            } else {
                alertsEnabled.add(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "ForgifiedAC alerts enabled.");
            }
            return true;
        }
        openQueueMenu(player, "unranked");
        return true;
    }

    private void handleSetLobbyCommand(Player player) {
        if (!isAdmin(player)) {
            player.sendMessage(prefix + ChatColor.RED + "No permission.");
            return;
        }
        frostConfig.set("spawnLocation", serializeLocation(player.getLocation()));
        saveYaml(frostConfig, new File(getDataFolder(), "config.yml"), "config.yml");
        reloadPractice();
        player.sendMessage(prefix + ChatColor.GREEN + "Lobby set. Players will return here after matches.");
    }

    private void handleFastBuilderCommand(Player player, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("spawn")) {
            if (args[1].equalsIgnoreCase("edit")) {
                startSpawnEdit(player);
                return;
            }
            if (args[1].equalsIgnoreCase("finish")) {
                finishSpawnEdit(player);
                return;
            }
        }
        player.sendMessage(prefix + ChatColor.YELLOW + "Use /fp spawn edit or /fp spawn finish.");
    }

    private void startSpawnEdit(Player player) {
        if (!isAdmin(player)) {
            player.sendMessage(prefix + ChatColor.RED + "No permission.");
            return;
        }
        spawnEditors.add(player.getUniqueId());
        player.sendMessage(prefix + ChatColor.GREEN + "Spawn edit mode enabled. Use /fp spawn finish when done.");
    }

    private void finishSpawnEdit(Player player) {
        if (!isAdmin(player)) {
            player.sendMessage(prefix + ChatColor.RED + "No permission.");
            return;
        }
        if (!spawnEditors.remove(player.getUniqueId())) {
            player.sendMessage(prefix + ChatColor.RED + "You are not editing spawn.");
            return;
        }
        player.sendMessage(prefix + ChatColor.GREEN + "Spawn edit mode disabled.");
        returnToSpawn(player, false);
    }

    private void handleSpectateCommand(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Use /spectate <player>.");
            return;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "That player is not online.");
            return;
        }
        Match match = matches.get(target.getUniqueId());
        if (match == null || match.ending) {
            player.sendMessage(ChatColor.RED + "That player is not in a match.");
            return;
        }
        if (matches.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot spectate while in a match.");
            return;
        }

        resetPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);

        spectatingMatch.put(player.getUniqueId(), match);
        pluginTeleport(player, target.getLocation());
        clearPracticeScoreboard(player);

        // Tracker Compass
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Player Tracker " + ChatColor.GRAY + "(Right-Click)");
        compass.setItemMeta(meta);
        player.getInventory().setItem(0, compass);
        player.getInventory().setItem(8, simpleItem(Material.INK_SACK, (short) 1, ChatColor.RED + "Return to Lobby " + ChatColor.GRAY + "(Right Click)", Collections.emptyList()));
        player.getInventory().setHeldItemSlot(0);

        player.sendMessage(ChatColor.GREEN + "Now spectating " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + ".");
        
        // Announcements
        target.sendMessage(ChatColor.YELLOW + player.getName() + " started spectating you.");
        Player opponent = Bukkit.getPlayer(match.other(target.getUniqueId()));
        if (opponent != null) {
            opponent.sendMessage(ChatColor.YELLOW + player.getName() + " started spectating the match.");
        }

        // Hide from all online players
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.hidePlayer(player);
            }
        }
    }

    private void finishArenaEdit(Player player) {
        if (!isAdmin(player)) {
            player.sendMessage(prefix + ChatColor.RED + "No permission.");
            return;
        }
        String arena = arenaEditors.remove(player.getUniqueId());
        if (arena == null) {
            player.sendMessage(prefix + ChatColor.RED + "You are not editing an arena.");
            return;
        }
        player.sendMessage(prefix + ChatColor.GREEN + "Finished editing " + arena + ".");
        returnToSpawn(player, false);
    }

    private boolean isAdmin(Player player) {
        return player != null && player.isOp();
    }

    private void handleKitAdminCommand(Player player, String[] args) {
        if (!isAdmin(player)) {
            player.sendMessage(prefix + ChatColor.RED + "No permission.");
            return;
        }
        if (args.length < 2 || !(args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("save"))) {
            player.sendMessage(prefix + ChatColor.YELLOW + "Use /fp kit create <name>, /fp kit edit <name>, or /fp kit save <name>.");
            return;
        }
        String kitName = args[1];
        String key = normalize(kitName);
        if (args[0].equalsIgnoreCase("edit")) {
            KitData kit = kits.get(key);
            if (kit == null) {
                message(player, "no-kit");
                return;
            }
            giveKit(player, kit);
            player.sendMessage(prefix + ChatColor.GREEN + "Loaded kit " + kit.name + " into your inventory. Use /fp kit save " + kit.name + " when done.");
            return;
        }
        saveKitFromPlayer(player, kitName);
        reloadPractice();
        player.sendMessage(prefix + ChatColor.GREEN + "Saved kit " + kitName + " from your current inventory.");
    }

    private void saveKitFromPlayer(Player player, String kitName) {
        String path = "kits." + kitName;
        frostKits.set(path + ".displayName", kitName);
        frostKits.set(path + ".icon", player.getItemInHand() == null || player.getItemInHand().getType() == Material.AIR
                ? new ItemStack(Material.DIAMOND_SWORD)
                : player.getItemInHand().clone());
        frostKits.set(path + ".contents", Arrays.asList(cloneItems(player.getInventory().getContents(), 36)));
        frostKits.set(path + ".armor", Arrays.asList(cloneItems(player.getInventory().getArmorContents(), 4)));
        frostKits.set(path + ".unrankedPos", frostKits.getInt(path + ".unrankedPos", kits.size()));
        frostKits.set(path + ".rankedPos", frostKits.getInt(path + ".rankedPos", -1));
        frostKits.set(path + ".editorPos", frostKits.getInt(path + ".editorPos", -1));
        frostKits.set(path + ".spawnFfaPos", frostKits.getInt(path + ".spawnFfaPos", -1));
        frostKits.set(path + ".arenaWhitelist", frostKits.getStringList(path + ".arenaWhitelist"));
        frostKits.set(path + ".ranked", frostKits.getBoolean(path + ".ranked", false));
        frostKits.set(path + ".boxing", frostKits.getBoolean(path + ".boxing", normalize(kitName).contains("boxing")));
        frostKits.set(path + ".sumo", frostKits.getBoolean(path + ".sumo", normalize(kitName).contains("sumo")));
        frostKits.set(path + ".bedWars", frostKits.getBoolean(path + ".bedWars", false));
        saveYaml(frostKits, new File(getDataFolder(), "kits.yml"), "kits.yml");
    }

    private void handleArenaAdminCommand(Player player, String[] args) {
        if (!isAdmin(player)) {
            player.sendMessage(prefix + ChatColor.RED + "No permission.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(prefix + ChatColor.YELLOW + "Use /fp arena create <name>, /fp arena edit <name>, or /fp arena setspawn <name> <first|second>.");
            return;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        String arenaName = args[1];
        if (action.equals("create")) {
            String path = "arenas." + arenaName;
            String location = serializeLocation(player.getLocation());
            frostArenas.set(path + ".first", location);
            frostArenas.set(path + ".second", location);
            saveYaml(frostArenas, new File(getDataFolder(), "arenas.yml"), "arenas.yml");
            reloadPractice();
            player.sendMessage(prefix + ChatColor.GREEN + "Created arena " + arenaName + ". Move and use /fp arena setspawn " + arenaName + " second.");
            return;
        }
        if (action.equals("edit")) {
            ArenaSpawns arena = arenaByName(arenaName);
            if (arena == null) {
                player.sendMessage(prefix + ChatColor.RED + "Unknown arena.");
                return;
            }
            arenaEditors.put(player.getUniqueId(), arena.baseName);
            pluginTeleport(player, arena.first);
            player.sendMessage(prefix + ChatColor.GREEN + "Editing arena " + arena.baseName + ". Change blocks freely, then set spawns if needed.");
            return;
        }
        if (action.equals("setspawn") && args.length >= 3) {
            String spawnKey = args[2].equalsIgnoreCase("second") || args[2].equalsIgnoreCase("2") ? "second" : "first";
            frostArenas.set("arenas." + arenaName + "." + spawnKey, serializeLocation(player.getLocation()));
            saveYaml(frostArenas, new File(getDataFolder(), "arenas.yml"), "arenas.yml");
            reloadPractice();
            player.sendMessage(prefix + ChatColor.GREEN + "Saved " + spawnKey + " spawn for arena " + arenaName + ".");
            return;
        }
        player.sendMessage(prefix + ChatColor.YELLOW + "Use /fp arena create <name>, /fp arena edit <name>, or /fp arena setspawn <name> <first|second>.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && command.getName().equalsIgnoreCase("queue")) {
            return startsWith(args[0], kitNames());
        }
        if (args.length == 1 && command.getName().equalsIgnoreCase("duel")) {
            List<String> values = onlinePlayerNames();
            values.add("accept");
            return startsWith(args[0], values);
        }
        if (args.length == 2 && command.getName().equalsIgnoreCase("duel")) {
            if (args[0].equalsIgnoreCase("accept")) {
                return startsWith(args[1], onlinePlayerNames());
            }
            return startsWith(args[1], kitNames());
        }
        if (args.length == 1 && command.getName().equalsIgnoreCase("party")) {
            return startsWith(args[0], Arrays.asList("create", "open", "close", "public", "private", "invite", "accept", "join", "leave", "disband", "info"));
        }
        if (args.length == 1 && command.getName().equalsIgnoreCase("event")) {
            return startsWith(args[0], Arrays.asList("host", "join", "start", "forcestart", "stop", "end", "leave", "spectate", "list", "info"));
        }
        if (args.length == 1 && command.getName().equalsIgnoreCase("spectate")) {
            return startsWith(args[0], onlinePlayerNames());
        }
        if (args.length == 2 && command.getName().equalsIgnoreCase("party") && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("join"))) {
            return startsWith(args[1], onlinePlayerNames());
        }
        if (args.length == 2 && command.getName().equalsIgnoreCase("event") && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("spectate") || args[0].equalsIgnoreCase("info"))) {
            return startsWith(args[1], onlinePlayerNames());
        }
        if ((command.getName().equalsIgnoreCase("practice") || command.getName().equalsIgnoreCase("forgifiedpractice")) && args.length > 1
                && Arrays.asList("reload", "setlobby", "finish", "kit", "arena", "lobby").contains(args[0].toLowerCase(Locale.ROOT))
                && (!(sender instanceof Player) || !isAdmin((Player) sender))) {
            return Collections.emptyList();
        }
        if (args.length == 2 && (command.getName().equalsIgnoreCase("practice") || command.getName().equalsIgnoreCase("forgifiedpractice")) && args[0].equalsIgnoreCase("lobby")) {
            return startsWith(args[1], Arrays.asList("edit", "finish"));
        }
        if ((command.getName().equalsIgnoreCase("practice") || command.getName().equalsIgnoreCase("forgifiedpractice")) && args.length == 1) {
            List<String> values = new ArrayList<>(Arrays.asList("queue", "duel", "party", "settings", "leave"));
            if (sender instanceof Player && isAdmin((Player) sender)) {
                values.addAll(Arrays.asList("reload", "setlobby", "finish", "kit", "arena", "lobby"));
            }
            values.addAll(kitNames());
            return startsWith(args[0], values);
        }
        if (args.length == 2 && (command.getName().equalsIgnoreCase("practice") || command.getName().equalsIgnoreCase("forgifiedpractice")) && args[0].equalsIgnoreCase("kit")) {
            return startsWith(args[1], Arrays.asList("create", "edit", "save"));
        }
        if (args.length == 3 && (command.getName().equalsIgnoreCase("practice") || command.getName().equalsIgnoreCase("forgifiedpractice")) && args[0].equalsIgnoreCase("kit")
                && (args[1].equalsIgnoreCase("edit") || args[1].equalsIgnoreCase("save"))) {
            return startsWith(args[2], kitNames());
        }
        if (args.length == 2 && (command.getName().equalsIgnoreCase("practice") || command.getName().equalsIgnoreCase("forgifiedpractice")) && args[0].equalsIgnoreCase("arena")) {
            return startsWith(args[1], Arrays.asList("create", "edit", "setspawn"));
        }
        if (args.length == 3 && (command.getName().equalsIgnoreCase("practice") || command.getName().equalsIgnoreCase("forgifiedpractice")) && args[0].equalsIgnoreCase("arena")
                && (args[1].equalsIgnoreCase("edit") || args[1].equalsIgnoreCase("setspawn"))) {
            return startsWith(args[2], arenaNames());
        }
        if (args.length == 4 && (command.getName().equalsIgnoreCase("practice") || command.getName().equalsIgnoreCase("forgifiedpractice")) && args[0].equalsIgnoreCase("arena")
                && args[1].equalsIgnoreCase("setspawn")) {
            return startsWith(args[3], Arrays.asList("first", "second"));
        }
        if (args.length == 2 && (command.getName().equalsIgnoreCase("practice") || command.getName().equalsIgnoreCase("forgifiedpractice")) && args[0].equalsIgnoreCase("queue")) {
            return startsWith(args[1], kitNames());
        }
        return Collections.emptyList();
    }

    private void openQueueMenu(Player player, String type) {
        String normalizedType = normalizeQueueType(type);
        String menuPath = "ranked".equals(normalizedType) ? "QUEUE-INVENTORY.RANKED" : "QUEUE-INVENTORY.UNRANKED";
        int rows = frostMenus.getInt("QUEUE-INVENTORY." + normalizedType.toUpperCase(Locale.ROOT) + "-SIZE", 0);
        int size = rows > 0 ? Math.min(54, Math.max(9, rows * 9)) : Math.min(54, Math.max(9, ((kits.size() + 8) / 9) * 9));
        String title = color(frostMenus.getString(menuPath + ".TITLE", MENU_TITLE));
        Inventory inventory = Bukkit.createInventory(null, size, title);
        Set<Integer> occupied = new HashSet<>();
        int fallbackSlot = 0;
        for (KitData kit : kits.values()) {
            if ("ranked".equals(normalizedType) && !kit.ranked) {
                continue;
            }
            int slot = "ranked".equals(normalizedType) ? kit.rankedPos : kit.unrankedPos;
            if (slot < 0 || slot >= size || occupied.contains(slot)) {
                while (fallbackSlot < size && occupied.contains(fallbackSlot)) {
                    fallbackSlot++;
                }
                if (fallbackSlot >= size) {
                    break;
                }
                slot = fallbackSlot;
            }
            ItemStack icon = menuIcon(kit.icon, Material.DIAMOND_SWORD);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(frostMenus.getString(menuPath + ".NAME", "&6&l<kit_name>").replace("<kit_name>", kit.name)));
                List<String> lore = new ArrayList<>();
                for (String line : frostMenus.getStringList(menuPath + ".LORE")) {
                    if (line.contains("<top-3>")) {
                        continue;
                    }
                    lore.add(color(line
                            .replace("<fighting_unranked>", String.valueOf(playingCount("unranked", kit.name)))
                            .replace("<queueing_unranked>", String.valueOf(queueSize("unranked", normalize(kit.name))))
                            .replace("<fighting_ranked>", String.valueOf(playingCount("ranked", kit.name)))
                            .replace("<queueing_ranked>", String.valueOf(queueSize("ranked", normalize(kit.name))))
                            .replace("<kit_name>", kit.name)));
                }
                if (lore.isEmpty()) {
                    lore.add(ChatColor.GOLD + "Click to play!");
                }
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot, icon);
            occupied.add(slot);
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onQueueMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || event.getInventory() == null) {
            return;
        }
        if (SETTINGS_TITLE.equals(event.getInventory().getTitle()) || color(frostMenus.getString("SETTINGS-INVENTORY.TITLE", "")).equals(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleSettingsClick((Player) event.getWhoClicked(), event.getSlot());
            return;
        }
        if (color(frostMenus.getString("QUEUES.TITLE", "")).equals(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleQueuesMenuClick((Player) event.getWhoClicked(), event.getSlot());
            return;
        }
        if (isDuelKitMenu(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleDuelKitClick((Player) event.getWhoClicked(), event.getCurrentItem());
            return;
        }
        if (isDuelMapMenu(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleDuelMapClick((Player) event.getWhoClicked(), event.getCurrentItem());
            return;
        }
        if (isPartyKitMenu(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handlePartyKitClick((Player) event.getWhoClicked(), event.getCurrentItem());
            return;
        }
        if (isPartyMapMenu(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handlePartyMapClick((Player) event.getWhoClicked(), event.getCurrentItem());
            return;
        }
        // ── New event/party menus – check BEFORE any catch-all menu checks ──
        if (event.getInventory().getTitle().equals(EVENTS_LOBBY_TITLE)) {
            event.setCancelled(true);
            handleEventsLobbyClick((Player) event.getWhoClicked(), event.getSlot());
            return;
        }
        if (isHostEventMenu(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleHostEventClick((Player) event.getWhoClicked(), event);
            return;
        }
        if (isHostEventMapMenu(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleHostEventMapClick((Player) event.getWhoClicked(), event.getCurrentItem());
            return;
        }
        if (event.getInventory().getTitle().equals(HOST_KIT_TITLE)) {
            event.setCancelled(true);
            handleHostEventKitClick((Player) event.getWhoClicked(), event.getCurrentItem());
            return;
        }
        if (event.getInventory().getTitle().equals(PUBLIC_PARTY_TITLE)) {
            event.setCancelled(true);
            handlePublicPartyClick((Player) event.getWhoClicked(), event.getSlot());
            return;
        }
        if (event.getInventory().getTitle().equals(PUBLIC_LIST_TITLE)) {
            event.setCancelled(true);
            handlePublicPartyListClick((Player) event.getWhoClicked(), event.getCurrentItem());
            return;
        }
        if (isKitEditorMenu(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleKitEditorClick((Player) event.getWhoClicked(), event.getCurrentItem());
            return;
        }
        if (isLayoutEditor(event.getInventory().getTitle())) {
            handleLayoutEditorClick(event);
            return;
        }
        if (EVENTS_TITLE.equals(event.getInventory().getTitle()) || color(frostMenus.getString("EVENTS-INVENTORY.TITLE", "")).equals(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleEventMenuClick((Player) event.getWhoClicked(), event.getSlot());
            return;
        }
        if (isOtherPartiesMenu(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleOtherPartyClick((Player) event.getWhoClicked(), event.getCurrentItem());
            return;
        }
        if (isSettingsMenu(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleSettingsClick((Player) event.getWhoClicked(), event.getSlot());
            return;
        }
        if (isUtilityMenu(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleLeaderboardClick((Player) event.getWhoClicked(), event.getCurrentItem());
            return;
        }
        if (!isQueueMenu(event.getInventory().getTitle())) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        player.closeInventory();
        queue(player, ChatColor.stripColor(item.getItemMeta().getDisplayName()), isRankedQueueMenu(event.getInventory().getTitle()) ? "ranked" : "unranked");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player && isLayoutEditor(event.getInventory().getTitle())) {
            layoutEditorKits.remove(event.getPlayer().getUniqueId());
        }
    }

    private boolean isQueueMenu(String title) {
        return MENU_TITLE.equals(title)
                || color(frostMenus.getString("QUEUE-INVENTORY.UNRANKED.TITLE", "")).equals(title)
                || color(frostMenus.getString("QUEUE-INVENTORY.RANKED.TITLE", "")).equals(title);
    }

    private void openDuelKitMenu(Player player, Player target) {
        duelMenuTargets.put(player.getUniqueId(), target.getUniqueId());
        duelMenuKits.remove(player.getUniqueId());
        int rows = frostMenus.getInt("DUEL-INVENTORY.SIZE", frostMenus.getInt("QUEUE-INVENTORY.UNRANKED-SIZE", 5));
        int size = Math.min(54, Math.max(9, rows * 9));
        Inventory inventory = Bukkit.createInventory(null, size, duelKitTitle());
        fillMenuPlaceholders(inventory);
        Set<Integer> occupied = new HashSet<>();
        int fallbackSlot = 0;
        for (KitData kit : kits.values()) {
            int slot = kit.unrankedPos;
            if (slot < 0 || slot >= inventory.getSize() || occupied.contains(slot)) {
                while (fallbackSlot < inventory.getSize() && occupied.contains(fallbackSlot)) {
                    fallbackSlot++;
                }
                if (fallbackSlot >= inventory.getSize()) {
                    break;
                }
                slot = fallbackSlot;
            }
            ItemStack icon = menuIcon(kit.icon, Material.DIAMOND_SWORD);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(frostMenus.getString("DUEL-INVENTORY.KIT.NAME", "&6&l<kit_name>")
                        .replace("<kit_name>", kit.name)
                        .replace("<opponent>", target.getName())
                        .replace("<opponent_name>", target.getName())));
                meta.setLore(colorList(replaceLines(frostMenus.getStringList("DUEL-INVENTORY.KIT.LORE"),
                        "<kit_name>", kit.name,
                        "<opponent>", target.getName(),
                        "<opponent_name>", target.getName())));
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot, icon);
            occupied.add(slot);
        }
        player.openInventory(inventory);
    }

    private void handleDuelKitClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) {
            return;
        }
        KitData kit = kits.get(normalize(item.getItemMeta().getDisplayName()));
        if (kit == null) {
            return;
        }
        UUID targetId = duelMenuTargets.get(player.getUniqueId());
        Player target = targetId == null ? null : Bukkit.getPlayer(targetId);
        if (target == null) {
            player.closeInventory();
            player.sendMessage(prefix + ChatColor.RED + "That duel target is gone.");
            return;
        }
        duelMenuKits.put(player.getUniqueId(), kit);
        openDuelMapMenu(player, kit);
    }

    private void openDuelMapMenu(Player player, KitData kit) {
        List<ArenaSpawns> choices = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ArenaSpawns arena : arenas) {
            if (!kit.canUseArena(arena) || !seen.add(arena.baseName.toLowerCase(Locale.ROOT))) {
                continue;
            }
            choices.add(arena);
        }
        int rows = frostMenus.getInt("DUEL-INVENTORY.MAP-SIZE", frostMenus.getInt("QUEUE-INVENTORY.UNRANKED-SIZE", 5));
        int size = Math.min(54, Math.max(9, rows * 9));
        Inventory inventory = Bukkit.createInventory(null, size, duelMapTitle());
        fillMenuPlaceholders(inventory);
        int slot = 0;
        for (ArenaSpawns arena : choices) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, simpleItem(Material.EMPTY_MAP, (short) 0,
                    color(frostMenus.getString("DUEL-INVENTORY.MAP.NAME", "&6&l<arena_name>")
                            .replace("<arena_name>", arena.baseName)
                            .replace("<kit_name>", kit.name)),
                    colorList(replaceLines(frostMenus.getStringList("DUEL-INVENTORY.MAP.LORE"),
                            "<arena_name>", arena.baseName,
                            "<kit_name>", kit.name))));
        }
        player.openInventory(inventory);
    }

    private void handleDuelMapClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) {
            return;
        }
        if (ChatColor.stripColor(item.getItemMeta().getDisplayName()).trim().isEmpty()) {
            return;
        }
        UUID targetId = duelMenuTargets.remove(player.getUniqueId());
        KitData kit = duelMenuKits.remove(player.getUniqueId());
        Player target = targetId == null ? null : Bukkit.getPlayer(targetId);
        ArenaSpawns arena = arenaByName(ChatColor.stripColor(item.getItemMeta().getDisplayName()));
        player.closeInventory();
        if (target == null || kit == null || arena == null) {
            player.sendMessage(prefix + ChatColor.RED + "That duel selection is gone.");
            return;
        }
        sendDuelRequest(player, target, kit, arena);
    }

    private boolean isRankedQueueMenu(String title) {
        return color(frostMenus.getString("QUEUE-INVENTORY.RANKED.TITLE", "")).equals(title);
    }

    private boolean isSettingsMenu(String title) {
        return (ChatColor.GOLD + "" + ChatColor.BOLD + "Settings").equals(title);
    }

    private boolean isUtilityMenu(String title) {
        return LEADERBOARDS_TITLE.equals(title)
                || (ChatColor.GOLD + "" + ChatColor.BOLD + "Your Stats").equals(title)
                || (ChatColor.GOLD + "" + ChatColor.BOLD + "Ranked Leaderboards").equals(title)
                || (ChatColor.GOLD + "" + ChatColor.BOLD + "Unranked (All Time)").equals(title)
                || (ChatColor.GOLD + "" + ChatColor.BOLD + "Win Streak").equals(title);
    }

    private int queueSize(String type, String kit) {
        Queue<UUID> queue = queues.get(queueKey(type, kit));
        return queue == null ? 0 : queue.size();
    }

    private int playingCount(String type, String kit) {
        int count = 0;
        for (Match match : new HashSet<>(matches.values())) {
            if (match.type.equalsIgnoreCase(type) && normalize(match.kit.name).equalsIgnoreCase(normalize(kit))) {
                count += 2;
            }
        }
        return count;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerStats stats = stats(player.getUniqueId());
        if (stats.name == null || !stats.name.equals(player.getName())) {
            stats.name = player.getName();
            writeStats(player.getUniqueId(), stats);
            saveStats();
        }
        resetPlayer(player);
        returnToSpawn(player, false);
        spawnProtectionUntil.remove(player.getUniqueId());
        primedTntOwners.entrySet().removeIf(entry -> entry.getValue().equals(player.getUniqueId()));
        // Hide currently spectating or respawning players from the joining player
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (spectatingMatch.containsKey(online.getUniqueId()) || respawningPlayers.contains(online.getUniqueId())) {
                player.hidePlayer(online);
            }
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline() || matches.containsKey(player.getUniqueId())) {
                return;
            }
            giveStateHotbar(player);
            updateLobbyScoreboard(player);
            updateLuckPermsPrefix(player);
            for (UUID friendId : stats.friends) {
                Player friend = Bukkit.getPlayer(friendId);
                if (friend != null) {
                    friend.sendMessage(ChatColor.YELLOW + "[Friend] " + ChatColor.GREEN + player.getName() + " is now online!");
                }
            }
        }, 10L);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldProtectionInteract(PlayerInteractEvent event) {
        if (!isProtectedLobbyPlayer(event.getPlayer())) {
            return;
        }
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null && isCropTrampleBlock(event.getClickedBlock().getType())) {
            event.setCancelled(true);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && isProtectedOpenable(event.getClickedBlock().getType())) {
            event.setCancelled(true);
        }
    }

    private boolean isProtectedLobbyPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        return !matches.containsKey(uuid)
                && !activeEvents.containsKey(uuid)
                && !spectatingMatch.containsKey(uuid)
                && !arenaEditors.containsKey(uuid)
                && !spawnEditors.contains(uuid)
                && !Boolean.TRUE.equals(fastbridgeAssignedState(player));
    }

    private boolean isCropTrampleBlock(Material material) {
        return material == Material.SOIL || material == Material.CROPS;
    }

    private boolean isProtectedOpenable(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.contains("DOOR")
                || name.contains("TRAP_DOOR")
                || name.contains("FENCE_GATE")
                || name.equals("CHEST")
                || name.equals("TRAPPED_CHEST")
                || name.equals("ENDER_CHEST")
                || name.equals("FURNACE")
                || name.equals("BURNING_FURNACE")
                || name.equals("WORKBENCH")
                || name.equals("ENCHANTMENT_TABLE")
                || name.equals("ANVIL")
                || name.equals("BREWING_STAND")
                || name.equals("DISPENSER")
                || name.equals("DROPPER")
                || name.equals("HOPPER");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHotbarInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();

        ItemStack hand = player.getItemInHand();
        if (hand != null && hand.getType() == Material.PAPER && hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
            if (hand.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Play Again")) {
                event.setCancelled(true);
                Match lastMatch = lastPlayedMatch.remove(player.getUniqueId());
                if (lastMatch != null) {
                    returnToSpawn(player, false);
                    Bukkit.getScheduler().runTask(this, () -> {
                        queue(player, lastMatch.kit.name, lastMatch.type);
                    });
                }
                return;
            }
        }

        if (matches.containsKey(player.getUniqueId())) {
            if (respawningPlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            if (player.getItemInHand() != null && player.getItemInHand().getType() == Material.FIREBALL) {
                launchPracticeFireball(player, event);
            }
            return;
        }

        if (spectatingMatch.containsKey(player.getUniqueId())) {
            ItemStack stack = player.getItemInHand();
            if (stack != null && stack.getType() == Material.COMPASS) {
                openSpectatorTrackerMenu(player);
                event.setCancelled(true);
                return;
            }
            if (stack != null && stack.getType() == Material.INK_SACK) {
                spectatingMatch.remove(player.getUniqueId());
                returnToSpawn(player, true);
                player.sendMessage(ChatColor.GREEN + "Stopped spectating.");
                event.setCancelled(true);
                return;
            }
        }

        if (activeEvents.containsKey(player.getUniqueId())) {
            ItemStack stack = player.getItemInHand();
            if (stack != null && stack.getType() == Material.INK_SACK && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
                leaveEvent(player, false);
                returnToSpawn(player, true);
                event.setCancelled(true);
                return;
            }
        }

        HotbarItem item = findClickedHotbarItem(player.getItemInHand());
        if (item == null) {
            return;
        }
        event.setCancelled(true);
        runHotbarAction(player, item);
    }

    private HotbarItem findClickedHotbarItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta() || stack.getItemMeta().getDisplayName() == null) {
            return null;
        }
        String name = ChatColor.stripColor(stack.getItemMeta().getDisplayName());
        for (List<HotbarItem> items : hotbars.values()) {
            for (HotbarItem item : items) {
                if (ChatColor.stripColor(item.name).equals(name)) {
                    return item;
                }
            }
        }
        return null;
    }

    private void launchPracticeFireball(Player player, PlayerInteractEvent event) {
        Match match = matches.get(player.getUniqueId());
        if (match == null || !match.started || match.ending || !fireballThrowEnabled) {
            return;
        }
        long now = System.currentTimeMillis();
        long lastLaunch = fireballCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastLaunch < 300L) {
            event.setCancelled(true);
            return;
        }
        fireballCooldown.put(player.getUniqueId(), now);
        event.setCancelled(true);
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getAmount() <= 0) {
            return;
        }
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setShooter(player);
        fireball.setIsIncendiary(false);
        fireball.setYield(2.0F);
        fireball.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(fireballSpeed));
        if (hand.getAmount() <= 1) {
            player.setItemInHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
            player.setItemInHand(hand);
        }
        player.updateInventory();
    }

    private void runHotbarAction(Player player, HotbarItem item) {
        if (item.command != null && !item.command.trim().isEmpty()) {
            player.performCommand(item.command.startsWith("/") ? item.command.substring(1) : item.command);
            return;
        }
        String action = item.action.toUpperCase(Locale.ROOT);
        if (action.equals("QUEUES_MENU")) {
            openQueuesMenu(player);
        } else if (action.equals("JOIN_UNRANKED") || action.equals("DUEL_MENU")) {
            openQueueMenu(player, "unranked");
        } else if (action.equals("JOIN_RANKED")) {
            openQueueMenu(player, "ranked");
        } else if (action.equals("JOIN_PREMIUM")) {
            openQueueMenu(player, "premium");
        } else if (action.equals("CREATE_PARTY")) {
            handlePartyCommand(player, new String[]{"create"});
            giveStateHotbar(player);
        } else if (action.equals("PARTY_INFO")) {
            showPartyInfo(player);
        } else if (action.equals("PARTY_LEAVE")) {
            handlePartyCommand(player, new String[]{"leave"});
            giveStateHotbar(player);
        } else if (action.equals("LEAVE_QUEUE")) {
            leaveQueue(player, true);
            giveStateHotbar(player);
        } else if (action.equals("LEAVE_EVENT") || action.equals("LEAVE_SPECTATOR")) {
            leaveEvent(player, true);
            returnToSpawn(player, false);
        } else if (action.equals("SETTINGS_MENU")) {
            openSettingsMenu(player);
        } else if (action.equals("LEADERBOARDS_MENU")) {
            refreshLeaderboards();
            openLeaderboardsMenu(player);
        } else if (action.equals("OPEN_PUBLIC_PARTIES")) {
            openPublicPartyListMenu(player);
        } else if (action.equals("EVENTS_MENU")) {
            // Repurposed: show public parties list (events are disabled for now)
            openPublicPartyListMenu(player);
        } else if (action.equals("PARTY_EVENTS")) {
            openPartyKitMenu(player, "event", null);
        } else if (action.equals("OTHER_PARTIES")) {
            openPartyMenu(player);
        } else if (action.equals("EDITOR_MENU")) {
            openKitEditorMenu(player);
        } else if (action.equals("JOIN_FFA")) {
            joinFfaSpawn(player);
        } else if (action.equals("OPEN_CURRENT_MATCHES")) {
            openCurrentMatchesMenu(player);
        } else if (action.equals("OPEN_STATS")) {
            openStatsMenu(player);
        } else {
            player.sendMessage(prefix + ChatColor.YELLOW + "That Frost hotbar action is not implemented yet: " + action);
        }
    }

    private void openQueuesMenu(Player player) {
        int size = Math.min(54, Math.max(9, frostMenus.getInt("QUEUES.SIZE", 5) * 9));
        Inventory inventory = Bukkit.createInventory(null, size, color(frostMenus.getString("QUEUES.TITLE", "&6&lQueues")));
        ConfigurationSection types = frostMenus.getConfigurationSection("QUEUES.TYPES");
        if (types != null) {
            for (String key : types.getKeys(false)) {
                String path = "QUEUES.TYPES." + key;
                if (!frostMenus.getBoolean(path + ".ENABLED", true)) {
                    continue;
                }
                Material material = Material.matchMaterial(frostMenus.getString(path + ".ICON", "IRON_SWORD"));
                int slot = frostMenus.getInt(path + ".SLOT", 0);
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, simpleItem(material == null ? Material.IRON_SWORD : material, (short) frostMenus.getInt(path + ".DATA", 0),
                            color(frostMenus.getString(path + ".NAME", "&6&l" + key)), colorList(frostMenus.getStringList(path + ".LORE"))));
                }
            }
        }
        player.openInventory(inventory);
    }

    private void handleQueuesMenuClick(Player player, int slot) {
        ConfigurationSection types = frostMenus.getConfigurationSection("QUEUES.TYPES");
        if (types == null) {
            return;
        }
        for (String key : types.getKeys(false)) {
            String path = "QUEUES.TYPES." + key;
            if (frostMenus.getBoolean(path + ".ENABLED", true) && frostMenus.getInt(path + ".SLOT", -1) == slot) {
                player.closeInventory();
                openQueueMenu(player, key);
                return;
            }
        }
    }

    private void giveStateHotbar(Player player) {
        if (matches.containsKey(player.getUniqueId())) {
            return;
        }
        String section = queuedKit.containsKey(player.getUniqueId()) ? "IN-QUEUE" : parties.containsKey(player.getUniqueId()) ? "IN-PARTY" : "IN-SPAWN";
        List<HotbarItem> items = hotbars.get(section);
        if (items == null || items.isEmpty()) {
            items = hotbars.get("IN-SPAWN");
        }
        player.getInventory().clear();
        if (items != null) {
            for (HotbarItem item : items) {
                if (item.slot < 0 || item.slot > 8) {
                    continue;
                }
                player.getInventory().setItem(item.slot, item.toItemStack());
            }
        }
        player.updateInventory();
    }

    private void openSettingsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.GOLD + "" + ChatColor.BOLD + "Settings");
        PlayerSettings s = settings(player);

        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta gm = glass.getItemMeta(); gm.setDisplayName(" "); glass.setItemMeta(gm);
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        // Clean layout: keep icons 1 slot away from the left/right edges
        // Restore layout and move Ping clock to slot 21 (instead of 22)
        inv.setItem(10, globalChatItem(s.globalChatMode));
        inv.setItem(11, menuItem(Material.BOOK_AND_QUILL, (short) 0, "Friend Requests", s.friendRequests));
        inv.setItem(12, menuItem(Material.EGG, (short) 0, "Duel Requests", s.duelRequests));
        inv.setItem(13, menuItem(Material.ROTTEN_FLESH, (short) 0, "Party Invites", s.partyRequests));
        inv.setItem(14, menuItem(Material.EYE_OF_ENDER, (short) 0, "Allow Spectators", s.allowSpectators));
        inv.setItem(15, menuItem(Material.REDSTONE, (short) 0, "Toggle Scoreboard", s.scoreboard));
        inv.setItem(16, menuItem(Material.DIAMOND_SWORD, (short) 0, "Show Division in Chat", s.divisionsOnNametag));

        inv.setItem(21, menuItem(Material.WATCH, (short) 0, "Show Ping on Scoreboard", s.pingOnScoreboard));
        inv.setItem(23, menuItem(Material.NAME_TAG, (short) 0, "Public Parties", s.publicPartiesEnabled));

        player.openInventory(inv);
    }

    private void handleSettingsClick(Player player, int slot) {
        PlayerSettings s = settings(player);
        if (slot == 10) s.globalChatMode = (s.globalChatMode + 1) % 3;
        else if (slot == 11) s.friendRequests = !s.friendRequests;
        else if (slot == 12) s.duelRequests = !s.duelRequests;
        else if (slot == 13) s.partyRequests = !s.partyRequests;
        else if (slot == 14) s.allowSpectators = !s.allowSpectators;
        else if (slot == 15) {
            s.scoreboard = !s.scoreboard;
            if (!s.scoreboard) resetScoreboard(player);
            else updateLobbyScoreboard(player);
        }
        else if (slot == 16) {
            s.divisionsOnNametag = !s.divisionsOnNametag;
            updateLuckPermsPrefix(player);
        }
        else if (slot == 21) s.pingOnScoreboard = !s.pingOnScoreboard;
        else if (slot == 23) togglePublicParty(player);
        else return;

        openSettingsMenu(player);
        playSound(player, "click");
    }

    private void togglePublicParty(Player player) {
        PlayerSettings s = settings(player);
        s.publicPartiesEnabled = !s.publicPartiesEnabled;

        Party myParty = parties.get(player.getUniqueId());
        if (myParty != null && myParty.leader.equals(player.getUniqueId())) {
            // If they already have a party, apply immediately.
            setPartyPublic(player, s.publicPartiesEnabled, true);
            return;
        }

        // No party yet (or not leader) -> store preference only.
        if (!s.publicPartiesEnabled) {
            // Safety: ensure they don't appear as public if they are not a valid leader anymore.
            publicParties.remove(player.getUniqueId());
            player.sendMessage(prefix + ChatColor.RED + "Public parties disabled.");
        } else {
            player.sendMessage(prefix + ChatColor.GREEN + "Public parties enabled. Create a party to appear in the public parties list.");
        }
    }

    private void openStatsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.GOLD + "" + ChatColor.BOLD + "Your Stats");
        PlayerStats stats = stats(player.getUniqueId());

        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta glassMeta = glass.getItemMeta(); glassMeta.setDisplayName(" "); glass.setItemMeta(glassMeta);
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        inv.setItem(4, simpleItem(Material.PAPER, (short) 0, ChatColor.GOLD + "" + ChatColor.BOLD + "Your Stats", Collections.emptyList()));
        
        List<String> globalLore = new ArrayList<>();
        globalLore.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "Unranked");
        globalLore.add(ChatColor.WHITE + "  Wins: " + ChatColor.LIGHT_PURPLE + stats.wins);
        globalLore.add(ChatColor.WHITE + "  Best Streak: " + ChatColor.YELLOW + stats.bestWinstreak);
        globalLore.add("");
        globalLore.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "Ranked");
        globalLore.add(ChatColor.WHITE + "  Wins: " + ChatColor.LIGHT_PURPLE + stats.wins);
        globalLore.add(ChatColor.WHITE + "  Losses: " + ChatColor.LIGHT_PURPLE + stats.losses);
        inv.setItem(13, simpleItem(Material.NETHER_STAR, (short) 0, ChatColor.YELLOW + "" + ChatColor.BOLD + "Global Stats", globalLore));

        int slot = 19;
        for (KitData kit : kits.values()) {
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
            KitStats ks = stats.kitStats(normalize(kit.name));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Elo: " + ChatColor.LIGHT_PURPLE + ks.elo);
            lore.add(ChatColor.WHITE + "Wins: " + ChatColor.LIGHT_PURPLE + ks.rankedWins);
            ItemStack icon = menuIcon(kit.icon, Material.IRON_SWORD);
            inv.setItem(slot++, simpleItem(icon.getType(), icon.getDurability(), ChatColor.YELLOW + "" + ChatColor.BOLD + kit.name, lore));
        }
        player.openInventory(inv);
    }

    private final Map<UUID, LeaderboardType> viewingLeaderboardType = new HashMap<>();

    private void openLeaderboardsMenu(Player player) {
        openLeaderboardsMenu(player, viewingLeaderboardType.getOrDefault(player.getUniqueId(), LeaderboardType.RANKED));
    }

    private void openLeaderboardsMenu(Player player, LeaderboardType type) {
        viewingLeaderboardType.put(player.getUniqueId(), type);
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.GOLD + "" + ChatColor.BOLD + type.getTitle());
        fillLeaderboardsInventory(inv, player, type);
        player.openInventory(inv);
    }

    private void fillLeaderboardsInventory(Inventory inv, Player viewer, LeaderboardType type) {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta glassMeta = glass.getItemMeta(); glassMeta.setDisplayName(" "); glass.setItemMeta(glassMeta);
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        List<String> typeLore = new ArrayList<>();
        for (LeaderboardType t : LeaderboardType.values()) {
            String prefix = (t == type) ? ChatColor.GREEN + "» " : ChatColor.GRAY + "  ";
            typeLore.add(prefix + t.getTitle());
        }
        typeLore.add("");
        typeLore.add(ChatColor.YELLOW + "Click to change!");
        inv.setItem(4, simpleItem(Material.ENDER_PORTAL_FRAME, (short) 0, ChatColor.GOLD + "" + ChatColor.BOLD + "Display Type", typeLore));

        String globalKey = type == LeaderboardType.WIN_STREAK ? "GLOBAL_STREAK" : "GLOBAL_WINS";
        List<LeaderboardEntry> globalLb = cachedLeaderboards.getOrDefault(globalKey, Collections.emptyList());
        List<String> globalLore = new ArrayList<>();
        globalLore.add("");
        int rank = 1;
        for (LeaderboardEntry entry : globalLb) {
            ChatColor color = rank == 1 ? ChatColor.GOLD : rank == 2 ? ChatColor.GRAY : rank == 3 ? ChatColor.DARK_GRAY : ChatColor.YELLOW;
            String div = ChatColor.translateAlternateColorCodes('&', getDivisionForPlayer(entry.uuid));
            globalLore.add(color + "" + rank + ". " + div + " " + ChatColor.AQUA + entry.name + ChatColor.GRAY + " - " + ChatColor.YELLOW + entry.value);
            rank++;
        }
        appendViewerLeaderboardPlacement(globalLore, viewer, globalKey);
        inv.setItem(13, simpleItem(Material.NETHER_STAR, (short) 0, ChatColor.YELLOW + "" + ChatColor.BOLD + "Global Stats", globalLore));

        int slot = 19;
        for (KitData kit : kits.values()) {
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;

            String kitSuffix = type == LeaderboardType.RANKED ? "_RANKED" : "_UNRANKED_ALL_TIME";
            String key = normalize(kit.name) + kitSuffix;
            List<LeaderboardEntry> lb = cachedLeaderboards.getOrDefault(key, Collections.emptyList());
            List<String> lore = new ArrayList<>();
            lore.add("");
            int kRank = 1;
            for (LeaderboardEntry entry : lb) {
                ChatColor color = kRank == 1 ? ChatColor.GOLD : kRank == 2 ? ChatColor.GRAY : kRank == 3 ? ChatColor.DARK_GRAY : ChatColor.YELLOW;
                String div = ChatColor.translateAlternateColorCodes('&', getDivisionForPlayer(entry.uuid));
                lore.add(color + "" + kRank + ". " + div + " " + ChatColor.AQUA + entry.name + ChatColor.GRAY + " - " + ChatColor.YELLOW + entry.value);
                kRank++;
            }
            appendViewerLeaderboardPlacement(lore, viewer, key);
            
            ItemStack icon = menuIcon(kit.icon, Material.IRON_SWORD);
            inv.setItem(slot++, simpleItem(icon.getType(), icon.getDurability(), ChatColor.YELLOW + "" + ChatColor.BOLD + kit.name, lore));
        }
    }

    private void refreshOpenLeaderboardInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                org.bukkit.inventory.InventoryView view = player.getOpenInventory();
                if (view == null) continue;
                String title = ChatColor.stripColor(view.getTitle());
                LeaderboardType type = null;
                for (LeaderboardType t : LeaderboardType.values()) {
                    if (t.getTitle().equalsIgnoreCase(title)) {
                        type = t;
                        break;
                    }
                }
                if (type == null) continue;
                Inventory top = view.getTopInventory();
                if (top == null || top.getSize() != 54) continue;
                viewingLeaderboardType.put(player.getUniqueId(), type);
                fillLeaderboardsInventory(top, player, type);
                player.updateInventory();
            } catch (Exception ignored) {}
        }
    }

    private void handleLeaderboardClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (name.equals("Display Type")) {
            LeaderboardType current = viewingLeaderboardType.getOrDefault(player.getUniqueId(), LeaderboardType.RANKED);
            LeaderboardType[] values = LeaderboardType.values();
            LeaderboardType next = values[(current.ordinal() + 1) % values.length];
            openLeaderboardsMenu(player, next);
            playSound(player, "click");
        }
    }

    private void handleAirLeaderboardCommand(Player player, String[] args) {
        if (!isAdmin(player)) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length < 1) {
            player.sendMessage(prefix + ChatColor.YELLOW + "Use /fp leaderboard create <type>, delete <type>, purge [type], or purgehere [radius].");
            return;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        String rawType = args.length >= 2 ? joinArgs(args, 1) : null;
        String type = rawType == null ? null : normalizeAirLeaderboardType(rawType);

        if (action.equals("purgehere")) {
            double radius = 15.0D;
            if (args.length >= 2) {
                try {
                    radius = Double.parseDouble(args[1]);
                } catch (Exception ignored) {}
            }
            int removed = purgeNearbyLeaderboardArmorStands(player.getLocation(), radius);
            player.sendMessage(prefix + ChatColor.GREEN + "Purged " + removed + " nearby leaderboard armorstands (radius " + radius + ").");
            return;
        }

        if (action.equals("purge")) {
            // Purge removes old/orphaned armorstands at the configured leaderboard locations, then respawns them.
            int removed = purgeOrphanAirLeaderboards(type);
            refreshLeaderboards();
            if (type != null) {
                spawnAirLeaderboard(type);
                player.sendMessage(prefix + ChatColor.GREEN + "Purged leaderboard armorstands (" + removed + " removed) and respawned " + displayAirLeaderboardType(type) + ".");
            } else {
                spawnAirLeaderboards();
                player.sendMessage(prefix + ChatColor.GREEN + "Purged leaderboard armorstands (" + removed + " removed) and respawned all leaderboards.");
            }
            return;
        }

        if (type == null) {
            player.sendMessage(prefix + ChatColor.RED + "Types: wins, winstreak, daily streak, elo.");
            return;
        }
        if (action.equals("create")) {
            String base = "leaderboards." + type + ".";
            airLeaderboardConfig.set(base + "type", type);
            airLeaderboardConfig.set(base + "location", serializeLocation(player.getLocation()));
            saveYaml(airLeaderboardConfig, airLeaderboardFile, "Airleaderboard.yml");
            refreshLeaderboards();
            spawnAirLeaderboard(type);
            player.sendMessage(prefix + ChatColor.GREEN + "Created " + displayAirLeaderboardType(type) + " leaderboard here.");
            return;
        }
        if (action.equals("delete")) {
            // Delete should remove both tracked + any orphaned armorstands at that location, then remove from config.
            int removed = purgeOrphanAirLeaderboard(type);
            airLeaderboardConfig.set("leaderboards." + type, null);
            saveYaml(airLeaderboardConfig, airLeaderboardFile, "Airleaderboard.yml");
            player.sendMessage(prefix + ChatColor.YELLOW + "Deleted " + displayAirLeaderboardType(type) + " leaderboard. (" + removed + " armorstands removed)");
            return;
        }
        player.sendMessage(prefix + ChatColor.YELLOW + "Use /fp leaderboard create <type>, delete <type>, or purge [type].");
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private String normalizeAirLeaderboardType(String raw) {
        String normalized = normalize(raw);
        if (normalized.equals("leaderboard") || normalized.equals("wins") || normalized.equals("win")) {
            return "wins";
        }
        if (normalized.equals("winstreak") || normalized.equals("streak")) {
            return "winstreak";
        }
        if (normalized.equals("daily") || normalized.equals("dailystreak")) {
            return "daily_streak";
        }
        if (normalized.equals("elo") || normalized.equals("ranked")) {
            return "elo";
        }
        return null;
    }

    private String displayAirLeaderboardType(String type) {
        if ("daily_streak".equals(type)) {
            return "Daily Streak";
        }
        if ("winstreak".equals(type)) {
            return "Winstreak";
        }
        if ("elo".equals(type)) {
            return "ELO";
        }
        return "Wins";
    }

    private void openEventsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, Math.max(9, frostMenus.getInt("EVENTS-INVENTORY.SIZE", 3) * 9), color(frostMenus.getString("EVENTS-INVENTORY.TITLE", EVENTS_TITLE)));
        ConfigurationSection events = frostMenus.getConfigurationSection("EVENTS-INVENTORY.EVENTS");
        if (events != null) {
            for (String event : events.getKeys(false)) {
                if (!frostMenus.getBoolean("EVENTS-INVENTORY.EVENTS." + event + ".SHOW", true)) {
                    continue;
                }
                int slot = frostMenus.getInt("EVENTS-INVENTORY.EVENTS." + event + ".SLOT", 0);
                Material material = Material.matchMaterial(frostMenus.getString("EVENTS-INVENTORY.EVENTS." + event + ".ICON", "NETHER_STAR"));
                inventory.setItem(slot, simpleItem(material == null ? Material.NETHER_STAR : material, (short) 0,
                        color(frostMenus.getString("EVENTS-INVENTORY.EVENTS." + event + ".NAME", "&6&l" + event)),
                        colorList(frostMenus.getStringList("EVENTS-INVENTORY.EVENTS." + event + ".LORE"))));
            }
        }
        player.openInventory(inventory);
    }

    private void handleEventMenuClick(Player player, int slot) {
        ConfigurationSection events = frostMenus.getConfigurationSection("EVENTS-INVENTORY.EVENTS");
        if (events == null) {
            return;
        }
        for (String event : events.getKeys(false)) {
            String path = "EVENTS-INVENTORY.EVENTS." + event;
            if (!frostMenus.getBoolean(path + ".SHOW", true) || frostMenus.getInt(path + ".SLOT", -1) != slot) {
                continue;
            }
            player.closeInventory();
            teleportToConfigLocation(player, event);
            return;
        }
    }

    private void teleportToConfigLocation(Player player, String event) {
        String normalized = canonicalEventKey(event);
        List<String> candidates = new ArrayList<>();
        if (normalized.equals("TNT_TAG")) {
            candidates.add("tntTagLocation");
            candidates.add("tntTagGameLocation");
        } else {
            String camel = normalized.toLowerCase(Locale.ROOT);
            String[] parts = camel.split("_");
            StringBuilder key = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                key.append(parts[i].substring(0, 1).toUpperCase(Locale.ROOT)).append(parts[i].substring(1));
            }
            candidates.add(key + "Location");
            candidates.add(key + "GameLocation");
        }
        for (String candidate : candidates) {
            Location location = parseLocation(frostConfig.getString(candidate), spawnLocation == null ? null : spawnLocation.getWorld());
            if (location != null) {
                resetPlayer(player);
                pluginTeleport(player, location);
                EventSession session = new EventSession(normalizeEventKey(event), player.getUniqueId(), System.currentTimeMillis(), 1, frostSettings.getInt("SETTINGS.GENERAL.MAXIMUM-PARTY-SIZE", 32));
                activeEvents.put(player.getUniqueId(), session);
                sendFrostLines(player, "MESSAGES.EVENT.JOINED", "<eventName>", displayEventName(session.name));
                updateEventScoreboard(player, session, false);
                return;
            }
        }
        player.sendMessage(prefix + ChatColor.RED + "No location for " + event + " was found in Frost/config.yml.");
    }

    private String canonicalEventKey(String event) {
        String normalized = event.toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        if (normalized.equals("BRAKETS")) {
            return "BRACKETS";
        }
        return normalized;
    }

    // ── Hostrank permission check ──────────────────────────────────────────────

    private boolean canHostEvent(Player player) {
        if (player.isOp()) return true;
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            LuckPerms lp = provider.getProvider();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                List<String> allowed = hostrankConfig.getStringList("allowed-ranks");
                Set<String> groups = new HashSet<>();
                user.getInheritedGroups(user.getQueryOptions()).forEach(g -> groups.add(g.getName().toLowerCase(Locale.ROOT)));
                for (String rank : allowed) {
                    if (groups.contains(rank.toLowerCase(Locale.ROOT))) return true;
                }
                return false;
            }
        }
        // Fallback: only OP
        return false;
    }

    private String noPermissionHostMessage() {
        String raw = hostrankConfig.getString("no-permission-message", "&cAvailable on VIP or higher");
        return color(raw);
    }

    // ── Events lobby menu (Iron Axe = Public Party, Diamond Axe = Host Event) ─

    private static final String EVENTS_LOBBY_TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Events";
    private static final String HOST_EVENT_TITLE   = ChatColor.GOLD + "" + ChatColor.BOLD + "Host Event Setup";
    private static final String HOST_KIT_TITLE     = ChatColor.GREEN + "" + ChatColor.BOLD + "Host Event Kit";
    private static final String PUBLIC_PARTY_TITLE = ChatColor.AQUA + "" + ChatColor.BOLD + "Public Party";
    private static final String PUBLIC_LIST_TITLE  = ChatColor.GOLD + "" + ChatColor.BOLD + "Public Parties";

    /** Called when the player clicks the hotbar "Events" item (replaces the old coming-soon). */
    private void openEventsLobbyMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, EVENTS_LOBBY_TITLE);

        // Fill border with glass
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta gm = glass.getItemMeta(); gm.setDisplayName(" "); glass.setItemMeta(gm);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Iron Axe – Public Party (slot 11)
        ItemStack ironAxe = new ItemStack(Material.IRON_AXE);
        ItemMeta iaMeta = ironAxe.getItemMeta();
        iaMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Public Party");
        List<String> iaLore = new ArrayList<>();
        iaLore.add(ChatColor.GRAY + "Make your party public or browse");
        iaLore.add(ChatColor.GRAY + "open parties to join!");
        iaMeta.setLore(iaLore);
        ironAxe.setItemMeta(iaMeta);
        inv.setItem(11, ironAxe);

        // Diamond Axe – Host Event (slot 15)
        ItemStack diamondAxe = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta daMeta = diamondAxe.getItemMeta();
        daMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Host Event");
        List<String> daLore = new ArrayList<>();
        daLore.add(ChatColor.GRAY + "Host a custom event for all");
        daLore.add(ChatColor.GRAY + "players on the server!");
        if (!canHostEvent(player)) {
            daLore.add("");
            daLore.add(ChatColor.RED + noPermissionHostMessage());
        }
        daMeta.setLore(daLore);
        diamondAxe.setItemMeta(daMeta);
        inv.setItem(15, diamondAxe);

        player.openInventory(inv);
    }

    private void handleEventsLobbyClick(Player player, int slot) {
        if (slot == 11) {
            player.closeInventory();
            openPublicPartyMenu(player);
        } else if (slot == 15) {
            player.closeInventory();
            if (!canHostEvent(player)) {
                player.sendMessage(noPermissionHostMessage());
                return;
            }
            openHostEventMenu(player);
        }
    }

    // ── Public Party menu ──────────────────────────────────────────────────────

    private void openPublicPartyMenu(Player player) {
        prunePublicParties();
        Inventory inv = Bukkit.createInventory(null, 27, PUBLIC_PARTY_TITLE);

        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta gm = glass.getItemMeta(); gm.setDisplayName(" "); glass.setItemMeta(gm);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        Party myParty = parties.get(player.getUniqueId());
        boolean isLeader = myParty != null && myParty.leader.equals(player.getUniqueId());
        boolean isPublic  = isLeader && publicParties.contains(player.getUniqueId());

        // Toggle public slot 11
        ItemStack toggle = new ItemStack(isPublic ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta tm = toggle.getItemMeta();
        if (isPublic) {
            tm.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Party is Public");
            List<String> tl = new ArrayList<>();
            tl.add(ChatColor.GRAY + "Click to make your party private.");
            tl.add(ChatColor.YELLOW + "Members: " + ChatColor.WHITE + myParty.members.size());
            tm.setLore(tl);
        } else {
            tm.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Party is Private");
            List<String> tl = new ArrayList<>();
            tl.add(ChatColor.GRAY + "Click to make your party public.");
            if (!isLeader) tl.add(ChatColor.RED + "You must be party leader.");
            tm.setLore(tl);
        }
        toggle.setItemMeta(tm);
        inv.setItem(11, toggle);

        // Browse public parties slot 15
        ItemStack browse = new ItemStack(Material.BOOK);
        ItemMeta bm = browse.getItemMeta();
        bm.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Browse Public Parties");
        List<String> bl = new ArrayList<>();
        bl.add(ChatColor.GRAY + "See all open parties and join!");
        bm.setLore(bl);
        browse.setItemMeta(bm);
        inv.setItem(15, browse);

        player.openInventory(inv);
    }

    private void handlePublicPartyClick(Player player, int slot) {
        if (slot == 11) {
            // Toggle public/private
            Party myParty = parties.get(player.getUniqueId());
            if (myParty == null) {
                // Auto-create a party first
                handlePartyCommand(player, new String[]{"create"});
                myParty = parties.get(player.getUniqueId());
                if (myParty == null) {
                    player.closeInventory();
                    return;
                }
            }
            if (!myParty.leader.equals(player.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "Only the party leader can toggle public status.");
                player.closeInventory();
                return;
            }
            setPartyPublic(player, !publicParties.contains(player.getUniqueId()), true);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(this, () -> openPublicPartyMenu(player), 1L);
        } else if (slot == 15) {
            player.closeInventory();
            openPublicPartyListMenu(player);
        }
    }

    private void openPublicPartyListMenu(Player player) {
        prunePublicParties();
        Inventory inv = Bukkit.createInventory(null, 54, PUBLIC_LIST_TITLE);

        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta gm = glass.getItemMeta(); gm.setDisplayName(" "); glass.setItemMeta(gm);
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);

        int slot = 0;
        for (UUID leaderId : publicParties) {
            if (slot >= 45) break;
            Player leader = Bukkit.getPlayer(leaderId);
            if (leader == null || !leader.isOnline()) continue;
            Party party = parties.get(leaderId);
            if (party == null) continue;

            ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwner(leader.getName());
            sm.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + leader.getName() + "'s Party");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Members: " + ChatColor.WHITE + party.members.size() + "/" +
                    frostSettings.getInt("SETTINGS.GENERAL.MAXIMUM-PARTY-SIZE", 32));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to join!");
            sm.setLore(lore);
            head.setItemMeta(sm);
            inv.setItem(slot++, head);
        }

        if (slot == 0) {
            ItemStack none = new ItemStack(Material.BARRIER);
            ItemMeta nm = none.getItemMeta();
            nm.setDisplayName(ChatColor.RED + "No public parties available.");
            none.setItemMeta(nm);
            inv.setItem(22, none);
        }

        player.openInventory(inv);
    }

    private void setPartyPublic(Player leader, boolean makePublic, boolean announce) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null || !party.leader.equals(leader.getUniqueId())) {
            leader.sendMessage(prefix + ChatColor.RED + "You must create a party and be its leader first.");
            return;
        }
        // Keep the Settings toggle in sync with actual party visibility.
        settings(leader).publicPartiesEnabled = makePublic;
        if (makePublic) {
            boolean changed = publicParties.add(leader.getUniqueId());
            leader.sendMessage(prefix + ChatColor.GREEN + "Your party is now " + ChatColor.BOLD + "public" + ChatColor.GREEN + "!");
            if (announce && changed) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.equals(leader)) continue;
                    online.sendMessage(color("&6[Party] &e" + leader.getName() + " &fhas opened a &6public party&f."));
                    sendSingleActionButton(online, "&a&l[JOIN PARTY]", "/party join " + leader.getName(), "&aClick to join " + leader.getName() + "'s public party.");
                }
            }
            return;
        }
        publicParties.remove(leader.getUniqueId());
        leader.sendMessage(prefix + ChatColor.RED + "Your party is now " + ChatColor.BOLD + "private" + ChatColor.RED + ".");
    }

    private void prunePublicParties() {
        Iterator<UUID> iterator = publicParties.iterator();
        while (iterator.hasNext()) {
            UUID leaderId = iterator.next();
            Player leader = Bukkit.getPlayer(leaderId);
            Party party = parties.get(leaderId);
            if (leader == null || !leader.isOnline() || party == null || !party.leader.equals(leaderId)) {
                iterator.remove();
            }
        }
    }

    private void handlePublicPartyListClick(Player player, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta() || clicked.getType() != Material.SKULL_ITEM) return;
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).replace("'s Party", "").trim();
        Player leader = Bukkit.getPlayer(name);
        if (leader == null) {
            player.sendMessage(prefix + ChatColor.RED + "That party leader is offline.");
            player.closeInventory();
            return;
        }
        player.closeInventory();
        handlePartyCommand(player, new String[]{"join", leader.getName()});
    }

    // ── Host Event menu ────────────────────────────────────────────────────────

    // Map to track pending event config and active hosted events
    private final Map<UUID, String> pendingEventKit = new HashMap<>();
    private final Map<UUID, Integer> pendingEventMaxSize = new HashMap<>();
    private final Map<UUID, Integer> pendingEventTeamSize = new HashMap<>();
    private final Map<UUID, String> pendingEventArenaName = new HashMap<>();
    private final Map<UUID, EventSession> hostedEvents = new HashMap<>();

    private String hostMenuTitle() {
        return color(frostMenus.getString("EVENT-SETTINGS-MENU.TITLE", HOST_EVENT_TITLE));
    }

    private boolean isHostEventMenu(String title) {
        return HOST_EVENT_TITLE.equals(title) || hostMenuTitle().equals(title);
    }

    private String hostEventMapTitle() {
        return color(frostMenus.getString("EVENT-SETTINGS-MENU.MAP-TITLE", "&6&lSelect Event Map"));
    }

    private boolean isHostEventMapMenu(String title) {
        return hostEventMapTitle().equals(title);
    }

    private int hostMenuSlot(String key, int fallback, int size) {
        int raw = frostMenus.getInt("EVENT-SETTINGS-MENU." + key + ".SLOT", fallback);
        return Math.max(0, Math.min(size - 1, raw));
    }

    private String defaultEventKit() {
        if (kits.isEmpty()) {
            return "None";
        }
        return kits.values().iterator().next().name;
    }

    private String defaultEventMap() {
        return "Random";
    }

    private String formatEventMap(String arenaName) {
        if (arenaName == null || arenaName.trim().isEmpty() || "Random".equalsIgnoreCase(arenaName)) {
            return "Random";
        }
        return arenaName;
    }

    private void ensurePendingEventConfig(Player player) {
        pendingEventKit.putIfAbsent(player.getUniqueId(), defaultEventKit());
        pendingEventMaxSize.putIfAbsent(player.getUniqueId(), 16);
        pendingEventTeamSize.putIfAbsent(player.getUniqueId(), 1);
        pendingEventArenaName.putIfAbsent(player.getUniqueId(), defaultEventMap());
    }

    private void openHostEventMenu(Player player) {
        try {
            if (frostMenus == null) {
                player.sendMessage(prefix + ChatColor.RED + "Menu configuration not loaded. Contact an admin.");
                getLogger().severe("frostMenus is null!");
                return;
            }

            ensurePendingEventConfig(player);
            EventSession hosted = hostedEvents.get(player.getUniqueId());
            int size = Math.max(36, Math.min(54, frostMenus.getInt("EVENT-SETTINGS-MENU.SIZE", 5) * 9));
            Inventory inv = Bukkit.createInventory(null, size, hostMenuTitle());

            ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
            ItemMeta gm = glass.getItemMeta();
            if (gm != null) {
                gm.setDisplayName(" ");
                glass.setItemMeta(gm);
            }
            for (int i = 0; i < size; i++) {
                inv.setItem(i, glass);
            }

            String chosenKit = hosted == null ? pendingEventKit.get(player.getUniqueId()) : hosted.kitName;
            int maxPlayers = hosted == null ? pendingEventMaxSize.get(player.getUniqueId()) : hosted.max;
            int teamSize = hosted == null ? pendingEventTeamSize.get(player.getUniqueId()) : hosted.teamSize;
            String chosenMap = hosted == null ? pendingEventArenaName.get(player.getUniqueId()) : hosted.arenaPool;
            String currentMap = formatEventMap(chosenMap);

            int kitSlot = hostMenuSlot("KIT-SELECTION", 10, size);
            int mapSlot = hostMenuSlot("MAP-SELECTION", 13, size);
            int teamSlot = hostMenuSlot("TEAM-SIZE-SELECTION", 16, size);
            int maxSlot = hostMenuSlot("MAX-PLAYERS-SELECTION", 22, size);
            int startSlot = hostMenuSlot("START", 31, size);
            int cancelSlot = hostMenuSlot("CANCEL", 33, size);

            inv.setItem(kitSlot, simpleItem(Material.BOOK, (short) 0,
                    color(frostMenus.getString("EVENT-SETTINGS-MENU.KIT-SELECTION.COLOR", "&6&l") + "Select Kit"),
                    resolveMenuLore("EVENT-SETTINGS-MENU.KIT-SELECTION.LORE", "<kit>", chosenKit == null ? "None" : chosenKit)));
            inv.setItem(mapSlot, simpleItem(Material.EMPTY_MAP, (short) 0,
                    color(frostMenus.getString("EVENT-SETTINGS-MENU.MAP-SELECTION.COLOR", "&6&l") + "Select Map"),
                    resolveMenuLore("EVENT-SETTINGS-MENU.MAP-SELECTION.LORE", "<map>", currentMap)));
            inv.setItem(teamSlot, simpleItem(Material.IRON_INGOT, (short) 0,
                    color(frostMenus.getString("EVENT-SETTINGS-MENU.TEAM-SIZE-SELECTION.COLOR", "&6&l") + "Team Size"),
                    resolveMenuLore("EVENT-SETTINGS-MENU.TEAM-SIZE-SELECTION.LORE", "<teamSize>", String.valueOf(teamSize))));
            inv.setItem(maxSlot, simpleItem(Material.REDSTONE, (short) 0,
                    color(frostMenus.getString("EVENT-SETTINGS-MENU.MAX-PLAYERS-SELECTION.COLOR", "&6&l") + "Max Players"),
                    resolveMenuLore("EVENT-SETTINGS-MENU.MAX-PLAYERS-SELECTION.LORE", "<maxPlayers>", String.valueOf(maxPlayers))));
            inv.setItem(startSlot, simpleItem(Material.EMERALD_BLOCK, (short) 0,
                    color(frostMenus.getString("EVENT-SETTINGS-MENU.START.NAME", "&a&l✓ Start Event")),
                    resolveMenuLore("EVENT-SETTINGS-MENU.START.LORE", "<kit>", chosenKit == null ? "None" : chosenKit,
                            "<map>", currentMap,
                            "<maxPlayers>", String.valueOf(maxPlayers),
                            "<teamSize>", String.valueOf(teamSize))));
            inv.setItem(cancelSlot, simpleItem(Material.BARRIER, (short) 0,
                    color(frostMenus.getString("EVENT-SETTINGS-MENU.CANCEL.NAME", "&c&l✕ Cancel")),
                    resolveMenuLore("EVENT-SETTINGS-MENU.CANCEL.LORE")));

            player.openInventory(inv);
        } catch (Exception e) {
            player.sendMessage(prefix + ChatColor.RED + "Error opening host menu: " + e.getMessage());
            getLogger().severe("Host event menu error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openHostEventMapMenu(Player player) {
        String selectedKit = pendingEventKit.getOrDefault(player.getUniqueId(), defaultEventKit());
        KitData kit = null;
        for (KitData candidate : kits.values()) {
            if (candidate.name.equalsIgnoreCase(selectedKit)) {
                kit = candidate;
                break;
            }
        }

        List<ArenaSpawns> choices = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ArenaSpawns arena : arenas) {
            if (kit != null && !kit.canUseArena(arena)) {
                continue;
            }
            if (seen.add(arena.baseName.toLowerCase(Locale.ROOT))) {
                choices.add(arena);
            }
        }
        if (choices.isEmpty()) {
            for (ArenaSpawns arena : arenas) {
                if (seen.add(arena.baseName.toLowerCase(Locale.ROOT))) {
                    choices.add(arena);
                }
            }
        }

        int size = Math.max(27, Math.min(54, ((choices.size() + 8) / 9) * 9));
        Inventory inventory = Bukkit.createInventory(null, size, hostEventMapTitle());
        fillMenuPlaceholders(inventory);

        int slot = 0;
        inventory.setItem(slot++, simpleItem(Material.MAP, (short) 0,
                color("&6&lRandom"),
                colorList(Arrays.asList("&fUses a random compatible map.", " ", "&6Click to select this map!"))));
        for (ArenaSpawns arena : choices) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, simpleItem(Material.EMPTY_MAP, (short) 0,
                    color("&6&l" + arena.baseName),
                    colorList(Arrays.asList("&fKit: &6" + (kit == null ? selectedKit : kit.name), " ", "&6Click to select this map!"))));
        }
        player.openInventory(inventory);
    }

    private List<String> resolveMenuLore(String path, String... replacements) {
        List<String> lore = new ArrayList<>();
        for (String line : frostMenus.getStringList(path)) {
            String resolved = color(line);
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                resolved = resolved.replace(replacements[i], replacements[i + 1] == null ? "" : replacements[i + 1]);
            }
            lore.add(resolved);
        }
        return lore;
    }

    private void handleHostEventClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        int size = Math.max(36, Math.min(54, frostMenus.getInt("EVENT-SETTINGS-MENU.SIZE", 5) * 9));
        int kitSlot = hostMenuSlot("KIT-SELECTION", 10, size);
        int mapSlot = hostMenuSlot("MAP-SELECTION", 13, size);
        int maxSlot = hostMenuSlot("MAX-PLAYERS-SELECTION", 22, size);
        int teamSlot = hostMenuSlot("TEAM-SIZE-SELECTION", 16, size);
        int startSlot = hostMenuSlot("START", 31, size);
        int cancelSlot = hostMenuSlot("CANCEL", 33, size);

        if (slot == kitSlot) {
            if (hostedEvents.containsKey(player.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "Stop your current event before changing kits.");
                return;
            }
            player.closeInventory();
            openHostEventKitMenu(player);
        } else if (slot == mapSlot) {
            if (hostedEvents.containsKey(player.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "Stop your current event before changing the map.");
                return;
            }
            player.closeInventory();
            openHostEventMapMenu(player);
        } else if (slot == maxSlot) {
            if (hostedEvents.containsKey(player.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "Stop your current event before changing max players.");
                return;
            }
            int cur = pendingEventMaxSize.getOrDefault(player.getUniqueId(), 16);
            if (event.isRightClick()) {
                cur = Math.max(2, cur - 2);
            } else {
                cur = Math.min(64, cur + 2);
            }
            pendingEventMaxSize.put(player.getUniqueId(), cur);
            Bukkit.getScheduler().runTaskLater(this, () -> openHostEventMenu(player), 1L);
        } else if (slot == teamSlot) {
            if (hostedEvents.containsKey(player.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "Stop your current event before changing team size.");
                return;
            }
            int cur = pendingEventTeamSize.getOrDefault(player.getUniqueId(), 1);
            if (event.isRightClick()) {
                cur = Math.max(1, cur - 1);
            } else {
                cur = Math.min(4, cur + 1);
            }
            pendingEventTeamSize.put(player.getUniqueId(), cur);
            Bukkit.getScheduler().runTaskLater(this, () -> openHostEventMenu(player), 1L);
        } else if (slot == startSlot) {
            player.closeInventory();
            EventSession hosted = hostedEvents.get(player.getUniqueId());
            if (hosted == null) {
                startHostedEvent(player);
            } else {
                startHostedEventRounds(hosted, true);
            }
        } else if (slot == cancelSlot) {
            EventSession hosted = hostedEvents.get(player.getUniqueId());
            if (hosted != null) {
                stopHostedEvent(hosted, color("&c[Event] The event has been cancelled by the host."), player, true);
            }
            player.closeInventory();
        }
    }

    private void handleHostEventMapClick(Player player, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String rawName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).trim();
        if (rawName.isEmpty()) {
            return;
        }
        if ("Random".equalsIgnoreCase(rawName)) {
            pendingEventArenaName.put(player.getUniqueId(), "Random");
        } else {
            ArenaSpawns arena = arenaByName(rawName);
            if (arena == null) {
                player.sendMessage(prefix + ChatColor.RED + "That map is no longer available.");
                return;
            }
            pendingEventArenaName.put(player.getUniqueId(), arena.baseName);
        }
        player.closeInventory();
        player.sendMessage(prefix + ChatColor.GREEN + "Map set to " + ChatColor.YELLOW + rawName + ChatColor.GREEN + ".");
        Bukkit.getScheduler().runTaskLater(this, () -> openHostEventMenu(player), 1L);
    }

    private void openHostEventKitMenu(Player player) {
        int rows = frostMenus.getInt("DUEL-INVENTORY.SIZE", frostMenus.getInt("QUEUE-INVENTORY.UNRANKED-SIZE", 5));
        int size = Math.min(54, Math.max(9, rows * 9));
        Inventory inv = Bukkit.createInventory(null, size, HOST_KIT_TITLE);
        fillMenuPlaceholders(inv);
        Set<Integer> occupied = new HashSet<>();
        int fallbackSlot = 0;
        for (KitData kit : kits.values()) {
            int slot = kit.unrankedPos;
            if (slot < 0 || slot >= size || occupied.contains(slot)) {
                while (fallbackSlot < size && occupied.contains(fallbackSlot)) {
                    fallbackSlot++;
                }
                if (fallbackSlot >= size) {
                    break;
                }
                slot = fallbackSlot;
            }
            ItemStack icon = menuIcon(kit.icon, Material.BOOK);
            ItemMeta im = icon.getItemMeta();
            if (im == null) {
                continue;
            }
            im.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + kit.name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Playing: " + ChatColor.GOLD + playingCount("unranked", kit.name));
            lore.add(ChatColor.WHITE + "Queuing: " + ChatColor.GOLD + queueSize("unranked", normalize(kit.name)));
            lore.add(" ");
            lore.add(ChatColor.GOLD + "Click to select this kit!");
            im.setLore(lore);
            icon.setItemMeta(im);
            inv.setItem(slot, icon);
            occupied.add(slot);
        }
        player.openInventory(inv);
    }

    private void handleHostEventKitClick(Player player, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String rawName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).trim();
        pendingEventKit.put(player.getUniqueId(), rawName);
        player.closeInventory();
        player.sendMessage(prefix + ChatColor.GREEN + "Kit set to " + ChatColor.YELLOW + rawName + ChatColor.GREEN + ".");
        Bukkit.getScheduler().runTaskLater(this, () -> openHostEventMenu(player), 1L);
    }

    private void startHostedEvent(Player player) {
        ensurePendingEventConfig(player);
        String kitName = pendingEventKit.get(player.getUniqueId());
        if (kitName == null || kitName.equals("None")) {
            player.sendMessage(prefix + ChatColor.RED + "Please select a kit first.");
            Bukkit.getScheduler().runTaskLater(this, () -> openHostEventMenu(player), 1L);
            return;
        }
        KitData kit = null;
        for (KitData k : kits.values()) {
            if (k.name.equalsIgnoreCase(kitName)) {
                kit = k;
                break;
            }
        }
        if (kit == null) {
            player.sendMessage(prefix + ChatColor.RED + "Kit '" + kitName + "' not found.");
            return;
        }
        int max = Math.max(2, Math.min(64, pendingEventMaxSize.getOrDefault(player.getUniqueId(), 16)));
        int teamSize = Math.max(1, Math.min(4, pendingEventTeamSize.getOrDefault(player.getUniqueId(), 1)));
        String selectedMap = pendingEventArenaName.getOrDefault(player.getUniqueId(), defaultEventMap());

        EventSession existing = hostedEvents.get(player.getUniqueId());
        if (existing != null) {
            stopHostedEvent(existing, color("&c[Event] The previous event has been replaced by a new one."), player, false);
        }

        EventSession session = new EventSession(kitName, kitName, player.getUniqueId(), System.currentTimeMillis(), 0, max,
                "Tournament", teamSize, true, false, selectedMap);
        session.players.add(player.getUniqueId());
        session.alivePlayers.add(player.getUniqueId());
        hostedEvents.put(player.getUniqueId(), session);
        activeEvents.put(player.getUniqueId(), session);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(" ");
            online.sendMessage(color("&6&lTournament Event"));
            online.sendMessage(color("&f • Host: &6" + player.getName()));
            online.sendMessage(color("&f • Kit: &6" + kitName));
            online.sendMessage(color("&f • Team Size: &6" + teamSize + "v" + teamSize));
            online.sendMessage(color("&f • Map: &6" + formatEventMap(selectedMap)));
            online.sendMessage(color("&f • Players: &6" + session.players.size() + "&f/&6" + max));
            sendSingleActionButton(online, "&a&l(CLICK TO JOIN)", "/event join " + player.getName(), "&aClick to join " + player.getName() + "'s event.");
            online.sendMessage(" ");
        }
        player.sendMessage(prefix + ChatColor.GREEN + "Event created! Waiting for players to join...");
        player.sendMessage(prefix + ChatColor.YELLOW + "The event will auto-start when it fills, or use the Start Event button to force-start it.");
    }

    private void joinHostedEvent(Player joiner, Player host) {
        EventSession session = hostedEvents.get(host.getUniqueId());
        if (session == null) {
            joiner.sendMessage(prefix + ChatColor.RED + "That event no longer exists.");
            return;
        }
        if (session.players.size() >= session.max) {
            joiner.sendMessage(prefix + ChatColor.RED + "That event is full (" + session.max + "/" + session.max + ").");
            return;
        }
        if (session.players.contains(joiner.getUniqueId())) {
            joiner.sendMessage(prefix + ChatColor.RED + "You are already in that event.");
            return;
        }
        if (matches.containsKey(joiner.getUniqueId())) {
            joiner.sendMessage(prefix + ChatColor.RED + "You cannot join an event while in a match.");
            return;
        }
        session.players.add(joiner.getUniqueId());
        session.alivePlayers.add(joiner.getUniqueId());
        activeEvents.put(joiner.getUniqueId(), session);
        setEventWaitingPlayer(joiner, null, true);
        broadcastToEvent(session, color("&6[Event] &e" + joiner.getName() + " &fjoined the &6Event &7(" + session.players.size() + "/" + session.max + ")"));
        joiner.sendMessage(prefix + ChatColor.GREEN + "Joined " + ChatColor.GOLD + host.getName() + ChatColor.GREEN + "'s " + ChatColor.GOLD + session.kitName + ChatColor.GREEN + " event!");
        if (!session.started && session.players.size() >= session.max) {
            startHostedEventRounds(session, false);
        }
    }

    private void startHostedEventRounds(EventSession session, boolean forced) {
        if (session == null) {
            return;
        }
        if (session.started) {
            Player host = Bukkit.getPlayer(session.host);
            if (forced && host != null) {
                host.sendMessage(prefix + ChatColor.RED + "That event has already started.");
            }
            return;
        }
        if (session.players.size() < 2) {
            Player host = Bukkit.getPlayer(session.host);
            if (host != null) {
                host.sendMessage(prefix + ChatColor.RED + "At least 2 players are required to start the event.");
            }
            return;
        }
        session.started = true;
        broadcastToEvent(session, color("&6[Event] &fStarting " + (forced ? "early " : "") + "&6" + session.kitName + " Event&f with &6" + session.players.size() + "&f players."));
        for (UUID playerId : session.players) {
            Player eventPlayer = Bukkit.getPlayer(playerId);
            if (eventPlayer != null && eventPlayer.isOnline()) {
                setEventWaitingPlayer(eventPlayer, null, session.alivePlayers.contains(playerId));
            }
        }
        startNextEventRound(session);
    }

    private void stopHostedEvent(EventSession session, String message, Player host, boolean notifyHost) {
        if (session == null) {
            return;
        }
        hostedEvents.remove(session.host);
        Set<Match> eventMatches = new HashSet<>();
        for (UUID uid : new HashSet<>(session.players)) {
            Match match = matches.get(uid);
            if (match != null && session.host.equals(match.eventHost)) {
                eventMatches.add(match);
            }
        }
        for (Match match : eventMatches) {
            match.cancelMatchTasks();
            matches.remove(match.first);
            matches.remove(match.second);
            lastPlayedMatch.remove(match.first);
            lastPlayedMatch.remove(match.second);
            respawningPlayers.remove(match.first);
            respawningPlayers.remove(match.second);
            resetArena(match);
            busyArenas.remove(match.arena);
        }
        if (message != null && !message.isEmpty()) {
            broadcastToEvent(session, message);
        }
        for (UUID uid : new HashSet<>(session.players)) {
            activeEvents.remove(uid);
            Player participant = Bukkit.getPlayer(uid);
            if (participant != null && participant.isOnline()) {
                returnToSpawn(participant, false);
            }
        }
        session.players.clear();
        session.alivePlayers.clear();
        session.started = false;
        session.currentMatchFirst = null;
        session.currentMatchSecond = null;
        if (notifyHost && host != null && host.isOnline()) {
            host.sendMessage(prefix + ChatColor.GREEN + "Event stopped.");
        }
    }

    // ── Event round management ─────────────────────────────────────────────────

    private void startNextEventRound(EventSession event) {
        if (event.alivePlayers.size() < 2) {
            // Event over
            UUID winnerId = event.alivePlayers.isEmpty() ? null : event.alivePlayers.get(0);
            String winnerName = winnerId == null ? "Nobody" : playerName(winnerId);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (event.players.contains(online.getUniqueId())) {
                    online.sendMessage(color("&6[Event] &d" + winnerName + " &fwins the event! Congratulations!"));
                }
            }
            // Broadcast event end to all
            Bukkit.broadcastMessage(color("&6[Event] &eThe event has ended! Winner: &d" + winnerName));
            // Clean up
            for (UUID uid : event.players) {
                activeEvents.remove(uid);
                Player p = Bukkit.getPlayer(uid);
                if (p != null) { returnToSpawn(p, false); }
            }
            hostedEvents.remove(event.host);
            event.players.clear();
            return;
        }

        // Find the kit
        KitData kit = null;
        for (KitData k : kits.values()) {
            if (k.name.equalsIgnoreCase(event.kitName)) { kit = k; break; }
        }
        if (kit == null) {
            broadcastToEvent(event, color("&c[Event] Kit not found, ending event."));
            for (UUID uid : event.players) {
                activeEvents.remove(uid);
                Player p = Bukkit.getPlayer(uid);
                if (p != null) { returnToSpawn(p, false); }
            }
            hostedEvents.remove(event.host);
            return;
        }

        final ArenaSpawns requestedArena = event.arenaPool == null || "Random".equalsIgnoreCase(event.arenaPool)
                ? null
                : arenaByName(event.arenaPool);

        // Pick next two alive players
        final UUID firstId  = event.alivePlayers.get(0);
        final UUID secondId = event.alivePlayers.get(1);
        final Player first  = Bukkit.getPlayer(firstId);
        final Player second = Bukkit.getPlayer(secondId);

        if (first == null || !first.isOnline() || second == null || !second.isOnline()) {
            // One is offline – remove and retry
            if (first == null || !first.isOnline())  event.alivePlayers.remove(firstId);
            if (second == null || !second.isOnline()) event.alivePlayers.remove(secondId);
            startNextEventRound(event);
            return;
        }

        broadcastToEvent(event, color("&6[Event] &fRound &6" + event.round + "&f: " + ChatColor.BLUE + first.getName() + ChatColor.GOLD + " &fvs " + ChatColor.RED + second.getName() + ChatColor.GOLD + "&f!"));
        event.round++;
        event.currentMatchFirst  = firstId;
        event.currentMatchSecond = secondId;

        // Start match flagged with this event host
        final KitData finalKit = kit;
        final EventSession finalEvent = event;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!first.isOnline() || !second.isOnline()) {
                if (!first.isOnline())  finalEvent.alivePlayers.remove(firstId);
                if (!second.isOnline()) finalEvent.alivePlayers.remove(secondId);
                startNextEventRound(finalEvent);
                return;
            }
            ArenaSpawns arena = requestedArena == null ? acquireArena(finalKit) : acquireArena(finalKit, requestedArena);
            if (arena == null) {
                broadcastToEvent(finalEvent, color("&c[Event] No arena available, retrying..."));
                Bukkit.getScheduler().runTaskLater(this, () -> startNextEventRound(finalEvent), 60L);
                return;
            }
            setEventWaitingPlayers(finalEvent, firstId, secondId, arena);
            Match match = new Match(firstId, secondId, finalKit, arena, "event");
            match.eventHost = finalEvent.host;
            match.displayKind = "event";
            matches.put(firstId, match);
            matches.put(secondId, match);
            preparePlayer(first,  finalKit, arena.first,  true);
            preparePlayer(second, finalKit, arena.second, false);
            updateMatchScoreboard(match);
        }, 20L);
    }

    private void broadcastToEvent(EventSession event, String message) {
        for (UUID uid : event.players) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) p.sendMessage(message);
        }
    }

    private void setEventWaitingPlayers(EventSession event, UUID firstId, UUID secondId, ArenaSpawns arena) {
        for (UUID playerId : event.players) {
            if (playerId.equals(firstId) || playerId.equals(secondId)) {
                continue;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                setEventWaitingPlayer(player, arena, event.alivePlayers.contains(playerId));
            }
        }
    }

    private void setEventWaitingPlayer(Player player, ArenaSpawns arena, boolean canLeaveEvent) {
        resetPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        if (arena != null) {
            pluginTeleport(player, eventSpectatorLocation(arena));
        } else if (spawnLocation != null) {
            pluginTeleport(player, spawnLocation);
        }
        ItemStack dye = new ItemStack(Material.INK_SACK, 1, (short) 1);
        ItemMeta meta = dye.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(canLeaveEvent
                    ? ChatColor.RED + "Leave Event " + ChatColor.GRAY + "(Right Click)"
                    : ChatColor.RED + "Return to Lobby " + ChatColor.GRAY + "(Right Click)");
            dye.setItemMeta(meta);
        }
        player.getInventory().setItem(8, dye);
        player.updateInventory();
        EventSession session = activeEvents.get(player.getUniqueId());
        if (session != null) {
            updateEventScoreboard(player, session, false);
        }
    }

    private Location eventSpectatorLocation(ArenaSpawns arena) {
        Location first = arena.first;
        Location second = arena.second;
        Location base = first.clone();
        base.setX((first.getX() + second.getX()) / 2.0D);
        base.setY(Math.max(first.getY(), second.getY()) + 6.0D);
        base.setZ((first.getZ() + second.getZ()) / 2.0D);
        return base;
    }


    private void openKitEditorMenu(Player player) {
        int rows = frostMenus.getInt("KIT-EDITOR-INVENTORY.SIZE", frostMenus.getInt("QUEUE-INVENTORY.UNRANKED-SIZE", 5));
        int size = Math.min(54, Math.max(9, rows * 9));
        Inventory inventory = Bukkit.createInventory(null, size, kitEditorTitle());
        fillMenuPlaceholders(inventory);
        int fallbackSlot = 0;

        Set<Integer> occupied = new HashSet<>();
        for (KitData kit : kits.values()) {
            int slot = kit.editorPos >= 0 ? kit.editorPos : kit.unrankedPos;
            if (slot < 0 || slot >= size || occupied.contains(slot)) {
                while (fallbackSlot < size && occupied.contains(fallbackSlot)) {
                    fallbackSlot++;
                }
                if (fallbackSlot >= size) {
                    break;
                }
                slot = fallbackSlot;
            }
            ItemStack icon = menuIcon(kit.icon, Material.BOOK);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(frostMenus.getString("KIT-EDITOR-INVENTORY.NAME-COLOR", "&6&l") + kit.name));
                meta.setLore(colorList(replaceLines(frostMenus.getStringList("KIT-EDITOR-INVENTORY.LORE"), "<kit>", kit.name, "<kit_name>", kit.name)));
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot, icon);
            occupied.add(slot);
        }
        player.openInventory(inventory);
    }

    private void handleKitEditorClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) {
            return;
        }
        KitData kit = kits.get(normalize(item.getItemMeta().getDisplayName()));
        if (kit != null) {
            openLayoutEditor(player, kit);
        }
    }

    private void openLayoutEditor(Player player, KitData kit) {
        Inventory inventory = Bukkit.createInventory(null, 54, layoutEditorTitle(kit));
        ItemStack[] contents = customKitContents(player.getUniqueId(), kit);
        for (int i = 0; i < Math.min(36, contents.length); i++) {
            inventory.setItem(i, contents[i] == null ? null : contents[i].clone());
        }
        inventory.setItem(48, simpleItem(Material.WOOL, (short) 5, color("&a&lSave Layout"), colorList(Arrays.asList("&7Save this layout for &6" + kit.name + "&7."))));
        inventory.setItem(49, simpleItem(Material.WOOL, (short) 4, color("&e&lReset Layout"), colorList(Arrays.asList("&7Restore the default &6" + kit.name + " &7layout."))));
        inventory.setItem(50, simpleItem(Material.WOOL, (short) 14, color("&c&lCancel"), colorList(Arrays.asList("&7Close without saving."))));
        layoutEditorKits.put(player.getUniqueId(), kit);
        player.openInventory(inventory);
    }

    private void handleLayoutEditorClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int raw = event.getRawSlot();
        if (raw == 48 || raw == 49 || raw == 50 || raw >= 45 && raw <= 53) {
            event.setCancelled(true);
            KitData kit = layoutEditorKits.get(player.getUniqueId());
            if (kit == null) {
                player.closeInventory();
                return;
            }
            if (raw == 48) {
                saveLayout(player, kit, event.getInventory());
                player.sendMessage(prefix + color("&aSaved &6" + kit.name + " &alayout."));
                player.closeInventory();
            } else if (raw == 49) {
                resetLayout(player, kit);
                player.sendMessage(prefix + color("&eReset &6" + kit.name + " &elayout."));
                openLayoutEditor(player, kit);
            } else if (raw == 50) {
                player.closeInventory();
            }
        }
    }

    private void joinFfaSpawn(Player player) {
        Location location = parseLocation(frostConfig.getString("ffaLocation"), spawnLocation == null ? null : spawnLocation.getWorld());
        if (location == null) {
            player.sendMessage(prefix + ChatColor.RED + "Frost/config.yml has no ffaLocation.");
            return;
        }
        resetPlayer(player);
        pluginTeleport(player, location);
        EventSession session = new EventSession("FFA", player.getUniqueId(), System.currentTimeMillis(), 1, frostSettings.getInt("SETTINGS.GENERAL.MAXIMUM-PARTY-SIZE", 32));
        activeEvents.put(player.getUniqueId(), session);
        sendFrostLines(player, "MESSAGES.FFA.JOINED", "<kit>", "Default");
        updateLobbyScoreboard(player);
    }

    private void openCurrentMatchesMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.GOLD + "" + ChatColor.BOLD + "Current Matches");
        Set<Match> unique = new HashSet<>(matches.values());
        int slot = 0;
        for (Match match : unique) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, simpleItem(Material.COMPASS, (short) 0,
                    ChatColor.GOLD + playerName(match.first) + ChatColor.GRAY + " vs " + ChatColor.GOLD + playerName(match.second),
                    Arrays.asList(ChatColor.GRAY + "Kit: " + ChatColor.GOLD + match.kit.name, ChatColor.GRAY + "Arena: " + ChatColor.GOLD + match.arena.name)));
        }
        player.openInventory(inventory);
    }

    private void openPartyMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, color(frostMenus.getString("QUEUE-INVENTORY.PARTY-OTHER-PARTIES-INVENTORY-TITLE", PARTY_TITLE)));
        fillMenuPlaceholders(inventory);
        int slot = 0;
        Set<Party> unique = new HashSet<>(parties.values());
        Party own = parties.get(player.getUniqueId());
        for (Party party : unique) {
            if (slot >= inventory.getSize()) {
                break;
            }
            if (own == null || party == own || party.leader.equals(player.getUniqueId())) {
                continue;
            }
            Player leader = Bukkit.getPlayer(party.leader);
            if (leader == null) {
                continue;
            }
            inventory.setItem(slot++, simpleItem(Material.DIAMOND_AXE, (short) 0,
                    color("&6&l" + leader.getName() + "'s Party"),
                    colorList(Arrays.asList("&fMembers: &6" + party.members.size(), " ", "&6Click to fight!"))));
        }
        player.openInventory(inventory);
    }

    private void handleOtherPartyClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) {
            return;
        }
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName()).replace("'s Party", "").trim();
        Player targetLeader = Bukkit.getPlayer(name);
        if (targetLeader == null || parties.get(targetLeader.getUniqueId()) == null) {
            player.sendMessage(prefix + ChatColor.RED + "That party is no longer available.");
            player.closeInventory();
            return;
        }
        openPartyKitMenu(player, "partyfight", targetLeader.getUniqueId());
    }

    private void openPartyKitMenu(Player player, String mode, UUID targetLeader) {
        Party party = parties.get(player.getUniqueId());
        if (party == null || !party.leader.equals(player.getUniqueId())) {
            player.sendMessage(prefix + ChatColor.RED + "Only the party leader can start this.");
            return;
        }
        if ("event".equals(mode) && firstOnlinePartyMember(party, player.getUniqueId()) == null) {
            player.sendMessage(prefix + ChatColor.RED + "Invite at least one party member first.");
            return;
        }
        partyMenuModes.put(player.getUniqueId(), mode);
        partyMenuTargets.put(player.getUniqueId(), targetLeader);
        partyMenuKits.remove(player.getUniqueId());
        int rows = frostMenus.getInt("PARTY-DUEL-INVENTORY.SIZE", frostMenus.getInt("QUEUE-INVENTORY.UNRANKED-SIZE", 5));
        int size = Math.min(54, Math.max(9, rows * 9));
        Inventory inventory = Bukkit.createInventory(null, size, partyKitTitle(mode));
        fillMenuPlaceholders(inventory);
        Set<Integer> occupied = new HashSet<>();

        // Party Events: allow toggling between FFA-style (1v1 rotation) and team balancing (e.g., 2v1 for 3 players).
        // Slot 4 is reserved for this toggle (top row center) so it doesn't collide with kit slots.
        if ("event".equals(mode) && size >= 9) {
            int toggleSlot = 4;
            occupied.add(toggleSlot);
            boolean ffa = party.partyEventFfa;
            ItemStack toggle = new ItemStack(ffa ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
            ItemMeta tm = toggle.getItemMeta();
            if (tm != null) {
                tm.setDisplayName(color("&6&lFFA: " + (ffa ? "&aON" : "&cOFF")));
                tm.setLore(colorList(Arrays.asList(
                        "&7Click to toggle party event mode.",
                        " ",
                        "&fOFF: &62v1 &7when party size is 3",
                        "&fON:  &6normal 1v1 &7(party FFA style)"
                )));
                toggle.setItemMeta(tm);
            }
            inventory.setItem(toggleSlot, toggle);
        }

        int fallbackSlot = 0;
        for (KitData kit : kits.values()) {
            int slot = kit.unrankedPos;
            if (slot < 0 || slot >= size || occupied.contains(slot)) {
                while (fallbackSlot < size && occupied.contains(fallbackSlot)) {
                    fallbackSlot++;
                }
                if (fallbackSlot >= size) {
                    break;
                }
                slot = fallbackSlot;
            }
            ItemStack icon = menuIcon(kit.icon, Material.DIAMOND_SWORD);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color("&6&l" + kit.name));
                meta.setLore(colorList(Arrays.asList("&fMode: &6" + ("event".equals(mode) ? "Party Event" : "Party Fight"), " ", "&6Click to choose a map!")));
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot, icon);
            occupied.add(slot);
        }
        player.openInventory(inventory);
    }

    private void handlePartyKitClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) {
            return;
        }
        // Party event FFA toggle
        String stripped = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (stripped != null && stripped.toUpperCase(Locale.ROOT).startsWith("FFA:")) {
            Party party = parties.get(player.getUniqueId());
            if (party == null || !party.leader.equals(player.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "Only the party leader can toggle this.");
                return;
            }
            party.partyEventFfa = !party.partyEventFfa;
            // Re-open the same menu
            String mode = partyMenuModes.get(player.getUniqueId());
            UUID targetLeader = partyMenuTargets.get(player.getUniqueId());
            Bukkit.getScheduler().runTask(this, () -> openPartyKitMenu(player, mode == null ? "event" : mode, targetLeader));
            return;
        }
        KitData kit = kits.get(normalize(item.getItemMeta().getDisplayName()));
        if (kit == null) {
            return;
        }
        partyMenuKits.put(player.getUniqueId(), kit);
        openPartyMapMenu(player, kit);
    }

    private void openPartyMapMenu(Player player, KitData kit) {
        List<ArenaSpawns> choices = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ArenaSpawns arena : arenas) {
            if (kit.canUseArena(arena) && seen.add(arena.baseName.toLowerCase(Locale.ROOT))) {
                choices.add(arena);
            }
        }
        Inventory inventory = Bukkit.createInventory(null, 45, partyMapTitle());
        fillMenuPlaceholders(inventory);
        int slot = 0;
        for (ArenaSpawns arena : choices) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, simpleItem(Material.EMPTY_MAP, (short) 0, color("&6&l" + arena.baseName),
                    colorList(Arrays.asList("&fKit: &6" + kit.name, " ", "&6Click to start!"))));
        }
        player.openInventory(inventory);
    }

    private void sendCurrentMap(Player player) {
        Match match = matches.get(player.getUniqueId());
        if (match == null) {
            player.sendMessage(prefix + color("&cYou are not currently playing a map."));
            return;
        }
        player.sendMessage(prefix + color("&fYou are playing on &6" + match.arena.baseName + "&f."));
    }

    private void handlePartyMapClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) {
            return;
        }
        KitData kit = partyMenuKits.remove(player.getUniqueId());
        String mode = partyMenuModes.remove(player.getUniqueId());
        UUID targetLeaderId = partyMenuTargets.remove(player.getUniqueId());
        ArenaSpawns arena = arenaByName(ChatColor.stripColor(item.getItemMeta().getDisplayName()));
        player.closeInventory();
        if (kit == null || mode == null || arena == null) {
            player.sendMessage(prefix + ChatColor.RED + "That party selection is gone.");
            return;
        }
        if ("event".equals(mode)) {
            startPartyEvent(player, kit, arena);
        } else {
            startPartyFight(player, targetLeaderId, kit, arena);
        }
    }

    private void startPartyEvent(Player leader, KitData kit, ArenaSpawns arena) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null || !party.leader.equals(leader.getUniqueId())) {
            leader.sendMessage(prefix + ChatColor.RED + "You are not a party leader.");
            return;
        }

        // Gather ready party members (online + not in match/queue)
        List<Player> ready = new ArrayList<>();
        for (UUID uuid : party.members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !matches.containsKey(uuid) && !queuedKit.containsKey(uuid)) {
                ready.add(p);
            }
        }
        if (ready.size() < 2) {
            leader.sendMessage(prefix + ChatColor.RED + "No party member is ready.");
            return;
        }

        // Party size 3: support 2v1 when FFA is OFF.
        if (ready.size() == 3 && !party.partyEventFfa) {
            // Only allow 2v1 for simple kits (avoid special modes that assume 1v1 logic).
            if (kit.boxing || kit.sumo || kit.bridges || kit.bedWars) {
                leader.sendMessage(prefix + ChatColor.RED + "2v1 party events are not supported for this kit. Enable FFA or choose another kit.");
                return;
            }

            // Team A = leader + one teammate, Team B = remaining player
            Player teammate = null;
            Player solo = null;
            for (Player p : ready) {
                if (p.getUniqueId().equals(leader.getUniqueId())) {
                    continue;
                }
                if (teammate == null) {
                    teammate = p;
                } else {
                    solo = p;
                }
            }
            if (teammate == null || solo == null) {
                leader.sendMessage(prefix + ChatColor.RED + "Could not start party event (missing players).");
                return;
            }

            startTeamMatch(Arrays.asList(leader, teammate), Collections.singletonList(solo), kit, "duel", arena, "party");
            return;
        }

        // Default: start a normal 1v1 (Party Event FFA style)
        Player opponent = firstOnlinePartyMember(party, leader.getUniqueId());
        if (opponent == null) {
            leader.sendMessage(prefix + ChatColor.RED + "No party member is ready.");
            return;
        }
        startMatch(leader, opponent, kit, "duel", arena, "party", 1);
    }

    /**
     * Starts a match with 2 teams (supports uneven teams, e.g. 2v1). This is only used for party events.
     * NOTE: This intentionally only supports simple kits (no bridges/boxing/sumo/bed respawn modes).
     */
    private void startTeamMatch(List<Player> firstTeam, List<Player> secondTeam, KitData kit, String type,
                                ArenaSpawns requestedArena, String displayKind) {
        if (firstTeam == null || secondTeam == null || firstTeam.isEmpty() || secondTeam.isEmpty()) {
            return;
        }
        Player firstCaptain = firstTeam.get(0);
        Player secondCaptain = secondTeam.get(0);
        if (firstCaptain == null || secondCaptain == null) {
            return;
        }
        // Basic availability checks
        for (Player p : firstTeam) {
            if (p == null || !p.isOnline() || matches.containsKey(p.getUniqueId()) || queuedKit.containsKey(p.getUniqueId())) {
                firstCaptain.sendMessage(prefix + ChatColor.RED + "A team member is not ready.");
                return;
            }
        }
        for (Player p : secondTeam) {
            if (p == null || !p.isOnline() || matches.containsKey(p.getUniqueId()) || queuedKit.containsKey(p.getUniqueId())) {
                firstCaptain.sendMessage(prefix + ChatColor.RED + "A team member is not ready.");
                return;
            }
        }

        ArenaSpawns arena = requestedArena == null ? acquireArena(kit) : acquireArena(kit, requestedArena);
        if (arena == null) {
            firstCaptain.sendMessage(format("no-arena"));
            secondCaptain.sendMessage(format("no-arena"));
            return;
        }

        Match match = new Match(firstCaptain.getUniqueId(), secondCaptain.getUniqueId(), kit, arena, normalizeQueueType(type));
        match.displayKind = displayKind == null ? "" : displayKind;
        match.teamSize = Math.max(1, Math.max(firstTeam.size(), secondTeam.size()));

        Set<UUID> firstIds = new HashSet<>();
        for (Player p : firstTeam) firstIds.add(p.getUniqueId());
        Set<UUID> secondIds = new HashSet<>();
        for (Player p : secondTeam) secondIds.add(p.getUniqueId());
        match.initTeams(firstIds, secondIds);

        // Register all participants in the matches map
        for (UUID id : match.participants) {
            matches.put(id, match);
        }

        match.firstIsBlue = detectIsBlue(arena.first);
        match.secondIsBlue = !match.firstIsBlue;

        // Prepare all players
        for (Player p : firstTeam) {
            preparePlayer(p, kit, arena.first, match.firstIsBlue);
        }
        for (Player p : secondTeam) {
            preparePlayer(p, kit, arena.second, match.secondIsBlue);
        }

        updateMatchScoreboard(match);
        for (UUID id : match.participants) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            UUID opponentCaptain = match.other(id);
            Player opp = opponentCaptain == null ? null : Bukkit.getPlayer(opponentCaptain);
            sendMatchStartMessage(p, opp, match);
        }

        match.countdownTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            private int left = 5;

            @Override
            public void run() {
                // Abort if captains are gone (match ended)
                if (!matches.containsKey(firstCaptain.getUniqueId()) || !matches.containsKey(secondCaptain.getUniqueId())) {
                    match.cancelMatchTasks();
                    return;
                }
                if (left <= 0) {
                    match.started = true;
                    releaseMatchRespawnState(match);
                    updateMatchScoreboard(match);
                    for (UUID id : match.participants) {
                        Player p = Bukkit.getPlayer(id);
                        if (p != null) {
                            sendTitle(p, ChatColor.GREEN + "Fight!", "");
                            playSound(p, "fight");
                        }
                    }
                    sendTitleToSpectators(match, ChatColor.GREEN + "Fight!", "");
                    match.cancelCountdownTaskOnly();
                    releaseMatchRespawnState(match);
                    return;
                }
                String title = ChatColor.GOLD + String.valueOf(left);
                for (UUID id : match.participants) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) {
                        sendTitle(p, title, "");
                        playSound(p, "countdown");
                    }
                }
                sendTitleToSpectators(match, title, "");
                left--;
            }
        }, 0L, 20L);
    }

    private void startPartyFight(Player leader, UUID targetLeaderId, KitData kit, ArenaSpawns arena) {
        Party own = parties.get(leader.getUniqueId());
        Player targetLeader = targetLeaderId == null ? null : Bukkit.getPlayer(targetLeaderId);
        Party other = targetLeader == null ? null : parties.get(targetLeader.getUniqueId());
        if (own == null || other == null || targetLeader == null) {
            leader.sendMessage(prefix + ChatColor.RED + "That party is no longer available.");
            return;
        }
        int teamSize = Math.max(1, Math.min(own.members.size(), other.members.size()));
        startMatch(leader, targetLeader, kit, "duel", arena, "party", teamSize);
    }

    private ItemStack menuItem(Material material, short data, String name, boolean enabled) {
        return simpleItem(material, data, name, enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled");
    }

    private ItemStack globalChatItem(int mode) {
        String state = mode == 0
                ? ChatColor.GREEN + "Enabled"
                : mode == 1
                ? ChatColor.YELLOW + "(Friends Only)"
                : ChatColor.RED + "Disabled";
        return simpleItem(Material.PAPER, (short) 0, "Global Chat", state);
    }

    private ItemStack menuIcon(ItemStack configured, Material fallback) {
        ItemStack icon = configured == null ? new ItemStack(fallback) : configured.clone();
        icon.setAmount(1);
        return icon;
    }

    private ItemStack simpleItem(Material material, short data, String name, String lore) {
        return simpleItem(material, data, name, Collections.singletonList(lore));
    }

    private ItemStack simpleItem(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillMenuPlaceholders(Inventory inventory) {
        if (!frostMenus.getBoolean("QUEUE-INVENTORY.PLACEHOLDER-ITEMS-ENABLED", true)) {
            return;
        }
        ItemStack placeholder = simpleItem(placeholderMaterial(), (short) frostMenus.getInt("QUEUE-INVENTORY.PLACEHOLDER-ITEM-DATA", 15),
                color(frostMenus.getString("DUEL-INVENTORY.PLACEHOLDER-NAME", "&8 ")), Collections.emptyList());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, placeholder);
        }
    }

    private Material placeholderMaterial() {
        Material material = Material.matchMaterial(frostMenus.getString("QUEUE-INVENTORY.PLACEHOLDER-ITEM-MATERIAL", "STAINED_GLASS_PANE"));
        return material == null ? Material.STAINED_GLASS_PANE : material;
    }

    private String duelKitTitle() {
        return color(frostMenus.getString("DUEL-INVENTORY.KIT.TITLE",
                frostMenus.getString("QUEUE-INVENTORY.DUEL-INVENTORY-TITLE", DUEL_KIT_TITLE)));
    }

    private String duelMapTitle() {
        return color(frostMenus.getString("DUEL-INVENTORY.MAP.TITLE",
                frostMenus.getString("QUEUE-INVENTORY.DUEL-ARENA-SELECTION-TITLE", DUEL_MAP_TITLE)));
    }

    private boolean isDuelKitMenu(String title) {
        return DUEL_KIT_TITLE.equals(title)
                || color(frostMenus.getString("QUEUE-INVENTORY.DUEL-INVENTORY-TITLE", "")).equals(title)
                || duelKitTitle().equals(title);
    }

    private boolean isDuelMapMenu(String title) {
        return DUEL_MAP_TITLE.equals(title)
                || color(frostMenus.getString("QUEUE-INVENTORY.DUEL-ARENA-SELECTION-TITLE", "")).equals(title)
                || duelMapTitle().equals(title);
    }

    private String kitEditorTitle() {
        return color(frostMenus.getString("KIT-EDITOR-INVENTORY.TITLE", KIT_EDITOR_TITLE));
    }

    private boolean isKitEditorMenu(String title) {
        return KIT_EDITOR_TITLE.equals(title) || kitEditorTitle().equals(title);
    }

    private String layoutEditorTitle(KitData kit) {
        return color(frostMenus.getString("KIT-EDITOR-INVENTORY.EDITOR-TITLE", "&6&lEditing Layout &f<kit_name>")
                .replace("<kit_name>", kit.name)
                .replace("<kit>", kit.name));
    }

    private boolean isLayoutEditor(String title) {
        return ChatColor.stripColor(title).toLowerCase(Locale.ROOT).contains("editing layout");
    }

    private boolean isOtherPartiesMenu(String title) {
        return color(frostMenus.getString("QUEUE-INVENTORY.PARTY-OTHER-PARTIES-INVENTORY-TITLE", PARTY_TITLE)).equals(title);
    }

    private String partyKitTitle(String mode) {
        return color(frostMenus.getString("PARTY-DUEL-INVENTORY.KIT-TITLE",
                "event".equals(mode) ? "&6&lParty Event Kit" : "&6&lParty Fight Kit"));
    }

    private String partyMapTitle() {
        return color(frostMenus.getString("PARTY-DUEL-INVENTORY.MAP-TITLE", "&6&lSelect Party Map"));
    }

    private boolean isPartyKitMenu(String title) {
        return partyKitTitle("event").equals(title) || partyKitTitle("partyfight").equals(title);
    }

    private boolean isPartyMapMenu(String title) {
        return partyMapTitle().equals(title);
    }

    private PlayerSettings settings(Player player) {
        return playerSettings.computeIfAbsent(player.getUniqueId(), key -> new PlayerSettings());
    }

    private void queue(Player player, String kitName, String type) {
        KitData kit = kits.get(normalize(kitName));
        if (kit == null) {
            message(player, "no-kit");
            return;
        }
        if (matches.containsKey(player.getUniqueId())) {
            message(player, "already-fighting");
            return;
        }
        String normalizedType = normalizeQueueType(type);
        if ("ranked".equals(normalizedType) && !kit.ranked) {
            player.sendMessage(prefix + ChatColor.RED + "That kit is not available in ranked.");
            return;
        }
        if ("ranked".equals(normalizedType) && getPing(player) > frostSettings.getInt("SETTINGS.MATCH.MAX-RANKED-PING", 1000)) {
            player.sendMessage(prefix + color(frostSettings.getString("SETTINGS.MATCH.PING-TOO-HIGH-MESSAGE", "&cYour ping is too high to play ranked.")));
            return;
        }
        if (queuedKit.containsKey(player.getUniqueId())) {
            message(player, "already-queued");
            return;
        }
        if (arenas.isEmpty()) {
            message(player, "no-arena");
            return;
        }

        Queue<UUID> queue = queues.computeIfAbsent(queueKey(normalizedType, normalize(kit.name)), key -> new ArrayDeque<>());
        UUID opponentId = pollOnlineOpponent(queue, player.getUniqueId());
        if (opponentId == null) {
            queue.add(player.getUniqueId());
            queuedKit.put(player.getUniqueId(), normalize(kit.name));
            queuedType.put(player.getUniqueId(), normalizedType);
            queuedAt.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendMessage(format("queued", "<kit>", kit.name, "<queue>", displayQueueType(normalizedType)));
            giveStateHotbar(player);
            updateLobbyScoreboard(player);
            refreshQueueMenus();
            return;
        }

        Player opponent = Bukkit.getPlayer(opponentId);
        if (opponent == null) {
            queue.add(player.getUniqueId());
            queuedKit.put(player.getUniqueId(), normalize(kit.name));
            queuedType.put(player.getUniqueId(), normalizedType);
            queuedAt.put(player.getUniqueId(), System.currentTimeMillis());
            refreshQueueMenus();
            return;
        }
        queuedKit.remove(opponentId);
        queuedType.remove(opponentId);
        queuedAt.remove(opponentId);
        startMatch(player, opponent, kit, normalizedType);
        refreshQueueMenus();
    }

    private void handleDuelCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(prefix + ChatColor.RED + "Use /duel <player> or /duel accept <player>.");
            return;
        }
        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage(prefix + ChatColor.RED + "Use /duel accept <player>.");
                return;
            }
            Player challenger = Bukkit.getPlayer(args[1]);
            DuelRequest request = duelRequests.get(player.getUniqueId());
            if (challenger == null || request == null || !request.sender.equals(challenger.getUniqueId()) || request.expired()) {
                message(player, "duel-expired");
                return;
            }
            duelRequests.remove(player.getUniqueId());
            if (!canStartDirectMatch(player) || !canStartDirectMatch(challenger)) {
                return;
            }
            startMatch(challenger, player, request.kit, "duel", request.arena);
            return;
        }
        if (args[0].equalsIgnoreCase("deny")) {
            if (args.length < 2) {
                player.sendMessage(prefix + ChatColor.RED + "Use /duel deny <player>.");
                return;
            }
            Player challenger = Bukkit.getPlayer(args[1]);
            DuelRequest request = duelRequests.get(player.getUniqueId());
            if (challenger == null || request == null || !request.sender.equals(challenger.getUniqueId()) || request.expired()) {
                message(player, "duel-expired");
                return;
            }
            duelRequests.remove(player.getUniqueId());
            player.sendMessage(prefix + ChatColor.RED + "You declined " + challenger.getName() + "'s duel request.");
            challenger.sendMessage(prefix + ChatColor.RED + player.getName() + " declined your duel request.");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || target.equals(player)) {
            player.sendMessage(prefix + ChatColor.RED + "That player is not online.");
            return;
        }
        if (!canStartDirectMatch(player) || !canStartDirectMatch(target)) {
            return;
        }
        if (!settings(target).duelRequests) {
            player.sendMessage(prefix + ChatColor.RED + "That player is not accepting duel requests.");
            return;
        }
        if (args.length == 1) {
            openDuelKitMenu(player, target);
            return;
        }
        KitData kit = kits.get(normalize(args[1]));
        if (kit == null) {
            message(player, "no-kit");
            return;
        }
        ArenaSpawns arena = args.length > 2 ? arenaByName(args[2]) : null;
        if (args.length > 2 && arena == null) {
            player.sendMessage(prefix + ChatColor.RED + "Unknown map.");
            return;
        }
        sendDuelRequest(player, target, kit, arena);
    }

    private void sendDuelRequest(Player player, Player target, KitData kit, ArenaSpawns arena) {
        if (!canStartDirectMatch(player) || !canStartDirectMatch(target)) {
            return;
        }
        if (!settings(target).duelRequests) {
            player.sendMessage(prefix + ChatColor.RED + "That player is not accepting duel requests.");
            return;
        }
        duelRequests.put(target.getUniqueId(), new DuelRequest(player.getUniqueId(), kit, arena, System.currentTimeMillis() + 60000L));
        
        String mapName = (arena == null ? "Random" : arena.baseName);
        String playerPrefix = getLPPrefix(player);
        String targetPrefix = getLPPrefix(target);

        player.sendMessage(" ");
        player.sendMessage(color("&6&lDuel Sent"));
        player.sendMessage(color("&f • To: &6" + targetPrefix + target.getName()));
        player.sendMessage(color("&f • Kit: &6" + kit.name));
        player.sendMessage(color("&f • Map: &6" + mapName));
        player.sendMessage(" ");

        target.sendMessage(" ");
        target.sendMessage(color("&6&lDuel Received"));
        target.sendMessage(color("&f • From: &6" + playerPrefix + player.getName()));
        target.sendMessage(color("&f • Kit: &6" + kit.name));
        target.sendMessage(color("&f • Map: &6" + mapName));
        target.sendMessage(" ");
        sendDuelButtons(target, player);
    }

    private String getLPPrefix(Player player) {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            LuckPerms lp = provider.getProvider();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                return prefix == null ? "" : color(prefix);
            }
        }
        return "";
    }

    private void sendActionButtons(Player target, String primaryLabel, String primaryCommand, String primaryHover,
                                   String secondaryLabel, String secondaryCommand, String secondaryHover) {
        TextComponent line = new TextComponent("");
        line.addExtra(commandButton(primaryLabel, primaryCommand, primaryHover));
        if (secondaryLabel != null && secondaryCommand != null) {
            line.addExtra(new TextComponent(color(" &7")));
            line.addExtra(commandButton(secondaryLabel, secondaryCommand, secondaryHover));
        }
        target.spigot().sendMessage(line);
    }

    private void sendSingleActionButton(Player target, String label, String command, String hover) {
        target.spigot().sendMessage(commandButton(label, command, hover));
    }

    private TextComponent commandButton(String label, String command, String hover) {
        TextComponent button = new TextComponent(color(label));
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(color(hover)).create()));
        return button;
    }

    private void sendDuelButtons(Player target, Player challenger) {
        String acceptCmd = "/duel accept " + challenger.getName();
        sendSingleActionButton(target, "&a&l[ACCEPT DUEL]", acceptCmd, "&aClick to accept this duel.");
    }

    private void handlePartyCommand(Player player, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            showPartyInfo(player);
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("create")) {
            if (parties.containsKey(player.getUniqueId())) {
                showPartyInfo(player);
                return;
            }
            Party party = new Party(player.getUniqueId());
            party.members.add(player.getUniqueId());
            parties.put(player.getUniqueId(), party);
            player.sendMessage(format("party-created"));
            // Only list in public parties if the setting is enabled AND a party was created.
            if (settings(player).publicPartiesEnabled) {
                setPartyPublic(player, true, false);
            } else {
                publicParties.remove(player.getUniqueId());
            }
            updateLobbyScoreboard(player);
            return;
        }
        if (sub.equals("open") || sub.equals("public")) {
            Party party = getOrCreateParty(player);
            if (!party.leader.equals(player.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "Only the party leader can make the party public.");
                return;
            }
            setPartyPublic(player, true, true);
            return;
        }
        if (sub.equals("close") || sub.equals("private")) {
            Party party = parties.get(player.getUniqueId());
            if (party == null || !party.leader.equals(player.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "Only the party leader can make the party private.");
                return;
            }
            setPartyPublic(player, false, false);
            return;
        }
        if (sub.equals("invite")) {
            Party party = getOrCreateParty(player);
            if (!party.leader.equals(player.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "Only the party leader can invite players.");
                return;
            }
            if (args.length < 2) {
                player.sendMessage(prefix + ChatColor.RED + "Use /party invite <player>.");
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || target.equals(player)) {
                player.sendMessage(prefix + ChatColor.RED + "That player is not online.");
                return;
            }
            if (!settings(target).partyRequests) {
                player.sendMessage(prefix + ChatColor.RED + "That player is not accepting party invites.");
                return;
            }
            partyInvites.put(target.getUniqueId(), new PartyInvite(player.getUniqueId(), System.currentTimeMillis() + 60000L));
            player.sendMessage(format("party-invited", "<player>", target.getName()));
            target.sendMessage(prefix + color("&6" + player.getName() + " &fhas invited you to their &6party&f."));
            sendActionButtons(target,
                    "&a&l[JOIN PARTY]", "/party accept " + player.getName(), "&aClick to join " + player.getName() + "'s party.",
                    "&c&l[DENY]", "/party deny " + player.getName(), "&7Click to deny this party invite.");
            return;
        }
        if (sub.equals("accept")) {
            if (args.length < 2) {
                player.sendMessage(prefix + ChatColor.RED + "Use /party accept <player>.");
                return;
            }
            Player inviter = Bukkit.getPlayer(args[1]);
            if (inviter == null || !inviter.isOnline()) {
                player.sendMessage(prefix + ChatColor.RED + "That player is not online.");
                return;
            }
            PartyInvite invite = partyInvites.get(player.getUniqueId());
            boolean isPublic = publicParties.contains(inviter.getUniqueId());
            if (!isPublic && (invite == null || !invite.sender.equals(inviter.getUniqueId()) || invite.expired())) {
                player.sendMessage(prefix + ChatColor.RED + "That party invite is gone.");
                return;
            }
            Party party = parties.get(inviter.getUniqueId());
            if (party == null) {
                player.sendMessage(prefix + ChatColor.RED + "That party no longer exists.");
                return;
            }
            if (party.members.size() >= frostSettings.getInt("SETTINGS.GENERAL.MAXIMUM-PARTY-SIZE", 32)) {
                player.sendMessage(prefix + color(firstMessage("ERROR-MESSAGES.PLAYER.PARTY-LIMIT-REACHED", "&cParty size has reached its limit")));
                return;
            }
            leaveParty(player, false);
            party.members.add(player.getUniqueId());
            parties.put(player.getUniqueId(), party);
            partyInvites.remove(player.getUniqueId());
            broadcast(party, format("party-joined", "<player>", player.getName()));
            updatePartyScoreboards(party);
            giveStateHotbar(player);
            return;
        }
        if (sub.equals("deny")) {
            if (args.length < 2) {
                player.sendMessage(prefix + ChatColor.RED + "Use /party deny <player>.");
                return;
            }
            Player inviter = Bukkit.getPlayer(args[1]);
            PartyInvite invite = partyInvites.get(player.getUniqueId());
            if (inviter == null || invite == null || !invite.sender.equals(inviter.getUniqueId()) || invite.expired()) {
                player.sendMessage(prefix + ChatColor.RED + "That party invite is gone.");
                return;
            }
            partyInvites.remove(player.getUniqueId());
            player.sendMessage(prefix + color("&cDenied &6" + inviter.getName() + "'s &cparty invite."));
            inviter.sendMessage(prefix + color("&6" + player.getName() + " &fdenied your &6party invite&f."));
            return;
        }
        if (sub.equals("join")) {
            if (args.length < 2) {
                player.sendMessage(prefix + ChatColor.RED + "Use /party join <player>.");
                return;
            }
            Player leader = Bukkit.getPlayer(args[1]);
            if (leader == null || !leader.isOnline()) {
                player.sendMessage(prefix + ChatColor.RED + "That player is not online.");
                return;
            }
            Party party = parties.get(leader.getUniqueId());
            if (party == null || !party.leader.equals(leader.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "That player is not a party leader.");
                return;
            }
            if (!publicParties.contains(leader.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "That party is not public. Ask for an invite instead.");
                return;
            }
            if (party.members.size() >= frostSettings.getInt("SETTINGS.GENERAL.MAXIMUM-PARTY-SIZE", 32)) {
                player.sendMessage(prefix + ChatColor.RED + "That party is full.");
                return;
            }
            leaveParty(player, false);
            party.members.add(player.getUniqueId());
            parties.put(player.getUniqueId(), party);
            broadcast(party, format("party-joined", "<player>", player.getName()));
            updatePartyScoreboards(party);
            giveStateHotbar(player);
            return;
        }
        if (sub.equals("leave")) {
            leaveParty(player, true);
            return;
        }
        if (sub.equals("disband")) {
            Party party = parties.get(player.getUniqueId());
            if (party == null || !party.leader.equals(player.getUniqueId())) {
                player.sendMessage(prefix + ChatColor.RED + "You are not the party leader.");
                return;
            }
            disbandParty(party);
            return;
        }
        player.sendMessage(prefix + ChatColor.RED + "Use /party create, open, close, invite, accept, deny, join, leave, disband, info.");
    }

    private void handleEventCommand(Player player, String[] args) {
        try {
            if (args.length == 0) {
                openEventsLobbyMenu(player);
                return;
            }

            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "host":
                    if (!canHostEvent(player)) {
                        player.sendMessage(noPermissionHostMessage());
                        return;
                    }
                    try {
                        openHostEventMenu(player);
                    } catch (Exception e) {
                        player.sendMessage(prefix + ChatColor.RED + "Error opening host menu: " + e.getMessage());
                        getLogger().severe("Host event menu error: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return;
            case "join":
                if (args.length < 2) {
                    player.sendMessage(prefix + ChatColor.RED + "Use /event join <player>.");
                    return;
                }
                Player host = Bukkit.getPlayer(args[1]);
                if (host == null || !host.isOnline()) {
                    player.sendMessage(prefix + ChatColor.RED + "That player is not online.");
                    return;
                }
                joinHostedEvent(player, host);
                return;
            case "start":
            case "forcestart":
                if (!canHostEvent(player)) {
                    player.sendMessage(noPermissionHostMessage());
                    return;
                }
                EventSession session = hostedEvents.get(player.getUniqueId());
                if (session != null) {
                    startHostedEventRounds(session, true);
                } else {
                    openHostEventMenu(player);
                }
                return;
            case "end":
            case "stop":
                {
                    EventSession sessionEnd = hostedEvents.get(player.getUniqueId());
                    if (sessionEnd == null) {
                        player.sendMessage(prefix + ChatColor.RED + "You are not hosting an event.");
                        return;
                    }
                    stopHostedEvent(sessionEnd, color("&c[Event] The event has been stopped by the host."), player, true);
                    return;
                }
            case "leave":
                leaveEvent(player, true);
                return;
            case "spectate":
                if (args.length < 2) {
                    player.sendMessage(prefix + ChatColor.RED + "Use /event spectate <player>.");
                    return;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(prefix + ChatColor.RED + "That player is not online.");
                    return;
                }
                // Spectate the event match if available
                EventSession spectateSession = hostedEvents.get(target.getUniqueId());
                if (spectateSession == null || !spectateSession.started) {
                    player.sendMessage(prefix + ChatColor.RED + "That player is not hosting an active event.");
                    return;
                }
                // TODO: Implement actual spectate logic for event rounds
                player.sendMessage(prefix + ChatColor.YELLOW + "Spectate is not yet implemented for events.");
                return;
            case "list":
                if (hostedEvents.isEmpty()) {
                    player.sendMessage(prefix + ChatColor.YELLOW + "No events are currently being hosted.");
                } else {
                    player.sendMessage(prefix + ChatColor.GREEN + "Active Events:");
                    for (EventSession evt : hostedEvents.values()) {
                        Player evtHost = Bukkit.getPlayer(evt.host);
                        player.sendMessage(ChatColor.GOLD + "- " + (evtHost != null ? evtHost.getName() : "Unknown") + ChatColor.GRAY + " (" + evt.kitName + ", " + evt.players.size() + "/" + evt.max + ")");
                    }
                }
                return;
            case "info":
                if (args.length < 2) {
                    player.sendMessage(prefix + ChatColor.RED + "Use /event info <player>.");
                    return;
                }
                Player infoHost = Bukkit.getPlayer(args[1]);
                if (infoHost == null || !infoHost.isOnline()) {
                    player.sendMessage(prefix + ChatColor.RED + "That player is not online.");
                    return;
                }
                EventSession infoSession = hostedEvents.get(infoHost.getUniqueId());
                if (infoSession == null) {
                    player.sendMessage(prefix + ChatColor.RED + "That player is not hosting an event.");
                    return;
                }
                player.sendMessage(prefix + ChatColor.GREEN + "Event Info for " + infoHost.getName() + ":");
                player.sendMessage(ChatColor.YELLOW + "Kit: " + infoSession.kitName + ChatColor.GRAY + ", Players: " + infoSession.players.size() + "/" + infoSession.max);
                return;
            default:
                player.sendMessage(prefix + ChatColor.RED + "Unknown event command. Use /event host, join, start, stop, leave, spectate, list, info.");
                return;
        }
        // (removed unreachable legacy code after switch-case)
        } catch (Exception e) {
            player.sendMessage(prefix + ChatColor.RED + "An error occurred: " + e.getMessage());
            getLogger().severe("Event command error for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendEventInfo(Player player, EventSession session) {
        player.sendMessage(prefix + ChatColor.GOLD + "Event Information");
        player.sendMessage(ChatColor.GRAY + "Host: " + ChatColor.YELLOW + playerName(session.host));
        player.sendMessage(ChatColor.GRAY + "Kit: " + ChatColor.YELLOW + session.kitName);
        player.sendMessage(ChatColor.GRAY + "Type: " + ChatColor.YELLOW + session.eventType);
        player.sendMessage(ChatColor.GRAY + "Players: " + ChatColor.YELLOW + session.players.size() + ChatColor.GRAY + "/" + ChatColor.YELLOW + session.max);
        player.sendMessage(ChatColor.GRAY + "Team Size: " + ChatColor.YELLOW + session.teamSize + "v" + session.teamSize);
        player.sendMessage(ChatColor.GRAY + "Map: " + ChatColor.YELLOW + formatEventMap(session.arenaPool));
        player.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.YELLOW + (session.started ? "Running" : "Waiting"));
    }

    private boolean canStartDirectMatch(Player player) {
        if (matches.containsKey(player.getUniqueId())) {
            message(player, "already-fighting");
            return false;
        }
        if (queuedKit.containsKey(player.getUniqueId())) {
            message(player, "already-queued");
            return false;
        }
        return true;
    }

    private KitData firstKit() {
        Iterator<KitData> iterator = kits.values().iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    private Party getOrCreateParty(Player player) {
        Party party = parties.get(player.getUniqueId());
        if (party != null) {
            return party;
        }
        party = new Party(player.getUniqueId());
        party.members.add(player.getUniqueId());
        parties.put(player.getUniqueId(), party);
        player.sendMessage(format("party-created"));
        giveStateHotbar(player);
        return party;
    }

    private void showPartyInfo(Player player) {
        Party party = parties.get(player.getUniqueId());
        if (party == null) {
            player.sendMessage(prefix + ChatColor.GRAY + "You are not in a party. Use /party create.");
            return;
        }
        player.sendMessage(prefix + ChatColor.AQUA + "Party members:");
        for (UUID uuid : party.members) {
            Player member = Bukkit.getPlayer(uuid);
            player.sendMessage(ChatColor.GRAY + "- " + (member == null ? uuid.toString() : member.getName()) + (party.leader.equals(uuid) ? ChatColor.YELLOW + " (leader)" : ""));
        }
    }

    private void leaveParty(Player player, boolean notify) {
        Party party = parties.get(player.getUniqueId());
        if (party == null) {
            if (notify) {
                player.sendMessage(prefix + ChatColor.RED + "You are not in a party.");
            }
            return;
        }
        if (party.leader.equals(player.getUniqueId())) {
            disbandParty(party);
            return;
        }
        party.members.remove(player.getUniqueId());
        parties.remove(player.getUniqueId());
        if (notify) {
            player.sendMessage(format("party-left", "<player>", player.getName()));
        }
        broadcast(party, format("party-left", "<player>", player.getName()));
        giveStateHotbar(player);
        updatePartyScoreboards(party);
    }

    private void disbandParty(Party party) {
        publicParties.remove(party.leader);
        for (UUID uuid : new HashSet<>(party.members)) {
            parties.remove(uuid);
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(format("party-disbanded"));
                giveStateHotbar(member);
                updateLobbyScoreboard(member);
            }
        }
        party.members.clear();
    }

    private void broadcast(Party party, String message) {
        for (UUID uuid : party.members) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(message);
            }
        }
    }

    private Player firstOnlinePartyMember(Party party, UUID except) {
        for (UUID uuid : party.members) {
            if (uuid.equals(except)) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && !matches.containsKey(uuid) && !queuedKit.containsKey(uuid)) {
                return player;
            }
        }
        return null;
    }

    private void broadcast(Match match, String message) {
        Player first = Bukkit.getPlayer(match.first);
        Player second = Bukkit.getPlayer(match.second);
        if (first != null) {
            first.sendMessage(message);
        }
        if (second != null) {
            second.sendMessage(message);
        }
        for (Map.Entry<UUID, Match> entry : spectatingMatch.entrySet()) {
            if (entry.getValue() == match) {
                Player spec = Bukkit.getPlayer(entry.getKey());
                if (spec != null) spec.sendMessage(message);
            }
        }
    }

    private UUID pollOnlineOpponent(Queue<UUID> queue, UUID self) {
        while (!queue.isEmpty()) {
            UUID uuid = queue.poll();
            if (!uuid.equals(self) && Bukkit.getPlayer(uuid) != null && !matches.containsKey(uuid)) {
                return uuid;
            }
            queuedKit.remove(uuid);
            queuedType.remove(uuid);
            queuedAt.remove(uuid);
        }
        return null;
    }

    private void startMatch(Player first, Player second, KitData kit, String type) {
        startMatch(first, second, kit, type, null);
    }

    private void startMatch(Player first, Player second, KitData kit, String type, ArenaSpawns requestedArena) {
        startMatch(first, second, kit, type, requestedArena, null, 1);
    }

    private void startMatch(Player first, Player second, KitData kit, String type, ArenaSpawns requestedArena, String displayKind, int teamSize) {
        ArenaSpawns arena = requestedArena == null ? acquireArena(kit) : acquireArena(kit, requestedArena);
        if (arena == null) {
            first.sendMessage(format("no-arena"));
            second.sendMessage(format("no-arena"));
            return;
        }

        Match match = new Match(first.getUniqueId(), second.getUniqueId(), kit, arena, normalizeQueueType(type));
        match.displayKind = displayKind == null ? "" : displayKind;
        match.teamSize = Math.max(1, teamSize);
        match.bedRespawn = (kit.bedWars || bedRespawnKits.contains(normalize(kit.name))) && !isBattleRush(match) && !kit.bridges;
        discoverMatchBeds(match);
        matches.put(first.getUniqueId(), match);
        matches.put(second.getUniqueId(), match);

        match.firstIsBlue = detectIsBlue(arena.first);
        match.secondIsBlue = !match.firstIsBlue;

        preparePlayer(first, kit, arena.first, match.firstIsBlue);
        preparePlayer(second, kit, arena.second, match.secondIsBlue);
        updateMatchScoreboard(match);
        sendMatchStartMessage(first, second, match);
        sendMatchStartMessage(second, first, match);

        match.countdownTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            private int left = 5;

            @Override
            public void run() {
                if (!matches.containsKey(first.getUniqueId()) || !matches.containsKey(second.getUniqueId())) {
                    match.cancelMatchTasks();
                    return;
                }
                if (left <= 0) {
                    match.started = true;
                    releaseMatchRespawnState(match);
                    updateMatchScoreboard(match);
                    sendTitle(first, ChatColor.GREEN + "Fight!", "");
                    sendTitle(second, ChatColor.GREEN + "Fight!", "");
                    sendTitleToSpectators(match, ChatColor.GREEN + "Fight!", "");
                    playSound(first, "fight");
                    playSound(second, "fight");
                    match.cancelCountdownTaskOnly();
                    releaseMatchRespawnState(match);
                    return;
                }
                String title = ChatColor.GOLD + String.valueOf(left);
                sendTitle(first, title, "");
                sendTitle(second, title, "");
                sendTitleToSpectators(match, title, "");
                playSound(first, "countdown");
                playSound(second, "countdown");
                left--;
            }
        }, 0L, 20L);
    }

    private ArenaSpawns acquireArena(KitData kit) {
        List<ArenaSpawns> compatible = new ArrayList<>();
        for (ArenaSpawns arena : arenas) {
            if (kit.canUseArena(arena)) {
                compatible.add(arena);
            }
        }
        if (compatible.isEmpty()) return null;

        Collections.shuffle(compatible, random);
        String kitKey = normalize(kit.name);
        String lastArena = lastArenaNamePerKit.get(kitKey);
        ArenaSpawns repeatCandidate = null;

        for (ArenaSpawns arena : compatible) {
            if (busyArenas.contains(arena)) continue;
            if (lastArena != null && arena.name.equals(lastArena)) {
                repeatCandidate = arena;
                continue;
            }
            if (busyArenas.add(arena)) {
                lastArenaNamePerKit.put(kitKey, arena.name);
                return arena;
            }
        }

        if (repeatCandidate != null && busyArenas.add(repeatCandidate)) {
            lastArenaNamePerKit.put(kitKey, repeatCandidate.name);
            return repeatCandidate;
        }

        return null;
    }

    private ArenaSpawns acquireArena(KitData kit, ArenaSpawns requestedArena) {
        List<ArenaSpawns> matches = new ArrayList<>();
        for (ArenaSpawns arena : arenas) {
            if (arena.baseName.equalsIgnoreCase(requestedArena.baseName) || arena.name.equalsIgnoreCase(requestedArena.name)) {
                if (kit.canUseArena(arena)) {
                    matches.add(arena);
                }
            }
        }
        if (!matches.isEmpty()) {
            Collections.shuffle(matches, random);
            for (ArenaSpawns arena : matches) {
                if (busyArenas.add(arena)) {
                    lastArenaNamePerKit.put(normalize(kit.name), arena.name);
                    return arena;
                }
            }
        }
        return acquireArena(kit);
    }

    private void sendMatchStartMessage(Player player, Player opponent, Match match) {
        if ("event".equals(match.displayKind)) {
            sendEventMatchStartMessage(player, opponent, match);
            return;
        }
        if ("party".equals(match.displayKind)) {
            sendCustomMatchStartMessage(player, opponent, match, "&6&lParty Fight", true);
            return;
        }
        if ("duel".equals(match.type)) {
            sendCustomMatchStartMessage(player, opponent, match, "&6&lDuel Match", false);
            return;
        }
        String path = "ranked".equals(match.type) ? "MESSAGES.MATCH.RANKED-STARTING" : "MESSAGES.MATCH.STARTING";
        String kitKey = normalize(match.kit.name);
        int rating = stats(opponent.getUniqueId()).kitStats(kitKey).elo;
        sendFrostLines(player, path,
                "<arena_name>", match.arena.baseName,
                "<map>", match.arena.baseName,
                "<kit_name>", match.kit.name,
                "<kit>", match.kit.name,
                "<opponent_name>", opponent.getName(),
                "<opponent>", opponent.getName(),
                "<opponent_ping>", String.valueOf(getPing(opponent)),
                "<opponent_rating>", String.valueOf(rating),
                "<queue_type>", displayQueueType(match.type),
                "<queue>", displayQueueType(match.type));
    }

    private void sendEventMatchStartMessage(Player player, Player opponent, Match match) {
        EventSession session = match.eventHost == null ? null : activeEvents.get(match.eventHost);
        String players = session == null ? "2" : String.valueOf(session.players.size());
        String maxPlayers = session == null ? "2" : String.valueOf(session.max);
        List<String> lines = Arrays.asList(
                " ",
                "&6&lEvent",
                "&f • Kit: &6<kit_name>",
                "&f • Type: &61v1",
                "&f • Players: &6<players>&f/&6<max_players>",
                "&f • Map: &6<arena_name>",
                "&f • Opponent: &6<opponent_name>",
                "&f • Ping: &6<opponent_ping>ms",
                " "
        );
        for (String line : lines) {
            player.sendMessage(color(line
                    .replace("<kit_name>", match.kit.name)
                    .replace("<players>", players)
                    .replace("<max_players>", maxPlayers)
                    .replace("<arena_name>", match.arena.baseName)
                    .replace("<opponent_name>", opponent == null ? "?" : opponent.getName())
                    .replace("<opponent_ping>", opponent == null ? "0" : String.valueOf(getPing(opponent)))));
        }
    }

    private void sendCustomMatchStartMessage(Player player, Player opponent, Match match, String title, boolean includePartySize) {
        List<String> lines = new ArrayList<>();
        lines.add(" ");
        lines.add(title);
        lines.add("&f • Map: &6<arena_name>");
        lines.add("&f • Kit: &6<kit_name>");
        if (includePartySize) {
            lines.add("&f • Type: &6<team_size>v<team_size>");
        }
        lines.add("&f • Opponent: &6<opponent_name>");
        lines.add("&f • Ping: &6<opponent_ping>ms");
        lines.add(" ");
        for (String line : lines) {
            player.sendMessage(color(line
                    .replace("<arena_name>", match.arena.baseName)
                    .replace("<kit_name>", match.kit.name)
                    .replace("<team_size>", String.valueOf(match.teamSize))
                    .replace("<opponent_name>", opponent == null ? "?" : opponent.getName())
                    .replace("<opponent_ping>", opponent == null ? "0" : String.valueOf(getPing(opponent)))));
        }
    }

    private void preparePlayer(Player player, KitData kit, Location location, boolean blueTeam) {
        leaveQueue(player, false);
        resetPlayer(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setMaximumNoDamageTicks(kit.combo ? 0 : Math.max(0, kit.damageTicks));
        player.setNoDamageTicks(0);
        if (kit.boxing) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false), true);
        }
        pluginTeleport(player, location);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.showPlayer(player);
                    player.showPlayer(online);
                }
            }
        }, 1L);
        giveKit(player, kit, blueTeam);
        // Add health display below nametag for damage-enabled gamemodes (excl. Boxing, Sumo, BattleRush, StickFight, Spleef)
        boolean isDamageMode = !kit.boxing && !kit.sumo 
                && !kit.name.toLowerCase(Locale.ROOT).contains("battlerush") 
                && !kit.name.toLowerCase(Locale.ROOT).contains("stickfight") 
                && !kit.name.toLowerCase(Locale.ROOT).contains("spleef");
        if (isDamageMode) {
            setupHealthDisplay(player);
        } else {
            removeHealthDisplay(player);
        }
    }

    private void giveKit(Player player, KitData kit) {
        giveKit(player, kit, false);
    }

    private void giveKit(Player player, KitData kit, boolean blueTeam) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(null);
        ItemStack[] contents = customKitContents(player.getUniqueId(), kit);
        inventory.setContents(recolorItems(cloneItems(contents, 36), blueTeam));
        inventory.setArmorContents(recolorItems(cloneItems(kit.armor, 4), blueTeam));
        player.updateInventory();
    }

    private void setupHealthDisplay(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        Scoreboard board = player.getScoreboard();
        if (board == null || board == manager.getMainScoreboard()) {
            board = manager.getNewScoreboard();
            player.setScoreboard(board);
        }
        Objective healthObj = board.getObjective("health");
        if (healthObj == null) {
            healthObj = board.registerNewObjective("health", "health");
        }
        healthObj.setDisplaySlot(DisplaySlot.BELOW_NAME);
        healthObj.setDisplayName(ChatColor.RED + "❤");
        
        // Pre-initialize scores for the dueling players so it displays instantly
        healthObj.getScore(player.getName()).setScore((int) player.getHealth());
        Match match = matches.get(player.getUniqueId());
        if (match != null) {
            Player opponent = Bukkit.getPlayer(match.other(player.getUniqueId()));
            if (opponent != null) {
                healthObj.getScore(opponent.getName()).setScore((int) opponent.getHealth());
            }
        }
    }

    private void removeHealthDisplay(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board != null) {
            Objective healthObj = board.getObjective("health");
            if (healthObj != null) {
                healthObj.unregister();
            }
        }
    }

    private ItemStack[] customKitContents(UUID playerId, KitData kit) {
        List<?> saved = kitLayoutsConfig == null ? null : kitLayoutsConfig.getList("players." + playerId + "." + normalize(kit.name) + ".contents");
        if (saved == null || saved.isEmpty()) {
            return cloneItems(kit.contents, 36);
        }
        return readItems(saved, 36);
    }

    private void saveLayout(Player player, KitData kit, Inventory inventory) {
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = inventory.getItem(i);
            contents[i] = item == null ? null : item.clone();
        }
        kitLayoutsConfig.set("players." + player.getUniqueId() + "." + normalize(kit.name) + ".contents", Arrays.asList(contents));
        saveYaml(kitLayoutsConfig, kitLayoutsFile, "kit layouts");
    }

    private void resetLayout(Player player, KitData kit) {
        kitLayoutsConfig.set("players." + player.getUniqueId() + "." + normalize(kit.name), null);
        saveYaml(kitLayoutsConfig, kitLayoutsFile, "kit layouts");
    }

    private void updateMatchScoreboard(Match match) {
        Player first = Bukkit.getPlayer(match.first);
        Player second = Bukkit.getPlayer(match.second);
        if (first != null) {
            setMatchScoreboard(first, second, match);
        }
        if (second != null) {
            setMatchScoreboard(second, first, match);
        }
        Player specTarget = first != null ? first : second;
        if (specTarget != null) {
            for (Map.Entry<UUID, Match> entry : spectatingMatch.entrySet()) {
                if (entry.getValue() == match) {
                    Player spec = Bukkit.getPlayer(entry.getKey());
                    if (spec != null) {
                        setMatchScoreboard(spec, specTarget, match);
                    }
                }
            }
        }
    }

    private void setMatchScoreboard(Player player, Player opponent, Match match) {
        if (!scoreboardEnabled || !settings(player).scoreboard) {
            clearPracticeScoreboard(player);
            return;
        }
        setScoreboardContext(player, "MATCH");
        List<String> lines = new ArrayList<>();
        boolean isSpectator = spectatingMatch.containsKey(player.getUniqueId());
        for (String line : frostScoreboard.getStringList(settings(player).pingOnScoreboard ? "SCOREBOARD.IN-MATCH-PING" : "SCOREBOARD.IN-MATCH")) {
            if (isSpectator) {
                if (line.contains("Fighting:")) {
                    ChatColor colorA = match.isBlue(match.first) ? ChatColor.BLUE : ChatColor.RED;
                    ChatColor colorB = match.isBlue(match.second) ? ChatColor.BLUE : ChatColor.RED;
                    line = line.replace("Fighting: &6<opponent_name>", colorA + playerName(match.first) + " &fvs " + colorB + playerName(match.second));
                }
                if (line.contains("Your Ping:")) {
                    ChatColor colorA = match.isBlue(match.first) ? ChatColor.BLUE : ChatColor.RED;
                    line = line.replace("Your Ping:", colorA + playerName(match.first) + "&f's Ping:")
                               .replace("<your_ping>", String.valueOf(getPing(Bukkit.getPlayer(match.first))));
                }
                if (line.contains("Their Ping:")) {
                    ChatColor colorB = match.isBlue(match.second) ? ChatColor.BLUE : ChatColor.RED;
                    line = line.replace("Their Ping:", colorB + playerName(match.second) + "&f's Ping:")
                               .replace("<opponent_ping>", String.valueOf(getPing(Bukkit.getPlayer(match.second))));
                }
            }
            expandScoreboardLine(lines, line, player, opponent, match);
        }
        setScoreboard(player, lines);
    }

    private void markScoreboardDirty(UUID uuid) {
        if (uuid != null) {
            scoreboardDirty.add(uuid);
            scheduleDirtyScoreboardRefresh();
        }
    }

    private void markScoreboardDirty(Player player) {
        if (player != null) {
            scoreboardDirty.add(player.getUniqueId());
            scheduleDirtyScoreboardRefresh();
        }
    }

    private void scheduleDirtyScoreboardRefresh() {
        if (scoreboardDirtyRefreshTask != null) {
            return;
        }
        scoreboardDirtyRefreshTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            scoreboardDirtyRefreshTask = null;
            refreshScoreboards(false);
        }, 1L);
    }

    private void refreshScoreboardsTick() {
        // Every tick: refresh everyone, but only apply diffs (prevents flicker and keeps timings correct).
        refreshScoreboards(true);
    }

    /**
     * @param full if true, refreshes everyone (matches + lobby); if false, only refreshes dirty players.
     */
    private void refreshScoreboards(boolean full) {
        // 1) Always handle "dirty" players immediately (state changes, match end/start, etc.)
        Set<UUID> dirtyNow = new HashSet<>(scoreboardDirty);
        scoreboardDirty.clear();
        Set<Match> updatedMatches = new HashSet<>();

        for (UUID uuid : dirtyNow) {
            Match match = matches.get(uuid);
            if (match == null) {
                match = spectatingMatch.get(uuid);
            }
            if (match != null && !match.ending) {
                if (updatedMatches.add(match)) {
                    updateMatchScoreboard(match);
                }
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            EventSession event = activeEvents.get(player.getUniqueId());
            if (event != null) {
                updateEventScoreboard(player, event, false);
            } else {
                updateLobbyScoreboard(player);
            }
        }

        if (!full) {
            return;
        }

        // 2) Periodic full updates (every 20 ticks by default)
        Set<Match> refreshed = new HashSet<>(matches.values());
        for (Match match : refreshed) {
            if (!match.ending && updatedMatches.add(match)) {
                updateMatchScoreboard(match);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!matches.containsKey(player.getUniqueId()) && !spectatingMatch.containsKey(player.getUniqueId())) {
                EventSession event = activeEvents.get(player.getUniqueId());
                if (event != null) {
                    updateEventScoreboard(player, event, false);
                } else {
                    updateLobbyScoreboard(player);
                }
            }
        }
    }

    private void updateEventScoreboard(Player player, EventSession session, boolean playing) {
        if (!scoreboardEnabled || !settings(player).scoreboard || shouldSuppressPracticeScoreboard(player)) {
            clearPracticeScoreboard(player);
            return;
        }
        setScoreboardContext(player, "EVENT");
        String path = "SCOREBOARD." + session.name + "." + (playing ? "PLAYING" : "WAITING");
        List<String> configured = frostEventScoreboard.getStringList(path);
        if (configured.isEmpty()) {
            updateLobbyScoreboard(player);
            return;
        }
        List<String> lines = new ArrayList<>();
        for (String line : configured) {
            lines.add(color(replaceEventPlaceholders(line, player, session)));
        }
        setScoreboard(player, lines);
    }

    private String replaceEventPlaceholders(String line, Player player, EventSession session) {
        return line
                .replace("<event_host>", playerName(session.host))
                .replace("<event_joined>", String.valueOf(session.players.size()))
                .replace("<event_max>", String.valueOf(session.max))
                .replace("<event_countdown>", Math.max(0L, 30L - ((System.currentTimeMillis() - session.startedAt) / 1000L)) + "s")
                .replace("<alive_players>", String.valueOf(session.alivePlayers.size()))
                .replace("<event_kit>", session.kitName == null ? "Default" : session.kitName)
                .replace("<current_round>", String.valueOf(Math.max(1, session.round - (session.currentMatchFirst == null ? 0 : 1))))
                .replace("<playerA>", session.currentMatchFirst == null ? player.getName() : playerName(session.currentMatchFirst))
                .replace("<playerB>", session.currentMatchSecond == null ? "Waiting" : playerName(session.currentMatchSecond))
                .replace("<playerA_ping>", String.valueOf(getPing(player)))
                .replace("<playerB_ping>", session.currentMatchSecond == null ? "0" : String.valueOf(getPing(Bukkit.getPlayer(session.currentMatchSecond))))
                .replace("<player_score>", "0")
                .replace("<first_place>", player.getName())
                .replace("<first_place_score>", "0")
                .replace("<second_place>", "-")
                .replace("<second_place_score>", "0")
                .replace("<third_place>", "-")
                .replace("<third_place_score>", "0")
                .replace("<tnt_time>", "30")
                .replace("<checkpoint_id>", "0")
                .replace("<next_round>", "0")
                .replace("<current_map>", "1")
                .replace("<maps_total>", "1")
                .replace("<status>", "Waiting");
    }

    private String bedText(boolean alive) {
        return alive ? color("&a&l\u2714") : color("&c&lX");
    }

    private void updateLobbyScoreboard(Player player) {
        if (!scoreboardEnabled || !settings(player).scoreboard || matches.containsKey(player.getUniqueId()) || spectatingMatch.containsKey(player.getUniqueId()) || shouldSuppressPracticeScoreboard(player)) {
            if (!matches.containsKey(player.getUniqueId()) && !spectatingMatch.containsKey(player.getUniqueId())) {
                clearPracticeScoreboard(player);
            }
            return;
        }
        setScoreboardContext(player, "LOBBY");
        EventSession event = activeEvents.get(player.getUniqueId());
        if (event != null && !"FFA".equals(event.name)) {
            updateEventScoreboard(player, event, false);
            return;
        }
        String kit = queuedKit.get(player.getUniqueId());
        String type = queuedType.getOrDefault(player.getUniqueId(), "unranked");
        String path = parties.containsKey(player.getUniqueId()) && kit == null ? "SCOREBOARD.PARTY-IN-LOBBY"
                : kit == null ? "SCOREBOARD.IN-LOBBY"
                : "ranked".equals(type) ? "SCOREBOARD.IN-RANKED-QUEUE"
                : "premium".equals(type) ? "SCOREBOARD.IN-PREMIUM-QUEUE"
                : "SCOREBOARD.IN-UNRANKED-QUEUE";
        List<String> lines = new ArrayList<>();
        for (String line : frostScoreboard.getStringList(path)) {
            lines.add(color(replaceCommonPlaceholders(line, player, kit)));
        }
        setScoreboard(player, lines);
    }

    private void setScoreboard(Player player, List<String> lines) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Scoreboard board = practiceScoreboards.get(uuid);
        if (board == null) {
            board = manager.getNewScoreboard();
            practiceScoreboards.put(uuid, board);
        }
        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }

        // --- Dynamic Health Display Under Nametag ---
        Match match = matches.get(player.getUniqueId());
        boolean shouldHaveHealth = false;
        if (match != null && match.started && match.kit != null) {
            KitData kit = match.kit;
            shouldHaveHealth = !kit.boxing && !kit.sumo 
                    && !kit.name.toLowerCase(Locale.ROOT).contains("battlerush") 
                    && !kit.name.toLowerCase(Locale.ROOT).contains("stickfight") 
                    && !kit.name.toLowerCase(Locale.ROOT).contains("spleef");
        }
        Boolean hadHealth = scoreboardHadHealth.get(uuid);
        if (shouldHaveHealth) {
            Objective healthObj = board.getObjective("health");
            if (healthObj == null) {
                healthObj = board.registerNewObjective("health", "health");
            }
            if (hadHealth == null || !hadHealth) {
                healthObj.setDisplaySlot(DisplaySlot.BELOW_NAME);
                healthObj.setDisplayName(ChatColor.RED + "❤");
            }
            
            // Sync current health scores for all players in the match
            Player p1 = Bukkit.getPlayer(match.first);
            if (p1 != null) {
                int h = (int) p1.getHealth();
                Integer last = lastBelowNameHealth.get(p1.getUniqueId());
                if (last == null || last != h) {
                    healthObj.getScore(p1.getName()).setScore(h);
                    lastBelowNameHealth.put(p1.getUniqueId(), h);
                }
            }
            Player p2 = Bukkit.getPlayer(match.second);
            if (p2 != null) {
                int h = (int) p2.getHealth();
                Integer last = lastBelowNameHealth.get(p2.getUniqueId());
                if (last == null || last != h) {
                    healthObj.getScore(p2.getName()).setScore(h);
                    lastBelowNameHealth.put(p2.getUniqueId(), h);
                }
            }
        } else {
            // Avoid clearing the slot every update (can still cause flicker). Only clear on transition.
            if (hadHealth != null && hadHealth) {
                try {
                    board.clearSlot(DisplaySlot.BELOW_NAME);
                } catch (Exception ignored) {}
            }
        }
        scoreboardHadHealth.put(uuid, shouldHaveHealth);
        // --------------------------------------------

        Objective objective = board.getObjective("practice");
        if (objective == null) {
            objective = board.registerNewObjective("practice", "dummy");
        }
        // Setting the display slot every tick can trigger client-side flicker on some 1.8 clients.
        // Only set it when needed.
        if (objective.getDisplaySlot() != DisplaySlot.SIDEBAR) {
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        if (scoreboardTitle != null && !scoreboardTitle.equals(objective.getDisplayName())) {
            objective.setDisplayName(scoreboardTitle);
        }

        // Build rendered lines (max 15) and keep stable team entries to avoid flicker.
        int blanks = 0;
        List<String> rendered = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                if (rendered.size() >= 15) break;
                String value = cleanScoreboardDots(color(applyExternalPlaceholders(player, applyPracticePlaceholders(player, line))));
                if (ChatColor.stripColor(value).trim().isEmpty()) {
                    value = repeat(" ", ++blanks);
                }
                rendered.add(value);
            }
        }

        // Don't force extra filler lines; keep exactly what is configured/rendered to avoid visible blank rows.
        int newCount = Math.max(1, Math.min(15, rendered.size()));
        int oldCount = scoreboardLineCount.getOrDefault(uuid, 0);
        int targetCount = newCount;

        // If the scoreboard shrank, remove leftover scores so old rows disappear instead of turning into blanks.
        if (oldCount > targetCount) {
            try {
                for (int i = targetCount; i < oldCount; i++) {
                    board.resetScores(sidebarEntry(i));
                }
            } catch (Exception ignored) {}
        }

        List<String> old = scoreboardEntries.get(uuid);
        boolean firstInit = (old == null || old.isEmpty() || oldCount == 0);
        boolean countChanged = oldCount != targetCount;

        for (int i = 0; i < targetCount; i++) {
            String entry = sidebarEntry(i);
            String teamName = "sb_" + i;
            org.bukkit.scoreboard.Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            if (!team.hasEntry(entry)) {
                try {
                    team.addEntry(entry);
                } catch (Exception ignored) {}
            }

            String newText = rendered.get(i);
            String oldText = old != null && old.size() > i ? old.get(i) : null;
            if (!firstInit && oldText != null && oldText.equals(newText)) {
                // Skip updates if line didn't change (reduces packets and helps perceived smoothness)
            } else {
                String[] parts = splitSidebarText(newText);
                try {
                    team.setPrefix(parts[0]);
                    team.setSuffix(parts[1]);
                } catch (Exception ignored) {}
            }

            // Only set scores when needed; scores are constant (15..1) and don't need to be resent every tick.
            if (firstInit || countChanged) {
                try {
                    objective.getScore(entry).setScore(targetCount - i);
                } catch (Exception ignored) {}
            }
        }

        scoreboardEntries.put(uuid, rendered);
        scoreboardLineCount.put(uuid, targetCount);
    }

    private String sidebarEntry(int index) {
        // Use color codes as stable unique entries (15 max)
        ChatColor[] values = ChatColor.values();
        if (index < 0) index = 0;
        if (index >= values.length) index = values.length - 1;
        return values[index].toString();
    }

    private String[] splitSidebarText(String text) {
        if (text == null) {
            return new String[]{"", ""};
        }
        // Teams support 16-char prefix + 16-char suffix. Keep color continuity.
        if (text.length() <= 16) {
            return new String[]{text, ""};
        }
        String prefix = text.substring(0, 16);
        String rest = text.substring(16);
        if (prefix.endsWith("§")) {
            prefix = text.substring(0, 15);
            rest = text.substring(15);
        }
        String lastColors = ChatColor.getLastColors(prefix);
        String suffix = lastColors + rest;
        if (suffix.length() > 16) {
            suffix = suffix.substring(0, 16);
        }
        if (suffix.endsWith("§")) {
            suffix = suffix.substring(0, 15);
        }
        return new String[]{prefix, suffix};
    }

    private void setScoreboardContext(Player player, String context) {
        if (player == null || context == null) return;
        UUID uuid = player.getUniqueId();
        String prev = scoreboardContext.put(uuid, context);
        if (prev == null || prev.equals(context)) {
            return;
        }

        // Context changed (e.g., lobby -> match). Do NOT reset scores here; that "remove/add" cycle is a common
        // source of sidebar flicker on 1.8 clients. Instead, just clear our cached rendered lines so the next
        // refresh will rewrite all prefixes/suffixes in-place.
        scoreboardEntries.remove(uuid);
        // Keep scoreboardLineCount so setScoreboard() can properly clear leftover rows when the next context has
        // fewer lines (prevents "ghost" lines remaining after match end).
    }

    private void clearPracticeScoreboard(Player player) {
        Scoreboard current = player.getScoreboard();
        if (current == null) return;

        Objective objective = current.getObjective(DisplaySlot.SIDEBAR);
        if (objective == null || !"practice".equalsIgnoreCase(objective.getName())) return;

        // Clear slots first (prevents the "flash" some clients show on scoreboard swap)
        try {
            current.clearSlot(DisplaySlot.SIDEBAR);
        } catch (Exception ignored) {}
        try {
            current.clearSlot(DisplaySlot.BELOW_NAME);
        } catch (Exception ignored) {}

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
        UUID uuid = player.getUniqueId();
        scoreboardEntries.remove(uuid);
        scoreboardLineCount.remove(uuid);
        scoreboardHadHealth.remove(uuid);
        scoreboardContext.remove(uuid);
        practiceScoreboards.remove(uuid);
        lastBelowNameHealth.remove(uuid);
    }

    private void expandScoreboardLine(List<String> lines, String line, Player player, Player opponent, Match match) {
        if (line.equals("<isBedWars>")) {
            if (match.bedRespawn && !isMlgRush(match)) {
                for (String extra : frostScoreboard.getStringList("SCOREBOARD.BEDWARS")) {
                    lines.add(replaceMatchPlaceholders(extra, player, opponent, match));
                }
            }
            return;
        }
        if (line.equals("<isBoxing>")) {
            addKitScoreboard(lines, "BOXING", match, player, opponent, normalize(match.kit.name).contains("boxing"));
            return;
        }
        if (line.equals("<isBridges>")) {
            addKitScoreboard(lines, "BRIDGES", match, player, opponent, normalize(match.kit.name).contains("bridge"));
            return;
        }
        if (line.equals("<isBattleRush>")) {
            addKitScoreboard(lines, "BATTLERUSH", match, player, opponent, normalize(match.kit.name).contains("battlerush"));
            return;
        }
        if (line.equals("<isMlgRush>")) {
            addKitScoreboard(lines, "MLGRUSH", match, player, opponent, normalize(match.kit.name).contains("mlgrush"));
            return;
        }
        if (line.equals("<isStickFight>")) {
            addKitScoreboard(lines, "STICKFIGHT", match, player, opponent, normalize(match.kit.name).contains("stickfight"));
            return;
        }
        if (line.startsWith("<is")) {
            return;
        }
        lines.add(replaceMatchPlaceholders(line, player, opponent, match));
    }

    private void addKitScoreboard(List<String> lines, String key, Match match, Player player, Player opponent, boolean enabled) {
        if (!enabled) {
            return;
        }
        for (String extra : frostScoreboard.getStringList("SCOREBOARD." + key)) {
            lines.add(replaceMatchPlaceholders(extra, player, opponent, match));
        }
    }

    private String replaceCommonPlaceholders(String line, Player player, String queuedKitName) {
        Party party = parties.get(player.getUniqueId());
        KitData kit = queuedKitName == null ? null : kits.get(queuedKitName);
        String replaced = line
                .replace("<online_players>", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("<fighting>", String.valueOf(matches.size() / 2))
                .replace("<queued_type>", displayQueueType(queuedType.getOrDefault(player.getUniqueId(), "unranked")))
                .replace("<queued_kit>", kit == null ? "" : kit.name)
                .replace("<queued_time>", queuedDuration(player.getUniqueId()))
                .replace("<party_leader>", party == null ? "" : playerName(party.leader))
                .replace("<party_members>", party == null ? "0" : String.valueOf(party.members.size()))
                .replace("<party_max>", String.valueOf(frostSettings.getInt("SETTINGS.GENERAL.PARTY-LIMIT-BY-DEFAULT", 12)))
                .replace("<ping>", String.valueOf(getPing(player)));
        replaced = applyExternalPlaceholders(player, applyPracticePlaceholders(player, replaced));
        return replaced
                .replace("%player_name%", player.getName())
                .replace("%luckperms_primary_group_name%", "Default");
    }

    private String applyPracticePlaceholders(Player player, String text) {
        PlayerStats stats = stats(player.getUniqueId());
        return text
                .replace("%wins%", String.valueOf(stats.wins))
                .replace("<wins>", String.valueOf(stats.wins))
                .replace("%losses%", String.valueOf(stats.losses))
                .replace("<losses>", String.valueOf(stats.losses))
                .replace("%winstreak%", String.valueOf(stats.winstreak))
                .replace("<winstreak>", String.valueOf(stats.winstreak))
                .replace("%best_winstreak%", String.valueOf(stats.bestWinstreak))
                .replace("<best_winstreak>", String.valueOf(stats.bestWinstreak));
    }

    private String applyExternalPlaceholders(Player player, String text) {
        if (text == null || text.indexOf('%') < 0 || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return text;
        }
        try {
            Class<?> placeholderApi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            for (Method method : placeholderApi.getMethods()) {
                if (!"setPlaceholders".equals(method.getName()) || method.getParameterTypes().length != 2) {
                    continue;
                }
                if (!method.getParameterTypes()[1].isAssignableFrom(String.class)) {
                    continue;
                }
                Object value = method.invoke(null, player, text);
                if (value instanceof String) {
                    return (String) value;
                }
            }
        } catch (Exception ignored) {
            return text;
        }
        return text;
    }

    private String replaceMatchPlaceholders(String line, Player player, Player opponent, Match match) {
        boolean first = match.first.equals(player.getUniqueId());
        
        String rBedStr;
        String bBedStr;
        String bRedStr;
        if (isMlgRush(match)) {
            int max = match.kit.goals > 0 ? match.kit.goals : 3;
            int blueScore = match.firstIsBlue ? match.bridgeScore(match.first) : match.bridgeScore(match.second);
            int redScore = match.firstIsBlue ? match.bridgeScore(match.second) : match.bridgeScore(match.first);
            rBedStr = mlgRushBeds(blueScore, max);
            bBedStr = mlgRushBeds(redScore, max);
            bRedStr = bBedStr;
        } else {
            // rBed = red team's bed, bBed = blue team's bed (based on actual colour assignment)
            boolean playerIsBlue = first ? match.firstIsBlue : match.secondIsBlue;
            boolean playerBedAlive = first ? match.firstBedAlive : match.secondBedAlive;
            boolean opponentBedAlive = first ? match.secondBedAlive : match.firstBedAlive;
            if (playerIsBlue) {
                // player is blue → bBed = player's bed, rBed = opponent's bed
                bBedStr = bedText(playerBedAlive);
                rBedStr = bedText(opponentBedAlive);
            } else {
                // player is red → rBed = player's bed, bBed = opponent's bed
                rBedStr = bedText(playerBedAlive);
                bBedStr = bedText(opponentBedAlive);
            }
            bRedStr = bBedStr;
        }

        return line
                .replace("<opponent_name>", opponent == null ? "?" : opponent.getName())
                .replace("<match_duration>", match.duration())
                .replace("<kitName>", match.kit.name)
                .replace("<kit_name>", match.kit.name)
                .replace("<arenaName>", match.arena.name)
                .replace("<arena_name>", match.arena.name)
                .replace("<your_ping>", String.valueOf(getPing(player)))
                .replace("<opponent_ping>", opponent == null ? "0" : String.valueOf(getPing(opponent)))
                .replace("<rBed>", rBedStr)
                .replace("<bBed>", bBedStr)
                .replace("<bRed>", bRedStr)
                .replace("<rGoal>", String.valueOf(first ? match.bridgeScore(match.first) : match.bridgeScore(match.second)))
                .replace("<bGoal>", String.valueOf(first ? match.bridgeScore(match.second) : match.bridgeScore(match.first)))
                .replace("<goals>", String.valueOf(match.bridgeScore(player.getUniqueId())))
                .replace("<kills>", "0")
                .replace("<hits>", String.valueOf(match.hits(player.getUniqueId())))
                .replace("<your_hits>", String.valueOf(match.hits(player.getUniqueId())))
                .replace("<opponent_hits>", String.valueOf(match.hits(opponent == null ? match.other(player.getUniqueId()) : opponent.getUniqueId())))
                .replace("<combo>", "")
                .replace("<rLives>", "1")
                .replace("<bLives>", "1")
                .replace("<playerA>", playerName(match.first))
                .replace("<playerB>", playerName(match.second))
                .replace("<isRanked>", "ranked".equals(match.type) ? "Yes" : "No");
    }

    private String playerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player == null ? "Unknown" : player.getName();
    }

    private String repeat(String text, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(text);
        }
        return builder.toString();
    }

    private void updatePartyScoreboards(Party party) {
        for (UUID uuid : party.members) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                updateLobbyScoreboard(member);
            }
        }
    }

    private void resetScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
        scoreboardEntries.remove(player.getUniqueId());
    }

    private String scoreEntry(String text, List<String> existing) {
        String value = text.length() > 40 ? text.substring(0, 40) : text;
        int duplicate = 0;
        while (existing.contains(value)) {
            ChatColor[] colors = ChatColor.values();
            String suffix = colors[duplicate++ % colors.length].toString() + ChatColor.RESET;
            value = value.length() + suffix.length() > 40
                    ? value.substring(0, 40 - suffix.length()) + suffix
                    : value + suffix;
        }
        return value;
    }

    private String cleanScoreboardDots(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replace("●", "")
                .replace("•", "")
                .replace("⬤", "")
                .replace("â¬¤", "")
                .replaceAll("(?i)(?:&[0-9A-FK-OR])*[●•⬤]", "");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && spectatingMatch.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (activeEvents.containsKey(player.getUniqueId()) && !matches.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        Match match = matches.get(player.getUniqueId());
        if (match == null) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            event.setCancelled(true);
            return;
        }
        if (!match.started || match.ending) {
            event.setCancelled(true);
            return;
        }
        if (respawningPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) event;
            Player attacker = null;
            if (damageByEntity.getDamager() instanceof Player) {
                attacker = (Player) damageByEntity.getDamager();
            } else if (damageByEntity.getDamager() instanceof Projectile && ((Projectile) damageByEntity.getDamager()).getShooter() instanceof Player) {
                attacker = (Player) ((Projectile) damageByEntity.getDamager()).getShooter();
            }
            if (attacker != null && match.isEnemy(attacker.getUniqueId(), player.getUniqueId())) {
                lastDamager.put(player.getUniqueId(), attacker.getUniqueId());
                lastDamageTime.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) event;
            Player attacker = null;
            if (damageByEntity.getDamager() instanceof Player) {
                attacker = (Player) damageByEntity.getDamager();
            } else if (damageByEntity.getDamager() instanceof Projectile && ((Projectile) damageByEntity.getDamager()).getShooter() instanceof Player) {
                attacker = (Player) ((Projectile) damageByEntity.getDamager()).getShooter();
            }
            if (attacker != null) {
                if (respawningPlayers.contains(attacker.getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
                if (!match.isEnemy(attacker.getUniqueId(), player.getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
                clearSpawnProtection(attacker.getUniqueId());
                if (hasSpawnProtection(player.getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
                event.setCancelled(false);
                if (match.kit.combo) {
                    player.setMaximumNoDamageTicks(0);
                    player.setNoDamageTicks(0);
                }
            }
        }
        if (hasSpawnProtection(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (isNoDamageScoringKit(match) && event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) event;
                Entity damager = damageByEntity.getDamager();
                if (damager instanceof Player || (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player)) {
                    event.setCancelled(false);
                    damageByEntity.setDamage(0.0D);
                    return;
                }
            }
            event.setCancelled(true);
            return;
        }
        if (match.kit.noFall && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }
        if (isSpleef(match) && event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
            if (edbe.getDamager() instanceof Snowball) {
                event.setCancelled(false);
                edbe.setDamage(0.0D);
            } else {
                event.setCancelled(true);
            }
            return;
        }
        if ((match.kit.boxing || match.kit.sumo) && event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) event;
            if (damageByEntity.getDamager() instanceof Player) {
                event.setCancelled(false);
                damageByEntity.setDamage(0.0D);
                handlePracticeHit((Player) damageByEntity.getDamager(), player, match);
            }
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID || player.getHealth() - event.getFinalDamage() <= 0.0D) {
            event.setCancelled(true);
            ChatColor playerColor = match.isBlue(player.getUniqueId()) ? ChatColor.BLUE : ChatColor.RED;
            String reason;
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                UUID victimId = player.getUniqueId();
                UUID killerId = lastDamager.get(victimId);
                Long time = lastDamageTime.get(victimId);
                if (killerId != null && time != null && (System.currentTimeMillis() - time <= 15000L)) {
                    Player killer = Bukkit.getPlayer(killerId);
                    if (killer != null && killer.isOnline()) {
                        ChatColor killerColor = match.isBlue(killerId) ? ChatColor.BLUE : ChatColor.RED;
                        reason = color(resolveMessage("void-killed"))
                                .replace("<victim>", playerColor + player.getName() + ChatColor.GOLD)
                                .replace("<killer>", killerColor + killer.getName() + ChatColor.GOLD);
                    } else {
                        reason = color(resolveMessage("void-death")).replace("<player>", playerColor + player.getName() + ChatColor.GOLD);
                    }
                } else {
                    reason = color(resolveMessage("void-death")).replace("<player>", playerColor + player.getName() + ChatColor.GOLD);
                }
            } else {
                reason = color(resolveMessage("match-death")).replace("<victim>", playerColor + player.getName() + ChatColor.GOLD);
            }
            if (event instanceof EntityDamageByEntityEvent) {
                Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                if (damager instanceof Player) {
                    ChatColor killerColor = match.isBlue(damager.getUniqueId()) ? ChatColor.BLUE : ChatColor.RED;
                    reason = color(resolveMessage("match-killed"))
                            .replace("<victim>", playerColor + player.getName() + ChatColor.GOLD)
                            .replace("<killer>", killerColor + damager.getName() + ChatColor.GOLD);
                } else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
                    Player shooter = (Player) ((Projectile) damager).getShooter();
                    ChatColor killerColor = match.isBlue(shooter.getUniqueId()) ? ChatColor.BLUE : ChatColor.RED;
                    reason = color(resolveMessage("match-killed"))
                            .replace("<victim>", playerColor + player.getName() + ChatColor.GOLD)
                            .replace("<killer>", killerColor + shooter.getName() + ChatColor.GOLD);
                }
            }
            handlePlayerDeath(player, match, reason);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamageMonitor(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Bukkit.getScheduler().runTask(this, () -> updatePlayerHealthForMatch(player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRegenMonitor(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Bukkit.getScheduler().runTask(this, () -> updatePlayerHealthForMatch(player));
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Snowball) {
            Snowball s = (Snowball) event.getEntity();
            if (s.getShooter() instanceof Player) {
                Player p = (Player) s.getShooter();
                Match match = matches.get(p.getUniqueId());
                if (isSpleef(match)) {
                    Location loc = s.getLocation();
                    Block closest = null;
                    double bestDist = 4.0;
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                Block b = loc.clone().add(x, y, z).getBlock();
                                if (b.getType() == Material.SNOW_BLOCK) {
                                    double d = b.getLocation().add(0.5, 0.5, 0.5).distanceSquared(loc);
                                    if (d < bestDist) {
                                        bestDist = d;
                                        closest = b;
                                    }
                                }
                            }
                        }
                    }
                    if (closest != null) {
                        trackArenaBlock(match, closest);
                        closest.setType(Material.AIR);
                        p.getInventory().addItem(new ItemStack(Material.SNOW_BALL));
                        p.updateInventory();
                        closest.getWorld().playEffect(closest.getLocation(), org.bukkit.Effect.STEP_SOUND, Material.SNOW_BLOCK.getId());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        event.setCancelled(true);
    }



    private void handlePracticeHit(Player attacker, Player victim, Match match) {
        if (!matches.containsKey(attacker.getUniqueId()) || !match.isEnemy(attacker.getUniqueId(), victim.getUniqueId())) {
            return;
        }
        if (match.kit.boxing) {
            int hits = match.addHit(attacker.getUniqueId());
            if (hits >= 100) {
                finishMatch(match, attacker.getUniqueId(), attacker.getName() + " reached 100 hits");
            } else {
                updateMatchScoreboard(match);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Match match = matches.get(event.getEntity().getUniqueId());
        if (match == null) {
            return;
        }
        event.setDeathMessage(null);
        event.getDrops().clear();
        Bukkit.getScheduler().runTask(this, () -> {
            ChatColor playerColor = match.isBlue(event.getEntity().getUniqueId()) ? ChatColor.BLUE : ChatColor.RED;
            String reason = color(resolveMessage("match-death")).replace("<victim>", playerColor + event.getEntity().getName() + ChatColor.GOLD);
            handlePlayerDeath(event.getEntity(), match, reason);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (!matches.containsKey(player.getUniqueId()) && !activeEvents.containsKey(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (matches.containsKey(event.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTask(this, () -> updatePlayerHealthForMatch(event.getPlayer()));
        }
        if (!frostSettings.getBoolean("SETTINGS.MATCH.REMOVE-BOTTLE", true) || !matches.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getItem() == null || event.getItem().getType() != Material.POTION) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> event.getPlayer().getInventory().remove(Material.GLASS_BOTTLE), 1L);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player) || !(event.getInventory().getHolder() instanceof Chest)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (!activeEvents.containsKey(player.getUniqueId())) {
            return;
        }
        Chest chest = (Chest) event.getInventory().getHolder();
        String key = chest.getWorld().getName() + ":" + chest.getX() + ":" + chest.getY() + ":" + chest.getZ();
        if (!filledEventChests.add(key) || !isInventoryEmpty(event.getInventory())) {
            return;
        }
        fillChest(event.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getTitle().equals(ChatColor.DARK_GRAY + "Spectate Tracker")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.SKULL_ITEM && item.hasItemMeta()) {
                String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                Player target = Bukkit.getPlayer(name);
                if (target != null && target.isOnline()) {
                    player.teleport(target.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + ".");
                    player.closeInventory();
                }
            }
            return;
        }
        if (event.getWhoClicked() instanceof Player && spectatingMatch.containsKey(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(PlayerMoveEvent event) {
        Match match = matches.get(event.getPlayer().getUniqueId());
        if (match == null) {
            if (spawnLocation != null && spawnMin != null && spawnMax != null && !activeEvents.containsKey(event.getPlayer().getUniqueId())
                    && !arenaEditors.containsKey(event.getPlayer().getUniqueId())
                    && !spawnEditors.contains(event.getPlayer().getUniqueId())
                    && !Boolean.TRUE.equals(fastbridgeAssignedState(event.getPlayer()))
                    && event.getTo() != null && event.getTo().getWorld().equals(spawnLocation.getWorld())
                    && !insideCuboid(event.getTo(), spawnMin, spawnMax)) {
                pluginTeleport(event.getPlayer(), spawnLocation);
            }
            if (event.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                event.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
            }
            return;
        }
        UUID pid = event.getPlayer().getUniqueId();
        if (!match.started || respawningPlayers.contains(pid)) {
            applyCountdownOrRespawnSpawnLock(event, match, pid);
            return;
        }
        if (event.getTo() != null && event.getTo().getY() <= matchVoidY(match, event.getPlayer().getUniqueId())) {
            playSound(event.getPlayer(), "abyss");
            if (match.kit.bridges) {
                instantBridgeRespawn(event.getPlayer(), match);
                return;
            }
            ChatColor playerColor = match.isBlue(event.getPlayer().getUniqueId()) ? ChatColor.BLUE : ChatColor.RED;
            String reason;
            UUID victimId = event.getPlayer().getUniqueId();
            UUID killerId = lastDamager.get(victimId);
            Long time = lastDamageTime.get(victimId);
            if (killerId != null && time != null && (System.currentTimeMillis() - time <= 15000L)) {
                Player killer = Bukkit.getPlayer(killerId);
                if (killer != null && killer.isOnline()) {
                    ChatColor killerColor = match.isBlue(killerId) ? ChatColor.BLUE : ChatColor.RED;
                    reason = color(resolveMessage("void-killed"))
                            .replace("<victim>", playerColor + event.getPlayer().getName() + ChatColor.GOLD)
                            .replace("<killer>", killerColor + killer.getName() + ChatColor.GOLD);
                } else {
                    reason = color(resolveMessage("void-death")).replace("<player>", playerColor + event.getPlayer().getName() + ChatColor.GOLD);
                }
            } else {
                reason = color(resolveMessage("void-death")).replace("<player>", playerColor + event.getPlayer().getName() + ChatColor.GOLD);
            }
            handlePlayerDeath(event.getPlayer(), match, reason);
            return;
        }
        if (match.started && event.getTo() != null && isBridgePortal(event.getTo().getBlock().getType())) {
            if ((match.kit.bridges || isBattleRush(match) || isStickFight(match)) && isEnemyBridgePortal(event.getPlayer(), match, event.getTo())) {
                scoreBridgeGoal(event.getPlayer(), match);
            } else {
                pluginTeleport(event.getPlayer(), match.spawnFor(event.getPlayer().getUniqueId()));
            }
            return;
        }
    }

    /**
     * Pre-fight countdown and respawn countdown: lock horizontal position to the spawn column only (free vertical
     * movement and look). Void snaps back onto that column with a safe Y.
     */
    private void applyCountdownOrRespawnSpawnLock(PlayerMoveEvent event, Match match, UUID pid) {
        if (event.getTo() == null) {
            return;
        }
        boolean isStartCountdown = !match.started;
        boolean isPortalScoredCountdown = match.bridgeResetting;
        boolean isHoldMode = isBattleRush(match) || match.kit.bridges;

        if (!isStartCountdown && !isPortalScoredCountdown && !isHoldMode) {
            return;
        }
        Location to = event.getTo();
        Location from = event.getFrom();
        Location spawn = match.spawnFor(pid);
        int voidYLevel = matchVoidY(match, pid);
        if (to.getY() <= voidYLevel) {
            Location safe = to.clone();
            safe.setX(spawn.getX());
            safe.setZ(spawn.getZ());
            safe.setY(Math.max(spawn.getY(), voidYLevel + 1.0));
            safe.setYaw(to.getYaw());
            safe.setPitch(to.getPitch());
            event.setTo(safe);
            return;
        }
        if (from != null && (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ())) {
            Location locked = spawn.clone();
            locked.setY(to.getY());
            locked.setYaw(to.getYaw());
            locked.setPitch(to.getPitch());
            event.setTo(locked);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTeleport(PlayerTeleportEvent event) {
        if (pluginTeleports.remove(event.getPlayer().getUniqueId())) {
            return;
        }
        Match match = matches.get(event.getPlayer().getUniqueId());
        if (match == null) {
            return;
        }
        if (!match.started || respawningPlayers.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        
        // Allow plugin or unknown teleports (anti-cheat rubberbands, etc.) to prevent random TP backs to spawn
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            return;
        }
        
        // Allow ender pearls if the kit supports it
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(this, () -> pluginTeleport(event.getPlayer(), match.spawnFor(event.getPlayer().getUniqueId())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPortal(PlayerPortalEvent event) {
        Match match = matches.get(event.getPlayer().getUniqueId());
        if (match == null || !match.kit.bridges) {
            return;
        }
        event.setCancelled(true);
        if (match.started && !match.ending && isEnemyBridgePortal(event.getPlayer(), match, event.getFrom())) {
            scoreBridgeGoal(event.getPlayer(), match);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!Boolean.TRUE.equals(fastbridgeAssignedState(event.getPlayer()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockDamage(BlockDamageEvent event) {
        Match match = matches.get(event.getPlayer().getUniqueId());
        if (match == null || match.ending || !match.bedRespawn) {
            return;
        }
        Material type = event.getBlock().getType();
        if (type == Material.BED_BLOCK || type == Material.BED) {
            if (!match.started) {
                event.setCancelled(true);
                return;
            }
        }
        if (!match.started) {
            return;
        }
        if (respawningPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (type == Material.BED_BLOCK || type == Material.BED) {
            UUID brokenOwner = match.ownerOfBed(event.getBlock().getLocation());
            UUID opponentBedOwner = match.other(event.getPlayer().getUniqueId());
            if (brokenOwner == null || !brokenOwner.equals(opponentBedOwner)) {
                event.setCancelled(true);
                return;
            }
            event.setInstaBreak(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Match match = matches.get(event.getPlayer().getUniqueId());
        if (match == null && (arenaEditors.containsKey(event.getPlayer().getUniqueId()) 
                || spawnEditors.contains(event.getPlayer().getUniqueId())
                || Boolean.TRUE.equals(fastbridgeAssignedState(event.getPlayer())))) {
            return;
        }
        if (match == null || match.ending) {
            event.setCancelled(true);
            return;
        }
        Material type = event.getBlock().getType();
        String key = blockKey(event.getBlock().getLocation());
        boolean playerPlaced = match.placedBlocks.contains(key);
        if (!match.started) {
            event.setCancelled(true);
            return;
        }
        if (respawningPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (match.bedRespawn && (type == Material.BED_BLOCK || type == Material.BED)) {
            UUID bedOwner = match.ownerOfBed(event.getBlock().getLocation());
            UUID opponentBedOwner = match.other(event.getPlayer().getUniqueId());
            if (bedOwner == null || !bedOwner.equals(opponentBedOwner)) {
                event.setCancelled(true);
                if (bedOwner != null && bedOwner.equals(event.getPlayer().getUniqueId())) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot break your own bed.");
                }
                return;
            }
            event.setCancelled(true);
            if (!breakOpponentBed(match, event.getPlayer(), event.getBlock())) {
                return;
            }
            return;
        }
        if (!playerPlaced) {
            if (isSpleef(match) && type == Material.SNOW_BLOCK) {
                trackArenaBlock(match, event.getBlock());
                event.setCancelled(false);
                event.getBlock().setType(Material.AIR);
                event.getPlayer().getInventory().addItem(new ItemStack(Material.SNOW_BALL));
                return;
            }
            if (match.bedRespawn && isAutoDetectedBedDefense(match, event.getBlock())) {
                trackArenaBlock(match, event.getBlock());
                event.setCancelled(false);
                return;
            }
            if (match.kit.bridges && (type == Material.STAINED_CLAY || type == Material.HARD_CLAY || type == Material.CLAY)) {
                trackArenaBlock(match, event.getBlock());
                event.setCancelled(false);
                return;
            }
            event.setCancelled(true);
            return;
        }
        if (!match.originalBlocks.containsKey(key)) {
            trackArenaBlock(match, event.getBlock());
        }
        event.setCancelled(false);
        match.placedBlocks.remove(key);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFade(org.bukkit.event.block.BlockFadeEvent event) {
        if (event.getBlock().getType() == Material.GRASS && event.getNewState().getType() == Material.DIRT) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Match match = matches.get(event.getPlayer().getUniqueId());
        if (match == null && (arenaEditors.containsKey(event.getPlayer().getUniqueId()) 
                || spawnEditors.contains(event.getPlayer().getUniqueId())
                || Boolean.TRUE.equals(fastbridgeAssignedState(event.getPlayer())))) {
            return;
        }
        if (match == null || match.ending || !match.started || !match.kit.build) {
            event.setCancelled(true);
            return;
        }
        if (respawningPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (match.kit.bridges) {
            int radius = bridgePortalConfig != null ? bridgePortalConfig.getInt("portal-protection-radius", 3) : 3;
            boolean nearPortal = false;
            Location loc = event.getBlockPlaced().getLocation();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (isBridgePortal(loc.clone().add(x, y, z).getBlock().getType())) {
                            nearPortal = true;
                            break;
                        }
                    }
                    if (nearPortal) break;
                }
                if (nearPortal) break;
            }
            if (nearPortal) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot place blocks this close to the portal.");
                return;
            }
        }
        Location placedLoc = event.getBlockPlaced().getLocation();
        if (isNearSpawnpoint(placedLoc, match.arena.first) || isNearSpawnpoint(placedLoc, match.arena.second)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot place blocks on the spawnpoint.");
            return;
        }
        if (getMatchHeightLimit(match) > 0 && event.getBlockPlaced().getY() >= matchHeightLimitY(match, event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot place blocks above the height limit.");
            return;
        }
        if (isOutsideArenaBounds(match, event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot place blocks here");
            return;
        }
        String key = blockKey(event.getBlockPlaced().getLocation());
        if (!match.originalBlocks.containsKey(key)) {
            match.originalBlocks.put(key, event.getBlockReplacedState());
        }
        if (event.getBlockPlaced().getType() == Material.TNT) {
            primePlacedTnt(event.getPlayer(), match, event.getBlockPlaced());
            return;
        }
        match.placedBlocks.add(key);
        if (isBattleRush(match)) {
            final String finalKey = key;
            final Block block = event.getBlockPlaced();
            final Player player = event.getPlayer();
            final Match finalMatch = match;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline() && matches.get(player.getUniqueId()) == finalMatch && !finalMatch.ending) {
                    if (finalMatch.placedBlocks.contains(finalKey)) {
                        trackArenaBlock(finalMatch, block);
                        block.setType(Material.AIR);
                        finalMatch.placedBlocks.remove(finalKey);
                    }
                }
            }, 10 * 20L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockCanBuild(BlockCanBuildEvent event) {
        if (!event.isBuildable()) {
            Location loc = event.getBlock().getLocation();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isInvisOrSpectator(player)) {
                    if (isPlayerIntersectingBlock(player, loc)) {
                        event.setBuildable(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (!isBedMaterial(event.getBlock().getType())) {
            return;
        }
        Match match = matchAtBed(event.getBlock());
        if (match == null || match.ending || !match.bedRespawn || !match.started) {
            return;
        }
        if (match.includesRegisteredBedBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (hasBedSupport(event.getBlock())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        if (item != null && isBedMaterial(item.getType())) {
            event.setCancelled(true);
            event.getEntity().remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
            handleTntExplosion(event, (TNTPrimed) event.getEntity());
            return;
        }
        Projectile projectile = event.getEntity() instanceof Projectile ? (Projectile) event.getEntity() : null;
        if (projectile == null || !(projectile.getShooter() instanceof Player)) {
            event.setCancelled(true);
            return;
        }
        Player shooter = (Player) projectile.getShooter();
        Match match = matches.get(shooter.getUniqueId());
        if (match == null || match.ending) {
            event.setCancelled(true);
            return;
        }
        boolean isFireball = event.getEntity() instanceof Fireball;
        if (isFireball) {
            // Fireballs should never destroy the arena/map itself.
            // Only allow them to remove wool that was placed during the match.
            List<Block> affected = new ArrayList<>(event.blockList());
            event.blockList().clear();
            for (Block block : affected) {
                if (block.getType() != Material.WOOL) {
                    continue;
                }
                String key = blockKey(block.getLocation());
                if (!match.placedBlocks.contains(key)) {
                    continue;
                }
                trackArenaBlock(match, block);
                block.setType(Material.AIR);
                match.placedBlocks.remove(key);
            }
        } else {
            for (Block block : event.blockList()) {
                trackArenaBlock(match, block);
            }
        }
        if (fireballKbEnabled) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!match.first.equals(player.getUniqueId()) && !match.second.equals(player.getUniqueId())) {
                    continue;
                }
                if (!player.getWorld().equals(event.getLocation().getWorld()) || player.getLocation().distanceSquared(event.getLocation()) > 25.0D) {
                    continue;
                }
                Vector knockback = player.getLocation().toVector().subtract(event.getLocation().toVector()).normalize().multiply(fireballKbMultiplier);
                knockback.setY(Math.max(0.45D, knockback.getY() + 0.35D));
                player.setVelocity(knockback);
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockedCommand(PlayerCommandPreprocessEvent event) {
        trackExternalScoreboardCommand(event);
        String lower = event.getMessage().toLowerCase(Locale.ROOT);
        
        // Disable /event and /events for now
        if (lower.equals("/event") || lower.startsWith("/event ")
                || lower.equals("/events") || lower.startsWith("/events ")
                || lower.equals("/hostevent") || lower.startsWith("/hostevent ")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(prefix + ChatColor.RED + "Events are disabled for now.");
            return;
        }

        boolean inMatch = matches.containsKey(event.getPlayer().getUniqueId());
        boolean inQueue = queuedKit.containsKey(event.getPlayer().getUniqueId());



        List<String> aliases = leaveCommandConfig.getStringList("leave-aliases");
        boolean isAlias = false;
        String cmdPart = lower.startsWith("/") ? lower.substring(1) : lower;
        if (cmdPart.contains(" ")) cmdPart = cmdPart.split(" ")[0];
        
        for (String alias : aliases) {
            if (cmdPart.equalsIgnoreCase(alias)) {
                isAlias = true;
                break;
            }
        }

        if (lower.equals("/lobby") || lower.startsWith("/lobby ") ||
            lower.equals("/l") || lower.startsWith("/l ") ||
            lower.equals("/hub") || lower.startsWith("/hub ") ||
            lower.equals("/spawn") || lower.startsWith("/spawn ") ||
            isAlias) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (event.getPlayer().isOnline() && !matches.containsKey(event.getPlayer().getUniqueId())) {
                    resetPlayer(event.getPlayer());
                    resetScoreboard(event.getPlayer());
                    giveStateHotbar(event.getPlayer());
                    updateLobbyScoreboard(event.getPlayer());
                }
            }, 5L);
        }

        if (!inMatch) {
            return;
        }
        if (lower.startsWith("/spawn") || lower.startsWith("/leave") || lower.startsWith("/hub") || lower.startsWith("/lobby")) {
            return;
        }
        if (isAdmin(event.getPlayer())) {
            return;
        }
        if (lower.startsWith("/fly ") || lower.equals("/fly") || lower.startsWith("/efly ") || lower.equals("/efly")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(prefix + ChatColor.RED + "You cannot use that command in a match.");
            return;
        }
        List<String> blocked = frostSettings.getStringList("SETTINGS.MATCH.BLOCKED-COMMANDS");
        if (blocked.isEmpty()) {
            blocked = Arrays.asList("/kill", "/gamemode", "/gmc", "/tp", "/tpa", "/warp");
        }
        for (String command : blocked) {
            if (lower.startsWith(command.toLowerCase(Locale.ROOT))) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(prefix + ChatColor.RED + "You cannot use that command in a match.");
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerAchievement(PlayerAchievementAwardedEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        leaveQueue(event.getPlayer(), false);
        Match match = matches.get(event.getPlayer().getUniqueId());
        if (match != null) {
            finishMatch(match, match.other(event.getPlayer().getUniqueId()), event.getPlayer().getName() + " quit");
        }
        spectatingMatch.remove(event.getPlayer().getUniqueId());
        pluginTeleports.remove(event.getPlayer().getUniqueId());
        partyInvites.remove(event.getPlayer().getUniqueId());
        // Clean up hosted event if this player was the host
        EventSession hosted = hostedEvents.get(event.getPlayer().getUniqueId());
        if (hosted != null) {
            stopHostedEvent(hosted, color("&c[Event] The host has left. Event has ended."), event.getPlayer(), false);
        }
        // Remove from any event they joined as participant
        activeEvents.remove(event.getPlayer().getUniqueId());
        // Remove from public party list
        publicParties.remove(event.getPlayer().getUniqueId());
        pendingEventKit.remove(event.getPlayer().getUniqueId());
        pendingEventMaxSize.remove(event.getPlayer().getUniqueId());
        pendingEventTeamSize.remove(event.getPlayer().getUniqueId());
        pendingEventArenaName.remove(event.getPlayer().getUniqueId());
        respawningPlayers.remove(event.getPlayer().getUniqueId());
        spawnProtectionUntil.remove(event.getPlayer().getUniqueId());
        spawnEditors.remove(event.getPlayer().getUniqueId());
        scoreboardEntries.remove(event.getPlayer().getUniqueId());
        scoreboardLineCount.remove(event.getPlayer().getUniqueId());
        practiceScoreboards.remove(event.getPlayer().getUniqueId());
        cachedFastbridgeAssigned.remove(event.getPlayer().getUniqueId());
        cachedFastbridgeAssignedUntil.remove(event.getPlayer().getUniqueId());
        event.getPlayer().setGameMode(GameMode.SURVIVAL);
        event.getPlayer().setAllowFlight(false);
        event.getPlayer().setFlying(false);
        leaveParty(event.getPlayer(), false);
        Player leaving = event.getPlayer();
        if (spawnLocation != null && spawnLocation.getWorld() != null) {
            pluginTeleport(leaving, spawnLocation);
        }
    }

    private void trackExternalScoreboardCommand(PlayerCommandPreprocessEvent event) {
        String lower = event.getMessage().trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("/fb join") || lower.startsWith("/fastbridge join") || lower.startsWith("/fastbuilder join")) {
            externalScoreboardPlayers.add(event.getPlayer().getUniqueId());
            Bukkit.getScheduler().runTaskLater(this, () -> clearPracticeScoreboard(event.getPlayer()), 1L);
            return;
        }
        String externalLeave = leaveCommandConfig.getString("external-leave-command", "fp leave").toLowerCase(Locale.ROOT);
        if (!externalLeave.startsWith("/")) externalLeave = "/" + externalLeave;

        List<String> aliases = leaveCommandConfig.getStringList("leave-aliases");
        boolean isAlias = false;
        String cmdPart = lower.startsWith("/") ? lower.substring(1) : lower;
        if (cmdPart.contains(" ")) cmdPart = cmdPart.split(" ")[0];
        for (String alias : aliases) {
            if (cmdPart.equalsIgnoreCase(alias)) {
                isAlias = true;
                break;
            }
        }
        
        if (lower.startsWith(externalLeave) || lower.startsWith("/fp leave") || lower.startsWith("/fb leave") || lower.startsWith("/fastbridge leave")
                || lower.startsWith("/lobby") || lower.startsWith("/hub") || lower.startsWith("/spawn")
                || lower.equals("/l") || lower.startsWith("/l ") || isAlias) {
            if (externalScoreboardPlayers.remove(event.getPlayer().getUniqueId())) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (event.getPlayer().isOnline() && !matches.containsKey(event.getPlayer().getUniqueId())) {
                        giveStateHotbar(event.getPlayer());
                        updateLobbyScoreboard(event.getPlayer());
                    }
                }, 3L);
            }
        }
    }

    private boolean shouldSuppressPracticeScoreboard(Player player) {
        Boolean fastbridgeAssigned = fastbridgeAssignedState(player);
        if (Boolean.TRUE.equals(fastbridgeAssigned)) {
            externalScoreboardPlayers.add(player.getUniqueId());
            return true;
        }
        if (Boolean.FALSE.equals(fastbridgeAssigned)) {
            if (externalScoreboardPlayers.remove(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(this, () -> {
                    if (player.isOnline() && !matches.containsKey(player.getUniqueId())) {
                        giveStateHotbar(player);
                    }
                });
            }
            return false;
        }
        // If reflection check failed (null), use a short cache to prevent flicker from intermittent failures
        long now = System.currentTimeMillis();
        Long until = cachedFastbridgeAssignedUntil.get(player.getUniqueId());
        if (until != null && until > now) {
            Boolean cached = cachedFastbridgeAssigned.get(player.getUniqueId());
            if (cached != null) {
                if (cached) externalScoreboardPlayers.add(player.getUniqueId());
                return cached;
            }
        }
        return externalScoreboardPlayers.contains(player.getUniqueId());
    }

    private Boolean fastbridgeAssignedState(Player player) {
        // Cache successful checks for a short window to avoid scoreboard tug-of-war if reflection occasionally fails
        long now = System.currentTimeMillis();
        Long until = cachedFastbridgeAssignedUntil.get(player.getUniqueId());
        if (until != null && until > now) {
            Boolean cached = cachedFastbridgeAssigned.get(player.getUniqueId());
            if (cached != null) {
                return cached;
            }
        }
        boolean checkedAnySupportedPlugin = false;
        for (String pluginName : Arrays.asList("Fastbridge", "FastBuilder")) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (plugin == null || !plugin.isEnabled()) {
                continue;
            }
            Object islandManager = invokeNoArg(plugin, "getIslandManager");
            if (islandManager == null) {
                continue;
            }
            checkedAnySupportedPlugin = true;
            Object island = invokeIslandLookup(islandManager, player);
            if (island != null) {
                cachedFastbridgeAssigned.put(player.getUniqueId(), true);
                cachedFastbridgeAssignedUntil.put(player.getUniqueId(), now + 5000L);
                return true;
            }
        }
        if (checkedAnySupportedPlugin) {
            cachedFastbridgeAssigned.put(player.getUniqueId(), false);
            cachedFastbridgeAssignedUntil.put(player.getUniqueId(), now + 5000L);
            return Boolean.FALSE;
        }
        return null;
    }

    private Object invokeIslandLookup(Object islandManager, Player player) {
        Object result = invokeOneArg(islandManager, "getIslandForPlayer", Player.class, player);
        if (result != null) {
            return result;
        }
        return invokeOneArg(islandManager, "getIslandForPlayer", UUID.class, player.getUniqueId());
    }

    private Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object invokeOneArg(Object target, String methodName, Class<?> parameterType, Object value) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.setAccessible(true);
            return method.invoke(target, value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void handlePlayerDeath(Player player, Match match, String reason) {
        UUID uuid = player.getUniqueId();
        if (!match.started) {
            resetPlayer(player);
            pluginTeleport(player, match.spawnFor(uuid));
            giveKit(player, match.kit, match.isBlue(uuid));
            return;
        }
        if (!isStickFight(match)) {
            broadcast(match, prefix + ChatColor.GOLD + reason);
        }
        // Team matches: eliminate the player, and finish the match only if their whole team is out.
        if (match.isTeamMatch()) {
            UUID killerId = lastDamager.get(uuid);
            if (killerId != null && match.isEnemy(killerId, uuid)) {
                Player killer = Bukkit.getPlayer(killerId);
                if (killer != null && killer.isOnline()) {
                    playSound(killer, "kill");
                }
            }

            match.markDead(uuid);
            // Remove the eliminated player from the match map and return them to spawn.
            matches.remove(uuid);
            respawningPlayers.remove(uuid);
            leaveRespawnWaiting(player);
            returnToSpawn(player, false);

            UUID teamCaptain = match.teamCaptain(uuid);
            if (match.teamEliminated(teamCaptain)) {
                finishMatch(match, match.other(uuid), reason);
            } else {
                updateMatchScoreboard(match);
            }
            return;
        }

        // Play kill sound for the killer (1v1)
        Player killer = Bukkit.getPlayer(match.other(uuid));
        if (killer != null && killer.isOnline()) {
            playSound(killer, "kill");
        }
        if (isMlgRush(match)) {
            instantMlgRushRespawn(player, match);
            return;
        }
        if (isStickFight(match)) {
            Player scorer = recentVoidScorer(player, match);
            if (scorer != null) {
                scoreBridgeGoal(scorer, match);
            } else {
                broadcast(match, prefix + ChatColor.GOLD + reason);
                respawnBridgeRound(match);
            }
            return;
        }
        if (match.kit.bridges && bridgeVoidInstantRespawn) {
            instantBridgeRespawn(player, match);
            return;
        }
        if ((match.bedRespawn && match.bedAlive(uuid)) || isBattleRush(match)) {
            respawnInMatch(player, match);
            return;
        }
        finishMatch(match, match.other(uuid), reason);
    }

    private void instantMlgRushRespawn(Player player, Match match) {
        if (match.ending || !player.isOnline()) {
            return;
        }
        resetPlayer(player);
        pluginTeleport(player, match.spawnFor(player.getUniqueId()));
        giveKit(player, match.kit, match.isBlue(player.getUniqueId()));
        applySpawnProtection(player);
        updateMatchScoreboard(match);
    }

    private void instantBridgeRespawn(Player player, Match match) {
        if (match.ending || !player.isOnline()) {
            return;
        }
        resetPlayer(player);
        pluginTeleport(player, match.spawnFor(player.getUniqueId()));
        giveKit(player, match.kit, match.isBlue(player.getUniqueId()));
        applySpawnProtection(player);
        updateMatchScoreboard(match);
    }

    private void scoreBridgeGoal(Player player, Match match) {
        if (match.bridgeResetting || match.ending) {
            return;
        }
        int score = match.addBridgePoint(player.getUniqueId());
        broadcast(match, format("bridge-scored",
                "<player>", player.getName(),
                "<first_score>", String.valueOf(match.bridgeScore(match.first)),
                "<second_score>", String.valueOf(match.bridgeScore(match.second)),
                "<score>", String.valueOf(score),
                "<target>", String.valueOf(Math.max(1, match.kit.goals))));
        updateMatchScoreboard(match);
        int required = match.kit.goals;
        if (isBattleRush(match) || isStickFight(match) || isMlgRush(match)) {
            required = (required <= 0) ? 3 : required;
        }
        if (score >= Math.max(1, required)) {
            if (isMlgRush(match)) {
                resetArena(match);
            }
            finishMatch(match, player.getUniqueId(), player.getName() + " reached the goal");
            return;
        }
        respawnBridgeRound(match);
    }

    private void respawnBridgeRound(Match match) {
        if (!match.bridgeResetting) {
            match.bridgeResetting = true;
            if (!match.kit.bridges) {
                resetArena(match);
            }
            if (match.bedRespawn) {
                rediscoverBedsNextTick(match);
            }
            Player first = Bukkit.getPlayer(match.first);
            Player second = Bukkit.getPlayer(match.second);
            int seconds = bridgeRespawnCountdownSeconds;
            boolean shouldInvis = !isBattleRush(match) && !match.kit.bridges && !isMlgRush(match);
            if (first != null) {
                pluginTeleport(first, match.arena.first);
                enterRespawnWaiting(first, seconds, shouldInvis);
            }
            if (second != null) {
                pluginTeleport(second, match.arena.second);
                enterRespawnWaiting(second, seconds, shouldInvis);
            }
            respawningPlayers.add(match.first);
            respawningPlayers.add(match.second);
            final BukkitTask[] holder = new BukkitTask[1];
            holder[0] = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                private int left = seconds;

                @Override
                public void run() {
                    if (match.ending || !matches.containsKey(match.first) || !matches.containsKey(match.second)) {
                        match.bridgeResetting = false;
                        if (holder[0] != null) {
                            holder[0].cancel();
                        }
                        return;
                    }
                    if (left <= 0) {
                        respawningPlayers.remove(match.first);
                        respawningPlayers.remove(match.second);
                        leaveRespawnWaiting(first);
                        leaveRespawnWaiting(second);
                        pluginTeleport(first, match.arena.first);
                        pluginTeleport(second, match.arena.second);
                        giveKit(first, match.kit, match.firstIsBlue);
                        giveKit(second, match.kit, match.secondIsBlue);
                        applySpawnProtection(first);
                        applySpawnProtection(second);
                        sendTitle(first, ChatColor.GREEN + "Fight!", "");
                        sendTitle(second, ChatColor.GREEN + "Fight!", "");
                        sendTitleToSpectators(match, ChatColor.GREEN + "Fight!", "");
                        playSound(first, "fight");
                        playSound(second, "fight");
                        match.bridgeResetting = false;
                        if (holder[0] != null) {
                            holder[0].cancel();
                        }
                        return;
                    }
                    sendTitle(first, ChatColor.AQUA + String.valueOf(left), ChatColor.GRAY + "Respawning");
                    sendTitle(second, ChatColor.AQUA + String.valueOf(left), ChatColor.GRAY + "Respawning");
                    sendTitleToSpectators(match, ChatColor.GRAY + "Respawning", "");
                    playSound(first, "countdown");
                    playSound(second, "countdown");
                    left--;
                }
            }, 0L, 20L);
        }
    }

    private void respawnInMatch(Player player, Match match) {
        UUID uuid = player.getUniqueId();
        if (!respawningPlayers.add(uuid)) {
            return;
        }
        boolean shouldInvis = !match.kit.bridges;
        enterRespawnWaiting(player, respawnCountdownSeconds, shouldInvis);
        updateMatchScoreboard(match);
        String specMsg = ChatColor.GOLD + player.getName() + " is respawning...";
        for (Map.Entry<UUID, Match> entry : spectatingMatch.entrySet()) {
            if (entry.getValue() == match) {
                Player spec = Bukkit.getPlayer(entry.getKey());
                if (spec != null) spec.sendMessage(specMsg);
            }
        }
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            private int left = respawnCountdownSeconds;

            @Override
            public void run() {
                if (!matches.containsKey(uuid) || match.ending || !player.isOnline()) {
                    respawningPlayers.remove(uuid);
                    match.respawnTasks.remove(uuid);
                    if (holder[0] != null) {
                        holder[0].cancel();
                    }
                    return;
                }
                if (left <= 0) {
                    respawningPlayers.remove(uuid);
                    leaveRespawnWaiting(player);
                    pluginTeleport(player, match.spawnFor(uuid));
                    giveKit(player, match.kit, match.isBlue(uuid));
                    applySpawnProtection(player);
                    updateMatchScoreboard(match);
                    match.respawnTasks.remove(uuid);
                    sendTitle(player, ChatColor.GREEN + "Fight!", "");
                    sendTitleToSpectators(match, ChatColor.GREEN + "Fight!", "");
                    playSound(player, "fight");
                    if (holder[0] != null) {
                        holder[0].cancel();
                    }
                    return;
                }
                String respawningMsg = format("respawning", "<seconds>", String.valueOf(left));
                player.sendMessage(respawningMsg);
                sendTitle(player, ChatColor.AQUA + String.valueOf(left), ChatColor.GRAY + "Respawning");
                sendTitleToSpectators(match, ChatColor.GRAY + "Respawning", "");
                playSound(player, "countdown");
                left--;
            }
        }, 0L, 20L);
        match.respawnTasks.put(uuid, holder[0]);
    }

    private void loadStats() {
        statsFile = new File(getDataFolder(), "stats.yml");
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        kitLayoutsFile = new File(getDataFolder(), "kit-layouts.yml");
        kitLayoutsConfig = YamlConfiguration.loadConfiguration(kitLayoutsFile);
    }

    private void loadAirLeaderboards() {
        airLeaderboardFile = new File(getDataFolder(), "Airleaderboard.yml");
        airLeaderboardConfig = YamlConfiguration.loadConfiguration(airLeaderboardFile);
        boolean changed = false;
        changed |= setDefaultAirLeaderboard("settings.line-spacing", 0.28D);
        changed |= setDefaultAirLeaderboard("settings.max-entries", 10);
        changed |= setDefaultAirLeaderboard("formats.empty", "&7No leaderboard data yet.");
        changed |= setDefaultAirLeaderboard("formats.wins.title", "&6&lWins Leaderboard");
        changed |= setDefaultAirLeaderboard("formats.wins.line", "&e<number>. <luckperms_nametag> &7- &6<value>");
        changed |= setDefaultAirLeaderboard("formats.winstreak.title", "&6&lWinstreak Leaderboard");
        changed |= setDefaultAirLeaderboard("formats.winstreak.line", "&e<number>. <luckperms_nametag> &7- &6<value>");
        changed |= setDefaultAirLeaderboard("formats.daily_streak.title", "&6&lDaily Streak Leaderboard");
        changed |= setDefaultAirLeaderboard("formats.daily_streak.line", "&e<number>. <luckperms_nametag> &7- &6<value>");
        changed |= setDefaultAirLeaderboard("formats.elo.title", "&6&lELO Leaderboard");
        changed |= setDefaultAirLeaderboard("formats.elo.line", "&e<number>. <luckperms_nametag> &7- &6<value>");
        changed |= migrateAirLeaderboardLine("formats.wins.line");
        changed |= migrateAirLeaderboardLine("formats.winstreak.line");
        changed |= migrateAirLeaderboardLine("formats.daily_streak.line");
        changed |= migrateAirLeaderboardLine("formats.elo.line");
        if (changed) {
            saveYaml(airLeaderboardConfig, airLeaderboardFile, "Airleaderboard.yml");
        }
    }

    private boolean setDefaultAirLeaderboard(String path, Object value) {
        if (airLeaderboardConfig.contains(path)) {
            return false;
        }
        airLeaderboardConfig.set(path, value);
        return true;
    }

    private boolean migrateAirLeaderboardLine(String path) {
        String current = airLeaderboardConfig.getString(path, "");
        if (!current.contains("<luckperms_prefix>") && !current.contains("<player>")) {
            return false;
        }
        airLeaderboardConfig.set(path, "&e<number>. <luckperms_nametag> &7- &6<value>");
        return true;
    }

    private PlayerStats stats(UUID uuid) {
        PlayerStats cached = statsCache.get(uuid);
        if (cached != null) {
            return cached;
        }
        PlayerStats loaded = new PlayerStats();
        String base = "players." + uuid + ".";
        loaded.wins = statsConfig.getInt(base + "wins", 0);
        loaded.losses = statsConfig.getInt(base + "losses", 0);
        loaded.winstreak = statsConfig.getInt(base + "winstreak", 0);
        loaded.bestWinstreak = statsConfig.getInt(base + "best-winstreak", 0);
        List<String> friendsList = statsConfig.getStringList(base + "friends");
        if (friendsList != null) {
            for (String f : friendsList) loaded.friends.add(UUID.fromString(f));
        }
        ConfigurationSection kitSection = statsConfig.getConfigurationSection(base + "kits");
        if (kitSection != null) {
            for (String kit : kitSection.getKeys(false)) {
                KitStats kitStats = loaded.kitStats(kit);
                String path = base + "kits." + kit + ".";
                kitStats.unrankedWins = statsConfig.getInt(path + "unranked-wins", 0);
                kitStats.unrankedLosses = statsConfig.getInt(path + "unranked-losses", 0);
                kitStats.rankedWins = statsConfig.getInt(path + "ranked-wins", 0);
                kitStats.rankedLosses = statsConfig.getInt(path + "ranked-losses", 0);
                kitStats.elo = statsConfig.getInt(path + "elo", 1000);
            }
        }
        statsCache.put(uuid, loaded);
        loaded.name = statsConfig.getString(base + "name", "Unknown");
        loaded.customDivision = statsConfig.getString(base + "custom-division", null);
        return loaded;
    }

    private void recordMatchStats(Match match, UUID winnerId) {
        if (winnerId == null || (!winnerId.equals(match.first) && !winnerId.equals(match.second))) {
            return;
        }
        if ("duel".equalsIgnoreCase(match.type)) {
            return;
        }
        UUID loserId = match.other(winnerId);
        PlayerStats winnerStats = stats(winnerId);
        PlayerStats loserStats = stats(loserId);
        String kitKey = normalize(match.kit.name);
        KitStats winnerKit = winnerStats.kitStats(kitKey);
        KitStats loserKit = loserStats.kitStats(kitKey);
        winnerStats.wins++;
        winnerStats.winstreak++;
        winnerStats.bestWinstreak = Math.max(winnerStats.bestWinstreak, winnerStats.winstreak);
        loserStats.losses++;
        loserStats.winstreak = 0;
        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);
        if (winner != null) winnerStats.name = winner.getName();
        if (loser != null) loserStats.name = loser.getName();
        if ("ranked".equals(match.type)) {
            winnerKit.rankedWins++;
            loserKit.rankedLosses++;
            int change = eloChange(winnerKit.elo, loserKit.elo);
            winnerKit.elo += change;
            loserKit.elo = Math.max(0, loserKit.elo - change);
        } else {
            winnerKit.unrankedWins++;
            loserKit.unrankedLosses++;
        }
        writeStats(winnerId, winnerStats);
        writeStats(loserId, loserStats);
        saveStats();

        Player w = Bukkit.getPlayer(winnerId);
        if (w != null) updateLuckPermsPrefix(w);
        Player l = Bukkit.getPlayer(loserId);
        if (l != null) updateLuckPermsPrefix(l);

        refreshLeaderboards();
    }

    private int eloChange(int winnerElo, int loserElo) {
        double expected = 1.0D / (1.0D + Math.pow(10.0D, (loserElo - winnerElo) / 400.0D));
        return Math.max(5, (int) Math.round(32.0D * (1.0D - expected)));
    }

    private void writeStats(UUID uuid, PlayerStats stats) {
        String base = "players." + uuid + ".";
        if (stats.name != null) statsConfig.set(base + "name", stats.name);
        statsConfig.set(base + "wins", stats.wins);
        statsConfig.set(base + "losses", stats.losses);
        statsConfig.set(base + "winstreak", stats.winstreak);
        statsConfig.set(base + "best-winstreak", stats.bestWinstreak);
        List<String> friendsStr = new ArrayList<>();
        for (UUID f : stats.friends) friendsStr.add(f.toString());
        statsConfig.set(base + "friends", friendsStr);
        for (Map.Entry<String, KitStats> entry : stats.kits.entrySet()) {
            String path = base + "kits." + entry.getKey() + ".";
            KitStats kitStats = entry.getValue();
            statsConfig.set(path + "unranked-wins", kitStats.unrankedWins);
            statsConfig.set(path + "unranked-losses", kitStats.unrankedLosses);
            statsConfig.set(path + "ranked-wins", kitStats.rankedWins);
            statsConfig.set(path + "ranked-losses", kitStats.rankedLosses);
            statsConfig.set(path + "elo", kitStats.elo);
        }
        statsConfig.set(base + "custom-division", stats.customDivision);
    }

    private void saveStats() {
        if (statsFile == null || statsConfig == null) {
            return;
        }
        try {
            statsConfig.save(statsFile);
        } catch (Exception exception) {
            getLogger().warning("Could not save stats.yml: " + exception.getMessage());
        }
    }

    private void finishMatch(Match match, UUID winnerId, String reason) {
        if (match.ending) {
            return;
        }
        match.ending = true;
        // Ensure participants are always available (supports party team matches)
        Set<UUID> participants = new HashSet<>();
        participants.add(match.first);
        participants.add(match.second);
        if (match.participants != null) {
            participants.addAll(match.participants);
        }

        for (UUID id : participants) {
            lastPlayedMatch.put(id, match);
        }

        Map<UUID, Player> onlineParticipants = new HashMap<>();
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                onlineParticipants.put(id, p);
                leaveRespawnWaiting(p);
            }
        }

        match.cancelMatchTasks();
        for (UUID id : participants) {
            respawningPlayers.remove(id);
            matches.remove(id);
        }

        // Only record stats for normal 1v1 matches
        if (participants.size() <= 2) {
            recordMatchStats(match, winnerId);
        }

        Player first = onlineParticipants.get(match.first);
        Player second = onlineParticipants.get(match.second);
        Player winner = winnerId == null ? null : Bukkit.getPlayer(winnerId);
        final Player finalLoser;
        if (winnerId != null) {
            finalLoser = winnerId.equals(match.first) ? second : first;
        } else {
            finalLoser = null;
        }

        final boolean loserLeft = reason != null && (reason.endsWith(" left") || reason.endsWith(" quit"));

        if (match.eventHost != null) {
            EventSession event = activeEvents.get(match.eventHost);
            if (event != null) {
                UUID loserId = match.first.equals(winnerId) ? match.second : match.first;
                event.alivePlayers.remove(loserId);
                String winnerName = winnerId == null ? "Unknown" : playerName(winnerId);
                String loserName = loserId == null ? "Unknown" : playerName(loserId);
                ChatColor winnerColor = winnerId != null && match.isBlue(winnerId) ? ChatColor.BLUE : ChatColor.RED;
                ChatColor loserColor = loserId != null && match.isBlue(loserId) ? ChatColor.BLUE : ChatColor.RED;
                broadcastToEvent(event, color("&6[Event] " + winnerColor + winnerName + ChatColor.GOLD + " &fhas defeated " + loserColor + loserName + ChatColor.GOLD + "&f!"));
                
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    startNextEventRound(event);
                }, 60L);
            }
        }

        if (participants.size() > 2) {
            // Team match: keep it simple (no winner/loser inventory screens)
            for (Player p : onlineParticipants.values()) {
                p.sendMessage(prefix + ChatColor.YELLOW + (reason == null ? "Match ended." : reason));
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
                p.updateInventory();
            }
        } else if (winner != null && finalLoser != null) {
            winner.sendMessage(format("won", "<opponent>", finalLoser.getName()));
            finalLoser.sendMessage(format("lost", "<opponent>", winner.getName()));
            
            // Hide the loser from all other players
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(finalLoser)) {
                    online.hidePlayer(finalLoser);
                }
            }
            finalLoser.getInventory().clear();
            finalLoser.getInventory().setArmorContents(null);
            
            winner.getInventory().clear();
            winner.getInventory().setArmorContents(null);

            ItemStack playAgain = new ItemStack(Material.PAPER);
            org.bukkit.inventory.meta.ItemMeta playAgainMeta = playAgain.getItemMeta();
            playAgainMeta.setDisplayName(ChatColor.AQUA + "Play Again");
            playAgain.setItemMeta(playAgainMeta);

            winner.getInventory().setItem(0, playAgain);
            winner.updateInventory();

            if (!loserLeft) {
                finalLoser.getInventory().setItem(0, playAgain);
                finalLoser.updateInventory();
            }

            sendTimedTitle(winner, ChatColor.GREEN + "Victory", color(frostMessages.getString("MESSAGES.MATCH.THIS-SUBTITLE", "<player> &fwon the match!"))
                    .replace("<player>", winner.getName()), 60);
            playSound(winner, "victory");
            
            if (!loserLeft) {
                sendTimedTitle(finalLoser, ChatColor.RED + "Defeat", color(frostMessages.getString("MESSAGES.MATCH.THIS-SUBTITLE", "<player> &fwon the match!"))
                        .replace("<player>", winner.getName()), 60);
                playSound(finalLoser, "defeat");
            }
            sendMatchResults(winner, winner, finalLoser);
            sendMatchResults(finalLoser, winner, finalLoser);
        } else {
            ItemStack playAgain = new ItemStack(Material.PAPER);
            org.bukkit.inventory.meta.ItemMeta playAgainMeta = playAgain.getItemMeta();
            playAgainMeta.setDisplayName(ChatColor.AQUA + "Play Again");
            playAgain.setItemMeta(playAgainMeta);

            if (first != null) {
                first.sendMessage(prefix + ChatColor.YELLOW + reason);
                first.getInventory().clear();
                first.getInventory().setArmorContents(null);
                first.getInventory().setItem(0, playAgain);
                first.updateInventory();
            }
            if (second != null) {
                second.sendMessage(prefix + ChatColor.YELLOW + reason);
                second.getInventory().clear();
                second.getInventory().setArmorContents(null);
                second.getInventory().setItem(0, playAgain);
                second.updateInventory();
            }
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            // Teleport spectators back
            for (UUID spectatorId : new HashSet<>(spectatingMatch.keySet())) {
                if (spectatingMatch.get(spectatorId) == match) {
                    Player spectator = Bukkit.getPlayer(spectatorId);
                    if (spectator != null) {
                        spectatingMatch.remove(spectatorId);
                        returnToSpawn(spectator, false);
                        spectator.sendMessage(ChatColor.YELLOW + "The match has ended. Returning to lobby.");
                    }
                }
            }

            // Return players to spawn
            if (participants.size() > 2) {
                for (Player p : onlineParticipants.values()) {
                    if (p != null && p.isOnline()) {
                        returnToSpawn(p, false);
                    }
                }
            }

            if (first != null && first.isOnline()) {
                if (lastPlayedMatch.containsKey(first.getUniqueId())) {
                    if (!(loserLeft && finalLoser != null && finalLoser.equals(first))) {
                        if (match.eventHost != null && activeEvents.containsKey(match.eventHost)) {
                            EventSession event = activeEvents.get(match.eventHost);
                            setEventWaitingPlayer(first, null, event != null && event.alivePlayers.contains(first.getUniqueId()));
                        } else {
                            returnToSpawn(first, false);
                        }
                    }
                    lastPlayedMatch.remove(first.getUniqueId());
                }
            }
            if (second != null && second.isOnline()) {
                if (lastPlayedMatch.containsKey(second.getUniqueId())) {
                    if (!(loserLeft && finalLoser != null && finalLoser.equals(second))) {
                        if (match.eventHost != null && activeEvents.containsKey(match.eventHost)) {
                            EventSession event = activeEvents.get(match.eventHost);
                            setEventWaitingPlayer(second, null, event != null && event.alivePlayers.contains(second.getUniqueId()));
                        } else {
                            returnToSpawn(second, false);
                        }
                    }
                    lastPlayedMatch.remove(second.getUniqueId());
                }
            }
            if (match.eventHost != null && activeEvents.containsKey(match.eventHost)) {
                EventSession event = activeEvents.get(match.eventHost);
                for (UUID playerId : event.players) {
                    if (playerId.equals(match.first) || playerId.equals(match.second)) {
                        continue;
                    }
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null && p.isOnline()) {
                        setEventWaitingPlayer(p, null, event.alivePlayers.contains(playerId));
                    }
                }
            }
            resetArena(match);
            busyArenas.remove(match.arena);
        }, 60L);
    }

    private void sendMatchResults(Player target, Player winner, Player loser) {
        List<String> lines = frostMessages.getStringList("MESSAGES.MATCH.POST-MATCH.NORMAL");
        if (lines.isEmpty()) {
            lines = Arrays.asList("", "&f&lMatch Results &7(click player to view):", "&aWinner: &6<winner> &7| &cLoser: &6<loser>", "");
        }
        for (String line : lines) {
            target.sendMessage(color(line)
                    .replace(" &7(click player to view):", ":")
                    .replace(" &7(Click to view):", ":")
                    .replace(" &7(click to view):", ":")
                    .replace("<winner>", winner.getName())
                    .replace("<loser>", loser.getName()));
        }
    }

    private void returnToSpawn(Player player, boolean endMatch) {
        lastPlayedMatch.remove(player.getUniqueId());
        leaveQueue(player, true);
        if (endMatch) {
            Match match = matches.get(player.getUniqueId());
            if (match != null) {
                UUID opponentId = match.other(player.getUniqueId());
                finishMatch(match, opponentId, player.getName() + " left");
            }
        }
        // If the player leaves while in a respawn state, they can carry that "invis" into the next match
        // until the next countdown ends. Clear respawn state immediately when returning to spawn.
        respawningPlayers.remove(player.getUniqueId());
        leaveRespawnWaiting(player);
        resetPlayer(player);
        resetScoreboard(player);
        leaveEvent(player, false);
        if (spawnLocation != null) {
            pluginTeleport(player, spawnLocation);
        }
        clearMomentum(player);
        giveStateHotbar(player);
        updateLobbyScoreboard(player);
    }

    private void leaveQueue(Player player, boolean notify) {
        String kit = queuedKit.remove(player.getUniqueId());
        if (kit == null) {
            return;
        }
        String type = queuedType.remove(player.getUniqueId());
        queuedAt.remove(player.getUniqueId());
        Queue<UUID> queue = queues.get(queueKey(type, kit));
        if (queue != null) {
            queue.remove(player.getUniqueId());
        }
        if (notify) {
            player.sendMessage(format("unqueued"));
        }
        giveStateHotbar(player);
        updateLobbyScoreboard(player);
        refreshQueueMenus();
    }

    private void leaveEvent(Player player, boolean notify) {
        EventSession session = activeEvents.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (session.host.equals(player.getUniqueId())) {
            stopHostedEvent(session, color("&c[Event] The event has been ended by the host."), player, false);
            return;
        }
        session.players.remove(player.getUniqueId());
        session.alivePlayers.remove(player.getUniqueId());
        if (notify) {
            sendFrostLines(player, "MESSAGES.EVENT.LEFT", "<eventName>", displayEventName(session.name));
        }
        broadcastToEvent(session, color("&6[Event] &e" + player.getName() + " &fleft the &6Event &7(" + session.players.size() + "/" + session.max + ")"));
    }

    private void resetPlayer(Player player) {
        spawnProtectionUntil.remove(player.getUniqueId());
        pearlCooldown.remove(player.getUniqueId());
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        clearMomentum(player);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setMaximumNoDamageTicks(20);
        player.setNoDamageTicks(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        // Ensure player is visible to everyone
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(player);
            player.showPlayer(online);
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.updateInventory();
        removeHealthDisplay(player);
        lastDamager.remove(player.getUniqueId());
        lastDamageTime.remove(player.getUniqueId());
    }

    private void enterRespawnWaiting(Player player, int seconds, boolean invisibility) {
        resetPlayer(player);
        Match match = matches.get(player.getUniqueId());
        boolean isHoldMode = match != null && (isBattleRush(match) || isMlgRush(match) || match.kit.bridges);
        if (isHoldMode) {
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
        } else {
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        if (invisibility) {
            // Hide the respawning player from everyone else
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    online.hidePlayer(player);
                }
            }
        }
    }

    private void leaveRespawnWaiting(Player player) {
        // Show the respawning player to everyone again
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(player);
        }
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    private void applySpawnProtection(Player player) {
        if (spawnProtectionSeconds <= 0) {
            return;
        }
        spawnProtectionUntil.put(player.getUniqueId(), System.currentTimeMillis() + spawnProtectionSeconds * 1000L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPearlInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_PEARL) {
            return;
        }
        Match match = matches.get(player.getUniqueId());
        if (match == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Long cooldown = pearlCooldown.get(player.getUniqueId());
        if (cooldown != null && cooldown > now) {
            double remaining = (cooldown - now) / 1000.0;
            player.sendMessage(ChatColor.RED + "Pearl cooldown: " + String.format("%.1f", remaining) + "s");
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        if (normalize(match.kit.name).contains("nodebuff")) {
            pearlCooldown.put(player.getUniqueId(), now + 15000L);
        }
    }

    @EventHandler
    public void onEntityShootBow(org.bukkit.event.entity.EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        Match match = matches.get(player.getUniqueId());
        if (match != null && match.kit.bridges) {
            long now = System.currentTimeMillis();
            Long cooldown = bowCooldown.get(player.getUniqueId());
            if (cooldown != null && cooldown > now) {
                double remaining = (cooldown - now) / 1000.0;
                player.sendMessage(ChatColor.RED + "Bow cooldown: " + String.format("%.1f", remaining) + "s");
                event.setCancelled(true);
                player.updateInventory();
                return;
            }
            bowCooldown.put(player.getUniqueId(), now + 3000L);
            
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) {
                    boolean hasArrow = false;
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.ARROW) {
                            hasArrow = true;
                            break;
                        }
                    }
                    if (!hasArrow) {
                        player.getInventory().addItem(new ItemStack(Material.ARROW));
                    }
                }
            }, 1L);
        }
    }

    private boolean hasSpawnProtection(UUID uuid) {
        Long until = spawnProtectionUntil.get(uuid);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            spawnProtectionUntil.remove(uuid);
            return false;
        }
        return true;
    }

    private void clearSpawnProtection(UUID uuid) {
        spawnProtectionUntil.remove(uuid);
    }

    private void pluginTeleport(Player player, Location location) {
        if (location == null) {
            return;
        }
        clearMomentum(player);
        pluginTeleports.add(player.getUniqueId());
        if (!player.teleport(location)) {
            pluginTeleports.remove(player.getUniqueId());
        }
        clearMomentum(player);
        Bukkit.getScheduler().runTask(this, () -> clearMomentum(player));
        Bukkit.getScheduler().runTaskLater(this, () -> clearMomentum(player), 2L);
    }

    private void clearMomentum(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.setFallDistance(0.0F);
        player.setVelocity(new Vector(0, 0, 0));
    }

    private ItemStack[] readItems(List<?> list, int size) {
        ItemStack[] result = new ItemStack[size];
        if (list == null) {
            return result;
        }
        for (int i = 0; i < Math.min(size, list.size()); i++) {
            Object value = list.get(i);
            if (value instanceof ItemStack) {
                result[i] = ((ItemStack) value).clone();
            }
        }
        return result;
    }

    private ItemStack[] cloneItems(ItemStack[] source, int size) {
        ItemStack[] clone = new ItemStack[size];
        if (source == null) {
            return clone;
        }
        for (int i = 0; i < Math.min(size, source.length); i++) {
            clone[i] = source[i] == null ? null : source[i].clone();
        }
        return clone;
    }

    private ItemStack[] recolorItems(ItemStack[] source, boolean blueTeam) {
        for (int i = 0; i < source.length; i++) {
            source[i] = recolorTeamItem(source[i], blueTeam);
        }
        return source;
    }

    @SuppressWarnings("deprecation")
    private ItemStack recolorTeamItem(ItemStack item, boolean blueTeam) {
        if (item == null) {
            return null;
        }
        Material type = item.getType();
        short teamData = blueTeam ? (short) 11 : (short) 14;
        short otherData = blueTeam ? (short) 14 : (short) 11;
        if ((type == Material.WOOL || type == Material.STAINED_CLAY || type == Material.STAINED_GLASS
                || type == Material.STAINED_GLASS_PANE || type == Material.CARPET)
                && (item.getDurability() == otherData || item.getDurability() == teamData)) {
            item.setDurability(teamData);
        } else if (type == Material.INK_SACK && (item.getDurability() == 1 || item.getDurability() == 4)) {
            item.setDurability(blueTeam ? (short) 4 : (short) 1);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                meta.setDisplayName(recolorTeamText(meta.getDisplayName(), blueTeam));
            }
            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    lore.add(recolorTeamText(line, blueTeam));
                }
                meta.setLore(lore);
            }
            if (meta instanceof LeatherArmorMeta) {
                ((LeatherArmorMeta) meta).setColor(blueTeam ? Color.BLUE : Color.RED);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String recolorTeamText(String text, boolean blueTeam) {
        if (blueTeam) {
            return text.replace("Red", "Blue")
                    .replace("red", "blue")
                    .replace("RED", "BLUE")
                    .replace(ChatColor.RED.toString(), ChatColor.BLUE.toString());
        }
        return text.replace("Blue", "Red")
                    .replace("blue", "red")
                    .replace("BLUE", "RED")
                    .replace(ChatColor.BLUE.toString(), ChatColor.RED.toString())
                    .replace(ChatColor.AQUA.toString(), ChatColor.RED.toString());
    }

    private Location parseLocation(String value, World fallbackWorld) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String[] raw = value.split(",");
        if (raw.length < 3) {
            return null;
        }
        try {
            int offset = 0;
            World world = fallbackWorld;
            String first = raw[0].trim();
            String last = raw[raw.length - 1].trim();
            if (raw.length >= 6 && Bukkit.getWorld(first) != null) {
                world = Bukkit.getWorld(first);
                offset = 1;
            } else if (raw.length >= 6 && Bukkit.getWorld(last) != null) {
                world = Bukkit.getWorld(last);
            }
            if (world == null) {
                return null;
            }
            double x = Double.parseDouble(raw[offset].trim());
            double y = Double.parseDouble(raw[offset + 1].trim());
            double z = Double.parseDouble(raw[offset + 2].trim());
            float yaw = raw.length > offset + 3 ? Float.parseFloat(raw[offset + 3].trim()) : 0.0F;
            float pitch = raw.length > offset + 4 ? Float.parseFloat(raw[offset + 4].trim()) : 0.0F;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String serializeLocation(Location location) {
        return location.getX() + ", " + location.getY() + ", " + location.getZ() + ", "
                + location.getYaw() + ", " + location.getPitch() + ", " + location.getWorld().getName();
    }

    private ArenaSpawns arenaByName(String name) {
        String normalized = normalize(name);
        List<ArenaSpawns> matches = new ArrayList<>();
        for (ArenaSpawns arena : arenas) {
            if (normalize(arena.name).equals(normalized) || normalize(arena.baseName).equals(normalized)) {
                matches.add(arena);
            }
        }
        if (matches.isEmpty()) return null;
        if (matches.size() == 1) return matches.get(0);
        return matches.get(random.nextInt(matches.size()));
    }

    private void saveYaml(FileConfiguration configuration, File file, String displayName) {
        try {
            configuration.save(file);
        } catch (IOException exception) {
            getLogger().warning("Could not save " + displayName + ": " + exception.getMessage());
        }
    }

    private Location findSafe(Location location) {
        Location safe = location.clone();
        safe.setX(safe.getBlockX() + 0.5D);
        safe.setZ(safe.getBlockZ() + 0.5D);
        for (int i = 0; i <= 8; i++) {
            if (isClear(safe) && isClear(safe.clone().add(0, 1, 0))) {
                return safe;
            }
            safe.add(0, 1, 0);
        }
        return location.clone();
    }

    private boolean isClear(Location location) {
        Material type = location.getBlock().getType();
        return type == Material.AIR || !type.isSolid();
    }

    private boolean movedBlock(Location from, Location to) {
        return from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ();
    }

    private boolean insideCuboid(Location location, Location a, Location b) {
        double minX = Math.min(a.getX(), b.getX());
        double maxX = Math.max(a.getX(), b.getX());
        double minY = Math.min(a.getY(), b.getY());
        double maxY = Math.max(a.getY(), b.getY());
        double minZ = Math.min(a.getZ(), b.getZ());
        double maxZ = Math.max(a.getZ(), b.getZ());
        return location.getX() >= minX && location.getX() <= maxX
                && location.getY() >= minY && location.getY() <= maxY
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    private void trackArenaBlock(Match match, Block block) {
        if (block == null || !sameWorld(block.getLocation(), match.arena.first) || !nearArena(match.arena, block.getLocation())) {
            return;
        }
        String key = blockKey(block.getLocation());
        if (!match.originalBlocks.containsKey(key)) {
            match.originalBlocks.put(key, block.getState());
        }
    }

    private void resetArena(Match match) {
        List<BlockState> states = new ArrayList<>(match.originalBlocks.values());
        if (!isMlgRush(match)) {
            Collections.reverse(states);
            for (BlockState state : states) {
                state.update(true, false);
            }
            match.originalBlocks.clear();
            removeArenaEntities(match);
            return;
        }

        List<BlockState> bedStates = new ArrayList<>();
        for (Iterator<BlockState> iterator = states.iterator(); iterator.hasNext();) {
            BlockState state = iterator.next();
            if (isBedMaterial(state.getType())) {
                bedStates.add(state);
                iterator.remove();
            }
        }
        for (BlockState state : states) {
            state.update(true, false);
        }
        for (BlockState state : bedStates) {
            state.update(true, false);
        }
        if (isMlgRush(match)) {
            restoreOriginalMlgRushBeds(match);
        }
        match.originalBlocks.clear();
        removeArenaEntities(match);
    }

    private void removeArenaEntities(Match match) {
        if (match.arena.first != null && match.arena.first.getWorld() != null) {
            World world = match.arena.first.getWorld();
            for (Entity entity : world.getEntities()) {
                if (nearArena(match.arena, entity.getLocation())) {
                    if (entity instanceof Player || entity.hasMetadata("NPC")) {
                        continue;
                    }
                    if (entity instanceof LivingEntity || entity instanceof Item || entity instanceof TNTPrimed || entity instanceof Projectile || entity instanceof Fireball) {
                        entity.remove();
                    }
                }
            }
        }
    }

    private boolean nearArena(ArenaSpawns arena, Location location) {
        double minX = Math.min(arena.first.getX(), arena.second.getX()) - 80.0D;
        double maxX = Math.max(arena.first.getX(), arena.second.getX()) + 80.0D;
        double minY = Math.min(arena.first.getY(), arena.second.getY()) - 80.0D;
        double maxY = Math.max(arena.first.getY(), arena.second.getY()) + 80.0D;
        double minZ = Math.min(arena.first.getZ(), arena.second.getZ()) - 80.0D;
        double maxZ = Math.max(arena.first.getZ(), arena.second.getZ()) + 80.0D;
        return location.getX() >= minX && location.getX() <= maxX
                && location.getY() >= minY && location.getY() <= maxY
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    private boolean sameWorld(Location first, Location second) {
        return first != null && second != null && first.getWorld() != null && first.getWorld().equals(second.getWorld());
    }

    private String blockKey(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private boolean isBridgePortal(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.equals("ENDER_PORTAL") || name.equals("END_PORTAL") || name.equals("PORTAL");
    }

    private boolean isEnemyBridgePortal(Player player, Match match, Location location) {
        if (location == null) {
            return false;
        }
        Location ownSpawn = match.spawnFor(player.getUniqueId());
        Location enemySpawn = match.spawnFor(match.other(player.getUniqueId()));
        return enemySpawn.distanceSquared(location) < ownSpawn.distanceSquared(location);
    }

    private void releaseMatchRespawnState(Match match) {
        respawningPlayers.remove(match.first);
        respawningPlayers.remove(match.second);
        Player first = Bukkit.getPlayer(match.first);
        Player second = Bukkit.getPlayer(match.second);
        if (first != null) {
            leaveRespawnWaiting(first);
        }
        if (second != null) {
            leaveRespawnWaiting(second);
        }
    }

    private void rediscoverBedsNextTick(Match match) {
        Bukkit.getScheduler().runTask(this, () -> {
            if (!match.ending && matches.containsKey(match.first) && matches.containsKey(match.second)) {
                discoverMatchBeds(match);
            }
        });
    }

    private void discoverMatchBeds(Match match) {
        if (!match.bedRespawn || match.arena.first == null || match.arena.second == null
                || match.arena.first.getWorld() == null || !sameWorld(match.arena.first, match.arena.second)) {
            return;
        }
        match.firstBedBlocks.clear();
        match.secondBedBlocks.clear();
        Set<String> seenBedKeys = new HashSet<>();
        int radius = 96;
        assignBedBlocksNearSpawn(match, match.arena.first, radius, seenBedKeys);
        assignBedBlocksNearSpawn(match, match.arena.second, radius, seenBedKeys);
        if (match.firstBedBlocks.isEmpty() && match.secondBedBlocks.isEmpty()) {
            seenBedKeys.clear();
            assignBedsInArenaFootprint(match, seenBedKeys);
        }
        syncBedAliveFromRegisteredBlocks(match);
        if (isMlgRush(match)) {
            snapshotOriginalMlgRushBeds(match);
        }
    }

    private void snapshotOriginalMlgRushBeds(Match match) {
        for (String key : combinedBedBlockKeys(match)) {
            if (match.originalBedBlocks.containsKey(key)) {
                continue;
            }
            Location location = locationFromBlockKey(key);
            if (location != null && location.getWorld() != null && isBedMaterial(location.getBlock().getType())) {
                match.originalBedBlocks.put(key, location.getBlock().getState());
            }
        }
    }

    private void restoreOriginalMlgRushBeds(Match match) {
        for (BlockState state : match.originalBedBlocks.values()) {
            state.update(true, false);
        }
        for (String key : match.originalBedBlocks.keySet()) {
            Location location = locationFromBlockKey(key);
            if (location != null && location.getWorld() != null) {
                match.firstBedBlocks.remove(key);
                match.secondBedBlocks.remove(key);
                if (location.distanceSquared(match.arena.first) <= location.distanceSquared(match.arena.second)) {
                    match.firstBedBlocks.add(key);
                } else {
                    match.secondBedBlocks.add(key);
                }
            }
        }
        syncBedAliveFromRegisteredBlocks(match);
    }

    private Set<String> combinedBedBlockKeys(Match match) {
        Set<String> keys = new HashSet<>();
        keys.addAll(match.firstBedBlocks);
        keys.addAll(match.secondBedBlocks);
        return keys;
    }

    /**
     * Full arena box between spawns (plus padding) when sphere scans miss beds (very tall / offset builds).
     */
    private void assignBedsInArenaFootprint(Match match, Set<String> seenBedKeys) {
        Location a = match.arena.first;
        Location b = match.arena.second;
        if (a == null || b == null || a.getWorld() == null || !sameWorld(a, b)) {
            return;
        }
        World w = a.getWorld();
        int minX = Math.min(a.getBlockX(), b.getBlockX()) - 80;
        int maxX = Math.max(a.getBlockX(), b.getBlockX()) + 80;
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ()) - 80;
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ()) + 80;
        int minY = Math.min(a.getBlockY(), b.getBlockY()) - 48;
        int maxY = Math.max(a.getBlockY(), b.getBlockY()) + 48;
        Location firstSpawn = match.arena.first;
        Location secondSpawn = match.arena.second;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = w.getBlockAt(x, y, z);
                    if (!isBedMaterial(block.getType())) {
                        continue;
                    }
                    String key = blockKey(block.getLocation());
                    if (!seenBedKeys.add(key)) {
                        continue;
                    }
                    double dFirst = block.getLocation().distanceSquared(firstSpawn);
                    double dSecond = block.getLocation().distanceSquared(secondSpawn);
                    if (dFirst < dSecond) {
                        match.firstBedBlocks.add(key);
                    } else if (dSecond < dFirst) {
                        match.secondBedBlocks.add(key);
                    } else {
                        if (match.first.getMostSignificantBits() < match.second.getMostSignificantBits()) {
                            match.firstBedBlocks.add(key);
                        } else {
                            match.secondBedBlocks.add(key);
                        }
                    }
                }
            }
        }
    }

    /**
     * Collects bed blocks in a cube around one spawn; each bed is owned exclusively by whichever spawn is closer
     * (fixes overlap where the same block was stored for both players).
     */
    private void assignBedBlocksNearSpawn(Match match, Location center, int radius, Set<String> seenBedKeys) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        Location firstSpawn = match.arena.first;
        Location secondSpawn = match.arena.second;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (!isBedMaterial(block.getType())) {
                        continue;
                    }
                    String key = blockKey(block.getLocation());
                    if (!seenBedKeys.add(key)) {
                        continue;
                    }
                    double dFirst = block.getLocation().distanceSquared(firstSpawn);
                    double dSecond = block.getLocation().distanceSquared(secondSpawn);
                    if (dFirst < dSecond) {
                        match.firstBedBlocks.add(key);
                    } else if (dSecond < dFirst) {
                        match.secondBedBlocks.add(key);
                    } else {
                        if (match.first.getMostSignificantBits() < match.second.getMostSignificantBits()) {
                            match.firstBedBlocks.add(key);
                        } else {
                            match.secondBedBlocks.add(key);
                        }
                    }
                }
            }
        }
    }

    private void syncBedAliveFromRegisteredBlocks(Match match) {
        match.firstBedAlive = anyRegisteredBedStillPresent(match.firstBedBlocks);
        match.secondBedAlive = anyRegisteredBedStillPresent(match.secondBedBlocks);
    }

    private boolean anyRegisteredBedStillPresent(Set<String> keys) {
        if (keys.isEmpty()) {
            return false;
        }
        for (String key : keys) {
            Location loc = locationFromBlockKey(key);
            if (loc != null && loc.getWorld() != null && isBedMaterial(loc.getBlock().getType())) {
                return true;
            }
        }
        return false;
    }

    private Location locationFromBlockKey(String key) {
        if (key == null) {
            return null;
        }
        int lz = key.lastIndexOf(':');
        if (lz < 0) {
            return null;
        }
        int ly = key.lastIndexOf(':', lz - 1);
        if (ly < 0) {
            return null;
        }
        int lx = key.lastIndexOf(':', ly - 1);
        if (lx < 0) {
            return null;
        }
        String worldName = key.substring(0, lx);
        try {
            int bx = Integer.parseInt(key.substring(lx + 1, ly));
            int by = Integer.parseInt(key.substring(ly + 1, lz));
            int bz = Integer.parseInt(key.substring(lz + 1));
            World w = Bukkit.getWorld(worldName);
            if (w == null) {
                return null;
            }
            return new Location(w, bx, by, bz);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void primePlacedTnt(Player player, Match match, Block placed) {
        Location center = placed.getLocation().add(0.5D, 0.5D, 0.5D);
        placed.setType(Material.AIR);
        TNTPrimed primed = placed.getWorld().spawn(center, TNTPrimed.class);
        primedTntOwners.put(primed.getUniqueId(), player.getUniqueId());
        primed.setFuseTicks(60);
    }

    private void handleTntExplosion(EntityExplodeEvent event, TNTPrimed tnt) {
        UUID ownerId = primedTntOwners.remove(tnt.getUniqueId());
        Match match = ownerId == null ? null : matches.get(ownerId);
        if (match == null || match.ending || !match.started) {
            event.setCancelled(true);
            return;
        }

        UUID sourceId = ownerId;
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (isBedMaterial(block.getType())) {
                iterator.remove();
                continue;
            }
            if (!canTntBreakBlock(match, block, sourceId)) {
                iterator.remove();
                continue;
            }
            trackArenaBlock(match, block);
            match.placedBlocks.remove(blockKey(block.getLocation()));
        }

        applyTntKnockback(match, event.getLocation());
    }

    private boolean canTntBreakBlock(Match match, Block block, UUID sourceId) {
        if (block == null || block.getType() == Material.AIR) {
            return false;
        }
        String key = blockKey(block.getLocation());
        if (match.placedBlocks.contains(key)) {
            return true;
        }
        return match.bedRespawn && isAutoDetectedBedDefense(match, block);
    }

    private void applyTntKnockback(Match match, Location center) {
        double radiusForce = frostSettings.getDouble("SETTINGS.MATCH.EXPLOSIONS.KNOCKBACK.TNT.RADIUS-FORCE", 2.0D);
        double heightForce = frostSettings.getDouble("SETTINGS.MATCH.EXPLOSIONS.KNOCKBACK.TNT.HEIGHT-FORCE", 1.25D);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!match.first.equals(player.getUniqueId()) && !match.second.equals(player.getUniqueId())) {
                continue;
            }
            if (!player.getWorld().equals(center.getWorld()) || player.getLocation().distanceSquared(center) > 36.0D) {
                continue;
            }
            Vector knockback = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(radiusForce);
            knockback.setY(Math.max(0.35D, knockback.getY() + heightForce));
            player.setVelocity(knockback);
        }
    }

    private boolean breakOpponentBed(Match match, Player breaker, Block bedBlock) {
        UUID brokenOwner = match.ownerOfBed(bedBlock.getLocation());
        UUID breakerOpponent = match.other(breaker.getUniqueId());
        if (brokenOwner == null || !brokenOwner.equals(breakerOpponent)) {
            return false;
        }
        trackArenaBlock(match, bedBlock);
        Location bedSound = bedBlock.getLocation().clone().add(0.5D, 0.5D, 0.5D);
        removeBedNoDrop(match, bedBlock);
        Player owner = Bukkit.getPlayer(brokenOwner);
        String ownerName = owner == null ? "opponent" : owner.getName();
        broadcast(match, format("bed-broken",
                "<player>", breaker.getName(),
                "<opponent>", ownerName));
        playBedBreakSounds(match, bedSound);

        if (isMlgRush(match)) {
            scoreBridgeGoal(breaker, match);
            Bukkit.getScheduler().runTask(this, () -> {
                if (!match.ending && matches.containsKey(match.first) && matches.containsKey(match.second)) {
                    restoreOriginalMlgRushBeds(match);
                    updateMatchScoreboard(match);
                }
            });
            return true;
        }

        if (brokenOwner.equals(match.first)) {
            match.firstBedAlive = false;
        } else {
            match.secondBedAlive = false;
        }
        UUID breakerId = breaker.getUniqueId();
        UUID victimId = brokenOwner;
        Bukkit.getScheduler().runTask(this, () -> {
            Player victim = Bukkit.getPlayer(victimId);
            Player br = Bukkit.getPlayer(breakerId);
            
            if (victim != null) {
                boolean enabled = true;
                String title = ChatColor.RED + "" + ChatColor.BOLD + "BED DESTROYED!";
                String subtitle = ChatColor.WHITE + "You will no longer respawn!";
                
                if (bedConfig != null) {
                    enabled = bedConfig.getBoolean("bed-destroyed-title.enabled", true);
                    title = color(bedConfig.getString("bed-destroyed-title.title", title));
                    subtitle = color(bedConfig.getString("bed-destroyed-title.subtitle", subtitle));
                }
                
                if (enabled) {
                    sendTimedTitle(victim, title, subtitle, 70);
                }
                victim.sendMessage(title + " " + subtitle);
            }
            
            if (br != null) {
                boolean enabled = true;
                String title = ChatColor.GREEN + "" + ChatColor.BOLD + "BED BROKEN!";
                String subtitle = ChatColor.WHITE + ownerName + " can no longer respawn!";
                
                if (bedConfig != null) {
                    enabled = bedConfig.getBoolean("bed-broken-title.enabled", true);
                    title = color(bedConfig.getString("bed-broken-title.title", title));
                    subtitle = color(bedConfig.getString("bed-broken-title.subtitle", subtitle));
                    subtitle = subtitle.replace("<opponent>", ownerName);
                }
                
                if (enabled) {
                    sendTimedTitle(br, title, subtitle, 70);
                    sendTitleToSpectators(match, title, subtitle);
                }
            }
        });
        updateMatchScoreboard(match);
        return true;
    }

    private int matchVoidY(Match match, UUID playerId) {
        if (match.kit.bridges) {
            return bridgeUseAbsoluteVoidY
                    ? bridgeVoidAbsoluteY
                    : match.spawnFor(playerId).getBlockY() - bridgeVoidBlocksUnderSpawn;
        }
        if (match.kit.sumo) {
            return sumoUseAbsoluteY
                    ? sumoVoidAbsoluteY
                    : match.spawnFor(playerId).getBlockY() - sumoVoidBlocksUnderSpawn;
        }
        if (isSpleef(match)) {
            return spleefUseAbsoluteY
                    ? spleefVoidAbsoluteY
                    : match.spawnFor(playerId).getBlockY() - spleefVoidBlocksUnderSpawn;
        }
        return match.spawnFor(playerId).getBlockY() - voidBlocksUnderSpawn;
    }

    private int getMatchHeightLimit(Match match) {
        int limit = heightLimitBlocksAboveSpawn;
        if (match != null && match.kit != null && normalize(match.kit.name).equalsIgnoreCase("fireballfight")) {
            limit = heightLimitConfig.getInt("height-limit.fireballfight.blocks-above-spawn", limit);
        }
        return limit;
    }

    private int matchHeightLimitY(Match match, UUID playerId) {
        return match.spawnFor(playerId).getBlockY() + getMatchHeightLimit(match);
    }

    private int getArenaWidthLimit(Match match) {
        if (match == null || match.kit == null) {
            return widthOverall;
        }
        String kitName = normalize(match.kit.name);
        if (kitName.contains("bedfight")) {
            return widthBedFight > 0 ? widthBedFight : widthOverall;
        }
        if (kitName.contains("fireball")) {
            return widthFireballFight > 0 ? widthFireballFight : widthOverall;
        }
        if (match.kit.bridges) {
            return widthBridge > 0 ? widthBridge : widthOverall;
        }
        if (kitName.contains("battlerush")) {
            return widthBattleRush > 0 ? widthBattleRush : widthOverall;
        }
        return widthOverall;
    }

    private int getArenaLengthLimit(Match match) {
        if (match == null || match.kit == null) {
            return lengthOverall;
        }
        String kitName = normalize(match.kit.name);
        if (kitName.contains("bedfight")) {
            return lengthBedFight > 0 ? lengthBedFight : lengthOverall;
        }
        if (kitName.contains("fireball")) {
            return lengthFireballFight > 0 ? lengthFireballFight : lengthOverall;
        }
        if (match.kit.bridges) {
            return lengthBridge > 0 ? lengthBridge : lengthOverall;
        }
        if (kitName.contains("battlerush")) {
            return lengthBattleRush > 0 ? lengthBattleRush : lengthOverall;
        }
        return lengthOverall;
    }

    private boolean isOutsideArenaBounds(Match match, Location location) {
        if (match == null || match.arena == null) {
            return false;
        }
        Location first = match.arena.first;
        Location second = match.arena.second;
        if (first == null || second == null) {
            return false;
        }
        if (!sameWorld(first, location)) {
            return false;
        }
        int widthLimit = getArenaWidthLimit(match);
        int lengthLimit = getArenaLengthLimit(match);
        if (widthLimit <= 0 && lengthLimit <= 0) {
            return false;
        }
        double diffX = Math.abs(first.getX() - second.getX());
        double diffZ = Math.abs(first.getZ() - second.getZ());
        boolean isXAxisLength = diffX > diffZ;
        double centerX = (first.getX() + second.getX()) / 2.0;
        double centerZ = (first.getZ() + second.getZ()) / 2.0;
        if (isXAxisLength) {
            double distLength = Math.abs(location.getX() - centerX);
            double distWidth = Math.abs(location.getZ() - centerZ);
            if (lengthLimit > 0 && distLength > lengthLimit) {
                return true;
            }
            if (widthLimit > 0 && distWidth > widthLimit) {
                return true;
            }
        } else {
            double distLength = Math.abs(location.getZ() - centerZ);
            double distWidth = Math.abs(location.getX() - centerX);
            if (lengthLimit > 0 && distLength > lengthLimit) {
                return true;
            }
            if (widthLimit > 0 && distWidth > widthLimit) {
                return true;
            }
        }
        return false;
    }

    private void removeBedNoDrop(Match match, Block block) {
        List<Block> blocks = connectedBedBlocks(match, block);
        if (blocks.isEmpty()) {
            blocks.add(block);
        }
        for (Block bedBlock : blocks) {
            trackArenaBlock(match, bedBlock);
        }
        for (Block bedBlock : blocks) {
            if (isBedMaterial(bedBlock.getType())) {
                bedBlock.setType(Material.AIR);
            }
        }
    }

    private List<Block> connectedBedBlocks(Match match, Block block) {
        List<Block> blocks = new ArrayList<>();
        addBedBlock(blocks, block);
        int[][] offsets = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : offsets) {
            Block relative = block.getRelative(offset[0], 0, offset[1]);
            if (isBedMaterial(relative.getType()) || match.includesRegisteredBedBlock(relative.getLocation())) {
                addBedBlock(blocks, relative);
            }
        }
        collectRegisteredBedNeighbors(match, block, blocks);
        return blocks;
    }

    private void collectRegisteredBedNeighbors(Match match, Block block, List<Block> blocks) {
        Set<String> registered = new HashSet<>();
        registered.addAll(match.firstBedBlocks);
        registered.addAll(match.secondBedBlocks);
        for (String key : registered) {
            Location location = locationFromBlockKey(key);
            if (location == null || location.getWorld() == null || !sameWorld(location, block.getLocation())) {
                continue;
            }
            if (Math.abs(location.getBlockX() - block.getX()) <= 1
                    && location.getBlockY() == block.getY()
                    && Math.abs(location.getBlockZ() - block.getZ()) <= 1) {
                addBedBlock(blocks, location.getBlock());
            }
        }
    }

    private void addBedBlock(List<Block> blocks, Block block) {
        if (block == null) {
            return;
        }
        for (Block existing : blocks) {
            if (existing.getWorld().equals(block.getWorld())
                    && existing.getX() == block.getX()
                    && existing.getY() == block.getY()
                    && existing.getZ() == block.getZ()) {
                return;
            }
        }
        blocks.add(block);
    }

    private Match matchAtBed(Block block) {
        for (Match match : new HashSet<>(matches.values())) {
            if (match.bedRespawn && sameWorld(block.getLocation(), match.arena.first) && nearArena(match.arena, block.getLocation())) {
                return match;
            }
        }
        return null;
    }

    private boolean isBedMaterial(Material material) {
        return material == Material.BED_BLOCK || material == Material.BED;
    }

    private boolean hasBedSupport(Block block) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(block);
        int[][] offsets = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : offsets) {
            Block relative = block.getRelative(offset[0], 0, offset[1]);
            if (isBedMaterial(relative.getType())) {
                blocks.add(relative);
            }
        }
        for (Block bedBlock : blocks) {
            if (!bedBlock.getRelative(BlockFace.DOWN).getType().isSolid()) {
                return false;
            }
        }
        return true;
    }

    private boolean isAutoDetectedBedDefense(Match match, Block block) {
        if (!isBedDefenseMaterial(block.getType())) {
            return false;
        }
        int radius = Math.max(1, soundConfig == null ? 6 : soundConfig.getInt("bed-defense-detect-radius", 6));
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block nearby = block.getRelative(x, y, z);
                    Material type = nearby.getType();
                    if (type == Material.BED_BLOCK || type == Material.BED) {
                        return true;
                    }
                }
            }
        }
        return block.getLocation().distanceSquared(match.arena.first) <= 196.0D
                || block.getLocation().distanceSquared(match.arena.second) <= 196.0D;
    }

    private boolean isBedDefenseMaterial(Material material) {
        return material == Material.ENDER_STONE
                || material == Material.WOOD
                || material == Material.LOG
                || material == Material.LOG_2;
    }

    private void playBedBreakSounds(Match match, Location at) {
        boolean mainOn = soundConfig == null || soundConfig.getBoolean("sounds.bed-break.enabled", true);
        if (mainOn) {
            Sound sound = Sound.DIG_WOOD;
            String soundName = soundConfig == null ? "DIG_WOOD" : soundConfig.getString("sounds.bed-break.name", "DIG_WOOD");
            if (soundName != null && !soundName.isEmpty()) {
                try {
                    sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    sound = Sound.DIG_WOOD;
                }
            }
            float volume = soundConfig == null ? 3.0f : (float) Math.max(3.0D, soundConfig.getDouble("sounds.bed-break.volume", 3.0D));
            float pitch = soundConfig == null ? 0.9f : (float) soundConfig.getDouble("sounds.bed-break.pitch", 0.9D);
            playBedBreakSoundForMatch(match, at, sound, volume, pitch);
        }
        boolean witherOn = soundConfig == null || soundConfig.getBoolean("sounds.bed-break-wither.enabled", true);
        if (witherOn) {
            Sound wither = Sound.WITHER_DEATH;
            if (soundConfig != null) {
                try {
                    String witherName = soundConfig.getString("sounds.bed-break-wither.name", "WITHER_DEATH");
                    if (witherName != null && !witherName.isEmpty()) {
                        wither = Sound.valueOf(witherName.toUpperCase(Locale.ROOT));
                    }
                } catch (IllegalArgumentException ignored) {
                    wither = Sound.WITHER_DEATH;
                }
            }
            float wVol = soundConfig == null ? 2.0f : (float) Math.max(1.5D, soundConfig.getDouble("sounds.bed-break-wither.volume", 2.0D));
            float wPitch = soundConfig == null ? 1.0f : (float) soundConfig.getDouble("sounds.bed-break-wither.pitch", 1.0D);
            playBedBreakSoundForMatch(match, at, wither, wVol, wPitch);
        }
    }

    private boolean isSpleef(Match match) {
        return match != null && match.kit != null && match.kit.name.toLowerCase(Locale.ROOT).contains("spleef");
    }

    private boolean isMlgRush(Match match) {
        return match != null && match.kit != null && match.kit.name.toLowerCase(Locale.ROOT).contains("mlgrush");
    }

    private boolean isNoDamageScoringKit(Match match) {
        return isBattleRush(match) || isMlgRush(match) || isStickFight(match);
    }

    private Player recentVoidScorer(Player victim, Match match) {
        UUID victimId = victim.getUniqueId();
        UUID scorerId = lastDamager.get(victimId);
        Long time = lastDamageTime.get(victimId);
        if (scorerId == null || time == null || System.currentTimeMillis() - time > 15000L) {
            return null;
        }
        UUID expectedOpponent = match.other(victimId);
        if (!expectedOpponent.equals(scorerId)) {
            return null;
        }
        Player scorer = Bukkit.getPlayer(scorerId);
        return scorer != null && scorer.isOnline() ? scorer : null;
    }

    private String mlgRushBeds(int score, int max) {
        int lives = Math.max(0, max - score);
        StringBuilder sb = new StringBuilder();
        String circle = "⬤";
        if (frostScoreboard != null) {
            circle = frostScoreboard.getString("SCOREBOARD.BRIDGES-GOAL", "⬤");
        }
        for (int i = 0; i < lives; i++) {
            sb.append("&a").append(circle);
        }
        for (int i = 0; i < (max - lives); i++) {
            sb.append("&c").append(circle);
        }
        return color(sb.toString());
    }

    private String getDivisionPrefix(String divisionKey) {
        if (divisionsConfig != null && divisionsConfig.contains("divisions." + divisionKey)) {
            return divisionsConfig.getString("divisions." + divisionKey + ".prefix", divisionKey);
        }
        return divisionKey;
    }

    private String getDivisionForPlayer(UUID uuid) {
        PlayerStats stats = stats(uuid);
        if (stats.customDivision != null && !stats.customDivision.isEmpty()) {
            return getDivisionPrefix(stats.customDivision);
        }
        return getDivision(stats.wins);
    }

    private void refreshQueueMenus() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getOpenInventory() != null && online.getOpenInventory().getTopInventory() != null) {
                String title = online.getOpenInventory().getTitle();
                if (title != null) {
                    if (title.equals(color(frostMenus.getString("QUEUE-INVENTORY.UNRANKED.TITLE", MENU_TITLE)))) {
                        openQueueMenu(online, "unranked");
                    } else if (title.equals(color(frostMenus.getString("QUEUE-INVENTORY.RANKED.TITLE", MENU_TITLE)))) {
                        openQueueMenu(online, "ranked");
                    } else if (title.equals(color(frostMenus.getString("QUEUE-INVENTORY.PREMIUM.TITLE", MENU_TITLE)))) {
                        openQueueMenu(online, "premium");
                    }
                }
            }
        }
    }

    private boolean isBattleRush(Match match) {
        return match != null && match.kit != null && match.kit.name.toLowerCase(Locale.ROOT).contains("battlerush");
    }

    private boolean isStickFight(Match match) {
        return match != null && match.kit != null && match.kit.name.toLowerCase(Locale.ROOT).contains("stickfight");
    }

    private boolean isFireball(Match match) {
        return match != null && match.kit != null && match.kit.name.toLowerCase(Locale.ROOT).contains("fireball");
    }

    private boolean isBedFight(Match match) {
        return match != null && match.kit != null && match.kit.name.toLowerCase(Locale.ROOT).contains("bedfight");
    }

    private void playBedBreakSoundForMatch(Match match, Location at, Sound sound, float worldVolume, float pitch) {
        float playerVol = Math.max(1.0f, worldVolume);
        if (at != null && at.getWorld() != null) {
            at.getWorld().playSound(at, sound, worldVolume, pitch);
        }
        Player matchFirst = Bukkit.getPlayer(match.first);
        Player matchSecond = Bukkit.getPlayer(match.second);
        if (matchFirst != null) {
            matchFirst.playSound(matchFirst.getLocation(), sound, playerVol, pitch);
        }
        if (matchSecond != null) {
            matchSecond.playSound(matchSecond.getLocation(), sound, playerVol, pitch);
        }
    }

    private void sendTitleToSpectators(Match match, String title, String subtitle) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR && !matches.containsKey(player.getUniqueId())) {
                if (player.getWorld().equals(match.arena.first.getWorld()) && player.getLocation().distanceSquared(match.arena.first) < 10000.0D) {
                    sendTitle(player, title, subtitle);
                }
            }
        }
    }

    private void playSound(Player player, String key) {
        if (player == null) {
            return;
        }
        
        boolean enabled = true;
        String defaultName = "";
        float defaultVolume = 1.0f;
        float defaultPitch = 1.0f;
        
        switch (key.toLowerCase(Locale.ROOT)) {
            case "countdown":
                defaultName = "NOTE_PLING";
                defaultVolume = 1.0f;
                defaultPitch = 1.4f;
                break;
            case "fight":
                defaultName = "LEVEL_UP";
                defaultVolume = 1.0f;
                defaultPitch = 1.0f;
                break;
            case "victory":
                defaultName = "ambient.weather.thunder";
                defaultVolume = 1.0f;
                defaultPitch = 1.2f;
                break;
            case "defeat":
                defaultName = "random.explode";
                defaultVolume = 0.8f;
                defaultPitch = 0.8f;
                break;
            case "abyss":
                defaultName = "ENDERDRAGON_HIT";
                defaultVolume = 1.0f;
                defaultPitch = 0.8f;
                break;
            case "kill":
                defaultName = "ORB_PICKUP";
                defaultVolume = 1.0f;
                defaultPitch = 1.0f;
                break;
        }
        
        if (soundConfig != null) {
            enabled = soundConfig.getBoolean("sounds." + key + ".enabled", true);
            defaultName = soundConfig.getString("sounds." + key + ".name", defaultName);
            defaultVolume = (float) soundConfig.getDouble("sounds." + key + ".volume", defaultVolume);
            defaultPitch = (float) soundConfig.getDouble("sounds." + key + ".pitch", defaultPitch);
        }
        
        if (!enabled || defaultName == null || defaultName.isEmpty()) {
            return;
        }
        
        try {
            Sound sound = Sound.valueOf(defaultName.toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound, defaultVolume, defaultPitch);
        } catch (IllegalArgumentException e) {
            // Fall back to playing as a custom/raw sound string
            try {
                player.playSound(player.getLocation(), defaultName, defaultVolume, defaultPitch);
            } catch (Exception ex) {
                getLogger().warning("Failed to play custom sound '" + defaultName + "' for key '" + key + "': " + ex.getMessage());
            }
        }
    }

    private boolean isInventoryEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void fillChest(Inventory inventory) {
        ConfigurationSection section = frostChest.getConfigurationSection("CHESTS");
        if (section == null || section.getKeys(false).isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>(section.getKeys(false));
        String preset = frostChest.getString("CHESTS." + keys.get(random.nextInt(keys.size())), "");
        int slot = 0;
        for (String token : preset.split(";")) {
            ItemStack item = parseChestItem(token);
            if (item == null) {
                continue;
            }
            while (slot < inventory.getSize() && inventory.getItem(slot) != null) {
                slot++;
            }
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, item);
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack parseChestItem(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        int typeId = -1;
        int amount = 1;
        short data = 0;
        for (String part : token.split(":")) {
            String[] pair = part.split("@");
            if (pair.length != 2) {
                continue;
            }
            if (pair[0].equalsIgnoreCase("t")) {
                typeId = parseInt(pair[1], -1);
            } else if (pair[0].equalsIgnoreCase("a")) {
                amount = Math.max(1, parseInt(pair[1], 1));
            } else if (pair[0].equalsIgnoreCase("d")) {
                data = (short) parseInt(pair[1], 0);
            }
        }
        Material material = Material.getMaterial(typeId);
        return material == null ? null : new ItemStack(material, amount, data);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private List<String> kitNames() {
        List<String> names = new ArrayList<>();
        for (KitData kit : kits.values()) {
            names.add(kit.name);
        }
        return names;
    }

    private List<String> arenaNames() {
        List<String> names = new ArrayList<>();
        for (ArenaSpawns arena : arenas) {
            if (!names.contains(arena.baseName)) {
                names.add(arena.baseName);
            }
        }
        return names;
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private int getPing(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private List<String> startsWith(String prefix, Collection<String> values) {
        List<String> result = new ArrayList<>();
        String normalized = prefix.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(value);
            }
        }
        return result;
    }

    private List<String> colorList(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(color(line));
        }
        return result.isEmpty() ? Collections.singletonList(ChatColor.GOLD + "Click to use.") : result;
    }

    private List<String> replaceLines(List<String> lines, String... replacements) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String value = line;
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                value = value.replace(replacements[i], replacements[i + 1]);
            }
            result.add(value);
        }
        return result;
    }

    private String queueKey(String type, String kit) {
        return normalizeQueueType(type) + ":" + kit;
    }

    private String normalizeQueueType(String type) {
        String normalized = normalize(type);
        if (normalized.contains("ranked") && !normalized.contains("unranked")) {
            return "ranked";
        }
        if (normalized.contains("premium")) {
            return "premium";
        }
        if (normalized.contains("duel")) {
            return "duel";
        }
        return "unranked";
    }

    private String displayQueueType(String type) {
        String normalized = normalizeQueueType(type);
        if (normalized.equals("ranked")) {
            return "Ranked";
        }
        if (normalized.equals("premium")) {
            return "Premium";
        }
        if (normalized.equals("duel")) {
            return "Duel";
        }
        return "Unranked";
    }

    private String queuedDuration(UUID uuid) {
        Long started = queuedAt.get(uuid);
        if (started == null) {
            return "0s";
        }
        long seconds = Math.max(0L, (System.currentTimeMillis() - started) / 1000L);
        if (seconds < 60L) {
            return seconds + "s";
        }
        return (seconds / 60L) + "m " + (seconds % 60L) + "s";
    }

    private String normalizeEventKey(String event) {
        return canonicalEventKey(event).replace("_", "-");
    }

    private String displayEventName(String event) {
        return canonicalEventKey(event).replace("_", " ").replace("-", " ");
    }

    private void sendFrostLines(Player player, String path, String... replacements) {
        List<String> lines = frostMessages.getStringList(path);
        if (lines.isEmpty()) {
            String single = frostMessages.getString(path);
            if (single != null) {
                lines = Collections.singletonList(single);
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        for (String line : lines) {
            String value = color(line);
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                value = value.replace(replacements[i], replacements[i + 1]);
            }
            player.sendMessage(value);
        }
    }

    private void message(Player player, String key) {
        player.sendMessage(format(key));
    }

    private String format(String key, String... replacements) {
        String value = color(resolveMessage(key));
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace(replacements[i], replacements[i + 1]);
            if (replacements[i].equals("<kit>")) {
                value = value.replace("<kit_name>", replacements[i + 1]);
            } else if (replacements[i].equals("<queue>")) {
                value = value.replace("<queue_type>", replacements[i + 1]);
            } else if (replacements[i].equals("<opponent>")) {
                value = value.replace("<opponent_name>", replacements[i + 1]).replace("<opponent>", replacements[i + 1]);
            }
        }
        return prefix + value;
    }

    private String resolveMessage(String key) {
        String msg;
        if (key.equals("queued")) msg = firstMessage("MESSAGES.QUEUE.JOINED-SOLO", "&7Queued for &b<queue> <kit>&7.");
        else if (key.equals("unqueued")) msg = firstMessage("MESSAGES.QUEUE.LEFT", "&7You left the queue.");
        else if (key.equals("match-found")) msg = frostMessages.getString("MESSAGES.MATCH.FOUND", "&6<queue> match found: &f<player> &7vs &f<opponent> &8(&6<kit>&8)");
        else if (key.equals("won")) msg = "&aYou won against &f<opponent>&a.";
        else if (key.equals("lost")) msg = "&cYou lost against &f<opponent>&c.";
        else if (key.equals("no-kit")) msg = "&cUnknown kit.";
        else if (key.equals("no-arena")) msg = "&cNo Frost arena spawns were loaded.";
        else if (key.equals("already-fighting")) msg = "&cYou are already in a match.";
        else if (key.equals("already-queued")) msg = "&cYou are already queued.";
        else if (key.equals("not-fighting")) msg = "&cYou are not in a match.";
        else if (key.equals("duel-sent")) msg = "&7Sent a duel request to &b<player>&7.";
        else if (key.equals("duel-received")) msg = "&b<player> &7challenged you to &b<kit>&7. Use &f/duel accept <player>&7.";
        else if (key.equals("duel-expired")) msg = "&cThat duel request is gone.";
        else if (key.equals("party-created")) msg = firstMessage("MESSAGES.PARTY.CREATED", "&7Party created.");
        else if (key.equals("party-invited")) msg = firstMessage("MESSAGES.PARTY.INVITED", "&7Invited &b<player> &7to your party.");
        else if (key.equals("party-invite-received")) msg = "&b<player> &7invited you to a party. Use &f/party accept <player>&7.";
        else if (key.equals("party-joined")) msg = firstMessage("MESSAGES.PARTY.JOINED", "&b<player> &7joined the party.");
        else if (key.equals("party-left")) msg = firstMessage("MESSAGES.PARTY.LEFT", "&b<player> &7left the party.");
        else if (key.equals("party-disbanded")) msg = firstMessage("MESSAGES.PARTY.DISBANDED", "&cThe party was disbanded.");
        else if (key.equals("bed-broken")) msg = "&c<player> broke <opponent>'s bed!";
        else if (key.equals("void-death")) msg = frostMessages.getString("MESSAGES.MATCH.VOID-DEATH", "&6<player> &ffell into the void!");
        else if (key.equals("void-killed")) msg = frostMessages.getString("MESSAGES.PLAYER.VOID-KILLED", "&6<victim> &fgot hit into the void by &6<killer>&f.");
        else if (key.equals("match-death")) msg = frostMessages.getString("MESSAGES.PLAYER.DEATH", "&6<victim> &fwas killed.");
        else if (key.equals("match-killed")) msg = frostMessages.getString("MESSAGES.PLAYER.KILLED", "&6<victim> &fwas killed by &6<killer>&f.");
        else if (key.equals("bridge-scored")) msg = frostMessages.getString("MESSAGES.MATCH.BRIDGE-SCORED", "&6<player> &fscored! &6<first_score>&7 - &6<second_score>");
        else if (key.equals("respawning")) msg = frostMessages.getString("MESSAGES.MATCH.RESPAWNING", "&6Respawning in <seconds>...");
        else msg = key;

        if (msg != null) {
            msg = msg.replace("was defeated by", "was killed by")
                     .replace("was defeated", "was killed")
                     .replace("has defeated", "has killed")
                     .replace("defeated by", "killed by");
        }
        return msg;
    }

    private String firstMessage(String path, String fallback) {
        List<String> lines = frostMessages.getStringList(path);
        if (!lines.isEmpty()) {
            return String.join("\n", lines);
        }
        return frostMessages.getString(path, fallback);
    }

    private String normalize(String value) {
        return ChatColor.stripColor(color(value == null ? "" : value))
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .toLowerCase(Locale.ROOT);
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    @SuppressWarnings("deprecation")
    private void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle);
    }

    private void sendTimedTitle(Player player, String title, String subtitle, int stayTicks) {
        try {
            Method method = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            method.invoke(player, title, subtitle, 5, stayTicks, 5);
        } catch (Exception ignored) {
            sendTitle(player, title, subtitle);
        }
    }

    private static final class KitData {
        private final String name;
        private final String display;
        private final ItemStack icon;
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final int unrankedPos;
        private final int rankedPos;
        private final int editorPos;
        private final int spawnFfaPos;
        private final Set<String> arenaWhitelist;
        private final boolean ranked;
        private final boolean combo;
        private final boolean sumo;
        private final boolean boxing;
        private final boolean build;
        private final boolean bridges;
        private final boolean skyWars;
        private final boolean bedWars;
        private final boolean noFall;
        private final boolean noHunger;
        private final boolean noRegen;
        private final int damageTicks;
        private final int goals;

        private KitData(String name, String display, ItemStack icon, ItemStack[] contents, ItemStack[] armor, int unrankedPos, int rankedPos,
                        int editorPos, int spawnFfaPos, Set<String> arenaWhitelist, boolean ranked, boolean combo, boolean sumo, boolean boxing,
                        boolean build, boolean bridges, boolean skyWars, boolean bedWars, boolean noFall, boolean noHunger, boolean noRegen,
                        int damageTicks, int goals) {
            this.name = name;
            this.display = display;
            this.icon = icon;
            this.contents = contents;
            this.armor = armor;
            this.unrankedPos = unrankedPos;
            this.rankedPos = rankedPos;
            this.editorPos = editorPos;
            this.spawnFfaPos = spawnFfaPos;
            this.arenaWhitelist = arenaWhitelist;
            this.ranked = ranked;
            this.combo = combo;
            this.sumo = sumo;
            this.boxing = boxing;
            this.build = build;
            this.bridges = bridges;
            this.skyWars = skyWars;
            this.bedWars = bedWars;
            this.noFall = noFall;
            this.noHunger = noHunger;
            this.noRegen = noRegen;
            this.damageTicks = damageTicks;
            this.goals = goals;
        }

        private boolean canUseArena(ArenaSpawns arena) {
            if (arenaWhitelist == null || arenaWhitelist.isEmpty()) {
                return true;
            }
            return arenaWhitelist.contains(normalizeStatic(arena.name)) || arenaWhitelist.contains(normalizeStatic(arena.baseName));
        }
    }

    private static final class ArenaSpawns {
        private final String name;
        private final String baseName;
        private final Location first;
        private final Location second;

        private ArenaSpawns(String name, Location first, Location second) {
            this.name = name;
            this.baseName = name.contains("-") ? name.substring(0, name.indexOf('-')) : name;
            this.first = first;
            this.second = second;
        }
    }

    private static String normalizeStatic(String value) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', value == null ? "" : value))
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .toLowerCase(Locale.ROOT);
    }

    private static final class HotbarItem {
        private final int slot;
        private final Material material;
        private final short data;
        private final int amount;
        private final String name;
        private final String action;
        private final String command;

        private HotbarItem(int slot, Material material, short data, int amount, String name, String action, String command) {
            this.slot = slot;
            this.material = material;
            this.data = data;
            this.amount = amount;
            this.name = name;
            this.action = action == null ? "" : action;
            this.command = command == null ? "" : command;
        }

        private ItemStack toItemStack() {
            ItemStack item = new ItemStack(material, amount, data);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                item.setItemMeta(meta);
            }
            return item;
        }
    }    
    private static final class PlayerSettings {
        private boolean duelRequests = true;
        private boolean partyRequests = true;
        private boolean allowSpectators = true;
        private boolean scoreboard = true;
        private boolean pingOnScoreboard = true;
        private boolean divisionsOnNametag = true;
        private int globalChatMode = 0;
        private boolean friendRequests = true;
        private boolean publicPartiesEnabled = false;
    }

    private static final class EventSession {
        private final String name;
        private final String kitName;
        private final UUID host;
        private final long startedAt;
        private final int joined;
        private final int max;
        private final String eventType;
        private final int teamSize;
        private final boolean allowSpectators;
        private final boolean ranked;
        private final String arenaPool;
        private final Set<UUID> players = new HashSet<>();
        private final List<UUID> alivePlayers = new ArrayList<>();
        private boolean started = false;
        private int round = 1;
        private UUID currentMatchFirst = null;
        private UUID currentMatchSecond = null;

        private EventSession(String name, UUID host, long startedAt, int joined, int max) {
            this(name, name, host, startedAt, joined, max, "Tournament", 1, true, false, "Random");
        }

        private EventSession(String name, String kitName, UUID host, long startedAt, int joined, int max) {
            this(name, kitName, host, startedAt, joined, max, "Tournament", 1, true, false, "Random");
        }

        private EventSession(String name, String kitName, UUID host, long startedAt, int joined, int max,
                             String eventType, int teamSize, boolean allowSpectators, boolean ranked, String arenaPool) {
            this.name = name;
            this.kitName = kitName;
            this.host = host;
            this.startedAt = startedAt;
            this.joined = joined;
            this.max = max;
            this.eventType = eventType == null || eventType.trim().isEmpty() ? "Tournament" : eventType;
            this.teamSize = Math.max(1, teamSize);
            this.allowSpectators = allowSpectators;
            this.ranked = ranked;
            this.arenaPool = arenaPool == null || arenaPool.trim().isEmpty() ? "Random" : arenaPool;
        }
    }

    private static final class Match {
        private final UUID first;
        private final UUID second;
        private final KitData kit;
        private final ArenaSpawns arena;
        private final String type;
        private UUID eventHost = null;
        private String displayKind = "";
        private int teamSize = 1;
        private Set<UUID> participants = new HashSet<>();
        private Set<UUID> firstTeamMembers = new HashSet<>();
        private Set<UUID> secondTeamMembers = new HashSet<>();
        private Set<UUID> aliveFirstTeam = new HashSet<>();
        private Set<UUID> aliveSecondTeam = new HashSet<>();
        private boolean firstIsBlue;
        private boolean secondIsBlue;
        private BukkitTask countdownTask;
        private final Map<UUID, BukkitTask> respawnTasks = new HashMap<>();
        private final Map<String, BlockState> originalBlocks = new LinkedHashMap<>();
        private final Set<String> placedBlocks = new HashSet<>();
        private final Set<String> firstBedBlocks = new HashSet<>();
        private final Set<String> secondBedBlocks = new HashSet<>();
        private final Map<String, BlockState> originalBedBlocks = new LinkedHashMap<>();
        private boolean started;
        private boolean ending;
        private boolean bedRespawn;
        private boolean firstBedAlive = true;
        private boolean secondBedAlive = true;
        private boolean bridgeResetting;
        private int firstHits;
        private int secondHits;
        private int firstBridgeScore;
        private int secondBridgeScore;
        private final long startedAt = System.currentTimeMillis();

        private Match(UUID first, UUID second, KitData kit, ArenaSpawns arena, String type) {
            this.first = first;
            this.second = second;
            this.kit = kit;
            this.arena = arena;
            this.type = type;
            initTeams(Collections.singleton(first), Collections.singleton(second));
        }

        private void initTeams(Collection<UUID> firstTeam, Collection<UUID> secondTeam) {
            participants.clear();
            firstTeamMembers.clear();
            secondTeamMembers.clear();
            aliveFirstTeam.clear();
            aliveSecondTeam.clear();

            if (firstTeam != null) {
                firstTeamMembers.addAll(firstTeam);
            }
            if (secondTeam != null) {
                secondTeamMembers.addAll(secondTeam);
            }

            // Ensure captains are always included
            firstTeamMembers.add(first);
            secondTeamMembers.add(second);

            participants.addAll(firstTeamMembers);
            participants.addAll(secondTeamMembers);
            aliveFirstTeam.addAll(firstTeamMembers);
            aliveSecondTeam.addAll(secondTeamMembers);
        }

        private boolean isTeamMatch() {
            return participants.size() > 2 || firstTeamMembers.size() > 1 || secondTeamMembers.size() > 1;
        }

        private UUID teamCaptain(UUID uuid) {
            if (uuid == null) return second;
            if (firstTeamMembers.contains(uuid) || first.equals(uuid)) return first;
            if (secondTeamMembers.contains(uuid) || second.equals(uuid)) return second;
            // Fallback (shouldn't happen)
            return second;
        }

        private boolean isEnemy(UUID attacker, UUID victim) {
            if (attacker == null || victim == null) return false;
            if (!participants.contains(attacker) || !participants.contains(victim)) return false;
            return !teamCaptain(attacker).equals(teamCaptain(victim));
        }

        private UUID other(UUID uuid) {
            UUID captain = teamCaptain(uuid);
            return first.equals(captain) ? second : first;
        }

        /** True if this coordinate is a match-registered bed block (either foot/head cell). */
        private boolean includesRegisteredBedBlock(Location location) {
            if (location == null || location.getWorld() == null) {
                return false;
            }
            if (bedBlockBelongsTo(location, firstBedBlocks) || bedBlockBelongsTo(location, secondBedBlocks)) {
                return true;
            }
            for (int[] offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                Block relative = location.getWorld().getBlockAt(
                        location.getBlockX() + offset[0],
                        location.getBlockY(),
                        location.getBlockZ() + offset[1]);
                if (bedBlockBelongsTo(relative.getLocation(), firstBedBlocks)
                        || bedBlockBelongsTo(relative.getLocation(), secondBedBlocks)) {
                    return true;
                }
            }
            return false;
        }

        private Location spawnFor(UUID uuid) {
            UUID captain = teamCaptain(uuid);
            return first.equals(captain) ? arena.first : arena.second;
        }

        private boolean bedAlive(UUID uuid) {
            UUID captain = teamCaptain(uuid);
            return first.equals(captain) ? firstBedAlive : secondBedAlive;
        }

        private boolean isBlue(UUID uuid) {
            UUID captain = teamCaptain(uuid);
            return first.equals(captain) ? firstIsBlue : secondIsBlue;
        }

        private int addHit(UUID uuid) {
            UUID captain = teamCaptain(uuid);
            if (first.equals(captain)) {
                return ++firstHits;
            }
            if (second.equals(captain)) {
                return ++secondHits;
            }
            return 0;
        }

        private int hits(UUID uuid) {
            UUID captain = teamCaptain(uuid);
            return first.equals(captain) ? firstHits : second.equals(captain) ? secondHits : 0;
        }

        private int addBridgePoint(UUID uuid) {
            UUID captain = teamCaptain(uuid);
            if (first.equals(captain)) {
                return ++firstBridgeScore;
            }
            if (second.equals(captain)) {
                return ++secondBridgeScore;
            }
            return 0;
        }

        private int bridgeScore(UUID uuid) {
            UUID captain = teamCaptain(uuid);
            return first.equals(captain) ? firstBridgeScore : second.equals(captain) ? secondBridgeScore : 0;
        }

        private void markDead(UUID uuid) {
            if (uuid == null) return;
            if (firstTeamMembers.contains(uuid) || first.equals(uuid)) {
                aliveFirstTeam.remove(uuid);
            } else if (secondTeamMembers.contains(uuid) || second.equals(uuid)) {
                aliveSecondTeam.remove(uuid);
            }
        }

        private boolean teamEliminated(UUID captain) {
            if (captain == null) return false;
            return first.equals(captain) ? aliveFirstTeam.isEmpty() : aliveSecondTeam.isEmpty();
        }

        private UUID ownerOfBed(Location location) {
            if (location == null) {
                return null;
            }
            if (bedBlockBelongsTo(location, firstBedBlocks)) {
                return first;
            }
            if (bedBlockBelongsTo(location, secondBedBlocks)) {
                return second;
            }
            for (int[] offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                Block relative = location.getWorld().getBlockAt(
                        location.getBlockX() + offset[0],
                        location.getBlockY(),
                        location.getBlockZ() + offset[1]);
                if (bedBlockBelongsTo(relative.getLocation(), firstBedBlocks)) {
                    return first;
                }
                if (bedBlockBelongsTo(relative.getLocation(), secondBedBlocks)) {
                    return second;
                }
            }
            return null;
        }

        private boolean bedBlockBelongsTo(Location location, Set<String> beds) {
            if (location == null || location.getWorld() == null) {
                return false;
            }
            String key = location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
            return beds.contains(key);
        }

        private void cancelCountdownTaskOnly() {
            if (countdownTask != null) {
                countdownTask.cancel();
                countdownTask = null;
            }
        }

        /** Stops countdown + respawn timers (match over or aborted). */
        private void cancelMatchTasks() {
            cancelCountdownTaskOnly();
            for (BukkitTask task : respawnTasks.values()) {
                task.cancel();
            }
            respawnTasks.clear();
        }

        private String duration() {
            long seconds = Math.max(0L, (System.currentTimeMillis() - startedAt) / 1000L);
            return (seconds / 60L) + ":" + (seconds % 60L < 10 ? "0" : "") + (seconds % 60L);
        }
    }

    private static final class PlayerStats {
        private String name;
        private int wins;
        private int losses;
        private int winstreak;
        private int bestWinstreak;
        private String customDivision;
        private final Set<UUID> friends = new HashSet<>();
        private final Set<UUID> pendingFriends = new HashSet<>();
        private final Map<String, KitStats> kits = new HashMap<>();

        private KitStats kitStats(String kit) {
            KitStats stats = kits.get(kit);
            if (stats == null) {
                stats = new KitStats();
                kits.put(kit, stats);
            }
            return stats;
        }
    }

    private static final class KitStats {
        private int unrankedWins;
        private int unrankedLosses;
        private int rankedWins;
        private int rankedLosses;
        private int elo = 1000;
    }

    private static final class DuelRequest {
        private final UUID sender;
        private final KitData kit;
        private final ArenaSpawns arena;
        private final long expiresAt;

        private DuelRequest(UUID sender, KitData kit, ArenaSpawns arena, long expiresAt) {
            this.sender = sender;
            this.kit = kit;
            this.arena = arena;
            this.expiresAt = expiresAt;
        }

        private boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private static final class Party {
        private final UUID leader;
        private final Set<UUID> members = new HashSet<>();
        private boolean partyEventFfa = false;

        private Party(UUID leader) {
            this.leader = leader;
        }
    }

    private static final class PartyInvite {
        private final UUID sender;
        private final long expiresAt;

        private PartyInvite(UUID sender, long expiresAt) {
            this.sender = sender;
            this.expiresAt = expiresAt;
        }

        private boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private boolean detectIsBlue(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        int blueCount = 0;
        int redCount = 0;
        int radius = 8;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    if (isColorBlock(b.getType())) {
                        byte data = b.getData();
                        if (data == 11 || data == 3 || data == 9) { // Blue/Light Blue/Cyan
                            blueCount++;
                        } else if (data == 14 || data == 6 || data == 1) { // Red/Pink/Orange
                            redCount++;
                        }
                    }
                }
            }
        }
        return blueCount > redCount;
    }

    private boolean isColorBlock(Material m) {
        return m == Material.WOOL || m == Material.STAINED_CLAY || m == Material.STAINED_GLASS 
                || m == Material.STAINED_GLASS_PANE || m == Material.CARPET || m == Material.HARD_CLAY;
    }
    private enum LeaderboardType {
        RANKED("Ranked Leaderboards"),
        UNRANKED_ALL_TIME("Unranked (All Time)"),
        WIN_STREAK("Win Streak");

        private final String title;
        LeaderboardType(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private static final class LeaderboardEntry {
        private final UUID uuid;
        private final String name;
        private final int value;

        private LeaderboardEntry(UUID uuid, String name, int value) {
            this.uuid = uuid;
            this.name = name;
            this.value = value;
        }
    }

    private static final class LeaderboardPlacement {
        private final int rank;
        private final int value;
        private final String name;

        private LeaderboardPlacement(int rank, int value, String name) {
            this.rank = rank;
            this.value = value;
            this.name = name;
        }
    }

    private final Map<String, List<LeaderboardEntry>> cachedLeaderboards = new HashMap<>();

    private void refreshLeaderboards() {
        if (statsConfig == null) return;
        ConfigurationSection section = statsConfig.getConfigurationSection("players");
        if (section == null) return;
        Map<String, List<LeaderboardEntry>> temp = new HashMap<>();
        boolean changed = false;
        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String name = statsConfig.getString("players." + uuidStr + ".name", "Unknown");
                if ("Unknown".equals(name)) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    if (op.getName() != null) {
                        name = op.getName();
                        statsConfig.set("players." + uuidStr + ".name", name);
                        changed = true;
                    }
                }
                int globalWins = statsConfig.getInt("players." + uuidStr + ".wins", 0);
                int globalStreak = statsConfig.getInt("players." + uuidStr + ".winstreak", 0);
                temp.computeIfAbsent("GLOBAL_WINS", k -> new ArrayList<>()).add(new LeaderboardEntry(uuid, name, globalWins));
                temp.computeIfAbsent("GLOBAL_STREAK", k -> new ArrayList<>()).add(new LeaderboardEntry(uuid, name, globalStreak));
                ConfigurationSection kitsSection = statsConfig.getConfigurationSection("players." + uuidStr + ".kits");
                if (kitsSection != null) {
                    for (String kitKey : kitsSection.getKeys(false)) {
                        String path = "players." + uuidStr + ".kits." + kitKey + ".";
                        int elo = statsConfig.getInt(path + "elo", 1000);
                        int wins = statsConfig.getInt(path + "unranked-wins", 0) + statsConfig.getInt(path + "ranked-wins", 0);
                        temp.computeIfAbsent(kitKey + "_RANKED", k -> new ArrayList<>()).add(new LeaderboardEntry(uuid, name, elo));
                        temp.computeIfAbsent(kitKey + "_UNRANKED_ALL_TIME", k -> new ArrayList<>()).add(new LeaderboardEntry(uuid, name, wins));
                    }
                }
            } catch (Exception ignored) {}
        }
        cachedLeaderboards.clear();
        for (Map.Entry<String, List<LeaderboardEntry>> entry : temp.entrySet()) {
            List<LeaderboardEntry> list = entry.getValue();
            list.sort((a, b) -> Integer.compare(b.value, a.value));
            if (list.size() > 10) list = list.subList(0, 10);
            cachedLeaderboards.put(entry.getKey(), list);
        }
        if (changed) {
            saveStats();
        }
    }

    private void appendViewerLeaderboardPlacement(List<String> lore, Player viewer, String key) {
        if (viewer == null || lore == null) return;
        LeaderboardPlacement placement = computeLeaderboardPlacement(viewer.getUniqueId(), key);
        if (placement == null || placement.rank <= 10) return;

        lore.add(ChatColor.DARK_GRAY + "...");
        String div = ChatColor.translateAlternateColorCodes('&', getDivisionForPlayer(viewer.getUniqueId()));
        lore.add(ChatColor.GREEN + "" + placement.rank + ". " + div + " " + ChatColor.AQUA + placement.name + ChatColor.GRAY + " - " + ChatColor.YELLOW + placement.value);
    }

    private LeaderboardPlacement computeLeaderboardPlacement(UUID target, String key) {
        if (statsConfig == null || target == null || key == null) return null;
        ConfigurationSection players = statsConfig.getConfigurationSection("players");
        if (players == null) return null;

        Integer targetValue = leaderboardValueFor(target, key);
        if (targetValue == null) return null;

        String targetKey = target.toString();
        String targetName = statsConfig.getString("players." + targetKey + ".name", null);
        if (targetName == null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(target);
            targetName = op != null && op.getName() != null ? op.getName() : "Unknown";
        }

        int higher = 0;
        List<String> equals = new ArrayList<>();

        // Include target even if it isn't present in the config section (rare, but keeps output consistent)
        equals.add((targetName == null ? "Unknown" : targetName).toLowerCase(Locale.ROOT) + "|" + target.toString());

        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (Exception ignored) {
                continue;
            }
            if (uuid.equals(target)) continue;

            Integer value = leaderboardValueFor(uuid, key);
            if (value == null) continue;

            if (value > targetValue) {
                higher++;
            } else if (value == targetValue) {
                String name = statsConfig.getString("players." + uuidStr + ".name", uuidStr);
                equals.add((name == null ? uuidStr : name).toLowerCase(Locale.ROOT) + "|" + uuidStr);
            }
        }

        equals.sort(String::compareTo);
        String needle = (targetName == null ? "unknown" : targetName.toLowerCase(Locale.ROOT)) + "|" + target.toString();
        int index = equals.indexOf(needle);
        int rank = higher + (index >= 0 ? (index + 1) : 1);
        return new LeaderboardPlacement(rank, targetValue, targetName == null ? "Unknown" : targetName);
    }

    /**
     * Returns the numeric value used for ranking for a given leaderboard key, or null if the player
     * should not appear on that leaderboard (e.g., no stats for that kit).
     */
    private Integer leaderboardValueFor(UUID uuid, String key) {
        if (uuid == null || key == null || statsConfig == null) return null;
        String uuidStr = uuid.toString();

        if ("GLOBAL_WINS".equals(key)) {
            return statsConfig.getInt("players." + uuidStr + ".wins", 0);
        }
        if ("GLOBAL_STREAK".equals(key)) {
            return statsConfig.getInt("players." + uuidStr + ".winstreak", 0);
        }

        if (key.endsWith("_RANKED")) {
            String kit = key.substring(0, key.length() - "_RANKED".length());
            String base = "players." + uuidStr + ".kits." + kit + ".";
            if (!statsConfig.contains(base + "elo")) return null;
            return statsConfig.getInt(base + "elo", 1000);
        }

        if (key.endsWith("_UNRANKED_ALL_TIME")) {
            String kit = key.substring(0, key.length() - "_UNRANKED_ALL_TIME".length());
            String base = "players." + uuidStr + ".kits." + kit + ".";
            if (!statsConfig.contains(base + "unranked-wins") && !statsConfig.contains(base + "ranked-wins")) return null;
            return statsConfig.getInt(base + "unranked-wins", 0) + statsConfig.getInt(base + "ranked-wins", 0);
        }

        return null;
    }

    private void spawnAirLeaderboards() {
        removeAirLeaderboardEntities();
        if (airLeaderboardConfig == null) {
            return;
        }
        ConfigurationSection section = airLeaderboardConfig.getConfigurationSection("leaderboards");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String type = normalizeAirLeaderboardType(airLeaderboardConfig.getString("leaderboards." + key + ".type", key));
            if (type != null) {
                spawnAirLeaderboard(type);
            }
        }
    }

    private void updateAirLeaderboards() {
        if (airLeaderboardConfig == null || airLeaderboardConfig.getConfigurationSection("leaderboards") == null) {
            return;
        }
        Set<String> types = new HashSet<>(airLeaderboardEntities.keySet());
        types.addAll(airLeaderboardConfig.getConfigurationSection("leaderboards").getKeys(false));
        for (String key : types) {
            String type = normalizeAirLeaderboardType(airLeaderboardConfig.getString("leaderboards." + key + ".type", key));
            if (type != null) {
                spawnAirLeaderboard(type);
            }
        }
    }

    /** Updates ArmorStand leaderboard names without respawning entities (runs every 20 ticks). */
    private void tickAirLeaderboards() {
        if (airLeaderboardConfig == null) {
            return;
        }
        ConfigurationSection section = airLeaderboardConfig.getConfigurationSection("leaderboards");
        if (section == null) {
            return;
        }

        // Ensure any newly-added types get spawned
        for (String key : section.getKeys(false)) {
            String type = normalizeAirLeaderboardType(airLeaderboardConfig.getString("leaderboards." + key + ".type", key));
            if (type != null && !airLeaderboardEntities.containsKey(type)) {
                spawnAirLeaderboard(type);
            }
        }

        for (String type : new HashSet<>(airLeaderboardEntities.keySet())) {
            List<String> lines = airLeaderboardLines(type);
            List<UUID> spawned = airLeaderboardEntities.get(type);
            if (spawned == null || spawned.size() != lines.size()) {
                spawnAirLeaderboard(type);
                continue;
            }
            boolean bad = false;
            for (int i = 0; i < spawned.size(); i++) {
                UUID id = spawned.get(i);
                ArmorStand stand = airLeaderboardStandById.get(id);
                if (stand == null) {
                    Entity entity = findEntity(id);
                    if (entity instanceof ArmorStand) {
                        stand = (ArmorStand) entity;
                        airLeaderboardStandById.put(id, stand);
                    }
                }
                if (stand == null || !stand.isValid()) {
                    bad = true;
                    break;
                }
                stand.setCustomName(color(lines.get(i)));
                stand.setCustomNameVisible(true);
            }
            if (bad) {
                spawnAirLeaderboard(type);
            }
        }
    }

    private void spawnAirLeaderboard(String type) {
        removeAirLeaderboard(type);
        Location location = parseLocation(airLeaderboardConfig.getString("leaderboards." + type + ".location"), spawnLocation == null ? null : spawnLocation.getWorld());
        if (location == null || location.getWorld() == null) {
            return;
        }
        List<String> lines = airLeaderboardLines(type);
        double spacing = airLeaderboardConfig.getDouble("settings.line-spacing", 0.28D);
        List<UUID> spawned = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Location lineLocation = location.clone().add(0.0D, -spacing * i, 0.0D);
            ArmorStand stand = location.getWorld().spawn(lineLocation, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setCustomName(color(lines.get(i)));
            stand.setCustomNameVisible(true);
            try {
                ArmorStand.class.getMethod("setMarker", boolean.class).invoke(stand, true);
            } catch (Exception ignored) {
            }
            airLeaderboardStandById.put(stand.getUniqueId(), stand);
            spawned.add(stand.getUniqueId());
        }
        airLeaderboardEntities.put(type, spawned);
    }

    private List<String> airLeaderboardLines(String type) {
        List<String> lines = new ArrayList<>();
        lines.add(airLeaderboardConfig.getString("formats." + type + ".title", "&6&l" + displayAirLeaderboardType(type) + " Leaderboard"));
        List<LeaderboardEntry> entries = leaderboardEntriesForAirType(type);
        int max = Math.max(1, airLeaderboardConfig.getInt("settings.max-entries", 10));
        String format = airLeaderboardConfig.getString("formats." + type + ".line", "&e<number>. <luckperms_prefix>&f<player> &7- &6<value>");
        // IMPORTANT: Keep a constant line count (title + max lines) so we don't have to respawn armorstands
        // when the amount of data changes. Respawning causes visible flicker.
        if (entries.isEmpty()) {
            lines.add(airLeaderboardConfig.getString("formats.empty", "&7No leaderboard data yet."));
            while (lines.size() < (1 + max)) {
                lines.add(" ");
            }
            return lines;
        }
        for (int i = 0; i < max; i++) {
            if (i >= entries.size()) {
                lines.add(" ");
                continue;
            }
            LeaderboardEntry entry = entries.get(i);
            String lpPrefix = luckPermsPrefix(entry.uuid);
            String lpSuffix = luckPermsSuffix(entry.uuid);
            String lpName = lpPrefix + entry.name + lpSuffix;
            lines.add(format
                    .replace("<number>", String.valueOf(i + 1))
                    .replace("<position>", String.valueOf(i + 1))
                    .replace("<luckperms_nametag>", lpName)
                    .replace("<display_name>", lpName)
                    .replace("<plain_player>", entry.name)
                    .replace("<plain_name>", entry.name)
                    .replace("<player>", lpName)
                    .replace("<name>", lpName)
                    .replace("<value>", String.valueOf(entry.value))
                    .replace("<luckperms_prefix>", lpPrefix)
                    .replace("<luckperms_suffix>", lpSuffix));
        }
        return lines;
    }

    private int airLeaderboardExpectedLineCount() {
        if (airLeaderboardConfig == null) return 0;
        int max = Math.max(1, airLeaderboardConfig.getInt("settings.max-entries", 10));
        return 1 + max; // title + max entries (even if blank)
    }

    /**
     * Removes any ArmorStands at the configured leaderboard locations, even if they were spawned by
     * an older plugin version and are no longer tracked in-memory.
     *
     * @param type optional normalized type (wins/winstreak/daily_streak/elo). If null, purges all configured leaderboards.
     * @return number of ArmorStands removed
     */
    private int purgeOrphanAirLeaderboards(String type) {
        if (airLeaderboardConfig == null) return 0;
        ConfigurationSection section = airLeaderboardConfig.getConfigurationSection("leaderboards");
        if (section == null) return 0;

        int removed = 0;
        Set<String> keys = new HashSet<>(section.getKeys(false));
        for (String key : keys) {
            String t = normalizeAirLeaderboardType(airLeaderboardConfig.getString("leaderboards." + key + ".type", key));
            if (t == null) continue;
            if (type != null && !type.equals(t)) continue;
            removed += purgeOrphanAirLeaderboard(t);
        }
        return removed;
    }

    private int purgeOrphanAirLeaderboards() {
        return purgeOrphanAirLeaderboards(null);
    }

    private int purgeOrphanAirLeaderboard(String type) {
        if (airLeaderboardConfig == null || type == null) return 0;
        Location location = parseLocation(airLeaderboardConfig.getString("leaderboards." + type + ".location"), spawnLocation == null ? null : spawnLocation.getWorld());
        if (location == null || location.getWorld() == null) return 0;

        double spacing = airLeaderboardConfig.getDouble("settings.line-spacing", 0.28D);
        int lines = Math.max(1, airLeaderboardExpectedLineCount());
        int removed = 0;

        // Also remove currently tracked entities for this type
        removeAirLeaderboard(type);

        for (int i = 0; i < lines; i++) {
            Location lineLocation = location.clone().add(0.0D, -spacing * i, 0.0D);
            try {
                for (Entity entity : lineLocation.getWorld().getNearbyEntities(lineLocation, 0.35D, 0.35D, 0.35D)) {
                    if (entity instanceof ArmorStand) {
                        entity.remove();
                        removed++;
                    }
                }
            } catch (Exception ignored) {}
        }
        return removed;
    }

    /**
     * Emergency cleanup when the config entry was deleted but the armorstands remained.
     * This intentionally ONLY removes armorstands that look like our leaderboard holograms.
     */
    private int purgeNearbyLeaderboardArmorStands(Location center, double radius) {
        if (center == null || center.getWorld() == null) return 0;
        int removed = 0;
        try {
            for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (!(entity instanceof ArmorStand)) continue;
                ArmorStand stand = (ArmorStand) entity;
                if (!looksLikeOurLeaderboardArmorStand(stand)) continue;
                stand.remove();
                removed++;
            }
        } catch (Exception ignored) {}
        return removed;
    }

    private boolean looksLikeOurLeaderboardArmorStand(ArmorStand stand) {
        if (stand == null) return false;
        try {
            if (stand.isVisible()) return false;
            if (stand.hasGravity()) return false;
            if (!stand.isSmall()) return false;
            if (!stand.isCustomNameVisible()) return false;
            String name = stand.getCustomName();
            if (name == null) return false;
            String stripped = ChatColor.stripColor(name);
            if (stripped == null) return false;
            stripped = stripped.trim();
            // Very specific to our formats:
            // - "<Type> Leaderboard"
            // - "1. <name> - <value>" lines
            // - "No leaderboard data yet."
            if (stripped.endsWith("Leaderboard")) return true;
            if (stripped.matches("^\\d+\\..*")) return true;
            if (stripped.equalsIgnoreCase("No leaderboard data yet.")) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private List<LeaderboardEntry> leaderboardEntriesForAirType(String type) {
        if ("wins".equals(type)) {
            return cachedLeaderboards.getOrDefault("GLOBAL_WINS", Collections.emptyList());
        }
        if ("winstreak".equals(type) || "daily_streak".equals(type)) {
            return cachedLeaderboards.getOrDefault("GLOBAL_STREAK", Collections.emptyList());
        }
        if ("elo".equals(type)) {
            Map<UUID, LeaderboardEntry> bestByPlayer = new HashMap<>();
            for (Map.Entry<String, List<LeaderboardEntry>> leaderboard : cachedLeaderboards.entrySet()) {
                if (!leaderboard.getKey().endsWith("_RANKED")) {
                    continue;
                }
                for (LeaderboardEntry entry : leaderboard.getValue()) {
                    LeaderboardEntry current = bestByPlayer.get(entry.uuid);
                    if (current == null || entry.value > current.value) {
                        bestByPlayer.put(entry.uuid, entry);
                    }
                }
            }
            List<LeaderboardEntry> entries = new ArrayList<>(bestByPlayer.values());
            entries.removeIf(entry -> entry.value <= 0);
            entries.sort((a, b) -> Integer.compare(b.value, a.value));
            return entries.size() > 10 ? entries.subList(0, 10) : entries;
        }
        return Collections.emptyList();
    }

    private String luckPermsPrefix(UUID uuid) {
        return luckPermsMeta(uuid, true);
    }

    private String luckPermsSuffix(UUID uuid) {
        return luckPermsMeta(uuid, false);
    }

    private String luckPermsMeta(UUID uuid, boolean prefixMeta) {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            return "";
        }
        try {
            User user = provider.getProvider().getUserManager().getUser(uuid);
            if (user == null) {
                user = provider.getProvider().getUserManager().loadUser(uuid).join();
            }
            String value = user == null ? null : prefixMeta
                    ? user.getCachedData().getMetaData().getPrefix()
                    : user.getCachedData().getMetaData().getSuffix();
            return value == null ? "" : color(value);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void removeAirLeaderboard(String type) {
        List<UUID> entities = airLeaderboardEntities.remove(type);
        if (entities == null) {
            return;
        }
        for (UUID uuid : entities) {
            airLeaderboardStandById.remove(uuid);
            Entity entity = findEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    private Entity findEntity(UUID uuid) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uuid)) {
                    return entity;
                }
            }
        }
        return null;
    }

    private void removeAirLeaderboardEntities() {
        for (String type : new HashSet<>(airLeaderboardEntities.keySet())) {
            removeAirLeaderboard(type);
        }
        airLeaderboardStandById.clear();
    }

    private String getDivision(int wins) {

        if (divisionsConfig != null && divisionsConfig.contains("divisions")) {
            ConfigurationSection sec = divisionsConfig.getConfigurationSection("divisions");
            if (sec != null) {
                // Find the division with the highest wins requirement that the player meets
                String bestPrefix = "&8Iron I";
                int bestWins = -1;
                for (String key : sec.getKeys(false)) {
                    int req = sec.getInt(key + ".wins", 0);
                    if (wins >= req && req > bestWins) {
                        bestWins = req;
                        bestPrefix = sec.getString(key + ".prefix", bestPrefix);
                    }
                }
                return bestPrefix;
            }
        }
        return "&8Iron I";
    }

    private void updateLuckPermsPrefix(Player player) {
        // Strip out the old nametag prefix we set in the first version so it removes it from TAB/nametags
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            LuckPerms lp = provider.getProvider();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                user.data().clear(node -> node.getType() == NodeType.PREFIX
                        && NodeType.PREFIX.cast(node).getPriority() == 100);
                lp.getUserManager().saveUser(user);
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        PlayerSettings s = settings(player);
        PlayerStats stats = stats(player.getUniqueId());

        if (s.globalChatMode == 2) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Your chat is disabled.");
            return;
        }

        event.getRecipients().removeIf(recipient -> {
            if (recipient.equals(player)) {
                return false;
            }
            int senderMode = s.globalChatMode;
            int recipientMode = settings(recipient).globalChatMode;
            boolean senderFriend = stats.friends.contains(recipient.getUniqueId());
            boolean recipientFriend = stats(recipient.getUniqueId()).friends.contains(player.getUniqueId());
            return recipientMode == 2
                    || (senderMode == 1 && !senderFriend)
                    || (recipientMode == 1 && !recipientFriend);
        });

        // If disabled, just let EssentialsChat or default chat handle it
        if (!s.divisionsOnNametag) {
            return;
        }

        // Build division prefix
        String divisionRaw = getDivisionForPlayer(player.getUniqueId());
        String divisionColored = ChatColor.translateAlternateColorCodes('&', divisionRaw);

        // Get chat format from divisions.yml, falling back to just a space if missing.
        String formatString = "<division> <format>";
        if (divisionsConfig != null && divisionsConfig.contains("chat-format")) {
            formatString = divisionsConfig.getString("chat-format", formatString);
        }

        String currentFormat = event.getFormat();

        // Apply placeholders
        String finalFormat = formatString
                .replace("<division>", divisionColored)
                .replace("<format>", currentFormat);

        // Prepend the division to whatever chat format is already set (e.g. by LPC)
        // This perfectly preserves the exact colors and role styles from LuckPerms!
        event.setFormat(ChatColor.translateAlternateColorCodes('&', finalFormat));
    }

    private void handleDivisionsCommand(Player player) {
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "---------------------------------------------");
        player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + " PRACTICE DIVISIONS");
        player.sendMessage("");

        if (divisionsConfig != null && divisionsConfig.contains("divisions")) {
            ConfigurationSection sec = divisionsConfig.getConfigurationSection("divisions");
            if (sec != null) {
                // Get all division keys and sort them by wins requirement
                List<String> keys = new ArrayList<>(sec.getKeys(false));
                keys.sort((k1, k2) -> Integer.compare(sec.getInt(k1 + ".wins", 0), sec.getInt(k2 + ".wins", 0)));

                for (String key : keys) {
                    String prefix = color(sec.getString(key + ".prefix", ""));
                    int wins = sec.getInt(key + ".wins", 0);
                    player.sendMessage(prefix + ChatColor.GRAY + " - " + ChatColor.WHITE + wins + " Wins");
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Divisions configuration not loaded.");
        }

        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Your Wins: " + ChatColor.YELLOW + stats(player.getUniqueId()).wins);
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "---------------------------------------------");
    }

    private void handleFriendCommand(Player player, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            PlayerStats stats = stats(player.getUniqueId());
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Friends List:");
            if (stats.friends.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "You have no friends added.");
            } else {
                for (UUID friendId : stats.friends) {
                    OfflinePlayer friend = Bukkit.getOfflinePlayer(friendId);
                    String status = friend.isOnline() ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline";
                    player.sendMessage(ChatColor.YELLOW + " - " + friend.getName() + " (" + status + ChatColor.YELLOW + ")");
                }
            }
            return;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /friend add <player>");
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "That player is not online.");
                return;
            }
            if (target.equals(player)) {
                player.sendMessage(ChatColor.RED + "You cannot friend yourself.");
                return;
            }
            if (!settings(target).friendRequests) {
                player.sendMessage(ChatColor.RED + "That player is not accepting friend requests.");
                return;
            }
            PlayerStats targetStats = stats(target.getUniqueId());
            if (targetStats.friends.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You are already friends with " + target.getName());
                return;
            }
            if (targetStats.pendingFriends.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You have already sent a friend request to " + target.getName());
                return;
            }
            targetStats.pendingFriends.add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Friend request sent to " + target.getName());
            target.sendMessage(color("&6" + player.getName() + " &fhas sent you a &6friend request&f."));
            sendActionButtons(target,
                    "&a&l[ACCEPT FRIEND]", "/friend accept " + player.getName(), "&aClick to accept " + player.getName() + "'s friend request.",
                    "&c&l[DENY]", "/friend deny " + player.getName(), "&7Click to deny this friend request.");
            return;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /friend accept <player>");
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "That player is not online.");
                return;
            }
            PlayerStats stats = stats(player.getUniqueId());
            if (!stats.pendingFriends.contains(target.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You do not have a pending friend request from " + target.getName());
                return;
            }
            stats.pendingFriends.remove(target.getUniqueId());
            stats.friends.add(target.getUniqueId());
            PlayerStats targetStats = stats(target.getUniqueId());
            targetStats.friends.add(player.getUniqueId());
            
            writeStats(player.getUniqueId(), stats);
            writeStats(target.getUniqueId(), targetStats);
            saveStats();

            player.sendMessage(ChatColor.GREEN + "You are now friends with " + target.getName() + "!");
            target.sendMessage(ChatColor.GREEN + player.getName() + " has accepted your friend request!");
            return;
        }

        if (args[0].equalsIgnoreCase("deny")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /friend deny <player>");
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "That player is not online.");
                return;
            }
            PlayerStats stats = stats(player.getUniqueId());
            if (!stats.pendingFriends.contains(target.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You do not have a pending friend request from " + target.getName());
                return;
            }
            stats.pendingFriends.remove(target.getUniqueId());
            writeStats(player.getUniqueId(), stats);
            saveStats();
            player.sendMessage(ChatColor.RED + "Denied " + target.getName() + "'s friend request.");
            target.sendMessage(ChatColor.YELLOW + player.getName() + " denied your friend request.");
            return;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /friend remove <player>");
                return;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            PlayerStats stats = stats(player.getUniqueId());
            if (!stats.friends.contains(target.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You are not friends with " + target.getName());
                return;
            }
            stats.friends.remove(target.getUniqueId());
            PlayerStats targetStats = stats(target.getUniqueId());
            targetStats.friends.remove(player.getUniqueId());
            
            writeStats(player.getUniqueId(), stats);
            writeStats(target.getUniqueId(), targetStats);
            saveStats();

            player.sendMessage(ChatColor.YELLOW + "Removed " + target.getName() + " from your friends list.");
            return;
        }
    }

    private void openSpectatorTrackerMenu(Player player) {
        Match match = spectatingMatch.get(player.getUniqueId());
        if (match == null) return;
        
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_GRAY + "Spectate Tracker");
        
        Player p1 = Bukkit.getPlayer(match.first);
        if (p1 != null) {
            inv.addItem(createSpectateItem(p1));
        }
        Player p2 = Bukkit.getPlayer(match.second);
        if (p2 != null) {
            inv.addItem(createSpectateItem(p2));
        }
        
        player.openInventory(inv);
    }

    private ItemStack createSpectateItem(Player target) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(target.getName());
        meta.setDisplayName(ChatColor.GREEN + target.getName());
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Click to teleport to this player."));
        item.setItemMeta(meta);
        return item;
    }

    private boolean isNearSpawnpoint(Location placed, Location spawn) {
        if (spawn == null || placed == null) {
            return false;
        }
        if (placed.getWorld() != spawn.getWorld()) {
            return false;
        }
        if (placed.getBlockX() == spawn.getBlockX() && placed.getBlockZ() == spawn.getBlockZ()) {
            if (placed.getBlockY() >= spawn.getBlockY() && placed.getBlockY() <= spawn.getBlockY() + 2) {
                return true;
            }
        }
        return false;
    }

    private void updatePlayerHealthForMatch(Player player) {
        Match match = matches.get(player.getUniqueId());
        if (match == null || !match.started || match.kit == null) {
            return;
        }
        KitData kit = match.kit;
        boolean shouldHaveHealth = !kit.boxing && !kit.sumo 
                && !kit.name.toLowerCase(Locale.ROOT).contains("battlerush") 
                && !kit.name.toLowerCase(Locale.ROOT).contains("stickfight") 
                && !kit.name.toLowerCase(Locale.ROOT).contains("spleef");
        if (!shouldHaveHealth) {
            return;
        }
        
        int healthVal = (int) Math.ceil(player.getHealth());
        
        Player first = Bukkit.getPlayer(match.first);
        Player second = Bukkit.getPlayer(match.second);
        
        if (first != null) {
            Scoreboard board = first.getScoreboard();
            if (board != null) {
                Objective healthObj = board.getObjective("health");
                if (healthObj != null) {
                    healthObj.getScore(player.getName()).setScore(healthVal);
                }
            }
        }
        if (second != null) {
            Scoreboard board = second.getScoreboard();
            if (board != null) {
                Objective healthObj = board.getObjective("health");
                if (healthObj != null) {
                    healthObj.getScore(player.getName()).setScore(healthVal);
                }
            }
        }
    }

    private boolean isInvisOrSpectator(Player player) {
        if (player == null) {
            return false;
        }
        if (spectatingMatch.containsKey(player.getUniqueId())) {
            return true;
        }
        if (respawningPlayers.contains(player.getUniqueId())) {
            return true;
        }
        Match match = matches.get(player.getUniqueId());
        if (match != null && match.ending) {
            return true;
        }
        return false;
    }

    private boolean isPlayerIntersectingBlock(Player player, Location blockLoc) {
        if (player.getWorld() != blockLoc.getWorld()) {
            return false;
        }
        Location playerLoc = player.getLocation();
        double px = playerLoc.getX();
        double py = playerLoc.getY();
        double pz = playerLoc.getZ();
        
        double bx = blockLoc.getBlockX();
        double by = blockLoc.getBlockY();
        double bz = blockLoc.getBlockZ();
        
        if (Math.abs(px - (bx + 0.5)) < 0.8 && Math.abs(pz - (bz + 0.5)) < 0.8) {
            if (py < by + 1.0 && py + 1.8 > by) {
                return true;
            }
        }
        return false;
    }
}
