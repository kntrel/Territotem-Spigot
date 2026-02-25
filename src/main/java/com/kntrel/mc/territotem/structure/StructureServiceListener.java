package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.territotem.structure.blueprint.BlueprintRegistration;
import com.kntrel.mc.territotem.structure.blueprint.TrackingInfo;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

class StructureServiceListener implements Listener {

    //CONSTANTS
    private static final Listener VOID_LISTENER = new Listener() {};


    //FIELDS
    private final StructureService service_;


    //CONSTRUCTOR
    StructureServiceListener(StructureService service) {
        this.service_ = service;
    }


    //API
    public void listen(BlueprintRegistration registration) {
        this.service_.getServer().getPluginManager().registerEvent(
                registration.eventClass(),
                VOID_LISTENER,
                EventPriority.NORMAL,
                (l, e) -> {
                    if (registration.appliesTo(e)) {
                        TrackingInfo tracking = registration.track(e);
                        this.service_.track(registration.blueprint(), tracking.where(), tracking.world(), tracking.who());
                    }
                },
                this.service_.getPlugin(),
                false
        );
    }


    //LISTENERS
    @EventHandler void onBlockPlaced(BlockPlaceEvent e) {
        Block b = e.getBlock();
        this.service_.handleBlockUpdate(
                e.getPlayer(),
                Vec3i.ofBlock(b),
                b.getWorld(),
                b.getType(),
                b.getBlockData().createBlockState()
        );
    }

    @EventHandler void onBlockBroken(BlockBreakEvent e) {
        Block b = e.getBlock();
        this.service_.handleBlockUpdate(
                e.getPlayer(),
                Vec3i.ofBlock(b),
                b.getWorld(),
                Material.AIR,
                b.getBlockData().createBlockState()
        );
    }

    @EventHandler void onChunkLoad(ChunkLoadEvent e) {
        this.service_.handleChunkLoad(e.getChunk());
    }

    @EventHandler void onChunkUnload(ChunkUnloadEvent e) {
        this.service_.handleChunkUnload(e.getChunk());
    }
}
