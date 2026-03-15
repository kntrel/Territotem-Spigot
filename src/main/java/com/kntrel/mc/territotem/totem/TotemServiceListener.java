package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.runical.bukkit.Runical;
import com.kntrel.mc.runical.core.Placeholder;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.event.StructureChangedEvent;
import com.kntrel.mc.territotem.structure.event.StructureCompletedEvent;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.event.TotemCoreBreakEvent;
import com.kntrel.mc.territotem.totem.event.TotemCoreRightClickedEvent;
import com.kntrel.mc.territotem.totem.region.Expansion;
import com.kntrel.mc.territotem.totem.region.ExpansionResult;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;

final class TotemServiceListener implements Listener {

    private static final double REGION_GROWTH_RATE = 6d;
    private static final double EPSILON = 1.0E-9;

    private final TotemService service_;
    private final RegionContext regionContext_;
    private final Runical runical_;

    TotemServiceListener(TotemService service, RegionContext regionContext, Runical runical) {
        this.service_ = service;
        this.regionContext_ = regionContext;
        this.runical_ = runical;
    }

    @EventHandler
    void onTotemCompleted(StructureCompletedEvent e) {
        Structure structure = e.getStructure();
        if (!(structure.blueprint() instanceof TotemBlueprint)) {
            return;
        }

        Player placer = (e.getCauser() instanceof Player p) ? p : null;
        String name = this.defaultTotemName(placer);
        TotemService.NewTotemResult result = this.service_.newTotem(structure, name);
        if (placer == null) {
            return;
        }

        if (result.status() == TotemService.NewTotemResult.Status.BLOCKED) {
            this.sendPlacementRejectedMessage(placer, result.blockers());
            return;
        }

        if (result.status() == TotemService.NewTotemResult.Status.CREATED) {
            Region region = result.totem().region();
            region.display(placer);
            this.runical_.sendTranslationOrDefault(
                    placer,
                    "totem.creation",
                    "Region created: '{region}'.",
                    Placeholder.of("region", region.getName())
            );
        }
    }

    @EventHandler
    void onCoreRightClicked(TotemCoreRightClickedEvent e) {
        ItemStack itemStack = e.getItemStack();
        if (itemStack == null || itemStack.getType() != Material.DIAMOND) {
            return;
        }

        e.setCancelled(true);

        Totem totem = this.service_.totemOfCore(e.getCore()).orElse(null);
        if (totem == null) {
            return;
        }

        TotemCore.Direction direction = e.getCore().getDirection();
        ExpansionResult result = totem.expand(expansionFor(direction));
        Player player = e.getPlayer();
        if (!result.hasGrowth()) {
            this.sendExpansionBlockedMessage(player, direction, result);
            return;
        }

        consumeOneItem(player, e.getHand(), itemStack);
        swingHand(player, e.getHand());
        totem.region().display(player);
        this.sendExpansionFeedback(player, totem, direction, result);
    }

    @EventHandler
    void onCoreDestroyed(TotemCoreBreakEvent e) {
        Totem totem = this.service_.totemOfCore(e.getCore()).orElse(null);
        if (totem == null) {
            return;
        }
        this.service_.destroyTotem(totem);
    }

    @EventHandler
    void onTotemChanged(StructureChangedEvent e) {
        if (e.getCurrentState() == Structure.State.COMPLETE) {
            return;
        }

        if (!(e.getStructure().blueprint() instanceof TotemBlueprint)) {
            return;
        }

        this.service_.updateTotemStructure(e.getStructure(), e.getChangedPiece(), e.getPieceMatched());
    }

    @EventHandler
    void onSignEdited(SignChangeEvent e) {
        if (!(e.getBlock().getState() instanceof Sign sign)) {
            return;
        }

        Totem totem = this.service_.totemAtSign(sign).orElse(null);
        if (totem == null) {
            return;
        }

        String content = String.join(" ", e.getLines()).trim();
        RegionContextConfig conf = this.regionContext_.getConfig();
        int length = content.length();
        if (length < conf.minNameLength || length > conf.maxNameLength) {
            e.setCancelled(true);
            this.runical_.sendTranslationOrDefault(
                    e.getPlayer(),
                    "totem.rename.invalid_length",
                    "Name length must be {min}-{max} characters.",
                    Placeholder.of("min", Integer.toString(conf.minNameLength)),
                    Placeholder.of("max", Integer.toString(conf.maxNameLength))
            );
            return;
        }

        String oldName = totem.region().getName();
        this.service_.renameTotem(totem, content);
        this.runical_.sendTranslationOrDefault(
                e.getPlayer(),
                "totem.rename.success",
                "Region renamed to '{new_name}'.",
                Placeholder.of("old_name", oldName),
                Placeholder.of("new_name", content)
        );
    }

    @EventHandler
    void onBlockPlaceAttempt(BlockRightClickedEvent e) {
        ItemStack item = e.getItem();
        Material type = item.getType();

        boolean isSign = Tag.SIGNS.isTagged(type);
        boolean isLectern = type == Material.LECTERN;
        if (!(isSign || isLectern)) {
            return;
        }

        Block target = e.getBlock().getRelative(e.getBlockFace());
        if (this.service_.blocksTotemPlacement(target, isLectern)) {
            e.setCancelled(true);
        }
    }


    //HELPERS
    private String defaultTotemName(Player placer) {
        if (placer != null) {
            return this.runical_.translateOrDefault(
                    placer,
                    "totem.default_name.player_placed",
                    "{player}'s lands",
                    Placeholder.of("player", placer.getName())
            );
        }
        return this.runical_.translateOrDefault(
                this.runical_.getDefaultLocale(),
                "totem.default_name.undefined_placer",
                "Unnamed region"
        );
    }

    private void sendPlacementRejectedMessage(Player player, Collection<Region> blockers) {
        String blockerNames = this.formatRegionList(player, blockers);
        if (!blockerNames.isBlank()) {
            this.runical_.sendTranslationOrDefault(
                    player,
                    "totem.creation_failed.blocked",
                    "Cannot claim here: {blockers}. Move the totem.",
                    Placeholder.of("blockers", blockerNames)
            );
            return;
        }

        this.runical_.sendTranslationOrDefault(
                player,
                "totem.creation_failed.generic",
                "Cannot claim here. Move the totem."
        );
    }

    private void sendExpansionFeedback(Player player, Totem totem, TotemCore.Direction direction, ExpansionResult result) {
        Placeholder[] placeholders = this.expansionPlaceholders(player, totem, direction, result);
        if (this.isShifted(result)) {
            this.runical_.sendTranslationOrDefault(
                    player,
                    "totem.expansion.shifted",
                    "Expanded {direction}; shifted around {blockers}. Size: H {height}, X {x}, Z {z}.",
                    placeholders
            );
            return;
        }

        if (result.unachievedTotal() > EPSILON) {
            this.runical_.sendTranslationOrDefault(
                    player,
                    "totem.expansion.partial",
                    "Expanded {direction}; {blockers} blocked the rest. Size: H {height}, X {x}, Z {z}.",
                    placeholders
            );
            return;
        }

        this.runical_.sendTranslationOrDefault(
                player,
                "totem.expansion.success",
                "Expanded {direction}. Size: H {height}, X {x}, Z {z}.",
                placeholders
        );
    }

    private void sendExpansionBlockedMessage(Player player, TotemCore.Direction direction, ExpansionResult result) {
        String blockerNames = this.formatRegionList(player, result.blockingRegions());
        if (!blockerNames.isBlank()) {
            this.runical_.sendTranslationOrDefault(
                    player,
                    "totem.expansion.blocked",
                    "Cannot expand {direction}: {blockers}.",
                    Placeholder.of("direction", this.translateDirection(player, direction)),
                    Placeholder.of("blockers", blockerNames)
            );
            return;
        }

        this.runical_.sendTranslationOrDefault(
                player,
                "totem.expansion.blocked_generic",
                "Cannot expand {direction}.",
                Placeholder.of("direction", this.translateDirection(player, direction))
        );
    }

    private Placeholder[] expansionPlaceholders(Player player, Totem totem, TotemCore.Direction direction, ExpansionResult result) {
        BoundingBox bounds = totem.region().getBoundingBox();
        return new Placeholder[] {
                Placeholder.of("direction", this.translateDirection(player, direction)),
                Placeholder.of("height", formatMeasure(bounds.getHeight())),
                Placeholder.of("x", formatMeasure(bounds.getWidthX())),
                Placeholder.of("z", formatMeasure(bounds.getWidthZ())),
                Placeholder.of("blockers", this.formatRegionList(player, result.blockingRegions()))
        };
    }

    private String translateDirection(Player player, TotemCore.Direction direction) {
        String key = "totem.expansion.direction." + direction.name().toLowerCase();
        String fallback = switch (direction) {
            case ALL -> "in all directions";
            case UP -> "upward";
            case DOWN -> "downward";
            case NORTH -> "to the north";
            case SOUTH -> "to the south";
            case EAST -> "to the east";
            case WEST -> "to the west";
        };

        if (player != null) {
            return this.runical_.translateOrDefault(player, key, fallback);
        }
        return this.runical_.translateOrDefault(this.runical_.getDefaultLocale(), key, fallback);
    }

    private String formatRegionList(Player player, Collection<Region> regions) {
        if (regions == null || regions.isEmpty()) {
            return "";
        }

        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (Region region : regions) {
            names.add(this.regionName(player, region));
        }

        if (player != null) {
            return this.runical_.formatList(player, names);
        }
        return this.runical_.formatList(this.runical_.getDefaultLocale(), names);
    }

    private String regionName(Player player, Region region) {
        String name = region.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }

        String id = (region.getId() == null) ? "?" : region.getId().toString();
        if (player != null) {
            return this.runical_.translateOrDefault(
                    player,
                    "totem.region.unnamed",
                    "Region #{id}",
                    Placeholder.of("id", id)
            );
        }
        return this.runical_.translateOrDefault(
                this.runical_.getDefaultLocale(),
                "totem.region.unnamed",
                "Region #{id}",
                Placeholder.of("id", id)
        );
    }

    private boolean isShifted(ExpansionResult result) {
        if (result.unachievedTotal() > EPSILON) {
            return false;
        }

        Expansion intended = result.intended();
        Expansion accomplished = result.accomplished();
        return !same(intended.up(), accomplished.up())
                || !same(intended.down(), accomplished.down())
                || !same(intended.north(), accomplished.north())
                || !same(intended.south(), accomplished.south())
                || !same(intended.east(), accomplished.east())
                || !same(intended.west(), accomplished.west());
    }

    private static String formatMeasure(double value) {
        double rounded = Math.rint(value);
        if (same(value, rounded)) {
            return Long.toString(Math.round(rounded));
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static Expansion expansionFor(TotemCore.Direction direction) {
        if (direction == TotemCore.Direction.ALL) {
            return Expansion.all(REGION_GROWTH_RATE / 6d);
        }
        return Expansion.forDirection(direction, REGION_GROWTH_RATE);
    }

    private static void consumeOneItem(Player player, EquipmentSlot hand, ItemStack stack) {
        if (stack.getAmount() < 2) {
            stack = new ItemStack(Material.AIR);
        } else {
            stack.setAmount(stack.getAmount() - 1);
        }
        player.getInventory().setItem(hand, stack);
    }

    private static void swingHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.swingOffHand();
        } else {
            player.swingMainHand();
        }
    }

    private static boolean same(double a, double b) {
        return Math.abs(a - b) <= EPSILON;
    }
}
