package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TotemCoreListener implements Listener {

    private static final Map<Vec3i, TotemCore> CORES = new HashMap<>();

    @EventHandler
    void onPlayerInteract(BlockRightClickedEvent e) {
        Block b = e.getClickedBlock();
        Vec3i loc = Vec3i.ofBlock(b);
        ItemStack itemStack = e.getItem();
        if (itemStack == null || itemStack.getType() == Material.AIR) { return; }

        TotemCore core = CORES.get(loc);
        if (core != null) {
            TotemCore.State state = core.getState();
            if (itemStack.getType() == Material.ENDER_EYE) {
                if (state == TotemCore.State.EMPTY) {
                    core.setState(TotemCore.State.END_EYE);
                } else if (state == TotemCore.State.AMETHIST) {
                    core.setState(TotemCore.State.ACTIVE);
                }
            } else if (itemStack.getType() == Material.AMETHYST_SHARD) {
                if (state == TotemCore.State.EMPTY) {
                    core.setState(TotemCore.State.AMETHIST);
                } else if (state == TotemCore.State.END_EYE) {
                    core.setState(TotemCore.State.ACTIVE);
                }
            }
            if (core.getState() != state) {
                if (itemStack.getAmount() < 2) {
                    itemStack = new ItemStack(Material.AIR);
                } else {
                    itemStack.setAmount(itemStack.getAmount() - 1);
                }
                e.getPlayer().getInventory().setItem(e.getHand(), itemStack);
            }
            return;
        }

        if (b.getType() != Material.TINTED_GLASS) { return; }
        World w = b.getWorld();
        if (itemStack.getType() == Material.ENDER_EYE) {
            core = new TotemCore(loc, w, TotemCore.State.END_EYE, TotemCore.Direction.ALL);
        } else if (itemStack.getType() == Material.AMETHYST_SHARD) {
            core = new TotemCore(loc, w, TotemCore.State.AMETHIST, TotemCore.Direction.ALL);
        }

        if (core == null) { return; }

        CORES.put(loc, core);
    }


    @EventHandler
    void onPlayerTotemInteract(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction entity)) { return; }

        TotemCore core = CORES.values().stream()
                .filter(c -> {
                    Interaction h = c.getHitBox();
                    if (h == null) { return false; }
                    return h.equals(entity);
                })
                .findFirst()
                .orElse(null);
        if (core == null) { return; }

        core.setDirection(cycleDirection(core.getDirection()));
    }


    private final static List<TotemCore.Direction> DIRECTIONS = Arrays.stream(TotemCore.Direction.values()).toList();
    private static TotemCore.Direction cycleDirection(TotemCore.Direction direction) {
        int i = DIRECTIONS.indexOf(direction);
        i++;
        if (i >= DIRECTIONS.size()) { i = 0; };
        return DIRECTIONS.get(i);
    }
}
