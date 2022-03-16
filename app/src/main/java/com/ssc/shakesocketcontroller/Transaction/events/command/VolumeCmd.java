package com.ssc.shakesocketcontroller.Transaction.events.command;

public class VolumeCmd extends BaseCmdEvent {

    public VolumeCmd(String str){
        cmdType = CMD_VOLUME;
        paramStr = str;
    }
}
