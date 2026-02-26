package com.kntrel.mc.territotem.mock;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;
import java.util.List;

public class MockBlueprint {

    public static Blueprint get() {

        List<Tile> elms = List.of(
                new Tile(Vec3i.zeroes(), Piece.block(Material.LIGHTNING_ROD)),
                new Tile(new Vec3i(0, -1, 0), Piece.block(Material.OBSIDIAN)),
                new Tile(new Vec3i(0, -2, 0), Piece.block(Material.OBSIDIAN))
        );

        BoundingBox bounds = new BoundingBox(-10, -10, -10, 10, 10, 10);

        Hierarchy hierarchy = new Hierarchy(1L, "mock_hierarchy");

        return new Blueprint(1, "mock_blueprint", elms, bounds, hierarchy);

    }

}
