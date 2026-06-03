package dev.forgified.frostduelrespawn;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FrostDuelRespawnPatchPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, RespawnSession> sessions = new HashMap<>();
    private final Map<UUID, Long> lastFilteredMessage = new HashMap<>();
    private final Map<UUID, TeleportGuard> releasedTeleportGuards = new HashMap<>();
    private final Map<UUID, Location> rememberedArenaSpawns = new HashMap<>();
    private final Set<UUID> pluginTeleports = new HashSet<>();
    private final Set<String> targetKits = new HashSet<>();
    private final List<ArenaSpawnPair> arenaSpawnPairs = new ArrayList<>();
    private FileConfiguration frostKits;
    private FileConfiguration frostArenas;

    private int respawnDelaySeconds;
    private int forceRespawnAfterTicks;
    private int lockAfterDamageTicks;
    private int teleportDetectionWindowMillis;
    private int voidY;
    private int antiTeleportAfterReleaseTicks;
    private double antiTeleportMaxDistance;
    private double arenaSpawnRememberDistanceSquared;
    private int chatFilterCooldownMillis;
    private boolean chatFilterEnabled;
    private boolean pseudoDeathEnabled;
    private boolean applyToAnyFrostFightIfKitUnknown;
    private boolean applyIfFrostStateUnknown;
    private boolean antiFrostTeleportEnabled;
    private boolean blockAllTeleportsDuringRespawn;
    private boolean blockFrostPluginTeleportsAlways;
    private boolean debug;
    private String countdownTitle;
    private String countdownSubtitle;
    private String releasedTitle;
    private String releasedSubtitle;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        frostKits = YamlConfiguration.loadConfiguration(new File(getDataFolder().getParentFile(), "Frost/kits.yml"));
        frostArenas = YamlConfiguration.loadConfiguration(new File(getDataFolder().getParentFile(), "Frost/arenas.yml"));
        loadArenaSpawns();
        Bukkit.getPluginManager().registerEvents(this, this);
        hookProtocolLib();
    }

    @Override
    public void onDisable() {
        for (RespawnSession session : sessions.values()) {
            session.cancel();
        }
        sessions.clear();
    }

    private void loadSettings() {
        targetKits.clear();
        for (String kit : getConfig().getStringList("target-kits")) {
            targetKits.add(normalize(kit));
        }

        respawnDelaySeconds = Math.max(1, getConfig().getInt("respawn-delay-seconds", 3));
        forceRespawnAfterTicks = Math.max(1, getConfig().getInt("force-respawn-after-ticks", 2));
        pseudoDeathEnabled = getConfig().getBoolean("pseudo-death.enabled", true);
        applyToAnyFrostFightIfKitUnknown = getConfig().getBoolean("pseudo-death.apply-to-any-frost-fight-if-kit-unknown", true);
        applyIfFrostStateUnknown = getConfig().getBoolean("pseudo-death.apply-if-frost-state-is-unknown", true);
        lockAfterDamageTicks = Math.max(1, getConfig().getInt("pseudo-death.lock-after-damage-ticks", 6));
        teleportDetectionWindowMillis = Math.max(500, getConfig().getInt("pseudo-death.teleport-detection-window-millis", 4000));
        voidY = getConfig().getInt("pseudo-death.void-y", 0);
        antiFrostTeleportEnabled = getConfig().getBoolean("anti-frost-teleport.enabled", true);
        blockAllTeleportsDuringRespawn = getConfig().getBoolean("anti-frost-teleport.block-all-during-respawn", true);
        blockFrostPluginTeleportsAlways = getConfig().getBoolean("anti-frost-teleport.block-plugin-teleports-always", true);
        antiTeleportAfterReleaseTicks = Math.max(0, getConfig().getInt("anti-frost-teleport.after-release-ticks", 40));
        antiTeleportMaxDistance = Math.max(0.25D, getConfig().getDouble("anti-frost-teleport.max-distance-from-respawn", 1.5D));
        double arenaSpawnRememberDistance = Math.max(2.0D, getConfig().getDouble("arena-spawns.remember-distance", 14.0D));
        arenaSpawnRememberDistanceSquared = arenaSpawnRememberDistance * arenaSpawnRememberDistance;
        chatFilterEnabled = getConfig().getBoolean("chat-filter.enabled", true);
        chatFilterCooldownMillis = Math.max(500, getConfig().getInt("chat-filter.cooldown-millis", 2500));
        countdownTitle = color(getConfig().getString("messages.countdown-title", "&bRespawning"));
        countdownSubtitle = color(getConfig().getString("messages.countdown-subtitle", "&7You can move in &b<seconds>&7..."));
        releasedTitle = color(getConfig().getString("messages.released-title", "&aGo!"));
        releasedSubtitle = color(getConfig().getString("messages.released-subtitle", "&7You have respawned."));
        debug = getConfig().getBoolean("debug", false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathStart(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        Player player = event.getEntity();
        if (!isTargetFrostDuel(player)) {
            return;
        }

        RespawnSession session = getOrCreateSession(player);
        session.waitingForRespawn = true;
        session.startedAt = System.currentTimeMillis();
        schedulePluginRespawn(player, session, forceRespawnAfterTicks + 1L);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            RespawnSession current = sessions.get(player.getUniqueId());
            if (current != session || !player.isOnline() || !player.isDead() || current.locking) {
                return;
            }
            player.spigot().respawn();
        }, forceRespawnAfterTicks);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPossiblePseudoDeath(EntityDamageEvent event) {
        if (!pseudoDeathEnabled || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (!looksLikePseudoDeath(player, event) || !shouldPatchPlayer(player)) {
            return;
        }

        beginPluginRespawn(player, "damage:" + event.getCause());

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawnLocation(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        RespawnSession session = sessions.get(player.getUniqueId());
        if (session == null && !isTargetFrostDuel(player)) {
            return;
        }
        if (session == null) {
            session = getOrCreateSession(player);
        }

        Location safe = findSafeLocation(event.getRespawnLocation());
        session.location = safe;
        session.waitingForRespawn = false;
        event.setRespawnLocation(safe);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void afterRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        RespawnSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        session.cancel();
        Bukkit.getScheduler().runTask(this, () -> {
            RespawnSession current = sessions.get(player.getUniqueId());
            if (current != session || !player.isOnline()) {
                return;
            }
            stabilize(player, session.location);
            startCountdown(player, session);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageWhileLocked(EntityDamageEvent event) {
        RespawnSession session = event.getEntity() instanceof Player ? sessions.get(event.getEntity().getUniqueId()) : null;
        if (session != null && session.locking) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMoveWhileLocked(PlayerMoveEvent event) {
        rememberArenaSpawn(event.getPlayer(), event.getTo());
        RespawnSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null && pseudoDeathEnabled && event.getTo() != null && event.getTo().getY() <= voidY && shouldPatchPlayer(event.getPlayer())) {
            beginPluginRespawn(event.getPlayer(), "move-void");
            event.setTo(resolveRespawnAnchor(event.getPlayer()));
            return;
        }
        if (session == null || !session.locking || session.location == null || event.getTo() == null || samePosition(event.getFrom(), event.getTo())) {
            return;
        }

        Location to = session.location.clone();
        to.setYaw(event.getTo().getYaw());
        to.setPitch(event.getTo().getPitch());
        event.setTo(to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFrostTeleportAfterPseudoDeath(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        RespawnSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.waitingForRespawn || session.locking || event.getTo() == null) {
            return;
        }
        if (System.currentTimeMillis() - session.startedAt > teleportDetectionWindowMillis) {
            return;
        }

        Bukkit.getScheduler().runTask(this, () -> {
            RespawnSession current = sessions.get(player.getUniqueId());
            if (current != session || !player.isOnline() || player.isDead() || current.locking) {
                return;
            }
            current.location = resolveRespawnAnchor(player);
            stabilize(player, current.location);
            restoreKit(player, current);
            startCountdown(player, current);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        RespawnSession session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.cancel();
        }
        lastFilteredMessage.remove(event.getPlayer().getUniqueId());
        releasedTeleportGuards.remove(event.getPlayer().getUniqueId());
        rememberedArenaSpawns.remove(event.getPlayer().getUniqueId());
        pluginTeleports.remove(event.getPlayer().getUniqueId());
    }

    private RespawnSession getOrCreateSession(Player player) {
        UUID uuid = player.getUniqueId();
        RespawnSession old = sessions.remove(uuid);
        if (old != null) {
            old.cancel();
        }
        RespawnSession session = new RespawnSession();
        session.location = resolveRespawnAnchor(player);
        session.kitName = resolveFrostKitName(player);
        session.contents = cloneContents(player.getInventory().getContents());
        session.armor = cloneContents(player.getInventory().getArmorContents());
        sessions.put(uuid, session);
        return session;
    }

    private RespawnSession beginPluginRespawn(Player player, String reason) {
        RespawnSession existing = sessions.get(player.getUniqueId());
        if (existing != null && existing.locking) {
            return existing;
        }

        RespawnSession session = getOrCreateSession(player);
        session.waitingForRespawn = true;
        session.startedAt = System.currentTimeMillis();
        debug(player.getName() + " begin respawn patch: " + reason + " -> " + formatLocation(session.location));
        schedulePluginRespawn(player, session, lockAfterDamageTicks);
        return session;
    }

    private void schedulePluginRespawn(Player player, RespawnSession session, long delay) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            RespawnSession current = sessions.get(player.getUniqueId());
            if (current != session || !player.isOnline() || player.isDead() || current.locking) {
                return;
            }
            current.location = resolveRespawnAnchor(player);
            debug(player.getName() + " plugin teleport -> " + formatLocation(current.location));
            pluginTeleport(player, current.location);
            restoreKit(player, current);
            Bukkit.getScheduler().runTaskLater(this, () -> restoreKit(player, current), 5L);
            Bukkit.getScheduler().runTaskLater(this, () -> restoreKit(player, current), 20L);
            startCountdown(player, current);
        }, delay);
    }

    private void startCountdown(Player player, RespawnSession session) {
        session.waitingForRespawn = false;
        session.locking = true;
        Bukkit.getScheduler().runTaskLater(this, () -> restoreKit(player, session), 2L);
        session.task = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            private int secondsLeft = respawnDelaySeconds;

            @Override
            public void run() {
                RespawnSession current = sessions.get(player.getUniqueId());
                if (current != session || !player.isOnline()) {
                    session.cancel();
                    return;
                }

                if (secondsLeft <= 0) {
                    sessions.remove(player.getUniqueId());
                    session.locking = false;
                    session.cancel();
                    clearBadMotion(player);
                    startReleasedTeleportGuard(player, session.location);
                    sendTitle(player, releasedTitle, releasedSubtitle);
                    return;
                }

                clearBadMotion(player);
                sendTitle(player, countdownTitle, countdownSubtitle.replace("<seconds>", String.valueOf(secondsLeft)));
                secondsLeft--;
            }
        }, 0L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void blockFrostTeleportBack(PlayerTeleportEvent event) {
        if (!antiFrostTeleportEnabled || event.getTo() == null) {
            return;
        }

        Player player = event.getPlayer();
        if (pluginTeleports.remove(player.getUniqueId())) {
            return;
        }

        RespawnSession session = sessions.get(player.getUniqueId());
        if (isBlockedFrostTeleport(event, player, session)) {
            debug(player.getName() + " cancelled Frost teleport: " + event.getCause() + " -> " + formatLocation(event.getTo()));
            event.setCancelled(true);
            clearBadMotion(player);
            snapBackAfterBlockedTeleport(player, session);
            return;
        }
        if (session != null && blockAllTeleportsDuringRespawn) {
            debug(player.getName() + " cancelled teleport during respawn: " + event.getCause() + " -> " + formatLocation(event.getTo()));
            event.setCancelled(true);
            clearBadMotion(player);
            snapBackAfterBlockedTeleport(player, session);
            return;
        }
        if (session != null && session.locking && session.location != null && awayFromAnchor(session.location, event.getTo())) {
            event.setCancelled(true);
            clearBadMotion(player);
            snapBackAfterBlockedTeleport(player, session);
            return;
        }

        TeleportGuard guard = releasedTeleportGuards.get(player.getUniqueId());
        if (guard == null) {
            return;
        }
        if (System.currentTimeMillis() > guard.expiresAt) {
            releasedTeleportGuards.remove(player.getUniqueId());
            return;
        }
        if (awayFromAnchor(guard.location, event.getTo())) {
            event.setCancelled(true);
            clearBadMotion(player);
            snapBackAfterBlockedTeleport(player, session);
        }
    }

    private boolean isBlockedFrostTeleport(PlayerTeleportEvent event, Player player, RespawnSession session) {
        if (session != null || releasedTeleportGuards.containsKey(player.getUniqueId())) {
            return true;
        }
        if (!blockFrostPluginTeleportsAlways || !isPatchedFrostGamemode(player)) {
            return false;
        }
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || isAwayFromRememberedArena(player, event.getTo())) {
            return true;
        }
        return false;
    }

    private void snapBackAfterBlockedTeleport(Player player, RespawnSession session) {
        Location anchor = session != null ? session.location : rememberedArenaSpawns.get(player.getUniqueId());
        if (anchor == null) {
            return;
        }
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                pluginTeleport(player, anchor);
            }
        });
    }

    private boolean isAwayFromRememberedArena(Player player, Location target) {
        Location remembered = rememberedArenaSpawns.get(player.getUniqueId());
        return remembered != null && target != null && awayFromAnchor(remembered, target);
    }

    private void startReleasedTeleportGuard(Player player, Location location) {
        if (!antiFrostTeleportEnabled || antiTeleportAfterReleaseTicks <= 0 || location == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long expiresAt = System.currentTimeMillis() + (antiTeleportAfterReleaseTicks * 50L);
        releasedTeleportGuards.put(uuid, new TeleportGuard(location.clone(), expiresAt));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            TeleportGuard guard = releasedTeleportGuards.get(uuid);
            if (guard != null && System.currentTimeMillis() >= guard.expiresAt) {
                releasedTeleportGuards.remove(uuid);
            }
        }, antiTeleportAfterReleaseTicks + 1L);
    }

    private boolean awayFromAnchor(Location anchor, Location target) {
        if (anchor.getWorld() != target.getWorld()) {
            return true;
        }
        return anchor.distanceSquared(target) > antiTeleportMaxDistance * antiTeleportMaxDistance;
    }

    private void pluginTeleport(Player player, Location location) {
        if (location == null) {
            return;
        }
        pluginTeleports.add(player.getUniqueId());
        boolean teleported = player.teleport(location);
        if (!teleported) {
            pluginTeleports.remove(player.getUniqueId());
        }
        clearBadMotion(player);
    }

    private void debug(String message) {
        if (debug) {
            getLogger().info(message);
        }
    }

    private static String formatLocation(Location location) {
        if (location == null) {
            return "null";
        }
        return location.getWorld().getName() + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private void hookProtocolLib() {
        if (!chatFilterEnabled || Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            return;
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, ListenerPriority.HIGHEST,
                PacketType.Play.Server.CHAT, PacketType.Play.Server.SYSTEM_CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null || !sessions.containsKey(player.getUniqueId())) {
                    return;
                }

                String message = readChatMessage(event);
                if (!looksLikeRespawnMessage(message)) {
                    return;
                }

                long now = System.currentTimeMillis();
                long last = lastFilteredMessage.getOrDefault(player.getUniqueId(), 0L);
                if (now - last < chatFilterCooldownMillis) {
                    event.setCancelled(true);
                }
                lastFilteredMessage.put(player.getUniqueId(), now);
            }
        });
    }

    private static String readChatMessage(PacketEvent event) {
        try {
            WrappedChatComponent component = event.getPacket().getChatComponents().readSafely(0);
            return component == null ? "" : component.getJson();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean looksLikeRespawnMessage(String message) {
        String plain = normalize(message);
        return plain.contains("respawn") || plain.contains("died") || plain.contains("death");
    }

    private boolean isTargetFrostDuel(Player player) {
        Object frost = callStatic("dev.demeng.frost.Frost", "k");
        Object service = call(frost, "P");
        Object matchManager = call(service, "P");
        Object match = findMatchForPlayer(matchManager, player.getUniqueId());
        if (match == null) {
            return scoreboardContainsTargetKit(player) || (applyToAnyFrostFightIfKitUnknown && isFrostFighting(player));
        }

        Object kit = call(match, "b");
        return (kit != null && hasTargetKitName(kit)) || (kit == null && applyToAnyFrostFightIfKitUnknown && isFrostFighting(player));
    }

    private boolean isPatchedFrostGamemode(Player player) {
        return isTargetFrostDuel(player) || (applyToAnyFrostFightIfKitUnknown && isFrostFighting(player));
    }

    private String resolveFrostKitName(Player player) {
        Object frost = callStatic("dev.demeng.frost.Frost", "k");
        Object service = call(frost, "P");
        Object matchManager = call(service, "P");
        Object match = findMatchForPlayer(matchManager, player.getUniqueId());
        Object kit = call(match, "b");
        if (kit == null) {
            return null;
        }

        for (Method method : kit.getClass().getMethods()) {
            if (method.getParameterTypes().length != 0 || method.getReturnType() != String.class) {
                continue;
            }
            try {
                Object value = method.invoke(kit);
                if (value != null && frostKits.contains("kits." + value)) {
                    return String.valueOf(value);
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                // Keep trying public string getters.
            }
        }
        return resolveKitNameFromScoreboard(player);
    }

    private String resolveKitNameFromScoreboard(Player player) {
        if (frostKits == null || frostKits.getConfigurationSection("kits") == null) {
            return null;
        }

        StringBuilder scoreboardText = new StringBuilder();
        try {
            for (String entry : player.getScoreboard().getEntries()) {
                scoreboardText.append(' ').append(normalize(entry));
            }
        } catch (RuntimeException ignored) {
            return null;
        }

        String text = scoreboardText.toString();
        for (String kitName : frostKits.getConfigurationSection("kits").getKeys(false)) {
            String displayName = frostKits.getString("kits." + kitName + ".displayName", kitName);
            if (text.contains(normalize(kitName)) || text.contains(normalize(displayName))) {
                return kitName;
            }
        }
        return null;
    }

    private Location resolveRespawnAnchor(Player player) {
        Object frost = callStatic("dev.demeng.frost.Frost", "k");
        Object service = call(frost, "P");
        Object matchManager = call(service, "P");
        Object match = findMatchForPlayer(matchManager, player.getUniqueId());
        Location teamSpawn = findTeamSpawn(match, player.getUniqueId());
        if (teamSpawn != null) {
            Location safe = findSafeLocation(teamSpawn);
            rememberedArenaSpawns.put(player.getUniqueId(), safe.clone());
            return safe;
        }

        Location remembered = rememberedArenaSpawns.get(player.getUniqueId());
        if (remembered != null) {
            return findSafeLocation(remembered);
        }

        Location arenaSpawn = findNearestArenaSpawn(player.getLocation());
        if (arenaSpawn != null) {
            Location safe = findSafeLocation(arenaSpawn);
            rememberedArenaSpawns.put(player.getUniqueId(), safe.clone());
            return safe;
        }
        return findSafeLocation(player.getLocation());
    }

    private Location findTeamSpawn(Object match, UUID uuid) {
        if (match == null) {
            return null;
        }

        Object teams = call(match, "i");
        if (!(teams instanceof Collection)) {
            return null;
        }

        for (Object team : (Collection<?>) teams) {
            if (!teamContainsPlayer(team, uuid)) {
                continue;
            }
            Location location = getLocationFromZeroArgMethod(team, "t");
            if (location != null) {
                return location;
            }
            for (Method method : team.getClass().getMethods()) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == Location.class) {
                    try {
                        return ((Location) method.invoke(team)).clone();
                    } catch (IllegalAccessException | InvocationTargetException ignored) {
                        // Keep trying public location getters.
                    }
                }
            }
        }
        return null;
    }

    private boolean teamContainsPlayer(Object team, UUID uuid) {
        if (team == null) {
            return false;
        }
        Object players = call(team, "e");
        if (players instanceof Collection && ((Collection<?>) players).contains(uuid)) {
            return true;
        }
        return matchContainsPlayer(team, uuid);
    }

    private static Location getLocationFromZeroArgMethod(Object target, String methodName) {
        Object value = call(target, methodName);
        return value instanceof Location ? ((Location) value).clone() : null;
    }

    private void loadArenaSpawns() {
        arenaSpawnPairs.clear();
        if (frostArenas == null || frostArenas.getConfigurationSection("arenas") == null) {
            return;
        }

        for (String arena : frostArenas.getConfigurationSection("arenas").getKeys(false)) {
            String base = "arenas." + arena;
            addArenaSpawnPair(base);
            if (frostArenas.getConfigurationSection(base + ".standaloneArenas") == null) {
                continue;
            }
            for (String standalone : frostArenas.getConfigurationSection(base + ".standaloneArenas").getKeys(false)) {
                addArenaSpawnPair(base + ".standaloneArenas." + standalone);
            }
        }
        debug("Loaded " + arenaSpawnPairs.size() + " Frost arena spawn pairs from arenas.yml");
    }

    private void addArenaSpawnPair(String path) {
        String a = frostArenas.getString(path + ".a");
        String b = frostArenas.getString(path + ".b");
        if (a == null || b == null) {
            return;
        }
        ArenaPoint first = parseArenaPoint(a);
        ArenaPoint second = parseArenaPoint(b);
        if (first != null && second != null) {
            arenaSpawnPairs.add(new ArenaSpawnPair(first, second));
        }
    }

    private void rememberArenaSpawn(Player player, Location location) {
        if (location == null || arenaSpawnPairs.isEmpty()) {
            return;
        }
        ArenaPoint nearest = findNearestArenaPoint(location);
        if (nearest == null || nearest.distanceSquared(location) > arenaSpawnRememberDistanceSquared) {
            return;
        }
        Location remembered = nearest.toLocation(location.getWorld());
        rememberedArenaSpawns.put(player.getUniqueId(), findSafeLocation(remembered));
    }

    private Location findNearestArenaSpawn(Location location) {
        ArenaPoint nearest = findNearestArenaPoint(location);
        return nearest == null ? null : nearest.toLocation(location.getWorld());
    }

    private ArenaPoint findNearestArenaPoint(Location location) {
        ArenaPoint nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ArenaSpawnPair pair : arenaSpawnPairs) {
            double firstDistance = pair.first.distanceSquared(location);
            if (firstDistance < nearestDistance) {
                nearestDistance = firstDistance;
                nearest = pair.first;
            }
            double secondDistance = pair.second.distanceSquared(location);
            if (secondDistance < nearestDistance) {
                nearestDistance = secondDistance;
                nearest = pair.second;
            }
        }
        return nearest;
    }

    private static ArenaPoint parseArenaPoint(String value) {
        String[] parts = value.split(",");
        if (parts.length < 3) {
            return null;
        }
        try {
            double x = Double.parseDouble(parts[0].trim());
            double y = Double.parseDouble(parts[1].trim());
            double z = Double.parseDouble(parts[2].trim());
            float yaw = parts.length > 3 ? Float.parseFloat(parts[3].trim()) : 0.0F;
            float pitch = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0.0F;
            return new ArenaPoint(x, y, z, yaw, pitch);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean shouldPatchPlayer(Player player) {
        if (isTargetFrostDuel(player) || (applyToAnyFrostFightIfKitUnknown && isFrostFighting(player))) {
            return true;
        }
        return applyIfFrostStateUnknown;
    }

    private boolean looksLikePseudoDeath(Player player, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            return true;
        }
        return player.getHealth() - event.getFinalDamage() <= 0.0D;
    }

    private boolean isFrostFighting(Player player) {
        Object frost = callStatic("dev.demeng.frost.Frost", "k");
        Object service = call(frost, "P");
        Object profileManager = call(service, "x");
        Object profile = call(profileManager, "I", player.getUniqueId());
        Object state = call(profile, "u");
        return state != null && "FIGHTING".equalsIgnoreCase(String.valueOf(state));
    }

    private Object findMatchForPlayer(Object matchManager, UUID uuid) {
        Object direct = firstNonNull(
                call(matchManager, "y", uuid),
                call(matchManager, "b", uuid),
                call(matchManager, "W", uuid)
        );
        if (direct != null) {
            return direct;
        }

        Object map = call(matchManager, "B");
        if (!(map instanceof Map)) {
            return null;
        }

        for (Object match : ((Map<?, ?>) map).values()) {
            if (matchContainsPlayer(match, uuid)) {
                return match;
            }
        }
        return null;
    }

    private boolean matchContainsPlayer(Object match, UUID uuid) {
        if (match == null) {
            return false;
        }
        for (Method method : match.getClass().getMethods()) {
            if (method.getParameterTypes().length != 0 || !Set.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            try {
                Object result = method.invoke(match);
                if (result instanceof Set && ((Set<?>) result).contains(uuid)) {
                    return true;
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                // Frost is obfuscated; keep scanning methods that are safe to call.
            }
        }
        return false;
    }

    private boolean hasTargetKitName(Object kit) {
        for (Method method : kit.getClass().getMethods()) {
            if (method.getParameterTypes().length != 0 || method.getReturnType() != String.class) {
                continue;
            }
            try {
                Object value = method.invoke(kit);
                if (value != null && targetKits.contains(normalize(String.valueOf(value)))) {
                    return true;
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                // Keep trying the rest of the public string getters.
            }
        }
        return false;
    }

    private boolean scoreboardContainsTargetKit(Player player) {
        try {
            Collection<String> entries = player.getScoreboard().getEntries();
            for (String entry : entries) {
                String normalized = normalize(entry);
                for (String kit : targetKits) {
                    if (normalized.contains(kit)) {
                        return true;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            return false;
        }
        return false;
    }

    private void stabilize(Player player, Location location) {
        if (location != null) {
            pluginTeleport(player, location);
        }
        clearBadMotion(player);
    }

    private static void clearBadMotion(Player player) {
        player.setFallDistance(0.0F);
        player.setFireTicks(0);
        player.setVelocity(new Vector(0, 0, 0));
        player.setHealth(Math.max(1.0D, player.getHealth()));
    }

    private void restoreKit(Player player, RespawnSession session) {
        RespawnSession current = sessions.get(player.getUniqueId());
        if (current != session || !player.isOnline()) {
            return;
        }

        ItemStack[] contents = readKitItems(session.kitName, "contents", 36);
        ItemStack[] armor = readKitItems(session.kitName, "armor", 4);
        if (contents == null) {
            contents = cloneContents(session.contents);
        }
        if (armor == null) {
            armor = cloneContents(session.armor);
        }

        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armor);
        player.updateInventory();
    }

    private ItemStack[] readKitItems(String kitName, String path, int size) {
        if (kitName == null || frostKits == null || !frostKits.contains("kits." + kitName + "." + path)) {
            return null;
        }

        java.util.List<?> list = frostKits.getList("kits." + kitName + "." + path);
        if (list == null) {
            return null;
        }

        ItemStack[] items = new ItemStack[size];
        for (int i = 0; i < Math.min(size, list.size()); i++) {
            Object item = list.get(i);
            if (item instanceof ItemStack) {
                items[i] = ((ItemStack) item).clone();
            }
        }
        return items;
    }

    private static ItemStack[] cloneContents(ItemStack[] source) {
        if (source == null) {
            return null;
        }
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            clone[i] = source[i] == null ? null : source[i].clone();
        }
        return clone;
    }

    private static Location findSafeLocation(Location location) {
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

    private static boolean isClear(Location location) {
        Material type = location.getBlock().getType();
        return type == Material.AIR || !type.isSolid();
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Object callStatic(String className, String methodName) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod(methodName);
            return method.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object call(Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!method.getName().equals(methodName) || parameterTypes.length != args.length || !parametersFit(parameterTypes, args)) {
                continue;
            }
            try {
                return method.invoke(target, args);
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean parametersFit(Class<?>[] parameterTypes, Object[] args) {
        for (int i = 0; i < parameterTypes.length; i++) {
            if (args[i] != null && !wrap(parameterTypes[i]).isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        return Void.class;
    }

    private static boolean samePosition(Location from, Location to) {
        return from.getWorld() == to.getWorld()
                && from.getX() == to.getX()
                && from.getY() == to.getY()
                && from.getZ() == to.getZ();
    }

    private static String normalize(String value) {
        return ChatColor.stripColor(color(value))
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .replace("\"", "")
                .replace("\\", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    @SuppressWarnings("deprecation")
    private static void sendTitle(Player player, String main, String sub) {
        player.sendTitle(main, sub);
    }

    private static final class RespawnSession {
        private BukkitTask task;
        private Location location;
        private String kitName;
        private ItemStack[] contents;
        private ItemStack[] armor;
        private long startedAt;
        private boolean waitingForRespawn;
        private boolean locking;

        private void cancel() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
    }

    private static final class TeleportGuard {
        private final Location location;
        private final long expiresAt;

        private TeleportGuard(Location location, long expiresAt) {
            this.location = location;
            this.expiresAt = expiresAt;
        }
    }

    private static final class ArenaSpawnPair {
        private final ArenaPoint first;
        private final ArenaPoint second;

        private ArenaSpawnPair(ArenaPoint first, ArenaPoint second) {
            this.first = first;
            this.second = second;
        }
    }

    private static final class ArenaPoint {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        private ArenaPoint(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        private double distanceSquared(Location location) {
            double dx = x - location.getX();
            double dy = y - location.getY();
            double dz = z - location.getZ();
            return dx * dx + dy * dy + dz * dz;
        }

        private Location toLocation(org.bukkit.World world) {
            return new Location(world, x, y, z, yaw, pitch);
        }
    }
}
