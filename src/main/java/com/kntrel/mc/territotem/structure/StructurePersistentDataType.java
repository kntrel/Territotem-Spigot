package com.kntrel.mc.territotem.structure;

import com.kntrel.util.Bytes;
import com.kntrel.util.Vec3i;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class StructurePersistentDataType implements PersistentDataType<byte[], List<StructureChunkData>> {

    //CONSTANTS
    private static final int CANDIDATE_BYTES = 29;
    private static final int LEGACY_CANDIDATE_BYTES = 28;


    //FACTORY
    private static final StructurePersistentDataType INSTANCE = new StructurePersistentDataType();
    public static StructurePersistentDataType instance() { return INSTANCE; }


    //CONSTRUCTOR
    private StructurePersistentDataType() {}


    //IMPLEMENTATION
    @Override
    public @NonNull Class<byte[]> getPrimitiveType() { return byte[].class; }

    @Override @SuppressWarnings({ "unchecked" })
    public @NonNull Class<List<StructureChunkData>> getComplexType() { return (Class<List<StructureChunkData>>) (Class<?>) List.class; }


    @Override
    public byte[] toPrimitive(@NonNull List<StructureChunkData> complex, @NonNull PersistentDataAdapterContext context) {
        byte[] out = new byte[complex.size() * CANDIDATE_BYTES];
        int offset = 0;
        for (StructureChunkData candidate : complex) {
            toBytes(out, offset, candidate);
            offset += CANDIDATE_BYTES;
        }
        return out;
    }

    @Override
    public @NonNull List<StructureChunkData> fromPrimitive(byte[] primitive, @NonNull PersistentDataAdapterContext context) {
        int entrySize = this.entrySizeFor(primitive.length);
        List<StructureChunkData> out = new ArrayList<>(primitive.length / entrySize);
        for (int i = 0; (i + entrySize) <= primitive.length; i += entrySize) {
            out.add(toCandidate(primitive, i, entrySize == CANDIDATE_BYTES));
        }
        return out;
    }


    //HELPERS
    private int entrySizeFor(int rawLength) {
        if (rawLength >= CANDIDATE_BYTES && rawLength % CANDIDATE_BYTES == 0) {
            return CANDIDATE_BYTES;
        }
        return LEGACY_CANDIDATE_BYTES;
    }
    private static StructureChunkData toCandidate(byte[] raw, int offset, boolean hasState) {
        int entrySize = hasState ? CANDIDATE_BYTES : LEGACY_CANDIDATE_BYTES;
        int len = raw.length;
        if (offset + entrySize > len) {
            throw new IllegalArgumentException(entrySize + " bytes are required to deserialize a structure candidate. Size " + len + " with offset " + offset);
        }

        byte x = raw[offset], z = raw[offset + 1];
        short y = Bytes.toShort(raw, offset + 2);
        long blueprintId = Bytes.toLong(raw, offset + 4);
        long mostSigBits = Bytes.toLong(raw, offset + 12);
        long leastSigBits = Bytes.toLong(raw, offset + 20);

        Structure.State state = Structure.State.EMPTY;
        if (hasState) {
            int ordinal = Byte.toUnsignedInt(raw[offset + 28]);
            Structure.State[] values = Structure.State.values();
            if (ordinal < values.length) {
                state = values[ordinal];
            }
        }

        return new StructureChunkData(new Vec3i(x, y, z), blueprintId, new UUID(mostSigBits, leastSigBits), state);
    }

    private static void toBytes(byte[] target, int offset, StructureChunkData candidate) {
        int len = target.length;
        if (offset + CANDIDATE_BYTES > len) {
            throw new IllegalArgumentException(CANDIDATE_BYTES + " bytes are required to serialize a structure candidate. Size " + len + " with offset " + offset);
        }

        Vec3i pos = candidate.offset();
        byte x = (byte) pos.x(), z = (byte) pos.z();
        short y = (short) pos.y();
        long blueprintId = candidate.blueprintId();
        UUID structureId = candidate.structureId();

        target[offset] = x;
        target[offset + 1] = z;
        Bytes.fromShort(y, target, offset + 2);
        Bytes.fromLong(blueprintId, target, offset + 4);
        Bytes.fromLong(structureId.getMostSignificantBits(), target, offset + 12);
        Bytes.fromLong(structureId.getLeastSignificantBits(), target, offset + 20);
        target[offset + 28] = (byte) candidate.state().ordinal();
    }
}
