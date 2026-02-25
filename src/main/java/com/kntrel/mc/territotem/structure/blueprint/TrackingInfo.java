package com.kntrel.mc.territotem.structure.blueprint;

import com.kntrel.util.Vec3i;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public record TrackingInfo(Vec3i where, World world, Entity who) {
}
