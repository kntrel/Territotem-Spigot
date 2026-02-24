package com.kntrel.mc.territotem.structure.piece;

import com.kntrel.mc.nbt.NBTCompound;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.jspecify.annotations.Nullable;

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
}
