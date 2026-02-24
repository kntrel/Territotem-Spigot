package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.util.Vec3i;

public class BlueprintCoreTile extends Tile {

    public BlueprintCoreTile(Vec3i offset, Piece.Core element) {
        super(offset, element);
    }

    public BlueprintCoreTile(int x, int y, int z, Piece.Core element) {
        super(x, y, z, element);
    }

    @Override public Piece.Core element() {
        return (Piece.Core) super.element();
    }

    @Override public BlueprintCoreTile withOffset(Vec3i offset) {
        return new BlueprintCoreTile(offset, this.element());
    }
}
