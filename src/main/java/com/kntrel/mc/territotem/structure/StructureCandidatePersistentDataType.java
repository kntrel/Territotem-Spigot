package com.kntrel.mc.territotem.structure;

import com.kntrel.util.Bytes;
import com.kntrel.util.Vec3i;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;

class StructureCandidatePersistentDataType implements PersistentDataType<byte[], List<StructureChunkData>> {

    //FACTORY
    private static final StructureCandidatePersistentDataType INSTANCE = new StructureCandidatePersistentDataType();
    public static StructureCandidatePersistentDataType instance() { return INSTANCE; }


    //CONSTRUCTOR
    private StructureCandidatePersistentDataType() {}


    //IMPLEMENTATION
    @Override
    public @NonNull Class<byte[]> getPrimitiveType() { return byte[].class; }

    @Override @SuppressWarnings({ "unchecked" })
    public @NonNull Class<List<StructureChunkData>> getComplexType() { return (Class<List<StructureChunkData>>) (Class<?>) List.class; }


    @Override
    public byte[] toPrimitive(@NonNull List<StructureChunkData> complex, @NonNull PersistentDataAdapterContext context) {
        byte[] out = new byte[complex.size() * 12];
        int offset = 0;
        for (StructureChunkData candidate : complex) {
            toBytes(out, offset, candidate);
            offset += 12;
        }
        return out;
    }

    @Override
    public @NonNull List<StructureChunkData> fromPrimitive(byte[] primitive, @NonNull PersistentDataAdapterContext context) {
        List<StructureChunkData> out = new ArrayList<>(primitive.length / 12);
        for (int i = 0; (i + 12) <= primitive.length; i += 12) {
            StructureChunkData candidate = toCandidate(primitive, i);
            out.add(candidate);
        }
        return out;
    }


    //HELPERS
    private static StructureChunkData toCandidate(byte[] raw, int offset) {
       /* Candidates are 12 bytes
        * 1(byte)  -> x offset
        * 1(byte)  -> z offset
        * 2(short) -> y offset
        * 8(long)  -> blueprint id
        */

        int len = raw.length;
        if (offset + 12 > len) {
            throw new IllegalArgumentException("12 bytes are required to deserialize a totem candidate. Size " + len + " with offset " + offset);
        }

        byte x = raw[offset], z = raw[offset + 1];
        short y = Bytes.toShort(raw,  offset + 2);
        long blueprintId = Bytes.toLong(raw,  offset + 4);

        return new StructureChunkData(new Vec3i(x, y, z), blueprintId);
    }
    private static void toBytes(byte[] target, int offset, StructureChunkData candidate) {

        int len = target.length;
        if (offset + 12 > len) {
            throw new IllegalArgumentException("12 bytes are required to serialize a totem candidate. Size " + len + " with offset " + offset);
        }

        Vec3i pos = candidate.offset();
        byte x = (byte) pos.x(), z = (byte) pos.z();
        short y = (short) pos.y();
        long blueprintId = candidate.blueprintId();

        target[offset] = x;
        target[offset + 1] = z;
        Bytes.fromShort(y, target, offset + 2);
        Bytes.fromLong(blueprintId, target, offset + 4);
    }
}
