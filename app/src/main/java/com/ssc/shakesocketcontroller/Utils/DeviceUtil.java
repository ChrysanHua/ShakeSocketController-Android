package com.ssc.shakesocketcontroller.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.NetworkOnMainThreadException;
import android.provider.Settings;
import android.util.Log;

import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;

import java.net.DatagramSocket;
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
        try (final DatagramSocket socket = new DatagramSocket()) {
            //because it is udp socket, it will perform an invalid connect operation
            socket.connect(InetAddress.getByName("8.8.8.8"), 12000);
            //but we can still get the IP from it
            return socket.getLocalAddress();
        } catch (NetworkOnMainThreadException e) {
            // TODO: 2022/3/29 仅用于当前开发阶段（因为上述获取IP的新方法不能在主线程中执行），后续应当完成废弃该方法！
            return getLocalAddressSpare();
        } catch (Exception e) {
            Log.e(TAG, "getLocalAddress: failed", e);
        }
        return null;
    }

    //Deprecated! Because this method is unreliable.
    private static InetAddress getLocalAddressSpare() {
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

    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

}
