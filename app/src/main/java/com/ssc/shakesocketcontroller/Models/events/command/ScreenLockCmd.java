package com.ssc.shakesocketcontroller.Models.events.command;

public class ScreenLockCmd extends BaseCmdEvent {

    public ScreenLockCmd() {
        cmdType = CMD_SCREENLOCK;
        paramStr = CMD_SCREENLOCK;
    }
}
