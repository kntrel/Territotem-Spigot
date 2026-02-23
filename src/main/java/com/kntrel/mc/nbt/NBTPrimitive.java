package com.kntrel.mc.nbt;

import com.saicone.rtag.tag.TagBase;

public class NBTPrimitive extends NBTTag {

    //FIELDS
    private final byte typeID_;


    public NBTPrimitive(Object tag) {
        super(tag);
        if (!TagBase.isTag(tag)) {
            throw new ClassCastException("Passed object is not an NBTList");
        }
        this.typeID_ = TagBase.getTypeId(tag);
    }
}
