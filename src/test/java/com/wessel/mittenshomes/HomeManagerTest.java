package com.wessel.mittenshomes;

import com.wessel.mittenshomes.manager.HomeManager;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HomeManagerTest {
    @TempDir Path directory;
    Homes plugin;
    Server server;
    World world;
    YamlConfiguration config;
    HomeManager manager;
    UUID uuid = UUID.randomUUID();

    @BeforeEach void setup() {
        plugin = mock(Homes.class);
        server = mock(Server.class);
        world = mock(World.class);
        config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("homes-test"));
        when(world.getName()).thenReturn("world");
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getWorld(world.getUID())).thenReturn(world);
        manager = new HomeManager(plugin);
    }

    Location location() { return new Location(world, -1.2, 64, 2.7, 90, 10); }
    String path(String name) { return "homes." + uuid + "." + name; }

    @Test void persistsAndReloadsCoordinatesAndRotation() {
        manager.setHome(uuid, "Ma base", location());
        var disk = YamlConfiguration.loadConfiguration(directory.resolve("config.yml").toFile());
        assertEquals("world,-1.2,64.0,2.7,90.0,10.0", disk.getString(path("Ma base")));
        when(plugin.getConfig()).thenReturn(disk);
        assertEquals(location(), new HomeManager(plugin).getHome(uuid, "Ma base"));
    }

    @Test void rejectsPathInjectionAndFormattingNames() {
        for (String name : List.of("a.b", "&aBase", "", " ", " Base", "Base ", "12345678901234567"))
            assertThrows(IllegalArgumentException.class, () -> manager.setHome(uuid, name, location()));
        assertEquals(0, manager.getHomeCount(uuid));
    }

    @Test void enforcesLimitButAllowsUpdating() {
        for (String name : List.of("a", "b", "c")) manager.setHome(uuid, name, location());
        assertThrows(IllegalArgumentException.class, () -> manager.setHome(uuid, "Home 1", location()));
        manager.setHome(uuid, "b", new Location(world, 5, 6, 7));
        assertEquals(5, manager.getHome(uuid, "b").getX());
        assertEquals(3, manager.getHomeCount(uuid));
    }

    @Test void unloadedAndInvalidEntriesSurviveUnrelatedSave() {
        config.set(path("Away"), "unloaded,1,2,3,0,0");
        config.set(path("Bad"), "world,NaN,2,3,0,0");
        config.set("homes.invalid-uuid.Base", "world,1,2,3,0,0");
        manager.load();
        assertTrue(manager.hasHome(uuid, "Away"));
        assertNull(manager.getHome(uuid, "Away"));
        assertFalse(manager.hasHome(uuid, "Bad"));
        manager.setHome(uuid, "New", location());
        var disk = YamlConfiguration.loadConfiguration(directory.resolve("config.yml").toFile());
        assertEquals("unloaded,1,2,3,0,0", disk.getString(path("Away")));
        assertTrue(disk.contains(path("Bad")));
        assertTrue(disk.contains("homes.invalid-uuid.Base"));
        when(server.getWorld("unloaded")).thenReturn(world);
        assertNotNull(manager.getHome(uuid, "Away"));
    }

    @Test void worldNameCanContainCommas() {
        when(world.getName()).thenReturn("world,custom");
        when(server.getWorld("world,custom")).thenReturn(world);
        manager.setHome(uuid, "Base", location());
        manager.load();
        assertEquals(location(), manager.getHome(uuid, "Base"));
    }

    @Test void numericalNamesTakePrecedenceOverLegacyAliases() {
        manager.setHome(uuid, "Home 1", location());
        assertEquals("Home 1", manager.resolveName(uuid, "1"));
        manager.setHome(uuid, "1", location());
        assertEquals("1", manager.resolveName(uuid, "1"));
    }

    @Test void returnedLocationsCannotMutateSavedHomes() {
        Location original = location();
        manager.setHome(uuid, "Base", original);
        original.setX(900);
        manager.getHome(uuid, "Base").setX(800);
        manager.getPlayerHomes(uuid).get("Base").setX(700);
        assertEquals(-1.2, manager.getHome(uuid, "Base").getX());
    }

    @Test void writeFailureRollsBackConfigAndMemory() throws Exception {
        manager.setHome(uuid, "Base", location());
        Path file = Files.createFile(directory.resolve("not-a-directory"));
        when(plugin.getDataFolder()).thenReturn(file.toFile());
        assertThrows(IllegalStateException.class, () -> manager.setHome(uuid, "Base", new Location(world, 9, 9, 9)));
        assertEquals(location(), manager.getHome(uuid, "Base"));
        assertEquals("world,-1.2,64.0,2.7,90.0,10.0", config.getString(path("Base")));
        assertThrows(IllegalStateException.class, () -> manager.removeHome(uuid, "Base"));
        assertTrue(manager.hasHome(uuid, "Base"));
    }

    @Test void deletePersistsAcrossReload() {
        manager.setHome(uuid, "Base", location());
        manager.removeHome(uuid, "Base");
        manager.load();
        assertFalse(manager.hasHome(uuid, "Base"));
        assertFalse(YamlConfiguration.loadConfiguration(directory.resolve("config.yml").toFile()).contains(path("Base")));
    }

    @Test void nonFiniteCoordinatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> manager.setHome(uuid, "Base", new Location(world, Double.NaN, 0, 0)));
        assertEquals(0, manager.getHomeCount(uuid));
    }

    @Test void teleportChecksDelaySuccessCooldownAndDuplicateRequests() {
        teleportScenario(true);
    }
    @Test void cancelledTeleportDoesNotClaimSuccessOrApplyCooldown() {
        teleportScenario(false);
    }
    void teleportScenario(boolean succeeds) {
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(task.getTaskId()).thenReturn(1);
        List<Runnable> scheduled = new ArrayList<>();
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(20L), eq(20L))).thenAnswer(call -> {
            scheduled.add(call.getArgument(1)); return task;
        });
        Player player = player();
        when(player.teleport(any(Location.class))).thenReturn(succeeds);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            manager.startTeleport(player, location());
            manager.startTeleport(player, location());
            assertEquals(1, scheduled.size());
            for (int i = 0; i < 4; i++) scheduled.getFirst().run();
            verify(player, never()).teleport(any(Location.class));
            scheduled.getFirst().run();
            verify(player).teleport(location());
            assertFalse(manager.isTeleporting(uuid));
            assertEquals(succeeds, manager.isOnCooldown(uuid));
            if (succeeds) verify(player).sendMessage(ChatColor.GREEN + "Teleported!");
            else verify(player, never()).sendMessage(ChatColor.GREEN + "Teleported!");
            verify(task).cancel();
        }
    }

    Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission("mittenshomes.home")).thenReturn(true);
        when(player.isOnline()).thenReturn(true);
        when(player.getLocation()).thenReturn(location());
        return player;
    }

    @Test void teleportWithoutPermissionOrAvailableWorldNeverStarts() {
        Player player = player();
        when(player.hasPermission("mittenshomes.home")).thenReturn(false);
        manager.startTeleport(player, location());
        assertFalse(manager.isTeleporting(uuid));
        when(player.hasPermission("mittenshomes.home")).thenReturn(true);
        manager.startTeleport(player, null);
        assertFalse(manager.isTeleporting(uuid));
    }

    @Test void disconnectCancelsPendingTaskAndClearsCooldown() {
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(task);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            manager.startTeleport(player(), location());
            manager.setCooldown(uuid);
            manager.disconnect(uuid);
            assertFalse(manager.isTeleporting(uuid));
            assertFalse(manager.isOnCooldown(uuid));
            verify(task).cancel();
        }
    }
}
