package com.kntrel.mc.territotem.config;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.nbt.NBTList;
import com.kntrel.mc.nbt.NBTTag;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import java.util.List;
import java.util.Map;

public final class NbtYamlParser {

    private NbtYamlParser() {}

    public static @Nullable NBTCompound compound(@Nullable Object raw, String path, Logger logger) {
        if (raw == null) {
            logger.error("Invalid null NBT value for config key '{}'. Expected a map. Ignoring row.", path);
            return null;
        }
        NBTTag tag = parse(raw, path, logger);
        if (tag instanceof NBTCompound compound) {
            return compound;
        }

        logger.error("Invalid NBT value '{}' for config key '{}'. Expected a compound. Ignoring row.", raw, path);
        return null;
    }

    private static @Nullable NBTTag parse(@Nullable Object raw, String path, Logger logger) {
        if (raw instanceof Map<?, ?> rawMap) {
            NBTCompound compound = NBTCompound.create();
            for (var entry : rawMap.entrySet()) {
                Object key = entry.getKey();
                if (key == null) {
                    logger.error("Invalid null NBT key at '{}'.", path);
                    return null;
                }
                NBTTag child = parse(entry.getValue(), path + "." + key, logger);
                if (child == null) {
                    return null;
                }
                compound.put(key.toString(), child);
            }
            return compound;
        }

        if (raw instanceof List<?> rawList) {
            NBTList list = NBTList.create();
            for (int i = 0; i < rawList.size(); i++) {
                NBTTag child = parse(rawList.get(i), path + "[" + i + "]", logger);
                if (child == null) {
                    return null;
                }
                list.add(child);
            }
            return list;
        }

        if (raw == null) {
            logger.error("Invalid null NBT value at '{}'.", path);
            return null;
        }

        try {
            return NBTTag.asTag(raw);
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid NBT value '{}' at '{}'.", raw, path);
            return null;
        }
    }
}
