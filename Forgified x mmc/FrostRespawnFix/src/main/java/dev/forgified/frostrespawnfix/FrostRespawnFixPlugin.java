package dev.forgified.frostrespawnfix;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FrostRespawnFixPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, RespawnLock> locks = new HashMap<>();
    private final Set<String> targetKits = new HashSet<>();

    private int delaySeconds;
    private String title;
    private String subtitle;
    private String doneTitle;
    private String doneSubtitle;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (RespawnLock lock : locks.values()) {
            lock.cancel();
        }
        locks.clear();
    }

    private void reloadSettings() {
        targetKits.clear();
        for (String kit : getConfig().getStringList("target-kits")) {
            targetKits.add(normalize(kit));
        }

        delaySeconds = Math.max(1, getConfig().getInt("respawn-delay-seconds", 3));
        title = color(getConfig().getString("messages.title", "&bRespawning"));
        subtitle = color(getConfig().getString("messages.subtitle", "&7You can move in &b<seconds>&7..."));
        doneTitle = color(getConfig().getString("messages.done-title", "&aGo!"));
        doneSubtitle = color(getConfig().getString("messages.done-subtitle", "&7You have respawned."));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!isTargetFrostKit(player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        RespawnLock oldLock = locks.remove(uuid);
        if (oldLock != null) {
            oldLock.cancel();
        }

        Location lockLocation = findSafeLocation(event.getRespawnLocation());
        RespawnLock lock = new RespawnLock(lockLocation);
        locks.put(uuid, lock);

        Bukkit.getScheduler().runTask(this, () -> {
            if (!player.isOnline() || !locks.containsKey(uuid)) {
                return;
            }
            stabilize(player, lockLocation);
            startTimer(player, lock);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        RespawnLock lock = locks.get(event.getPlayer().getUniqueId());
        if (lock == null || event.getTo() == null || samePosition(event.getFrom(), event.getTo())) {
            return;
        }

        Location to = lock.location.clone();
        to.setYaw(event.getTo().getYaw());
        to.setPitch(event.getTo().getPitch());
        event.setTo(to);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && locks.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        RespawnLock lock = locks.remove(event.getPlayer().getUniqueId());
        if (lock != null) {
            lock.cancel();
        }
    }

    private void startTimer(Player player, RespawnLock lock) {
        lock.task = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            private int secondsLeft = delaySeconds;

            @Override
            public void run() {
                UUID uuid = player.getUniqueId();
                if (!player.isOnline() || locks.get(uuid) != lock) {
                    lock.cancel();
                    return;
                }

                if (secondsLeft <= 0) {
                    locks.remove(uuid);
                    lock.cancel();
                    stabilize(player, lock.location);
                    sendTitle(player, doneTitle, doneSubtitle);
                    return;
                }

                stabilize(player, lock.location);
                sendTitle(player, title, subtitle.replace("<seconds>", String.valueOf(secondsLeft)));
                secondsLeft--;
            }
        }, 0L, 20L);
    }

    private void stabilize(Player player, Location location) {
        player.teleport(location);
        player.setFallDistance(0.0F);
        player.setFireTicks(0);
        player.setVelocity(new Vector(0, 0, 0));
    }

    private static Location findSafeLocation(Location location) {
        Location safe = location.clone();
        safe.setX(safe.getBlockX() + 0.5D);
        safe.setZ(safe.getBlockZ() + 0.5D);

        for (int i = 0; i <= 6; i++) {
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

    private boolean isTargetFrostKit(Player player) {
        Object frost = callStatic("dev.demeng.frost.Frost", "k");
        Object service = call(frost, "P");
        Object matchManager = call(service, "P");
        Object match = call(matchManager, "y", player.getUniqueId());
        if (match == null) {
            match = call(matchManager, "b", player.getUniqueId());
        }
        if (match == null) {
            match = call(matchManager, "W", player.getUniqueId());
        }

        Object kit = call(match, "b");
        return kit != null && hasTargetKitName(kit);
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
            } catch (ReflectiveOperationException ignored) {
                // Frost is obfuscated; try every public string getter and ignore the ones that fail.
            }
        }
        return false;
    }

    private static Object callStatic(String className, String methodName) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod(methodName);
            return method.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object call(Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }

        for (Method method : target.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!method.getName().equals(methodName) || parameterTypes.length != args.length) {
                continue;
            }
            if (!parametersFit(parameterTypes, args)) {
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
            if (args[i] == null) {
                continue;
            }
            Class<?> parameterType = wrap(parameterTypes[i]);
            if (!parameterType.isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
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
                .toLowerCase(Locale.ROOT);
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    @SuppressWarnings("deprecation")
    private static void sendTitle(Player player, String main, String sub) {
        player.sendTitle(main, sub);
    }

    private static final class RespawnLock {
        private final Location location;
        private BukkitTask task;

        private RespawnLock(Location location) {
            this.location = location;
        }

        private void cancel() {
            if (task != null) {
                task.cancel();
            }
        }
    }
}
