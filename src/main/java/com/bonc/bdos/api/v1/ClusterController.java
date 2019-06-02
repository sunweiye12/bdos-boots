package com.bonc.bdos.api.v1;

import com.bonc.bdos.common.ApiHandle;
import com.bonc.bdos.common.ApiResult;
import com.bonc.bdos.service.Global;
import com.bonc.bdos.service.entity.SysClusterHost;
import com.bonc.bdos.service.service.ClusterService;
import com.bonc.bdos.service.service.HostService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1")
public class ClusterController {

    private final HostService hostService;
    private final ClusterService clusterService;

    @Autowired
    public ClusterController(HostService hostService, ClusterService clusterService) {
        this.hostService = hostService;
        this.clusterService = clusterService;
    }

    /**
     * 主机保存接口
     */
    @RequestMapping(value = {"/host"}, method = RequestMethod.POST)
    @ApiOperation(value = "主机保存接口", notes = "主机保存接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "host", value = "主机信息"))
    public ApiResult saveHost(@RequestBody @Validated SysClusterHost host) {
        return ApiHandle.handle(() -> {
            hostService.saveHost(host);
            return new ArrayList<>();
        });
    }

    /**
     * 添加删除接口
     */
    @RequestMapping(value = {"/host"}, method = RequestMethod.DELETE)
    @ApiOperation(value = "主机删除接口", notes = "主机删除接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "ip", value = "主机IP"))
    public ApiResult delHost(@RequestBody String ip) {
        return ApiHandle.handle(() -> {
            hostService.deleteHost(ip, false);
            return new ArrayList<>();
        });
    }

    /**
     * 主机查询接口
     */
    @RequestMapping(value = {"/host"}, method = RequestMethod.GET)
    @ApiOperation(value = "主机查询接口", notes = "主机查询接口")
    public ApiResult findHost() {
        return ApiHandle.handle(hostService::findHosts);
    }

    /**
     * 查询存储配置信息
     */
    @RequestMapping(value = {"/store"}, method = RequestMethod.GET)
    @ApiOperation(value = "查询存储配置信息", notes = "查询存储配置信息")
    public ApiResult store() {
        return ApiHandle.handle(clusterService::storeCfg);
    }

    /**
     * 查询存储配置信息
     */
    @RequestMapping(value = {"/roles_cfg"}, method = RequestMethod.GET)
    @ApiOperation(value = "查询角色配置", notes = "查询角色配置")
    public ApiResult getRoles() {
        return ApiHandle.handle(clusterService::roleCfg);
    }

    /**
     * 角色保存接口
     */
    @RequestMapping(value = {"/roles"}, method = RequestMethod.POST)
    @ApiOperation(value = "角色保存接口", notes = "角色保存接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "roles", value = "角色信息", dataType = "Map[String,Set[String]]", example = "{\"roles\":[\"192.168.1.1\",\"192.168.1.2\",\"192.168.1.3\"]}"))
    public ApiResult saveRoles(@RequestBody HashMap<String, Set<String>> roles) {
        return ApiHandle.handle(() -> {
            clusterService.saveRoles(roles);
            return new ArrayList<>();
        });
    }

    /**
     * 角色查询接口
     */
    @RequestMapping(value = {"/roles"}, method = RequestMethod.GET)
    @ApiOperation(value = "角色查询接口", notes = "角色查询接口")
    public ApiResult findRoles() {
        return ApiHandle.handle(clusterService::findRoles);
    }

    /**
     * 全局配置保存接口
     */
    @RequestMapping(value = {"/global"}, method = RequestMethod.POST)
    @ApiOperation(value = "全局配置保存接口", notes = "全局配置保存接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "global", value = "全局配置参数"))
    public ApiResult saveGlobal(@RequestBody HashMap<String, String> global) {
        return ApiHandle.handle(() -> {
            clusterService.saveGlobal(global, Global.OUTER_SET);
            return new ArrayList<>();
        });
    }

    /**
     * 全局配置查询接口
     */
    @RequestMapping(value = {"/global"}, method = RequestMethod.GET)
    @ApiOperation(value = "全局配置查询接口", notes = "全局配置查询接口")
    public ApiResult findGlobal() {
        return ApiHandle.handle(clusterService::findGlobal);
    }

    /**
     * 上传文件接口
     *
     * @param template 文件模板数据流
     * @return 解析结果
     */
    @RequestMapping(value = {"/upload"}, method = RequestMethod.POST)
    public ApiResult upload(@RequestParam("template") MultipartFile template) {
        return ApiHandle.handle(() -> {
            hostService.saveTemplate(template.getInputStream());
            return new ArrayList<>();
        });
    }

    /**
     * 角色查询接口
     */
    @RequestMapping(value = {"/policy"}, method = RequestMethod.POST)
    @ApiOperation(value = "查询角色策略", notes = "角色查询接口")
    public ApiResult rolePolicy(@RequestBody List<SysClusterHost> hosts) {
        return ApiHandle.handle(() -> clusterService.rolePolicy(hosts));
    }
}
