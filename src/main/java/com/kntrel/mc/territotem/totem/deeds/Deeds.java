package com.kntrel.mc.territotem.totem.deeds;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Permission;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import java.util.List;

public class Deeds {

    //FIELDS
    private final Region region_;
    private final BookMeta book_;
    private final List<Permission> permissions_;
    private final int version_;


    //CONSTRUCTOR
    public Deeds(Region region, BookMeta book, List<Permission> permissions, int version) {
        this.region_ = region;
        this.book_ = book;
        this.permissions_ = permissions;
        this.version_ = version;
    }


    //GETTERS
    public Region region() {
        return this.region_;
    }
    public BookMeta book() {
        return this.book_;
    }
    public List<Permission> permissions() {
        return List.copyOf(this.permissions_);
    }
    public int version() {
        return this.version_;
    }
}
