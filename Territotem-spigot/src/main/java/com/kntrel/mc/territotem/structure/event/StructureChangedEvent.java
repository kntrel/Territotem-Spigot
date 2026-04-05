package com.kntrel.mc.territotem.structure.event;

import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.swing.plaf.PanelUI;

public class StructureChangedEvent extends StructureEvent {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NonNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final Structure.State previousState_, currentState_;
    private final Tile piece_;
    private final boolean match_;
    private final Entity causer_;


    //CONSTRUCTOR
    public StructureChangedEvent(Structure structure, @Nullable Entity causer, Structure.State previous, Structure.State current, @Nullable Tile piece, boolean match) {
        super(structure);
        this.previousState_ = previous;
        this.currentState_ = current;
        this.causer_ = causer;
        this.piece_ = piece;
        this.match_ = match;
    }


    //GETTERS
    public Structure.State getPreviousState() { return this.previousState_; }
    public Structure.State getCurrentState() { return this.currentState_; }
    public @Nullable Tile getChangedPiece() { return this.piece_; }
    public boolean getPieceMatched() { return this.match_; }
    public @Nullable Entity getCauser() { return this.causer_; }
}
