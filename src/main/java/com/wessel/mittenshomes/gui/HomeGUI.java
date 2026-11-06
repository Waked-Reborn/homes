package com.wessel.mittenshomes.gui;

import com.wessel.mittenshomes.Homes;
import com.wessel.mittenshomes.command.HomeAdminCommand;
import com.wessel.mittenshomes.manager.HomeManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;
import java.util.function.BiConsumer;

public class HomeGUI implements InventoryHolder {
    private static final int[] GRID = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int[] FEATURED = {20, 22, 24};
    private static final Material[] BEDS = {Material.CYAN_BED, Material.LIGHT_BLUE_BED, Material.BLUE_BED};
    private final Homes plugin;
    private final Map<Integer, String> slots = new HashMap<>();
    private final Map<Integer, BiConsumer<Player, ClickType>> actions = new HashMap<>();
    private Inventory inventory;
    private UUID owner;

    public HomeGUI(Homes plugin) { this.plugin = plugin; }
    @Override public Inventory getInventory() { return inventory; }
    public UUID getOwner() { return owner; }
    public String getHomeName(int slot) { return slots.get(slot); }

    public void handleClick(Player player, int slot, ClickType click) {
        if (!player.getUniqueId().equals(owner)) return;
        var action = actions.get(slot);
        if (action != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.35f, 1.2f);
            action.accept(player, click);
        }
    }

    public void openHomeGUI(Player player) { openHomes(player, player.getUniqueId(), false, 0, 0); }

    public void openAdminGUI(Player player) { openPlayers(player, 0); }

    private void frame(Player player, int size, String title) {
        owner = player.getUniqueId();
        slots.clear();
        actions.clear();
        inventory = Bukkit.createInventory(this, size, text("&8" + title));
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            inventory.setItem(i, item(edge ? Material.BLACK_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
        }
        for (int slot : new int[]{0, 8, size - 9, size - 1})
            inventory.setItem(slot, item(Material.CYAN_STAINED_GLASS_PANE, " ", List.of()));
        button(size - 5, Material.BARRIER, "&cClose", List.of("&7See you around."), (p, click) -> p.closeInventory());
    }

    private void openHomes(Player viewer, UUID target, boolean admin, int requestedPage, int playersPage) {
        if (admin && !require(viewer, "mittenshomes.admin.view")) return;
        var manager = plugin.getHomeManager();
        var homes = manager.getPlayerHomes(target);
        List<String> names = new ArrayList<>(homes.keySet());
        if (!admin) {
            for (int i = 1; names.size() < HomeManager.MAX_HOMES; i++) {
                String name = "Home " + i;
                if (!names.contains(name)) names.add(name);
            }
        }
        int pages = Math.max(1, (names.size() + GRID.length - 1) / GRID.length);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        String targetName = admin ? playerName(target) : "Your places";
        frame(viewer, 54, admin ? "WAKED / " + shortName(targetName) : "WAKED / Homes");
        inventory.setItem(4, item(admin ? Material.SPYGLASS : Material.COMPASS,
                "&b&l" + targetName, List.of("&8HOME COLLECTION", "", "&f" + homes.size() + (admin ? " &7saved homes" : " &7/ &f3 &7homes saved"),
                "&7Page &f" + (page + 1) + " &7of &f" + pages)));
        inventory.setItem(40, item(Material.BOOK, "&fA place to call home", List.of("&7Left-click &f• &7Travel or save", "&7Right-click &f• &7Review deletion",
                admin ? "&bAdmin travel is instant." : "&bStand still for 5 seconds to travel.")));
        if (names.isEmpty()) inventory.setItem(22, item(Material.GLASS_BOTTLE, "&7Nothing here yet", List.of("&7This player has no saved homes.")));
        for (int index = page * GRID.length; index < Math.min(names.size(), (page + 1) * GRID.length); index++) {
            String name = names.get(index);
            boolean exists = homes.containsKey(name);
            Location location = homes.get(name);
            int slot = names.size() <= 3 ? FEATURED[index] : GRID[index % GRID.length];
            slots.put(slot, name);
            List<String> lore = new ArrayList<>();
            lore.add(exists ? (location == null ? "&cWORLD OFFLINE" : "&3SAVED DESTINATION") : "&8EMPTY SLOT");
            lore.add("");
            if (!exists) {
                lore.add("&7Your next adventure starts here.");
                lore.add("&7Save where you're standing.");
            } else if (location == null) {
                lore.add("&7This world isn't loaded right now.");
                lore.add("&7Your home is still saved.");
            } else {
                lore.add("&7World  &f" + location.getWorld().getName());
                lore.add("&7XYZ    &f" + location.getBlockX() + " &8/ &f" + location.getBlockY() + " &8/ &f" + location.getBlockZ());
            }
            lore.add("");
            String travelPermission = admin ? "mittenshomes.admin.teleport" : "mittenshomes.home";
            lore.add(!exists ? (viewer.hasPermission("mittenshomes.sethome") ? "&aLeft-click to save here" : "&8Saving is locked")
                    : viewer.hasPermission(travelPermission) ? "&bLeft-click to travel" : "&8Travel is locked");
            if (exists) lore.add(viewer.hasPermission(admin ? "mittenshomes.admin.delete" : "mittenshomes.delhome")
                    ? "&7Right-click to delete..." : "&8Deletion is locked");
            button(slot, !exists ? Material.ENDER_EYE : location == null ? Material.GRAY_BED : BEDS[index % BEDS.length],
                    (exists ? "&f&l" : "&7") + name, lore, (p, click) -> {
                        if (admin && !require(p, "mittenshomes.admin.view")) return;
                        if (click == ClickType.RIGHT) {
                            if (!manager.hasHome(target, name)) return;
                            if (!require(p, admin ? "mittenshomes.admin.delete" : "mittenshomes.delhome")) return;
                            new HomeGUI(plugin).confirmDelete(p, target, name, admin, page, playersPage);
                        } else if (!exists) {
                            if (!require(p, "mittenshomes.sethome")) return;
                            if (manager.hasHome(target, name)) {
                                p.sendMessage(text("&eThat slot changed. Take another look."));
                            } else {
                                manager.setHome(target, name, p.getLocation());
                                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.3f);
                                p.sendMessage(text("&aHome saved. Make yourself at home."));
                            }
                            new HomeGUI(plugin).openHomes(p, target, admin, page, playersPage);
                        } else if (admin) {
                            if (!require(p, "mittenshomes.admin.teleport")) return;
                            adminAction(p, "tp", target, name);
                        } else {
                            manager.startTeleport(p, manager.getHome(target, name));
                            if (manager.isTeleporting(p.getUniqueId())) p.closeInventory();
                        }
                    });
        }
        if (page > 0) button(48, Material.ARROW, "&bPrevious page", List.of(), (p, c) -> new HomeGUI(plugin).openHomes(p, target, admin, page - 1, playersPage));
        if (page + 1 < pages) button(50, Material.ARROW, "&bNext page", List.of(), (p, c) -> new HomeGUI(plugin).openHomes(p, target, admin, page + 1, playersPage));
        if (admin) button(45, Material.ARROW, "&bBack to players", List.of(), (p, c) -> new HomeGUI(plugin).openPlayers(p, playersPage));
        else if (viewer.hasPermission("mittenshomes.admin.view")) button(53, Material.SPYGLASS, "&bAdmin dashboard", List.of("&7Browse players and their homes."), (p, c) -> new HomeGUI(plugin).openAdminGUI(p));
        viewer.openInventory(inventory);
    }

    private void openPlayers(Player viewer, int requestedPage) {
        if (!require(viewer, "mittenshomes.admin.view")) return;
        var manager = plugin.getHomeManager();
        List<UUID> players = manager.getHomeOwners().stream().sorted().toList();
        int pages = Math.max(1, (players.size() + GRID.length - 1) / GRID.length);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        frame(viewer, 54, "WAKED / Admin");
        inventory.setItem(4, item(Material.SPYGLASS, "&b&lHome control", List.of("&8ADMIN DASHBOARD", "", "&f" + players.size() + " &7players with homes", "&7Page &f" + (page + 1) + " &7of &f" + pages)));
        if (players.isEmpty()) inventory.setItem(22, item(Material.GLASS_BOTTLE, "&7All quiet here", List.of("&7No homes have been saved yet.")));
        for (int i = page * GRID.length; i < Math.min(players.size(), (page + 1) * GRID.length); i++) {
            UUID target = players.get(i);
            boolean online = plugin.getServer().getPlayer(target) != null;
            button(GRID[i % GRID.length], online ? Material.LIME_DYE : Material.LIGHT_GRAY_DYE, "&f&l" + playerName(target),
                    List.of(online ? "&aONLINE" : "&8OFFLINE", "", "&7Homes  &f" + manager.getHomeCount(target), "&8" + target, "", "&bClick to browse homes"),
                    (p, click) -> new HomeGUI(plugin).openHomes(p, target, true, 0, page));
        }
        inventory.setItem(40, item(Material.BOOK, "&fYou're in control", List.of("&7Pick a player to inspect their homes.", "&7Offline players are included.")));
        if (page > 0) button(48, Material.ARROW, "&bPrevious page", List.of(), (p, c) -> new HomeGUI(plugin).openPlayers(p, page - 1));
        if (page + 1 < pages) button(50, Material.ARROW, "&bNext page", List.of(), (p, c) -> new HomeGUI(plugin).openPlayers(p, page + 1));
        button(45, Material.COMPASS, "&bYour homes", List.of(), (p, c) -> new HomeGUI(plugin).openHomeGUI(p));
        viewer.openInventory(inventory);
    }

    private void confirmDelete(Player viewer, UUID target, String name, boolean admin, int page, int playersPage) {
        String snapshot = plugin.getHomeManager().getHomeSnapshot(target, name);
        frame(viewer, 27, "WAKED / Delete home?");
        inventory.setItem(4, item(Material.PAPER, "&fOne last check", List.of("&7This can't be undone.")));
        inventory.setItem(13, item(Material.RED_BED, "&f&l" + name, List.of("&7Owner  &f" + playerName(target), "", "&7Only this home will be removed.")));
        button(11, Material.LIME_CONCRETE, "&a&lKeep home", List.of("&7Go back. Nothing changes."), (p, c) -> new HomeGUI(plugin).openHomes(p, target, admin, page, playersPage));
        button(15, Material.RED_CONCRETE, "&c&lDelete home", List.of("&7Remove &f" + name + "&7 permanently.", "", "&cClick to confirm"), (p, c) -> {
            if (admin && !require(p, "mittenshomes.admin.view")) return;
            if (!require(p, admin ? "mittenshomes.admin.delete" : "mittenshomes.delhome")) return;
            var manager = plugin.getHomeManager();
            if (snapshot == null || !Objects.equals(snapshot, manager.getHomeSnapshot(target, name))) {
                p.sendMessage(text("&eThat home changed. Review it before deleting."));
            } else if (admin) {
                adminAction(p, "delete", target, name);
            } else {
                manager.removeHome(target, name);
                p.sendMessage(text("&aHome deleted. Room for something new."));
            }
            new HomeGUI(plugin).openHomes(p, target, admin, page, playersPage);
        });
        viewer.openInventory(inventory);
    }

    private void adminAction(Player player, String action, UUID target, String name) {
        new HomeAdminCommand(plugin).onCommand(player, plugin.getCommand("homeadmin"), "homeadmin", new String[]{action, target.toString(), name});
    }

    private boolean require(Player player, String permission) {
        if (player.hasPermission(permission)) return true;
        player.sendMessage(text("&cYou don't have access to that action."));
        return false;
    }

    private String playerName(UUID uuid) {
        String name = plugin.getServer().getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString() : name;
    }

    private String shortName(String name) { return name.length() > 20 ? name.substring(0, 17) + "..." : name; }

    private void button(int slot, Material material, String name, List<String> lore, BiConsumer<Player, ClickType> action) {
        inventory.setItem(slot, item(material, name, lore));
        actions.put(slot, action);
    }

    private static String text(String value) { return ChatColor.translateAlternateColorCodes('&', value); }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(text(name));
            meta.setLore(lore.stream().map(HomeGUI::text).toList());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}
