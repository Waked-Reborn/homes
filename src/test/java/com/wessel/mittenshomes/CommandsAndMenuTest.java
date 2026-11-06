package com.wessel.mittenshomes;

import com.wessel.mittenshomes.command.*;
import com.wessel.mittenshomes.gui.HomeGUI;
import com.wessel.mittenshomes.manager.HomeManager;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CommandsAndMenuTest {
    @Test void setAndDeleteUseWholeNameIncludingSpaces() {
        Homes plugin = mock(Homes.class);
        HomeManager manager = mock(HomeManager.class);
        when(plugin.getHomeManager()).thenReturn(manager);
        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(manager.resolveName(uuid, "Ma base")).thenReturn("Ma base");
        when(manager.hasHome(uuid, "Ma base")).thenReturn(true);
        Location location = new Location(mock(World.class), 1, 2, 3);
        when(player.getLocation()).thenReturn(location);
        Command command = mock(Command.class);
        when(command.getName()).thenReturn("sethome");
        new SethomeCommand(plugin).onCommand(player, command, "sethome", new String[]{"Ma", "base"});
        verify(manager).setHome(uuid, "Ma base", location);
        when(command.getName()).thenReturn("delhome");
        new DelhomeCommand(plugin).onCommand(player, command, "delhome", new String[]{"Ma", "base"});
        verify(manager).removeHome(uuid, "Ma base");
    }

    @Test void commandPermissionDenialDoesNotMutateHomes() {
        Homes plugin = mock(Homes.class);
        HomeManager manager = mock(HomeManager.class);
        when(plugin.getHomeManager()).thenReturn(manager);
        Player player = mock(Player.class);
        Command command = mock(Command.class);
        when(command.getName()).thenReturn("sethome");
        new SethomeCommand(plugin).onCommand(player, command, "sethome", new String[]{"Base"});
        verifyNoInteractions(manager);
    }

    @Test void menuDisplaysNamedHomesAndFillsOnlyRemainingSlots() { menu(false); }
    @Test void menuWithoutHomesOpensWithThreeCreationSlots() { menu(true); }

    void menu(boolean empty) {
        Homes plugin = mock(Homes.class);
        HomeManager manager = mock(HomeManager.class);
        when(plugin.getHomeManager()).thenReturn(manager);
        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        Map<String, Location> homes = new TreeMap<>();
        if (!empty) homes.put("Ma base", null); // An unavailable world must still be visible.
        when(manager.getPlayerHomes(uuid)).thenReturn(homes);
        Inventory inventory = mock(Inventory.class);
        ItemFactory factory = mock(ItemFactory.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(factory.getItemMeta(any(Material.class))).thenReturn(meta);
        when(factory.isApplicable(any(ItemMeta.class), any(Material.class))).thenReturn(true);
        when(factory.asMetaFor(any(ItemMeta.class), any(Material.class))).thenReturn(meta);
        when(meta.clone()).thenReturn(meta);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getItemFactory).thenReturn(factory);
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(27), anyString())).thenReturn(inventory);
            HomeGUI gui = new HomeGUI(plugin);
            gui.openHomeGUI(player);
            assertEquals(empty ? "Home 1" : "Ma base", gui.getHomeName(12));
            assertNotNull(gui.getHomeName(13));
            assertNotNull(gui.getHomeName(14));
            assertNull(gui.getHomeName(15));
            assertEquals(uuid, gui.getOwner());
            assertSame(inventory, gui.getInventory());
            verify(player).openInventory(inventory);
        }
    }
}
