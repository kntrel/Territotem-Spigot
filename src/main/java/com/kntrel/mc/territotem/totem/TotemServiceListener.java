package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.runical.bukkit.Translator;
import com.kntrel.mc.runical.core.argument.Argument;
import com.kntrel.mc.territotem.region.RegionColor;
import com.kntrel.mc.territotem.region.RegionColors;
import com.kntrel.mc.territotem.region.RegionArgument;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.event.StructureChangedEvent;
import com.kntrel.mc.territotem.structure.event.StructureCompletedEvent;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.deeds.Deeds;
import com.kntrel.mc.territotem.totem.deeds.DeedsDeTranspilingError;
import com.kntrel.mc.territotem.totem.deeds.DeedsInterpretationResult;
import com.kntrel.mc.territotem.totem.event.TotemCoreBreakEvent;
import com.kntrel.mc.territotem.totem.event.TotemCoreHitEvent;
import com.kntrel.mc.territotem.totem.event.TotemCoreRightClickedEvent;
import com.kntrel.mc.territotem.totem.region.Expansion;
import com.kntrel.mc.territotem.totem.region.ExpansionResult;
import com.kntrel.mc.territotem.totem.region.ExpansionTable;
import com.kntrel.util.Vec3i;
import com.kntrel.util.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;
import org.bukkit.util.BoundingBox;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

final class TotemServiceListener implements Listener {

    private static final double EPSILON = 1.0E-9;

    private final Plugin plugin_;
    private final TotemService service_;
    private final RegionContext regionContext_;
    private final Translator translator_;
    private final ExpansionTable expansionTable_;
    private final Set<Material> allowedDeedsRequestItems_;

    TotemServiceListener(
            TotemService service,
            RegionContext regionContext,
            Translator translator,
            ExpansionTable expansionTable,
            Set<Material> allowedDeedsRequestItems
    ) {
        this.plugin_ = regionContext.getPlugin();
        this.service_ = service;
        this.regionContext_ = regionContext;
        this.translator_ = translator;
        this.expansionTable_ = expansionTable;
        this.allowedDeedsRequestItems_ = Set.copyOf(allowedDeedsRequestItems);
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
            this.syncNameSignColor(result.totem());
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
            this.translator_.translate(placer, "creation.success")
                    .argument(RegionArgument.of(region))
                    .argument("creator", placer.getName())
                    .argument("x", formatMeasure(center.getX()))
                    .argument("y", formatMeasure(center.getY()))
                    .argument("z", formatMeasure(center.getZ()))
                    .orDefault("Region created: '{region.colorizedName}'.")
                    .send();
        }
    }

    @EventHandler
    void onCoreRightClicked(TotemCoreRightClickedEvent e) {
        if (e.isCancelled()) { return; }

        ItemStack itemStack = e.getItemStack();
        if (itemStack == null) { return; }

        Totem totem = this.service_.totemOfCore(e.getCore()).orElse(null);
        if (totem == null) { return; }

        ExpansionTable.Row expansionRow = this.expansionTable_.findMatch(itemStack).orElse(null);
        if (expansionRow != null) {
            this.onTotemExpand(totem, e, expansionRow);
            return;
        }

        if (this.allowedDeedsRequestItems_.contains(itemStack.getType())) {
            this.onDeedsCreation(e.getPlayer(), totem, e.getHand(), itemStack);
        }
    }

    @EventHandler
    void onCoreDestroyed(TotemCoreBreakEvent e) {
        if (e.isCancelled()) { return; }

        Totem totem = this.service_.totemOfCore(e.getCore()).orElse(null);
        if (totem == null) {
            return;
        }
        this.service_.destroyTotem(totem);
    }

    @EventHandler
    void onCoreHit(TotemCoreHitEvent e) {
        if (e.isCancelled()) { return; }
        if (e.getCore().getDirection() != TotemCore.Direction.ALL) { return; }

        Totem totem = this.service_.totemOfCore(e.getCore()).orElse(null);
        if (totem == null) { return; }
        if (!this.service_.rollbackLastExpansion(totem)) { return; }

        this.regionContext_.displayRegion(totem.region(), e.getPlayer());
        e.setCancelled(true);
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
            this.translator_.translate(e.getPlayer(), "rename.invalid_length")
                    .argument("min", Integer.toString(conf.minNameLength))
                    .argument("max", Integer.toString(conf.maxNameLength))
                    .orDefault("Name length must be {min}-{max} characters.")
                    .send();
            return;
        }

        String oldName = totem.region().getName();
        totem.rename(content);
        Player player = e.getPlayer();
        this.sendRenamedMessage(totem.region(), player, oldName, content);
        this.scheduleNameSignColorSync(e.getBlock(), totem);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onNameSignDyed(BlockRightClickedEvent e) {
        if (e.getClickedBlock() == null) {
            return;
        }

        DyeColor dyeColor = dyeColorOf(e.getItem());
        if (dyeColor == null) {
            return;
        }

        if (!(e.getClickedBlock().getState() instanceof Sign sign) || sign.isWaxed()) {
            return;
        }

        Totem totem = this.service_.ownerOfSign(sign).orElse(null);
        if (totem == null) {
            return;
        }

        Block block = e.getClickedBlock();
        this.plugin_.getServer().getScheduler().runTask(
                this.plugin_,
                () -> this.captureRegionColorFromSign(block, totem)
        );
    }

    @EventHandler
    void onLecternBooKPlace(BlockRightClickedEvent e) {
        if (e.isCancelled()) { return; }
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
                this.translator_.translate(player, "deeds.placement_error.not_a_deeds_book").send();
                e.setCancelled(true);
                return;
            }
            case DeedsInterpretationResult.Success success -> deeds = success.deeds().orElse(null);
            default -> {}
        }

        if (deeds == null) {
            this.translator_.translate(player, "deeds.placement_error.generic").send();
            e.setCancelled(true);
            return;
        }

        Region region = deeds.region();
        if (!deeds.region().getId().equals(totem.region().getId())) {
            this.translator_.translate(player, "deeds.placement_error.region_mismatch")
                    .argument("deedsRegion", region)
                    .argument("totemRegion", totem.region())
                    .send();
            e.setCancelled(true);
            return;
        }

        if (totem.getDeedsVersion() > deeds.version()) {
            this.translator_.translate(player, "deeds.placement_error.old_version")
                    .argument(RegionArgument.of(region))
                    .argument("deedsVersion", deeds.version())
                    .argument("totemDeedsVersion", totem.getDeedsVersion())
                    .send();
            e.setCancelled(true);
            return;
        }

        region.setPermissions(deeds.permissions().toArray(new Permission[0]));
        region.save(player);
    }

    @EventHandler
    void onBlockPlaceAttempt(BlockRightClickedEvent e) {
        if (e.isCancelled()) { return; }

        ItemStack item = e.getItem();
        if (item == null) { return; }
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
        this.translator_.translate(e.getPlayer(), (isSign) ? "block_place_reject.sign" : "block_place_reject.lectern")
                .argument(RegionArgument.of(rejectingTotem.region()))
                .send();
    }




    //SUB-LISTENERS
    private void onTotemExpand(Totem totem, TotemCoreRightClickedEvent e, ExpansionTable.Row row) {
        ItemStack itemStack = e.getItemStack();
        if (itemStack == null) {
            return;
        }
        if (itemStack.getAmount() < row.consumption()) {
            return;
        }

        TotemCore.Direction direction = e.getCore().getDirection();
        double scalar = row.pickExpansionScalar();
        ExpansionResult result = totem.expand(expansionFor(direction, scalar));
        Player player = e.getPlayer();
        if (!result.hasGrowth()) {
            this.sendExpansionBlockedMessage(player, direction, result);
            return;
        }

        consumeItems(player, e.getHand(), itemStack, row.consumption());
        swingHand(player, e.getHand());
        totem.region().display(player);
        this.sendExpansionFeedback(player, totem, direction, result);
        e.setCancelled(true);
    }
    public void onDeedsCreation(Player player, Totem totem, EquipmentSlot hand, ItemStack item) {
        if (item.getItemMeta() != null && !item.getItemMeta().getEnchants().isEmpty()) {
            return;
        }

        if (item.getItemMeta() instanceof BookMeta existingBookMeta) {
            DeedsInterpretationResult interpretation = this.service_.getDeedsFactory().interpret(existingBookMeta);
            if (!(interpretation instanceof DeedsInterpretationResult.NotADeedsBook)) {
                return;
            }
        }

        ItemStack deedsStack = new ItemStack(Material.WRITABLE_BOOK, 1);
        if (!(deedsStack.getItemMeta() instanceof BookMeta bookMeta)) {
            return;
        }
        this.service_.getDeedsFactory().generate(player, bookMeta, totem);
        deedsStack.setItemMeta(bookMeta);
        giveDeedsBook(player, hand, item, deedsStack);
        totem.save();
    }


    //HELPERS
    private String defaultTotemName(Player placer, Region region) {
        if (placer != null) {
            return this.translator_.translate(placer, "default_name.player_placed")
                    .argument("player", placer.getName())
                    .orDefault("{player}'s lands").message();
        }
        return this.translator_.translate(this.defaultLocale(), "default_name.undefined_placer")
                .argument(RegionArgument.of(region))
                .orDefault("Unnamed region").message();
    }

    private void sendRenamedMessage(Region region, Player renamer, String oldName, String newName) {
        String coloredOldName = RegionColors.displayName(region, oldName);
        String coloredNewName = RegionColors.displayName(region, newName);

        this.translator_.translate(renamer, "rename.first_person")
                .argument("oldName", coloredOldName)
                .argument("newName", coloredNewName)
                .argument(RegionArgument.of(region))
                .orDefault("Region renamed to '{newName}'.").send();

        List<Player> members = region.getOnlineMembers(p -> !p.getUniqueId().equals(renamer.getUniqueId()));
        if (members.isEmpty()) { return; }

        for (Player member : members) {
            this.translator_.translate(member, "rename.third_person")
                    .argument("oldName", coloredOldName)
                    .argument("newName", coloredNewName)
                    .argument("renamer", renamer.getName())
                    .argument(RegionArgument.of(region))
                    .send();
        }
    }

    private void sendPlacementRejectedMessage(Player player, Collection<Region> blockers) {
        String blockerNames = this.formatRegionList(player, blockers);
        if (!blockerNames.isBlank()) {
            this.translator_.translate(player, "creation.blocked")
                    .argument("blockers", blockerNames)
                    .orDefault("Cannot claim here: {blockers}. Move the totem.")
                    .send();
            return;
        }

        this.translator_.translate(player, "creation.failed")
                .orDefault("Cannot claim here. Move the totem.")
                .send();
    }

    private void sendExpansionFeedback(Player player, Totem totem, TotemCore.Direction direction, ExpansionResult result) {
        Argument[] arguments = this.expansionArguments(player, totem, direction, result);
        if (this.isShifted(result)) {
            this.translator_.translate(player, "expansion.shifted")
                    .arguments(arguments)
                    .orDefault("Expanded {direction}; shifted around {blockers}. Size: H {height}, X {x}, Z {z}.")
                    .send();
            return;
        }

        if (result.unachievedTotal() > EPSILON) {
            this.translator_.translate(player, "expansion.partial")
                    .arguments(arguments)
                    .orDefault("Expanded {direction}; {blockers} blocked the rest. Size: H {height}, X {x}, Z {z}.")
                    .send();
            return;
        }

        this.translator_.translate(player, "expansion.success")
                .arguments(arguments)
                .orDefault("Expanded {direction}. Size: H {height}, X {x}, Z {z}.")
                .send();
    }

    private void sendExpansionBlockedMessage(Player player, TotemCore.Direction direction, ExpansionResult result) {
        String blockerNames = this.formatRegionList(player, result.blockingRegions());
        if (!blockerNames.isBlank()) {
            this.translator_.translate(player, "expansion.blocked")
                    .argument("direction", this.translateDirection(player, direction))
                    .argument("blockers", blockerNames)
                    .orDefault("Cannot expand {direction}: {blockers}.")
                    .send();
            return;
        }

        this.translator_.translate(player, "expansion.blocked_generic")
                .argument("direction", this.translateDirection(player, direction))
                .orDefault("Cannot expand {direction}.").send();
    }

    private Argument[] expansionArguments(Player player, Totem totem, TotemCore.Direction direction, ExpansionResult result) {
        BoundingBox bounds = totem.region().getBoundingBox();
        return new Argument[] {
                Argument.of("direction", this.translateDirection(player, direction)),
                Argument.of("height", formatMeasure(bounds.getHeight())),
                Argument.of("x", formatMeasure(bounds.getWidthX())),
                Argument.of("z", formatMeasure(bounds.getWidthZ())),
                Argument.of("blockers", this.formatRegionList(player, result.blockingRegions()))
        };
    }

    private String translateDirection(Player player, TotemCore.Direction direction) {
        String key = "expansion.direction." + direction.name().toLowerCase();
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
            return this.translator_.translate(player, key).orDefault(fallback).message();
        }
        return this.translator_.translate(this.defaultLocale(), key).orDefault(fallback).message();
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
            return this.translator_.formatList(player, names);
        }
        return String.join(", ", names);
    }

    private String regionName(Player player, Region region) {
        String name = region.getName();
        if (name != null && !name.isBlank()) {
            return RegionColors.displayName(region, name);
        }

        String fallback;
        if (player != null) {
            fallback = this.translator_.translate(player, "region.unnamed")
                    .argument(RegionArgument.of(region))
                    .orDefault("Region #{region.id}")
                    .message();
        }
        else {
            fallback = this.translator_.translate(this.defaultLocale(), "region.unnamed")
                    .argument(RegionArgument.of(region))
                    .orDefault("Region #{region.id}")
                    .message();
        }
        return RegionColors.displayName(region, fallback);
    }

    private String defaultLocale() {
        return this.translator_.getRoot().getDefaultLocale();
    }

    private void captureRegionColorFromSign(Block block, Totem totem) {
        if (!(block.getState() instanceof Sign sign)) {
            return;
        }

        Totem currentOwner = this.service_.ownerOfSign(sign).orElse(null);
        if (currentOwner == null || !currentOwner.id().equals(totem.id())) {
            return;
        }

        RegionColors.set(totem.region(), RegionColor.fromDyeColor(sign.getSide(Side.FRONT).getColor()));
        totem.region().save();
    }

    private void syncNameSignColor(Totem totem) {
        Sign sign = totem.nameSign().orElse(null);
        if (sign == null || sign.isWaxed()) {
            return;
        }

        sign.getSide(Side.FRONT).setColor(RegionColors.getOrDefault(totem.region()).dyeColor());
        sign.update(true, false);
    }

    private void scheduleNameSignColorSync(Block block, Totem totem) {
        this.plugin_.getServer().getScheduler().runTask(this.plugin_, () -> {
            Totem currentOwner = null;
            if (block.getState() instanceof Sign sign) {
                currentOwner = this.service_.ownerOfSign(sign).orElse(null);
            }
            if (currentOwner == null || !currentOwner.id().equals(totem.id())) {
                return;
            }
            this.syncNameSignColor(currentOwner);
        });
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

    private static @Nullable DyeColor dyeColorOf(@Nullable ItemStack item) {
        if (item == null) {
            return null;
        }

        return switch (item.getType()) {
            case WHITE_DYE -> DyeColor.WHITE;
            case ORANGE_DYE -> DyeColor.ORANGE;
            case MAGENTA_DYE -> DyeColor.MAGENTA;
            case LIGHT_BLUE_DYE -> DyeColor.LIGHT_BLUE;
            case YELLOW_DYE -> DyeColor.YELLOW;
            case LIME_DYE -> DyeColor.LIME;
            case PINK_DYE -> DyeColor.PINK;
            case GRAY_DYE -> DyeColor.GRAY;
            case LIGHT_GRAY_DYE -> DyeColor.LIGHT_GRAY;
            case CYAN_DYE -> DyeColor.CYAN;
            case PURPLE_DYE -> DyeColor.PURPLE;
            case BLUE_DYE -> DyeColor.BLUE;
            case BROWN_DYE -> DyeColor.BROWN;
            case GREEN_DYE -> DyeColor.GREEN;
            case RED_DYE -> DyeColor.RED;
            case BLACK_DYE -> DyeColor.BLACK;
            default -> null;
        };
    }

    private void sendDeedsDeTranspileErrorFeedback(Player player, DeedsInterpretationResult.DeTranspileError error) {

        List<Argument> basePlaceHolders = Stream.concat(
                error.region().stream().map(RegionArgument::of),
                Stream.of(
                        Argument.of("lineNumber", error.lineNumber()),
                        Argument.of("line", error.line()),
                        Argument.of("pageNumber", error.page())
                )
        ).toList();

        String key; List<Argument> detailPlaceHolders;

        String lineContent = error.line();
        if (lineContent == null || lineContent.isBlank()) {
            key = "deeds.formatting_error.empty_input";
            detailPlaceHolders = List.of();
        } else {
            Pair<String, List<Argument>> details = getDeTranspileErrorTranslationDetails(error.error());
            key = details.first();
            detailPlaceHolders = details.second();
        }

        Argument[] arguments = Stream.concat(
                basePlaceHolders.stream(),
                detailPlaceHolders.stream()
        ).toArray(Argument[]::new);

        this.translator_.translate(player, key)
                .arguments(arguments)
                .async()
                .message()
                .thenCompose(detail -> {
                    Argument[] args = Arrays.copyOf(arguments, arguments.length + 1);
                    args[args.length - 1] = Argument.of("details", detail);
                    return this.translator_.translate(player, "deeds.formatting_error.message").arguments(args).send();
                });
    }

    private Pair<String, List<Argument>> getDeTranspileErrorTranslationDetails(DeedsDeTranspilingError error) {

        final String prefix = "deeds.formatting_error.detail.";

        return switch (error) {
            case DeedsDeTranspilingError.PlayerNotFound playerNotFound -> Pair.of(
                    prefix + "player_not_found",
                    List.of(Argument.of("playerName", playerNotFound.name()))
            );
            case DeedsDeTranspilingError.UnexpectedCharacter unexpectedCharacter -> Pair.of(
                    prefix + "unexpected_character",
                    List.of(Argument.of("column", unexpectedCharacter.index()))
            );
            case DeedsDeTranspilingError.UnknownGroup unknownGroup -> Pair.of(
                    prefix + "unknown_group",
                    List.of(Argument.of("groupName", unknownGroup.groupName()))
            );
            case DeedsDeTranspilingError.NoGroupProvided ignored -> Pair.of(
                    prefix + "no_group_name_provided",
                    List.of()
            );
        };
    }

    private static Expansion expansionFor(TotemCore.Direction direction, double amount) {
        if (direction == TotemCore.Direction.ALL) {
            return Expansion.all(amount / 6d);
        }
        return Expansion.forDirection(direction, amount);
    }

    private static void consumeItems(Player player, EquipmentSlot hand, ItemStack stack, int amount) {
        if (stack.getAmount() <= amount) {
            stack = new ItemStack(Material.AIR);
        } else {
            stack.setAmount(stack.getAmount() - amount);
        }
        player.getInventory().setItem(hand, stack);
    }

    private static void giveDeedsBook(Player player, EquipmentSlot hand, ItemStack paymentStack, ItemStack deedsStack) {
        if (paymentStack.getAmount() < 2) {
            player.getInventory().setItem(hand, deedsStack);
            return;
        }

        paymentStack.setAmount(paymentStack.getAmount() - 1);
        player.getInventory().setItem(hand, paymentStack);
        player.getInventory().addItem(deedsStack).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover)
        );
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
