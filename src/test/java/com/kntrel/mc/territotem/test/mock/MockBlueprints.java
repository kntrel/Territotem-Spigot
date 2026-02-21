package com.kntrel.mc.territotem.test.mock;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.territotem.blueprint.Blueprint;
import com.kntrel.mc.territotem.blueprint.BlueprintCoreTile;
import com.kntrel.mc.territotem.blueprint.BlueprintElement;
import com.kntrel.mc.territotem.blueprint.BlueprintTile;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;

import java.util.List;

public final class MockBlueprints {

    private MockBlueprints() {}

    public static Blueprint square4() {
        List<BlueprintTile> elements = List.of(
                new BlueprintCoreTile(new Vec3i(0, 0, 0), new BlueprintElement.Core(Material.LIGHTNING_ROD)),
                new BlueprintTile(new Vec3i(1, 0, 0), new BlueprintElement.Block(Material.OBSIDIAN)),
                new BlueprintTile(new Vec3i(0, 0, 1), new BlueprintElement.Block(Material.CRYING_OBSIDIAN)),
                new BlueprintTile(new Vec3i(1, 0, 1), new BlueprintElement.Block(Material.GOLD_BLOCK))
        );

        return new Blueprint(
                99,
                "test_square4",
                elements,
                new BoundingBox(0, 0, 0, 2, 1, 2),
                new Hierarchy(1L, "test_hierarchy"),
                List.of()
        );
    }
}
