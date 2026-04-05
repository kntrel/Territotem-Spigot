package com.kntrel.mc.territotem.totem.piece;

import com.kntrel.mc.territotem.structure.worldTile.WorldTile;

public interface TotemOptionalPiece extends TotemPiece {

    boolean isPlaced(WorldTile tile);
    @Override default boolean matches(WorldTile tile) { return true; }
}
