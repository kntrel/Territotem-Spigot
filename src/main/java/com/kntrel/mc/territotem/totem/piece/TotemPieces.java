package com.kntrel.mc.territotem.totem.piece;

import com.kntrel.mc.territotem.totem.TotemService;

public final class TotemPieces {
    private TotemPieces() {}


    //API
    public static TotemCorePiece core(TotemService totemService) {
        return TotemCorePiece.ofService(totemService);
    }
    public static TotemNameSignPiece sign(TotemPieceDirection direction) {
        return TotemNameSignPiece.ofDirection(direction);
    }
    public static TotemLecternPiece lectern(TotemPieceDirection direction) {
        return TotemLecternPiece.ofDirection(direction);
    }
}
