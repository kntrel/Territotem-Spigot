package com.kntrel.mc.territotem.region;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.runical.core.argument.Argument;
import com.kntrel.mc.runical.core.argument.Translatable;
import com.kntrel.mc.runical.core.argument.TranslationProperty;
import java.util.UUID;

@Translatable
public record RegionArgument(
        Long id,
        @TranslationProperty String name,
        @TranslationProperty double minX,
        @TranslationProperty double minY,
        @TranslationProperty double minZ,
        @TranslationProperty double maxX,
        @TranslationProperty double maxY,
        @TranslationProperty double maxZ,
        @TranslationProperty double height,
        @TranslationProperty double widthX,
        @TranslationProperty double widthZ,
        @TranslationProperty String nameSpace,
        @TranslationProperty World world,
        @TranslationProperty RegionColor color
) {

    public static RegionArgument from(Region region) {
        return new RegionArgument(
                region.getId(),
                region.getName(),
                region.getMinX(),
                region.getMinY(),
                region.getMinZ(),
                region.getMaxX(),
                region.getMaxY(),
                region.getMaxZ(),
                region.getHeight(),
                region.getWidthX(),
                region.getWidthZ(),
                region.getContext().getNamespace(),
                new World(region.getWorld().getName(), region.getWorld().getUID()),
                RegionColors.getOrDefault(region)
        );
    }

    public static Argument of(String rootKey, RegionArgument region) {
        return Argument.of(rootKey, region);
    }

    public static Argument of(RegionArgument region) {
        return of("region", region);
    }

    public static Argument of(String rootKey, Region region) {
        return of(rootKey, from(region));
    }

    public static Argument of(Region region) {
        return of("region", region);
    }

    @Translatable
    public record World(
            @TranslationProperty String name,
            @TranslationProperty UUID id
    ) {}

    @TranslationProperty(root = true)
    public String rootProperty() { return "[" + this.displayId() + "] " + this.name; }

    @TranslationProperty("id")
    public String displayId() {
        return (this.id == null) ? "?" : this.id.toString();
    }

    @TranslationProperty
    public String colorizedName() {
        return this.color.apply(this.name);
    }

}
