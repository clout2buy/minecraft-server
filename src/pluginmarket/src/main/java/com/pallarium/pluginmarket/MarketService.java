package com.pallarium.pluginmarket;

import com.pallarium.pluginmarket.model.Category;
import com.pallarium.pluginmarket.model.PluginInfo;
import com.pallarium.pluginmarket.model.SortMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/** Runs Spiget calls off the main thread and hops back for anything that touches Bukkit state. */
public class MarketService {
    private final PluginMarketPlugin plugin;
    private final SpigetClient client;

    public MarketService(PluginMarketPlugin plugin) {
        this.plugin = plugin;
        this.client = new SpigetClient(plugin.getConfig().getString("user-agent", "PluginMarket/1.0.0"));
    }

    public void search(Category category, SortMode sort, String query, int page, Consumer<List<PluginInfo>> onDone, Consumer<Exception> onError) {
        int size = plugin.getConfig().getInt("page-size", 28);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<PluginInfo> results = client.browse(category, sort, query, page, size);
                Bukkit.getScheduler().runTask(plugin, () -> onDone.accept(results));
            } catch (Exception ex) {
                Bukkit.getScheduler().runTask(plugin, () -> onError.accept(ex));
            }
        });
    }

    public void download(Player installer, PluginInfo info, Consumer<File> onDone, Consumer<Exception> onError) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (info.externalDownload) {
                    Bukkit.getScheduler().runTask(plugin, () -> onError.accept(new IOException("This plugin hosts its jar off SpigotMC - open its page and download manually.")));
                    return;
                }
                byte[] data = client.download(client.resolveDownloadUrl(info.id));
                if (data.length < 500) throw new IOException("Download too small (" + data.length + " bytes) - likely blocked or premium-gated.");
                String safe = info.name.replaceAll("[^a-zA-Z0-9._-]", "_");
                File dir = new File(plugin.getDataFolder().getParentFile(), plugin.getConfig().getString("install-dir", "plugins"));
                dir.mkdirs();
                File out = new File(dir, safe + ".jar");
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(data);
                }
                Bukkit.getScheduler().runTask(plugin, () -> onDone.accept(out));
            } catch (Exception ex) {
                Bukkit.getScheduler().runTask(plugin, () -> onError.accept(ex));
            }
        });
    }
}
