package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTileWriter;
import java.util.HashMap;
import java.util.Map;

public class TotemCorePiece implements Piece {

    //FACTORY
    private static final Map<TotemService, TotemCorePiece> PIECE_MAP = new HashMap<>();
    public static TotemCorePiece ofService(TotemService service) {
        return PIECE_MAP.computeIfAbsent(service, TotemCorePiece::new);
    }


    //FIELDS
    private final TotemService service_;


    //CONSTRUCTOR
    private TotemCorePiece(TotemService service) {
        this.service_ = service;
    }

    @Override public boolean matches(WorldTile tile) {

    }

    @Override
    public void place(WorldTileWriter tile) {

    }


    //IMPLEMENTATION

}
