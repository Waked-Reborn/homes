package com.wessel.mittenshomes;

import com.wessel.mittenshomes.command.*;
import com.wessel.mittenshomes.gui.HomeGUIListener;
import com.wessel.mittenshomes.manager.HomeManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public class Homes extends JavaPlugin {
    private static Homes instance;
    private HomeManager homeManager;

    @Override
    public void onEnable() {
        instance = this;
        try {
            saveDefaultConfig();
            // Bukkit's default loader logs malformed YAML and returns an empty config.
            // Fail closed instead, before an edit could overwrite the original homes.
            new org.bukkit.configuration.file.YamlConfiguration().load(
                    new java.io.File(getDataFolder(), "config.yml"));
            homeManager = new HomeManager(this);
            registerCommand("home", new HomeCommand(this));
            registerCommand("sethome", new SethomeCommand(this));
            registerCommand("delhome", new DelhomeCommand(this));
            registerCommand("homes", new HomesCommand(this));
            getServer().getPluginManager().registerEvents(new HomeGUIListener(this), this);
            getLogger().info("homes has been enabled!");
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to enable homes plugin", ex);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (homeManager != null) homeManager.shutdown();
        instance = null;
    }

    private void registerCommand(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) throw new IllegalStateException("Command '" + name + "' missing from plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public static Homes getInstance() { return instance; }
    public HomeManager getHomeManager() { return homeManager; }
}
