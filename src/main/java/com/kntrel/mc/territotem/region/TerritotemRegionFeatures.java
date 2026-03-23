package com.kntrel.mc.territotem.region;

import com.kntrel.mc.regionLib.event.AbilityTriggeredEvent;
import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.mc.regionLib.provided.Abilities;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.territotem.totem.TotemService;
import com.kntrel.mc.territotem.totem.event.TotemCoreBreakEvent;
import com.kntrel.mc.territotem.totem.event.TotemCoreRightClickedEvent;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class TerritotemRegionFeatures implements Listener {

    private static final long AUTOPLANT_STALE_TICKS = 20L;

    private final Plugin plugin_;
    private final RegionContext regionContext_;
    private final TotemService totemService_;
    private final Set<Material> allowedDeedsRequestItems_;
    private final Set<Material> enforcedButtons_;
    private final Set<Material> leverLockerBlocks_;
    private final Set<Event> autoplantHandled_ = Collections.newSetFromMap(new WeakHashMap<>());
    private final Map<ReplantKey, PendingReplant> pendingAutoplants_ = new HashMap<>();

    public TerritotemRegionFeatures(
            Plugin plugin,
            RegionContext regionContext,
            TotemService totemService,
            Set<Material> allowedDeedsRequestItems,
            RegionFeatureMaterialsConfig materialsConfig
    ) {
        this.plugin_ = plugin;
        this.regionContext_ = regionContext;
        this.totemService_ = totemService;
        this.allowedDeedsRequestItems_ = Set.copyOf(allowedDeedsRequestItems);
        this.enforcedButtons_ = materialsConfig.enforcedButtons();
        this.leverLockerBlocks_ = materialsConfig.leverLockerBlocks();
    }

    public void register() {
        this.plugin_.getServer().getPluginManager().registerEvents(this, this.plugin_);
        this.registerAbilities();
        this.registerRules();
    }

    private void registerAbilities() {
        this.regionContext_.registerAbility(
                Ability.on(BlockRightClickedEvent.class)
                        .when(this::isEnforcedButton)
                        .at(BlockRightClickedEvent::getClickedLocation)
                        .extend(Abilities.PRESS_BUTTONS)
                        .named("press_enforced_buttons")
        );

        this.regionContext_.registerAbility(
                Ability.on(BlockRightClickedEvent.class)
                        .when(this::isLockedLever)
                        .at(BlockRightClickedEvent::getClickedLocation)
                        .extend(Abilities.PULL_LEVERS)
                        .named("pull_locked_levers")
        );

        this.regionContext_.registerAbility(
                Ability.on(BlockRightClickedEvent.class)
                        .when(this::isTotemLecternAccess)
                        .at(BlockRightClickedEvent::getClickedLocation)
                        .extend(Abilities.ACCESS_LECTERNS)
                        .named("access_totem_lecterns")
        );

        this.regionContext_.registerAbility(
                Ability.on(BlockRightClickedEvent.class)
                        .when(this::isTotemLecternBookPlacement)
                        .at(BlockRightClickedEvent::getClickedLocation)
                        .extend(Abilities.PUT_BOOKS_ON_LECTERNS)
                        .named("put_books_on_totem_lecterns")
        );

        this.regionContext_.registerAbility(
                Ability.on(PlayerTakeLecternBookEvent.class)
                        .when(this::isTotemLecternBookTaken)
                        .at(e -> center(e.getLectern().getBlock()))
                        .extend(Abilities.TAKE_BOOKS_FROM_LECTERNS)
                        .named("take_books_from_totem_lecterns")
        );

        this.regionContext_.registerAbility(
                Ability.on(TotemCoreRightClickedEvent.class)
                        .when(this::isCreateDeedsInteraction)
                        .at(TotemCoreRightClickedEvent::getLocation)
                        .named("create_deeds")
        );

        this.regionContext_.registerAbility(
                Ability.on(TotemCoreBreakEvent.class)
                        .at(e -> e.getCore().getCenter())
                        .named("destroy_totems")
        );
    }

    private void registerRules() {
        this.regionContext_.getRuleRegistry().register(
                Rule.on(AbilityTriggeredEvent.class)
                        .when(this::isSuccessfulCropBreak)
                        .at(AbilityTriggeredEvent::getLocation)
                        .ifTrue()
                        .then(this::handleAutoplant)
                        .named("autoplant")
        );

        this.regionContext_.getRuleRegistry().register(
                Rule.on(PlayerInteractEvent.class)
                        .when(this::isFarmlandStep)
                        .at(e -> center(e.getClickedBlock()))
                        .ifTrue()
                        .thenCancel()
                        .named("farmlandProtected")
        );

        this.regionContext_.getRuleRegistry().register(
                Rule.on(CreatureSpawnEvent.class)
                        .when(this::isNaturalMonsterSpawn)
                        .at(CreatureSpawnEvent::getLocation)
                        .ifTrue()
                        .thenCancel()
                        .named("noMonsterSpawn")
        );

        this.regionContext_.getRuleRegistry().register(
                Rule.on(RaidTriggerEvent.class)
                        .at(e -> e.getRaid().getLocation())
                        .ifTrue()
                        .thenCancel()
                        .named("raidProtected")
        );
    }

    private boolean isEnforcedButton(BlockRightClickedEvent e) {
        return this.enforcedButtons_.contains(e.getBlock().getType());
    }

    private boolean isLockedLever(BlockRightClickedEvent e) {
        if (e.getBlock().getType() != Material.LEVER) {
            return false;
        }

        BlockFace attachedFace = attachedBlockFace(e.getBlock());
        if (attachedFace == null) {
            return false;
        }
        return this.leverLockerBlocks_.contains(e.getBlock().getRelative(attachedFace).getType());
    }

    private boolean isTotemLecternAccess(BlockRightClickedEvent e) {
        Lectern lectern = toTotemLectern(e.getBlock());
        return lectern != null && !this.isTotemLecternBookPlacement(e);
    }

    private boolean isTotemLecternBookPlacement(BlockRightClickedEvent e) {
        Lectern lectern = toTotemLectern(e.getBlock());
        return lectern != null && lectern.getInventory().isEmpty() && isLecternBook(e.getItem());
    }

    private boolean isTotemLecternBookTaken(PlayerTakeLecternBookEvent e) {
        return this.totemService_.ownerOfLectern(e.getLectern()).isPresent();
    }

    private boolean isCreateDeedsInteraction(TotemCoreRightClickedEvent e) {
        ItemStack item = e.getItemStack();
        return item != null && this.allowedDeedsRequestItems_.contains(item.getType());
    }

    private boolean isSuccessfulCropBreak(AbilityTriggeredEvent e) {
        return e.isAllowed() && Abilities.BREAK_CROPS.equals(e.getAbility());
    }

    private boolean isFarmlandStep(PlayerInteractEvent e) {
        return e.getAction() == org.bukkit.event.block.Action.PHYSICAL
                && e.getClickedBlock() != null
                && e.getClickedBlock().getType() == Material.FARMLAND;
    }

    private boolean isNaturalMonsterSpawn(CreatureSpawnEvent e) {
        return e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL && e.getEntity() instanceof Monster;
    }

    private void handleAutoplant(AbilityTriggeredEvent e) {
        if (!this.autoplantHandled_.add(e)) {
            return;
        }
        if (!(e.getTriggererEvent() instanceof BlockBreakEvent blockBreakEvent)) {
            return;
        }

        Block block = blockBreakEvent.getBlock();
        PendingReplant pending = PendingReplant.of(block);
        this.pendingAutoplants_.put(pending.key(), pending);
        this.plugin_.getServer().getScheduler().runTaskLater(
                this.plugin_,
                () -> this.restorePendingIfCurrent(block, pending),
                AUTOPLANT_STALE_TICKS
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onAutoplantBreak(BlockBreakEvent e) {
        ReplantKey key = ReplantKey.of(e.getBlock());
        PendingReplant pending = this.pendingAutoplants_.get(key);
        if (pending == null) { return; }
        if (e.isCancelled()) {
            this.pendingAutoplants_.remove(key, pending);
            return;
        }

        this.plugin_.getServer().getScheduler().runTaskLater(this.plugin_, () -> {
            this.restorePendingIfCurrent(e.getBlock(), pending);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onAutoplantDrops(BlockDropItemEvent e) {
        PendingReplant pending = this.pendingAutoplants_.get(ReplantKey.of(e.getBlock()));
        if (pending == null) { return; }
        chargeReplantItem(e.getItems(), pending.replantItem());
    }

    static boolean isLecternBook(@Nullable ItemStack item) {
        return item != null && Tag.ITEMS_LECTERN_BOOKS.isTagged(item.getType());
    }

    static @Nullable Material replantItemFor(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case NETHER_WART -> Material.NETHER_WART;
            case BAMBOO, BAMBOO_SAPLING -> Material.BAMBOO;
            case SUGAR_CANE -> Material.SUGAR_CANE;
            case KELP -> Material.KELP;
            case BROWN_MUSHROOM -> Material.BROWN_MUSHROOM;
            case RED_MUSHROOM -> Material.RED_MUSHROOM;
            case ACACIA_SAPLING,
                 AZALEA,
                 BIRCH_SAPLING,
                 CHERRY_SAPLING,
                 DARK_OAK_SAPLING,
                 FLOWERING_AZALEA,
                 JUNGLE_SAPLING,
                 MANGROVE_PROPAGULE,
                 OAK_SAPLING,
                 PALE_OAK_SAPLING,
                 SPRUCE_SAPLING -> crop;
            default -> null;
        };
    }

    static BlockData resetGrowth(BlockData blockData) {
        BlockData reset = blockData.clone();
        if (reset instanceof Ageable ageable) {
            ageable.setAge(0);
            return reset;
        }
        if (reset instanceof Sapling sapling) {
            sapling.setStage(0);
        }
        return reset;
    }

    static boolean chargeReplantItem(List<Item> drops, @Nullable Material replantItem) {
        if (replantItem == null) {
            return false;
        }

        for (Iterator<Item> iterator = drops.iterator(); iterator.hasNext(); ) {
            Item drop = iterator.next();
            ItemStack stack = drop.getItemStack();
            if (stack.getType() != replantItem) {
                continue;
            }

            if (stack.getAmount() > 1) {
                ItemStack updated = stack.clone();
                updated.setAmount(updated.getAmount() - 1);
                drop.setItemStack(updated);
            } else {
                iterator.remove();
            }
            return true;
        }
        return false;
    }

    static @Nullable BlockFace attachedBlockFace(Block block) {
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Directional directional) || !(blockData instanceof FaceAttachable faceAttachable)) {
            return null;
        }

        return switch (faceAttachable.getAttachedFace()) {
            case FLOOR -> BlockFace.DOWN;
            case CEILING -> BlockFace.UP;
            case WALL -> switch (directional.getFacing()) {
                case NORTH -> BlockFace.SOUTH;
                case EAST -> BlockFace.WEST;
                case SOUTH -> BlockFace.NORTH;
                case WEST -> BlockFace.EAST;
                default -> null;
            };
        };
    }

    private @Nullable Lectern toTotemLectern(Block block) {
        if (!(block.getState() instanceof Lectern lectern)) {
            return null;
        }
        if (this.totemService_.ownerOfLectern(lectern).isEmpty()) {
            return null;
        }
        return lectern;
    }

    private void restorePendingIfCurrent(Block block, PendingReplant pending) {
        if (!this.pendingAutoplants_.remove(pending.key(), pending)) {
            return;
        }
        pending.restore(block);
    }

    private static org.bukkit.Location center(Block block) {
        return block.getLocation().add(0.5d, 0.5d, 0.5d);
    }

    private record ReplantKey(String worldName, int x, int y, int z) {

        static ReplantKey of(Block block) {
            return new ReplantKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }
    }

    private record PendingReplant(ReplantKey key, Material cropType, BlockData cropData, @Nullable Material replantItem) {

        static PendingReplant of(Block block) {
            return new PendingReplant(
                    ReplantKey.of(block),
                    block.getType(),
                    resetGrowth(block.getBlockData()),
                    replantItemFor(block.getType())
            );
        }

        void restore(Block block) {
            block.setType(this.cropType);
            block.setBlockData(this.cropData.clone());
        }
    }
}
