package com.kntrel.mc.nbt.impl;

import com.kntrel.mc.nbt.NBTPrimitive;
import com.kntrel.mc.nbt.NBTType;
import com.saicone.rtag.tag.TagBase;

import java.util.Set;

public class RTagNBTPrimitive extends RTagNBT implements NBTPrimitive {

    //CONSTANTS
    private static final Set<NBTType<?>> NUMERIC_TYPES = Set.of(
            NBTType.BYTE, NBTType.SHORT, NBTType.INTEGER, NBTType.LONG, NBTType.FLOAT, NBTType.DOUBLE
    );
    private static final Set<NBTType<?>> NUMERIC_ARRAY_TYPES = Set.of(
            NBTType.BYTE_ARRAY, NBTType.INTEGER_ARRAY, NBTType.LONG_ARRAY
    );


    //FIELDS
    private final NBTType<?> type_;


    //CONSTRUCTORS
    public RTagNBTPrimitive(Object tag) {
        super(tag);
        if (!TagBase.isTag(tag)) {
            throw new ClassCastException("Passed object is not an NBTList");
        }
        this.type_ = NBTType.ofTypeId(TagBase.getTypeId(tag));
        if (this.type_ == null) {
            throw new IllegalArgumentException("Provided inner tag is not of a valid type id");
        }
    }


    //IMPLEMENTATION
    @Override public boolean is(NBTType<?> type) {
        return this.type_.equals(type);
    }
    @Override public boolean isNumeric() {
        return NUMERIC_TYPES.contains(this.type_);
    }
    @Override public boolean isNumericArray() {
        return NUMERIC_ARRAY_TYPES.contains(this.type_);
    }
    @Override public boolean isString() {
        return this.type_.equals(NBTType.STRING);
    }
    @Override public boolean isBooleanish() {
        return this.isNumeric();
    }
    @Override public <T> T get(NBTType<T> type) {
        if (!this.is(type)) { return null; }
        return type.type().cast(TagBase.getValue(this.handle()));
    }
    @Override public Number getAsNumber() {
        if (!this.isNumeric()) { return null; }
        return (Number) this.get();
    }
    @Override public Number[] getAsNumberArray() {
        if (!this.isNumericArray()) { return null; }
        return (Number[]) this.get();
    }
    @Override public String getAsString() {
        if (this.isString()) {
            return this.get(NBTType.STRING);
        }
        return this.get().toString();
    }
    @Override public boolean getAsBoolean() {
        Number num = this.getAsNumber();
        if (num != null) {
            return num.longValue() != 0;
        }
        throw new IllegalStateException("Tag is not of a boolean-is type");
    }
}
