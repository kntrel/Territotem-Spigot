package com.kntrel.mc.territotem.geyser.render;

import com.kntrel.mc.territotem.geyser.block.BedrockTotemCore;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.event.TotemCoreBreakEvent;
import com.kntrel.mc.territotem.totem.event.TotemCoreCreatedEvent;
import com.kntrel.mc.territotem.totem.event.TotemCoreLoadedEvent;
import com.kntrel.mc.territotem.totem.event.TotemCoreUnloadedEvent;
import com.kntrel.mc.territotem.totem.event.TotemCoreUpdatedEvent;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.geysermc.geyser.api.block.custom.CustomBlockState;

public final class TotemCoreBedrockRenderListener implements Listener {

    //FIELDS
    private final Plugin plugin_;
    private final Server server_;
    private final BedrockBlockRenderer renderer_;
    private final BedrockTotemCore bedrockTotemCore_;


    //CONSTRUCTORS
    public TotemCoreBedrockRenderListener(Plugin plugin, BedrockBlockRenderer renderer, BedrockTotemCore bedrockTotemCore) {
        this.plugin_ = plugin;
        this.server_ = this.plugin_.getServer();
        this.renderer_ = renderer;
        this.bedrockTotemCore_ = bedrockTotemCore;
    }


    //LISTENERS
    @EventHandler
    public void onTotemCoreLoaded(TotemCoreLoadedEvent event) {
        this.render(event.getCore());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemCoreCreated(TotemCoreCreatedEvent event) {
        this.defered(() -> this.render(event.getCore()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemCoreBreak(TotemCoreBreakEvent event) {
        this.clear(event.getCore());
    }

    @EventHandler
    public void onTotemCoreUnloaded(TotemCoreUnloadedEvent event) {
        this.clear(event.getCore());
    }

    @EventHandler
    public void onTotemCoreUpdated(TotemCoreUpdatedEvent event) {
        this.defered(() -> this.render(event.getCore()));
    }


    //SERVICES
    public void render(TotemCore core) {
        if (core.getState() == TotemCore.State.EMPTY) {
            this.clear(core);
            return;
        }

        this.renderer_.render(core.getWorld(), core.getCoordinates(), this.blockState(core));
    }

    public void clear(TotemCore core) {
        this.renderer_.clear(core.getWorld(), core.getCoordinates());
    }


    //HELPERS
    private void defered(Runnable task) {
        task.run();
        this.server_.getScheduler().runTaskLater(this.plugin_, task, 4);
    }
    private CustomBlockState blockState(TotemCore core) {
        return this.bedrockTotemCore_.blockStateBuilder()
                .stringProperty(BedrockTotemCore.STATE_PROPERTY, state(core.getState()))
                .stringProperty(BedrockTotemCore.DIRECTION_PROPERTY, direction(core.getDirection()))
                .build();
    }

    private static String state(TotemCore.State state) {
        return switch (state) {
            case AMETHIST -> "amethyst";
            case END_EYE -> "eye";
            case FULL -> "complete";
            case ACTIVE -> "active";
            case INACTIVE -> "inactive";
            case EMPTY -> "amethyst";
        };
    }

    private static String direction(TotemCore.Direction direction) {
        return switch (direction) {
            case UP -> "up";
            case DOWN -> "down";
            case SOUTH -> "south";
            case NORTH -> "north";
            case EAST -> "east";
            case WEST -> "west";
            case ALL -> "all";
        };
    }
}
