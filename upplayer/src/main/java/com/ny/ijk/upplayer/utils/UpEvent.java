package com.ny.ijk.upplayer.utils;

/**
 * 事件，EventBus订阅发布。
 */
public class UpEvent<T> {
    private int code;
    private T data;

    public UpEvent(int code) {
        this.code = code;
    }

    public UpEvent(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
