package com.kntrel.mc.territotem.event;

import com.kntrel.mc.territotem.totem.Totem;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TotemDestroyedByPlayerEvent extends PlayerEvent implements Cancellable {
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

    //ASSETS
    public enum Method { HAND, ARROW }

    //FIELDS
    private final Totem totem_;
    private final TotemDestroyedByPlayerEvent.Method method_;
    private final Arrow arrow_;
    private boolean cancelled_;

    //CONSTRUCTORS
    public TotemDestroyedByPlayerEvent(Player who, Totem totem, @Nullable Arrow weapon) {
        super(who);
        this.totem_ = totem;
        this.method_ = (weapon == null) ? Method.HAND : Method.ARROW;
        this.arrow_ = weapon;
    }

    //GETTERS
    public Player getDestroyer() {
        return super.getPlayer();
    }
    public Totem getTotem() {
        return totem_;
    }
    public Method getMethod() {
        return method_;
    }
    public Arrow getArrow() {
        return this.arrow_;
    }
    @Override
    public boolean isCancelled() {
        return cancelled_;
    }

    //SETTERS
    @Override
    public void setCancelled(boolean isCancelled) {
        this.cancelled_ = isCancelled;
    }
}
