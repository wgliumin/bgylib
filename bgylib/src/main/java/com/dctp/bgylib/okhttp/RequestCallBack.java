package com.dctp.bgylib.okhttp;

public interface RequestCallBack {
    /**
     * 网络返回
     *
     * @param requestID 请求ID
     * @param obj       返回对象
     * @param code      -1连接超时 -2服务器异常
     */
    void receive(int requestID, Object obj, int code);
}
