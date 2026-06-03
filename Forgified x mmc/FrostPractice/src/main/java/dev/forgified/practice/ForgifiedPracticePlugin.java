package dev.forgified.practice;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.Vector;

import java.io.File;
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
    private static final String EVENTS_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Host Event";
    private static final String PARTY_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Party";

    private final Map<String, KitData> kits = new LinkedHashMap<>();
    private final List<ArenaSpawns> arenas = new ArrayList<>();
    private final Set<ArenaSpawns> busyArenas = new HashSet<>();
    private final Map<String, Queue<UUID>> queues = new HashMap<>();
    private final Map<UUID, String> queuedKit = new HashMap<>();
    private final Map<UUID, String> queuedType = new HashMap<>();
    private final Map<UUID, Long> queuedAt = new HashMap<>();
    private final Map<UUID, Match> matches = new HashMap<>();
    private final Map<UUID, DuelRequest> duelRequests = new HashMap<>();
    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, PartyInvite> partyInvites = new HashMap<>();
    private final Map<UUID, PlayerSettings> playerSettings = new HashMap<>();
    private final Map<UUID, EventSession> activeEvents = new HashMap<>();
    private final Map<String, List<HotbarItem>> hotbars = new HashMap<>();
    private final Set<UUID> pluginTeleports = new HashSet<>();
    private final Set<String> filledEventChests = new HashSet<>();
    private final Set<String> bedRespawnKits = new HashSet<>();
    private final Random random = new Random();

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
    private Location spawnLocation;
    private Location spawnMin;
    private Location spawnMax;
    private int countdownSeconds;
    private int voidY;
    private boolean useFrostHotbar;
    private boolean scoreboardEnabled;
    private String scoreboardTitle;
    private String prefix;
    private BukkitTask scoreboardTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPractice();
        Bukkit.getPluginManager().registerEvents(this, this);
        scoreboardTask = Bukkit.getScheduler().runTaskTimer(this, this::refreshScoreboards, 20L, 20L);
        registerCommand("frostpractice");
        registerCommand("practice");
        registerCommand("queue");
        registerCommand("duel");
        registerCommand("party");
        registerCommand("settings");
        registerCommand("unqueue");
        registerCommand("spawn");
        registerCommand("leave");
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
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }
    }

    private void registerCommand(String name) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(this);
            getCommand(name).setTabCompleter(this);
        }
    }

    private void reloadPractice() {
        countdownSeconds = 3;
        voidY = 0;
        useFrostHotbar = true;
        scoreboardEnabled = true;
        bedRespawnKits.clear();
        Collections.addAll(bedRespawnKits, normalize("BedFight"), normalize("FireballFight"), normalize("BattleRush"), normalize("MLGRush"), normalize("Bridge"));

        File frostFolder = new File(getDataFolder().getParentFile(), "Frost");
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
        scoreboardTitle = color(frostScoreboard.getString("SCOREBOARD.TITLE", "&6&lPractice"));
        prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Practice" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

        World fallbackWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        spawnLocation = parseLocation(frostConfig.getString("spawnLocation"), fallbackWorld);
        if (spawnLocation == null && fallbackWorld != null) {
            spawnLocation = fallbackWorld.getSpawnLocation();
        }
        spawnMin = parseLocation(frostConfig.getString("spawnMin"), spawnLocation == null ? fallbackWorld : spawnLocation.getWorld());
        spawnMax = parseLocation(frostConfig.getString("spawnMax"), spawnLocation == null ? fallbackWorld : spawnLocation.getWorld());

        loadKits(frostKits);
        loadArenas(frostArenas, fallbackWorld);
        loadHotbars(frostHotbar);
        getLogger().info("Loaded Frost folder: kits=" + kits.size() + ", arenas=" + arenas.size()
                + ", hotbars=" + hotbars.size() + ", chestPresets=" + frostChest.getConfigurationSection("CHESTS"));
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
            kits.put(normalize(name), new KitData(name, display, icon, contents, armor, unrankedPos, rankedPos, editorPos, spawnFfaPos));
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
            items.add(new HotbarItem(
                    file.getInt(path + ".SLOT", 0),
                    material,
                    (short) file.getInt(path + ".DATA", 0),
                    Math.max(1, file.getInt(path + ".AMOUNT", 1)),
                    color(file.getString(path + ".NAME", key)),
                    file.getString(path + ".ACTION", key),
                    file.getString(path + ".COMMAND", "")));
        }
        hotbars.put(sectionName, items);
    }

    private void loadFallbackHotbars() {
        hotbars.put("IN-SPAWN", Arrays.asList(
                new HotbarItem(0, Material.IRON_SWORD, (short) 0, 1, ChatColor.GOLD + "Unranked Queue " + ChatColor.GRAY + "(Right Click)", "JOIN_UNRANKED", ""),
                new HotbarItem(1, Material.DIAMOND_SWORD, (short) 0, 1, ChatColor.GOLD + "Duel " + ChatColor.GRAY + "(Right Click)", "DUEL_MENU", ""),
                new HotbarItem(4, Material.NAME_TAG, (short) 0, 1, ChatColor.GOLD + "Create Party " + ChatColor.GRAY + "(Right Click)", "CREATE_PARTY", ""),
                new HotbarItem(6, Material.EMERALD, (short) 0, 1, ChatColor.GOLD + "Leaderboards " + ChatColor.GRAY + "(Right Click)", "LEADERBOARDS_MENU", ""),
                new HotbarItem(7, Material.EYE_OF_ENDER, (short) 0, 1, ChatColor.GOLD + "Host Events " + ChatColor.GRAY + "(Right Click)", "EVENTS_MENU", ""),
                new HotbarItem(8, Material.SKULL_ITEM, (short) 3, 1, ChatColor.GOLD + "Settings " + ChatColor.GRAY + "(Right Click)", "SETTINGS_MENU", "")));
        hotbars.put("IN-QUEUE", Collections.singletonList(
                new HotbarItem(8, Material.INK_SACK, (short) 1, 1, ChatColor.RED + "Leave Queue " + ChatColor.GRAY + "(Right Click)", "LEAVE_QUEUE", "")));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (!(sender instanceof Player)) {
            if (name.equals("practice") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadPractice();
                sender.sendMessage("ForgifiedPractice reloaded.");
                return true;
            }
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;
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
        if (name.equals("settings")) {
            openSettingsMenu(player);
            return true;
        }
        if (name.equals("unqueue")) {
            leaveQueue(player, true);
            return true;
        }
        if (name.equals("spawn")) {
            returnToSpawn(player, true);
            return true;
        }
        if (name.equals("leave")) {
            Match match = matches.get(player.getUniqueId());
            if (match == null) {
                message(player, "not-fighting");
                return true;
            }
            finishMatch(match, match.other(player.getUniqueId()), player.getName() + " left");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload") && player.hasPermission("forgifiedpractice.admin")) {
            reloadPractice();
            player.sendMessage(prefix + ChatColor.GREEN + "Reloaded Frost kits and arenas.");
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
        openQueueMenu(player, "unranked");
        return true;
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
            return startsWith(args[0], Arrays.asList("create", "invite", "accept", "leave", "disband", "info"));
        }
        if (args.length == 2 && command.getName().equalsIgnoreCase("party") && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("accept"))) {
            return startsWith(args[1], onlinePlayerNames());
        }
        if (args.length == 1 && command.getName().equalsIgnoreCase("practice")) {
            List<String> values = new ArrayList<>(Arrays.asList("queue", "duel", "party", "settings", "reload"));
            values.addAll(kitNames());
            return startsWith(args[0], values);
        }
        if (args.length == 2 && command.getName().equalsIgnoreCase("practice") && args[0].equalsIgnoreCase("queue")) {
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
            ItemStack icon = kit.icon == null ? new ItemStack(Material.DIAMOND_SWORD) : kit.icon.clone();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(frostMenus.getString(menuPath + ".NAME", "&6&l<kit_name>").replace("<kit_name>", kit.name)));
                List<String> lore = new ArrayList<>();
                for (String line : frostMenus.getStringList(menuPath + ".LORE")) {
                    if (line.contains("<top-3>")) {
                        continue;
                    }
                    lore.add(color(line
                            .replace("<fighting_unranked>", String.valueOf(matches.size() / 2))
                            .replace("<queueing_unranked>", String.valueOf(queueSize("unranked", normalize(kit.name))))
                            .replace("<fighting_ranked>", String.valueOf(matches.size() / 2))
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
        if (EVENTS_TITLE.equals(event.getInventory().getTitle()) || color(frostMenus.getString("EVENTS-INVENTORY.TITLE", "")).equals(event.getInventory().getTitle())) {
            event.setCancelled(true);
            handleEventMenuClick((Player) event.getWhoClicked(), event.getSlot());
            return;
        }
        if (isUtilityMenu(event.getInventory().getTitle())) {
            event.setCancelled(true);
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

    private boolean isQueueMenu(String title) {
        return MENU_TITLE.equals(title)
                || color(frostMenus.getString("QUEUE-INVENTORY.UNRANKED.TITLE", "")).equals(title)
                || color(frostMenus.getString("QUEUE-INVENTORY.RANKED.TITLE", "")).equals(title);
    }

    private boolean isRankedQueueMenu(String title) {
        return color(frostMenus.getString("QUEUE-INVENTORY.RANKED.TITLE", "")).equals(title);
    }

    private boolean isUtilityMenu(String title) {
        return LEADERBOARDS_TITLE.equals(title)
                || EVENTS_TITLE.equals(title)
                || PARTY_TITLE.equals(title)
                || color(frostMenus.getString("LEADERBOARDS-INVENTORY.GLOBAL-ELO.TITLE", "")).equals(title)
                || color(frostMenus.getString("EVENTS-INVENTORY.TITLE", "")).equals(title)
                || color(frostMenus.getString("QUEUE-INVENTORY.PARTY-OTHER-PARTIES-INVENTORY-TITLE", "")).equals(title);
    }

    private int queueSize(String type, String kit) {
        Queue<UUID> queue = queues.get(queueKey(type, kit));
        return queue == null ? 0 : queue.size();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (event.getPlayer().isOnline() && !matches.containsKey(event.getPlayer().getUniqueId())) {
                giveStateHotbar(event.getPlayer());
                updateLobbyScoreboard(event.getPlayer());
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHotbarInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (matches.containsKey(player.getUniqueId())) {
            return;
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
            openLeaderboardsMenu(player);
        } else if (action.equals("EVENTS_MENU") || action.equals("PARTY_EVENTS")) {
            openEventsMenu(player);
        } else if (action.equals("OTHER_PARTIES")) {
            openPartyMenu(player);
        } else if (action.equals("EDITOR_MENU")) {
            openKitEditorMenu(player);
        } else if (action.equals("JOIN_FFA")) {
            joinFfaSpawn(player);
        } else if (action.equals("OPEN_CURRENT_MATCHES")) {
            openCurrentMatchesMenu(player);
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
        Inventory inventory = Bukkit.createInventory(null, 9, color(frostMenus.getString("SETTINGS-INVENTORY.TITLE", SETTINGS_TITLE)));
        PlayerSettings settings = settings(player);
        inventory.setItem(0, menuItem(Material.DIAMOND_SWORD, (short) 0, settingName("DUEL_REQUESTS", "Toggle Duel Requests"), settings.duelRequests));
        inventory.setItem(1, menuItem(Material.PAPER, (short) 0, settingName("PARTY_REQUESTS", "Party Invite Requests"), settings.partyRequests));
        inventory.setItem(2, menuItem(Material.EMERALD, (short) 0, settingName("ALLOW_SPECTATORS", "Allow Spectators"), settings.allowSpectators));
        inventory.setItem(3, menuItem(Material.PAINTING, (short) 0, settingName("TOGGLE_SCOREBOARD", "Toggle Scoreboard"), settings.scoreboard));
        inventory.setItem(4, menuItem(Material.FLINT, (short) 0, settingName("PING_ON_SCOREBOARD", "Show Ping on Scoreboard"), settings.pingOnScoreboard));
        inventory.setItem(5, menuItem(Material.QUARTZ, (short) 0, settingName("VANILLA_TAB", "Show Vanilla Tablist"), settings.vanillaTab));
        inventory.setItem(8, simpleItem(Material.SKULL_ITEM, (short) 0, settingName("DEATH_EFFECT_SETTINGS", "Death Effects"), ChatColor.GRAY + "Coming next."));
        player.openInventory(inventory);
    }

    private String settingName(String key, String fallback) {
        return color(frostMenus.getString("SETTINGS-INVENTORY.SETTINGS." + key + ".NAME", "&6&l" + fallback));
    }

    private void handleSettingsClick(Player player, int slot) {
        PlayerSettings settings = settings(player);
        if (slot == 0) {
            settings.duelRequests = !settings.duelRequests;
        } else if (slot == 1) {
            settings.partyRequests = !settings.partyRequests;
        } else if (slot == 2) {
            settings.allowSpectators = !settings.allowSpectators;
        } else if (slot == 3) {
            settings.scoreboard = !settings.scoreboard;
            if (!settings.scoreboard) {
                resetScoreboard(player);
            } else {
                updateLobbyScoreboard(player);
            }
        } else if (slot == 4) {
            settings.pingOnScoreboard = !settings.pingOnScoreboard;
        } else if (slot == 5) {
            settings.vanillaTab = !settings.vanillaTab;
        }
        openSettingsMenu(player);
    }

    private void openLeaderboardsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, color(frostMenus.getString("LEADERBOARDS-INVENTORY.GLOBAL-ELO.TITLE", LEADERBOARDS_TITLE).replace("<kit>", "Global")));
        int slot = 0;
        for (KitData kit : kits.values()) {
            if (slot >= inventory.getSize()) {
                break;
            }
            ItemStack icon = kit.icon == null ? new ItemStack(Material.CARPET, 1, (short) 11) : kit.icon.clone();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + kit.name + ChatColor.GRAY + " | Top 10");
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Stats storage is coming next.", ChatColor.GRAY + "Kit loaded from Frost files."));
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot++, icon);
        }
        player.openInventory(inventory);
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
        String normalized = event.toUpperCase(Locale.ROOT).replace("-", "_");
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

    private void openKitEditorMenu(Player player) {
        int size = Math.min(54, Math.max(9, ((kits.size() + 8) / 9) * 9));
        Inventory inventory = Bukkit.createInventory(null, size, color(frostMenus.getString("QUEUE-INVENTORY.DUEL-INVENTORY-TITLE", "&6&lSelect A Kit")));
        int fallbackSlot = 0;
        Set<Integer> occupied = new HashSet<>();
        for (KitData kit : kits.values()) {
            int slot = kit.editorPos;
            if (slot < 0 || slot >= size || occupied.contains(slot)) {
                while (fallbackSlot < size && occupied.contains(fallbackSlot)) {
                    fallbackSlot++;
                }
                if (fallbackSlot >= size) {
                    break;
                }
                slot = fallbackSlot;
            }
            ItemStack icon = kit.icon == null ? new ItemStack(Material.BOOK) : kit.icon.clone();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + kit.name);
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Click to preview this Frost kit.", ChatColor.GRAY + "Use /queue " + kit.name + " to play it."));
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot, icon);
            occupied.add(slot);
        }
        player.openInventory(inventory);
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
        int slot = 0;
        Set<Party> unique = new HashSet<>(parties.values());
        for (Party party : unique) {
            if (slot >= inventory.getSize()) {
                break;
            }
            Player leader = Bukkit.getPlayer(party.leader);
            inventory.setItem(slot++, simpleItem(Material.DIAMOND_AXE, (short) 0,
                    ChatColor.GOLD + (leader == null ? "Party" : leader.getName() + "'s Party"),
                    ChatColor.GRAY + "Members: " + party.members.size()));
        }
        player.openInventory(inventory);
    }

    private ItemStack menuItem(Material material, short data, String name, boolean enabled) {
        return simpleItem(material, data, name, enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled");
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
            return;
        }

        Player opponent = Bukkit.getPlayer(opponentId);
        if (opponent == null) {
            queue.add(player.getUniqueId());
            queuedKit.put(player.getUniqueId(), normalize(kit.name));
            queuedType.put(player.getUniqueId(), normalizedType);
            queuedAt.put(player.getUniqueId(), System.currentTimeMillis());
            return;
        }
        queuedKit.remove(opponentId);
        queuedType.remove(opponentId);
        queuedAt.remove(opponentId);
        startMatch(player, opponent, kit, normalizedType);
    }

    private void handleDuelCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(prefix + ChatColor.RED + "Use /duel <player> [kit] or /duel accept <player>.");
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
            startMatch(challenger, player, request.kit, "duel");
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
        KitData kit = args.length > 1 ? kits.get(normalize(args[1])) : firstKit();
        if (kit == null) {
            message(player, "no-kit");
            return;
        }
        duelRequests.put(target.getUniqueId(), new DuelRequest(player.getUniqueId(), kit, System.currentTimeMillis() + 60000L));
        player.sendMessage(format("duel-sent", "<player>", target.getName(), "<kit>", kit.name));
        target.sendMessage(format("duel-received", "<player>", player.getName(), "<kit>", kit.name));
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
            updateLobbyScoreboard(player);
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
            target.sendMessage(format("party-invite-received", "<player>", player.getName()));
            return;
        }
        if (sub.equals("accept")) {
            if (args.length < 2) {
                player.sendMessage(prefix + ChatColor.RED + "Use /party accept <player>.");
                return;
            }
            Player inviter = Bukkit.getPlayer(args[1]);
            PartyInvite invite = partyInvites.get(player.getUniqueId());
            if (inviter == null || invite == null || !invite.sender.equals(inviter.getUniqueId()) || invite.expired()) {
                player.sendMessage(prefix + ChatColor.RED + "That party invite is gone.");
                return;
            }
            Party party = parties.get(inviter.getUniqueId());
            if (party == null) {
                player.sendMessage(prefix + ChatColor.RED + "That party no longer exists.");
                return;
            }
            leaveParty(player, false);
            if (party.members.size() >= frostSettings.getInt("SETTINGS.GENERAL.MAXIMUM-PARTY-SIZE", 32)) {
                player.sendMessage(prefix + color(firstMessage("ERROR-MESSAGES.PLAYER.PARTY-LIMIT-REACHED", "&cParty size has reached its limit")));
                return;
            }
            party.members.add(player.getUniqueId());
            parties.put(player.getUniqueId(), party);
            partyInvites.remove(player.getUniqueId());
            broadcast(party, format("party-joined", "<player>", player.getName()));
            updatePartyScoreboards(party);
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
        player.sendMessage(prefix + ChatColor.RED + "Use /party create, invite, accept, leave, disband, info.");
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

    private void broadcast(Match match, String message) {
        Player first = Bukkit.getPlayer(match.first);
        Player second = Bukkit.getPlayer(match.second);
        if (first != null) {
            first.sendMessage(message);
        }
        if (second != null) {
            second.sendMessage(message);
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
        ArenaSpawns arena = acquireArena();
        if (arena == null) {
            first.sendMessage(format("no-arena"));
            second.sendMessage(format("no-arena"));
            return;
        }

        Match match = new Match(first.getUniqueId(), second.getUniqueId(), kit, arena, normalizeQueueType(type));
        match.bedRespawn = bedRespawnKits.contains(normalize(kit.name));
        matches.put(first.getUniqueId(), match);
        matches.put(second.getUniqueId(), match);

        preparePlayer(first, kit, arena.first);
        preparePlayer(second, kit, arena.second);
        updateMatchScoreboard(match);
        first.sendMessage(format("match-found", "<player>", first.getName(), "<opponent>", second.getName(), "<kit>", kit.name, "<queue>", displayQueueType(match.type)));
        second.sendMessage(format("match-found", "<player>", second.getName(), "<opponent>", first.getName(), "<kit>", kit.name, "<queue>", displayQueueType(match.type)));

        match.countdownTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            private int left = countdownSeconds;

            @Override
            public void run() {
                if (!matches.containsKey(first.getUniqueId()) || !matches.containsKey(second.getUniqueId())) {
                    match.cancel();
                    return;
                }
                if (left <= 0) {
                    match.started = true;
                    updateMatchScoreboard(match);
                    sendTitle(first, ChatColor.GREEN + "Fight!", "");
                    sendTitle(second, ChatColor.GREEN + "Fight!", "");
                    match.cancel();
                    return;
                }
                String title = ChatColor.GOLD + String.valueOf(left);
                sendTitle(first, title, "");
                sendTitle(second, title, "");
                left--;
            }
        }, 0L, 20L);
    }

    private ArenaSpawns acquireArena() {
        for (ArenaSpawns arena : arenas) {
            if (busyArenas.add(arena)) {
                return arena;
            }
        }
        return null;
    }

    private void preparePlayer(Player player, KitData kit, Location location) {
        leaveQueue(player, false);
        resetPlayer(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        pluginTeleport(player, location);
        giveKit(player, kit);
    }

    private void giveKit(Player player, KitData kit) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(null);
        inventory.setContents(cloneItems(kit.contents, 36));
        inventory.setArmorContents(cloneItems(kit.armor, 4));
        player.updateInventory();
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
    }

    private void setMatchScoreboard(Player player, Player opponent, Match match) {
        if (!scoreboardEnabled || !settings(player).scoreboard) {
            return;
        }
        List<String> lines = new ArrayList<>();
        for (String line : frostScoreboard.getStringList(settings(player).pingOnScoreboard ? "SCOREBOARD.IN-MATCH-PING" : "SCOREBOARD.IN-MATCH")) {
            expandScoreboardLine(lines, line, player, opponent, match);
        }
        setScoreboard(player, lines);
    }

    private void refreshScoreboards() {
        Set<Match> refreshed = new HashSet<>(matches.values());
        for (Match match : refreshed) {
            if (!match.ending) {
                updateMatchScoreboard(match);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!matches.containsKey(player.getUniqueId())) {
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
        if (!scoreboardEnabled || !settings(player).scoreboard) {
            return;
        }
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
                .replace("<event_joined>", String.valueOf(session.joined))
                .replace("<event_max>", String.valueOf(session.max))
                .replace("<event_countdown>", Math.max(0L, 30L - ((System.currentTimeMillis() - session.startedAt) / 1000L)) + "s")
                .replace("<alive_players>", String.valueOf(session.joined))
                .replace("<event_kit>", firstKit() == null ? "Default" : firstKit().name)
                .replace("<current_round>", "1")
                .replace("<playerA>", player.getName())
                .replace("<playerB>", "Waiting")
                .replace("<playerA_ping>", String.valueOf(getPing(player)))
                .replace("<playerB_ping>", "0")
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
        return alive ? ChatColor.GREEN + "Alive" : ChatColor.RED + "Gone";
    }

    private void updateLobbyScoreboard(Player player) {
        if (!scoreboardEnabled || !settings(player).scoreboard || matches.containsKey(player.getUniqueId())) {
            return;
        }
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
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("practice", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(scoreboardTitle);
        int score = lines.size();
        int blanks = 0;
        for (String line : lines) {
            String value = color(line);
            if (ChatColor.stripColor(value).trim().isEmpty()) {
                value = repeat(" ", ++blanks);
            }
            addScore(objective, value, score--);
        }
        player.setScoreboard(board);
    }

    private void expandScoreboardLine(List<String> lines, String line, Player player, Player opponent, Match match) {
        if (line.equals("<isBedWars>")) {
            if (match.bedRespawn) {
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
        return line
                .replace("%player_name%", player.getName())
                .replace("%luckperms_primary_group_name%", "Default")
                .replace("<online_players>", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("<fighting>", String.valueOf(matches.size() / 2))
                .replace("<queued_type>", displayQueueType(queuedType.getOrDefault(player.getUniqueId(), "unranked")))
                .replace("<queued_kit>", kit == null ? "" : kit.name)
                .replace("<queued_time>", queuedDuration(player.getUniqueId()))
                .replace("<party_leader>", party == null ? "" : playerName(party.leader))
                .replace("<party_members>", party == null ? "0" : String.valueOf(party.members.size()))
                .replace("<party_max>", String.valueOf(frostSettings.getInt("SETTINGS.GENERAL.PARTY-LIMIT-BY-DEFAULT", 12)))
                .replace("<ping>", String.valueOf(getPing(player)));
    }

    private String replaceMatchPlaceholders(String line, Player player, Player opponent, Match match) {
        boolean first = match.first.equals(player.getUniqueId());
        return line
                .replace("<opponent_name>", opponent == null ? "?" : opponent.getName())
                .replace("<match_duration>", match.duration())
                .replace("<kitName>", match.kit.name)
                .replace("<kit_name>", match.kit.name)
                .replace("<arenaName>", match.arena.name)
                .replace("<arena_name>", match.arena.name)
                .replace("<your_ping>", String.valueOf(getPing(player)))
                .replace("<opponent_ping>", opponent == null ? "0" : String.valueOf(getPing(opponent)))
                .replace("<rBed>", bedText(first ? match.firstBedAlive : match.secondBedAlive))
                .replace("<bBed>", bedText(first ? match.secondBedAlive : match.firstBedAlive))
                .replace("<rGoal>", "0")
                .replace("<bGoal>", "0")
                .replace("<goals>", "0")
                .replace("<kills>", "0")
                .replace("<hits>", "0")
                .replace("<your_hits>", "0")
                .replace("<opponent_hits>", "0")
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
    }

    private void addScore(Objective objective, String text, int score) {
        String value = text.length() > 40 ? text.substring(0, 40) : text;
        objective.getScore(value).setScore(score);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        Match match = matches.get(player.getUniqueId());
        if (match == null) {
            return;
        }
        if (!match.started || match.ending) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID || player.getHealth() - event.getFinalDamage() <= 0.0D) {
            event.setCancelled(true);
            handlePlayerDeath(player, match, player.getName() + " died");
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
        Bukkit.getScheduler().runTask(this, () -> handlePlayerDeath(event.getEntity(), match, event.getEntity().getName() + " died"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConsume(PlayerItemConsumeEvent event) {
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(PlayerMoveEvent event) {
        Match match = matches.get(event.getPlayer().getUniqueId());
        if (match == null) {
            if (spawnLocation != null && spawnMin != null && spawnMax != null && !activeEvents.containsKey(event.getPlayer().getUniqueId())
                    && event.getTo() != null && event.getTo().getWorld().equals(spawnLocation.getWorld())
                    && !insideCuboid(event.getTo(), spawnMin, spawnMax)) {
                pluginTeleport(event.getPlayer(), spawnLocation);
            }
            return;
        }
        if (event.getTo() != null && event.getTo().getY() <= voidY) {
            handlePlayerDeath(event.getPlayer(), match, event.getPlayer().getName() + " fell");
            return;
        }
        if (!match.started && event.getTo() != null && movedBlock(event.getFrom(), event.getTo())) {
            Location locked = match.spawnFor(event.getPlayer().getUniqueId()).clone();
            locked.setYaw(event.getTo().getYaw());
            locked.setPitch(event.getTo().getPitch());
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
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(this, () -> pluginTeleport(event.getPlayer(), match.spawnFor(event.getPlayer().getUniqueId())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (matches.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Match match = matches.get(event.getPlayer().getUniqueId());
        if (match == null || !match.bedRespawn || match.ending) {
            return;
        }
        Material type = event.getBlock().getType();
        if (type != Material.BED_BLOCK && type != Material.BED) {
            return;
        }
        UUID brokenOwner = match.ownerOfBed(event.getBlock().getLocation());
        if (brokenOwner == null || brokenOwner.equals(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(false);
        if (brokenOwner.equals(match.first)) {
            match.firstBedAlive = false;
        } else {
            match.secondBedAlive = false;
        }
        Player owner = Bukkit.getPlayer(brokenOwner);
        broadcast(match, format("bed-broken",
                "<player>", event.getPlayer().getName(),
                "<opponent>", owner == null ? "opponent" : owner.getName()));
        updateMatchScoreboard(match);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockedCommand(PlayerCommandPreprocessEvent event) {
        if (!matches.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        String lower = event.getMessage().toLowerCase(Locale.ROOT);
        if (lower.startsWith("/spawn") || lower.startsWith("/leave") || lower.startsWith("/hub") || lower.startsWith("/lobby")) {
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
    public void onQuit(PlayerQuitEvent event) {
        leaveQueue(event.getPlayer(), false);
        Match match = matches.get(event.getPlayer().getUniqueId());
        if (match != null) {
            finishMatch(match, match.other(event.getPlayer().getUniqueId()), event.getPlayer().getName() + " quit");
        }
        pluginTeleports.remove(event.getPlayer().getUniqueId());
        partyInvites.remove(event.getPlayer().getUniqueId());
        activeEvents.remove(event.getPlayer().getUniqueId());
        leaveParty(event.getPlayer(), false);
    }

    private void handlePlayerDeath(Player player, Match match, String reason) {
        UUID uuid = player.getUniqueId();
        if (match.bedRespawn && match.bedAlive(uuid)) {
            respawnInMatch(player, match);
            return;
        }
        finishMatch(match, match.other(uuid), reason);
    }

    private void respawnInMatch(Player player, Match match) {
        resetPlayer(player);
        pluginTeleport(player, match.spawnFor(player.getUniqueId()));
        giveKit(player, match.kit);
        updateMatchScoreboard(match);
        player.sendMessage(format("respawning", "<seconds>", "0"));
    }

    private void finishMatch(Match match, UUID winnerId, String reason) {
        if (match.ending) {
            return;
        }
        match.ending = true;
        match.cancel();
        matches.remove(match.first);
        matches.remove(match.second);
        busyArenas.remove(match.arena);

        Player first = Bukkit.getPlayer(match.first);
        Player second = Bukkit.getPlayer(match.second);
        Player winner = winnerId == null ? null : Bukkit.getPlayer(winnerId);
        Player loser = null;
        if (winnerId != null) {
            loser = winnerId.equals(match.first) ? second : first;
        }

        if (winner != null && loser != null) {
            winner.sendMessage(format("won", "<opponent>", loser.getName()));
            loser.sendMessage(format("lost", "<opponent>", winner.getName()));
        } else if (first != null) {
            first.sendMessage(prefix + ChatColor.YELLOW + reason);
        } else if (second != null) {
            second.sendMessage(prefix + ChatColor.YELLOW + reason);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (first != null && first.isOnline()) {
                returnToSpawn(first, false);
            }
            if (second != null && second.isOnline()) {
                returnToSpawn(second, false);
            }
        }, 20L);
    }

    private void returnToSpawn(Player player, boolean endMatch) {
        leaveQueue(player, true);
        if (endMatch) {
            Match match = matches.get(player.getUniqueId());
            if (match != null) {
                finishMatch(match, match.other(player.getUniqueId()), player.getName() + " left");
                return;
            }
        }
        resetPlayer(player);
        resetScoreboard(player);
        leaveEvent(player, false);
        if (spawnLocation != null) {
            pluginTeleport(player, spawnLocation);
        }
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
    }

    private void leaveEvent(Player player, boolean notify) {
        EventSession session = activeEvents.remove(player.getUniqueId());
        if (session != null && notify) {
            sendFrostLines(player, "MESSAGES.EVENT.LEFT", "<eventName>", displayEventName(session.name));
        }
    }

    private void resetPlayer(Player player) {
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setVelocity(new Vector(0, 0, 0));
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.updateInventory();
    }

    private void pluginTeleport(Player player, Location location) {
        if (location == null) {
            return;
        }
        pluginTeleports.add(player.getUniqueId());
        if (!player.teleport(location)) {
            pluginTeleports.remove(player.getUniqueId());
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
        return event.toUpperCase(Locale.ROOT).replace("_", "-");
    }

    private String displayEventName(String event) {
        return event.replace("-", " ");
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
        if (key.equals("queued")) return firstMessage("MESSAGES.QUEUE.JOINED-SOLO", "&7Queued for &b<queue> <kit>&7.");
        if (key.equals("unqueued")) return firstMessage("MESSAGES.QUEUE.LEFT", "&7You left the queue.");
        if (key.equals("match-found")) return "&a<queue> match found: &f<player> &7vs &f<opponent> &8(&b<kit>&8)";
        if (key.equals("won")) return "&aYou won against &f<opponent>&a.";
        if (key.equals("lost")) return "&cYou lost against &f<opponent>&c.";
        if (key.equals("no-kit")) return "&cUnknown kit.";
        if (key.equals("no-arena")) return "&cNo Frost arena spawns were loaded.";
        if (key.equals("already-fighting")) return "&cYou are already in a match.";
        if (key.equals("already-queued")) return "&cYou are already queued.";
        if (key.equals("not-fighting")) return "&cYou are not in a match.";
        if (key.equals("duel-sent")) return "&7Sent a duel request to &b<player>&7.";
        if (key.equals("duel-received")) return "&b<player> &7challenged you to &b<kit>&7. Use &f/duel accept <player>&7.";
        if (key.equals("duel-expired")) return "&cThat duel request is gone.";
        if (key.equals("party-created")) return firstMessage("MESSAGES.PARTY.CREATED", "&7Party created.");
        if (key.equals("party-invited")) return firstMessage("MESSAGES.PARTY.INVITED", "&7Invited &b<player> &7to your party.");
        if (key.equals("party-invite-received")) return "&b<player> &7invited you to a party. Use &f/party accept <player>&7.";
        if (key.equals("party-joined")) return firstMessage("MESSAGES.PARTY.JOINED", "&b<player> &7joined the party.");
        if (key.equals("party-left")) return firstMessage("MESSAGES.PARTY.LEFT", "&b<player> &7left the party.");
        if (key.equals("party-disbanded")) return firstMessage("MESSAGES.PARTY.DISBANDED", "&cThe party was disbanded.");
        if (key.equals("bed-broken")) return "&c<player> broke <opponent>'s bed!";
        if (key.equals("respawning")) return "&7Respawning in &b<seconds>&7...";
        return key;
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

        private KitData(String name, String display, ItemStack icon, ItemStack[] contents, ItemStack[] armor, int unrankedPos, int rankedPos, int editorPos, int spawnFfaPos) {
            this.name = name;
            this.display = display;
            this.icon = icon;
            this.contents = contents;
            this.armor = armor;
            this.unrankedPos = unrankedPos;
            this.rankedPos = rankedPos;
            this.editorPos = editorPos;
            this.spawnFfaPos = spawnFfaPos;
        }
    }

    private static final class ArenaSpawns {
        private final String name;
        private final Location first;
        private final Location second;

        private ArenaSpawns(String name, Location first, Location second) {
            this.name = name;
            this.first = first;
            this.second = second;
        }
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
        private boolean vanillaTab;
    }

    private static final class EventSession {
        private final String name;
        private final UUID host;
        private final long startedAt;
        private final int joined;
        private final int max;

        private EventSession(String name, UUID host, long startedAt, int joined, int max) {
            this.name = name;
            this.host = host;
            this.startedAt = startedAt;
            this.joined = joined;
            this.max = max;
        }
    }

    private static final class Match {
        private final UUID first;
        private final UUID second;
        private final KitData kit;
        private final ArenaSpawns arena;
        private final String type;
        private BukkitTask countdownTask;
        private boolean started;
        private boolean ending;
        private boolean bedRespawn;
        private boolean firstBedAlive = true;
        private boolean secondBedAlive = true;
        private final long startedAt = System.currentTimeMillis();

        private Match(UUID first, UUID second, KitData kit, ArenaSpawns arena, String type) {
            this.first = first;
            this.second = second;
            this.kit = kit;
            this.arena = arena;
            this.type = type;
        }

        private UUID other(UUID uuid) {
            return first.equals(uuid) ? second : first;
        }

        private Location spawnFor(UUID uuid) {
            return first.equals(uuid) ? arena.first : arena.second;
        }

        private boolean bedAlive(UUID uuid) {
            return first.equals(uuid) ? firstBedAlive : secondBedAlive;
        }

        private UUID ownerOfBed(Location location) {
            double firstDistance = arena.first.distanceSquared(location);
            double secondDistance = arena.second.distanceSquared(location);
            return firstDistance <= secondDistance ? first : second;
        }

        private void cancel() {
            if (countdownTask != null) {
                countdownTask.cancel();
                countdownTask = null;
            }
        }

        private String duration() {
            long seconds = Math.max(0L, (System.currentTimeMillis() - startedAt) / 1000L);
            return (seconds / 60L) + ":" + (seconds % 60L < 10 ? "0" : "") + (seconds % 60L);
        }
    }

    private static final class DuelRequest {
        private final UUID sender;
        private final KitData kit;
        private final long expiresAt;

        private DuelRequest(UUID sender, KitData kit, long expiresAt) {
            this.sender = sender;
            this.kit = kit;
            this.expiresAt = expiresAt;
        }

        private boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private static final class Party {
        private final UUID leader;
        private final Set<UUID> members = new HashSet<>();

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
}
