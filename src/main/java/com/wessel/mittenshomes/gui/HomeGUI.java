package com.wessel.mittenshomes.gui;
import com.wessel.mittenshomes.Homes;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
public class HomeGUI {
    private final Homes plugin;
    private static final int[] BED_SLOTS = {12, 13, 14};
    private static final Material[] BED_MATERIALS = {
        Material.RED_BED,
        Material.WHITE_BED,
        Material.BLUE_BED
    };
    public HomeGUI(Homes plugin) {
        this.plugin = plugin;
    }
    public void openHomeGUI(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory gui = org.bukkit.Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&8&l» &fHome Slots &8«"));
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, createGlassPane());
        }
        for (int i = 0; i < BED_SLOTS.length; i++) {
            String homeName = "Home " + (i + 1);
            gui.setItem(BED_SLOTS[i], createBedItem(player, uuid, homeName, BED_MATERIALS[i]));
        }
        player.openInventory(gui);
    }
    private ItemStack createBedItem(Player player, UUID uuid, String homeName, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean hasHome = plugin.getHomeManager().hasHome(uuid, homeName);
            if (hasHome) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&l" + homeName));
                List<String> lore = new ArrayList<>();
                org.bukkit.Location loc = plugin.getHomeManager().getHome(uuid, homeName);
                if (loc != null) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7X: &f" + (int) loc.getX()));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Y: &f" + (int) loc.getY()));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Z: &f" + (int) loc.getZ()));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7World: &f" + loc.getWorld().getName()));
                    lore.add("");
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Left-click to teleport"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Right-click to delete"));
                }
                meta.setLore(lore);
            } else {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&8&l" + homeName));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Not set yet"));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Left-click to set this home"));
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    private ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', " "));
        item.setItemMeta(meta);
        return item;
    }
    public static int getSlotForHome(int homeIndex) {
        if (homeIndex >= 0 && homeIndex < BED_SLOTS.length) {
            return BED_SLOTS[homeIndex];
        }
        return -1;
    }
    public static String getHomeNameForSlot(int slot) {
        for (int i = 0; i < BED_SLOTS.length; i++) {
            if (BED_SLOTS[i] == slot) {
                return "Home " + (i + 1);
            }
        }
        return null;
    }
}
