package com.kntrel.mc.nbt;

import com.kntrel.mc.nbt.impl.RTagNBTPrimitive;

public interface NBTPrimitive extends NBTTag {

    //FACTORY
    static NBTPrimitive ofHandle(Object tag) {
        return new RTagNBTPrimitive(tag);
    }


    //API
    boolean is(NBTType<?> type);
    boolean isNumeric();
    boolean isNumericArray();
    boolean isString();
    boolean isBooleanish();
    <T> T get(NBTType<T> type);
    Number getAsNumber();
    Number[] getAsNumberArray();
    String getAsString();
    boolean getAsBoolean();
}
