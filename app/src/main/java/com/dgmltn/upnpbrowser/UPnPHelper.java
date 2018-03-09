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
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.dgmltn.upnpbrowser.event.UPnPDeviceEvent;
import com.dgmltn.upnpbrowser.event.UPnPErrorEvent;
import com.dgmltn.upnpbrowser.event.UPnPObserveEndedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class UPnPHelper {

    private static final String TAG = "UPnPHelper";

    @NonNull
    private RecyclerView mRecycler;

    @NonNull
    private UPnPDeviceAdapter mAdapter;

    @NonNull
    private UPnPDeviceFinder mUPnPFinder;

    public UPnPHelper(@NonNull RecyclerView recycler,
                      @NonNull UPnPDeviceAdapter adapter) {
        this.mRecycler = recycler;
        this.mAdapter = adapter;

        this.mUPnPFinder = new UPnPDeviceFinder();
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

    @AnyThread
    public void destroyObserver() {
        EventBus.getDefault().unregister(this);
    }

    ///////////////
    // EVENT BUS //
    ///////////////

    @Subscribe
    public void onUPnPDeviceEvent(@NonNull UPnPDeviceEvent event) {
        onUPnPDeviceFound(event.getUPnPDevice());
    }

    @Subscribe
    public void onUPnPObserveEndedEvent(@NonNull UPnPObserveEndedEvent event) {
        Log.i(TAG, "onUPnPObserveEndedEvent");
        destroyObserver();
    }

    @Subscribe
    public void onUPnPErrorEvent(@NonNull UPnPErrorEvent event) {
        destroyObserver();
        Log.i(TAG, "onUPnPErrorEvent.errorCode: " + event.getErrorCode());
    }

    /////////////////////
    // PRIVATE METHODS //
    /////////////////////

    @UiThread
    private void onUPnPDeviceFound(@NonNull UPnPDevice device) {
        try {
            device.downloadSpecs();
        } catch (Exception e) {
            Log.w(TAG, "onUPnPDeviceFound.downloadSpecs.Exception: ", e);
        }

        addToRecycler(mRecycler, mAdapter, device);
    }

    @UiThread
    private void addToRecycler(@NonNull RecyclerView recycler,
                               @NonNull UPnPDeviceAdapter adapter,
                               @NonNull UPnPDevice device) {
        // This is the first device found.
        if (adapter.getItemCount() == 0) {
            recycler.setAlpha(0f);
            recycler.setVisibility(View.VISIBLE);
            recycler.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setStartDelay(1000)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        adapter.addItem(device);
    }

}
