package com.kntrel.mc.territotem.structure.event;

import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.Nullable;

public class StructureCompletedEvent extends StructureChangedEvent {

    //CONSTRUCTOR
    public StructureCompletedEvent(Structure structure, @Nullable Entity completer, Structure.State previousState, @Nullable Tile piece) {
        super(structure, completer, previousState, Structure.State.COMPLETE, piece, true);
    }
}
