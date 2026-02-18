package com.kntrel.mc.territotem.blueprint;

import com.kntrel.util.BitSet3D;
import com.kntrel.util.IntBoundingBox;
import com.kntrel.util.Vec3i;
import org.bukkit.World;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class BlueprintTracker {

    //FIELDS
    private final BlueprintRegistry service_;
    private final Blueprint blueprint_;
    private final World world_;
    private final Vec3i origin_;
    private final IntBoundingBox boundingBox_;
    private final BitSet3D presenceMap_, knownMap_, matchMap_;
    private final Object mutex_;
    private int elementCount_, knownCount_, matchCount_;
    private CompletableFuture<Void> scanTask_;
    private Vec3i parkedUnlockOffset_;
    private boolean locked_;


    //CONSTRUCTOR
    public BlueprintTracker(BlueprintRegistry service, Blueprint blueprint, World world, Vec3i origin) {
        this.service_ = service;
        this.blueprint_ = blueprint;
        this.world_ = world;
        this.origin_ = origin;
        this.mutex_ = new Object();
        this.parkedUnlockOffset_ = null;

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
        this.elementCount_ = this.blueprint_.elementCount();
        this.matchCount_ = 0;
        this.locked_ = false;

        this.fullScan();
    }


    //GETTERS
    public Blueprint blueprint() { return this.blueprint_; }
    public World world() { return this.world_; }
    public Vec3i origin() { return this.origin_; }
    public IntBoundingBox boundingBox() { return this.boundingBox_; }
    public boolean isLocked() { synchronized (this.mutex_) { return this.locked_; } }


    //API
    public void lock() { synchronized (this.mutex_) { this.locked_ = true; } }
    public boolean contains(Vec3i coordinate) { return this.boundingBox_.contains(coordinate); }
    public boolean isComplete() {
        CompletableFuture<Void> scanTask;
        synchronized (this.mutex_) { scanTask = this.scanTask_; }
        if (scanTask != null) { wait(scanTask); }

        synchronized (this.mutex_) {
            return this.matchCount_ == this.elementCount_;
        }
    }
    public boolean isParked() { synchronized (this.mutex_) {
        return this.parkedUnlockOffset_ != null;
    }}
    public void update(Vec3i offset, BlueprintElement element) {
        CompletableFuture<Void> pendingScan;
        synchronized (this.mutex_) {
            if (this.locked_) { return; }
            pendingScan = this.scanTask_;
        }
        if (pendingScan != null) { wait(pendingScan); }

        pendingScan = null;
        synchronized (this.mutex_) {
            if (this.parkedUnlockOffset_ != null) {
                if (!this.parkedUnlockOffset_.equals(offset)) { return; }
                if (!this.presenceMap_.get(offset) || this.blueprint_.matchesAt(offset, element)) {
                    this.unPark();
                    pendingScan = this.scanTask_;
                } else { return; }
            }
        }
        if (pendingScan != null) { wait(pendingScan); }

        if (!this.presenceMap_.get(offset)) { return; }
        while (true) {
            CompletableFuture<Void> scan;
            synchronized (this.mutex_) { scan = this.scanTask_; }
            if (scan != null) { wait(scan); continue; }

            boolean match = blueprint_.matchesAt(offset, element);
            synchronized (this.mutex_) {
                if (this.scanTask_ != null) { continue; }
                this.markMatched(offset, match);
                if (!match && this.shouldPark()) { this.parkAt(offset); }
                return;
            }
        }
    }
    public CompletableFuture<Void> fullScan() {
        synchronized (this.mutex_) {
            if (this.scanTask_ != null) { return this.scanTask_; }
            this.scanTask_ = this.service_.runInMainThreadAsync(() -> { this.fullScanTask(); return null; })
                    .thenAccept(v -> { synchronized (this.mutex_) { this.scanTask_ = null; } });
            return this.scanTask_;
        }
    }



    //HELPERS
    private boolean markKnown(Vec3i offset) {
        if (!this.knownMap_.get(offset) && this.presenceMap_.get(offset)) {
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
        if (matched) { this.matchCount_++; } else { this.matchCount_--; }
        this.matchMap_.set(offset, matched);
    }
    private void unPark() {
        synchronized (this.mutex_) {
            if (this.parkedUnlockOffset_ == null) { return; }
            this.parkedUnlockOffset_ = null;
        }
        this.fullScan();
    }
    private void parkAt(Vec3i offset) {
        if (!this.presenceMap_.get(offset)) { return; }
        this.parkedUnlockOffset_ = offset;
    }
    private boolean shouldPark() {
        int threshold = (this.elementCount_ + 1) / 2;
        if (this.knownCount_ < threshold) return false;
        int mismatched = this.knownCount_ - this.matchCount_;
        return mismatched >= threshold;
    }
    private void fullScanTask() {
        synchronized (this.mutex_) {
            this.matchMap_.clear();
            this.knownMap_.clear();
            this.matchCount_ = 0;
            this.knownCount_ = 0;
            this.parkedUnlockOffset_ = null;
        }

        for (var entry : this.blueprint_.elementsByOffset().entrySet()) {
            var offset = entry.getKey();
            var element = entry.getValue();
            boolean match = element.matchesAt(offset, this.world_);

            synchronized (this.mutex_) {
                this.markMatched(offset, match);
                if (!match && this.shouldPark()) {
                    this.parkAt(offset);
                    break;
                }
            }
        }
    }
    private static void wait(Future<?> task) {
        try {
            task.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
