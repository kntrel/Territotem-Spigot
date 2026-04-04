package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.territotem.totem.region.Expansion;
import com.kntrel.mc.territotem.util.ItemStackInfo;
import org.jspecify.annotations.Nullable;
import java.util.Objects;

record TotemGrowthEntry(Expansion expansion, @Nullable ItemStackInfo refundStack, double dropBackRate) {

    TotemGrowthEntry {
        Objects.requireNonNull(expansion, "expansion");
        if (!Double.isFinite(dropBackRate) || dropBackRate < 0d || dropBackRate > 1d) {
            throw new IllegalArgumentException("dropBackRate must be between 0 and 1");
        }
    }

    static TotemGrowthEntry nonRefundable(Expansion expansion) {
        return new TotemGrowthEntry(expansion, null, 0d);
    }

    public int refundQuantity() {
        return (this.refundStack == null) ? 0 : this.refundStack.amount();
    }
}
