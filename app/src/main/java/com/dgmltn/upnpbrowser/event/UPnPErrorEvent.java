package com.dgmltn.upnpbrowser.event;

public class UPnPErrorEvent {

    public static final int ERROR_NULL_SOCKET = -1;

    private int mErrorCode;

    public UPnPErrorEvent(int errorCode) {
        this.mErrorCode = errorCode;
    }

    public int getErrorCode() {
        return mErrorCode;
    }
}
