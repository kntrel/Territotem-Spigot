package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.territotem.event.StructureCompletedEvent;
import com.kntrel.mc.territotem.event.StructureDestroyedEvent;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.structure.blueprint.BlueprintRegistration;
import com.kntrel.mc.territotem.structure.blueprint.TrackingInfo;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.EventExecutor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class StructureServiceListener implements Listener {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureServiceListener.class);
    private static final Listener VOID_LISTENER = new Listener() {};


    //FIELDS
    private final StructureService service_;
    private final Map<Long, BlueprintRegistration> registrations_;
    private final Map<Class<? extends Event>, Executor> executors_;


    //CONSTRUCTOR
    StructureServiceListener(StructureService service) {
        this.service_ = service;
        this.registrations_ = new HashMap<>();
        this.executors_ = new HashMap<>();
    }


    //API
    public void listen(BlueprintRegistration registration) {
        Blueprint blueprint = registration.blueprint();
        this.registrations_.put(blueprint.id(), registration);

        Executor executor = this.executors_.get(registration.eventClass());
        if (executor == null) {
            executor = new Executor(this);
            this.service_.getServer().getPluginManager().registerEvent(
                    registration.eventClass(),
                    VOID_LISTENER,
                    EventPriority.NORMAL,
                    executor,
                    this.service_.getPlugin(),
                    false
            );
        }

        executor.addBlueprint(blueprint.id());
    }


    //LISTENERS
    @EventHandler void onBlockPlaced(BlockPlaceEvent e) {
        Block b = e.getBlock();
        this.service_.updateAt(
                e.getPlayer(),
                Vec3i.ofBlock(b),
                b.getWorld()
        );
    }

    @EventHandler void onBlockBroken(BlockBreakEvent e) {
        Block b = e.getBlock();
        this.service_.updateAt(
                e.getPlayer(),
                Vec3i.ofBlock(b),
                b.getWorld()
        );
    }

    @EventHandler void onChunkLoad(ChunkLoadEvent e) {
        this.service_.handleChunkLoad(e.getChunk());
    }


    @EventHandler void onStructureCompleted(StructureCompletedEvent e) {
        long id = e.getStructure().blueprint().id();
        BlueprintRegistration registration = this.registrations_.get(id);

        if (registration == null) {
            LOGGER.trace("No registration for blueprint ID {} on structure completed event", id);
            return;
        }
        
        try {
            registration.onCompletion(e.getStructure(), e.getCompleter());
        } catch (Exception ex) {
            LOGGER.error("Listener for blueprint ID {} threw an error on structure completed event at onCompletion()", id, ex);
        }
    }

    @EventHandler void onStructureDestroyed(StructureDestroyedEvent e) {
        long id = e.getStructure().blueprint().id();
        BlueprintRegistration registration = this.registrations_.get(id);

        if (registration == null) {
            LOGGER.trace("No registration for blueprint ID {} on structure destroyed event", id);
            return;
        }
        
        try {
            registration.onDestruction(e.getStructure(), e.getDestructor());
        } catch (Exception ex) {
            LOGGER.error("Listener for blueprint ID {} threw an error on structure destroyed event at onDestruction()", id, ex);
        }
    }


    //SUBTYPES
    private static class Executor implements EventExecutor {

        //FIELDS
        private final StructureServiceListener listener_;
        private final Set<Long> blueprints_;


        //CONSTRUCTOR
        Executor(StructureServiceListener listener) {
            this.listener_ = listener;
            this.blueprints_ = new HashSet<>();
        }


        //SETTERS
        void addBlueprint(long id) {
            this.blueprints_.add(id);
        }


        //IMPLEMENTATION
        @Override
        public void execute(@NonNull Listener ignored, @NonNull Event event) throws EventException {
            for (long id : this.blueprints_) {
                BlueprintRegistration registration = this.listener_.registrations_.get(id);

                if (registration == null) {
                    LOGGER.warn("Could not find registration for blueprint ID {} listening to event {}", id, event.getClass().getSimpleName());
                    continue;
                }
                
                boolean applies;
                try {
                    applies = registration.appliesTo(event);
                } catch (Exception e) {
                    LOGGER.error("Listener for blueprint ID {} threw an error on event {} at appliesTo()", id, event.getClass().getSimpleName(), e);
                    continue;
                }

                if (!applies) {
                    LOGGER.trace("Registration for blueprint ID {} did not apply to event {}", id, event.getClass().getSimpleName());
                    continue;
                }
                
                TrackingInfo tracking;
                try {
                    tracking = registration.track(event);
                } catch (Exception e) {
                    LOGGER.error("Listener for blueprint ID {} threw an error on event {} at track()", id, event.getClass().getSimpleName(), e);
                    continue;
                }

                if (tracking == null) {
                    LOGGER.warn("Tracking info was null for blueprint ID {} on event {}", id, event.getClass().getSimpleName());
                    continue;
                }

                this.listener_.service_.track(registration.blueprint(), tracking.where(), tracking.world(), tracking.who());
            }
        }
    }
}
