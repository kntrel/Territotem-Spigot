package com.kntrel.mc.territotem.region;

import org.bukkit.Material;

import java.util.Objects;
import java.util.Set;

public record RegionFeatureMaterialsConfig(Set<Material> enforcedButtons, Set<Material> leverLockerBlocks) {

    public RegionFeatureMaterialsConfig {
        Objects.requireNonNull(enforcedButtons, "enforcedButtons");
        Objects.requireNonNull(leverLockerBlocks, "leverLockerBlocks");
        enforcedButtons = Set.copyOf(enforcedButtons);
        leverLockerBlocks = Set.copyOf(leverLockerBlocks);
    }
}
