package com.kntrel.mc.nbt;

import com.saicone.rtag.tag.TagBase;

public class NBTPrimitive extends NBTTag {

    //FIELDS
    private final NBTType<?> type_;


    //CONSTRUCTORS
    public NBTPrimitive(Object tag) {
        super(tag);
        if (!TagBase.isTag(tag)) {
            throw new ClassCastException("Passed object is not an NBTList");
        }
        this.type_ = NBTType.ofTypeId(TagBase.getTypeId(tag));
        if (this.type_ == null) {
            throw new IllegalArgumentException("Provided inner tag is not of a valid type id");
        }
    }


    //API
    public boolean is(NBTType<?> type) {
        return this.type_.equals(type);
    }
    public <T> T get(NBTType<T> type) {
        if (!this.is(type)) { return null; }
        return type.type().cast(TagBase.getValue(this.handle()));
    }
}
