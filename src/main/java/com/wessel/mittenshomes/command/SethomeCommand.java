package com.wessel.mittenshomes.command;
import com.wessel.mittenshomes.Homes;
import com.wessel.mittenshomes.manager.HomeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
public class SethomeCommand implements CommandExecutor {
    private final Homes plugin;
    public SethomeCommand(Homes plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPlayers only."));
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 1) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Usage: &f/sethome <name>"));
            return true;
        }
        String name = args[0];
        if (name.length() > 16) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cHome name must be 16 characters or less."));
            return true;
        }
        if (plugin.getHomeManager().hasHome(player.getUniqueId(), name)) {
            plugin.getHomeManager().setHome(player.getUniqueId(), name, player.getLocation());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aUpdated home &f" + name + "&a."));
            return true;
        }
        int count = plugin.getHomeManager().getHomeCount(player.getUniqueId());
        if (count >= HomeManager.MAX_HOMES) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou have reached the maximum of " + HomeManager.MAX_HOMES + " homes."));
            return true;
        }
        plugin.getHomeManager().setHome(player.getUniqueId(), name, player.getLocation());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aHome &f" + name + "&a has been saved."));
        return true;
    }
}
