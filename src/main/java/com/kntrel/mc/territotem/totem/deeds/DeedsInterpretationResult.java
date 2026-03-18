package com.kntrel.mc.territotem.totem.deeds;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.inventory.meta.BookMeta;
import org.jspecify.annotations.NonNull;
import java.util.Optional;

public sealed interface DeedsInterpretationResult {

    //TYPES
    final class Success implements DeedsInterpretationResult {

        //FIELDS
        private final Deeds deeds_;

        //CONSTRUCTOR
        public Success(@NonNull Deeds deeds) { this.deeds_ = deeds; }

        //IMPLEMENTATION
        @Override public Optional<Deeds> deeds() { return Optional.of(deeds_); }

    }
    final class DeTranspileError implements DeedsInterpretationResult {

        //FIELDS
        private final int page_;
        private final int lineNumber_;
        private final String line_;
        private final DeedsDeTranspilingError error_;
        private final Region region_;

        //CONSTRUCTOR
        public DeTranspileError(int page, int lineNumber, String line, DeedsDeTranspilingError error, Region region) {
            this.page_ = page;
            this.lineNumber_ = lineNumber;
            this.line_ = line;
            this.error_ = error;
            this.region_ = region;
        }

        //GETTERS
        public int page() { return this.page_; }
        public int lineNumber() { return this.lineNumber_; }
        public String line() { return this.line_; }
        public DeedsDeTranspilingError error() { return this.error_; }
        @Override public Optional<Region> region() { return Optional.of(this.region_); }
    }
    record WrongNameSpace(String foundNameSpace) implements DeedsInterpretationResult {}
    record NonExistentRegion(long foundId) implements DeedsInterpretationResult {}
    record NotADeedsBook(BookMeta providedBook) implements DeedsInterpretationResult {}


    //FACTORY
    static Success success(Deeds deeds) {
        return new Success(deeds);
    }
    static DeTranspileError deTranspileError(int page, int lineNumber, String line, DeedsDeTranspilingError error, Region region) {
        return new DeTranspileError(page, lineNumber, line, error, region);
    }
    static WrongNameSpace wrongNameSpace(String foundNameSpace) {
        return new WrongNameSpace(foundNameSpace);
    }
    static NonExistentRegion nonExistentRegion(long foundId) {
        return new NonExistentRegion(foundId);
    }
    static NotADeedsBook notADeedsBook(BookMeta providedBook) {
        return new NotADeedsBook(providedBook);
    }


    //DEFAULTS
    default Optional<Deeds> deeds() {
        return Optional.empty();
    }
    default Optional<Region> region() {
        return this.deeds().map(Deeds::region);
    }
}
