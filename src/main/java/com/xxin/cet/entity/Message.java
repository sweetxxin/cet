package com.xxin.cet.entity;

/**
 * @author xxin
 * @Created
 * @Date 2019/8/21 2:01
 * @Description
 */
public class Message {
    private boolean success;
    private String msg;
    private Object data;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
