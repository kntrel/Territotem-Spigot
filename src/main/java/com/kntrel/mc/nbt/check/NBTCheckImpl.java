package com.kntrel.mc.nbt.check;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.nbt.NBTList;
import com.kntrel.mc.nbt.NBTPrimitive;
import com.kntrel.mc.nbt.NBTTag;
import com.kntrel.util.Numbers;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

class NBTCheckImpl {

    record Equals(Object subject) implements NBTCheck {

        @Override public boolean test(NBTTag tag) {
            if (tag == null) { return false; }
            if (this.subject == tag) { return true; }

            if (!(tag instanceof NBTPrimitive primitive)) {
                return Objects.equals(tag.get(), this.subject);
            }

            if (primitive.isNumeric()) {
                if (!(this.subject instanceof Number num)) return false;
                return Numbers.equalish(primitive.getAsNumber(), num);
            }

            if (primitive.isNumericArray()) {
                Number[] arr = toNumericArray(this.subject);
                if (arr == null) { return false; }

                Number[] actual = primitive.getAsNumberArray();
                if (actual.length != arr.length) { return false; }

                for (int i = 0; i < actual.length; i++) {
                    if (!Numbers.equalish(actual[i], arr[i])) { return false; }
                }
                return true;
            }

            return Objects.equals(tag.get(), this.subject);
        }
    }

    record BiggerThan(Number number) implements NBTCheck {

        @Override public boolean test(NBTTag tag) {
            if (tag == null) { return false; }
            if (!(tag instanceof NBTPrimitive primitive)) { return false; }
            if (!primitive.isNumeric()) { return false; }

            return Numbers.compare(primitive.getAsNumber(), this.number) > 0;
        }
    }

    record LessThan(Number number) implements NBTCheck {

        @Override public boolean test(NBTTag tag) {
            if (tag == null) { return false; }
            if (!(tag instanceof NBTPrimitive primitive)) { return false; }
            if (!primitive.isNumeric()) { return false; }

            return Numbers.compare(primitive.getAsNumber(), this.number) < 0;
        }
    }

    record Contains(Object subject) implements NBTCheck {

        @Override public boolean test(NBTTag tag) {
            if (tag == null) { return false; }

            if (tag instanceof NBTPrimitive primitive) {
                // Numeric array: check if any element equals subject
                if (primitive.isNumericArray()) {
                    if (!(this.subject instanceof Number num)) return false;
                    Number[] arr = primitive.getAsNumberArray();
                    for (Number element : arr) {
                        if (Numbers.equalish(element, num)) { return true; }
                    }
                    return false;
                }

                // Numeric primitive: normal equals check
                if (primitive.isNumeric()) {
                    if (!(this.subject instanceof Number num)) return false;
                    return Numbers.equalish(primitive.getAsNumber(), num);
                }

                // String primitive: string contains check
                if (primitive.isString()) {
                    String value = primitive.getAsString();
                    String subject = this.subject.toString();
                    return value.contains(subject);
                }

                return false;
            }

            // NBTList: check equality on any sub NBTTag
            if (tag instanceof NBTList list) {
                for (NBTTag element : list) {
                    if (NBTCheck.isEquals(this.subject).test(element)) { return true; }
                }
                return false;
            }

            // NBTCompound: check if map contains the key specified by subject
            if (tag instanceof NBTCompound compound) {
                return compound.containsKey(this.subject.toString());
            }

            return false;
        }
    }

    record SizeEquals(int size) implements NBTCheck {

        @Override public boolean test(NBTTag tag) {
            if (tag == null) { return false; }
            return getSize(tag) == this.size;
        }
    }

    record SizeLessThan(int size) implements NBTCheck {

        @Override public boolean test(NBTTag tag) {
            if (tag == null) { return false; }
            return getSize(tag) < this.size;
        }
    }

    record SizeBiggerThan(int size) implements NBTCheck {

        @Override public boolean test(NBTTag tag) {
            if (tag == null) { return false; }
            return getSize(tag) > this.size;
        }
    }

    private static int getSize(NBTTag tag) {
        if (tag instanceof NBTList list) {
            return list.size();
        }
        if (tag instanceof NBTCompound compound) {
            return compound.size();
        }
        return 1;
    }

    record Regex(Pattern pattern) implements NBTCheck {

        public Regex(String patternString) {
            this(Pattern.compile(patternString));
        }

        @Override public boolean test(NBTTag tag) {
            if (tag == null) { return false; }
            
            String tagString = tag.toString();
            return this.pattern.matcher(tagString).find();
        }
    }

    record Exists(@Nullable String stringPath, @Nullable Object[] arrayPath) implements NBTCheck {

        Exists {
            if (stringPath == null && arrayPath == null) {
                throw new IllegalArgumentException("Either a string or an array version of a path must be supplied");
            }
        }
        Exists(String path) { this(path, null); }
        Exists(Object... path) { this(null, path); }

        @Override public boolean test(NBTTag tag) {
            if (this.arrayPath != null && this.arrayPath.length < 1) { return true; }
            if (this.stringPath != null && this.stringPath.isEmpty()) { return true; }

            Optional<NBTTag> optional = Optional.empty();
            if (tag instanceof NBTList list) {
                optional = (this.stringPath == null)
                        ? list.getAt(this.arrayPath)
                        : list.getAt(this.stringPath);
            } else if (tag instanceof NBTCompound compound) {
                optional = (this.stringPath == null)
                        ? compound.getAt(this.arrayPath)
                        : compound.getAt(this.stringPath);
            }
            return optional.isPresent();
        }
    }

    record CheckAt(NBTCheck check, @Nullable String stringPath, @Nullable Object[] arrayPath) implements NBTCheck {

        CheckAt {
            if (stringPath == null && arrayPath == null) {
                throw new IllegalArgumentException("Either a string or an array version of a path must be supplied");
            }
        }
        CheckAt(NBTCheck check, String path) { this(check, path, null); }
        CheckAt(NBTCheck check, Object... path) { this(check, null, path); }

        @Override public boolean test(NBTTag tag) {
            if (this.arrayPath != null && this.arrayPath.length < 1) {
                return check.test(tag);
            }
            if (this.stringPath != null && this.stringPath.isEmpty()) {
                return check.test(tag);
            }

            Optional<NBTTag> optional = Optional.empty();
            if (tag instanceof NBTList list) {
                optional = (this.stringPath == null)
                        ? list.getAt(this.arrayPath)
                        : list.getAt(this.stringPath);
            } else if (tag instanceof NBTCompound compound) {
                optional = (this.stringPath == null)
                        ? compound.getAt(this.arrayPath)
                        : compound.getAt(this.stringPath);
            }
            return optional.map(this.check::test).orElse(false);
        }
    }

    record Matches(NBTTag reference) implements NBTCheck {

        @Override public boolean test(NBTTag tag) {
            return this.testInner(this.reference, tag);
        }

        private static boolean testInner(NBTTag ref, NBTTag tag) {
            if (tag == null) { return false; }
            if (ref == null) { return false; }

            // Both primitives: exact equality
            if (ref instanceof NBTPrimitive && tag instanceof NBTPrimitive) {
                return NBTCheck.isEquals(ref.get()).test(tag);
            }

            // Both lists: exact equality
            if (ref instanceof NBTList refList && tag instanceof NBTList tagList) {
                if (refList.size() != tagList.size()) { return false; }     // If both lists aren't the same size, they're not equal

                Iterator<NBTTag> ri = refList.iterator(), ti = tagList.iterator();
                while (ri.hasNext()) {
                    NBTTag r = ri.next(), t = ti.next();
                    if (!testInner(r, t)) { return false; }
                }
                return true;
            }

            // Both compounds: reference ∩ tag == reference
            if (ref instanceof NBTCompound refCompound && tag instanceof NBTCompound tagCompound) {
                // Every key in reference must exist in tag and recursively match
                for (Map.Entry<String, NBTTag> entry : refCompound.entrySet()) {
                    NBTTag  r = entry.getValue(),
                            t = tagCompound.get(entry.getKey());

                    if (t == null) { return false; }

                    // Recursively check if tagValue matches refValue
                    if (!testInner(r, t)) {
                        return false;
                    }
                }
                return true;
            }

            // Different types: cannot match
            return false;
        }
    }


    //HELPERS
    private static Number[] toNumericArray(Object object) {
        if (object instanceof Number[] arr) {
            return arr;
        }
        if (object instanceof byte[] byteArr) {
            Number[] arr = new Number[byteArr.length];
            for (int i = 0; i < byteArr.length; i++) {
                arr[i] = byteArr[i];
            }
            return arr;
        }
        if (object instanceof int[] intArr) {
            Number[] arr = new Number[intArr.length];
            for (int i = 0; i < intArr.length; i++) {
                arr[i] = intArr[i];
            }
            return arr;
        }
        if (object instanceof long[] longArr) {
            Number[] arr = new Number[longArr.length];
            for (int i = 0; i < longArr.length; i++) {
                arr[i] = longArr[i];
            }
            return arr;
        }
        return null;
    }
}
