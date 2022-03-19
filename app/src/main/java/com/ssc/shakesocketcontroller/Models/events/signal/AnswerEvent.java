package com.ssc.shakesocketcontroller.Models.events.signal;

public class AnswerEvent {

    private String ansType;
    private boolean ansStatus;
    private String ansValue;
    private String ansMsg;

    public AnswerEvent(String ansType, boolean ansStatus) {
        this.ansType = ansType;
        this.ansStatus = ansStatus;
    }

    public AnswerEvent(String ansType, boolean ansStatus, String ansValue, String ansMsg) {
        this.ansType = ansType;
        this.ansStatus = ansStatus;
        this.ansValue = ansValue;
        this.ansMsg = ansMsg;
    }

    public String getAnsType() {
        return ansType;
    }

    public boolean isAnsStatus() {
        return ansStatus;
    }

    public String getAnsValue() {
        return ansValue;
    }

    public String getAnsMsg() {
        return ansMsg;
    }

    public boolean checkAnsType(String cmdType) {
        return ansType.equals(cmdType);
    }
}
