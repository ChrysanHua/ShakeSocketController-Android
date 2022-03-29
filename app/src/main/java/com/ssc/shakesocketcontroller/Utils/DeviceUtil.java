package com.ssc.shakesocketcontroller.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;

public final class DeviceUtil {
    private static final String TAG = "DeviceUtil";

    public static byte[] getMacAddress(InetAddress address) {
        try {
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
            if (networkInterface != null) {
                return networkInterface.getHardwareAddress();
            }
        } catch (SocketException e) {
            Log.e(TAG, "getMacAddress: failed", e);
        }
        return null;
    }

    public static String getMacAddressStr(InetAddress address) {
        return StrUtil.macByteToStr(getMacAddress(address));
    }

    public static InetAddress getLocalAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {
                NetworkInterface networkInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses();
                     enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "getLocalAddress: getNetworkInterfaces() failed", e);
        }
        return null;
    }

    public static String getLocalIP() {
        InetAddress address = getLocalAddress();
        return address == null ? "" : address.getHostAddress();
    }

    public static String getDeviceName() {
        String deviceName = Settings.System.getString(
                MyApplication.appContext.getContentResolver(), "device_name");
        if (StrUtil.isNullOrEmpty(deviceName)) {
            deviceName = Settings.Secure.getString(
                    MyApplication.appContext.getContentResolver(), "bluetooth_name");
        }
        if (StrUtil.isNullOrEmpty(deviceName)) {
            deviceName = Build.MODEL;
        }
        return deviceName;
    }

    public static boolean isNetworkConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) MyApplication.appContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr != null ? connMgr.getActiveNetworkInfo() : null;
        return activeInfo != null && activeInfo.isConnected();
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

}
