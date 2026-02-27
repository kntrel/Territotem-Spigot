package com.kntrel.mc.nbt;

import com.kntrel.mc.nbt.impl.RTagNBTList;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NBTList extends NBTTag, Collection<NBTTag> {

    //FACTORY
    static NBTList create() {
        return new RTagNBTList();
    }


    //SPECIALIZATION
    Optional<NBTTag> getAt(Object... path);
    Optional<NBTTag> getAt(String path);


    //CONTRACT
    @Override List<NBTTag> get();
}
