package com.kntrel.mc.state.check;

import com.kntrel.mc.state.State;
import org.jspecify.annotations.NonNull;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface StateCheck extends Predicate<State> {

    // FACTORY: state-level
    static StateCheck has(String key) { return new StateCheckImpl.Has(key); }

    static StateCheck sizeIs(int size) { return new StateCheckImpl.SizeEquals(size); }

    static StateCheck sizeIsBiggerThan(int size) { return new StateCheckImpl.SizeBiggerThan(size); }

    static StateCheck sizeIsLessThan(int size) { return new StateCheckImpl.SizeLessThan(size); }

    static StateCheck sizeIsBiggerEquals(int size) { return StateCheck.sizeIsLessThan(size).negate(); }

    static StateCheck sizeIsLessEquals(int size) { return StateCheck.sizeIsBiggerThan(size).negate(); }

    static StateCheck not(StateCheck negated) { return state -> !negated.test(state); }

    static StateCheck matches(State other) { return new StateCheckImpl.Matches(other); }

    static StateCheck all() { return state -> true; }

    static StateCheck all(Collection<StateCheck> checks) {
        return state -> checks.stream().allMatch(c -> c.test(state));
    }

    static StateCheck all(StateCheck... checks) { return StateCheck.all(List.of(checks)); }

    static StateCheck any() { return state -> false; }

    static StateCheck any(Collection<StateCheck> checks) {
        return state -> checks.stream().anyMatch(c -> c.test(state));
    }

    static StateCheck any(StateCheck... checks) { return StateCheck.any(List.of(checks)); }


    // FACTORY: value-level
    static StateCheck isEquals(String key, Object subject) {
        return StateCheck.check(key, value -> StateCheckImpl.valueEquals(value, subject));
    }

    static StateCheck isNotEquals(String key, Object subject) {
        return StateCheck.isEquals(key, subject).negate();
    }

    static StateCheck contains(String key, String subject) {
        return StateCheck.check(key, value -> StateCheckImpl.valueContains(value, subject));
    }

    static StateCheck isBiggerThan(String key, Object subject) {
        return StateCheck.check(key, value -> StateCheckImpl.valueGreaterThan(value, subject));
    }

    static StateCheck isLessThan(String key, Object subject) {
        return StateCheck.check(key, value -> StateCheckImpl.valueLessThan(value, subject));
    }

    static StateCheck isBiggerEquals(String key, Object subject) {
        return StateCheck.isLessThan(key, subject).negate();
    }

    static StateCheck isLessEquals(String key, Object subject) {
        return StateCheck.isBiggerThan(key, subject).negate();
    }

    static StateCheck in(String key, Object... options) {
        return StateCheck.in(key, List.of(options));
    }

    static StateCheck in(String key, Collection<?> options) {
        return StateCheck.check(key, value -> options.stream().anyMatch(option -> StateCheckImpl.valueEquals(value, option)));
    }

    static StateCheck all(String key, Collection<Predicate<State.Value>> checks) {
        return StateCheck.check(key, value -> checks.stream().allMatch(check -> check.test(value)));
    }

    @SafeVarargs
    static StateCheck all(String key, Predicate<State.Value>... checks) {
        return StateCheck.all(key, List.of(checks));
    }

    static StateCheck any(String key, Collection<Predicate<State.Value>> checks) {
        return StateCheck.check(key, value -> checks.stream().anyMatch(check -> check.test(value)));
    }

    @SafeVarargs
    static StateCheck any(String key, Predicate<State.Value>... checks) {
        return StateCheck.any(key, List.of(checks));
    }

    static StateCheck regexMatches(String key, String reg) {
        return StateCheck.regexMatches(key, Pattern.compile(reg));
    }

    static StateCheck regexMatches(String key, Pattern pattern) {
        return new StateCheckImpl.RegexMatches(key, pattern);
    }

    static StateCheck startsWith(String key, String subject) {
        return StateCheck.check(key, value -> value.getAsString().startsWith(subject));
    }

    static StateCheck endsWith(String key, String subject) {
        return StateCheck.check(key, value -> value.getAsString().endsWith(subject));
    }

    static StateCheck regexContains(String key, String reg) {
        return StateCheck.regexContains(key, Pattern.compile(reg));
    }

    static StateCheck regexContains(String key, Pattern pattern) {
        return new StateCheckImpl.RegexContains(key, pattern);
    }

    static StateCheck keyMatches(String reg) {
        return StateCheck.keyMatches(Pattern.compile(reg));
    }

    static StateCheck keyMatches(Pattern pattern) {
        return new StateCheckImpl.KeyMatches(pattern);
    }

    static StateCheck missing(String key) { return StateCheck.has(key).negate(); }

    static StateCheck check(String key, Predicate<State.Value> check) {
        return new StateCheckImpl.ValuePredicate(key, check);
    }


    // OVERWRITES
    @Override default @NonNull StateCheck and(@NonNull Predicate<? super State> other) {
        StateCheck otherCheck = (other instanceof StateCheck chk) ? chk : other::test;
        return StateCheck.all(this, otherCheck);
    }

    @Override default @NonNull StateCheck negate() {
        return StateCheck.not(this);
    }

    @Override default @NonNull StateCheck or(@NonNull Predicate<? super State> other) {
        StateCheck otherCheck = (other instanceof StateCheck chk) ? chk : other::test;
        return StateCheck.any(this, otherCheck);
    }
}
