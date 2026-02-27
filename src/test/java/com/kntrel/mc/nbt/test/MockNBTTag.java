package com.kntrel.mc.nbt.test;

import com.kntrel.mc.nbt.NBTTag;

import java.util.Objects;

public abstract class MockNBTTag implements NBTTag {

    private final Object handle_;

    protected MockNBTTag(Object handle) {
        this.handle_ = handle;
    }

    @Override public Object handle() {
        return this.handle_;
    }

    @Override public String toString() {
        return String.valueOf(this.get());
    }

    @Override public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof NBTTag other)) { return false; }
        return Objects.equals(this.handle_, other.handle());
    }

    @Override public int hashCode() {
        return Objects.hashCode(this.handle_);
    }
}
