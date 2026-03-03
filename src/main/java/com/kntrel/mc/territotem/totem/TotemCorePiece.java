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


    //IMPLEMENTATION
    @Override public boolean matches(WorldTile tile) {
        TotemCore core = this.service_.getCoreAt(tile.world(), tile.coordinates());
        if (core == null) { return false; }
        TotemCore.State state = core.getState();
        return state == TotemCore.State.FULL || state == TotemCore.State.ACTIVE;
    }
    @Override public void place(WorldTileWriter tile) {
        this.service_.createCore(tile.coordinates(), tile.actualWorld(), TotemCore.State.ACTIVE, TotemCore.Direction.ALL);
    }
}
