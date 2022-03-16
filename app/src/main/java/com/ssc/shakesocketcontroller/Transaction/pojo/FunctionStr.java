package com.ssc.shakesocketcontroller.Transaction.pojo;

public class FunctionStr {

    private String name;
    private String info;
    private Class<?> cls;

    public String getName() {
        return name;
    }

    public String getInfo() {
        return info;
    }

    public Class<?> getCls() {
        return cls;
    }

    public FunctionStr(String name, String info, Class<?> activityCls) {
        this.name = name;
        this.info = info;
        this.cls = activityCls;
    }
}
