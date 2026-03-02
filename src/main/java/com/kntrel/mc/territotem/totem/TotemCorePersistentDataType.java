package com.kntrel.mc.territotem.totem;

import com.kntrel.util.Bytes;
import com.kntrel.util.Vec3i;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;

class TotemCorePersistentDataType implements PersistentDataType<byte[], List<TotemCoreChunkData>> {

    //CONSTANTS
    private static final int ENTRY_SIZE = 6;


    //SINGLETON
    private static final TotemCorePersistentDataType INSTANCE = new TotemCorePersistentDataType();
    static TotemCorePersistentDataType instance() {
        return INSTANCE;
    }
    private TotemCorePersistentDataType() {}


    //IMPLEMENTATION
    @Override
    public @NonNull Class<byte[]> getPrimitiveType() { return byte[].class; }

    @Override
    @SuppressWarnings("unchecked")
    public @NonNull Class<List<TotemCoreChunkData>> getComplexType() {
        return (Class<List<TotemCoreChunkData>>) (Class<?>) List.class;
    }

    @Override
    public byte[] toPrimitive(@NonNull List<TotemCoreChunkData> complex, @NonNull PersistentDataAdapterContext context) {
        byte[] out = new byte[complex.size() * ENTRY_SIZE];
        int offset = 0;
        for (TotemCoreChunkData core : complex) {
            toBytes(out, offset, core);
            offset += ENTRY_SIZE;
        }
        return out;
    }

    @Override
    public @NonNull List<TotemCoreChunkData> fromPrimitive(byte @NonNull [] primitive, @NonNull PersistentDataAdapterContext context) {
        List<TotemCoreChunkData> out = new ArrayList<>(primitive.length / ENTRY_SIZE);
        for (int i = 0; (i + ENTRY_SIZE) <= primitive.length; i += ENTRY_SIZE) {
            out.add(toCore(primitive, i));
        }
        return out;
    }

    private static TotemCoreChunkData toCore(byte[] raw, int offset) {
        if (offset + ENTRY_SIZE > raw.length) {
            throw new IllegalArgumentException("Not enough bytes to deserialize totem core");
        }

        byte x = raw[offset];
        byte z = raw[offset + 1];
        short y = Bytes.toShort(raw, offset + 2);
        TotemCore.State state = TotemCore.State.values()[raw[offset + 4]];
        TotemCore.Direction direction = TotemCore.Direction.values()[raw[offset + 5]];

        return new TotemCoreChunkData(new Vec3i(x, y, z), state, direction);
    }

    private static void toBytes(byte[] target, int offset, TotemCoreChunkData core) {
        if (offset + ENTRY_SIZE > target.length) {
            throw new IllegalArgumentException("Not enough bytes to serialize totem core");
        }

        Vec3i pos = core.offset();
        target[offset] = (byte) pos.x();
        target[offset + 1] = (byte) pos.z();
        Bytes.fromShort((short) pos.y(), target, offset + 2);
        target[offset + 4] = (byte) core.state().ordinal();
        target[offset + 5] = (byte) core.direction().ordinal();
    }
}
