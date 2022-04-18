package com.ssc.shakesocketcontroller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;
import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;
import com.ssc.shakesocketcontroller.Utils.DeviceUtil;
import com.ssc.shakesocketcontroller.Utils.StrUtil;

import org.greenrobot.eventbus.util.AsyncExecutor;
import org.junit.Test;

import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ComputerInfoTest {

    @Test
    public void getDeviceName() {
        //验证Gson结果
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        List<ComputerInfo> infoList = MyApplication.createTestCPInfoList(10, false);
        for (int i = 0; i < infoList.size(); i++) {
            if (i % 2 == 0) {
                infoList.get(i).nickName = null;
            } else if (i != 5) {
                infoList.get(i).nickName = "";
            }
        }
        //showList(infoList);

        String s = gson.toJson(infoList);
        System.err.println(s);

        Type collectionType = new TypeToken<ArrayList<ComputerInfo>>() {
        }.getType();
        List<ComputerInfo> resList = gson.fromJson(s, collectionType);
        System.err.println(infoList.equals(resList));
        System.err.println(resList == null);
        showList(resList);
    }

    @Test
    public void getAddress() {
        //ComputerInfo对象Equals比较
        ComputerInfo info1 = new ComputerInfo("UUID", "Computer", "nike", DeviceUtil.getLocalAddress());
        ComputerInfo info2 = new ComputerInfo("UUID", "Computer", "nike", DeviceUtil.getLocalAddress());
        List<ComputerInfo> infoList1 = new ArrayList<>();
        List<ComputerInfo> infoList2 = new ArrayList<>();
        infoList1.add(info1);
        infoList2.add(info2);

        info2.nickName = "NEW_NickName";
        System.out.println("ip: " + info1.getIP().equals(info2.getIP()));
        System.out.println("equals: " + info1.equals(info2));
        System.out.println("==: " + (info1 == info2));
        System.out.println("list contains: " + infoList1.contains(info2));
        System.out.println("list equals: " + infoList1.equals(infoList2));
        assertEquals(info1, info2);
    }

    @Test
    public void getNickName() {
        //输出检查Gson结果
        Gson gson = new GsonBuilder().serializeNulls().create();

        ComputerInfo info1 = new ComputerInfo("73006b15-b9a1-4fcd-b64b-05b20640dee5", "Computer", null);
        String s = gson.toJson(info1);
        System.out.println(s);

        //测试json字符串少一个字段，多一个无关字段，然后反序列化
        //s = "{\"ip\":\"192.168.1.2\",\"userName\":\"nike\",\"nickName\":\"nike\",\"isChecked\":false,\"testField\":\"abc\"}";
        ComputerInfo info2 = gson.fromJson(s, ComputerInfo.class);
        System.out.println(info2);
    }

    @Test
    public void getIP() {
    }

    @Test
    public void getMacByte() {
    }

    @Test
    public void getMacStr() {
    }

    @Test
    public void socketDuplexTest() {
        //UDP Socket双工测试
        final int bufSize = 4096, port = 19019, remotePort = 19191;
        final String remoteIP = "192.168.1.15", sendTxt = "TEST.TEST";
        AsyncExecutor executor = AsyncExecutor.create();
        int testCount = 1;

        while (--testCount >= 0) {
            try (final DatagramSocket socket = new DatagramSocket(port)) {
                //先异步执行接收操作（长期阻塞）
                executor.execute(() -> {
                    try {
                        while (true) {
                            DatagramPacket packet = new DatagramPacket(new byte[bufSize], bufSize);
                            System.out.println("异步接收开始...");
                            socket.receive(packet);
                            System.out.println("异步接收到[" + packet.getAddress().getHostAddress()
                                    + ":" + packet.getPort() + "] , offset:" + packet.getOffset()
                                    + ", pkLength:" + packet.getLength()
                                    + ", len:" + packet.getData().length + " -> "
                                    + StrUtil.byteToStr(packet.getData()));
                        }
                    } catch (SocketException e) {
                        System.out.println("异步接收结束！");
                    }
                });

                //等待一小会，确保异步接收已阻塞
                Thread.sleep(1000);
                //执行发送操作
                byte[] buf = StrUtil.strToByte(sendTxt);
                System.out.println("主线程执行发送->" + sendTxt);
                socket.send(new DatagramPacket(buf, buf.length,
                        InetAddress.getByName(remoteIP), remotePort));
                System.out.println("主线程发多一次->" + sendTxt);
                socket.send(new DatagramPacket(buf, buf.length,
                        InetAddress.getByName(remoteIP), remotePort));

                //同时重复执行接收操作 -> 可以同时receive
                DatagramPacket packet = new DatagramPacket(new byte[bufSize], bufSize);
                System.out.println("开始重复执行接收...");
                socket.receive(packet);
                String resStr = StrUtil.byteToStr(packet.getData());
                System.out.println("主线程接收到[" + packet.getAddress().getHostAddress()
                        + ":" + packet.getPort() + "] , strLen:"
                        + resStr.length() + ", strTrimLen:" + resStr.trim().length()
                        + " -> " + resStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.err.println("主线程执行结束 -> " + testCount);
            System.out.println();
        }
    }

    @Test
    public void isComplete() {
        //证实集合的参数传递的是引用
        int initCount = 5, resCount = initCount - 1;
        List<ComputerInfo> infoList = MyApplication.createTestCPInfoList(initCount, true);
        ModifyListTest(infoList);
        showList(infoList);
        assertEquals(resCount, infoList.size());
    }

    private void ModifyListTest(Collection<ComputerInfo> computerInfos) {
        final List<ComputerInfo> infoList = (List<ComputerInfo>) computerInfos;
        infoList.remove(1);
    }

    private void showList(List<ComputerInfo> infoList) {
        System.err.println("count: " + infoList.size());
        for (ComputerInfo c : infoList) {
            System.out.println(c);
        }
        System.out.println();
    }

}