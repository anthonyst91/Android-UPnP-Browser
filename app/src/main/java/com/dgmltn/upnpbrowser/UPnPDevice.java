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
import java.io.StringReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UPnPDevice {

    private static final String TAG = "UPnPDevice";

    private String mRawUPnP;
    private String mRawXml;
    private URL mLocation;
    private String mServer;

    private HashMap<String, String> mProperties;
    private String mCachedIconUrl;

    private UPnPDevice() {
    }

    public String getHost() {
        return mLocation.getHost();
    }

    public InetAddress getInetAddress() throws UnknownHostException {
        return InetAddress.getByName(getHost());
    }

    @Nullable
    public URL getLocation() {
        return mLocation;
    }

    public String getRawUPnP() {
        return mRawUPnP;
    }

    public String getRawXml() {
        return mRawXml;
    }

    public String getServer() {
        return mServer;
    }

    @Nullable
    public String getIconUrl() {
        return mCachedIconUrl;
    }

    @NonNull
    public String getFriendlyName() {
        String friendlyName = mProperties.get("xml_friendly_name");
        // Special case for SONOS: remove the leading ip address from the friendly name
        // "192.168.1.123 - Sonos PLAY:1" => "Sonos PLAY:1"
        if (friendlyName != null && friendlyName.startsWith(getHost() + " - ")) {
            friendlyName = friendlyName.substring(getHost().length() + 3);
        }
        return TextUtils.isEmpty(friendlyName) ? "unknown" : friendlyName;
    }

    @NonNull
    public String getDeviceType() {
        String deviceType = mProperties.get("xml_device_type");
        return TextUtils.isEmpty(deviceType) ? "unknown" : deviceType;
    }

    @NonNull
    public String getManufacturer() {
        String manufacturer = mProperties.get("xml_manufacturer");
        return TextUtils.isEmpty(manufacturer) ? "unknown" : manufacturer;
    }

    @Nullable
    public String getManufacturerUrl() {
        return mProperties.get("xml_manufacturer_url");
    }

    @NonNull
    public String getModelName() {
        String modelName = mProperties.get("xml_model_name");
        return TextUtils.isEmpty(modelName) ? "unknown" : modelName;
    }

    @Override
    public String toString() {
        InetAddress inetAddr = null;
        try {
            inetAddr = getInetAddress();
        } catch (UnknownHostException e) {
            //ignore
        }
        return "UPnPDevice {" +
                "friendlyName: " + getFriendlyName() +
                ", server: " + getServer() +
                ", host: " + getHost() +
                ", inetAddr: " + inetAddr +
                ", location: " + getLocation() +
                ", iconUrl: " + getIconUrl() +
                ", deviceType: " + getDeviceType() +
                ", modelName: " + getModelName() +
                ", manufacturer: " + getManufacturer() +
                ", manufacturerUrl: " + getManufacturerUrl();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // UPnP Response Parsing
    ////////////////////////////////////////////////////////////////////////////////

    public static UPnPDevice parse(@NonNull String raw) {
        HashMap<String, String> parsed = parseRaw(raw);
        try {
            UPnPDevice device = new UPnPDevice();
            device.mRawUPnP = raw;
            device.mProperties = parsed;
            device.mLocation = new URL(parsed.get("upnp_location"));
            device.mServer = parsed.get("upnp_server");
            return device;

        } catch (MalformedURLException e) {
            Log.e(TAG, "parse.MalformedURLException: ", e);
            return null;
        }
    }

    private static HashMap<String, String> parseRaw(String raw) {
        HashMap<String, String> results = new HashMap<>();
        for (String line : raw.split("\r\n")) {
            int colon = line.indexOf(":");
            if (colon != -1) {
                String key = line.substring(0, colon).trim().toLowerCase();
                String value = line.substring(colon + 1).trim();
                results.put("upnp_" + key, value);
            }
        }
        return results;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // UPnP Specification Downloading / Parsing
    ////////////////////////////////////////////////////////////////////////////////

    private transient final OkHttpClient mClient = new OkHttpClient();

    void downloadSpecs() throws Exception {
        Request request = new Request.Builder()
                .url(mLocation)
                .build();

        Response response = mClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        ResponseBody body = response.body();

        mRawXml = (body == null) ? "" : body.string();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource source = new InputSource(new StringReader(mRawXml));
        Document doc;
        try {
            doc = db.parse(source);
        } catch (SAXParseException e) {
            return;
        }
        XPath xPath = XPathFactory.newInstance().newXPath();

        mProperties.put("xml_friendly_name", xPath.compile("//friendlyName").evaluate(doc));
        mProperties.put("xml_device_type", xPath.compile("//deviceType").evaluate(doc));
        mProperties.put("xml_manufacturer", xPath.compile("//manufacturer").evaluate(doc));
        mProperties.put("xml_manufacturer_url", xPath.compile("//manufacturerURL").evaluate(doc));
        mProperties.put("xml_model_name", xPath.compile("//modelName").evaluate(doc));

        mProperties.put("xml_icon_url", xPath.compile("//icon/url").evaluate(doc));
        generateIconUrl();
    }

    private void generateIconUrl() {
        String path = mProperties.get("xml_icon_url");
        if (TextUtils.isEmpty(path)) {
            return;
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        mCachedIconUrl = mLocation.getProtocol() + "://" + mLocation.getHost() + ":" + mLocation.getPort() + "/" + path;
    }
}
