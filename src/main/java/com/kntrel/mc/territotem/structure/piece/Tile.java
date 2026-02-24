package com.kntrel.mc.territotem.structure.piece;

import com.kntrel.util.Vec3i;
import org.bukkit.World;

public class Tile implements Piece, Comparable<Tile> {

    //FIELDS
    private final Vec3i offset_;
    private final Piece piece_;


    //CONSTRUCTOR
    public Tile(Vec3i offset, Piece piece) {
        this.offset_ = offset;
        this.piece_ = (piece instanceof Tile tile) ? tile.piece_ : piece;
    }
    public Tile(int x, int y, int z, Piece element) {
        this(new Vec3i(x, y ,z), element);
    }



    //GETTERS
    public int x() { return this.offset_.x(); }
    public int y() { return this.offset_.y(); }
    public int z() { return this.offset_.z(); }
    public Vec3i offset() { return this.offset_; }
    public Tile withOffset(Vec3i offset) { return new Tile(offset, this.piece_); }


    //IMPLEMENTATION
    @Override public boolean matches(WorldTile tile) {
        return this.piece_.matches(tile);
    }
    @Override public void place(WorldTileWriter tile) { this.piece_.place(tile); }
    @Override public int compareTo(Tile o) { return this.offset_.compareTo(o.offset_); }
}
