package com.kntrel.mc.nbt.test;

import com.kntrel.mc.nbt.NBTList;
import com.kntrel.mc.nbt.NBTTag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class MockNBTList extends MockNBTTag implements NBTList {

    //FIELDS
    private final List<NBTTag> values_;


    //CONSTRUCTORS
    public MockNBTList() {
        this(new ArrayList<>());
    }
    public MockNBTList(List<NBTTag> values) {
        this.values_ = values;
    }


    //IMPLEMENTATION
    @Override public List<NBTTag> get() {
        return List.copyOf(this.values_);
    }
    @Override public Optional<NBTTag> getAt(Object... path) {
        return NBTTag.getAt(this, path);
    }
    @Override public Optional<NBTTag> getAt(String path) {
        return NBTTag.getAt(this, path);
    }
    @Override public int size() { return this.values_.size(); }
    @Override public boolean isEmpty() { return this.values_.isEmpty(); }
    @Override public boolean contains(Object o) { return this.values_.contains(o); }
    @Override public Iterator<NBTTag> iterator() { return this.values_.iterator(); }
    @Override public Object[] toArray() { return this.values_.toArray(); }
    @Override public <T> T[] toArray(T[] a) { return this.values_.toArray(a); }
    @Override public boolean add(NBTTag tag) { return this.values_.add(tag); }
    @Override public boolean remove(Object o) { return this.values_.remove(o); }
    @Override public boolean containsAll(Collection<?> c) { return this.values_.containsAll(c); }
    @Override public boolean addAll(Collection<? extends NBTTag> c) { return this.values_.addAll(c); }
    @Override public boolean removeAll(Collection<?> c) { return this.values_.removeAll(c); }
    @Override public boolean retainAll(Collection<?> c) { return this.values_.retainAll(c); }
    @Override public void clear() { this.values_.clear(); }
}
