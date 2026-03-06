package com.kntrel.mc.territotem.structure;

import com.kntrel.util.Vec3i;
import java.util.UUID;

record StructureChunkData(Vec3i offset, long blueprintId, UUID structureId) {

    static final UUID UNASSIGNED_ID = new UUID(0L, 0L);

    StructureChunkData(Vec3i offset, long blueprintId) {
        this(offset, blueprintId, UNASSIGNED_ID);
    }

    StructureChunkData {
        if (structureId == null) { structureId = UNASSIGNED_ID; }
    }

    boolean hasAssignedId() {
        return !UNASSIGNED_ID.equals(this.structureId);
    }
}
