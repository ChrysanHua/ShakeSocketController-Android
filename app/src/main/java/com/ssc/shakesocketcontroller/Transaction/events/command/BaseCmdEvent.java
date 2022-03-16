package com.ssc.shakesocketcontroller.Transaction.events.command;

public abstract class BaseCmdEvent {

    public static final String CMD_SHUTDOWN = "shutdown";
    public static final String CMD_SCREENLOCK = "screenlock";
    public static final String CMD_PPT = "ppt";
    public static final String CMD_MUSIC = "music";
    public static final String CMD_VOLUME = "volume";

    String cmdType = null;
    String paramStr = null;

    transient int feedbackTimeout = 3000;

    public String getCmdType() {
        return cmdType;
    }

    public String getParamStr() {
        return paramStr;
    }

    public int getFeedbackTimeout() {
        return feedbackTimeout;
    }

}
