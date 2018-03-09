/*
 * Copyright (C) 2015 Doug Melton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dgmltn.upnpbrowser;

import android.support.annotation.AnyThread;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.Log;

import com.dgmltn.upnpbrowser.event.UPnPDeviceEvent;
import com.dgmltn.upnpbrowser.event.UPnPErrorEvent;
import com.dgmltn.upnpbrowser.event.UPnPObserverEndedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class UPnPHelper {

    private static final String TAG = "UPnPHelper";

    @NonNull
    private UPnPDeviceAdapter mAdapter;

    @NonNull
    private UPnPDeviceFinder mUPnPFinder;

    public UPnPHelper(@NonNull UPnPDeviceAdapter adapter) {
        this(adapter, 0);
    }

    public UPnPHelper(@NonNull UPnPDeviceAdapter adapter,
                      int timeoutMs) {
        this.mAdapter = adapter;

        this.mUPnPFinder = (timeoutMs > 0) ?
                new UPnPDeviceFinder(timeoutMs) : new UPnPDeviceFinder();
    }

    @AnyThread
    public void startObserver() {
        EventBus.getDefault().register(this);

        Thread thread = new Thread() {
            @Override
            public void run() {
                mUPnPFinder.observe();
            }
        };
        thread.start();
    }

    @SuppressWarnings("WeakerAccess")
    @AnyThread
    public void destroyObserver() {
        EventBus.getDefault().unregister(this);
        if (mUPnPFinder.getSocket() != null) {
            mUPnPFinder.getSocket().close();
        }
    }

    //////////////////////
    // BUSINESS METHODS //
    //////////////////////

    @SuppressWarnings("WeakerAccess")
    @UiThread
    public void onFirstUPnPDeviceFound() {
        //ignore
    }

    @SuppressWarnings("WeakerAccess")
    @UiThread
    public void onUPnPDeviceFound(@NonNull UPnPDevice device) {
        addToRecycler(mAdapter, device);
    }

    @SuppressWarnings("WeakerAccess")
    @UiThread
    public void onUPnPObserverEnded() {
        //ignore
    }

    @SuppressWarnings("WeakerAccess")
    @UiThread
    public void onUPnPObserverError() {
        //ignore
    }

    ///////////////
    // EVENT BUS //
    ///////////////

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    @UiThread
    public void onUPnPDeviceEvent(@NonNull UPnPDeviceEvent event) {
        onUPnPDeviceFound(event.getUPnPDevice());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    @UiThread
    public void onUPnPObserverEndedEvent(@NonNull UPnPObserverEndedEvent event) {
        Log.i(TAG, "onUPnPObserveEndedEvent");
        destroyObserver();
        onUPnPObserverEnded();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    @UiThread
    public void onUPnPErrorEvent(@NonNull UPnPErrorEvent event) {
        Log.i(TAG, "onUPnPErrorEvent.errorCode: " + event.getErrorCode());
        destroyObserver();
        onUPnPObserverError();
    }

    /////////////////////
    // PRIVATE METHODS //
    /////////////////////

    @UiThread
    private void addToRecycler(@NonNull UPnPDeviceAdapter adapter,
                               @NonNull UPnPDevice device) {
        if (adapter.getItemCount() == 0) {
            onFirstUPnPDeviceFound();
        }
        adapter.addItem(device);
    }

}
