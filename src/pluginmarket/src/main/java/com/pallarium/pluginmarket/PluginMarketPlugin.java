package com.pallarium.pluginmarket;

import com.pallarium.pluginmarket.gui.CategoryMenu;
import com.pallarium.pluginmarket.gui.DetailMenu;
import com.pallarium.pluginmarket.gui.ListingMenu;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginMarketPlugin extends JavaPlugin {
    private MarketService market;
    private CategoryMenu categoryMenu;
    private ListingMenu listingMenu;
    private DetailMenu detailMenu;
    private ChatInput chatInput;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        market = new MarketService(this);
        categoryMenu = new CategoryMenu(this);
        listingMenu = new ListingMenu(this);
        detailMenu = new DetailMenu(this);
        chatInput = new ChatInput(this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(chatInput, this);
        getLogger().info("PluginMarket ready. /pluginmarket opens the browser.");
    }

    public MarketService market() {
        return market;
    }

    public CategoryMenu categoryMenu() {
        return categoryMenu;
    }

    public ListingMenu listingMenu() {
        return listingMenu;
    }

    public DetailMenu detailMenu() {
        return detailMenu;
    }

    public ChatInput chatInput() {
        return chatInput;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("pluginmarket.use")) {
            p.sendMessage(ChatColor.RED + "You don't have permission to browse the plugin market.");
            return true;
        }
        categoryMenu.open(p);
        return true;
    }
}
