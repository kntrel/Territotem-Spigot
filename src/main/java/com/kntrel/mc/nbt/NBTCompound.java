package com.kntrel.mc.nbt;

import com.kntrel.mc.nbt.impl.RTagNBTCompound;

import java.util.Map;
import java.util.Optional;

public interface NBTCompound extends NBTTag, Map<String, NBTTag> {

    //FACTORY
    static NBTCompound create() {
        return new RTagNBTCompound();
    }


    //SPECIALIZATION
    Optional<NBTTag> getAt(Object... path);
    Optional<NBTTag> getAt(String path);


    //CONTRACT
    @Override Map<String, NBTTag> get();
}
