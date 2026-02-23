package com.kntrel.mc.nbt;

import com.saicone.rtag.tag.TagBase;
import com.saicone.rtag.tag.TagCompound;
import com.saicone.rtag.tag.TagList;

public abstract class NBTTag {

    //FACTORY
    public static NBTTag asTag(Object object) {
        if (object == null) { return null; }

        // Already an NBT handle?
        if (TagCompound.isTagCompound(object)) { return new NBTCompound(object); }
        if (TagList.isTagList(object)) { return new NBTList(object); }
        if (TagBase.isTag(object)) { return new NBTPrimitive(object); }

        // Convert Java value -> NBT handle once
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

        // Now it *must* be a tag (otherwise something is wrong)
        if (TagCompound.isTagCompound(tag)) { return new NBTCompound(tag); }
        if (TagList.isTagList(tag)) { return new NBTList(tag); }
        if (TagBase.isTag(tag)) { return new NBTPrimitive(tag); }

        throw new IllegalStateException(
                "TagBase.newTag produced a non-tag: " + tag.getClass().getName()
        );
    }


    //FIELDS
    private final Object handle_;


    //CONSTRUCTOR
    protected NBTTag(Object tag) {
        this.handle_ = tag;
    }


    protected Object handle() { return this.handle_; }
    public Object get() {
        return TagBase.getValue(this.handle_);
    }
}
