package com.example.minecartloader;

import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.api.loaders.LoaderData;
import com.bgsoftware.wildloaders.api.managers.LoadersManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkUsageManager {

    private final JavaPlugin plugin;
    private final LoadersManager loadersManager;
    private final LoaderData minecartLoaderData;
    private final Map<ChunkKey, ChunkUsageRecord> activeChunks = new ConcurrentHashMap<>();
    private final long timeoutTicks;
    private final long staticThresholdTicks;
    private final int chunkRadius;

    private boolean enabled;

    public ChunkUsageManager(JavaPlugin plugin,
                             LoadersManager loadersManager,
                             LoaderData minecartLoaderData,
                             long timeoutTicks,
                             long staticThresholdTicks,
                             int chunkRadius,
                             boolean enabled) {
        this.plugin = plugin;
        this.loadersManager = loadersManager;
        this.minecartLoaderData = minecartLoaderData;
        this.timeoutTicks = timeoutTicks;
        this.staticThresholdTicks = staticThresholdTicks;
        this.chunkRadius = chunkRadius;
        this.enabled = enabled;
    }

    public void handleMinecartEnterChunk(Minecart minecart, Chunk chunk) {
        if (!enabled) {
            return;
        }

        ChunkKey key = ChunkKey.fromChunk(chunk);
        long now = Bukkit.getCurrentTick();

        ChunkUsageRecord record = activeChunks.computeIfAbsent(key, k -> new ChunkUsageRecord());
        record.setMinecartCount(record.getMinecartCount() + 1);
        record.setExpireAtTick(now + timeoutTicks);
        record.setLastCrossTick(now);

        if (record.getLoader() == null) {
            Optional<ChunkLoader> existing = loadersManager.getChunkLoader(chunk);
            if (existing.isPresent()) {
                record.setLoader(existing.get());
                record.setOwnedByMinecartPlugin(false);
            } else {
                World world = chunk.getWorld();
                Location baseLocation = new Location(world, chunk.getX() << 4, minecart.getLocation().getY(), chunk.getZ() << 4);
                Player owner = minecart.getPassengers().stream()
                        .filter(p -> p instanceof Player)
                        .map(p -> (Player) p)
                        .findFirst()
                        .orElse(null);
                if (owner == null && !world.getPlayers().isEmpty()) {
                    owner = world.getPlayers().get(0);
                }
                if (owner != null) {
                    ChunkLoader loader = loadersManager.addChunkLoader(minecartLoaderData, owner, baseLocation, timeoutTicks);
                    record.setLoader(loader);
                    record.setOwnedByMinecartPlugin(true);
                }
            }
        }

        if (!record.isScheduled()) {
            scheduleExpiryCheck(key, record);
        }
    }

    public void handleMinecartLeaveChunk(Chunk chunk) {
        ChunkKey key = ChunkKey.fromChunk(chunk);
        ChunkUsageRecord record = activeChunks.get(key);
        if (record == null) {
            return;
        }
        int count = Math.max(0, record.getMinecartCount() - 1);
        record.setMinecartCount(count);
    }

    private void scheduleExpiryCheck(ChunkKey key, ChunkUsageRecord record) {
        record.setScheduled(true);
        long delay = timeoutTicks;
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkExpiry(key), delay);
    }

    private void checkExpiry(ChunkKey key) {
        ChunkUsageRecord record = activeChunks.get(key);
        if (record == null) {
            return;
        }

        long now = Bukkit.getCurrentTick();
        if (now < record.getExpireAtTick()) {
            record.setScheduled(false);
            scheduleExpiryCheck(key, record);
            return;
        }

        boolean shouldKeep = record.getMinecartCount() > 0;
        if (!shouldKeep && staticThresholdTicks > 0) {
            shouldKeep = now - record.getLastCrossTick() <= staticThresholdTicks;
        }

        ChunkLoader loader = record.getLoader();
        if (!shouldKeep && loader != null && record.isOwnedByMinecartPlugin()) {
            loader.remove();
            activeChunks.remove(key);
        } else if (shouldKeep) {
            record.setExpireAtTick(now + timeoutTicks);
            record.setScheduled(false);
            scheduleExpiryCheck(key, record);
        } else {
            activeChunks.remove(key);
        }
    }

    public void freezeAll() {
        Collection<ChunkUsageRecord> records = activeChunks.values();
        for (ChunkUsageRecord record : records) {
            if (record.isOwnedByMinecartPlugin() && record.getLoader() != null) {
                record.getLoader().remove();
            }
        }
        activeChunks.clear();
    }

    public void shutdown() {
        freezeAll();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getActiveChunkCount() {
        return activeChunks.size();
    }
}
