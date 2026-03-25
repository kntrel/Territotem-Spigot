package com.kntrel.mc.territotem.totem.region;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.nbt.check.NBTCheck;
import com.kntrel.mc.nbt.impl.RTagNBTCompound;
import com.saicone.rtag.item.ItemObject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class ExpansionTable {

    private final List<Row> rows_;

    public ExpansionTable(List<Row> rows) {
        this.rows_ = List.copyOf((rows == null) ? List.of() : rows);
    }

    public static ExpansionTable empty() {
        return new ExpansionTable(List.of());
    }

    public List<Row> rows() {
        return this.rows_;
    }

    public Optional<Row> findMatch(@Nullable ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return Optional.empty();
        }

        Object handle = ItemObject.getHandle(stack);
        NBTCompound fullItemNbt = new RTagNBTCompound(ItemObject.save(handle));
        Object rawCustomData = ItemObject.getCustomDataTag(handle);
        NBTCompound customItemNbt = (rawCustomData == null) ? null : new RTagNBTCompound(rawCustomData);

        for (Row row : this.rows_) {
            if (row.matches(stack, fullItemNbt, customItemNbt)) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    public record Row(
            Material item,
            int consumption,
            @Nullable NBTCompound nbt,
            double expansionMin,
            double expansionMax
    ) {

        public Row {
            Objects.requireNonNull(item, "item");
            if (consumption < 1) {
                throw new IllegalArgumentException("consumption must be >= 1");
            }
            validateExpansion("expansionMin", expansionMin);
            validateExpansion("expansionMax", expansionMax);
            if (expansionMin > expansionMax) {
                throw new IllegalArgumentException("expansionMin must be <= expansionMax");
            }
        }

        public boolean matches(ItemStack stack) {
            return ExpansionTable.of(this).findMatch(stack).isPresent();
        }

        boolean matches(
                @Nullable ItemStack stack,
                @Nullable NBTCompound fullItemNbt,
                @Nullable NBTCompound customItemNbt
        ) {
            if (stack == null || stack.getType() != this.item) {
                return false;
            }
            return this.matches(fullItemNbt, customItemNbt);
        }

        boolean matches(@Nullable NBTCompound fullItemNbt, @Nullable NBTCompound customItemNbt) {
            if (this.nbt == null || this.nbt.isEmpty()) {
                return true;
            }

            NBTCheck check = NBTCheck.matches(this.nbt);
            return (fullItemNbt != null && check.test(fullItemNbt))
                    || (customItemNbt != null && check.test(customItemNbt));
        }

        public double pickExpansionScalar() {
            if (this.expansionMin == this.expansionMax) {
                return this.expansionMin;
            }
            return this.expansionMin + (ThreadLocalRandom.current().nextDouble() * (this.expansionMax - this.expansionMin));
        }

        private static void validateExpansion(String name, double value) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException(name + " must be finite");
            }
            if (value < 0d) {
                throw new IllegalArgumentException(name + " must be >= 0");
            }
        }
    }

    private static ExpansionTable of(Row row) {
        return new ExpansionTable(List.of(row));
    }
}
