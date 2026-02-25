package com.kntrel.mc.territotem.structure.piece;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.territotem.structure.worldTile.EntityState;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTileWriter;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

final class PieceImpl {

    record BlockPiece(Material material, NBTCheck nbtCheck, @Nullable NBTCompound placeNbt) implements Piece {

        BlockPiece(Material material, NBTCompound nbt) {
            this(material, NBTCheck.matches(nbt), nbt);
        }
        BlockPiece(Material material) {
            this(material, t -> true, null);
        }

        @Override
        public boolean matches(WorldTile tile) {
            BlockState block = tile.block();
            if (block.getType() != this.material) { return false; }
            return this.nbtCheck.test(tile.blockNbt());
        }

        @Override
        public void place(WorldTileWriter tile) {
            tile.setBlock(this.material, this.placeNbt);
        }
    }

    record EntityPiece(EntityType type, @Nullable Vector offset, NBTCheck nbtCheck, @Nullable NBTCompound placeNbt) implements Piece {

        EntityPiece(EntityType type, NBTCompound nbt) {
            this(type, null, NBTCheck.matches(nbt), nbt);
        }
        EntityPiece(EntityType type) {
            this(type, null, e -> true, null);
        }
        EntityPiece(EntityType type, Vector offset) {
            this(type, offset, e -> true, null);
        }
        EntityPiece(EntityType type, Vector offset, NBTCompound nbt) {
            this(type, offset, NBTCheck.matches(nbt), nbt);
        }

        @Override
        public boolean matches(WorldTile tile) {
            for (EntityState entity : tile.entities()) {
                if (entity.type() == this.type && this.nbtCheck.test(entity.nbt())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void place(WorldTileWriter tile) {
            if (this.offset != null) {
                tile.spawnEntity(this.type, this.offset, this.placeNbt);
            } else {
                tile.spawnEntity(this.type, this.placeNbt);
            }
        }
    }

    static final class AnyPiece implements Piece {
        static final AnyPiece INSTANCE = new AnyPiece();
        private AnyPiece() {}

        @Override public boolean matches(WorldTile tile) { return true; }
        @Override public void place(WorldTileWriter tile) { /*nothing*/ }
    }

    static final class EmptyPiece implements Piece {
        static final EmptyPiece INSTANCE = new EmptyPiece();
        private EmptyPiece() {}

        @Override public boolean matches(WorldTile tile) { return tile.block().getType() == Material.AIR; }
        @Override public void place(WorldTileWriter tile) { tile.breakBlock(); }
    }

    record Either(Piece... options) implements Piece {

        Either {
            if (options.length < 1) {
                throw new IllegalArgumentException("At least one Piece option is required");
            }
        }
        Either (Collection<Piece> options) {
            this(options.toArray(new Piece[0]));
        }

        @Override public boolean matches(WorldTile tile) {
            for (Piece piece : this.options) {
                if (piece.matches(tile)) { return true; }
            }
            return false;
        }

        @Override public void place(WorldTileWriter tile) {
            this.options[0].place(tile);
        }
    }
}
