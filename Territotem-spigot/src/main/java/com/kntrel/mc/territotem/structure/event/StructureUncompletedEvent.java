package com.kntrel.mc.territotem.structure.event;

import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.Nullable;

public class StructureUncompletedEvent extends StructureChangedEvent {

    //CONSTRUCTOR
    public StructureUncompletedEvent(Structure structure, @Nullable Entity uncompleter, Structure.State currentState, @Nullable Tile piece) {
        super(structure, uncompleter, Structure.State.COMPLETE, currentState, piece, false);
    }
}
