package com.kntrel.mc.territotem.test;

import com.kntrel.util.Vec3i;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class TestTrackingEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Vec3i origin;
    private final World world;
    private final Player player;
    private final boolean shouldTrack;

    public TestTrackingEvent(Vec3i origin, World world, Player player, boolean shouldTrack) {
        this.origin = origin;
        this.world = world;
        this.player = player;
        this.shouldTrack = shouldTrack;
    }

    public Vec3i origin() {
        return this.origin;
    }

    public World world() {
        return this.world;
    }

    public Player player() {
        return this.player;
    }

    public boolean shouldTrack() {
        return this.shouldTrack;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
