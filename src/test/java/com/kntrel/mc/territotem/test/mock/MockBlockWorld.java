package com.kntrel.mc.territotem.test.mock;

import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.World;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class MockBlockWorld {

    private final Map<Vec3i, Material> blocks = new HashMap<>();

    public void set(Vec3i location, Material material) {
        this.blocks.put(location, material);
    }

    public World asWorld() {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class[]{World.class},
                (proxy, method, args) -> {
                    if ("getBlockAt".equals(method.getName())) {
                        Vec3i location = new Vec3i((Integer) args[0], (Integer) args[1], (Integer) args[2]);
                        Material material = blocks.getOrDefault(location, Material.AIR);
                        return Proxy.newProxyInstance(
                                method.getReturnType().getClassLoader(),
                                new Class[]{method.getReturnType()},
                                (blockProxy, blockMethod, blockArgs) -> {
                                    if ("getType".equals(blockMethod.getName())) {
                                        return material;
                                    }
                                    if ("toString".equals(blockMethod.getName())) {
                                        return "MockBlock{" + location + ":" + material + "}";
                                    }
                                    return defaultValue(blockMethod.getReturnType());
                                }
                        );
                    }
                    if ("toString".equals(method.getName())) {
                        return "MockWorld";
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
