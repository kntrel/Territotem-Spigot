package com.kntrel.mc.territotem.totem.piece;

//SUBTYPES
public enum TotemPieceDirection {

    //CONSTANTS
    EAST("east"),
    WEST("west"),
    NORTH("north"),
    SOUTH("south");


    //FIELDS
    private final String blockStateValue_;


    //CONSTRUCTOR
    TotemPieceDirection(String blockStateValue) {
        this.blockStateValue_ = blockStateValue;
    }


    //GETTERS
    String blockStateValue() { return this.blockStateValue_; }
}
