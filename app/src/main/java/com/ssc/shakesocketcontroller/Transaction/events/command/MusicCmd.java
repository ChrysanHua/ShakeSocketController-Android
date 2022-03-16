package com.ssc.shakesocketcontroller.Transaction.events.command;

public class MusicCmd extends BaseCmdEvent {

    public MusicCmd(String str) {
        cmdType = CMD_MUSIC;
        paramStr = str;
    }
}
