package com.kntrel.mc.territotem.blueprint;

import com.kntrel.util.Vec3i;

public class BlueprintCoreTile extends BlueprintTile {

    public BlueprintCoreTile(Vec3i offset, BlueprintElement.Core element) {
        super(offset, element);
    }

    public BlueprintCoreTile(int x, int y, int z, BlueprintElement.Core element) {
        super(x, y, z, element);
    }

    @Override public BlueprintElement.Core element() {
        return (BlueprintElement.Core) super.element();
    }

    @Override public BlueprintCoreTile withOffset(Vec3i offset) {
        return new BlueprintCoreTile(offset, this.element());
    }
}
