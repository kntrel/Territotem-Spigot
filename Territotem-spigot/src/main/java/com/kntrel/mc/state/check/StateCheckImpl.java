package com.kntrel.mc.state.check;

import com.kntrel.mc.state.StateMap;
import com.kntrel.util.Numbers;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

class StateCheckImpl {

    record Has(String key) implements StateCheck {
        @Override public boolean test(StateMap state) {
            return state != null && state.containsKey(this.key);
        }
    }

    record SizeEquals(int size) implements StateCheck {
        @Override public boolean test(StateMap state) {
            return state != null && state.size() == this.size;
        }
    }

    record SizeBiggerThan(int size) implements StateCheck {
        @Override public boolean test(StateMap state) {
            return state != null && state.size() > this.size;
        }
    }

    record SizeLessThan(int size) implements StateCheck {
        @Override public boolean test(StateMap state) {
            return state != null && state.size() < this.size;
        }
    }

    record Matches(StateMap other) implements StateCheck {
        @Override public boolean test(StateMap state) {
            if (state == null || this.other == null) { return false; }

            for (Map.Entry<String, StateMap.Value> entry : this.other.entrySet()) {
                if (!StateCheck.isEquals(entry.getKey(), entry.getValue()).test(state)) {
                    return false;
                }
            }
            return true;
        }
    }

    record ValuePredicate(String key, Predicate<StateMap.Value> predicate) implements StateCheck {
        @Override public boolean test(StateMap state) {
            if (state == null) { return false; }
            StateMap.Value value = state.get(this.key);
            return value != null && this.predicate.test(value);
        }
    }

    record RegexContains(String key, Pattern pattern) implements StateCheck {
        @Override public boolean test(StateMap state) {
            if (state == null) { return false; }
            StateMap.Value value = state.get(this.key);
            return value != null && this.pattern.matcher(value.getAsString()).find();
        }
    }

    record RegexMatches(String key, Pattern pattern) implements StateCheck {
        @Override public boolean test(StateMap state) {
            if (state == null) { return false; }
            StateMap.Value value = state.get(this.key);
            return value != null && this.pattern.matcher(value.getAsString()).matches();
        }
    }

    record KeyMatches(Pattern pattern) implements StateCheck {
        @Override public boolean test(StateMap state) {
            return state != null && state.keySet().stream().anyMatch(k -> this.pattern.matcher(k).find());
        }
    }

    static boolean valueEquals(StateMap.Value value, Object subject) {
        if (value == null) { return false; }

        if (subject instanceof StateMap.Value stateValue) {
            return valueEquals(value, stateValue.getAsString());
        }

        if (value.isNumeric() && subject instanceof Number number) {
            return Numbers.equalish(value.getAsNumber(), number);
        }

        return Objects.equals(value.getAsString(), String.valueOf(subject));
    }

    static boolean valueContains(StateMap.Value value, Object subject) {
        return value != null && value.getAsString().contains(String.valueOf(subject));
    }

    static boolean valueGreaterThan(StateMap.Value value, Object subject) {
        Number subjectNumber = toNumber(subject);
        return value != null && value.isNumeric() && subjectNumber != null
                && Numbers.greaterThan(value.getAsNumber(), subjectNumber);
    }

    static boolean valueLessThan(StateMap.Value value, Object subject) {
        Number subjectNumber = toNumber(subject);
        return value != null && value.isNumeric() && subjectNumber != null
                && Numbers.lessThan(value.getAsNumber(), subjectNumber);
    }

    static Number toNumber(Object subject) {
        if (subject instanceof Number number) { return number; }

        if (subject instanceof StateMap.Value value) {
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
