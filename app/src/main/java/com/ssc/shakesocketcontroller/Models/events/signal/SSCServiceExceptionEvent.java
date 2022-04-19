package com.ssc.shakesocketcontroller.Models.events.signal;

import com.google.common.base.MoreObjects;

public class SSCServiceExceptionEvent {

    private final boolean serviceState;             //标志是启动/停止时的异常，或用于表示异常发生时服务的状态
    private final boolean hasException;             //是否由已捕捉的Exception导致
    private final long threadID;                    //发生异常的工作线程的ID（值为-1则意味着是正在启动/停止时发生的异常）
    private final SendUDPEvent sendFailureEvent;    //导致发送异常的事件对象

    private SSCServiceExceptionEvent(boolean serviceState, boolean hasException, long threadID,
                                     SendUDPEvent sendFailureEvent) {
        this.serviceState = serviceState;
        this.hasException = hasException;
        this.threadID = threadID;
        this.sendFailureEvent = sendFailureEvent;
    }

    /**
     * 前台服务启动/停止时失败专用构造函数
     */
    public SSCServiceExceptionEvent(boolean serviceState) {
        this(serviceState, true, -1, null);
    }

    /**
     * 监听运行时异常专用构造函数
     */
    public SSCServiceExceptionEvent(boolean hasException, long threadID) {
        this(true, hasException, threadID, null);
    }

    /**
     * 发送异常专用构造函数
     */
    public SSCServiceExceptionEvent(SendUDPEvent sendFailureEvent, long threadID) {
        this(true, true, threadID, sendFailureEvent);
    }

    public boolean getServiceState() {
        return serviceState;
    }

    public boolean hasException() {
        return hasException;
    }

    public long getThreadID() {
        return threadID;
    }

    public SendUDPEvent getSendFailureEvent() {
        return sendFailureEvent;
    }

    public boolean isRunningException() {
        return threadID != -1;
    }

    public boolean isSendFailed() {
        return sendFailureEvent != null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("serviceState", serviceState)
                .add("hasException", hasException)
                .add("threadID", threadID)
                .add("sendFailureEvent", sendFailureEvent)
                .toString();
    }
}
