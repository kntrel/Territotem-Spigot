package com.kntrel.mc.territotem.totem.event;

import com.kntrel.mc.territotem.totem.core.TotemCore;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public abstract class TotemCoreInteractionEvent extends TotemCoreEvent implements Cancellable {


    //FIELDS
    private final Player player_;
    private final ItemStack itemStack_;
    private final EquipmentSlot hand_;
    private final Location location_;
    private boolean cancelled_;


    //CONSTRUCTOR
    public TotemCoreInteractionEvent(TotemCore core, Player player, ItemStack itemStack, EquipmentSlot hand, Location location) {
        super(core);
        this.player_ = player;
        this.itemStack_ = itemStack;
        this.hand_ = hand;
        this.location_ = location;
        this.cancelled_ = false;
    }


    //GETTERS
    public Player getPlayer() {
        return this.player_;
    }
    public ItemStack getItemStack() {
        return this.itemStack_;
    }
    public EquipmentSlot getHand() {
        return this.hand_;
    }
    public Location getLocation() {
        return this.location_;
    }
    @Override public boolean isCancelled() {
        return this.cancelled_;
    }


    //SETTERS
    @Override public void setCancelled(boolean b) {
        this.cancelled_ = b;
    }
}
