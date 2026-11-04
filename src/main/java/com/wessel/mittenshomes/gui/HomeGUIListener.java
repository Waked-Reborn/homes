package com.wessel.mittenshomes.gui;
import com.wessel.mittenshomes.Homes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
public class HomeGUIListener implements Listener {
    private final Homes plugin;
    public HomeGUIListener(Homes plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!title.contains("Home Slots")) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        int slot = event.getSlot();
        String homeName = HomeGUI.getHomeNameForSlot(slot);
        if (homeName == null) {
            return;
        }
        if (event.isLeftClick()) {
            if (plugin.getHomeManager().hasHome(player.getUniqueId(), homeName)) {
                if (plugin.getHomeManager().isTeleporting(player.getUniqueId())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou are already teleporting!"));
                    return;
                }
                if (plugin.getHomeManager().isOnCooldown(player.getUniqueId())) {
                    long remaining = plugin.getHomeManager().getCooldownRemaining(player.getUniqueId());
                    long seconds = (remaining + 999) / 1000;
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPlease wait &f" + seconds + "&c second(s) before teleporting again."));
                    return;
                }
                org.bukkit.Location loc = plugin.getHomeManager().getHome(player.getUniqueId(), homeName);
                if (loc != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eTeleporting in 5 seconds... (don't move)"));
                    plugin.getHomeManager().startTeleport(player, loc);
                    player.closeInventory();
                }
            } else {
                plugin.getHomeManager().setHome(player.getUniqueId(), homeName, player.getLocation());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + homeName + "&a has been set at your location."));
                new HomeGUI(plugin).openHomeGUI(player);
            }
        } else if (event.isRightClick()) {
            if (plugin.getHomeManager().hasHome(player.getUniqueId(), homeName)) {
                plugin.getHomeManager().cancelTeleport(player.getUniqueId());
                plugin.getHomeManager().removeHome(player.getUniqueId(), homeName);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c" + homeName + "&c has been deleted."));
                new HomeGUI(plugin).openHomeGUI(player);
            }
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.getHomeManager().isTeleporting(player.getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.distance(to) > 0.1) {
                plugin.getHomeManager().cancelTeleport(player.getUniqueId());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cTeleport cancelled! You moved."));
            }
        }
    }
}
