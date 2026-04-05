package com.kntrel.mc.territotem.region;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.display.BlockDisplayRegionDisplayer;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

public class ColoredRegionDisplayer extends BlockDisplayRegionDisplayer {

    public ColoredRegionDisplayer(RegionContext regionContext) {
        super(regionContext);
    }

    @Override
    protected BlockData getFaceBlockData(Region region, BlockFace facing) {
        RegionColor color = RegionColors.get(region);
        if (color == null) {
            return super.getFaceBlockData(region, facing);
        }

        Material material = switch (color.dyeColor()) {
            case WHITE -> Material.WHITE_STAINED_GLASS;
            case ORANGE -> Material.ORANGE_STAINED_GLASS;
            case MAGENTA -> Material.MAGENTA_STAINED_GLASS;
            case LIGHT_BLUE -> Material.LIGHT_BLUE_STAINED_GLASS;
            case YELLOW -> Material.YELLOW_STAINED_GLASS;
            case LIME -> Material.LIME_STAINED_GLASS;
            case PINK -> Material.PINK_STAINED_GLASS;
            case GRAY -> Material.GRAY_STAINED_GLASS;
            case LIGHT_GRAY -> Material.LIGHT_GRAY_STAINED_GLASS;
            case CYAN -> Material.CYAN_STAINED_GLASS;
            case PURPLE -> Material.PURPLE_STAINED_GLASS;
            case BLUE -> Material.BLUE_STAINED_GLASS;
            case BROWN -> Material.BROWN_STAINED_GLASS;
            case GREEN -> Material.GREEN_STAINED_GLASS;
            case RED -> Material.RED_STAINED_GLASS;
            case BLACK -> Material.BLACK_STAINED_GLASS;
            default -> null;
        };

        if (material == null) {
            return super.getFaceBlockData(region, facing);
        }

        return material.createBlockData();
    }
}
