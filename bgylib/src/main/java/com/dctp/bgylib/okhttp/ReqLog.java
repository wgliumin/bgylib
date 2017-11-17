package com.dctp.bgylib.okhttp;

import android.util.Log;

public class ReqLog {
    static boolean isDebug = true;

    public static void d(String key, String value) {
        if (isDebug)
            Log.d(key, value);
    }
}