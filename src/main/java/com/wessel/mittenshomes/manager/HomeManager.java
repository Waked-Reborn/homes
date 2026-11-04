package com.wessel.mittenshomes.manager;
import com.wessel.mittenshomes.Homes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class HomeManager {
    private final Homes plugin;
    private final Map<String, Map<String, Location>> homes = new HashMap<>();
    private final Map<UUID, Long> tpCooldown = new HashMap<>();
    private final Map<UUID, BukkitTask> teleportTasks = new HashMap<>();
    public static final long COOLDOWN_MS = 5000;
    public static final long TELEPORT_DELAY_TICKS = 100;
    public static final int MAX_HOMES = 3;
    public HomeManager(Homes plugin) {
        this.plugin = plugin;
        load();
    }
    public void load() {
        homes.clear();
        if (!plugin.getConfig().contains("homes")) {
            return;
        }
        var homesSection = plugin.getConfig().getConfigurationSection("homes");
        if (homesSection == null) {
            return;
        }
        for (String uuidString : homesSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            Map<String, Location> playerHomes = new HashMap<>();
            var playerSection = plugin.getConfig().getConfigurationSection("homes." + uuidString);
            if (playerSection == null) {
                continue;
            }
            for (String name : playerSection.getKeys(false)) {
                String value = plugin.getConfig().getString("homes." + uuidString + "." + name);
                try {
                    Location loc = deserialize(value);
                    if (loc != null) {
                        playerHomes.put(name, loc);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load home " + name + " for " + uuidString);
                }
            }
            if (!playerHomes.isEmpty()) {
                homes.put(uuidString, playerHomes);
            }
        }
    }
    public void save() {
        plugin.getConfig().set("homes", null);
        for (Map.Entry<String, Map<String, Location>> entry : homes.entrySet()) {
            String uuidString = entry.getKey();
            Map<String, Location> playerHomes = entry.getValue();
            for (Map.Entry<String, Location> homeEntry : playerHomes.entrySet()) {
                plugin.getConfig().set("homes." + uuidString + "." + homeEntry.getKey(), serialize(homeEntry.getValue()));
            }
        }
        plugin.saveConfig();
    }
    private String serialize(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }
    private Location deserialize(String value) {
        String[] parts = value.split(",");
        if (parts.length != 6) {
            return null;
        }
        try {
            World world = plugin.getServer().getWorld(parts[0]);
            if (world == null) {
                return null;
            }
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }
    public boolean hasHome(UUID uuid, String name) {
        Map<String, Location> playerHomes = homes.get(uuid.toString());
        return playerHomes != null && playerHomes.containsKey(name);
    }
    public Location getHome(UUID uuid, String name) {
        Map<String, Location> playerHomes = homes.get(uuid.toString());
        if (playerHomes == null) {
            return null;
        }
        return playerHomes.get(name);
    }
    public void setHome(UUID uuid, String name, Location loc) {
        homes.computeIfAbsent(uuid.toString(), k -> new HashMap<>()).put(name, loc);
        save();
    }
    public void removeHome(UUID uuid, String name) {
        Map<String, Location> playerHomes = homes.get(uuid.toString());
        if (playerHomes != null) {
            playerHomes.remove(name);
            save();
        }
    }
    public int getHomeCount(UUID uuid) {
        Map<String, Location> playerHomes = homes.get(uuid.toString());
        if (playerHomes == null) {
            return 0;
        }
        return playerHomes.size();
    }
    public Map<String, Location> getPlayerHomes(UUID uuid) {
        Map<String, Location> playerHomes = homes.get(uuid.toString());
        if (playerHomes == null) {
            return new HashMap<>();
        }
        return new HashMap<>(playerHomes);
    }
    public boolean isOnCooldown(UUID uuid) {
        Long lastTp = tpCooldown.get(uuid);
        if (lastTp == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        return now - lastTp < COOLDOWN_MS;
    }
    public long getCooldownRemaining(UUID uuid) {
        Long lastTp = tpCooldown.get(uuid);
        if (lastTp == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long remaining = COOLDOWN_MS - (now - lastTp);
        return remaining > 0 ? remaining : 0;
    }
    public void setCooldown(UUID uuid) {
        tpCooldown.put(uuid, System.currentTimeMillis());
    }
    public boolean isTeleporting(UUID uuid) {
        return teleportTasks.containsKey(uuid);
    }
    public void cancelTeleport(UUID uuid) {
        BukkitTask task = teleportTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
    public void startTeleport(Player player, Location target) {
        UUID uuid = player.getUniqueId();
        cancelTeleport(uuid);
        setCooldown(uuid);
        final int[] seconds = {5};
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    teleportTasks.remove(uuid);
                    return;
                }
                if (seconds[0] > 0) {
                    player.sendMessage(ChatColor.YELLOW + "Teleporting in " + seconds[0] + "... (don't move)");
                    seconds[0]--;
                } else {
                    cancel();
                    teleportTasks.remove(uuid);
                    setCooldown(uuid);
                    player.teleport(target);
                    player.sendMessage(ChatColor.GREEN + "Teleported!");
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        teleportTasks.put(uuid, task);
    }
}
