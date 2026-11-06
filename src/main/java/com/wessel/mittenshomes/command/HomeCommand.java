package com.wessel.mittenshomes.command;

import com.wessel.mittenshomes.Homes;
import com.wessel.mittenshomes.gui.HomeGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class HomeCommand extends PlayerCommand {
    public HomeCommand(Homes plugin) { super(plugin); }
    @Override
    protected void execute(Player player, String[] args) {
        if (args.length == 0) {
            new HomeGUI(plugin).openHomeGUI(player);
            return;
        }
        String name = name(player, args);
        if (!plugin.getHomeManager().hasHome(player.getUniqueId(), name)) {
            player.sendMessage(ChatColor.RED + "You do not have a home named " + name + ".");
            return;
        }
        plugin.getHomeManager().startTeleport(player, plugin.getHomeManager().getHome(player.getUniqueId(), name));
    }
}
