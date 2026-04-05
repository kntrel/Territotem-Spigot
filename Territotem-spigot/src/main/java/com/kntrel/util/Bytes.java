package com.kntrel.util;

public final class Bytes {

    //NO INSTANCING
    private Bytes() {}


    //UTIL METHODS
    public static short toShort(byte[] raw, int offset) { return (short) toNumber(raw, offset, 2); }
    public static int toInt(byte[] raw, int offset) { return (int) toNumber(raw, offset, 4); }
    public static long toLong(byte[] raw, int offset) { return toNumber(raw, offset, 8); }
    public static void fromNumber(long val, byte[] target, int offset, int bytes) {
        for (int i = bytes - 1; i >= 0; i--) {
            target[offset + i] = (byte) val;
            val >>= 8;
        }
    }
    public static void fromShort(short val, byte[] target, int offset) { fromNumber(val, target, offset, 2); }
    public static void fromInt(int val, byte[] target, int offset) { fromNumber(val, target, offset, 4); }
    public static void fromLong(long val, byte[] target, int offset) { fromNumber(val, target, offset, 8); }


    //HELPERS
    private static long toNumber(byte[] raw, int offset, int bytes) {
        int len = raw.length;
        if (offset > len) {
            throw new IndexOutOfBoundsException("offset " + offset + " is out of bounds of array of length " + len);
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("offset of " + offset + " is invalid. Must be a positive integer");
        }
        if (offset == len) { return 0; }

        int pos = offset + bytes;
        if (pos > len) { pos = len; }

        long out = 0;
        int shift = (offset + bytes - pos) * 8;
        while (--pos >= offset) {
            out |= (long)(raw[pos] & 0xFF) << shift;
            shift += 8;
        }
        return out;
    }
}
