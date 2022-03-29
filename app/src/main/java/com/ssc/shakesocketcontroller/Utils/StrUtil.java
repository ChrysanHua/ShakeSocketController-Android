package com.ssc.shakesocketcontroller.Utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class StrUtil {
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static String byteToStr(byte[] strByte) {
        return new String(strByte, DEFAULT_CHARSET);
    }

    public static byte[] strToByte(String str) {
        return str.getBytes(DEFAULT_CHARSET);
    }

    public static String macByteToStr(byte[] macByte) {
        StringBuilder sb = new StringBuilder();
        if (macByte != null) {
            for (int i = 0; i < macByte.length; i++) {
                if (i != 0) {
                    sb.append("-");
                }
                //byte to hexStr
                int temp = macByte[i] & 0xff;
                String str = Integer.toHexString(temp);
                if (str.length() == 1) {
                    sb.append("0");
                }
                sb.append(str);
            }
        }
        return sb.toString().toUpperCase();
    }

    public static boolean isNullOrEmpty(String str) {
        return (str == null || str.isEmpty());
    }

}
