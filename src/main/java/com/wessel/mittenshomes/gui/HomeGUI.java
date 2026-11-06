package com.wessel.mittenshomes.gui;

import com.wessel.mittenshomes.Homes;
import com.wessel.mittenshomes.manager.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class HomeGUI implements InventoryHolder {
    private final Homes plugin;
    private final Map<Integer, String> slots = new HashMap<>();
    private Inventory inventory;
    private UUID owner;
    private static final int[] BED_SLOTS = {12, 13, 14};
    private static final Material[] BEDS = {Material.RED_BED, Material.WHITE_BED, Material.BLUE_BED};

    public HomeGUI(Homes plugin) { this.plugin = plugin; }
    @Override
    public Inventory getInventory() { return inventory; }
    public UUID getOwner() { return owner; }
    public String getHomeName(int slot) { return slots.get(slot); }

    public void openHomeGUI(Player player) {
        owner = player.getUniqueId();
        slots.clear();
        Map<String, Location> homes = plugin.getHomeManager().getPlayerHomes(owner);
        List<String> names = new ArrayList<>(homes.keySet());
        // Include legacy excess homes so players can delete them and recover their slots.
        int size = names.size() > HomeManager.MAX_HOMES ? 54 : 27;
        inventory = Bukkit.createInventory(this, size, ChatColor.DARK_GRAY + "Home Slots");
        for (int i = 0; i < size; i++) inventory.setItem(i, item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
        for (int i = 1; names.size() < HomeManager.MAX_HOMES; i++) {
            String candidate = "Home " + i;
            if (!names.contains(candidate)) names.add(candidate);
        }
        for (int i = 0; i < Math.min(names.size(), size); i++) {
            String name = names.get(i);
            int slot = size == 27 ? BED_SLOTS[i] : i;
            slots.put(slot, name);
            Location location = homes.get(name);
            List<String> lore = new ArrayList<>();
            boolean exists = homes.containsKey(name);
            if (exists) {
                if (location == null) {
                    lore.add(ChatColor.RED + "World unavailable");
                } else {
                    lore.add(ChatColor.GRAY + "X: " + location.getBlockX() + " Y: " + location.getBlockY() + " Z: " + location.getBlockZ());
                    lore.add(ChatColor.GRAY + "World: " + location.getWorld().getName());
                }
                lore.add(ChatColor.GRAY + "Left-click to teleport");
                lore.add(ChatColor.GRAY + "Right-click to delete");
            } else {
                lore.add(ChatColor.GRAY + "Left-click to set this home");
            }
            inventory.setItem(slot, item(BEDS[i % BEDS.length], (exists ? ChatColor.GREEN : ChatColor.GRAY) + name, lore));
        }
        player.openInventory(inventory);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
