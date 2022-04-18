package com.ssc.shakesocketcontroller.Models.events.signal;

public class SSCServiceExceptionEvent {

    private final boolean initFailed;
    private final boolean hasException;
    private final long threadID;
    private final SendUDPEvent sendFailureEvent;

    private SSCServiceExceptionEvent(boolean initFailed, boolean hasException, long threadID,
                                     SendUDPEvent sendFailureEvent) {
        this.initFailed = initFailed;
        this.hasException = hasException;
        this.threadID = threadID;
        this.sendFailureEvent = sendFailureEvent;
    }

    /**
     * 前台服务初始化失败专用构造函数
     */
    public SSCServiceExceptionEvent() {
        this(true, true, -1, null);
    }

    /**
     * 监听运行时异常专用构造函数
     */
    public SSCServiceExceptionEvent(boolean hasException, long threadID) {
        this(false, hasException, threadID, null);
    }

    /**
     * 发送异常专用构造函数
     */
    public SSCServiceExceptionEvent(SendUDPEvent sendFailureEvent, long threadID) {
        this(false, true, threadID, sendFailureEvent);
    }

    public boolean isInitFailed() {
        return initFailed;
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

    public boolean isFromWorkThread() {
        return threadID != -1;
    }

    public boolean isSendFailed() {
        return sendFailureEvent != null;
    }
}
