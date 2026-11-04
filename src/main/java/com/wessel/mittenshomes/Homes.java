package com.wessel.mittenshomes;
import com.wessel.mittenshomes.manager.HomeManager;
import org.bukkit.plugin.java.JavaPlugin;
public class Homes extends JavaPlugin {
    private static Homes instance;
    private HomeManager homeManager;
    @Override
    public void onEnable() {
        instance = this;
        try {
            saveDefaultConfig();
            homeManager = new HomeManager(this);
            registerCommand("home", new com.wessel.mittenshomes.command.HomeCommand(this));
            registerCommand("sethome", new com.wessel.mittenshomes.command.SethomeCommand(this));
            registerCommand("delhome", new com.wessel.mittenshomes.command.DelhomeCommand(this));
            registerCommand("homes", new com.wessel.mittenshomes.command.HomesCommand(this));
            getServer().getPluginManager().registerEvents(new com.wessel.mittenshomes.gui.HomeGUIListener(this), this);
            getLogger().info("homes has been enabled!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable homes plugin:");
            e.printStackTrace();
        }
    }
    @Override
    public void onDisable() {
        getLogger().info("homes has been disabled!");
    }
    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        org.bukkit.command.Command cmd = getCommand(name);
        if (cmd == null) {
            getLogger().warning("Command '" + name + "' not found in plugin.yml!");
            return;
        }
        if (cmd instanceof org.bukkit.command.PluginCommand) {
            ((org.bukkit.command.PluginCommand) cmd).setExecutor(executor);
        } else {
            getLogger().warning("Command '" + name + "' is not a PluginCommand!");
        }
    }
    public static Homes getInstance() {
        return instance;
    }
    public HomeManager getHomeManager() {
        return homeManager;
    }
}
