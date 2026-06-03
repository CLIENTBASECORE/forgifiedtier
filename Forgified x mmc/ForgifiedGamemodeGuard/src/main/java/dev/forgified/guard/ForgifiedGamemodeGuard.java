package dev.forgified.guard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;

public class ForgifiedGamemodeGuard extends JavaPlugin implements Listener {

    private final List<String> blockedCommands = Arrays.asList("/l", "/lobby", "/spawn", "/hub");

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ForgifiedGamemodeGuard enabled! Protecting gamemodes from lobby commands.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase().trim();
        String cmd = msg.split(" ")[0];
        
        if (blockedCommands.contains(cmd)) {
            Player player = event.getPlayer();
            if (isBusy(player)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou cannot use this command while in a game! Use /leave instead."));
            }
        }
    }

    private boolean isBusy(Player player) {
        return isInPracticeMatch(player);
    }

    private boolean isInPracticeMatch(Player player) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ForgifiedPractice");
        if (plugin == null || !plugin.isEnabled()) return false;
        try {
            Field field = plugin.getClass().getDeclaredField("matches");
            field.setAccessible(true);
            Map<?, ?> matches = (Map<?, ?>) field.get(plugin);
            return matches.containsKey(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isInFastbridgeSession(Player player) {
        for (String name : new String[]{"Fastbridge", "FastBuilder"}) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
            if (plugin == null || !plugin.isEnabled()) continue;
            try {
                Method getIslandManager = plugin.getClass().getMethod("getIslandManager");
                Object islandManager = getIslandManager.invoke(plugin);
                if (islandManager == null) continue;
                
                // Try Player class first
                try {
                    Method getIslandForPlayer = islandManager.getClass().getMethod("getIslandForPlayer", Player.class);
                    if (getIslandForPlayer.invoke(islandManager, player) != null) return true;
                } catch (NoSuchMethodException e) {
                    // Try UUID class
                    Method getIslandForUUID = islandManager.getClass().getMethod("getIslandForPlayer", UUID.class);
                    if (getIslandForUUID.invoke(islandManager, player.getUniqueId()) != null) return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}
