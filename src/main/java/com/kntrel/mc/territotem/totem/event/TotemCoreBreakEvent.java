package com.kntrel.mc.territotem.totem.event;

import com.kntrel.mc.territotem.totem.core.TotemCore;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

public class TotemCoreBreakEvent extends TotemCoreEvent implements Cancellable {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NonNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final Player player_;
    private boolean cancel_;


    //CONSTRUCTOR
    public TotemCoreBreakEvent(TotemCore core, Player player) {
        super(core);
        this.player_ = player;
        this.cancel_ = false;
    }


    //GETTERS
    public Player getPlayer() {
        return this.player_;
    }
    @Override public boolean isCancelled() {
        return this.cancel_;
    }


    //SETTERS
    @Override public void setCancelled(boolean b) {
        this.cancel_ = b;
    }
}
