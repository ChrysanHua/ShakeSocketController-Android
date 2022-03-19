package com.ssc.shakesocketcontroller.Models.events.command;

public class PPTCmd extends BaseCmdEvent {

    public PPTCmd(String str){
        cmdType = CMD_PPT;
        paramStr = str;
    }
}
