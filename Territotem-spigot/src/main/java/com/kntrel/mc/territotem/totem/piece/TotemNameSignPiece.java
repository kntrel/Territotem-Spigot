package com.kntrel.mc.territotem.totem.piece;

import com.kntrel.mc.state.StateMap;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTileWriter;
import org.bukkit.Material;
import org.bukkit.Tag;
import java.util.Map;

public class TotemNameSignPiece extends TotemDirectionalPiece implements TotemOptionalPiece {

    //SINGLETON
    private static final Map<TotemPieceDirection, TotemNameSignPiece> PIECE_MAP = Map.of(
            TotemPieceDirection.EAST, new TotemNameSignPiece(TotemPieceDirection.EAST),
            TotemPieceDirection.WEST, new TotemNameSignPiece(TotemPieceDirection.WEST),
            TotemPieceDirection.NORTH, new TotemNameSignPiece(TotemPieceDirection.NORTH),
            TotemPieceDirection.SOUTH, new TotemNameSignPiece(TotemPieceDirection.SOUTH)
    );
    public static TotemNameSignPiece ofDirection(TotemPieceDirection direction) {
        return PIECE_MAP.get(direction);
    }


    //CONSTRUCTOR
    private TotemNameSignPiece(TotemPieceDirection direction) {
        super(direction, "facing");
    }


    //IMPLEMENTATION
    @Override public boolean isPlaced(WorldTile tile) {
        if (!Tag.WALL_SIGNS.isTagged(tile.blockType())) { return false; }
        return super.matches(tile);
    }
    @Override public boolean matches(WorldTile tile) {
        return true;
    }
    @Override public void place(WorldTileWriter tile) {
        tile.setBlock(Material.OAK_SIGN, StateMap.of(Map.entry("facing", this.getDirection().blockStateValue())));
    }
}
