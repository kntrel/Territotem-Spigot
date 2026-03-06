package com.kntrel.mc.territotem.totem.piece;

import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.util.Vec3i;

public class TotemTile<P extends TotemPiece> extends Tile {
    public TotemTile(Vec3i offset, P piece) {
        super(offset, piece);
    }

    public TotemTile(int x, int y, int z, P piece) {
        super(x, y, z, piece);
    }

    @Override @SuppressWarnings("unchecked") public P piece() {
        return (P) super.piece();
    }
}
