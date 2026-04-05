package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.util.BitSet3D;
import com.kntrel.util.IntBoundingBox;
import com.kntrel.util.Vec3i;
import org.bukkit.World;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

public class Structure {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(Structure.class);
    public enum State { EMPTY, IN_PROGRESS, COMPLETE }


    //FIELDS
    private final UUID id_;
    private final StructureService service_;
    private final Blueprint blueprint_;
    private final World world_;
    private final Vec3i origin_;
    private final IntBoundingBox boundingBox_;
    private final BitSet3D presenceMap_, knownMap_, matchMap_;
    private final Object mutex_;
    private final BiFunction<Vec3i, World, WorldTile> worldTileGetter_;


    //MUTABLE STATE
    private int pieceCount_, knownCount_, matchCount_;
    private CompletableFuture<Void> scanTask_;
    private Vec3i parkedUnlockOffset_;
    private boolean dropped_;
    private State state_;


    //CONSTRUCTOR
    public Structure(UUID id, StructureService service, Blueprint blueprint, World world, Vec3i origin, BiFunction<Vec3i, World, WorldTile> worldTileGetter) {
        this.id_ = id;
        this.service_ = service;
        this.blueprint_ = blueprint;
        this.world_ = world;
        this.origin_ = origin;
        this.mutex_ = new Object();
        this.parkedUnlockOffset_ = null;
        this.worldTileGetter_ = worldTileGetter;

        Vec3i dimensions = blueprint.dimensions();
        Vec3i end = new Vec3i(
                origin.x() + dimensions.x() - 1,
                origin.y() + dimensions.y() - 1,
                origin.z() + dimensions.z() - 1
        );
        this.boundingBox_ = new IntBoundingBox(origin, end);
        this.presenceMap_ = this.service_.getBitsetGenerator().generate(blueprint);
        this.knownMap_ = new BitSet3D(dimensions);
        this.matchMap_ = new BitSet3D(dimensions);
        this.pieceCount_ = this.blueprint_.elementCount();
        this.matchCount_ = 0;
        this.dropped_ = false;
        this.state_ = null;

        log(Level.TRACE, "Initialized with {} pieces", this.pieceCount_);
        this.fullScan();
    }
    public Structure(UUID id, StructureService service, Blueprint blueprint, World world, Vec3i origin) {
        this(id, service, blueprint, world, origin, WorldTile::of);
    }


    //GETTERS
    public UUID id() { return this.id_; }
    public Blueprint blueprint() { return this.blueprint_; }
    public World world() { return this.world_; }
    public Vec3i origin() { return this.origin_; }
    public IntBoundingBox boundingBox() { return this.boundingBox_; }
    public boolean isLocked() { synchronized (this.mutex_) { return this.dropped_; } }


    //API
    public @Nullable Piece pieceAt(Vec3i coordinates) {
        Vec3i offset = coordinates.subtract(this.origin_);
        return this.blueprint_.pieceAt(offset);
    }
    public void drop() {
        synchronized (this.mutex_) {
            log(Level.TRACE, "Dropping");
            this.dropped_ = true;
            this.service_.dropStructure(this);
        }
    }
    public boolean contains(Vec3i coordinate) { return this.boundingBox_.contains(coordinate); }
    public boolean isParked() { synchronized (this.mutex_) {
        return this.parkedUnlockOffset_ != null;
    }}
    public Optional<Vec3i> parkedAt() { synchronized (this.mutex_) {
        return Optional.ofNullable(this.parkedUnlockOffset_);
    }}
    public float completion() { synchronized (this.mutex_) {
        if (this.pieceCount_ == 0) { return 0.0f; }
        return (this.matchCount_ / (float) this.pieceCount_);
    }}
    public State getState() { synchronized (this.mutex_) {
        CompletableFuture<Void> scanTask;
        synchronized (this.mutex_) { scanTask = this.scanTask_; }
        if (scanTask != null) { wait(scanTask); }

        synchronized (this.mutex_) {
            return this.state_;
        }
    }}
    public boolean isComplete() {
        return this.getState() == State.COMPLETE;
    }
    public boolean isInProgress() {
        return this.getState() == State.IN_PROGRESS;
    }
    public boolean isEmpty() {
        return this.getState() == State.EMPTY;
    }
    public @NonNull UpdateResult update(Vec3i offset) {

        log(Level.TRACE, "Block update at offset {}", offset);
        CompletableFuture<Void> pendingScan;
        synchronized (this.mutex_) {
            if (this.dropped_) {
                log(Level.TRACE, "Dropped. Ignoring update");
                return UpdateResult.noChange();
            }
            pendingScan = this.scanTask_;
        }
        if (pendingScan != null) { wait(pendingScan); }

        Vec3i coordinates = this.origin_.add(offset);
        WorldTile worldTile = this.worldTileGetter_.apply(coordinates, this.world_);
        pendingScan = null;
        synchronized (this.mutex_) {
            if (this.parkedUnlockOffset_ != null) {
                if (!this.parkedUnlockOffset_.equals(offset)) {
                    log(Level.TRACE, "Parked offset {} does not match update offset {}", this.parkedUnlockOffset_, offset);
                    return UpdateResult.noChange();
                }
                if (!this.presenceMap_.get(offset) || this.blueprint_.matchesAt(offset, worldTile)) {
                    pendingScan = this.fullScan();
                } else { return UpdateResult.noChange(); }
            }
        }
        if (pendingScan != null) { wait(pendingScan); }

        if (!this.presenceMap_.get(offset)) {
            log(Level.TRACE, "Offset {} not in presence map", offset);
            return UpdateResult.noChange();
        }
        while (true) {
            CompletableFuture<Void> scan;
            synchronized (this.mutex_) { scan = this.scanTask_; }
            if (scan != null) { wait(scan); continue; }

            Tile piece = this.blueprint_.pieceAt(offset);
            if (piece == null) { return UpdateResult.noChange(); }

            boolean match = piece.matches(worldTile);
            log(Level.DEBUG, "Piece at offset {} " + (match ? "matches" : "does not match"), offset);
            synchronized (this.mutex_) {
                State oldState = this.state_;
                if (this.scanTask_ != null) { continue; }
                this.markMatched(offset, match);
                if (this.matchCount_ == this.pieceCount_) {
                    this.setComplete();
                } else if (this.matchCount_ < 1) {
                    this.setEmpty();
                } else {
                    this.setInProgress();
                }
                if (!match && this.shouldPark()) { this.parkAt(offset); }
                return UpdateResult.change(piece, match, oldState, this.state_);
            }
        }
    }
    public CompletableFuture<Void> fullScan() {
        synchronized (this.mutex_) {
            if (this.parkedUnlockOffset_ != null) {
                log(Level.TRACE, "Unparking from offset {}", this.parkedUnlockOffset_);
                this.parkedUnlockOffset_ = null;
            }

            if (this.scanTask_ != null) {
                log(Level.TRACE, "Full scan already in progress");
                return this.scanTask_;
            }
            CompletableFuture<Void> scanTask = this.service_.runInMainThreadAsync(() -> { this.fullScanTask(); return null; })
                    .thenAccept(v -> { synchronized (this.mutex_) { this.scanTask_ = null; } });
            synchronized (this.mutex_) {
                this.scanTask_ = (scanTask.isDone()) ? null : scanTask;
            }
            return scanTask;
        }
    }
    @Override public String toString() {
        return "[StructureTracker@"
                + Integer.toHexString(System.identityHashCode(this))
                + "] "
                + this.id_
                + " "
                + this.origin_
                + " Blueprint "
                + this.blueprint_.id()
                + " - "
                + this.blueprint_.name();
    }


    //HELPERS
    private boolean markKnown(Vec3i offset) {
        if (!this.knownMap_.get(offset) && this.presenceMap_.get(offset)) {
            log(Level.TRACE, "Marking offset {} as known", offset);
            this.knownMap_.set(offset);
            this.knownCount_++;
            return false;
        }
        return true;
    }
    private void markMatched(Vec3i offset, boolean matched) {
        this.markKnown(offset);
        boolean wasMatched = this.matchMap_.get(offset);
        if (wasMatched == matched) { return; }
        log(Level.DEBUG, "Block at offset {} " + (matched ? "matched" : "unmatched"), offset);
        if (matched) { this.matchCount_++; } else { this.matchCount_--; }
        this.matchMap_.set(offset, matched);
    }
    private void parkAt(Vec3i offset) {
        if (!this.presenceMap_.get(offset)) { return; }
        log(Level.TRACE, "Parking at offset {}", offset);
        this.state_ = State.IN_PROGRESS;
        this.parkedUnlockOffset_ = offset;
    }
    private void setComplete() {
        this.state_ = State.COMPLETE;
    }
    private void setInProgress() {
        this.state_ = State.IN_PROGRESS;
    }
    private void setEmpty() {
        this.state_ = State.EMPTY;
    }
    private boolean shouldPark() {
        int threshold = (this.pieceCount_ + 1) / 2;
        if (this.knownCount_ < threshold) return false;
        int mismatched = this.knownCount_ - this.matchCount_;
        return mismatched >= threshold;
    }
    private void fullScanTask() {
        log(Level.TRACE, "Starting full scan task");
        synchronized (this.mutex_) {
            this.matchMap_.clear();
            this.knownMap_.clear();
            this.matchCount_ = 0;
            this.knownCount_ = 0;
            this.parkedUnlockOffset_ = null;
        }

        for (var entry : this.blueprint_.piecesByOffset().entrySet()) {
            Vec3i offset = entry.getKey();
            Vec3i coordinates = offset.add(this.origin_);
            Piece piece = entry.getValue();
            WorldTile wTile = this.worldTileGetter_.apply(coordinates, this.world_);
            boolean match = piece.matches(wTile);

            synchronized (this.mutex_) {
                this.markMatched(offset, match);
                if (!match && this.shouldPark()) {
                    this.parkAt(offset);
                    break;
                }
            }
        }

        synchronized (this.mutex_) {
            if (this.matchCount_ == this.pieceCount_) {
                log(Level.INFO, "Candidate complete: {} blocks matched", this.matchCount_);
                this.setComplete();
            } else if (this.matchCount_ < 1) {
                log(Level.DEBUG, "Full scan complete. No matches");
                this.setEmpty();
            } else {
                log(Level.TRACE, "Full scan complete: {} / {} blocks matched", this.matchCount_, this.pieceCount_);
                this.setInProgress();
            }
        }
    }
    private void log(Level level, String message, Object... args) {
        String logMessage = this + " - " + message;
        LOGGER.atLevel(level).log(logMessage, args);
    }
    private static void wait(Future<?> task) {
        try {
            task.get();
        } catch (Exception e) {
            LOGGER.error("Error waiting for future task", e);
            throw new RuntimeException(e);
        }
    }

}
