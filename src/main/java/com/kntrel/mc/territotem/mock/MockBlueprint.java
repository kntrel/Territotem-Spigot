package com.kntrel.mc.territotem.mock;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.state.StateMap;
import com.kntrel.mc.state.check.StateCheck;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.structure.blueprint.BlueprintRegistration;
import com.kntrel.mc.territotem.structure.blueprint.TrackingInfo;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTileWriter;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.BoundingBox;
import java.util.List;
import java.util.Map;

public class MockBlueprint {

    public static Blueprint get() {

        Piece lightningRod = new Piece() {
            @Override
            public boolean matches(WorldTile tile) {
                if (!Tag.LIGHTNING_RODS.isTagged(tile.blockType())) { return false; };
                return StateCheck.isEquals("facing", "up").test(tile.blockState());
            }

            @Override
            public void place(WorldTileWriter tile) {
                tile.setBlock(Material.LIGHTNING_ROD, StateMap.of(Map.entry("facing", "up")));
            }
        };

        List<Tile> elms = List.of(
                new Tile(Vec3i.zeroes(), lightningRod),
                new Tile(new Vec3i(0, -1, 0), Piece.block(Material.OBSIDIAN)),
                new Tile(new Vec3i(0, -2, 0), Piece.block(Material.OBSIDIAN))
        );

        BoundingBox bounds = new BoundingBox(-10, -10, -10, 10, 10, 10);

        Hierarchy hierarchy = new Hierarchy(1L, "mock_hierarchy");

        return new Blueprint(1, "mock_blueprint", elms, bounds, hierarchy);

    }

    public static BlueprintRegistration registration() {
        return BlueprintRegistration.forBlueprint(get())
                .onCompletion((s, e) -> {
                    if (e instanceof Player) {
                        e.sendMessage("completed " + s.blueprint().name());
                    }
                })
                .onBlockPiecePlaced(b -> b.pieceAt(0, 2, 0));
    }

}
