package com.kntrel.mc.nbt;

import com.saicone.rtag.tag.TagBase;
import java.util.Set;

public class NBTPrimitive extends NBTTag {

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
    public boolean isNumeric() {
        return NUMERIC_TYPES.contains(this.type_);
    }
    public boolean isNumericArray() {
        return NUMERIC_ARRAY_TYPES.contains(this.type_);
    }
    public boolean isString() {
        return this.type_.equals(NBTType.STRING);
    }
    public boolean isBooleanish() {
        return this.isNumeric();
    }
    public <T> T get(NBTType<T> type) {
        if (!this.is(type)) { return null; }
        return type.type().cast(TagBase.getValue(this.handle()));
    }
    public Number getAsNumber() {
        if (!this.isNumeric()) { return null; }
        return (Number) this.get();
    }
    public Number[] getAsNumberArray() {
        if (!this.isNumericArray()) { return null; }
        return (Number[]) this.get();
    }
    public String getAsString() {
        if (this.isString()) {
            return this.get(NBTType.STRING);
        }
        return this.get().toString();
    }
    public boolean getAsBoolean() {
        Number num = this.getAsNumber();
        if (num != null) {
            return num.longValue() != 0;
        }
        throw new IllegalStateException("Tag is not of a boolean-is type");
    }
}
