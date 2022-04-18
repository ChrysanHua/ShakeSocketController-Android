package com.ssc.shakesocketcontroller.Models.events.signal;

public class SSCServiceExceptionEvent {

    private final boolean initFailed;
    private final boolean hasException;
    private final long threadID;

    private SSCServiceExceptionEvent(boolean initFailed, boolean hasException, long threadID) {
        this.initFailed = initFailed;
        this.hasException = hasException;
        this.threadID = threadID;
    }

    /**
     * 前台服务初始化失败专用构造函数
     */
    public SSCServiceExceptionEvent() {
        this(true, true, -1);
    }

    /**
     * 运行时异常专用构造函数
     */
    public SSCServiceExceptionEvent(boolean hasException, long threadID) {
        this(false, hasException, threadID);
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

    public boolean isFromWorkThread() {
        return threadID != -1;
    }
}
