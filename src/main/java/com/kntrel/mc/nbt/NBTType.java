package com.kntrel.mc.nbt;

import com.kntrel.util.Constants;
import org.jspecify.annotations.Nullable;
import java.util.Arrays;
import java.util.Objects;

public final class NBTType<T> {

    //FACTORY
    public static @Nullable NBTType<?> ofTypeId(byte typeId) {
        for (NBTType<?> type : VALUES_) {
            if (type.typeId_ == typeId) {
                return type;
            }
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    public static <T> @Nullable NBTType<T> ofType(Class<T> clazz) {
        Class<?> wrapped = wrap(clazz);
        for (NBTType<?> type : VALUES_) {
            if (type.type_.equals(wrapped)) {
                return (NBTType<T>) type;
            }
        }
        return null;
    }


    //CONSTANTS
    //Byte = 1 | Short = 2 | Int = 3 | Long = 4 | Float = 5 | Double = 6 | ByteArray = 7 | String = 8 | List = 9 | Compound = 10 | IntArray = 11 | LongArray = 12
    public static final NBTType<Byte> BYTE = new NBTType<>(1, Byte.class);
    public static final NBTType<Short> SHORT = new NBTType<>(2, Short.class);
    public static final NBTType<Integer> INTEGER = new NBTType<>(3, Integer.class);
    public static final NBTType<Long> LONG = new NBTType<>(4, Long.class);
    public static final NBTType<Float> FLOAT = new NBTType<>(5, Float.class);
    public static final NBTType<Double> DOUBLE = new NBTType<>(6, Double.class);
    public static final NBTType<Byte[]> BYTE_ARRAY = new NBTType<>(7, Byte[].class);
    public static final NBTType<String> STRING = new NBTType<>(8, String.class);
    public static final NBTType<Integer[]> INTEGER_ARRAY = new NBTType<>(11, Integer[].class);
    public static final NBTType<Long[]> LONG_ARRAY = new NBTType<>(12, Long[].class);

    private static final NBTType<?>[] VALUES_ = Constants.getAll(NBTType.class, NBTType.class).toArray(new NBTType<?>[0]);
    public static NBTType<?>[] values() {
        return Arrays.copyOf(VALUES_, VALUES_.length);
    }


    //FIELDS
    private final byte typeId_;
    private final Class<T> type_;


    //CONSTRUCTOR
    private NBTType(int typeId, Class<T> type) {
        this.typeId_ = (byte) typeId;
        this.type_ = type;
    }


    //GETTERS
    public byte typeId() { return this.typeId_; }
    public Class<T> type() { return this.type_; }


    //IMPLEMENTATION
    @Override public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o == this) { return true; }
        if (!(o instanceof NBTType<?> other)) { return false; }
        return this.typeId_ == other.typeId_;
    }
    @Override public int hashCode() {
        return Byte.hashCode(this.typeId_);
    }


    //HELPERS
    private static Class<?> wrap(Class<?> primitiveClass) {
        if (primitiveClass == byte.class) { return Byte.class; }
        if (primitiveClass == short.class) { return Short.class; }
        if (primitiveClass == int.class) { return Integer.class; }
        if (primitiveClass == long.class) { return Long.class; }
        if (primitiveClass == float.class) { return Float.class; }
        if (primitiveClass == double.class) { return Double.class; }
        if (primitiveClass == boolean.class) { return Boolean.class; }

        if (primitiveClass == byte[].class) { return Byte[].class; }
        if (primitiveClass == int[].class) { return Integer[].class; }
        if (primitiveClass == long[].class) { return Long[].class; }
    
        return primitiveClass;
    }
}
