package com.kntrel.mc.nbt;

import com.kntrel.mc.nbt.impl.RTagNBT;
import com.kntrel.util.Numbers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface NBTTag {

    //FACTORY
    static NBTTag asTag(Object object) {
        return RTagNBT.asTag(object);
    }


    //CONTRACT
    Object handle();
    Object get();


    //HELPERS
    static Optional<NBTTag> getAt(NBTTag tag, Object... path) {
        NBTTag next = tag;
        for (Object token : path) {
            if (next instanceof NBTList list) {
                if (!(token instanceof Number num)) { return Optional.empty(); }
                if (Numbers.hasDecimals(num)) { return Optional.empty(); }

                int index = num.intValue();
                if (index < 0 || index >= list.size()) { return Optional.empty(); }

                int i = 0;
                NBTTag value = null;
                for (NBTTag element : list) {
                    if (i == index) {
                        value = element;
                        break;
                    }
                    i++;
                }
                if (value == null) { return Optional.empty(); }

                next = value;
                continue;
            }

            if (next instanceof NBTCompound compound) {
                next = compound.get(token.toString());
                if (next == null) { return Optional.empty(); }
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
