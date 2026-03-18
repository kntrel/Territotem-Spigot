package com.kntrel.mc.territotem.totem.deeds;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import java.util.Optional;

public sealed interface DeedsInterpretationResult {

    //TYPES
    final class Success implements DeedsInterpretationResult {

        private final Deeds deeds_;

        public Success(@NonNull Deeds deeds) { this.deeds_ = deeds; }

        @Override public Optional<Deeds> deeds() { return Optional.of(deeds_); }

    }
    record DeTranspileError(int lineNumber, String line, DeedsDeTranspilingError error) implements DeedsInterpretationResult {}
    record WrongNamesPace(String foundNameSpace) implements DeedsInterpretationResult {}
    record NonExistentRegion(long foundId) implements DeedsInterpretationResult {}
    record NotADeedsBook(ItemStack providedItem) implements DeedsInterpretationResult {}


    //FACTORY
    static Success success(Deeds deeds) {
        return new Success(deeds);
    }
    static DeTranspileError deTranspileError(int lineNumber, String line, DeedsDeTranspilingError error) {
        return new DeTranspileError(lineNumber, line, error);
    }
    static WrongNamesPace wrongNameSpace(String foundNameSpace) {
        return new WrongNamesPace(foundNameSpace);
    }
    static NonExistentRegion nonExistentRegion(long foundId) {
        return new NonExistentRegion(foundId);
    }
    static NotADeedsBook notADeedsBook(ItemStack providedItem) {
        return new NotADeedsBook(providedItem);
    }


    //DEFAULTS
    default Optional<Deeds> deeds() {
        return Optional.empty();
    }
}
