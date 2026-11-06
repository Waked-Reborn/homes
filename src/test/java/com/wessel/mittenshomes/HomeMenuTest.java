package com.wessel.mittenshomes;

import com.wessel.mittenshomes.gui.HomeGUI;
import com.wessel.mittenshomes.manager.HomeManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class HomeMenuTest {
    Homes plugin;
    HomeManager manager;
    Player player;
    Server server;
    UUID id = UUID.randomUUID();
    HomeGUI shown;
    MockedStatic<Bukkit> bukkit;

    @BeforeEach void setup() {
        plugin = mock(Homes.class);
        manager = mock(HomeManager.class);
        player = mock(Player.class);
        server = mock(Server.class);
        when(plugin.getHomeManager()).thenReturn(manager);
        when(plugin.getServer()).thenReturn(server);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getLocation()).thenReturn(new Location(mock(World.class), 1, 64, 1));
        when(player.hasPermission(anyString())).thenReturn(true);
        when(player.hasPermission("mittenshomes.admin.view")).thenReturn(false);
        when(server.getOfflinePlayer(any(UUID.class))).thenAnswer(call -> {
            OfflinePlayer profile = mock(OfflinePlayer.class);
            when(profile.getName()).thenReturn("Player");
            return profile;
        });
        Map<String, Location> homes = new TreeMap<>();
        homes.put("Base", null);
        when(manager.getPlayerHomes(id)).thenReturn(homes);
        when(manager.hasHome(id, "Base")).thenReturn(true);
        when(manager.getHomeSnapshot(id, "Base")).thenReturn("world,1,64,1,0,0");
        ItemFactory factory = mock(ItemFactory.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(factory.getItemMeta(any(Material.class))).thenReturn(meta);
        when(factory.isApplicable(any(ItemMeta.class), any(Material.class))).thenReturn(true);
        when(factory.asMetaFor(any(ItemMeta.class), any(Material.class))).thenReturn(meta);
        when(meta.clone()).thenReturn(meta);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getItemFactory).thenReturn(factory);
        bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString())).thenAnswer(call -> {
            shown = call.getArgument(0);
            Inventory inventory = mock(Inventory.class);
            when(inventory.getHolder()).thenReturn(shown);
            when(inventory.getSize()).thenReturn(call.getArgument(1));
            return inventory;
        });
    }

    @AfterEach void cleanup() { bukkit.close(); }

    void openConfirmation() {
        new HomeGUI(plugin).openHomeGUI(player);
        shown.handleClick(player, 20, ClickType.RIGHT);
        assertEquals(27, shown.getInventory().getSize());
    }

    @Test void deletingRequiresASecondClick() {
        openConfirmation();
        verify(manager, never()).removeHome(any(), anyString());
        shown.handleClick(player, 15, ClickType.LEFT);
        verify(manager).removeHome(id, "Base");
        assertEquals(54, shown.getInventory().getSize());
    }

    @Test void keepButtonLeavesHomeUntouched() {
        openConfirmation();
        shown.handleClick(player, 11, ClickType.LEFT);
        verify(manager, never()).removeHome(any(), anyString());
        assertEquals(54, shown.getInventory().getSize());
    }

    @Test void changedHomeCannotBeDeletedFromStaleConfirmation() {
        openConfirmation();
        when(manager.getHomeSnapshot(id, "Base")).thenReturn("world,9,64,9,0,0");
        shown.handleClick(player, 15, ClickType.LEFT);
        verify(manager, never()).removeHome(any(), anyString());
        verify(player).sendMessage(contains("home changed"));
    }

    @Test void revokedDeletePermissionIsCheckedAgain() {
        openConfirmation();
        when(player.hasPermission("mittenshomes.delhome")).thenReturn(false);
        shown.handleClick(player, 15, ClickType.LEFT);
        verify(manager, never()).removeHome(any(), anyString());
    }

    @Test void noSetPermissionMeansNoNewHome() {
        new HomeGUI(plugin).openHomeGUI(player);
        when(player.hasPermission("mittenshomes.sethome")).thenReturn(false);
        shown.handleClick(player, 22, ClickType.LEFT);
        verify(manager, never()).setHome(any(), anyString(), any());
    }

    @Test void staleEmptySlotDoesNotOverwriteNewHome() {
        new HomeGUI(plugin).openHomeGUI(player);
        when(manager.hasHome(id, "Home 1")).thenReturn(true);
        shown.handleClick(player, 22, ClickType.LEFT);
        verify(manager, never()).setHome(any(), anyString(), any());
        verify(player).sendMessage(contains("slot changed"));
    }

    @Test void otherViewerCannotUseTheMenu() {
        new HomeGUI(plugin).openHomeGUI(player);
        Player other = mock(Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        shown.handleClick(other, 20, ClickType.RIGHT);
        assertEquals(54, shown.getInventory().getSize());
        verify(manager, never()).getHomeSnapshot(any(), anyString());
    }

    @Test void legacyHomesHaveWorkingPagination() {
        Map<String, Location> homes = new TreeMap<>();
        for (int i = 0; i < 22; i++) homes.put(String.format("Home %02d", i), null);
        when(manager.getPlayerHomes(id)).thenReturn(homes);
        new HomeGUI(plugin).openHomeGUI(player);
        assertEquals("Home 00", shown.getHomeName(10));
        shown.handleClick(player, 50, ClickType.LEFT);
        assertEquals("Home 21", shown.getHomeName(10));
        assertNull(shown.getHomeName(11));
        shown.handleClick(player, 48, ClickType.LEFT);
        assertEquals("Home 00", shown.getHomeName(10));
    }

    @Test void adminBrowseRechecksViewPermission() {
        when(player.hasPermission("mittenshomes.admin.view")).thenReturn(true);
        when(manager.getHomeOwners()).thenReturn(Set.of(UUID.randomUUID()));
        new HomeGUI(plugin).openAdminGUI(player);
        HomeGUI dashboard = shown;
        when(player.hasPermission("mittenshomes.admin.view")).thenReturn(false);
        dashboard.handleClick(player, 10, ClickType.LEFT);
        assertSame(dashboard, shown);
        verify(manager, never()).getPlayerHomes(any());
    }
}
