package com.kntrel.mc.nbt.test;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.nbt.NBTTag;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MockNBTCompound extends MockNBTTag implements NBTCompound {

    //FIELDS
    private final Map<String, NBTTag> values_;


    //CONSTRUCTORS
    public MockNBTCompound() {
        this(new HashMap<>());
    }
    public MockNBTCompound(Map<String, NBTTag> values) {
        this.values_ = values;
    }


    //IMPLEMENTATION
    @Override public Map<String, NBTTag> get() {
        return Map.copyOf(this.values_);
    }
    @Override public Optional<NBTTag> getAt(Object... path) {
        return NBTTag.getAt(this, path);
    }
    @Override public Optional<NBTTag> getAt(String path) {
        return NBTTag.getAt(this, path);
    }
    @Override public int size() { return this.values_.size(); }
    @Override public boolean isEmpty() { return this.values_.isEmpty(); }
    @Override public boolean containsKey(Object key) { return this.values_.containsKey(key); }
    @Override public boolean containsValue(Object value) { return this.values_.containsValue(value); }
    @Override public NBTTag get(Object key) { return this.values_.get(key); }
    @Override public NBTTag put(String key, NBTTag value) { return this.values_.put(key, value); }
    @Override public NBTTag remove(Object key) { return this.values_.remove(key); }
    @Override public void putAll(Map<? extends String, ? extends NBTTag> m) { this.values_.putAll(m); }
    @Override public void clear() { this.values_.clear(); }
    @Override public Set<String> keySet() { return this.values_.keySet(); }
    @Override public Collection<NBTTag> values() { return this.values_.values(); }
    @Override public Set<Entry<String, NBTTag>> entrySet() { return this.values_.entrySet(); }
}
