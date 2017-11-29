package com.dctp.bgylib.event;

public class RequestEvent {
    private int code;

    public RequestEvent(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}