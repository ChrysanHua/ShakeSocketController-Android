package com.ssc.shakesocketcontroller.Models.events.command;

public class ShutdownCmd extends BaseCmdEvent {

    public ShutdownCmd(int timeout) {
        cmdType = CMD_SHUTDOWN;
        paramStr = String.valueOf(timeout);
    }
}
