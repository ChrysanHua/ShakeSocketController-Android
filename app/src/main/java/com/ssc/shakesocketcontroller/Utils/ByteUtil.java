package com.ssc.shakesocketcontroller.Utils;

public final class ByteUtil {

    public static byte[] subByte(byte[] bytes, int offset, int length) {
        byte[] partByte = new byte[length];
        System.arraycopy(bytes, offset, partByte, 0, length);
        return partByte;
    }

    public static byte[] concatByte(byte[] front, byte[] back) {
        byte[] allByte = new byte[front.length + back.length];
        System.arraycopy(front, 0, allByte, 0, front.length);
        System.arraycopy(back, 0, allByte, front.length, back.length);
        return allByte;
    }

    public static byte[] fixLen(byte[] bytes, int length) {
        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0, Math.min(bytes.length, length));
        return result;
    }

}
