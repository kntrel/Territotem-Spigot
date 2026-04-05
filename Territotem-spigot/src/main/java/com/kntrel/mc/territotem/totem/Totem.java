package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.blueprint.InvalidBlueprintException;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.deeds.Deeds;
import com.kntrel.mc.territotem.totem.deeds.DeedsFactory;
import com.kntrel.mc.territotem.totem.piece.TotemLecternPiece;
import com.kntrel.mc.territotem.totem.piece.TotemNameSignPiece;
import com.kntrel.mc.territotem.totem.piece.TotemTile;
import com.kntrel.mc.territotem.totem.region.Expansion;
import com.kntrel.mc.territotem.totem.region.ExpansionResult;
import com.kntrel.mc.territotem.totem.region.ExpansionTable;
import com.kntrel.mc.territotem.util.ItemStackInfo;
import com.kntrel.util.Vec3i;
import org.bukkit.SoundCategory;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

public class Totem {

    //CONSTANTS
    public static final long
            AMBIENT_SOUND_RATE  = 80L;
    public static final String
            AMBIENT_SOUND       = "block.beacon.ambient",
            FEED_SOUND          = "block.respawn_anchor.charge",
            HIT_SOUND           = "entity.blaze.hurt",
            DEEDS_CREATE_SOUND  = "block.enchantment_table.use";


    //FIELDS
    private final TotemService service_;
    private final Structure structure_;
    private final Region region_;
    private final TotemCore core_;
    private final ExpansionTable expansionTable_;
    private final double defaultDropBackRate_;
    private final Deque<TotemGrowthEntry> growthHistory_;
    private BoundingBox baseBounds_;
    private int deedsVersion_;


    //CONSTRUCTORS
    Totem(
            TotemService provenance,
            Structure structure,
            Region region,
            TotemCore core,
            ExpansionTable expansionTable,
            double defaultDropBackRate
    ) {
        if (!(structure.blueprint() instanceof TotemBlueprint)) {
            throw new InvalidBlueprintException(
                "A totem must be backed by a totem blueprint. Passed structure's blueprint member is not an instance of TotemBlueprint"
            );
        }
        this.service_ = provenance;
        this.structure_ = structure;
        this.region_ = region;
        this.core_ = core;
        this.expansionTable_ = (expansionTable == null) ? ExpansionTable.empty() : expansionTable;
        this.defaultDropBackRate_ = defaultDropBackRate;
        this.growthHistory_ = new ArrayDeque<>();
        this.baseBounds_ = region.getBoundingBox();
        this.deedsVersion_ = 0;
    }


    //API
    public Structure structure() {
        return this.structure_;
    }
    public UUID id() {
        return this.structure_.id();
    }
    public Vec3i origin() {
        return this.structure_.origin();
    }
    public TotemBlueprint blueprint() {
        return (TotemBlueprint) this.structure_.blueprint();
    }
    public Region region() {
        return this.region_;
    }
    public TotemCore core() {
        return this.core_;
    }
    public BoundingBox baseBounds() {
        return this.baseBounds_.clone();
    }
    public List<TotemGrowthEntry> growthHistory() {
        return List.copyOf(this.growthHistory_);
    }
    public FeedResult feed(@Nullable ItemStack stack) {
        TotemCore.Direction direction = this.core_.getDirection();
        if (stack == null) { return FeedResult.ignored(direction); }

        ExpansionTable.Row row = this.expansionTable_.findMatch(stack).orElse(null);
        if (row == null) {
            return FeedResult.ignored(direction);
        }
        if (stack.getAmount() < row.consumption()) {
            return FeedResult.insufficient(direction);
        }

        double scalar = row.pickExpansionScalar();
        ExpansionResult result = this.service_.expand(
                this,
                expansionFor(direction, scalar),
                refundStackData(stack, row.consumption()),
                row.dropBackRateOr(this.defaultDropBackRate_)
        );
        if (!result.hasGrowth()) {
            return FeedResult.blocked(direction, result);
        }

        ItemStack updatedStack = consumeItems(stack, row.consumption());
        this.world().playSound(this.core_.getLocation(), FEED_SOUND, 5, 1.5f);
        return FeedResult.expanded(direction, result, updatedStack);
    }
    public @Nullable TotemGrowthEntry takeDamage() {
        return this.takeDamage(ThreadLocalRandom.current()::nextDouble);
    }
    public void emitAmbientSound() {
        if (this.core_.getState() != TotemCore.State.ACTIVE) {
            return;
        }
        this.core_.getWorld().playSound(this.core_.getCenter(), AMBIENT_SOUND, SoundCategory.BLOCKS, 1, .8f);
    }
    public Deeds createDeedsBook(Player player) {
        DeedsFactory deedsFactory = this.service_.getDeedsFactory();
        Deeds deeds = deedsFactory.generate(player, this);
        this.save();
        this.world().playSound(this.core_.getLocation(), DEEDS_CREATE_SOUND, 5, 1f);
        return deeds;
    }
    public World world() {
        return this.structure_.world();
    }
    public int getDeedsVersion() {
        return this.deedsVersion_;
    }
    public void setDeedsVersion(int newVersion) {
        this.deedsVersion_ = newVersion;
    }
    public int incrementAndGetDeedsVersion() {
        return ++this.deedsVersion_;
    }
    public void loadGrowthState(BoundingBox baseBounds, Iterable<TotemGrowthEntry> history) {
        this.baseBounds_ = baseBounds.clone();
        this.growthHistory_.clear();
        if (history == null) {
            return;
        }
        for (TotemGrowthEntry growth : history) {
            if (growth == null || growth.expansion().isZero()) {
                continue;
            }
            this.growthHistory_.addLast(growth);
        }
    }
    public void recordGrowth(TotemGrowthEntry growth) {
        if (growth == null || growth.expansion().isZero()) {
            return;
        }
        this.growthHistory_.addLast(growth);
    }
    public TotemGrowthEntry latestGrowth() {
        return this.growthHistory_.peekLast();
    }
    public TotemGrowthEntry discardLatestGrowth() {
        return this.growthHistory_.pollLast();
    }
    public Optional<Sign> nameSign() {
        World world = this.world();
        for (TotemTile<TotemNameSignPiece> sign : this.blueprint().nameSings()) {
            Vec3i cords = sign.offset().add(this.origin());
            WorldTile worldTile = WorldTile.of(cords, world);
            if (!sign.piece().isPlaced(worldTile)) { continue; }
            Block b = this.world().getBlockAt(cords.x(), cords.y(), cords.z());
            if (b.getState() instanceof Sign s) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
    public Optional<Lectern> lectern() {
        World world = this.world();
        for (TotemTile<TotemLecternPiece> lectern : this.blueprint().lecterns()) {
            Vec3i cords = lectern.offset().add(this.origin());
            WorldTile worldTile = WorldTile.of(cords, world);
            if (!lectern.piece().isPlaced(worldTile)) { continue; }
            Block b = this.world().getBlockAt(cords.x(), cords.y(), cords.z());
            if (b.getState() instanceof Lectern l) {
                return Optional.of(l);
            }
        }
        return Optional.empty();
    }
    public boolean isEnabled() {
        return this.region_.isEnabled();
    }
    public void setEnabled(boolean enabled) {
        if (enabled == this.isEnabled()) { return; }
        this.core().setState(enabled ? TotemCore.State.ACTIVE : TotemCore.State.INACTIVE);
        this.region_.enabled(enabled);
        this.region_.save();
    }
    public boolean isDestroyed() {
        return this.region_.isDestroyed();
    }
    public void rename(String newName) {
        this.region_.setName(newName);
        this.region_.save();
    }
    public void save() {
        TotemClaim claim = TotemClaim.of(this);
        TotemClaim.write(this.region(), claim);
        TotemGrowthHistory.write(this.region(), TotemGrowthHistory.of(this));
        this.region().save();
    }
    public void destroy() {
        this.structure_.drop();
        this.region_.destroy();
        this.region_.save();
    }


    //HELPERS
    @Nullable TotemGrowthEntry takeDamage(DoubleSupplier random) {
        TotemGrowthEntry growth = this.service_.rollbackLastGrowth(this);
        if (growth == null) { return null; }

        this.dropBackItems(growth, random);
        this.world().playSound(this.core_.getLocation(), HIT_SOUND, 5, .8f);
        return growth;
    }
    private void dropBackItems(TotemGrowthEntry growth, DoubleSupplier random) {
        ItemStackInfo refundStack = growth.refundStack();
        if (refundStack == null) {
            return;
        }

        ItemStack template = refundStack.toItemStack();
        int refunded = rollDropBackCount(growth.refundQuantity(), growth.dropBackRate(), random);
        if (refunded <= 0) {
            return;
        }

        int maxStackSize = maxStackSize(template);
        while (refunded > 0) {
            int batch = Math.min(refunded, maxStackSize);
            ItemStack drop = template.clone();
            drop.setAmount(batch);
            this.core_.getWorld().dropItemNaturally(this.core_.getCenter(), drop);
            refunded -= batch;
        }
    }
    private static Expansion expansionFor(TotemCore.Direction direction, double amount) {
        if (direction == TotemCore.Direction.ALL) {
            return Expansion.all(amount / 6d);
        }
        return Expansion.forDirection(direction, amount);
    }
    static int rollDropBackCount(int attempts, double dropBackRate, DoubleSupplier random) {
        if (attempts <= 0 || dropBackRate <= 0d) {
            return 0;
        }
        if (dropBackRate >= 1d) {
            return attempts;
        }

        int refunded = 0;
        for (int i = 0; i < attempts; i++) {
            if (random.getAsDouble() <= dropBackRate) {
                refunded++;
            }
        }
        return refunded;
    }
    private static int maxStackSize(ItemStack template) {
        try {
            return Math.max(1, template.getMaxStackSize());
        } catch (Throwable ignored) {
            return 64;
        }
    }
    private static ItemStack consumeItems(ItemStack stack, int amount) {
        if (stack.getAmount() <= amount) {
            return new ItemStack(Material.AIR);
        }
        stack.setAmount(stack.getAmount() - amount);
        return stack;
    }
    private static ItemStackInfo refundStackData(ItemStack source, int amount) {
        ItemStack refund = source.clone();
        refund.setAmount(amount);
        return ItemStackInfo.fromItemStack(refund);
    }


    //SUBTYPES
    public record FeedResult(
            Status status,
            TotemCore.Direction direction,
            @Nullable ExpansionResult expansionResult,
            @Nullable ItemStack updatedStack
    ) {

        static FeedResult ignored(TotemCore.Direction direction) {
            return new FeedResult(Status.IGNORED, direction, null, null);
        }

        static FeedResult insufficient(TotemCore.Direction direction) {
            return new FeedResult(Status.INSUFFICIENT_ITEMS, direction, null, null);
        }

        static FeedResult blocked(TotemCore.Direction direction, ExpansionResult expansionResult) {
            return new FeedResult(Status.BLOCKED, direction, expansionResult, null);
        }

        static FeedResult expanded(TotemCore.Direction direction, ExpansionResult expansionResult, ItemStack updatedStack) {
            return new FeedResult(Status.EXPANDED, direction, expansionResult, updatedStack);
        }

        public boolean isIgnored() {
            return this.status == Status.IGNORED;
        }

        public boolean hasGrowth() {
            return this.status == Status.EXPANDED;
        }

        public enum Status {
            IGNORED,
            INSUFFICIENT_ITEMS,
            BLOCKED,
            EXPANDED
        }
    }
}
