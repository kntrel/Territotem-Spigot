package com.kntrel.mc.territotem.totem.core;

import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.mc.territotem.totem.event.*;
import com.kntrel.mc.territotem.util.RayTracing;
import com.kntrel.util.Vec3i;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.UUID;

class TotemCoreListener implements Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TotemCoreListener.class);

    private final TotemCoreTracker tracker_;

    TotemCoreListener(TotemCoreTracker service) {
        this.tracker_ = service;
    }

    @EventHandler
    void onPlayerInteract(BlockRightClickedEvent e) {
        Block clicked = e.getClickedBlock();
        ItemStack itemStack = e.getItem();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        Player player = e.getPlayer();
        TotemCore core = this.tracker_.getCore(clicked);
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
                consumeOneItem(player, e.getHand(), itemStack);
                this.tracker_.persistCore(core);
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

        core = this.tracker_.createCore(Vec3i.ofBlock(clicked), clicked.getWorld(), initialState, TotemCore.Direction.ALL);
        TotemCoreCreatedEvent event = new TotemCoreCreatedEvent(core, player);
        this.tracker_.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.tracker_.destroyCore(core);
            return;
        }
        consumeOneItem(player, e.getHand(), itemStack);
        e.setCancelled(true);
    }

    @EventHandler
    void onTotemCoreInteract(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction interaction)) {
            return;
        }

        TotemCore core = this.coreFromInteraction(interaction);
        if (core == null) { return; }

        ItemStack item = e.getPlayer().getInventory().getItem(e.getHand());

        Vector offset = e.getClickedPosition();
        Location pos = interaction.getLocation().clone().add(offset);

        TotemCoreRightClickedEvent event = new TotemCoreRightClickedEvent(core, e.getPlayer(), item, e.getHand(), pos);
        this.tracker_.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) { return; }

        if (this.onRightClickWithAmethist(event)) { return; }
        this.onRightClick(event);
    }

    @EventHandler
    void onTotemCoreDamaged(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Interaction interaction)) {
            return;
        }

        TotemCore core = this.coreFromInteraction(interaction);
        if (core == null) { return; }

        Entity damager = e.getDamager();
        Player player = null;
        if (damager instanceof Player p) {
            player = p;
        } else if (damager instanceof Projectile proj) {
            if (proj.getShooter() instanceof Player p) {
                player = p;
            }
        }
        if (player == null) { return; }

        TotemCoreHitEvent event = new TotemCoreHitEvent(core, player, player.getItemInUse(), EquipmentSlot.HAND, e.getDamager().getLocation());
        this.tracker_.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) { return; }

        if (this.onHitWithDirection(event)) { return; };
        this.onHit(event);
    }

    @EventHandler
    void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        TotemCore core = this.tracker_.getCore(b);
        if (core != null) {
            TotemCoreBreakEvent coreBreakEvent = new TotemCoreBreakEvent(core, e.getPlayer());
            this.tracker_.getServer().getPluginManager().callEvent(coreBreakEvent);
            if (coreBreakEvent.isCancelled()) {
                e.setCancelled(true);
                return;
            }
            e.setDropItems(false);
            this.tracker_.breakCore(core);
            return;
        }

        Vec3i pos = Vec3i.ofBlock(b);
        World world = b.getWorld();
        this.tracker_.getNearByCores(b)
                .thenApply(l -> l.stream().filter(c -> isAdjacent(c.getCoordinates(), pos)))
                .thenAccept(l -> l.forEach(c ->
                    this.tracker_.getServer().getScheduler().runTaskLater(this.tracker_.getPlugin(), () -> {
                        Vec3i loc = c.getCoordinates();
                        Material newType = world.getBlockAt(loc.x(), loc.y(), loc.z()).getType();
                        if (newType != Material.MEDIUM_AMETHYST_BUD && newType != Material.TINTED_GLASS) {
                            BlockBreakEvent event = new BlockBreakEvent(c.getCenter().getBlock(), e.getPlayer());
                            this.tracker_.getServer().getPluginManager().callEvent(event);
                            if (event.isCancelled()) {
                                TotemCore.State state = c.getState();
                                c.setState(TotemCore.State.EMPTY);
                                c.setState(state);
                            }
                        }
                    }, 1)
                ));
    }

    @EventHandler
    void onExplosion(EntityExplodeEvent e) {
        e.blockList().removeIf(this.tracker_::isCore);
    }

    @EventHandler
    void onExplosion(BlockExplodeEvent e) {
        e.blockList().removeIf(this.tracker_::isCore);
    }

    @EventHandler
    void onPistonExtend(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(this.tracker_::isCore)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    void onPistonRetract(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream().anyMatch(this.tracker_::isCore)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    void onChunkLoad(ChunkLoadEvent e) {
        this.tracker_.handleChunkLoad(e.getChunk());
    }

    @EventHandler
    void onChunkUnload(ChunkUnloadEvent e) {
        Chunk c = e.getChunk();
        this.tracker_.unloadChunk(e.getWorld(), c.getX(), c.getZ());
    }


    //SUB-HANDLERS
    private boolean onRightClickWithAmethist(TotemCoreRightClickedEvent e) {
        ItemStack itemStack = e.getItemStack();
        if (itemStack.getType() != Material.AMETHYST_BLOCK) { return false; }

        TotemCore core = e.getCore();
        if (core.getDirection() != TotemCore.Direction.ALL) { return false; }

        Player player = e.getPlayer();
        TotemCore.Direction direction = computeRelativeDirection(player.getEyeLocation(), core.getCenter());
        if (direction == null) { return false; }

        consumeOneItem(player, e.getHand(), itemStack);
        swingHand(player, e.getHand());

        core.setDirection(direction);
        this.tracker_.persistCore(core);
        return true;
    }
    private boolean onRightClick(TotemCoreRightClickedEvent e) {
        TotemCore core = e.getCore();
        TotemCore.Direction oldDirection = core.getDirection();
        if (oldDirection == TotemCore.Direction.ALL) { return false; }

        Player player = e.getPlayer();
        TotemCore.Direction direction = computeRelativeDirection(player.getEyeLocation(), core.getCenter());
        if (direction == null || direction == oldDirection) { return false; }

        swingHand(player, e.getHand());
        core.setDirection(direction);
        this.tracker_.persistCore(core);
        return true;
    }
    private boolean onHitWithDirection(TotemCoreHitEvent e) {
        TotemCore core = e.getCore();
        if (core.getDirection() == TotemCore.Direction.ALL) { return false; }

        Vector pos = core.getCenter().toVector().subtract(e.getLocation().toVector()).normalize().multiply(.3);

        core.getWorld().dropItemNaturally(core.getCenter().clone().subtract(pos), new ItemStack(Material.AMETHYST_BLOCK));
        core.setDirection(TotemCore.Direction.ALL);
        this.tracker_.persistCore(core);
        return true;
    }
    private boolean onHit(TotemCoreHitEvent e) {
        TotemCore core = e.getCore();
        if (core.getDirection() != TotemCore.Direction.ALL) { return false; }

        TotemCoreBreakEvent event = new TotemCoreBreakEvent(core, e.getPlayer());
        this.tracker_.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) { return false; }
        this.tracker_.breakCore(core);
        return true;
    }


    //HELPERS
    private @Nullable TotemCore coreFromInteraction(Interaction interaction) {
        if (!interaction.hasMetadata(TotemCore.METADATA_KEY)) { return null; }

        List<MetadataValue> vals = interaction.getMetadata(TotemCore.METADATA_KEY);
        UUID coreId = null;
        for (MetadataValue val : vals) {
            if (val.getOwningPlugin() == this.tracker_.getPlugin()) {
                coreId = UUID.fromString(val.asString());
                break;
            }
        }

        if (coreId == null) { return null; }

        TotemCore core = this.tracker_.getCore(coreId);

        if (core != null) { return core; }

        LOGGER.warn(
                "Interaction entity {} is marked as hitbox of totem core {}. But the core isn't loaded. Destroying the entity.",
                interaction.getUniqueId(),
                coreId
        );
        interaction.remove();
        return null;
    }
    private void completeCore(TotemCore core, Player player) {
        TotemCoreCompletedEvent coreCompletedEvent = new TotemCoreCompletedEvent(core, player);
        this.tracker_.getServer().getPluginManager().callEvent(coreCompletedEvent);
        if (!coreCompletedEvent.isCancelled()) {
            core.setState(TotemCore.State.FULL);
        }
    }

    private static void consumeOneItem(Player player, EquipmentSlot hand, ItemStack stack) {
        if (stack.getAmount() < 2) {
            stack = new ItemStack(Material.AIR);
        } else {
            stack.setAmount(stack.getAmount() - 1);
        }
        player.getInventory().setItem(hand, stack);
    }
    private static void swingHand(LivingEntity entity, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            entity.swingOffHand();
        } else {
            entity.swingMainHand();
        }
    }

    private static boolean isAdjacent(Vec3i a, Vec3i b) {
        int dx = Math.abs(a.x() - b.x());
        int dy = Math.abs(a.y() - b.y());
        int dz = Math.abs(a.z() - b.z());
        return (dx + dy + dz) == 1;
    }
    private static TotemCore.Direction computeRelativeDirection(Location origin, Location target) {

        double  xMag = origin.getX() - target.getX(),
                yMag = origin.getY() - target.getY(),
                zMag = origin.getZ() - target.getZ(),
                xDif = Math.abs(xMag),
                yDif = Math.abs(yMag),
                zDif = Math.abs(zMag);

        boolean horizontal = yDif > xDif && yDif > zDif;

        if (horizontal) {      // Project direction on a horizontal plane

            RayTracing.PlaneHit hit = RayTracing.hitOnHorizontalPlane(origin, target);
            if (hit == null) { return null; }

            xDif = Math.abs(hit.x());
            zDif = Math.abs(hit.y());

            double magnitude;
            if (xDif < zDif) {  //On X
                magnitude = hit.y();
                if (magnitude < 0) {
                    return TotemCore.Direction.WEST;
                } else {
                    return TotemCore.Direction.EAST;
                }
            } else {            //On Z
                magnitude = -hit.x();
                if (magnitude < 0) {
                    return TotemCore.Direction.SOUTH;
                } else {
                    return TotemCore.Direction.NORTH;
                }
            }
        }

        //Using a vertical plane
        RayTracing.PlaneHit hit = RayTracing.hitOnVerticalFacingPlane(origin, target);
        if (hit == null) { return null; }

        double  xComp = Math.abs(hit.x()),
                yComp = Math.abs(hit.y());

        if (yComp > xComp) {      //On Y
            if (hit.y() < 0) {
                return TotemCore.Direction.DOWN;
            } else {
                return TotemCore.Direction.UP;
            }
        }

        //If the Y component isn't the strongest, define weather or not to plane on X or Z
        double magnitude;
        if (xDif > zDif) {          //On Z
            magnitude = hit.x();
            if (xMag < 0) { magnitude *=-1; }
            if (magnitude < 0) {
                return TotemCore.Direction.SOUTH;
            } else {
                return TotemCore.Direction.NORTH;
            }
        } else {                    //On X
            magnitude = hit.x();
            if (zMag < 0) { magnitude *=-1; }
            if (magnitude < 0) {
                return TotemCore.Direction.WEST;
            } else {
                return TotemCore.Direction.EAST;
            }
        }
    }
}