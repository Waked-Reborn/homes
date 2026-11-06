package com.wessel.mittenshomes.command;

import com.wessel.mittenshomes.Homes;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public abstract class PlayerCommand implements TabExecutor {
    protected final Homes plugin;
    protected PlayerCommand(Homes plugin) { this.plugin = plugin; }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!player.hasPermission("mittenshomes." + command.getName())) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        try {
            execute(player, args);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + ex.getMessage());
        } catch (IllegalStateException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not update homes", ex);
            player.sendMessage(ChatColor.RED + "Could not save your homes. Please contact an administrator.");
        }
        return true;
    }

    protected abstract void execute(Player player, String[] args);

    protected String name(Player player, String[] args) {
        return plugin.getHomeManager().resolveName(player.getUniqueId(), String.join(" ", args));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !sender.hasPermission("mittenshomes." + command.getName())) return List.of();
        String prefix = String.join(" ", args).toLowerCase(Locale.ROOT);
        int start = args.length <= 1 ? 0 : String.join(" ", java.util.Arrays.copyOf(args, args.length - 1)).length() + 1;
        return plugin.getHomeManager().getPlayerHomes(player.getUniqueId()).keySet().stream()
                .filter(home -> home.toLowerCase(Locale.ROOT).startsWith(prefix))
                .map(home -> home.substring(Math.min(start, home.length()))).toList();
    }
}
