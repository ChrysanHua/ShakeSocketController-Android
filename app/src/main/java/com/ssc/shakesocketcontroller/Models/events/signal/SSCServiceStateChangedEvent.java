package com.ssc.shakesocketcontroller.Models.events.signal;

import com.google.common.base.MoreObjects;

public class SSCServiceStateChangedEvent {

    private final boolean finalState;
    private final boolean autoOperation;
    private final boolean hasError;
    private final long errWorkThreadID;
    private final SendUDPEvent sendFailureEvent;

    public SSCServiceStateChangedEvent(boolean finalState, boolean autoOperation, boolean hasError,
                                       long errWorkThreadID, SendUDPEvent sendFailureEvent) {
        this.finalState = finalState;
        this.autoOperation = autoOperation;
        this.hasError = hasError;
        this.errWorkThreadID = errWorkThreadID;
        this.sendFailureEvent = sendFailureEvent;
    }

    public SSCServiceStateChangedEvent(boolean finalState, boolean autoOperation,
                                       SendUDPEvent sendFailureEvent, long errWorkThreadID) {
        this(finalState, autoOperation, true, errWorkThreadID, sendFailureEvent);
    }

    public SSCServiceStateChangedEvent(boolean finalState, boolean autoOperation, boolean hasError,
                                       long errWorkThreadID) {
        this(finalState, autoOperation, hasError, errWorkThreadID, null);
    }

    public SSCServiceStateChangedEvent(boolean finalState, boolean autoOperation, boolean hasError) {
        this(finalState, autoOperation, hasError, -1, null);
    }

    public SSCServiceStateChangedEvent(boolean finalState) {
        this(finalState, false, false, -1, null);
    }

    public boolean getFinalState() {
        return finalState;
    }

    public boolean isHasError() {
        return hasError;
    }

    public long getErrWorkThreadID() {
        return errWorkThreadID;
    }

    public boolean isAutoOperation() {
        return autoOperation;
    }

    public SendUDPEvent getSendFailureEvent() {
        return sendFailureEvent;
    }

    public boolean isRunningException() {
        return errWorkThreadID != -1;
    }

    public boolean isSendFailed() {
        return sendFailureEvent != null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("finalState", finalState)
                .add("autoOperation", autoOperation)
                .add("hasError", hasError)
                .add("errWorkThreadID", errWorkThreadID)
                .add("sendFailureEvent", sendFailureEvent)
                .toString();
    }
}
