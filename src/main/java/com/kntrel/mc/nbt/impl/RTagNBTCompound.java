package com.kntrel.mc.nbt.impl;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.nbt.NBTTag;
import com.saicone.rtag.tag.TagCompound;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RTagNBTCompound extends RTagNBT implements NBTCompound {

    //FIELDS
    private Map<String, NBTTag> cache_;


    //CONSTRUCTORS
    public RTagNBTCompound(Object tag) {
        super(tag);
        if (!TagCompound.isTagCompound(tag)) {
            throw new ClassCastException("Passed object is not an NBTCompound");
        }
        this.compute();
    }
    public RTagNBTCompound() {
        this(TagCompound.newTag());
    }


    //IMPLEMENTATION
    @Override public Map<String, NBTTag> get() {
        return Map.copyOf(this.cache_);
    }
    @Override public int size() {
        return this.cache_.size();
    }
    @Override public boolean isEmpty() {
        return this.cache_.isEmpty();
    }
    @Override public boolean containsKey(Object key) {
        return this.cache_.containsKey(key);
    }
    @Override public boolean containsValue(Object value) {
        return this.cache_.containsValue(value);
    }
    @Override public NBTTag get(Object key) {
        return this.cache_.get(key);
    }
    @Override public NBTTag put(String key, NBTTag value) {
        TagCompound.set(this.handle(), key, value.get());
        return this.cache_.put(key, value);
    }
    @Override public NBTTag remove(Object key) {
        if (!(key instanceof String keyStr)) { return null; }
        TagCompound.remove(this.handle(), keyStr);
        return this.cache_.remove(key);
    }
    @Override public void putAll(Map<? extends String, ? extends NBTTag> m) {
        for (Entry<? extends String, ? extends NBTTag> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }
    @Override public void clear() {
        TagCompound.clear(this.handle());
        this.cache_.clear();
    }
    @Override public @NonNull Set<String> keySet() {
        return this.cache_.keySet();
    }
    @Override public @NonNull Collection<NBTTag> values() {
        return this.cache_.values();
    }
    @Override public @NonNull Set<Entry<String, NBTTag>> entrySet() {
        return this.cache_.entrySet();
    }
    @Override public Optional<NBTTag> getAt(Object... path) {
        return NBTTag.getAt(this, path);
    }
    @Override public Optional<NBTTag> getAt(String path) {
        return NBTTag.getAt(this, path);
    }


    //HELPERS
    private void compute() {
        this.cache_ = new HashMap<>();
        for (var entry : TagCompound.getValue(this.handle()).entrySet()) {
            this.cache_.put(entry.getKey(), NBTTag.asTag(entry.getValue()));
        }
    }
}
