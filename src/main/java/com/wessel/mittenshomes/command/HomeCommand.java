package com.wessel.mittenshomes.command;
import com.wessel.mittenshomes.Homes;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
public class HomeCommand implements CommandExecutor {
    private final Homes plugin;
    public HomeCommand(Homes plugin) {
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
            new com.wessel.mittenshomes.gui.HomeGUI(plugin).openHomeGUI(player);
            return true;
        }
        String input = args[0];
        String homeName = resolveHomeName(input);
        if (!plugin.getHomeManager().hasHome(player.getUniqueId(), homeName)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou do not have a home named &f" + input + "&c."));
            return true;
        }
        if (plugin.getHomeManager().isOnCooldown(player.getUniqueId())) {
            long remaining = plugin.getHomeManager().getCooldownRemaining(player.getUniqueId());
            long seconds = (remaining + 999) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPlease wait &f" + seconds + "&c second(s) before teleporting again."));
            return true;
        }
        if (plugin.getHomeManager().isTeleporting(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou are already teleporting!"));
            return true;
        }
        org.bukkit.Location loc = plugin.getHomeManager().getHome(player.getUniqueId(), homeName);
        if (loc == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cHome data is corrupted."));
            return true;
        }
        plugin.getHomeManager().startTeleport(player, loc);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eTeleporting to home &f" + homeName + "&e in 5 seconds... (don't move)"));
        return true;
    }
    private String resolveHomeName(String input) {
        if (input.equalsIgnoreCase("1")) {
            return "Home 1";
        }
        if (input.equalsIgnoreCase("2")) {
            return "Home 2";
        }
        if (input.equalsIgnoreCase("3")) {
            return "Home 3";
        }
        return input;
    }
}
