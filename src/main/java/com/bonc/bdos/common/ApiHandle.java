package com.bonc.bdos.common;

import com.bonc.bdos.api.v1.ClusterController;
import com.bonc.bdos.consts.ReturnCode;
import com.bonc.bdos.service.exception.ClusterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public interface ApiHandle {
    Logger LOG = LoggerFactory.getLogger(ClusterController.class);

    Object handle() throws ClusterException, IOException;

    static ApiResult handle(ApiHandle proxy){
        return handle(proxy,LOG);
    }

    static ApiResult handle(ApiHandle proxy,Logger log){
        try {
            Object data = proxy.handle();
            return new ApiResult(ReturnCode.CODE_SUCCESS,data,"操作成功！");
        }catch (ClusterException e){
            LOG.error(e.getMsg());
            return new ApiResult(e.getCode(),e.getDetail(),e.getMsg());
        }catch (Exception e){
            e.printStackTrace();
            return new ApiResult(ReturnCode.CODE_CLUSTER_SYSTEM_ERROR,e.getMessage(),"系统异常");
        }
    }

}
