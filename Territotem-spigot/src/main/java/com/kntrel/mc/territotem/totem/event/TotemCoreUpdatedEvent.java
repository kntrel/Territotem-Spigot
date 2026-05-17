package com.kntrel.mc.territotem.totem.event;

import com.kntrel.mc.territotem.totem.core.TotemCore;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

public class TotemCoreUpdatedEvent extends TotemCoreEvent {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NonNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final TotemCore.State oldState_;
    private final TotemCore.State newState_;
    private final TotemCore.Direction oldDirection_;
    private final TotemCore.Direction newDirection_;


    //CONSTRUCTOR
    public TotemCoreUpdatedEvent(
            TotemCore core,
            TotemCore.State oldState,
            TotemCore.State newState,
            TotemCore.Direction oldDirection,
            TotemCore.Direction newDirection
    ) {
        super(core);
        this.oldState_ = oldState;
        this.newState_ = newState;
        this.oldDirection_ = oldDirection;
        this.newDirection_ = newDirection;
    }


    //GETTERS
    public TotemCore.State getOldState() {
        return this.oldState_;
    }
    public TotemCore.State getNewState() {
        return this.newState_;
    }
    public TotemCore.Direction getOldDirection() {
        return this.oldDirection_;
    }
    public TotemCore.Direction getNewDirection() {
        return this.newDirection_;
    }
}
