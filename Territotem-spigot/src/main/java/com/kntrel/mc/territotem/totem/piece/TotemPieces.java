package com.kntrel.mc.territotem.totem.piece;

import com.kntrel.mc.territotem.totem.core.TotemCoreTracker;

public final class TotemPieces {
    private TotemPieces() {}


    //API
    public static TotemCorePiece core(TotemCoreTracker totemCoreTracker) {
        return TotemCorePiece.ofTracker(totemCoreTracker);
    }
    public static TotemNameSignPiece sign(TotemPieceDirection direction) {
        return TotemNameSignPiece.ofDirection(direction);
    }
    public static TotemLecternPiece lectern(TotemPieceDirection direction) {
        return TotemLecternPiece.ofDirection(direction);
    }
}
