package com.dctp.bgylib.okhttp;

public interface RequestCallBack {
    void receive(int requestID, Object obj, int code);
}
