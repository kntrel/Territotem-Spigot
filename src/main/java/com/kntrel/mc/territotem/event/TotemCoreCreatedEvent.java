package com.kntrel.mc.territotem.event;

import com.kntrel.mc.territotem.totem.TotemCore;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

public class TotemCoreCreatedEvent extends Event implements Cancellable {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NonNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final TotemCore core_;
    private final Entity creator_;
    private boolean cancelled_;


    //CONSTRUCTOR
    public TotemCoreCreatedEvent(TotemCore core, Entity by) {
        this.core_ = core;
        this.creator_ = by;
        this.cancelled_ = false;
    }


    //GETTERS
    public TotemCore getCore() {
        return core_;
    }
    public Entity getCreator() {
        return creator_;
    }
    @Override public boolean isCancelled() {
        return cancelled_;
    }


    //SETTERS
    @Override public void setCancelled(boolean b) {
        this.cancelled_ = b;
    }
}
