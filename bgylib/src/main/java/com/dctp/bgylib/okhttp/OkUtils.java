package com.dctp.bgylib.okhttp;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class OkUtils {

    public static OkHttpClient instance() {
        OkHttpClient singleton = null;
        if (singleton == null) {
            synchronized (OkUtils.class) {
                if (singleton == null) {
                    singleton = new OkHttpClient();
                }
            }
        }
        return singleton;
    }
}