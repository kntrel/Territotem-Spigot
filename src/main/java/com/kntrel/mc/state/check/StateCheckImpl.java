package com.kntrel.mc.state.check;

import com.kntrel.mc.state.State;
import com.kntrel.util.Numbers;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

class StateCheckImpl {

    record Has(String key) implements StateCheck {
        @Override public boolean test(State state) {
            return state != null && state.containsKey(this.key);
        }
    }

    record SizeEquals(int size) implements StateCheck {
        @Override public boolean test(State state) {
            return state != null && state.size() == this.size;
        }
    }

    record SizeBiggerThan(int size) implements StateCheck {
        @Override public boolean test(State state) {
            return state != null && state.size() > this.size;
        }
    }

    record SizeLessThan(int size) implements StateCheck {
        @Override public boolean test(State state) {
            return state != null && state.size() < this.size;
        }
    }

    record Matches(State other) implements StateCheck {
        @Override public boolean test(State state) {
            if (state == null || this.other == null) { return false; }

            for (Map.Entry<String, State.Value> entry : this.other.entrySet()) {
                State.Value value = state.get(entry.getKey());
                if (value == null || !StateCheck.valueEquals(value, entry.getValue())) {
                    return false;
                }
            }
            return true;
        }
    }

    record ValuePredicate(String key, Predicate<State.Value> predicate) implements StateCheck {
        @Override public boolean test(State state) {
            if (state == null) { return false; }
            State.Value value = state.get(this.key);
            return value != null && this.predicate.test(value);
        }
    }

    record RegexContains(String key, Pattern pattern) implements StateCheck {
        @Override public boolean test(State state) {
            if (state == null) { return false; }
            State.Value value = state.get(this.key);
            return value != null && this.pattern.matcher(value.getAsString()).find();
        }
    }

    record RegexMatches(String key, Pattern pattern) implements StateCheck {
        @Override public boolean test(State state) {
            if (state == null) { return false; }
            State.Value value = state.get(this.key);
            return value != null && this.pattern.matcher(value.getAsString()).matches();
        }
    }

    record KeyMatches(Pattern pattern) implements StateCheck {
        @Override public boolean test(State state) {
            return state != null && state.keySet().stream().anyMatch(k -> this.pattern.matcher(k).find());
        }
    }

    static boolean valueEquals(State.Value value, Object subject) {
        if (value == null) { return false; }

        if (subject instanceof State.Value stateValue) {
            return valueEquals(value, stateValue.getAsString());
        }

        if (value.isNumeric() && subject instanceof Number number) {
            return Numbers.equalish(value.getAsNumber(), number);
        }

        return Objects.equals(value.getAsString(), String.valueOf(subject));
    }

    static boolean valueContains(State.Value value, Object subject) {
        return value != null && value.getAsString().contains(String.valueOf(subject));
    }

    static boolean valueGreaterThan(State.Value value, Object subject) {
        Number subjectNumber = toNumber(subject);
        return value != null && value.isNumeric() && subjectNumber != null
                && Numbers.greaterThan(value.getAsNumber(), subjectNumber);
    }

    static boolean valueLessThan(State.Value value, Object subject) {
        Number subjectNumber = toNumber(subject);
        return value != null && value.isNumeric() && subjectNumber != null
                && Numbers.lessThan(value.getAsNumber(), subjectNumber);
    }

    static Number toNumber(Object subject) {
        if (subject instanceof Number number) { return number; }

        if (subject instanceof State.Value value) {
            return value.isNumeric() ? value.getAsNumber() : null;
        }

        if (subject instanceof String string) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}
