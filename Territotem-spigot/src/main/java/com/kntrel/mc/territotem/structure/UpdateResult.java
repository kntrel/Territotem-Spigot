package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;

import java.util.Optional;

public sealed interface UpdateResult {

    //IMPLEMENTATIONS
    final class NoChange implements UpdateResult {
        private NoChange() {}

        private static final NoChange INSTANCE = new NoChange();
    }
    record Change(Tile piece, boolean match, Structure.State oldState, Structure.State newState) implements UpdateResult {

        boolean stateChanged() {
            return this.oldState != this.newState;
        }
        boolean noStateChanged() {
            return this.oldState == this.newState;
        }
        boolean wasCompleted() {
            if (noStateChanged()) { return false; }
            return this.oldState != Structure.State.COMPLETE
                    && this.newState == Structure.State.COMPLETE;
        }
        boolean wasUncompleted() {
            if (noStateChanged()) { return false; }
            return this.oldState == Structure.State.COMPLETE;
        }

    }


    //FACTORY
    static NoChange noChange() { return NoChange.INSTANCE; }
    static Change change(Tile piece, boolean match, Structure.State oldState, Structure.State newState) {
        return new Change(piece, match, oldState, newState);
    }


    //DEFAULTS
    default boolean unchanged() {
        return this == NoChange.INSTANCE;
    }
    default Optional<Change> getChange() {
        if (this == NoChange.INSTANCE) { return Optional.empty(); }
        if (this instanceof Change c) { return Optional.of(c); }
        return Optional.empty();
    }
}
