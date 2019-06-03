package com.bonc.bdos.api.v1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bonc.bdos.common.ApiHandle;
import com.bonc.bdos.common.ApiResult;
import com.bonc.bdos.service.entity.SysClusterHost;
import com.bonc.bdos.service.entity.SysClusterHostRole;
import com.bonc.bdos.service.entity.SysClusterHostRoleDev;
import com.bonc.bdos.service.service.CallService;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/v1/callback")
public class CallbackController {
    private static final Logger LOG = LoggerFactory.getLogger(CallbackController.class);

    private final CallService callService;

    @Autowired
    public CallbackController(CallService callService) {
        this.callService = callService;
    }

    /**
     * 用户 执行脚本curl回调主机信息，保存主机的校验状态
     *
     * @param host 主机信息，里面可以有设备信息
     */
    @RequestMapping(value = {"/host"}, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存主机信息", notes = "回调保存主机信息")
    public ApiResult callbackHost(@RequestParam String host) {
        return ApiHandle.handle(() -> {
            callService.saveHost(JSON.parseObject(host, SysClusterHost.class));
            return new ArrayList<>();
        }, host,LOG);
    }

    @RequestMapping(value = {"/role"}, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存角色信息", notes = "回调保存角色信息")
    public ApiResult callbackRole(@RequestParam String hostRole) {
        return ApiHandle.handle(() -> {
            callService.saveHostRole(JSON.parseObject(hostRole, SysClusterHostRole.class));
            return new ArrayList<>();
        }, hostRole,LOG);
    }

    @RequestMapping(value = {"/dev"}, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存设备信息", notes = "回调保存设备信息")
    public ApiResult callbackDev(@RequestParam String roleDev) {
        return ApiHandle.handle(() -> {
            callService.saveRoleDev(JSON.parseObject(roleDev, SysClusterHostRoleDev.class));
            return new ArrayList<>();
        }, roleDev,LOG);
    }

    @RequestMapping(value = {"/global"}, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存全局信息", notes = "回调保存全局信息")
    public ApiResult callbackGlobal(@RequestParam String global) {
        return ApiHandle.handle(() -> {
            JSONObject json = JSON.parseObject(global);
            HashMap<String, String> map = new HashMap<>();
            if (null != json) {
                for (String key : json.keySet()) {
                    map.put(key, json.get(key).toString());
                }
            }
            callService.saveGlobal(map);
            return new ArrayList<>();
        },global, LOG);
    }
}
