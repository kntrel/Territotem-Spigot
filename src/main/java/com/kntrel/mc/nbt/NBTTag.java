package com.kntrel.mc.nbt;

import com.kntrel.util.Numbers;
import com.saicone.rtag.tag.TagBase;
import com.saicone.rtag.tag.TagCompound;
import com.saicone.rtag.tag.TagList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class NBTTag {

    //FACTORY
    public static NBTTag asTag(Object object) {
        if (object == null) { return null; }

        // Already an NBT handle?
        if (TagCompound.isTagCompound(object)) { return new NBTCompound(object); }
        if (TagList.isTagList(object)) { return new NBTList(object); }
        if (TagBase.isTag(object)) { return new NBTPrimitive(object); }

        // Convert Java value -> NBT handle once
        Object tag;
        try {
            tag = TagBase.newTag(object);
        } catch (Throwable t) {
            throw new IllegalArgumentException(
                    "Unsupported value for NBT conversion: " + object.getClass().getName(), t
            );
        }

        if (tag == null) {
            throw new IllegalArgumentException(
                    "Unsupported value for NBT conversion: " + object.getClass().getName()
            );
        }

        // Now it *must* be a tag (otherwise something is wrong)
        if (TagCompound.isTagCompound(tag)) { return new NBTCompound(tag); }
        if (TagList.isTagList(tag)) { return new NBTList(tag); }
        if (TagBase.isTag(tag)) { return new NBTPrimitive(tag); }

        throw new IllegalStateException(
                "TagBase.newTag produced a non-tag: " + tag.getClass().getName()
        );
    }


    //FIELDS
    private final Object handle_;


    //CONSTRUCTOR
    protected NBTTag(Object tag) {
        this.handle_ = tag;
    }


    //API
    public Object handle() { return this.handle_; }
    public Object get() {
        return TagBase.getValue(this.handle_);
    }
    @Override public String toString() {
        return this.handle_.toString();
    }
    @Override public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o == this) { return true; }
        if (!this.getClass().isInstance(o)) { return false; }
        return ((NBTTag) o).handle_.equals(this.handle_);
    }
    @Override public int hashCode() {
        return this.handle_.hashCode();
    }


    //HELPERS
    static Optional<NBTTag> getAt(NBTTag tag, Object... path) {
        NBTTag next = tag;
        for (Object token : path) {
            if (next instanceof NBTList list) {
                if (!(token instanceof Number num)) { return Optional.empty(); }
                if (Numbers.hasDecimals(num)) { return Optional.empty(); }
                next = list.get().get(num.intValue());
                continue;
            }

            if (next instanceof NBTCompound compound) {
                next = compound.get(token.toString());
                continue;
            }

            return Optional.empty();
        }

        return Optional.of(next);
    }
    static Optional<NBTTag> getAt(NBTTag tag, String path) {
        String[] parts = path.split("\\.");
        List<Object> list = new ArrayList<>(parts.length);

        for (String p : parts) {
            p = p.trim();
            if (p.isEmpty()) { continue; }

            Integer index = null;
            try { index = Integer.parseInt(p); } catch (NumberFormatException ignored) {}

            if (index != null) {
                list.add(index);
            } else {
                list.add(p);
            }
        }

        return getAt(tag, list.toArray());
    }
}
