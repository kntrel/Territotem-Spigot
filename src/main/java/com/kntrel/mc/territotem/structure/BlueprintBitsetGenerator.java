package com.kntrel.mc.territotem.structure;

import com.kntrel.util.BitSet3D;
import java.util.HashMap;
import java.util.Map;

class BlueprintBitsetGenerator {

    //FIELDS
    private final Map<Long, BitSet3D> cache_;


    //CONSTRUCTOR
    public BlueprintBitsetGenerator() {
        this.cache_ = new HashMap<>();
    }


    //API
    public BitSet3D generate(Blueprint blueprint) {
        long id = blueprint.id();
        BitSet3D out = this.cache_.get(id);
        if (out != null) { return out.clone(); }

        out = generateInner(blueprint);
        this.cache_.put(id, out);
        return out.clone();
    }


    //HELPERS
    private static BitSet3D generateInner(Blueprint blueprint) {
        BitSet3D out = new BitSet3D(blueprint.dimensions());
        blueprint.elementsByOffset().forEach((v, e) -> {
            if (!(e instanceof Piece.Any)) { out.set(v); }
        });
        return out;
    }
}
