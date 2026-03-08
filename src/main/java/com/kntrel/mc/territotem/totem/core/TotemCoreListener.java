package com.kntrel.mc.territotem.totem.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.mc.territotem.totem.TotemClaim;
import com.kntrel.mc.territotem.totem.event.TotemCoreCompletedEvent;
import com.kntrel.mc.territotem.totem.event.TotemCoreCreatedEvent;
import com.kntrel.util.Vec3i;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Arrays;
import java.util.List;

class TotemCoreListener implements Listener {

    private static final List<TotemCore.Direction> DIRECTIONS = Arrays.stream(TotemCore.Direction.values()).toList();
    private static final String TOTEM_DATA_KEY = "totemData";
    private static final Gson CLAIM_GSON = new GsonBuilder()
            .registerTypeAdapter(TotemClaim.class, TotemClaim.serializer())
            .registerTypeAdapter(TotemClaim.class, TotemClaim.deserializer())
            .create();

    private final TotemCoreTracker service_;

    TotemCoreListener(TotemCoreTracker service) {
        this.service_ = service;
    }

    @EventHandler
    void onPlayerInteract(BlockRightClickedEvent e) {
        Block clicked = e.getClickedBlock();
        ItemStack itemStack = e.getItem();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        Player player = e.getPlayer();
        TotemCore core = this.service_.getCore(clicked);
        if (core != null) {
            TotemCore.State previous = core.getState();
            if (itemStack.getType() == Material.ENDER_EYE) {
                if (previous == TotemCore.State.EMPTY) {
                    core.setState(TotemCore.State.END_EYE);
                } else if (previous == TotemCore.State.AMETHIST) {
                    this.completeCore(core, player);
                }
            } else if (itemStack.getType() == Material.AMETHYST_SHARD) {
                if (previous == TotemCore.State.EMPTY) {
                    core.setState(TotemCore.State.AMETHIST);
                } else if (previous == TotemCore.State.END_EYE) {
                    this.completeCore(core, player);
                }
            }

            if (core.getState() != previous) {
                consumeOneItem(e, itemStack);
                this.service_.persistCore(core);
                e.setCancelled(true);
            }
            return;
        }

        if (clicked.getType() != Material.TINTED_GLASS) {
            return;
        }

        TotemCore.State initialState = null;
        if (itemStack.getType() == Material.ENDER_EYE) {
            initialState = TotemCore.State.END_EYE;
        } else if (itemStack.getType() == Material.AMETHYST_SHARD) {
            initialState = TotemCore.State.AMETHIST;
        }

        if (initialState == null) { return; }

        core = this.service_.createCore(Vec3i.ofBlock(clicked), clicked.getWorld(), initialState, TotemCore.Direction.ALL);
        TotemCoreCreatedEvent event = new TotemCoreCreatedEvent(core, player);
        this.service_.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.service_.destroyCore(core);
            return;
        }
        consumeOneItem(e, itemStack);
        e.setCancelled(true);
    }

    @EventHandler
    void onPlayerTotemInteract(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction interaction)) {
            return;
        }

        TotemCore core = this.service_.getLoadedCores().stream()
                .filter(candidate -> interaction.equals(candidate.getHitBox()))
                .findFirst()
                .orElse(null);
        if (core == null) {
            return;
        }

        core.setDirection(cycleDirection(core.getDirection()));
        this.service_.persistCore(core);
    }

    @EventHandler
    void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (this.service_.breakCore(b)) {
            e.setDropItems(false);
            return;
        }

        Vec3i pos = Vec3i.ofBlock(b);
        World world = b.getWorld();
        this.service_.getNearByCores(b)
                .thenApply(l -> l.stream().filter(c -> isAdjacent(c.getCoordinates(), pos)))
                .thenAccept(l -> l.forEach(c ->
                    this.service_.getServer().getScheduler().runTaskLater(this.service_.getPlugin(), () -> {
                        Vec3i loc = c.getCoordinates();
                        Material newType = world.getBlockAt(loc.x(), loc.y(), loc.z()).getType();
                        if (newType != Material.MEDIUM_AMETHYST_BUD && newType != Material.TINTED_GLASS) {
                            this.service_.breakCore(c);
                        }
                    }, 1)
                ));
    }

    @EventHandler
    void onExplosion(EntityExplodeEvent e) {
        e.blockList().removeIf(this.service_::isCore);
    }

    @EventHandler
    void onExplosion(BlockExplodeEvent e) {
        e.blockList().removeIf(this.service_::isCore);
    }

    @EventHandler
    void onPistonExtend(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(this.service_::isCore)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    void onPistonRetract(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream().anyMatch(this.service_::isCore)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    void onChunkLoad(ChunkLoadEvent e) {
        this.service_.handleChunkLoad(e.getChunk());
    }

    private static TotemCore.Direction cycleDirection(TotemCore.Direction direction) {
        int i = DIRECTIONS.indexOf(direction) + 1;
        if (i >= DIRECTIONS.size()) {
            i = 0;
        }
        return DIRECTIONS.get(i);
    }

    @EventHandler
    void onChunkUnload(ChunkUnloadEvent e) {
        Chunk c = e.getChunk();
        this.service_.unloadChunk(e.getWorld(), c.getX(), c.getZ());
    }

    private void completeCore(TotemCore core, Player player) {
        TotemCoreCompletedEvent coreCompletedEvent = new TotemCoreCompletedEvent(core, player);
        this.service_.getServer().getPluginManager().callEvent(coreCompletedEvent);
        if (!coreCompletedEvent.isCancelled()) {
            core.setState(TotemCore.State.FULL);
        }
    }

    private static void consumeOneItem(BlockRightClickedEvent e, ItemStack stack) {
        if (stack.getAmount() < 2) {
            stack = new ItemStack(Material.AIR);
        } else {
            stack.setAmount(stack.getAmount() - 1);
        }
        e.getPlayer().getInventory().setItem(e.getHand(), stack);
    }

    private static boolean isAdjacent(Vec3i a, Vec3i b) {
        int dx = Math.abs(a.x() - b.x());
        int dy = Math.abs(a.y() - b.y());
        int dz = Math.abs(a.z() - b.z());
        return (dx + dy + dz) == 1;
    }

}