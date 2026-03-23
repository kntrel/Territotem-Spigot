package com.kntrel.mc.territotem.region;

public record RegionEnterTitleConfig(boolean enabled, int fadeIn, int stay, int fadeOut) {

    public RegionEnterTitleConfig {
        if (fadeIn < 0 || stay < 0 || fadeOut < 0) {
            throw new IllegalArgumentException("Title timings must be >= 0");
        }
    }
}
