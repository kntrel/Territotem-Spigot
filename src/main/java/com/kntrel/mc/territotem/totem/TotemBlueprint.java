package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.structure.blueprint.InvalidBlueprintException;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.totem.piece.TotemCorePiece;
import com.kntrel.mc.territotem.totem.piece.TotemLecternPiece;
import com.kntrel.mc.territotem.totem.piece.TotemNameSignPiece;
import com.kntrel.mc.territotem.totem.piece.TotemTile;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class TotemBlueprint extends Blueprint {

    //FIELDS
    private final TotemTile<TotemCorePiece> core_;
    private final Set<TotemTile<TotemLecternPiece>> lecterns_;
    private final Set<TotemTile<TotemNameSignPiece>> nameSigns_;


    //CONSTRUCTOR
    public TotemBlueprint(long id, String name, Iterable<Tile> pieces, BoundingBox initialRegionBounds, Hierarchy hierarchy) {
        super(id, name, pieces);
        Processed processed = process(pieces);
        this.core_ = processed.core;
        this.lecterns_ = processed.lecterns;
        this.nameSigns_ = processed.nameSigns;
    }


    //GETTER
    public TotemTile<TotemCorePiece> core() {
        return this.core_;
    }
    public Set<TotemTile<TotemLecternPiece>> lecterns() {
        return this.lecterns_;
    }
    public Set<TotemTile<TotemNameSignPiece>> nameSings() {
        return this.nameSigns_;
    }


    //HELPERS
    private static Processed process(Iterable<Tile> elements) {
        TotemTile<TotemCorePiece> corePiece = null;
        List<TotemTile<TotemLecternPiece>> lecternPieces = new ArrayList<>();
        List<TotemTile<TotemNameSignPiece>> signPieces = new ArrayList<>();

        for (Tile t : elements) {
            Piece p = t.piece();

            if (p instanceof TotemCorePiece c) {
                if (corePiece != null) {
                    throw new InvalidBlueprintException("Totem blueprint must not contain more than one totem core piece");
                }
                corePiece = new TotemTile<>(t.offset(), c);
                continue;
            }
            if (p instanceof TotemLecternPiece l) {
                lecternPieces.add(new TotemTile<>(t.offset(), l));
                continue;
            }
            if (p instanceof TotemNameSignPiece s) {
                signPieces.add(new TotemTile<>(t.offset(), s));
            }
        }

        if (corePiece == null) {
            throw new InvalidBlueprintException("Totem blueprint must contain one totem core piece");
        }

        return new Processed(corePiece, Set.copyOf(lecternPieces), Set.copyOf(signPieces));
    }


    //SUBTYPES
    private record Processed(TotemTile<TotemCorePiece> core, Set<TotemTile<TotemLecternPiece>> lecterns, Set<TotemTile<TotemNameSignPiece>> nameSigns) {}
}
