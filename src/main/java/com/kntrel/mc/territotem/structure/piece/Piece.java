package com.kntrel.mc.territotem.structure.piece;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.nbt.check.NBTCheck;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTileWriter;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import java.util.Collection;

public interface Piece {

    //FACTORY
    static Piece any() { return PieceImpl.AnyPiece.INSTANCE; }

    static Piece empty() { return PieceImpl.EmptyPiece.INSTANCE; }

    static Piece either(Piece... options) {
        if (options.length == 1) {
            return options[0];
        }
        return new PieceImpl.Either(options);
    }

    static Piece either(Collection<Piece> options) {
        return Piece.either(options.toArray(new Piece[0]));
    }

    static Piece block(Material material) {
        return new PieceImpl.BlockPiece(material);
    }

    static Piece block(Material material, NBTCompound nbt) {
        return new PieceImpl.BlockPiece(material, nbt);
    }

    static Piece block(Material material, NBTCompound placeNbt, NBTCheck matchCheck) {
        return new PieceImpl.BlockPiece(material, matchCheck, placeNbt);
    }

    static Piece entity(EntityType type) {
        return new PieceImpl.EntityPiece(type);
    }

    static Piece entity(EntityType type, NBTCompound nbt) {
        return new PieceImpl.EntityPiece(type, nbt);
    }

    static Piece entity(EntityType type, NBTCompound placeNbt, NBTCheck matchCheck) {
        return new PieceImpl.EntityPiece(type, null, matchCheck, placeNbt);
    }

    static Piece entity(EntityType type, Vector offset) {
        return new PieceImpl.EntityPiece(type, offset);
    }

    static Piece entity(EntityType type, Vector offset, NBTCompound nbt) {
        return new PieceImpl.EntityPiece(type, offset, nbt);
    }

    static Piece entity(EntityType type, Vector offset, NBTCompound placeNbt, NBTCheck matchCheck) {
        return new PieceImpl.EntityPiece(type, offset, matchCheck, placeNbt);
    }


    //CONTRACT
    boolean matches(WorldTile tile);
    void place(WorldTileWriter tile);

}
