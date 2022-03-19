package com.ssc.shakesocketcontroller.Models.events.command;

public class MusicCmd extends BaseCmdEvent {

    public MusicCmd(String str) {
        cmdType = CMD_MUSIC;
        paramStr = str;
    }
}
