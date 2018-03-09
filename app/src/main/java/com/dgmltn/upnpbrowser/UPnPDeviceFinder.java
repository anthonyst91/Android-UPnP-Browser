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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TimeUtils;

import com.dgmltn.upnpbrowser.event.UPnPDeviceEvent;
import com.dgmltn.upnpbrowser.event.UPnPErrorEvent;
import com.dgmltn.upnpbrowser.event.UPnPObserverEndedEvent;

import org.greenrobot.eventbus.EventBus;

import static com.dgmltn.upnpbrowser.event.UPnPErrorEvent.ERROR_NULL_SOCKET;

/**
 * Based on:
 * https://github.com/heb-dtc/SSDPDiscovery/blob/master/src/main/java/com/flo/upnpdevicedetector/UPnPDeviceFinder.java
 */
class UPnPDeviceFinder {

    private static final String TAG = "UPnPDeviceFinder";

    private static final String MULTICAST_ADDRESS = "239.255.255.250";
    private static final int PORT = 1900;

    private static final int DEFAULT_MAX_REPLY_TIME_MS = (int)TimeUnit.SECONDS.toMillis(60);

    private int mTimeoutMs;

    // From Apache InetAddressUtils
    // https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/conn/util/InetAddressUtils.html
    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    private static final String NEWLINE = "\r\n";

    private UPnPSocket mSock;

    UPnPDeviceFinder() {
        this(DEFAULT_MAX_REPLY_TIME_MS, true);
    }

    UPnPDeviceFinder(int timeoutMs) {
        this(timeoutMs, true);
    }

    private UPnPDeviceFinder(int timeoutMs, boolean IPV4) {
        this.mTimeoutMs = timeoutMs;

        InetAddress inetAddress = getDeviceLocalIP(IPV4);
        Log.d(TAG, "inet device address is: " + inetAddress);

        try {
            mSock = new UPnPSocket(inetAddress);
        } catch (IOException e) {
            Log.w(TAG, "new UPnPSocket(): IOException: ", e);
        }
    }

    void observe() {
        if (mSock == null) {
            EventBus.getDefault().post(new UPnPErrorEvent(ERROR_NULL_SOCKET));
        }

        try {
            // Broadcast SSDP search messages
            mSock.sendMulticastMsg();

            // Listen to responses from network until the socket timeout
            // noinspection InfiniteLoopStatement
            while (true) {
                Log.i(TAG, "UPnP.observe...");

                DatagramPacket dp = mSock.receiveMulticastMsg();
                String receivedString = new String(dp.getData());

                receivedString = receivedString.substring(0, dp.getLength());
                Log.i(TAG, "UPnP.observe.device found: " + receivedString);

                UPnPDevice device = UPnPDevice.parse(receivedString);

                if (device != null) {
                    EventBus.getDefault().post(new UPnPDeviceEvent(device));
                }
            }

        } catch (IOException e) {
            //sock timeout will get us out of the loop
            Log.i(TAG, "observe.timed out: " + e.getMessage());
            mSock.close();
            EventBus.getDefault().post(new UPnPObserverEndedEvent());
        }
    }

    @Nullable
    public DatagramSocket getSocket() {
        if (mSock != null) {
            return mSock.getSocket();
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // UPnPSocket
    ////////////////////////////////////////////////////////////////////////////////

    private class UPnPSocket {

        private static final String TAG = "UPnPSocket";

        private SocketAddress mMulticastGroup;
        private MulticastSocket mMultiSocket;

        UPnPSocket(InetAddress deviceIp) throws IOException {
            mMulticastGroup = new InetSocketAddress(MULTICAST_ADDRESS, PORT);
            mMultiSocket = new MulticastSocket(new InetSocketAddress(deviceIp, 0));

            mMultiSocket.setSoTimeout(mTimeoutMs + 1000);
        }

        void sendMulticastMsg() throws IOException {
            String ssdpMsg = buildSSDPSearchString();

            Log.d(TAG, "sendMulticastMsg: " + ssdpMsg);

            DatagramPacket dp = new DatagramPacket(ssdpMsg.getBytes(), ssdpMsg.length(), mMulticastGroup);
            mMultiSocket.send(dp);
        }

        DatagramPacket receiveMulticastMsg() throws IOException {
            byte[] buf = new byte[2048];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);

            mMultiSocket.receive(dp);

            return dp;
        }

        /**
         * Closing the Socket.
         */
        public void close() {
            if (mMultiSocket != null) {
                mMultiSocket.close();
            }
        }

        @Nullable
        public DatagramSocket getSocket() {
            return mMultiSocket;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Utils
    ////////////////////////////////////////////////////////////////////////////////

    private String buildSSDPSearchString() {
        StringBuilder content = new StringBuilder();

        content.append("M-SEARCH * HTTP/1.1").append(NEWLINE);
        content.append("Host: " + MULTICAST_ADDRESS + ":" + PORT).append(NEWLINE);
        content.append("Man:\"ssdp:discover\"").append(NEWLINE);
        content.append("MX: ").append(TimeUnit.MILLISECONDS.toSeconds(mTimeoutMs)).append(NEWLINE);
        content.append("ST: upnp:rootdevice").append(NEWLINE);
        content.append(NEWLINE);

        Log.d(TAG, "buildSSDPSearchString: " + content.toString());

        return content.toString();
    }

    private static InetAddress getDeviceLocalIP(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        Log.i(TAG, "IP from inet is: " + addr);
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4) {
                                Log.i(TAG, "getDeviceLocalIP: IPv4");
                                return addr;
                            }
                        }
                        else {
                            if (!isIPv4) {
                                Log.i(TAG, "getDeviceLocalIP: IPv6");
                                //int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                //return delim<0 ? sAddr : sAddr.substring(0, delim);
                                return addr;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getDeviceLocalIP.Exception: ", e);
        }
        return null;
    }

    private static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }
}