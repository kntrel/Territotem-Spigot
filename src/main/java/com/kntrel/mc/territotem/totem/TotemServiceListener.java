package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.runical.bukkit.Runical;
import com.kntrel.mc.runical.core.Placeholder;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.event.StructureChangedEvent;
import com.kntrel.mc.territotem.structure.event.StructureCompletedEvent;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.deeds.Deeds;
import com.kntrel.mc.territotem.totem.deeds.DeedsDeTranspilingError;
import com.kntrel.mc.territotem.totem.deeds.DeedsInterpretationResult;
import com.kntrel.mc.territotem.totem.event.TotemCoreBreakEvent;
import com.kntrel.mc.territotem.totem.event.TotemCoreRightClickedEvent;
import com.kntrel.mc.territotem.totem.region.Expansion;
import com.kntrel.mc.territotem.totem.region.ExpansionResult;
import com.kntrel.util.Vec3i;
import com.kntrel.util.tuple.Pair;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.util.BoundingBox;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

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
        TotemService.NewTotemResult result = this.service_.newTotem(structure, "_unnamed_");

        if (result.status() == TotemService.NewTotemResult.Status.CREATED) {
            Region region = result.totem().region();
            String name = this.defaultTotemName(placer, region);
            region.setName(name);
            if (placer != null) {
                region.addPermission(placer, region.getHierarchy().getLowestLever());
            }
            region.save();
        }

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
            Location center = region.getCenter();
            this.runical_.sendTranslationOrDefault(
                    placer,
                    "totem.creation.success",
                    "Region created: '{region}'.",
                    Placeholder.of("region_name", region.getName()),
                    Placeholder.of("creator", placer.getName()),
                    Placeholder.of("x", formatMeasure(center.getX())),
                    Placeholder.of("y", formatMeasure(center.getY())),
                    Placeholder.of("z", formatMeasure(center.getZ()))
            );
        }
    }

    @EventHandler
    void onCoreRightClicked(TotemCoreRightClickedEvent e) {
        ItemStack itemStack = e.getItemStack();
        if (itemStack == null) { return; }

        Totem totem = this.service_.totemOfCore(e.getCore()).orElse(null);
        if (totem == null) { return; }

        if (itemStack.getType() == Material.DIAMOND) {
            this.onTotemExpand(totem, e);
            if (e.isCancelled()) { return; }
        }

        if (itemStack.getType() == Material.WRITABLE_BOOK) {
            this.onDeedsCreation(e.getPlayer(), totem, itemStack);
        }
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

        Totem totem = this.service_.ownerOfSign(sign).orElse(null);
        if (totem == null) { return; }

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
        totem.rename(content);
        Player player = e.getPlayer();
        this.sendRenamedMessage(totem.region(), player, oldName, content);
    }

    @EventHandler
    void onLecternBooKPlace(BlockRightClickedEvent e) {
        if (!(e.getBlock().getState() instanceof Lectern lectern)) { return; }
        if (!lectern.getInventory().isEmpty()) { return; }

        ItemStack item = e.getItem();
        if (!Tag.ITEMS_LECTERN_BOOKS.isTagged(item.getType())) { return; }

        Totem totem = this.service_.ownerOfLectern(lectern).orElse(null);
        if (totem == null) { return; }

        if (!(item.getItemMeta() instanceof BookMeta bookMeta)) { return; }

        Player player = e.getPlayer();
        DeedsInterpretationResult interpretation = this.service_.getDeedsFactory().interpret(bookMeta);

        Deeds deeds = null;
        switch (interpretation) {
            case DeedsInterpretationResult.DeTranspileError err  -> {
                this.sendDeedsDeTranspileErrorFeedback(player, err);
                e.setCancelled(true);
                return;
            }
            case DeedsInterpretationResult.NotADeedsBook nadb -> {
                this.runical_.sendTranslation(player, "totem.deeds.placement_error.not_a_deeds_book");
                e.setCancelled(true);
                return;
            }
            case DeedsInterpretationResult.Success success -> deeds = success.deeds().orElse(null);
            default -> {}
        }

        if (deeds == null) {
            this.runical_.sendTranslation(player, "totem.deeds.placement_error.generic");
            e.setCancelled(true);
            return;
        }

        Region region = deeds.region();
        if (!deeds.region().getId().equals(totem.region().getId())) {
            this.runical_.sendTranslation(
                    player,
                    "totem.deeds.placement_error.region_mismatch",
                    Placeholder.of("deedsRegionId", region.getId()),
                    Placeholder.of("totemRegionId", totem.region().getId()),
                    Placeholder.of("deedsRegionName", region.getName()),
                    Placeholder.of("totemRegionName", totem.region().getName())
            );
            e.setCancelled(true);
            return;
        }

        if (totem.getDeedsVersion() > deeds.version()) {
            this.runical_.sendTranslation(
                    player,
                    "totem.deeds.placement_error.old_version",
                    Placeholder.of("regionId", region.getId()),
                    Placeholder.of("regionName", region.getName()),
                    Placeholder.of("deedsVersion", deeds.version()),
                    Placeholder.of("totemDeedsVersion", totem.getDeedsVersion())
            );
            e.setCancelled(true);
            return;
        }

        region.setPermissions(deeds.permissions().toArray(new Permission[0]));
    }

    @EventHandler
    void onBlockPlaceAttempt(BlockRightClickedEvent e) {
        ItemStack item = e.getItem();
        Material type = item.getType();

        boolean isSign = Tag.SIGNS.isTagged(type);
        boolean isLectern = type == Material.LECTERN;
        if (!(isSign || isLectern)) { return; }

        Block target = e.getBlock().getRelative(e.getBlockFace());
        List<Totem> totems = this.service_.totemsAtChunk(
                target.getX() >> Constants.CHUNK_SHIFT,
                target.getZ() >> Constants.CHUNK_SHIFT,
                target.getWorld()
        );
        if (totems.isEmpty()) { return; }

        Vec3i coordinates = new Vec3i(target.getX(), target.getY(), target.getZ());
        Totem rejectingTotem = null;
        for (Totem totem : totems) {
            Vec3i offset = coordinates.subtract(totem.origin());
            if (isLectern) {
                Lectern lectern = totem.lectern().orElse(null);
                if (lectern == null) { continue; }

                for (Tile tile : totem.blueprint().lecterns()) {
                    if (offset.equals(tile.offset())) {
                        rejectingTotem = totem;
                        break;
                    }
                }
            } else {
                Sign sign = totem.nameSign().orElse(null);
                if (sign == null) { continue; }

                for (Tile tile : totem.blueprint().nameSings()) {
                    if (offset.equals(tile.offset())) {
                        rejectingTotem = totem;
                        break;
                    }
                }
            }
        }

        if (rejectingTotem == null) { return; }

        e.setCancelled(true);
        this.runical_.sendTranslation(
                e.getPlayer(),
                (isSign) ? "totem.block_place_reject.sign" : "totem.block_place_reject.lectern",
                Placeholder.of("region_name", rejectingTotem.region().getName()),
                Placeholder.of("region_id", rejectingTotem.region().getId())
        );
    }




    //SUB-LISTENERS
    private void onTotemExpand(Totem totem, TotemCoreRightClickedEvent e) {
        ItemStack itemStack = e.getItemStack();
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
        e.setCancelled(true);
    }
    public void onDeedsCreation(Player player, Totem totem, ItemStack item) {
        if (!(item.getItemMeta() instanceof BookMeta bookMeta)) {
            return;
        }

        DeedsInterpretationResult interpretation = this.service_.getDeedsFactory().interpret(bookMeta);
        if (!(interpretation instanceof DeedsInterpretationResult.NotADeedsBook)) {
            return;
        }

        if (!bookMeta.getEnchants().isEmpty()) { return; }
        Deeds deeds = this.service_.getDeedsFactory().generate(player, bookMeta, totem);
        Placeholder[] placeholders = new Placeholder[] {
                Placeholder.of("regionName", deeds.region().getName()),
                Placeholder.of("regionId", deeds.region().getId()),
                Placeholder.of("playerName", player.getName()),
                Placeholder.of("version", deeds.version())
        };

        String deedsName = this.runical_.translate(player, "totem.deeds.item.name", placeholders);
        String lore = this.runical_.translate(player, "totem.deeds.item.lore", placeholders);
        List<String> loreList = Arrays.stream(lore.split("\n")).toList();


        bookMeta.setItemName(deedsName);
        bookMeta.setLore(loreList);
        item.setItemMeta(bookMeta);
        totem.save();
    }


    //HELPERS
    private String defaultTotemName(Player placer, Region region) {
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
                "Unnamed region",
                Placeholder.of("id", region.getId())
        );
    }

    private void sendRenamedMessage(Region region, Player renamer, String oldName, String newName) {

        this.runical_.sendTranslationOrDefault(
                renamer,
                "region.renamed.first_person",
                "Region renamed to '{new_name}'.",
                Placeholder.of("old_name", oldName),
                Placeholder.of("new_name", newName),
                Placeholder.of("region_id", region.getId())
        );

        List<Player> members = region.getOnlineMembers(p -> !p.getUniqueId().equals(renamer.getUniqueId()));
        if (members.isEmpty()) { return; }

        for (Player member : members) {
            this.runical_.sendTranslation(
                    member,
                    "region.renamed.third_person",
                    Placeholder.of("old_name", oldName),
                    Placeholder.of("new_name", newName),
                    Placeholder.of("renamer", renamer.getName()),
                    Placeholder.of("region_id", region.getId())
            );
        }
    }

    private void sendPlacementRejectedMessage(Player player, Collection<Region> blockers) {
        String blockerNames = this.formatRegionList(player, blockers);
        if (!blockerNames.isBlank()) {
            this.runical_.sendTranslationOrDefault(
                    player,
                    "totem.creation.blocked",
                    "Cannot claim here: {blockers}. Move the totem.",
                    Placeholder.of("blockers", blockerNames)
            );
            return;
        }

        this.runical_.sendTranslationOrDefault(
                player,
                "totem.creation.failed",
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

    private void sendDeedsDeTranspileErrorFeedback(Player player, DeedsInterpretationResult.DeTranspileError error) {

        List<Placeholder> basePlaceHolders = List.of(
            Placeholder.of("regionName", error.region().map(Region::getName).orElse(null)),
            Placeholder.of("regionId", error.region().map(r -> r.getId().toString()).orElse(null)),
            Placeholder.of("lineNumber", error.lineNumber()),
            Placeholder.of("line", error.line()),
            Placeholder.of("pageNumber", error.page())
        );

        String key; List<Placeholder> detailPlaceHolders;

        String lineContent = error.line();
        if (lineContent == null || lineContent.isBlank()) {
            key = "totem.deeds.formatting_error.empty_input";
            detailPlaceHolders = List.of();
        } else {
            Pair<String, List<Placeholder>> details = getDeTranspileErrorTranslationDetails(error.error());
            key = details.first();
            detailPlaceHolders = details.second();
        }

        Placeholder[] placeHolders = Stream.concat(
                basePlaceHolders.stream(),
                detailPlaceHolders.stream()
        ).toArray(Placeholder[]::new);

        this.runical_.translateAsync(player, key, placeHolders)
                .thenCompose(detail -> {
                    Placeholder[] ph = Arrays.copyOf(placeHolders, placeHolders.length + 1);
                    ph[ph.length - 1] = Placeholder.of("details", detail);
                    return this.runical_.sendTranslation(player, "totem.deeds.formatting_error.message", ph);
                });
    }

    private Pair<String, List<Placeholder>> getDeTranspileErrorTranslationDetails(DeedsDeTranspilingError error) {

        final String prefix = "totem.deeds.formatting_error.detail.";

        return switch (error) {
            case DeedsDeTranspilingError.PlayerNotFound playerNotFound -> Pair.of(
                    prefix + "player_not_found",
                    List.of(Placeholder.of("playerName", playerNotFound.name()))
            );
            case DeedsDeTranspilingError.UnexpectedCharacter unexpectedCharacter -> Pair.of(
                    prefix + "unexpected_character",
                    List.of(Placeholder.of("column", unexpectedCharacter.index()))
            );
            case DeedsDeTranspilingError.UnknownGroup unknownGroup -> Pair.of(
                    prefix + "unknown_group",
                    List.of(Placeholder.of("groupName", unknownGroup.groupName()))
            );
            case DeedsDeTranspilingError.EmptyGroupName ignored -> Pair.of(
                    prefix + "empty_group_name",
                    List.of()
            );
            case DeedsDeTranspilingError.NoGroupNameProvided ignored -> Pair.of(
                    prefix + "no_group_name_provided",
                    List.of()
            );
        };
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
