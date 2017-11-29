package com.dctp.bgylib.okhttp;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.dctp.bgylib.event.RequestEvent;
import com.dctp.bgylib.okhttp.entity.BaseEntity;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DctpModel<T> implements Callback {
    private final Class<T> mClazz;
    private final Activity mContext;
    private OkHttpClient.Builder builderUtils = null;
    private Request.Builder builder;
    public static String mUserId;
    public static String mToken;
    private int requestID;
    private RequestCallBack mCallBack;
    private Call call;

    public DctpModel(Activity context, Class<T> clazz) {
        this.mContext = context;
        this.mClazz = clazz;
        if (builderUtils == null) {
            builderUtils = OkUtils.instance().newBuilder();
            builderUtils.connectTimeout(20, TimeUnit.SECONDS);
            builderUtils.readTimeout(20, TimeUnit.SECONDS);
            builderUtils.writeTimeout(20, TimeUnit.SECONDS);
        }
    }

    public static void setHeadersUserInfo(String userId, String token) {
        mUserId = userId;
        mToken = token;
    }

    public void httpRequest(int reqID, MethodType methodType, String url, HashMap<String, Object> map, RequestCallBack callBack) {
        requestID = reqID;
        mCallBack = callBack;
        if (builder == null) {
            builder = addHeaders();
        }
        request(methodType, url, map, builder);
    }

    private void request(MethodType methodType, String url, HashMap<String, Object> map, Request.Builder builder) {
        if (methodType == MethodType.GET) {
            get(url, map, builder);
        } else if (methodType == MethodType.POST) {
            post(url, map, builder);
        } else if (methodType == MethodType.PUT) {
            put(url, map, builder);
        }
    }

    /**
     * post请求
     */
    private void get(String url, HashMap<String, Object> map, Request.Builder builder) {
        ReqLog.d("myLog", "reqURL-->" + url);
        String newUrl = ReqUrlToos.buildGetRequestUrl(url, map).toString();
        Request request = builder.url(newUrl).build();
        doRequest(request);
    }

    /**
     * post请求
     */
    private void post(String url, HashMap<String, Object> map, Request.Builder builder) {
        ReqLog.d("myLog", "reqURL-->" + url);
        String jsonBody = ReqUrlToos.createJsonObject(map).toString();
        //创建一个请求
        Request request = builder.url(url).post(RequestBody.create(getMediaType(), jsonBody)).build();
        doRequest(request);
    }

    /**
     * put请求
     */
    private void put(String url, HashMap<String, Object> map, Request.Builder builder) {
        ReqLog.d("myLog", "reqURL-->" + url);
        String jsonBody = ReqUrlToos.createJsonObject(map).toString();
        //创建一个请求
        Request request = builder.url(url).put(RequestBody.create(getMediaType(), jsonBody)).build();
        doRequest(request);
    }

    private void doRequest(Request request) {
        call = builderUtils.build().newCall(request);
        call.enqueue(this);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        if (e instanceof SocketTimeoutException) {
            try {
                onSuccess(requestID, null, -1);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            //超时重连
//            serversLoadTimes++;//超时次数
//            if (serversLoadTimes < maxLoadTimes)
//                OkHttpUtils.instance?.newCall(call?.request())?.enqueue(this);
//            if (serversLoadTimes == maxLoadTimes)
//            sendBroadcast(666)//连接超时
        }
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        onSuccess(requestID, response, response.code());
    }

    private void onSuccess(final int requestID, final Response response, final int code) throws IOException {
        final String data = response.body().string();
        ReqLog.d("myLog", "response-->" + data);
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (code == -1 || code == -2) {//连接超时||连接失败
                    mCallBack.receive(requestID, null, -1);
                    EventBus.getDefault().post(new RequestEvent(code));
                } else {
                    if (response.code() == 200) {
                        try {
                            Object obj = JSON.parseObject(data, mClazz);
                            mCallBack.receive(requestID, obj, code);
                        } catch (Exception e) {
                            e.printStackTrace();
                            mCallBack.receive(requestID, null, -2);
                            EventBus.getDefault().post(new RequestEvent(-2));
                        }
                    } else {
                        try {
                            Object obj = JSON.parseObject(data, BaseEntity.class);
                            mCallBack.receive(requestID, obj, code);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }


    /**
     * 统一为请求添加头信息
     *
     * @return
     */

    private Request.Builder addHeaders() {
        Request.Builder builder = new Request.Builder();
        builder.addHeader("Accept", "application/json");
        if (!TextUtils.isEmpty(mUserId) && !TextUtils.isEmpty(mToken)) {
            builder.addHeader("userId", mUserId);
            builder.addHeader("token", mToken);
        }
        return builder;
    }

    private MediaType getMediaType() {
        return MediaType.parse("application/json");
    }

}