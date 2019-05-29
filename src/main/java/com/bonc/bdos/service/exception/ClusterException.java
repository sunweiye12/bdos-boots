package com.bonc.bdos.service.exception;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClusterException extends RuntimeException {

    private int code;
    private String msg;
    private List<String> detail ;

    public ClusterException() {}

    public ClusterException(int code, String msg) {
        this.code = code;
        this.msg = msg;
        this.detail = new ArrayList<>();
    }

    public ClusterException(int code, Collection<String> errorMsgs, String msg) {
        this(code,msg);
        this.detail.addAll(errorMsgs);
    }

    public ClusterException(int code, String errorMsgs, String msg) {
        this(code,msg);
        this.detail.add(errorMsgs);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Collection<String> getDetail() {
        return detail;
    }

    public void setDetail(Collection<String> detail) {
        this.detail = new ArrayList<>(detail);
    }
}
