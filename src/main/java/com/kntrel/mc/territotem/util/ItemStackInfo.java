package com.kntrel.mc.territotem.util;

import com.saicone.rtag.item.ItemObject;
import com.saicone.rtag.tag.TagCompound;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;
import java.util.Objects;

public record ItemStackInfo(Material material, int amount, @Nullable String itemSnbt) {

    public ItemStackInfo {
        Objects.requireNonNull(material, "material");
        if (material == Material.AIR) {
            throw new IllegalArgumentException("material cannot be AIR");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be >= 1");
        }
        if (itemSnbt != null && itemSnbt.isBlank()) {
            itemSnbt = null;
        }
    }

    public static ItemStackInfo fromItemStack(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");

        String itemSnbt = null;
        try {
            Object handle = ItemObject.getHandle(stack);
            itemSnbt = ItemObject.save(handle).toString();
        } catch (RuntimeException ignored) {}

        return new ItemStackInfo(stack.getType(), stack.getAmount(), itemSnbt);
    }

    public ItemStack toItemStack() {
        ItemStack fallback = new ItemStack(this.material, this.amount);
        if (this.itemSnbt == null) {
            return fallback;
        }

        try {
            Object tag = TagCompound.newTag(this.itemSnbt);
            Object handle = ItemObject.newItem(tag);
            if (handle == null) {
                return fallback;
            }

            ItemStack restored = ItemObject.asBukkitCopy(handle);
            if (restored == null) {
                return fallback;
            }

            restored.setAmount(this.amount);
            return restored;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
