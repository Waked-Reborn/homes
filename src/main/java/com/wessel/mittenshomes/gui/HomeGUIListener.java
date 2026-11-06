package com.wessel.mittenshomes.gui;

import com.wessel.mittenshomes.Homes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import java.util.Objects;
import java.util.logging.Level;

public class HomeGUIListener implements Listener {
    private final Homes plugin;
    public HomeGUIListener(Homes plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof HomeGUI gui)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !player.getUniqueId().equals(gui.getOwner())) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        String name = gui.getHomeName(event.getRawSlot());
        if (name == null || (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT)) return;
        boolean delete = event.getClick() == ClickType.RIGHT;
        // Inventory open/close must happen after the click transaction has completed.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != gui.getInventory()) return;
            var manager = plugin.getHomeManager();
            boolean exists = manager.hasHome(player.getUniqueId(), name);
            String permission = delete ? "delhome" : exists ? "home" : "sethome";
            if (!player.hasPermission("mittenshomes." + permission)) {
                player.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                return;
            }
            try {
                if (delete) {
                    if (!exists) return;
                    manager.removeHome(player.getUniqueId(), name);
                    player.sendMessage(ChatColor.GREEN + "Home " + name + " has been deleted.");
                } else if (exists) {
                    manager.startTeleport(player, manager.getHome(player.getUniqueId(), name));
                    if (manager.isTeleporting(player.getUniqueId())) player.closeInventory();
                    return;
                } else {
                    manager.setHome(player.getUniqueId(), name, player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Home " + name + " has been saved.");
                }
                new HomeGUI(plugin).openHomeGUI(player);
            } catch (IllegalArgumentException ex) {
                player.sendMessage(ChatColor.RED + ex.getMessage());
            } catch (IllegalStateException ex) {
                plugin.getLogger().log(Level.SEVERE, "Could not update homes", ex);
                player.sendMessage(ChatColor.RED + "Could not save your homes. Please contact an administrator.");
            }
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof HomeGUI)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) { cancelOnMovement(event); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) { cancelOnMovement(event); }

    private void cancelOnMovement(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to != null && plugin.getHomeManager().isTeleporting(player.getUniqueId())
                && (!Objects.equals(from.getWorld(), to.getWorld()) || from.distanceSquared(to) > 0.000001)) {
            plugin.getHomeManager().cancelTeleport(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "Teleport cancelled! You moved.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { plugin.getHomeManager().disconnect(event.getPlayer().getUniqueId()); }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) { plugin.getHomeManager().cancelTeleport(event.getEntity().getUniqueId()); }
}
