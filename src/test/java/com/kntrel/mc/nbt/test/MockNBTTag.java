package com.kntrel.mc.nbt.test;

import com.kntrel.mc.nbt.NBTTag;

public abstract class MockNBTTag implements NBTTag {


    protected MockNBTTag() {}

    @Override public String toString() {
        return String.valueOf(this.get());
    }
}
