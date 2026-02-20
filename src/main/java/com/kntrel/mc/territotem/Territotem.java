package com.kntrel.mc.territotem;

import com.kntrel.mc.territotem.blueprint.BlueprintRegistry;
import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.territotem.totem.TotemService;
import org.bukkit.plugin.java.JavaPlugin;

public final class Territotem extends JavaPlugin {


    //IMPLEMENTATION
    @Override
    public void onEnable() {
        RegionLib.enable(this);
        RegionContext ctx = RegionLib.createDefaultContext(this);

        BlueprintRegistry blueprintRegistry = new BlueprintRegistry(this);
        new TotemService(ctx, blueprintRegistry);
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

