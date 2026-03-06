package com.kntrel.mc.territotem.test.mock;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;

import java.util.List;

public final class MockBlueprints {

    private MockBlueprints() {}

    public static Blueprint square4() {
        List<Tile> elements = List.of(
                new Tile(new Vec3i(0, 0, 0), Piece.block(Material.LIGHTNING_ROD)),
                new Tile(new Vec3i(1, 0, 0), Piece.block(Material.OBSIDIAN)),
                new Tile(new Vec3i(0, 0, 1), Piece.block(Material.CRYING_OBSIDIAN)),
                new Tile(new Vec3i(1, 0, 1), Piece.block(Material.GOLD_BLOCK))
        );

        return new Blueprint(
                99,
                "test_square4",
                elements
        );
    }
}
