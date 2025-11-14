package com.example.minecartloader;

import org.bukkit.Chunk;

import java.util.Objects;
import java.util.UUID;

public final class ChunkKey {

    private final UUID worldId;
    private final int x;
    private final int z;

    public ChunkKey(UUID worldId, int x, int z) {
        this.worldId = worldId;
        this.x = x;
        this.z = z;
    }

    public static ChunkKey fromChunk(Chunk chunk) {
        return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    public UUID getWorldId() {
        return worldId;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkKey chunkKey = (ChunkKey) o;
        return x == chunkKey.x && z == chunkKey.z && worldId.equals(chunkKey.worldId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldId, x, z);
    }
}
