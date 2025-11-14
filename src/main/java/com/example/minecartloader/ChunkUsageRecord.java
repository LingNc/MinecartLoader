package com.example.minecartloader;

import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;

public final class ChunkUsageRecord {

    private int minecartCount;
    private ChunkLoader loader;
    private boolean ownedByMinecartPlugin;
    private long expireAtTick;
    private long lastCrossTick;
    private boolean scheduled;

    public int getMinecartCount() {
        return minecartCount;
    }

    public void setMinecartCount(int minecartCount) {
        this.minecartCount = minecartCount;
    }

    public ChunkLoader getLoader() {
        return loader;
    }

    public void setLoader(ChunkLoader loader) {
        this.loader = loader;
    }

    public boolean isOwnedByMinecartPlugin() {
        return ownedByMinecartPlugin;
    }

    public void setOwnedByMinecartPlugin(boolean ownedByMinecartPlugin) {
        this.ownedByMinecartPlugin = ownedByMinecartPlugin;
    }

    public long getExpireAtTick() {
        return expireAtTick;
    }

    public void setExpireAtTick(long expireAtTick) {
        this.expireAtTick = expireAtTick;
    }

    public long getLastCrossTick() {
        return lastCrossTick;
    }

    public void setLastCrossTick(long lastCrossTick) {
        this.lastCrossTick = lastCrossTick;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }
}
