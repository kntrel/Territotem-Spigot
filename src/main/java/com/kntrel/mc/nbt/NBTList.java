package com.kntrel.mc.nbt;

import com.saicone.rtag.tag.TagList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class NBTList extends NBTTag implements Collection<NBTTag> {

    //FIELDS
    private List<NBTTag> cache_;


    //CONSTRUCTOR
    public NBTList(Object tag) {
        super(tag);
        if (!TagList.isTagList(tag)) {
            throw new ClassCastException("Passed object is not an NBTList");
        }
        this.compute();
    }


    //IMPLEMENTATION
    @Override public List<NBTTag> get() {
        return List.copyOf(cache_);
    }
    @Override
    public int size() {
        return this.cache_.size();
    }
    @Override public boolean isEmpty() {
        return this.cache_.isEmpty();
    }
    @Override public boolean contains(Object o) {
        return this.cache_.contains(o);
    }
    @Override public Iterator<NBTTag> iterator() {
        return this.cache_.iterator();
    }
    @Override public NBTTag[] toArray() {
        return this.toArray(new NBTTag[0]);
    }
    @Override public <T> T[] toArray(T[] a) {
        return this.cache_.toArray(a);
    }
    @Override public boolean add(NBTTag nbtTag) {
        TagList.add(this.handle(), nbtTag.get());
        this.cache_.add(nbtTag);
        return true;
    }
    @Override public boolean remove(Object o) {
        if (!(o instanceof NBTTag tag)) {
            return false;
        }
        int index = this.cache_.indexOf(o);
        if (index < 0) {
            return false;
        }
        TagList.remove(this.handle(), index);
        this.cache_.remove(index);
        return true;
    }
    @Override public boolean containsAll(Collection<?> c) {
        return this.cache_.containsAll(c);
    }
    @Override public boolean addAll(Collection<? extends NBTTag> c) {
        if (c.isEmpty()) {
            return false;
        }
        for (NBTTag tag : c) {
            TagList.add(this.handle(), tag.get());
            this.cache_.add(tag);
        }
        return true;
    }
    @Override public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (int i = this.cache_.size() - 1; i >= 0; i--) {
            if (c.contains(this.cache_.get(i))) {
                TagList.remove(this.handle(), i);
                this.cache_.remove(i);
                changed = true;
            }
        }
        return changed;
    }
    @Override public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        for (int i = this.cache_.size() - 1; i >= 0; i--) {
            if (!c.contains(this.cache_.get(i))) {
                TagList.remove(this.handle(), i);
                this.cache_.remove(i);
                changed = true;
            }
        }
        return changed;
    }
    @Override public void clear() {
        TagList.clear(this.handle());
        this.cache_.clear();
    }

    //HELPERS
    private void compute() {
        this.cache_ = new ArrayList<>(
                TagList.getValue(this.handle()).stream()
                        .map(NBTTag::asTag)
                        .toList()
        );
    }
}
