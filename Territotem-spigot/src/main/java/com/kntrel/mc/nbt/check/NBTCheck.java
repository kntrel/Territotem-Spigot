package com.kntrel.mc.nbt.check;

import com.kntrel.mc.nbt.NBTTag;
import org.jspecify.annotations.NonNull;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface NBTCheck extends Predicate<NBTTag> {

    //FACTORY
    static NBTCheck not(NBTCheck negated) { return t -> !negated.test(t); }

    static NBTCheck isEquals(Object subject) { return new NBTCheckImpl.Equals(subject); }

    static NBTCheck isNotEquals(Object subject) { return NBTCheck.isEquals(subject).negate(); }

    static NBTCheck isBiggerThan(Number number) { return new NBTCheckImpl.BiggerThan(number); }

    static NBTCheck isLessThan(Number number) { return new NBTCheckImpl.LessThan(number); }

    static NBTCheck isBiggerThanEquals(Number number) { return NBTCheck.isLessThan(number).negate(); }

    static NBTCheck isLessThanEquals(Number number) { return NBTCheck.isBiggerThan(number).negate(); }

    static NBTCheck contains(Object subject) { return new NBTCheckImpl.Contains(subject); }

    static NBTCheck matches(String patternString) { return new NBTCheckImpl.Regex(patternString); }

    static NBTCheck matches(Pattern pattern) { return new NBTCheckImpl.Regex(pattern); }

    static NBTCheck sizeIs(int size) { return new NBTCheckImpl.SizeEquals(size); }

    static NBTCheck sizeIsBiggerThan(int size) { return new NBTCheckImpl.SizeBiggerThan(size); }

    static NBTCheck sizeIsLessThan(int size) { return new NBTCheckImpl.SizeLessThan(size); }

    static NBTCheck sizeIsBiggerThanEquals(int size) { return NBTCheck.sizeIsLessThan(size).negate(); }

    static NBTCheck sizeIsLessThanEquals(int size) { return NBTCheck.sizeIsBiggerThan(size).negate(); }

    static NBTCheck regexMatch(Pattern pattern) { return new NBTCheckImpl.Regex(pattern); }

    static NBTCheck regexMatch(String pattern) { return new NBTCheckImpl.Regex(pattern); }

    static NBTCheck exists(Object... path) { return new NBTCheckImpl.Exists(path); }

    static NBTCheck exists(String path) { return new NBTCheckImpl.Exists(path); }

    static NBTCheck checkAt(NBTCheck check, String path) { return new NBTCheckImpl.CheckAt(check, path); }

    static NBTCheck checkAt(NBTCheck check, Object... path) { return new NBTCheckImpl.CheckAt(check, path); }

    static NBTCheck matches(NBTTag reference) { return new NBTCheckImpl.Matches(reference); }

    static NBTCheck all(Collection<NBTCheck> checks) {
        return t -> checks.stream().allMatch(c -> c.test(t));
    }

    static NBTCheck all(NBTCheck... checks) {
        return NBTCheck.all(List.of(checks));
    }

    static NBTCheck any(Collection<NBTCheck> checks) {
        return t -> checks.stream().anyMatch(c -> c.test(t));
    }

    static NBTCheck any(NBTCheck... checks) {
        return NBTCheck.any(List.of(checks));
    }

    static NBTCheck in(Object... set) {
        return t -> {
            for (Object o : set) {
                if (NBTCheck.isEquals(o).test(t)) { return true; }
            }
            return false;
        };
    }


    //OVERWRITES
    @Override default @NonNull NBTCheck and(@NonNull Predicate<? super NBTTag> other) {
        NBTCheck otherCheck = (other instanceof NBTCheck chk) ? chk : other::test;
        return NBTCheck.all(this, otherCheck);
    }
    @Override default @NonNull NBTCheck negate() {
        return NBTCheck.not(this);
    }
    @Override default @NonNull NBTCheck or(@NonNull Predicate<? super NBTTag> other) {
        NBTCheck otherCheck = (other instanceof NBTCheck chk) ? chk : other::test;
        return NBTCheck.any(this, otherCheck);
    }
}
