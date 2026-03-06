package com.kntrel.mc.territotem.totem.piece;

import com.kntrel.mc.territotem.structure.worldTile.WorldTile;

public abstract class TotemDirectionalPiece implements TotemPiece {

    //FIELDS
    private final TotemPieceDirection direction_;
    private final String stateKey_;


    //CONSTRUCTOR
    public TotemDirectionalPiece(TotemPieceDirection direction, String stateKey) {
        this.direction_ = direction;
        this.stateKey_ = stateKey;
    }


    //IMPLEMENTATION
    @Override
    public boolean matches(WorldTile tile) {
        Object val = tile.blockState().get(this.stateKey_);
        if (val == null) { return false; }
        return val.toString().equals(this.direction_.blockStateValue());
    }

    //GETTERS
    public TotemPieceDirection getDirection() {
        return this.direction_;
    }
}
