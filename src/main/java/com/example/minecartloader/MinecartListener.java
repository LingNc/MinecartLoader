package com.example.minecartloader;

import org.bukkit.Chunk;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.Plugin;

public final class MinecartListener implements Listener {

    private final Plugin plugin;
    private final ChunkUsageManager chunkUsageManager;

    public MinecartListener(Plugin plugin, ChunkUsageManager chunkUsageManager) {
        this.plugin = plugin;
        this.chunkUsageManager = chunkUsageManager;
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();
        if (!(vehicle instanceof Minecart)) {
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
}
