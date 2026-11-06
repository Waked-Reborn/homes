package com.wessel.mittenshomes;

import com.wessel.mittenshomes.gui.*;
import com.wessel.mittenshomes.manager.HomeManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.*;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class HomeGUIListenerTest {
    Homes plugin;
    HomeManager manager;
    HomeGUIListener listener;
    Player player;
    Inventory top;
    InventoryView view;
    HomeGUI gui;
    BukkitScheduler scheduler;

    @BeforeEach void setup() {
        plugin = mock(Homes.class);
        manager = mock(HomeManager.class);
        when(plugin.getHomeManager()).thenReturn(manager);
        Server server = mock(Server.class);
        scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        listener = new HomeGUIListener(plugin);
        player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOnline()).thenReturn(true);
        top = mock(Inventory.class);
        gui = mock(HomeGUI.class);
        when(gui.getOwner()).thenReturn(uuid);
        when(gui.getInventory()).thenReturn(top);
        when(top.getHolder()).thenReturn(gui);
        when(top.getSize()).thenReturn(27);
        view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        when(player.getOpenInventory()).thenReturn(view);
    }

    InventoryClickEvent click(int rawSlot) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlot()).thenReturn(rawSlot);
        when(event.getClick()).thenReturn(ClickType.LEFT);
        return event;
    }

    @Test void playerInventoryClicksCannotTriggerHomeActions() {
        InventoryClickEvent event = click(39);
        listener.onInventoryClick(event);
        verify(event).setCancelled(true);
        verifyNoInteractions(manager, scheduler);
    }

    @Test void unrelatedInventoryIsNotCancelled() {
        when(top.getHolder()).thenReturn(null);
        InventoryClickEvent event = click(12);
        listener.onInventoryClick(event);
        verify(event, never()).setCancelled(anyBoolean());
        verifyNoInteractions(manager);
    }

    @Test void draggingIntoMenuIsCancelled() {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getView()).thenReturn(view);
        listener.onInventoryDrag(event);
        verify(event).setCancelled(true);
    }

    @Test void menuActionRunsAfterClickTransaction() {
        when(gui.getHomeName(12)).thenReturn("Home 1");
        InventoryClickEvent event = click(12);
        listener.onInventoryClick(event);
        var runnable = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(eq(plugin), runnable.capture());
        verify(manager, never()).setHome(any(), any(), any());
        runnable.getValue().run();
        verify(gui).handleClick(player, 12, ClickType.LEFT);
        verify(manager, never()).setHome(any(), any(), any());
    }

    @Test void movementBetweenWorldsCancelsWithoutDistanceException() {
        when(manager.isTeleporting(player.getUniqueId())).thenReturn(true);
        World first = mock(World.class), second = mock(World.class);
        var event = new PlayerMoveEvent(player, new Location(first, 0, 0, 0), new Location(second, 0, 0, 0));
        assertDoesNotThrow(() -> listener.onPlayerMove(event));
        verify(manager).cancelTeleport(player.getUniqueId());
    }

    @Test void smallMovementsCancelButCameraRotationDoesNot() {
        when(manager.isTeleporting(player.getUniqueId())).thenReturn(true);
        World world = mock(World.class);
        listener.onPlayerMove(new PlayerMoveEvent(player, new Location(world, 0, 0, 0), new Location(world, 0, 0, 0, 90, 0)));
        verify(manager, never()).cancelTeleport(any());
        listener.onPlayerMove(new PlayerMoveEvent(player, new Location(world, 0, 0, 0), new Location(world, 0.02, 0, 0)));
        verify(manager).cancelTeleport(player.getUniqueId());
    }
}
