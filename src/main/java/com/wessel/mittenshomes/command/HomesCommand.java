package com.wessel.mittenshomes.command;
import com.wessel.mittenshomes.Homes;
import com.wessel.mittenshomes.gui.HomeGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
public class HomesCommand implements CommandExecutor {
    private final Homes plugin;
    public HomesCommand(Homes plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPlayers only."));
            return true;
        }
        Player player = (Player) sender;
        if (plugin.getHomeManager().getHomeCount(player.getUniqueId()) == 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou have no homes."));
            return true;
        }
        new HomeGUI(plugin).openHomeGUI(player);
        return true;
    }
}
