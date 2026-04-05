package com.kntrel.mc.territotem.totem.event;

import com.kntrel.mc.territotem.totem.core.TotemCore;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public class TotemCoreRightClickedEvent extends TotemCoreInteractionEvent {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NonNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    public TotemCoreRightClickedEvent(TotemCore core, Player player, ItemStack itemStack, EquipmentSlot hand, Location location) {
        super(core, player, itemStack, hand, location);
    }

}
