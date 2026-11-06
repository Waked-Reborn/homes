package com.wessel.mittenshomes.command;

import com.wessel.mittenshomes.Homes;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SethomeCommand extends PlayerCommand {
    public SethomeCommand(Homes plugin) { super(plugin); }
    @Override
    protected void execute(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.GRAY + "Usage: /sethome <name>");
            return;
        }
        String name = name(player, args);
        plugin.getHomeManager().setHome(player.getUniqueId(), name, player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Home " + name + " has been saved.");
    }
}
