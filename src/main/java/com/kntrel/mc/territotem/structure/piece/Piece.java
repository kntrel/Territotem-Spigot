package com.kntrel.mc.territotem.structure.piece;

import org.bukkit.Material;

public interface Piece {

    //FACTORY
    static Piece block(Material material) {
        return new PieceImpl.BlockPiece(material);
    }


    //CONTRACT
    boolean matches(WorldTile tile);
    void place(WorldTileWriter tile);

}
