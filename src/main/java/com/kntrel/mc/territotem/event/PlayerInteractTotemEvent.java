package com.kntrel.mc.territotem.event;

import com.kntrel.mc.territotem.totem.Totem;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class PlayerInteractTotemEvent extends PlayerInteractEntityEvent {
    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
    @Override
    @Nonnull
    public HandlerList getHandlers() {
        return PlayerInteractTotemEvent.HANDLERS;
    }
    //===============================================================

    //ASSETS
    public enum Action { RIGHT_CLICK, LEFT_CLICK }

    //FIELDS
    private final Totem totem_;
    private final BlockFace clickedFace_;
    private final Action action_;

    //CONSTRUCTORS
    public PlayerInteractTotemEvent(@NotNull Player who, @Nonnull Totem totem, EquipmentSlot hand, Action action) {
        super(who, totem.getEndCrystal(), hand);
        this.totem_ = totem;
        this.action_ = action;

        BoundingBox totemBox = BoundingBox.of(totem.getLocation().add(-.8,-.8,-.8), totem.getLocation().add(.8,.8,.8));
        Location loc = player.getEyeLocation();
        RayTraceResult result = totemBox.rayTrace(new Vector(loc.getX(), loc.getY(), loc.getZ()), loc.getDirection(),30);
        if (result == null) { this.clickedFace_ = null; return; }
        BlockFace face = result.getHitBlockFace();
        if (face == null) { this.clickedFace_ = null; return; }
        this.clickedFace_ = face;
    }

    //GETTERS
    public Totem getTotem() {
        return this.totem_;
    }
    public PlayerInteractTotemEvent.Action getAction() {
        return this.action_;
    }
    public BlockFace getClickedFace() {
        return this.clickedFace_;
    }
}
