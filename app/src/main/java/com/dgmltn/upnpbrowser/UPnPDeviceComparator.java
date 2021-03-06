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

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Comparator;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;

class UPnPDeviceComparator implements Comparator<UPnPDevice> {

    @Override
    public int compare(@Nullable UPnPDevice device1,
                       @Nullable UPnPDevice device2) {
        // Handle null objects
        int compare = compareNull(device1, device2);
        if (compare == 0 && device1 == null) {
            return compare;
        }

        URL mine = device1.getLocation();
        URL hers = device2.getLocation();
        compare = compareNull(mine, hers);
        //noinspection ConstantConditions
        if (compare == 0 && device1 == null) {
            return compare;
        }

        // Compare ip addresses
        compare = compareInetAddresses(mine, hers);
        if (compare != 0) {
            return compare;
        }

        // Compare ports
        if (mine == null) {
            return 1;
        }
        if (hers == null) {
            return -1;
        }
        compare = mine.getPort() - hers.getPort();
        if (compare != 0) {
            return compare;
        }

        // String compare paths
        return mine.getPath().compareTo(hers.getPath());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Null
    // a "null" object is less than a populated one
    // If they're both null or both non-null, then they're equal
    ///////////////////////////////////////////////////////////////////////////

    private int compareNull(@Nullable Object lhs,
                            @Nullable Object rhs) {
        if (lhs == null) {
            return rhs == null ? 0 : -1;
        } else if (rhs == null) {
            return 1;
        }
        return 0;
    }

    ///////////////////////////////////////////////////////////////////////////
    // IP Address
    ///////////////////////////////////////////////////////////////////////////

    private int compareInetAddresses(@NonNull URL lhs, @NonNull URL rhs) {
        InetAddress mine = null;
        InetAddress hers = null;
        try {
            mine = InetAddress.getByName(lhs.getHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            hers = InetAddress.getByName(rhs.getHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        int compare = compareNull(mine, hers);
        if (compare == 0 && mine == null) {
            return 0;
        }

        return compareInetAddress(mine, hers);
    }

    private int compareInetAddress(@NonNull InetAddress adr1, @NonNull InetAddress adr2) {
        byte[] ba1 = adr1.getAddress();
        byte[] ba2 = adr2.getAddress();

        // general ordering: ipv4 before ipv6
        if(ba1.length < ba2.length) {
            return -1;
        }
        if(ba1.length > ba2.length) {
            return 1;
        }

        // we have 2 ips of the same type, so we have to compare each byte
        for(int i = 0; i < ba1.length; i++) {
            int b1 = unsignedByteToInt(ba1[i]);
            int b2 = unsignedByteToInt(ba2[i]);
            if(b1 == b2) {
                continue;
            }
            if(b1 < b2) {
                return -1;
            } else {
                return 1;
            }
        }
        return 0;
    }

    private int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }
}
