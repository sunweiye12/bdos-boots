package com.bonc.bdos.api.v1;

import com.bonc.bdos.common.ApiHandle;
import com.bonc.bdos.common.ApiResult;
import com.bonc.bdos.service.service.DevService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Set;

@RestController
@RequestMapping("/v1/dev")
public class DevController {
    private static final Logger LOG = LoggerFactory.getLogger(DevController.class);

    private final DevService devService;

    @Autowired
    public DevController(DevService devService) {
        this.devService = devService;
    }

    @RequestMapping(value = { "" }, method = RequestMethod.GET)
    @ApiOperation(value = "设备查询接口", notes = "设备查询接口")
    public ApiResult findDev(@RequestParam String ip) {
        return ApiHandle.handle(() -> devService.findDev(ip),LOG);
    }

    /**
     * 设备启用接口
     */
    @RequestMapping(value = { "/enable" }, method = RequestMethod.POST)
    @ApiOperation(value = "设备启用接口", notes = "设备启用接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "devId",value = "设备ID,通过主机查询接口获取"))
    public ApiResult devEnable(@RequestBody String devId) {
        return ApiHandle.handle(() -> {
            devService.enableDev(devId,true);
            return new ArrayList<>();
        },LOG);
    }

    /**
     * 设备停用接口
     */
    @RequestMapping(value = { "/disable" }, method = RequestMethod.POST)
    @ApiOperation(value = "设备停用接口", notes = "设备停用接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "devId",value = "设备ID,通过主机查询接口获取"))
    public ApiResult dev(@RequestBody String devId) {
        return ApiHandle.handle(() -> {
            devService.enableDev(devId,false);
            return new ArrayList<>();
        },LOG);
    }


    /**
     * 设备计算接口
     */
    @RequestMapping(value = { "/allocate" }, method = RequestMethod.POST)
    @ApiOperation(value = "设备计算接口", notes = "设备计算接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "targets",value = "目标主机（必须已经校验通过）"))
    public ApiResult allocateDev(@RequestBody Set<String> targets) {
        return ApiHandle.handle(() -> {
            devService.allocate(targets);
            return new ArrayList<>();
        },LOG);
    }
}
