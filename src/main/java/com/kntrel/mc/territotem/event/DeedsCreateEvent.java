package com.kntrel.mc.territotem.event;

import com.kntrel.mc.territotem.totem.Deeds;
import com.kntrel.mc.territotem.totem.Totem;
import com.jkantrell.regionslib.regions.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;

public class DeedsCreateEvent extends PlayerEvent implements Cancellable {

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
    private final Deeds deeds_;
    private boolean cancelled_ = false;

    //CONSTRUCTOR
    public DeedsCreateEvent(@NotNull Player who, Deeds deeds, Totem totem) {
        super(who);
        if (!deeds.getRegion().equals(totem.getRegion().orElse(null))) {
            throw new IllegalArgumentException("Deeds' and Totem's region must be the same");
        }
        this.deeds_ = deeds;
        this.totem_ = totem;
    }

    //GETTERS
    @Override
    public boolean isCancelled() {
        return this.cancelled_;
    }
    public Totem getTotem() {
        return this.totem_;
    }
    public Deeds getDeeds() {
        return this.deeds_;
    }
    public Region getRegion() {
        return this.getDeeds().getRegion();
    }
    public Location getLocation() {
        return this.totem_.getLocation();
    }

    //SETTERS
    @Override
    public void setCancelled(boolean b) {
        this.cancelled_ = b;
    }
}
