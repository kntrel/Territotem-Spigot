package com.kntrel.mc.territotem.totem.event;

import com.kntrel.mc.territotem.totem.core.TotemCore;
import org.bukkit.Chunk;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

public class TotemCoreLoadedEvent extends TotemCoreEvent {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NonNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final Chunk chunk_;


    //CONSTRUCTOR
    public TotemCoreLoadedEvent(TotemCore core, Chunk chunk) {
        super(core);
        this.chunk_ = chunk;
    }


    //GETTERS
    public Chunk getChunk() {
        return this.chunk_;
    }
}
