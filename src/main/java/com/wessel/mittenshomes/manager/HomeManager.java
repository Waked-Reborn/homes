package com.wessel.mittenshomes.manager;

import com.wessel.mittenshomes.Homes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class HomeManager {
    private final Homes plugin;
    // Keep world names, not World references: unloaded worlds must not erase homes.
    private final Map<UUID, Map<String, String>> homes = new HashMap<>();
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
        var section = plugin.getConfig().getConfigurationSection("homes");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(id);
                var playerSection = section.getConfigurationSection(id);
                if (playerSection == null) continue;
                Map<String, String> playerHomes = new TreeMap<>();
                for (String name : playerSection.getKeys(false)) {
                    String value = playerSection.getString(name);
                    try {
                        parse(value); // Validate coordinates even if the world is unavailable.
                        playerHomes.put(name, value);
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Invalid home " + name + " for " + id + "; preserved in config.yml.");
                    }
                }
                homes.put(uuid, playerHomes);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid player UUID " + id + "; preserved in config.yml.");
            }
        }
    }

    public void save() {
        Path temporary = null;
        try {
            Path directory = plugin.getDataFolder().toPath();
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, "homes-", ".yml");
            plugin.getConfig().save(temporary.toFile());
            try {
                Files.move(temporary, directory.resolve("config.yml"), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temporary, directory.resolve("config.yml"), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save homes", ex);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private void persist(UUID uuid, String name, String value) {
        String path = "homes." + uuid + "." + name;
        Object previous = plugin.getConfig().get(path);
        plugin.getConfig().set(path, value);
        try {
            save();
        } catch (RuntimeException ex) {
            plugin.getConfig().set(path, previous);
            throw ex;
        }
    }

    private String serialize(Location loc) {
        loc.checkFinite();
        if (loc.getWorld() == null) throw new IllegalArgumentException("World is unavailable");
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ()
                + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private String[] parse(String value) {
        if (value == null) throw new IllegalArgumentException("Missing location");
        // World names can contain commas; coordinates are the final five fields.
        String[] parts = value.split(",", -1);
        int offset = parts.length - 5;
        if (offset < 1) throw new IllegalArgumentException("Invalid location");
        String world = String.join(",", Arrays.copyOfRange(parts, 0, offset));
        if (world.isBlank()) throw new IllegalArgumentException("Missing world");
        String[] result = {world, parts[offset], parts[offset + 1], parts[offset + 2], parts[offset + 3], parts[offset + 4]};
        for (int i = 1; i < 6; i++) {
            double number = i < 4 ? Double.parseDouble(result[i]) : Float.parseFloat(result[i]);
            if (!Double.isFinite(number)) throw new IllegalArgumentException("Non-finite coordinate");
        }
        return result;
    }

    public static boolean isValidName(String name) {
        return name != null && !name.isBlank() && name.equals(name.trim())
                && name.matches("[\\p{L}\\p{N}_ -]{1,16}");
    }

    public String resolveName(UUID uuid, String input) {
        if (hasHome(uuid, input)) return input;
        if (input.matches("[1-3]") && hasHome(uuid, "Home " + input)) return "Home " + input;
        return input;
    }

    public boolean hasHome(UUID uuid, String name) {
        return homes.getOrDefault(uuid, Map.of()).containsKey(name);
    }

    public Location getHome(UUID uuid, String name) {
        String value = homes.getOrDefault(uuid, Map.of()).get(name);
        if (value == null) return null;
        String[] parts = parse(value);
        World world = plugin.getServer().getWorld(parts[0]);
        if (world == null) return null;
        return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
    }

    public void setHome(UUID uuid, String name, Location loc) {
        if (!isValidName(name)) throw new IllegalArgumentException("Invalid home name");
        if (!hasHome(uuid, name) && getHomeCount(uuid) >= MAX_HOMES)
            throw new IllegalArgumentException("Maximum of " + MAX_HOMES + " homes reached");
        String value = serialize(loc);
        persist(uuid, name, value);
        homes.computeIfAbsent(uuid, key -> new TreeMap<>()).put(name, value);
        cancelTeleport(uuid);
    }

    public void removeHome(UUID uuid, String name) {
        if (!hasHome(uuid, name)) return;
        persist(uuid, name, null);
        homes.get(uuid).remove(name);
        cancelTeleport(uuid);
    }

    public int getHomeCount(UUID uuid) {
        return homes.getOrDefault(uuid, Map.of()).size();
    }

    public Map<String, Location> getPlayerHomes(UUID uuid) {
        Map<String, Location> result = new TreeMap<>();
        homes.getOrDefault(uuid, Map.of()).keySet().forEach(name -> result.put(name, getHome(uuid, name)));
        return result;
    }

    public boolean isOnCooldown(UUID uuid) { return getCooldownRemaining(uuid) > 0; }

    public long getCooldownRemaining(UUID uuid) {
        Long last = tpCooldown.get(uuid);
        if (last == null) return 0;
        long remaining = COOLDOWN_MS - (System.nanoTime() - last) / 1_000_000;
        if (remaining <= 0) tpCooldown.remove(uuid);
        return Math.max(0, remaining);
    }

    public void setCooldown(UUID uuid) { tpCooldown.put(uuid, System.nanoTime()); }
    public boolean isTeleporting(UUID uuid) { return teleportTasks.containsKey(uuid); }

    public void cancelTeleport(UUID uuid) {
        BukkitTask task = teleportTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void disconnect(UUID uuid) {
        cancelTeleport(uuid);
        tpCooldown.remove(uuid);
    }

    public void shutdown() {
        new ArrayList<>(teleportTasks.keySet()).forEach(this::cancelTeleport);
        tpCooldown.clear();
    }

    public void startTeleport(Player player, Location target) {
        UUID uuid = player.getUniqueId();
        if (!player.hasPermission("mittenshomes.home")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to teleport home.");
            return;
        }
        if (isTeleporting(uuid)) {
            player.sendMessage(ChatColor.RED + "You are already teleporting!");
            return;
        }
        if (isOnCooldown(uuid)) {
            player.sendMessage(ChatColor.RED + "Please wait " + ((getCooldownRemaining(uuid) + 999) / 1000) + " second(s) before teleporting again.");
            return;
        }
        if (target == null || target.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "This home's world is unavailable.");
            return;
        }
        Location destination = target.clone();
        Location origin = player.getLocation().clone();
        player.sendMessage(ChatColor.YELLOW + "Teleporting in 5 seconds... (don't move)");
        BukkitTask task = new BukkitRunnable() {
            private long remaining = TELEPORT_DELAY_TICKS;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.hasPermission("mittenshomes.home")) {
                    cancelTeleport(uuid);
                    return;
                }
                Location current = player.getLocation();
                if (!Objects.equals(origin.getWorld(), current.getWorld()) || origin.distanceSquared(current) > 0.01) {
                    cancelTeleport(uuid);
                    player.sendMessage(ChatColor.RED + "Teleport cancelled! You moved.");
                    return;
                }
                remaining -= 20;
                if (remaining > 0) {
                    player.sendMessage(ChatColor.YELLOW + "Teleporting in " + remaining / 20 + "...");
                    return;
                }
                cancelTeleport(uuid);
                World world = destination.getWorld();
                if (world != null && plugin.getServer().getWorld(world.getUID()) != null && player.teleport(destination)) {
                    setCooldown(uuid);
                    player.sendMessage(ChatColor.GREEN + "Teleported!");
                } else {
                    player.sendMessage(ChatColor.RED + "Teleport failed or was cancelled.");
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
        teleportTasks.put(uuid, task);
    }
}
