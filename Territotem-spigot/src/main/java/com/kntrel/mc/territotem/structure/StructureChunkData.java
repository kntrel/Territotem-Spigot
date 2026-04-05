package com.kntrel.mc.territotem.structure;

import com.kntrel.util.Vec3i;
import java.util.UUID;

record StructureChunkData(Vec3i offset, long blueprintId, UUID structureId, Structure.State state) {

    static final UUID UNASSIGNED_ID = new UUID(0L, 0L);

    StructureChunkData(Vec3i offset, long blueprintId, UUID structureId) {
        this(offset, blueprintId, structureId, Structure.State.EMPTY);
    }

    StructureChunkData(Vec3i offset, long blueprintId) {
        this(offset, blueprintId, UNASSIGNED_ID, Structure.State.EMPTY);
    }

    StructureChunkData {
        if (structureId == null) { structureId = UNASSIGNED_ID; }
        if (state == null) { state = Structure.State.EMPTY; }
    }

    boolean hasAssignedId() {
        return !UNASSIGNED_ID.equals(this.structureId);
    }
}
