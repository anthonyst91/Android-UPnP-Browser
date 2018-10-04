package com.dgmltn.upnpbrowser.event;

import android.support.annotation.Nullable;

public class UPnPErrorEvent {

    public static final int ERROR_NULL_SOCKET = -1;
    public static final int ERROR_OPEN_FAILED = -2;

    private int mErrorCode;

    @Nullable
    private String mMessage;

    public UPnPErrorEvent(int errorCode,
                          @Nullable String message) {
        this.mErrorCode = errorCode;
        this.mMessage = message;
    }

    public int getErrorCode() {
        return mErrorCode;
    }
}
