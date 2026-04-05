package com.kntrel.mc.territotem.totem.piece;

import com.kntrel.mc.state.StateMap;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTileWriter;
import org.bukkit.Material;
import java.util.Map;

public class TotemLecternPiece extends TotemDirectionalPiece implements TotemOptionalPiece {

    //SINGLETON
    private static final Map<TotemPieceDirection, TotemLecternPiece> PIECE_MAP = Map.of(
            TotemPieceDirection.EAST, new TotemLecternPiece(TotemPieceDirection.EAST),
            TotemPieceDirection.WEST, new TotemLecternPiece(TotemPieceDirection.WEST),
            TotemPieceDirection.NORTH, new TotemLecternPiece(TotemPieceDirection.NORTH),
            TotemPieceDirection.SOUTH, new TotemLecternPiece(TotemPieceDirection.SOUTH)
    );
    public static TotemLecternPiece ofDirection(TotemPieceDirection direction) {
        return PIECE_MAP.get(direction);
    }


    //CONSTRUCTOR
    private TotemLecternPiece(TotemPieceDirection direction) {
        super(direction, "facing");
    }


    //IMPLEMENTATION
    @Override public boolean isPlaced(WorldTile tile) {
        if (tile.blockType() != Material.LECTERN) { return false; }
        return super.matches(tile);
    }
    @Override public boolean matches(WorldTile tile) {
        return true;
    }
    @Override public void place(WorldTileWriter tile) {
        tile.setBlock(Material.LECTERN, StateMap.of(Map.entry("facing", this.getDirection().blockStateValue())));
    }
}
