package com.ssc.shakesocketcontroller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;
import com.ssc.shakesocketcontroller.Models.events.command.BaseCmdEvent;
import com.ssc.shakesocketcontroller.Models.events.command.ShutdownCmd;
import com.ssc.shakesocketcontroller.Utils.DeviceUtil;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class ComputerInfoTest {

    @Test
    public void getDeviceName() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        HashMap<String, ComputerInfo> map = new HashMap<>();

        ComputerInfo info = new ComputerInfo(DeviceUtil.getLocalAddress(),
                "Computer", "nike");
        map.put("1", info);
        Collection<ComputerInfo> infos = map.values();
        info = map.get("1");
        System.out.println(infos.contains(info));
        String s = gson.toJson(info);
        System.out.println(s);
        ComputerInfo ok = gson.fromJson(s, ComputerInfo.class);
        //System.out.println(ok.getAddress() + " " + ok.getDeviceName() + " " + ok.getNickName());
        //System.out.println(String.format("%s(%s)", info.getDeviceName(), info.getIP()));
        System.out.println(info.equals(ok));
        System.out.println(map.containsValue(ok));
    }

    @Test
    public void getNickName() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        BaseCmdEvent event = new ShutdownCmd(1000);
        String s = gson.toJson(event);
        System.out.println(s);

    }

    @Test
    public void getAddress() {
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
    public void isComplete() {
        //证实集合的参数传递的是引用
        int initCount = 5, resCount = initCount - 1;
        List<ComputerInfo> infoList = BuildTestCPInfos(initCount);
        ModifyListTest(infoList);
        showList(infoList);
        assertEquals(resCount, infoList.size());
    }

    private void ModifyListTest(Collection<ComputerInfo> computerInfos) {
        final List<ComputerInfo> infoList = (List<ComputerInfo>) computerInfos;
        infoList.remove(1);
    }

    private void showList(List<ComputerInfo> infoList) {
        System.out.println("count: " + infoList.size());
        for (ComputerInfo c : infoList) {
            //System.out.print(c.getDeviceName() + ", ");
        }
        System.out.println();
    }

    private List<ComputerInfo> BuildTestCPInfos(int count) {
        List<ComputerInfo> infos = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            infos.add(new ComputerInfo(DeviceUtil.getLocalAddress(),
                    "CP" + i, "NickName" + i));
        }
        return infos;
    }
}