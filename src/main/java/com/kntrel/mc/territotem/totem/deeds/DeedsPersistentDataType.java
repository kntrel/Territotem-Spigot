package com.kntrel.mc.territotem.totem.deeds;

import com.kntrel.util.Bytes;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

class DeedsPersistentDataType implements PersistentDataType<byte[], DeedsPersistentData> {

    private static final int FIXED_BYTES = 16;

    private static final DeedsPersistentDataType INSTANCE = new DeedsPersistentDataType();
    static DeedsPersistentDataType instance() {
        return INSTANCE;
    }

    private DeedsPersistentDataType() {}

    @Override
    public @NonNull Class<byte[]> getPrimitiveType() { return byte[].class; }

    @Override
    public @NonNull Class<DeedsPersistentData> getComplexType() { return DeedsPersistentData.class; }

    @Override
    public byte @NonNull [] toPrimitive(@NonNull DeedsPersistentData complex, @NonNull PersistentDataAdapterContext context) {
        byte[] namespace = Objects.requireNonNull(complex.nameSpace(), "nameSpace").getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[FIXED_BYTES + namespace.length];

        Bytes.fromInt(namespace.length, out, 0);
        System.arraycopy(namespace, 0, out, 4, namespace.length);
        Bytes.fromLong(complex.regionId(), out, 4 + namespace.length);
        Bytes.fromInt(complex.version(), out, 12 + namespace.length);
        return out;
    }

    @Override
    public @NonNull DeedsPersistentData fromPrimitive(byte @NonNull [] primitive, @NonNull PersistentDataAdapterContext context) {
        if (primitive.length < FIXED_BYTES) {
            throw new IllegalArgumentException("Not enough bytes to deserialize deeds data");
        }

        int namespaceLength = Bytes.toInt(primitive, 0);
        if (namespaceLength < 0) {
            throw new IllegalArgumentException("Namespace length cannot be negative");
        }

        int payloadLength = FIXED_BYTES + namespaceLength;
        if (primitive.length < payloadLength) {
            throw new IllegalArgumentException("Not enough bytes to deserialize deeds data");
        }

        String namespace = new String(primitive, 4, namespaceLength, StandardCharsets.UTF_8);
        long regionId = Bytes.toLong(primitive, 4 + namespaceLength);
        int version = Bytes.toInt(primitive, 12 + namespaceLength);
        return new DeedsPersistentData(namespace, regionId, version);
    }
}
