package com.wessel.mittenshomes.command;
import com.wessel.mittenshomes.Homes;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
public class DelhomeCommand implements CommandExecutor {
    private final Homes plugin;
    public DelhomeCommand(Homes plugin) {
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
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Usage: &f/delhome <name>"));
            return true;
        }
        String name = args[0];
        if (!plugin.getHomeManager().hasHome(player.getUniqueId(), name)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou do not have a home named &f" + name + "&c."));
            return true;
        }
        plugin.getHomeManager().removeHome(player.getUniqueId(), name);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cHome &f" + name + "&c has been deleted."));
        return true;
    }
}
