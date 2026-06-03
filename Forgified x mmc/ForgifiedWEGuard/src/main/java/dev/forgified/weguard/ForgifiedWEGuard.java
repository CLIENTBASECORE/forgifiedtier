package dev.forgified.weguard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;

public class ForgifiedWEGuard extends JavaPlugin implements Listener {

    private final List<String> blockedPrefixes = Arrays.asList("//", "/worldedit", "/we", "/wand", "/pos1", "/pos2", "/sel", "/set", "/replace", "/copy", "/paste", "/schem", "/schematic");

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ForgifiedWEGuard enabled! Blocking WorldEdit in games.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase().trim();
        
        boolean isWE = false;
        if (msg.startsWith("//")) {
            isWE = true;
        } else {
            String cmd = msg.split(" ")[0];
            for (String prefix : blockedPrefixes) {
                if (cmd.equals(prefix)) {
                    isWE = true;
                    break;
                }
            }
        }
        
        if (isWE) {
            Player player = event.getPlayer();
            if (isBusy(player)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cWorldEdit is disabled while in a game!"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isBusy(player)) {
            if (player.getItemInHand() != null && player.getItemInHand().getType() == Material.WOOD_AXE) {
                if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cWorldEdit is disabled while in a game!"));
                }
            }
        }
    }

    private boolean isBusy(Player player) {
        return isInPracticeMatch(player) || isInFastbridgeSession(player);
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
                
                try {
                    Method getIslandForPlayer = islandManager.getClass().getMethod("getIslandForPlayer", Player.class);
                    if (getIslandForPlayer.invoke(islandManager, player) != null) return true;
                } catch (NoSuchMethodException e) {
                    Method getIslandForUUID = islandManager.getClass().getMethod("getIslandForPlayer", UUID.class);
                    if (getIslandForUUID.invoke(islandManager, player.getUniqueId()) != null) return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}
