package com.kntrel.mc.territotem.structure.blueprint;

import com.kntrel.mc.territotem.structure.Structure;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BlueprintRegistrationBuilder {
    private BlueprintRegistrationBuilder() {}


    public static BlueprintRegistrationBuilder.Completer forBlueprint(Blueprint blueprint) {
        return new Impl<>().blueprint(blueprint);
    }
    public static BlueprintRegistrationBuilder.Completer forBlueprint(Blueprint blueprint, Consumer<BlueprintRegistration> callBack) {
        return new Impl<>(callBack).blueprint(blueprint);
    }


    public interface BlueprintSelector {
        Completer blueprint(Blueprint blueprint);
    }

    public interface Completer extends EventSelector {
        Destructor onCompletion(BiConsumer<Structure, Entity> action);
    }

    public interface Destructor extends EventSelector {
        EventSelector onDestruction(BiConsumer<Structure, Entity> action);
    }

    public interface EventSelector {
        BlueprintRegistration doNotTrack();
        <E extends Event> EventValidator<E> on(Class<E> eventClass);
    }

    public interface EventValidator<E extends Event> extends Tracker<E> {
        Tracker<E> when(Predicate<E> validator);
    }

    public interface Tracker<E extends Event> {
        BlueprintRegistration track(Function<E, TrackingInfo> locator);
    }

    private static class Impl<E extends Event>
    implements BlueprintSelector, Completer, Destructor, EventSelector, EventValidator<E>, Tracker<E> {

        //FIELDS
        private final Consumer<BlueprintRegistration> onBuild_;
        private Blueprint blueprint_;
        private Class<E> eventClass_;
        private Predicate<Event> validator_;
        private Function<Event, TrackingInfo> tracker_;
        private BiConsumer<Structure, Entity> completionAction_, destructionAction_;


        //CONSTRUCTOR
        Impl(Consumer<BlueprintRegistration> onBuild) {
            this.onBuild_ = onBuild;
            this.blueprint_ = null;
            this.eventClass_ = null;
            this.validator_ = e -> true;
            this.tracker_ = e -> null;
            this.completionAction_ = (s, e) -> {};
            this.destructionAction_ = (s, e) -> {};
        }
        Impl() { this(null); }


        //IMPLEMENTATION
        @Override public Completer blueprint(Blueprint blueprint) {
            this.blueprint_ = blueprint;
            return this;
        }
        @Override public Destructor onCompletion(BiConsumer<Structure, Entity> action) {
            this.completionAction_ = action;
            return this;
        }
        @Override public EventSelector onDestruction(BiConsumer<Structure, Entity> action) {
            this.destructionAction_ = action;
            return this;
        }
        @Override @SuppressWarnings("unchecked")
        public <O extends Event> EventValidator<O> on(Class<O> eventClass) {
            Impl<O> turned = (Impl<O>) this;
            turned.eventClass_ = eventClass;
            return turned;
        }
        @Override public Tracker<E> when(Predicate<E> validator) {
            this.validator_ = e -> {
                if (!this.eventClass_.isInstance(e)) { return false; }
                return validator.test(this.eventClass_.cast(e));
            };
            return this;
        }
        @Override public BlueprintRegistration doNotTrack() {
            return this.build();
        }
        @Override public BlueprintRegistration track(Function<E, TrackingInfo> locator) {
            this.tracker_ = e -> {
                if (!this.eventClass_.isInstance(e)) { return null; }
                return locator.apply(this.eventClass_.cast(e));
            };
            return this.build();
        }


        //HELPERS
        protected BlueprintRegistration build() {
            if (this.blueprint_ == null) {
                throw new IllegalStateException("No blueprint object defined. Can't build registration.");
            }

            if (this.eventClass_ == null) {
                this.validator_ = e -> false;
            }

            BlueprintRegistration registration = new BlueprintRegistrationImpl(
                    this.blueprint_,
                    this.eventClass_,
                    this.validator_,
                    this.tracker_,
                    this.completionAction_,
                    this.destructionAction_
            );

            if (this.onBuild_ != null) { this.onBuild_.accept(registration); }
            return registration;
        }
    }

    private record BlueprintRegistrationImpl(
            Blueprint blueprint,
            Class<? extends Event> eventClass,
            Predicate<Event> validator,
            Function<Event, TrackingInfo> locator,
            BiConsumer<Structure, Entity> completionAction,
            BiConsumer<Structure, Entity> destructionAction
    ) implements BlueprintRegistration {

        @Override public boolean appliesTo(Event event) {
            return this.validator.test(event);
        }
        @Override public TrackingInfo track(Event event) {
            return this.locator.apply(event);
        }
        @Override public void onCompletion(Structure structure, Entity completer) { this.completionAction.accept(structure, completer); }
        @Override public void onDestruction(Structure structure, Entity destructor) { this.destructionAction.accept(structure, destructor); }
    }
}
