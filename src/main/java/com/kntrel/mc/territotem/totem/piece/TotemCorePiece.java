package com.kntrel.mc.territotem.totem.piece;

import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTileWriter;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.core.TotemCoreTracker;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record TotemCorePiece(TotemCoreTracker service) implements TotemPiece {

    //SINGLETON
    private static final Map<TotemCoreTracker, TotemCorePiece> PIECE_MAP = new ConcurrentHashMap<>();
    public static TotemCorePiece ofTracker(TotemCoreTracker totemCoreTracker) {
        return PIECE_MAP.computeIfAbsent(totemCoreTracker, TotemCorePiece::new);
    }


    //IMPLEMENTATION
    @Override public boolean matches(WorldTile tile) {
        TotemCore core = this.service.getCoreAt(tile.world(), tile.coordinates());
        if (core == null) {
            return false;
        }
        TotemCore.State state = core.getState();
        return state == TotemCore.State.FULL || state == TotemCore.State.ACTIVE || state == TotemCore.State.INACTIVE;
    }
    @Override public void place(WorldTileWriter tile) {
        this.service.createCore(tile.coordinates(), tile.actualWorld(), TotemCore.State.ACTIVE, TotemCore.Direction.ALL);
    }
}
