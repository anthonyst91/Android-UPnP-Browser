package com.dgmltn.upnpbrowser.event;

import com.dgmltn.upnpbrowser.UPnPDevice;

import io.reactivex.annotations.NonNull;

public class UPnPDeviceEvent {

    @NonNull
    private UPnPDevice mUPnPDevice;

    public UPnPDeviceEvent(@NonNull UPnPDevice uPnPDevice) {
        this.mUPnPDevice = uPnPDevice;
    }

    @NonNull
    public UPnPDevice getUPnPDevice() {
        return mUPnPDevice;
    }
}
