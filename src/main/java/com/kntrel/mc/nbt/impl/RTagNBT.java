package com.kntrel.mc.nbt.impl;

import com.kntrel.mc.nbt.NBTTag;
import com.saicone.rtag.tag.TagBase;
import com.saicone.rtag.tag.TagCompound;
import com.saicone.rtag.tag.TagList;

public abstract class RTagNBT implements NBTTag {

    //FACTORY
    public static NBTTag asTag(Object object) {
        if (object == null) { return null; }

        if (TagCompound.isTagCompound(object)) { return new RTagNBTCompound(object); }
        if (TagList.isTagList(object)) { return new RTagNBTList(object); }
        if (TagBase.isTag(object)) { return new RTagNBTPrimitive(object); }

        Object tag;
        try {
            tag = TagBase.newTag(object);
        } catch (Throwable t) {
            throw new IllegalArgumentException(
                    "Unsupported value for NBT conversion: " + object.getClass().getName(), t
            );
        }

        if (tag == null) {
            throw new IllegalArgumentException(
                    "Unsupported value for NBT conversion: " + object.getClass().getName()
            );
        }

        if (TagCompound.isTagCompound(tag)) { return new RTagNBTCompound(tag); }
        if (TagList.isTagList(tag)) { return new RTagNBTList(tag); }
        if (TagBase.isTag(tag)) { return new RTagNBTPrimitive(tag); }

        throw new IllegalStateException(
                "TagBase.newTag produced a non-tag: " + tag.getClass().getName()
        );
    }


    //FIELDS
    private final Object handle_;


    //CONSTRUCTOR
    protected RTagNBT(Object tag) {
        this.handle_ = tag;
    }


    //IMPLEMENTATION
    public Object handle() {
        return this.handle_;
    }
    @Override public Object get() {
        return TagBase.getValue(this.handle_);
    }
    @Override public String toString() {
        return this.handle_.toString();
    }
    @Override public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o == this) { return true; }
        if (!(o instanceof RTagNBT other)) { return false; }
        return other.handle().equals(this.handle_);
    }
    @Override public int hashCode() {
        return this.handle_.hashCode();
    }
}
