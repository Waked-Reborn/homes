package com.wessel.mittenshomes.command;

import com.wessel.mittenshomes.Homes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

// Target UUID, not the admin's.
public class HomeAdminCommand implements TabExecutor {
    private static final List<String> ACTIONS = List.of("players", "list", "info", "tp", "delete");
    private static final int PAGE_SIZE = 10;
    private final Homes plugin;

    public HomeAdminCommand(Homes plugin) { this.plugin = plugin; }

    private String permission(String action) {
        return "mittenshomes.admin." + switch (action) {
            case "players", "list", "info" -> "view";
            case "tp" -> "teleport";
            default -> "delete";
        };
    }

    private boolean allowed(CommandSender sender, String action) {
        return sender.hasPermission(permission(action));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 && sender instanceof Player player && allowed(sender, "list")) {
            new com.wessel.mittenshomes.gui.HomeGUI(plugin).openAdminGUI(player);
            return true;
        }
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (!ACTIONS.contains(action)) {
            List<String> available = ACTIONS.stream().filter(a -> allowed(sender, a)).toList();
            sender.sendMessage(available.isEmpty() ? ChatColor.RED + "You do not have permission to manage homes."
                    : ChatColor.GRAY + "Usage: /" + label + " <" + String.join("|", available)
                    + "> [player|uuid] [home name] (players accepts an optional page number)");
            return true;
        }
        if (!allowed(sender, action)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do this.");
            return true;
        }
        try {
            if (action.equals("players")) {
                if (args.length > 2) throw new IllegalArgumentException("Usage: /" + label + " players [page]");
                showPlayers(sender, args.length == 2 ? Integer.parseInt(args[1]) : 1);
                return true;
            }
            if (args.length < 2 || (action.equals("list") ? args.length != 2 : args.length < 3)) {
                sender.sendMessage(ChatColor.GRAY + "Usage: /" + label + " " + action + " <player|uuid>"
                        + (action.equals("list") ? "" : " <home name>"));
                return true;
            }
            UUID owner = resolveOwner(args[1]);
            if (owner == null) {
                sender.sendMessage(ChatColor.RED + "Unknown player. Use a known player name or their full UUID.");
                return true;
            }
            var manager = plugin.getHomeManager();
            if (action.equals("list")) {
                var homes = manager.getPlayerHomes(owner);
                sender.sendMessage(ChatColor.GOLD + "Homes for " + args[1] + " (" + homes.size() + "): ");
                if (homes.isEmpty()) sender.sendMessage(ChatColor.GRAY + "No saved homes.");
                homes.forEach((name, location) -> sender.sendMessage(ChatColor.YELLOW + name + ChatColor.GRAY + " — " + describe(location)));
                return true;
            }
            String name = manager.resolveName(owner, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            if (!manager.hasHome(owner, name)) {
                sender.sendMessage(ChatColor.RED + "That player has no home named " + name + ".");
                return true;
            }
            switch (action) {
                case "info" -> sender.sendMessage(ChatColor.GOLD + args[1] + " / " + name + ChatColor.GRAY
                        + " — " + describe(manager.getHome(owner, name)));
                case "tp" -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(ChatColor.RED + "Only players can teleport. Use list or info from the console.");
                        return true;
                    }
                    Location destination = manager.getHome(owner, name);
                    if (destination == null || destination.getWorld() == null) {
                        sender.sendMessage(ChatColor.RED + "This home's world is unavailable.");
                        return true;
                    }
                    manager.cancelTeleport(player.getUniqueId());
                    if (player.teleport(destination)) {
                        sender.sendMessage(ChatColor.GREEN + "Teleported to " + args[1] + " / " + name + ".");
                        audit(sender, "teleported to", owner, name);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Teleport failed or was cancelled.");
                    }
                }
                case "delete" -> {
                    manager.removeHome(owner, name);
                    sender.sendMessage(ChatColor.GREEN + "Deleted " + args[1] + " / " + name + ".");
                    audit(sender, "deleted", owner, name);
                }
                default -> throw new IllegalArgumentException("Unknown action");
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Page must be a positive integer.");
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        } catch (IllegalStateException ex) {
            plugin.getLogger().log(Level.SEVERE, "Admin home action failed", ex);
            sender.sendMessage(ChatColor.RED + "Could not complete this action. Check the server log.");
        }
        return true;
    }

    private void showPlayers(CommandSender sender, int page) {
        List<UUID> owners = plugin.getHomeManager().getHomeOwners().stream().sorted().toList();
        int pages = Math.max(1, (owners.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 1 || page > pages) throw new IllegalArgumentException("Page must be between 1 and " + pages + ".");
        sender.sendMessage(ChatColor.GOLD + "Players with homes — page " + page + "/" + pages);
        if (owners.isEmpty()) sender.sendMessage(ChatColor.GRAY + "No saved homes.");
        for (UUID owner : owners.subList((page - 1) * PAGE_SIZE, Math.min(page * PAGE_SIZE, owners.size()))) {
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(owner);
            String name = player.getName();
            sender.sendMessage(ChatColor.YELLOW + (name == null ? owner.toString() : name)
                    + ChatColor.GRAY + " (" + owner + "): " + plugin.getHomeManager().getHomeCount(owner) + " home(s)");
        }
    }

    private String describe(Location location) {
        if (location == null) return "world unavailable (home preserved)";
        return String.format(Locale.ROOT, "%s: %.2f, %.2f, %.2f (yaw %.1f, pitch %.1f)",
                location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    private void audit(CommandSender sender, String action, UUID owner, String home) {
        plugin.getLogger().info("[HomeAdmin] " + sender.getName() + " " + action + " home '" + home + "' owned by " + owner);
    }

    private UUID resolveOwner(String input) {
        if (input.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            return UUID.fromString(input);
        Player online = plugin.getServer().getPlayerExact(input);
        if (online != null) return online.getUniqueId();
        // Keep profile lookups local.
        for (OfflinePlayer player : plugin.getServer().getOfflinePlayers()) {
            if (player.getName() != null && player.getName().equalsIgnoreCase(input)) return player.getUniqueId();
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) return matching(ACTIONS.stream().filter(a -> allowed(sender, a)).toList(), args.length == 0 ? "" : args[0]);
        String action = args[0].toLowerCase(Locale.ROOT);
        if (!ACTIONS.contains(action) || !allowed(sender, action) || action.equals("players")) return List.of();
        if (args.length == 2) {
            Set<String> players = new TreeSet<>();
            for (OfflinePlayer player : plugin.getServer().getOfflinePlayers()) {
                if (player.getName() != null) players.add(player.getName());
            }
            plugin.getServer().getOnlinePlayers().forEach(player -> players.add(player.getName()));
            plugin.getHomeManager().getHomeOwners().forEach(uuid -> players.add(uuid.toString()));
            return matching(players, args[1]);
        }
        if (action.equals("list")) return List.of();
        UUID owner = resolveOwner(args[1]);
        if (owner == null) return List.of();
        String prefix = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        int start = args.length == 3 ? 0 : String.join(" ", Arrays.copyOfRange(args, 2, args.length - 1)).length() + 1;
        return matching(plugin.getHomeManager().getPlayerHomes(owner).keySet(), prefix).stream()
                .map(name -> name.substring(Math.min(start, name.length()))).toList();
    }

    private List<String> matching(Collection<String> values, String prefix) {
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))).sorted().toList();
    }
}
