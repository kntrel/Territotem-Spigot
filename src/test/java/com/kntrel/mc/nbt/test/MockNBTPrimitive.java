package com.kntrel.mc.nbt.test;

import com.kntrel.mc.nbt.NBTPrimitive;
import com.kntrel.mc.nbt.NBTType;

import java.util.Set;

public class MockNBTPrimitive extends MockNBTTag implements NBTPrimitive {

    private static final Set<NBTType<?>> NUMERIC_TYPES = Set.of(
            NBTType.BYTE, NBTType.SHORT, NBTType.INTEGER, NBTType.LONG, NBTType.FLOAT, NBTType.DOUBLE
    );
    private static final Set<NBTType<?>> NUMERIC_ARRAY_TYPES = Set.of(
            NBTType.BYTE_ARRAY, NBTType.INTEGER_ARRAY, NBTType.LONG_ARRAY
    );

    private final Object value_;
    private final NBTType<?> type_;

    public MockNBTPrimitive(Object value) {
        super(value);
        this.value_ = value;

        @SuppressWarnings("unchecked")
        NBTType<Object> type = (NBTType<Object>) NBTType.ofType((Class<Object>) value.getClass());
        if (type == null) {
            throw new IllegalArgumentException("Unsupported primitive mock type: " + value.getClass().getName());
        }
        this.type_ = type;
    }

    @Override public Object get() {
        return this.value_;
    }

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
        return type.type().cast(this.value_);
    }

    @Override public Number getAsNumber() {
        if (!this.isNumeric()) { return null; }
        return (Number) this.value_;
    }

    @Override public Number[] getAsNumberArray() {
        if (!this.isNumericArray()) { return null; }
        return (Number[]) this.value_;
    }

    @Override public String getAsString() {
        return String.valueOf(this.value_);
    }

    @Override public boolean getAsBoolean() {
        Number num = this.getAsNumber();
        if (num == null) {
            throw new IllegalStateException("Tag is not of a boolean-is type");
        }
        return num.longValue() != 0L;
    }
}
