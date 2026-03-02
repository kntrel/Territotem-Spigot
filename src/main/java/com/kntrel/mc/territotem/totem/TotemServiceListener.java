package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
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

class TotemServiceListener implements Listener {

    private static final List<TotemCore.Direction> DIRECTIONS = Arrays.stream(TotemCore.Direction.values()).toList();

    private final TotemService service_;

    TotemServiceListener(TotemService service) {
        this.service_ = service;
    }

    @EventHandler
    void onPlayerInteract(BlockRightClickedEvent e) {
        Block clicked = e.getClickedBlock();
        ItemStack itemStack = e.getItem();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        TotemCore core = this.service_.getCore(clicked);
        if (core != null) {
            TotemCore.State previous = core.getState();
            if (itemStack.getType() == Material.ENDER_EYE) {
                if (previous == TotemCore.State.EMPTY) {
                    core.setState(TotemCore.State.END_EYE);
                } else if (previous == TotemCore.State.AMETHIST) {
                    core.setState(TotemCore.State.ACTIVE);
                }
            } else if (itemStack.getType() == Material.AMETHYST_SHARD) {
                if (previous == TotemCore.State.EMPTY) {
                    core.setState(TotemCore.State.AMETHIST);
                } else if (previous == TotemCore.State.END_EYE) {
                    core.setState(TotemCore.State.ACTIVE);
                }
            }

            if (core.getState() != previous) {
                consumeOneItem(e, itemStack);
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

        if (initialState == null) {
            return;
        }

        this.service_.createCore(Vec3i.ofBlock(clicked), clicked.getWorld(), initialState, TotemCore.Direction.ALL);
        consumeOneItem(e, itemStack);
    }

    @EventHandler
    void onPlayerTotemInteract(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction interaction)) {
            return;
        }

        TotemCore core = this.service_.getExistingCores().stream()
                .filter(candidate -> interaction.equals(candidate.getHitBox()))
                .findFirst()
                .orElse(null);
        if (core == null) {
            return;
        }

        core.setDirection(cycleDirection(core.getDirection()));
    }

    @EventHandler
    void onBlockBreak(BlockBreakEvent e) {
        this.service_.breakCore(e.getBlock());
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

    @EventHandler
    void onChunkUnload(ChunkUnloadEvent e) {
        this.service_.handleChunkUnload(e.getChunk());
    }

    private static TotemCore.Direction cycleDirection(TotemCore.Direction direction) {
        int i = DIRECTIONS.indexOf(direction) + 1;
        if (i >= DIRECTIONS.size()) {
            i = 0;
        }
        return DIRECTIONS.get(i);
    }

    private static void consumeOneItem(BlockRightClickedEvent e, ItemStack stack) {
        if (stack.getAmount() < 2) {
            stack = new ItemStack(Material.AIR);
        } else {
            stack.setAmount(stack.getAmount() - 1);
        }
        e.getPlayer().getInventory().setItem(e.getHand(), stack);
    }
}
