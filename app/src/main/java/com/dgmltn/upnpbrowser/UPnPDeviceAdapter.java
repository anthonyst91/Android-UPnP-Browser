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

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Dimension;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public abstract class UPnPDeviceAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private Comparator<UPnPDevice> mComparator = new UPnPDeviceComparator();

    @NonNull
    private Context mContext;

    @NonNull
    private LayoutInflater mInflater;

    @NonNull
    private Picasso mPicasso;

    @NonNull
    private ArrayList<UPnPDevice> mItems;

    public UPnPDeviceAdapter(@NonNull Context context) {
        mContext = context;
        mItems = new ArrayList<>();

        mInflater = LayoutInflater.from(context);
        mPicasso = Picasso.get();
        mPicasso.setIndicatorsEnabled(false);

        setHasStableIds(false);
    }

    public Context getContext() {
        return mContext;
    }

    @NonNull
    public LayoutInflater getInflater() {
        return mInflater;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    public UPnPDevice getItem(int position) {
        return mItems.get(position);
    }

    public void clear() {
        int count = mItems.size();
        mItems.clear();
        notifyItemRangeRemoved(0, count);
    }

    public void addItem(@NonNull UPnPDevice item) {
        int index = Collections.binarySearch(mItems, item, mComparator);
        if (index < 0) {
            int position = -index - 1;
            mItems.add(position, item);
            notifyItemInserted(position);
        } else {
            mItems.set(index, item);
            notifyItemChanged(index);
        }
    }

    public void setName(@NonNull TextView textView,
                        @NonNull UPnPDevice device) {
        textView.setText(device.getFriendlyName());
    }

    public void setIpAddress(@NonNull TextView textView,
                             @NonNull UPnPDevice device) {
        textView.setText(device.getHost());
    }

    @SuppressLint("SetTextI18n")
    public void setLocation(@NonNull TextView textView,
                            @NonNull UPnPDevice device) {
        if (device.getLocation() == null) {
            textView.setText("unknown");
            return;
        }
        String loc = device.getLocation().toExternalForm()
                // Uncomment to obscure actual ip addresses for screenshots
                // .replaceAll("[0-9]+\\.[0-9]+\\.[0-9]+", "192.258.1")
                ;
        linkify(textView, loc);
    }

    private void linkify(@NonNull TextView textView,
                         @Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        SpannableBuilder builder = new SpannableBuilder(textView.getContext());
        textView.setText(builder.build());
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void setIcon(@NonNull ImageView imageView,
                        @NonNull UPnPDevice device,
                        @Dimension int size) {
        if (!TextUtils.isEmpty(device.getIconUrl())) {
            mPicasso.load(device.getIconUrl())
                    .error(getDefaultIcon())
                    .resize(size, size)
                    .centerInside()
                    .into(imageView);

        } else {
            imageView.setImageResource(getDefaultIcon());
        }
    }

    /////////////////
    // ABSTRACTION //
    /////////////////

    @DrawableRes
    public abstract int getDefaultIcon();

}
