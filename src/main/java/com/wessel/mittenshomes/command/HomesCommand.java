package com.wessel.mittenshomes.command;

import com.wessel.mittenshomes.Homes;
import com.wessel.mittenshomes.gui.HomeGUI;
import org.bukkit.entity.Player;

public class HomesCommand extends PlayerCommand {
    public HomesCommand(Homes plugin) { super(plugin); }
    @Override
    protected void execute(Player player, String[] args) {
        new HomeGUI(plugin).openHomeGUI(player);
    }
}
