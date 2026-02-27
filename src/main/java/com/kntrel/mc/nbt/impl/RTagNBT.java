package com.kntrel.mc.nbt.impl;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.nbt.NBTList;
import com.kntrel.mc.nbt.NBTPrimitive;
import com.kntrel.mc.nbt.NBTTag;
import com.saicone.rtag.tag.TagBase;
import com.saicone.rtag.tag.TagCompound;
import com.saicone.rtag.tag.TagList;

public final class RTagNBT {

    private RTagNBT() {}

    public static NBTTag asTag(Object object) {
        if (object == null) { return null; }

        if (TagCompound.isTagCompound(object)) { return NBTCompound.ofHandle(object); }
        if (TagList.isTagList(object)) { return NBTList.ofHandle(object); }
        if (TagBase.isTag(object)) { return NBTPrimitive.ofHandle(object); }

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

        if (TagCompound.isTagCompound(tag)) { return NBTCompound.ofHandle(tag); }
        if (TagList.isTagList(tag)) { return NBTList.ofHandle(tag); }
        if (TagBase.isTag(tag)) { return NBTPrimitive.ofHandle(tag); }

        throw new IllegalStateException(
                "TagBase.newTag produced a non-tag: " + tag.getClass().getName()
        );
    }
}
