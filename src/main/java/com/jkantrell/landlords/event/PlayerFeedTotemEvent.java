package com.jkantrell.landlords.event;

import com.jkantrell.landlords.totem.Totem;
import com.jkantrell.regionslib.regions.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class PlayerFeedTotemEvent extends PlayerEvent implements Cancellable {
    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
    @Override
    @Nonnull
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    //===============================================================

    //FIELDS
    private final Totem totem_;
    private final BoundingBox oldArea_;
    private boolean cancelled_ = false;

    //CONSTRUCTORS
    public PlayerFeedTotemEvent(@NotNull Player who, Totem totem, BoundingBox oldArea) {
        super(who);
        this.totem_ = totem;
        this.oldArea_ = oldArea;
    }

    //GETTERS
    public Totem getTotem(){
        return this.totem_;
    }
    public Region getRegion() {
        return this.totem_.getRegion().orElse(null);
    }
    public BoundingBox getNewArea() {
        Region reg = this.getRegion();
        if (reg == null) { return null; }
        return reg.getBoundingBox();
    }
    public BoundingBox getOldArea() {
        return this.oldArea_;
    }
    @Override
    public boolean isCancelled() {
        return this.cancelled_;
    }

    //SETTERS
    @Override
    public void setCancelled(boolean b) {
        this.cancelled_ = b;
    }
}
