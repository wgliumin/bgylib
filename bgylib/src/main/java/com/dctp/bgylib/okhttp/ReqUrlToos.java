package com.dctp.bgylib.okhttp;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;

public class ReqUrlToos {
    public static JSONObject createJsonObject(HashMap<String, Object> mParams) {
        //解决Map中带数组转换String多了双引号的问题
        JSONObject jsonObject = new JSONObject();
        for (String key : mParams.keySet()) {
            if (mParams.get(key) instanceof List<?>) {
                JSONArray array = new JSONArray();
                List<?> listParams = (List<?>) mParams.get(key);
                if (listParams != null) {
                    for (int i = 0; i < listParams.size(); i++) {
                        if (listParams.get(i) instanceof HashMap<?, ?>) {
                            HashMap<String, Object> tMaps = (HashMap<String, Object>) listParams.get(i);
                            JSONObject child = new JSONObject(tMaps);
                            array.put(child);
                        } else {
                            array.put(listParams.get(i));
                        }
                    }
                }
                try {
                    jsonObject.put(key, array);
                } catch (org.json.JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                try {
                    jsonObject.put(key, mParams.get(key));
                } catch (org.json.JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return jsonObject;
    }

    public static String buildGetRequestUrl(String url, HashMap<String, Object> mParams) {
        String temp = "";
        String newUrl = url;
        if (mParams == null)
            return newUrl;
        for (String key : mParams.keySet()) {
            String value = String.valueOf(mParams.get(key));
            if (value != null) {
                try {
                    value = URLEncoder.encode(value, "utf-8");
                    temp += "&" + key + "=";
                    temp += value;
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        if (url != null && url.length() > 0 && temp.length() > 0) {
            newUrl += "?" + temp.substring(1);
        }
        return newUrl;
    }


}