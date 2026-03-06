package com.kntrel.mc.territotem.totem.piece;

import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTileWriter;
import com.kntrel.mc.territotem.totem.TotemCore;
import com.kntrel.mc.territotem.totem.TotemService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record TotemCorePiece(TotemService service) implements TotemPiece {

    //SINGLETON
    private static final Map<TotemService, TotemCorePiece> PIECE_MAP = new ConcurrentHashMap<>();
    public static TotemCorePiece ofService(TotemService totemService) {
        return PIECE_MAP.computeIfAbsent(totemService, TotemCorePiece::new);
    }


    //IMPLEMENTATION
    @Override public boolean matches(WorldTile tile) {
        TotemCore core = this.service.getCoreAt(tile.world(), tile.coordinates());
        if (core == null) {
            return false;
        }
        TotemCore.State state = core.getState();
        return state == TotemCore.State.FULL || state == TotemCore.State.ACTIVE;
    }
    @Override public void place(WorldTileWriter tile) {
        this.service.createCore(tile.coordinates(), tile.actualWorld(), TotemCore.State.ACTIVE, TotemCore.Direction.ALL);
    }
}
