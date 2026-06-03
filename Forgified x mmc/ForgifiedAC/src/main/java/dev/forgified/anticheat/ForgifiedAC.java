package dev.forgified.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ForgifiedAC extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, Integer> flightVL = new HashMap<>();
    private final Map<UUID, Integer> reachVL = new HashMap<>();
    private final Map<UUID, Integer> speedVL = new HashMap<>();
    private final Set<UUID> alertsEnabled = new HashSet<>();
    private String prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", "&8[&cForgifiedAC&8] "));
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("alerts").setExecutor(this);
        getLogger().info("ForgifiedAC has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if (!player.hasPermission("forgifiedac.admin")) {
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

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("forgifiedac.bypass") || player.getAllowFlight()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Horizontal speed check
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (getConfig().getBoolean("checks.speed.enabled") && speed > getConfig().getDouble("checks.speed.max-speed")) {
            flag(player, "Speed", speedVL, getConfig().getInt("checks.speed.alert-vl"));
        }

        // Vertical / Flight check
        double deltaY = to.getY() - from.getY();
        if (getConfig().getBoolean("checks.flight.enabled")) {
            if (deltaY > 0 && !player.getLocation().getBlock().getType().isSolid() && player.getFallDistance() == 0) {
                // Very basic upward movement check
                if (deltaY > 0.42) { // Higher than a normal jump
                     flag(player, "Flight (Upward)", flightVL, getConfig().getInt("checks.flight.alert-vl"));
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();
        if (damager.hasPermission("forgifiedac.bypass")) return;

        if (getConfig().getBoolean("checks.reach.enabled")) {
            double distance = damager.getLocation().distance(event.getEntity().getLocation());
            double max = getConfig().getDouble("checks.reach.max-distance", 3.8);

            if (distance > max) {
                flag(damager, "Reach (" + String.format("%.2f", distance) + ")", reachVL, getConfig().getInt("checks.reach.alert-vl"));
            }
        }
    }

    private void flag(Player player, String check, Map<UUID, Integer> vlMap, int alertThreshold) {
        int vl = vlMap.getOrDefault(player.getUniqueId(), 0) + 1;
        vlMap.put(player.getUniqueId(), vl);

        if (vl >= alertThreshold) {
            String alert = prefix + ChatColor.RED + player.getName() + ChatColor.GRAY + " flagged " + 
                           ChatColor.WHITE + check + ChatColor.GRAY + " (VL: " + ChatColor.RED + vl + ChatColor.GRAY + ")";
            
            for (UUID uuid : alertsEnabled) {
                Player staff = Bukkit.getPlayer(uuid);
                if (staff != null) staff.sendMessage(alert);
            }
            
            if (getConfig().getBoolean("punishments.enabled")) {
                // Execute punishment commands if configured
                List<String> cmds = getConfig().getStringList("punishments.commands");
                for (String cmd : cmds) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("<player>", player.getName()));
                }
            }
        }
    }
}
