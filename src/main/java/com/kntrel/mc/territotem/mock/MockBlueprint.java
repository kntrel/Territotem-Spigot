package com.kntrel.mc.territotem.mock;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.territotem.blueprint.Blueprint;
import com.kntrel.mc.territotem.blueprint.BlueprintCoreTile;
import com.kntrel.mc.territotem.blueprint.BlueprintElement;
import com.kntrel.mc.territotem.blueprint.BlueprintTile;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;
import java.util.List;

public class MockBlueprint {

    public static Blueprint get() {

        List<BlueprintTile> elms = List.of(
                new BlueprintCoreTile(Vec3i.zeroes(), new BlueprintElement.Core(Material.LIGHTNING_ROD)),
                new BlueprintTile(new Vec3i(0, -1, 0), new BlueprintElement.Block(Material.OBSIDIAN)),
                new BlueprintTile(new Vec3i(0, -2, 0), new BlueprintElement.Block(Material.OBSIDIAN))
        );

        BoundingBox bounds = new BoundingBox(-10, -10, -10, 10, 10, 10);

        Hierarchy hierarchy = new Hierarchy(1L, "mock_hierarchy");

        return new Blueprint(1, "mock_blueprint", elms, bounds, hierarchy, List.of());

    }

}
