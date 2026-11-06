package com.wessel.mittenshomes;

import com.wessel.mittenshomes.command.HomeAdminCommand;
import com.wessel.mittenshomes.manager.HomeManager;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class HomeAdminCommandTest {
    Homes plugin;
    HomeManager manager;
    Server server;
    Player admin;
    OfflinePlayer owner;
    UUID ownerId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    HomeAdminCommand executor;
    Command command;
    Location destination;

    @BeforeEach void setup() {
        plugin = mock(Homes.class);
        manager = mock(HomeManager.class);
        server = mock(Server.class);
        admin = mock(Player.class);
        owner = mock(OfflinePlayer.class);
        command = mock(Command.class);
        when(plugin.getHomeManager()).thenReturn(manager);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(admin.getUniqueId()).thenReturn(adminId);
        when(admin.getName()).thenReturn("Admin");
        when(owner.getUniqueId()).thenReturn(ownerId);
        when(owner.getName()).thenReturn("Owner");
        when(server.getOfflinePlayers()).thenReturn(new OfflinePlayer[]{owner});
        when(server.getOfflinePlayer(ownerId)).thenReturn(owner);
        when(manager.resolveName(ownerId, "My base")).thenReturn("My base");
        when(manager.hasHome(ownerId, "My base")).thenReturn(true);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        destination = new Location(world, 1, 64, -2, 90, 0);
        when(manager.getHome(ownerId, "My base")).thenReturn(destination);
        when(manager.getPlayerHomes(ownerId)).thenReturn(Map.of("My base", destination));
        executor = new HomeAdminCommand(plugin);
    }

    void run(String... args) { executor.onCommand(admin, command, "homeadmin", args); }
    void permit(String action) { when(admin.hasPermission("mittenshomes.admin." + action)).thenReturn(true); }

    @Test void unauthorizedActionsDoNotReadOrChangeHomes() {
        for (String action : List.of("players", "list", "info", "tp", "delete")) run(action, "Owner", "My", "base");
        verifyNoInteractions(manager, server);
        assertTrue(executor.onTabComplete(admin, command, "homeadmin", new String[]{""}).isEmpty());
        assertTrue(executor.onTabComplete(admin, command, "homeadmin", new String[]{"list", ""}).isEmpty());
    }

    @Test void viewPermissionDoesNotGrantTeleportOrDelete() {
        permit("view");
        run("tp", "Owner", "My", "base");
        run("delete", "Owner", "My", "base");
        verifyNoInteractions(manager);
        assertEquals(List.of("info", "list", "players"), executor.onTabComplete(admin, command, "homeadmin", new String[]{""}));
    }

    @Test void listsOfflinePlayersHomesByCaseInsensitiveName() {
        permit("view");
        run("list", "oWnEr");
        verify(manager).getPlayerHomes(ownerId);
        verify(admin).sendMessage(contains("My base"));
        verify(server, never()).getOfflinePlayer(anyString());
    }

    @Test void fullUuidWorksWithoutAnOfflineNameRecord() {
        permit("view");
        run("info", ownerId.toString(), "My", "base");
        verify(manager).getHome(ownerId, "My base");
        verify(server, never()).getOfflinePlayers();
        verify(admin).sendMessage(contains("world: 1.00, 64.00, -2.00"));
    }

    @Test void teleportsAdminToOwnersHomeAndCancelsAdminsPendingTeleport() {
        permit("teleport");
        when(admin.teleport(destination)).thenReturn(true);
        run("tp", "Owner", "My", "base");
        verify(manager).cancelTeleport(adminId);
        verify(manager, never()).cancelTeleport(ownerId);
        verify(admin).teleport(destination);
        verify(plugin.getLogger()).info(contains("teleported to"));
        verify(manager, never()).setCooldown(any());
    }

    @Test void failedTeleportDoesNotReportOrAuditSuccess() {
        permit("teleport");
        run("tp", "Owner", "My", "base");
        verify(admin).sendMessage(contains("Teleport failed"));
        verify(plugin.getLogger(), never()).info(anyString());
    }

    @Test void unavailableWorldIsReportedWithoutTeleporting() {
        permit("teleport");
        when(manager.getHome(ownerId, "My base")).thenReturn(null);
        run("tp", "Owner", "My", "base");
        verify(admin, never()).teleport(any(Location.class));
        verify(admin).sendMessage(contains("world is unavailable"));
    }

    @Test void deleteUsesOwnersUuidAndAuditsSuccess() {
        permit("delete");
        run("delete", "Owner", "My", "base");
        verify(manager).removeHome(ownerId, "My base");
        verify(manager, never()).removeHome(eq(adminId), anyString());
        verify(plugin.getLogger()).info(contains("deleted"));
    }

    @Test void failedSaveDoesNotReportDeletionOrAuditSuccess() {
        permit("delete");
        doThrow(new IllegalStateException("Disk full")).when(manager).removeHome(ownerId, "My base");
        run("delete", "Owner", "My", "base");
        verify(admin).sendMessage(contains("Could not complete"));
        verify(admin, never()).sendMessage(contains("Deleted"));
        verify(plugin.getLogger(), never()).info(anyString());
    }

    @Test void unknownPlayerDoesNotCreateOrFetchRemoteProfile() {
        permit("view");
        run("list", "Unknown");
        verify(admin).sendMessage(contains("Unknown player"));
        verify(server, never()).getOfflinePlayer(anyString());
        verifyNoInteractions(manager);
    }

    @Test void consoleCanInspectButCannotTeleport() {
        CommandSender console = mock(CommandSender.class);
        when(console.hasPermission(anyString())).thenReturn(true);
        executor.onCommand(console, command, "homeadmin", new String[]{"list", "Owner"});
        verify(console).sendMessage(contains("My base"));
        executor.onCommand(console, command, "homeadmin", new String[]{"tp", "Owner", "My", "base"});
        verify(console).sendMessage(contains("Only players can teleport"));
    }

    @Test void tabCompletionPreservesMultiwordHomeNames() {
        permit("view");
        assertEquals(List.of("My base"), executor.onTabComplete(admin, command, "homeadmin", new String[]{"info", "Owner", "My"}));
        assertEquals(List.of("base"), executor.onTabComplete(admin, command, "homeadmin", new String[]{"info", "Owner", "My", "b"}));
    }

    @Test void emptyOwnerListAndInvalidPagesAreHandled() {
        permit("view");
        when(manager.getHomeOwners()).thenReturn(Set.of());
        run("players");
        verify(admin).sendMessage(contains("No saved homes"));
        run("players", "0");
        verify(admin).sendMessage(contains("between 1 and 1"));
        run("players", "abc");
        verify(admin).sendMessage(contains("positive integer"));
    }
}
