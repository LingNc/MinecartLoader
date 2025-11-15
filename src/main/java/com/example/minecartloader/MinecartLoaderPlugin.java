package com.example.minecartloader;

import com.bgsoftware.wildloaders.api.WildLoaders;
import com.bgsoftware.wildloaders.api.WildLoadersAPI;
import com.bgsoftware.wildloaders.api.loaders.LoaderData;
import com.bgsoftware.wildloaders.api.managers.LoadersManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Minecart;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinecartLoaderPlugin extends JavaPlugin {

    private LoadersManager loadersManager;
    private LoaderData minecartLoaderData;
    private boolean enabled;
    private ChunkUsageManager chunkUsageManager;

    private boolean scanOnStartup;
    private long timeoutSeconds;
    private long staticThresholdSeconds;

    @Override
    public void onEnable() {
        WildLoaders wildLoaders = WildLoadersAPI.getWildLoaders();
        if (wildLoaders == null) {
            getLogger().severe("WildLoaders not found, disabling MinecartLoader");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.loadersManager = wildLoaders.getLoaders();

        initConfig();
        cleanupOldMinecartLoaders();
        initLoaderData();

        this.chunkUsageManager = new ChunkUsageManager(this, loadersManager, minecartLoaderData,
        timeoutSeconds * 20L,
        staticThresholdSeconds * 20L,
        enabled);

        getServer().getPluginManager().registerEvents(new MinecartListener(this, chunkUsageManager), this);
        getCommand("minecartloader").setExecutor(new MinecartLoaderCommand(this, chunkUsageManager));

        if (scanOnStartup) {
            Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.getWorlds().forEach(world -> {
                for (Minecart minecart : world.getEntitiesByClass(Minecart.class)) {
                    Chunk chunk = minecart.getLocation().getChunk();
                    chunkUsageManager.handleMinecartEnterChunk(minecart, chunk);
                }
            }), 20L);
        }
    }

    @Override
    public void onDisable() {
        if (chunkUsageManager != null) {
            chunkUsageManager.shutdown();
        }
    }

    private void initConfig() {
        saveDefaultConfig();
        reloadConfig();
        this.enabled = getConfig().getBoolean("minecart-loader-enabled", true);
        this.scanOnStartup = getConfig().getBoolean("scan-on-startup", true);
        this.timeoutSeconds = getConfig().getLong("timeout-seconds", 60L);
        this.staticThresholdSeconds = getConfig().getLong("static-threshold-seconds", 30L);
    }

    private void cleanupOldMinecartLoaders() {
        loadersManager.getLoaderDatas().stream()
                .filter(data -> "minecart-temporary".equalsIgnoreCase(data.getName()))
                .findFirst()
                .ifPresent(data -> loadersManager.getChunkLoaders().stream()
                        .filter(loader -> loader.getLoaderData().getName().equalsIgnoreCase(data.getName()))
                        .forEach(loader -> loadersManager.removeChunkLoader(loader)));
    }

    private void initLoaderData() {
        minecartLoaderData = loadersManager.getLoaderData("minecart-temporary").orElse(null);
        if (minecartLoaderData == null) {
            minecartLoaderData = loadersManager.createLoaderData(
                    "minecart-temporary",
                    timeoutSeconds,
                    new org.bukkit.inventory.ItemStack(
                            org.bukkit.Material.valueOf(getConfig().getString("display-item", "BARRIER")))
            );
        }
        minecartLoaderData.setChunksRadius(getConfig().getInt("chunk-radius", 0));
    }

    public boolean isMinecartLoaderEnabled() {
        return enabled;
    }

    public void setMinecartLoaderEnabled(boolean enabled) {
        this.enabled = enabled;
        if (chunkUsageManager != null) {
            chunkUsageManager.setEnabled(enabled);
        }
    }

    public ChunkUsageManager getChunkUsageManager() {
        return chunkUsageManager;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        this.enabled = getConfig().getBoolean("minecart-loader-enabled", true);
        this.scanOnStartup = getConfig().getBoolean("scan-on-startup", true);
        this.timeoutSeconds = getConfig().getLong("timeout-seconds", 60L);
        this.staticThresholdSeconds = getConfig().getLong("static-threshold-seconds", 30L);

        if (chunkUsageManager != null) {
            ChunkUsageManager newManager = new ChunkUsageManager(this, loadersManager, minecartLoaderData,
            timeoutSeconds * 20L,
            staticThresholdSeconds * 20L,
            enabled);
            this.chunkUsageManager.shutdown();
            this.chunkUsageManager = newManager;
        }
    }
}

