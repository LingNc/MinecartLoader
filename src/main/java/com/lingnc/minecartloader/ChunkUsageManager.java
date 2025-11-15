package com.lingnc.minecartloader;

import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.api.loaders.LoaderData;
import com.bgsoftware.wildloaders.api.managers.LoadersManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkUsageManager {

    private final JavaPlugin plugin;
    private final LoadersManager loadersManager;
    private final LoaderData minecartLoaderData;
    private final Map<ChunkKey, ChunkUsageRecord> activeChunks = new ConcurrentHashMap<>();
    private final long timeoutTicks;
    private final long staticThresholdTicks;
    private boolean enabled;

    public ChunkUsageManager(JavaPlugin plugin,
                             LoadersManager loadersManager,
                             LoaderData minecartLoaderData,
                             long timeoutTicks,
                             long staticThresholdTicks,
                             boolean enabled) {
        this.plugin = plugin;
        this.loadersManager = loadersManager;
        this.minecartLoaderData = minecartLoaderData;
        this.timeoutTicks = timeoutTicks;
        this.staticThresholdTicks = staticThresholdTicks;
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
                Location baseLocation = findBestLocationInChunk(chunk, minecart.getLocation(), record);
                Player owner = minecart.getPassengers().stream()
                        .filter(p -> p instanceof Player)
                        .map(p -> (Player) p)
                        .findFirst()
                        .orElse(null);
                if (owner == null && !world.getPlayers().isEmpty()) {
                    owner = world.getPlayers().get(0);
                }
                if (owner != null) {
                    // WildLoaders 的 timeLeft 单位为秒，因此将 tick 转换回秒用于显示和逻辑统一
                    long timeLeftSeconds = Math.max(1L, timeoutTicks / 20L + staticThresholdTicks / 20L);
                    ChunkLoader loader = loadersManager.addChunkLoader(minecartLoaderData, owner, baseLocation, timeLeftSeconds);
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
            restoreReplacedBlock(record);
            activeChunks.remove(key);
        } else if (shouldKeep) {
            record.setExpireAtTick(now + timeoutTicks);
            record.setScheduled(false);
            scheduleExpiryCheck(key, record);
        } else {
            activeChunks.remove(key);
        }
    }

    private Location findBestLocationInChunk(Chunk chunk, Location minecartLocation, ChunkUsageRecord record) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;

        Location bestAir = null;
        double bestAirDistanceSq = Double.MAX_VALUE;

        int worldMaxY = world.getMaxHeight();
        int worldMinY = world.getMinHeight();

        // 扫描整个 chunk，优先选择最近的安全空气方块
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                int startY = Math.min(worldMaxY - 1, (int) Math.max(worldMinY, minecartLocation.getBlockY() + 4));
                for (int y = startY; y >= worldMinY; y--) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.isEmpty() && block.getRelative(BlockFace.DOWN).getType().isSolid()) {
                        Location candidate = new Location(world, x + 0.5, y + 0.1, z + 0.5);
                        double distSq = candidate.distanceSquared(minecartLocation);
                        if (distSq < bestAirDistanceSq) {
                            bestAirDistanceSq = distSq;
                            bestAir = candidate;
                        }
                        break;
                    }
                }
            }
        }

        if (bestAir != null) {
            return bestAir;
        }

        // 若整个 chunk 内都没有合适空气方块，则尝试查找一个“可替换”的普通方块
        Location bestReplace = null;
        double bestReplaceDistanceSq = Double.MAX_VALUE;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                int startY = Math.min(worldMaxY - 1, minecartLocation.getBlockY() + 4);
                for (int y = startY; y >= worldMinY; y--) {
                    Block block = world.getBlockAt(x, y, z);
                    if (isReplaceableBlock(block)) {
                        Location candidate = new Location(world, x + 0.5, y + 0.1, z + 0.5);
                        double distSq = candidate.distanceSquared(minecartLocation);
                        if (distSq < bestReplaceDistanceSq) {
                            bestReplaceDistanceSq = distSq;
                            bestReplace = candidate;
                            record.setReplacedBlockLocation(block.getLocation());
                            record.setReplacedBlockData(block.getBlockData().clone());
                        }
                        break;
                    }
                }
            }
        }

        if (bestReplace != null) {
            return bestReplace;
        }

        // 仍然找不到时，退化为矿车当前位置附近
        return minecartLocation.clone().add(0, 0.5, 0);
    }

    private boolean isReplaceableBlock(Block block) {
        switch (block.getType()) {
            case RAIL:
            case POWERED_RAIL:
            case DETECTOR_RAIL:
            case ACTIVATOR_RAIL:
                return false;
            default:
                // 避免替换容器和红石组件，优先只替换普通方块
                String name = block.getType().name();
                if (name.contains("CHEST") || name.contains("SHULKER_BOX") || name.contains("FURNACE")
                        || name.contains("DROPPER") || name.contains("HOPPER") || name.contains("DISPENSER")
                        || name.contains("DOOR") || name.contains("BUTTON") || name.contains("PRESSURE_PLATE")
                        || name.contains("REDSTONE") || name.contains("TORCH") || name.contains("LEVER")) {
                    return false;
                }
                return !block.isEmpty();
        }
    }

    private void restoreReplacedBlock(ChunkUsageRecord record) {
        if (record.getReplacedBlockLocation() == null || record.getReplacedBlockData() == null) {
            return;
        }
        Block block = record.getReplacedBlockLocation().getBlock();
        block.setBlockData(record.getReplacedBlockData(), false);
        record.setReplacedBlockLocation(null);
        record.setReplacedBlockData(null);
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
