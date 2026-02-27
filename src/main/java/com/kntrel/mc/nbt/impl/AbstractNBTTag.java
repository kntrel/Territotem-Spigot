package com.kntrel.mc.nbt.impl;

import com.kntrel.mc.nbt.NBTTag;
import com.saicone.rtag.tag.TagBase;

public abstract class AbstractNBTTag implements NBTTag {

    private final Object handle_;

    protected AbstractNBTTag(Object tag) {
        this.handle_ = tag;
    }

    @Override public Object handle() {
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
        if (!(o instanceof NBTTag other)) { return false; }
        return other.handle().equals(this.handle_);
    }

    @Override public int hashCode() {
        return this.handle_.hashCode();
    }
}
