package dev.forgified.chat;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ForgifiedChat extends JavaPlugin {

    private static ForgifiedChat instance;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("ForgifiedChat API enabled!");
    }

    public static ForgifiedChat getInstance() {
        return instance;
    }

    /**
     * Sends a line with two clickable buttons to a player.
     */
    public void sendDuelButtons(Player target, String acceptCmd, String denyCmd) {
        TextComponent line = new TextComponent("");
        
        TextComponent acceptBtn = new TextComponent(ChatColor.translateAlternateColorCodes('&', "&a&l[ACCEPT]"));
        acceptBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, acceptCmd));
        
        TextComponent denyBtn = new TextComponent(ChatColor.translateAlternateColorCodes('&', " &c&l[DENY]"));
        denyBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, denyCmd));
        
        line.addExtra(acceptBtn);
        line.addExtra(denyBtn);
        
        target.spigot().sendMessage(line);
    }
}
