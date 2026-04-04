package com.kntrel.mc.territotem.totem.region;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.nbt.check.NBTCheck;
import com.kntrel.mc.nbt.impl.RTagNBTCompound;
import com.saicone.rtag.item.ItemObject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class ExpansionTable {

    //FIELDS
    private final List<Row> rows_;


    //FACTORY
    public static ExpansionTable empty() {
        return new ExpansionTable(List.of());
    }
    public static ExpansionTable of(Row... rows) {
        return new ExpansionTable(rows);
    }
    public static Row row(
            Material item,
            int consumption,
            @Nullable NBTCompound nbt,
            double expansionMin,
            double expansionMax,
            @Nullable Double dropBackRate
    ) {
        return new Row(item, consumption, nbt, expansionMin, expansionMax, dropBackRate);
    }
    public static Row row(Material item, int consumption, @Nullable NBTCompound nbt, double expansionMin, double expansionMax) {
        return new Row(item, consumption, nbt, expansionMin, expansionMax, null);
    }
    public static Row row(Material item, double expansionMin, double expansionMax) {
        return new Row(item, 1, null, expansionMin, expansionMax, null);
    }


    //CONSTRUCTOR
    public ExpansionTable(Collection<Row> rows) {
        this.rows_ = (rows == null) ? List.of() : List.copyOf(rows);
    }
    public ExpansionTable(Row... rows) {
        this.rows_ = List.of(rows);
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
            double expansionMax,
            @Nullable Double dropBackRate
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
            if (dropBackRate != null && (!Double.isFinite(dropBackRate) || dropBackRate < 0d || dropBackRate > 1d)) {
                throw new IllegalArgumentException("dropBackRate must be between 0 and 1");
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

        public double dropBackRateOr(double fallback) {
            return (this.dropBackRate != null) ? this.dropBackRate : fallback;
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
}
