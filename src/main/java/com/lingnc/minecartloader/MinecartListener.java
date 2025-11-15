package com.lingnc.minecartloader;

import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.Plugin;

public final class MinecartListener implements Listener {

    private final Plugin plugin;
    private final ChunkUsageManager chunkUsageManager;
    private final ConfigurationSection minecartsSection;

    public MinecartListener(Plugin plugin, ChunkUsageManager chunkUsageManager) {
        this.plugin = plugin;
        this.chunkUsageManager = chunkUsageManager;
        this.minecartsSection = plugin.getConfig().getConfigurationSection("minecarts");
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();
        if (!(vehicle instanceof Minecart)) {
            return;
        }

        if (!isMinecartTypeEnabled((Minecart) vehicle)) {
            return;
        }

        Chunk from = event.getFrom().getChunk();
        Chunk to = event.getTo().getChunk();
        if (from.equals(to)) {
            return;
        }

        chunkUsageManager.handleMinecartEnterChunk((Minecart) vehicle, to);
        chunkUsageManager.handleMinecartLeaveChunk(from);
    }

    private boolean isMinecartTypeEnabled(Minecart minecart) {
        if (minecartsSection == null) {
            return true;
        }

        EntityType type = minecart.getType();

        if (type == EntityType.MINECART) {
            return minecartsSection.getBoolean("normal", true);
        }
        if (type == EntityType.CHEST_MINECART) {
            return minecartsSection.getBoolean("chest", true);
        }
        if (type == EntityType.FURNACE_MINECART) {
            return minecartsSection.getBoolean("furnace", true);
        }
        if (type == EntityType.TNT_MINECART) {
            return minecartsSection.getBoolean("tnt", true);
        }
        if (type == EntityType.HOPPER_MINECART) {
            return minecartsSection.getBoolean("hopper", true);
        }
        if (type == EntityType.SPAWNER_MINECART) {
            return minecartsSection.getBoolean("spawner", true);
        }
        if (type == EntityType.COMMAND_BLOCK_MINECART) {
            return minecartsSection.getBoolean("command-block", true);
        }

        // 其他未来可能出现的矿车类型，默认允许
        return true;
    }
}
