package com.bonc.bdos.service.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
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
}
