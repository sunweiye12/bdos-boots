package com.bonc.bdos.api.v1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bonc.bdos.common.ApiResult;
import com.bonc.bdos.consts.ReturnCode;
import com.bonc.bdos.service.Global;
import com.bonc.bdos.service.entity.SysClusterHost;
import com.bonc.bdos.service.entity.SysClusterHostRole;
import com.bonc.bdos.service.entity.SysClusterHostRoleDev;
import com.bonc.bdos.service.exception.ClusterException;
import com.bonc.bdos.service.service.CallbackService;
import com.bonc.bdos.service.service.ClusterService;
import com.bonc.bdos.service.service.ExecService;
import com.bonc.bdos.service.service.HostService;
import com.bonc.bdos.service.tasks.TaskManager;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/cluster/")
public class ClusterController {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterController.class);

    private final HostService hostService;
    private final ClusterService clusterService;
    private final ExecService execService;
    private final CallbackService callbackService;

    interface BusinessHandle{

        Object handle() throws ClusterException, IOException;

    }

    private ApiResult handle(BusinessHandle proxy){
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

    @Autowired
    public ClusterController(HostService hostService,ClusterService clusterService,ExecService execService,CallbackService callbackService) {
        this.hostService = hostService;
        this.clusterService = clusterService;
        this.execService = execService;
        this.callbackService = callbackService;
    }

    /**
     * 主机保存接口
     */
    @RequestMapping(value = { "host" }, method = RequestMethod.POST)
    @ApiOperation(value = "主机保存接口", notes = "主机保存接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "host",value = "主机信息"))
    public ApiResult saveHost(@RequestBody @Validated SysClusterHost host) {
        return handle(() -> {
            hostService.saveHost(host);
            return new ArrayList<>();
        });
    }

    /**
     * 添加删除接口
     */
    @RequestMapping(value = { "host" }, method = RequestMethod.DELETE)
    @ApiOperation(value = "主机删除接口", notes = "主机删除接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "ip",value = "主机IP"))
    public ApiResult delHost(@RequestBody  String ip) {
        return handle(() -> {
            hostService.deleteHost(ip,false);
            return new ArrayList<>();
        });
    }
    
    /**
     * 主机查询接口
     */
    @RequestMapping(value = { "host" }, method = RequestMethod.GET)
    @ApiOperation(value = "主机查询接口", notes = "主机查询接口")
    public ApiResult findHost() {
        return handle(hostService::findHosts);
    }


    @RequestMapping(value = { "dev" }, method = RequestMethod.GET)
    @ApiOperation(value = "设备查询接口", notes = "设备查询接口")
    public ApiResult findDev(@RequestParam String ip) {
        return handle(() -> hostService.findDev(ip));
    }

    /**
     * 设备启用接口
     */
    @RequestMapping(value = { "dev/enable" }, method = RequestMethod.POST)
    @ApiOperation(value = "设备启用接口", notes = "设备启用接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "devId",value = "设备ID,通过主机查询接口获取"))
    public ApiResult devEnable(@RequestBody String devId) {
        return handle(() -> {
            hostService.enableDev(devId,true);
            return new ArrayList<>();
        });
    }

    /**
     * 设备停用接口
     */
    @RequestMapping(value = { "dev/disable" }, method = RequestMethod.POST)
    @ApiOperation(value = "设备停用接口", notes = "设备停用接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "devId",value = "设备ID,通过主机查询接口获取"))
    public ApiResult dev(@RequestBody String devId) {
        return handle(() -> {
            hostService.enableDev(devId,false);
            return new ArrayList<>();
        });
    }

    /**
     * 查询存储配置信息
     */
    @RequestMapping(value = { "store" }, method = RequestMethod.GET)
    @ApiOperation(value = "查询存储配置信息", notes = "查询存储配置信息")
    public ApiResult store() {
        return handle(clusterService::storeCfg);
    }

    /**
     * 设备计算接口
     */
    @RequestMapping(value = { "dev/calculate" }, method = RequestMethod.POST)
    @ApiOperation(value = "设备计算接口", notes = "设备计算接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "targets",value = "目标主机（必须已经校验通过）"))
    public ApiResult calculateDev(@RequestBody Set<String> targets) {
        return handle(() -> {
            clusterService.calculateDev(targets);
            return new ArrayList<>();
        });
    }

    /**
     * 查询存储配置信息
     */
    @RequestMapping(value = { "roles_cfg" }, method = RequestMethod.GET)
    @ApiOperation(value = "查询角色配置", notes = "查询角色配置")
    public ApiResult getRoles() {
        return handle(clusterService::roleCfg);
    }

    /**
     * 角色保存接口
     */
    @RequestMapping(value = { "roles" }, method = RequestMethod.POST)
    @ApiOperation(value = "角色保存接口", notes = "角色保存接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "roles",value = "角色信息",dataType = "Map[String,Set[String]]",example = "{\"roles\":[\"192.168.1.1\",\"192.168.1.2\",\"192.168.1.3\"]}"))
    public ApiResult saveRoles(@RequestBody HashMap<String,Set<String>> roles) {
        return handle(() -> {
            clusterService.saveRoles(roles);
            return new ArrayList<>();
        });
    }

    /**
     * 角色查询接口
     */
    @RequestMapping(value = { "roles" }, method = RequestMethod.GET)
    @ApiOperation(value = "角色查询接口", notes = "角色查询接口")
    public ApiResult findRoles() {
        return handle(clusterService::findRoles);
    }

    /**
     * 全局配置保存接口
     */
    @RequestMapping(value = { "global" }, method = RequestMethod.POST)
    @ApiOperation(value = "全局配置保存接口", notes = "全局配置保存接口")
    @ApiImplicitParams(@ApiImplicitParam(name = "global",value = "全局配置参数"))
    public ApiResult saveGlobal(@RequestBody  HashMap<String, String> global) {
        return handle(() -> {
            clusterService.saveGlobal(global, Global.OUTER_SET);
            return new ArrayList<>();
        });
    }

    /**
     * 全局配置查询接口
     */
    @RequestMapping(value = { "global" }, method = RequestMethod.GET)
    @ApiOperation(value = "全局配置查询接口", notes = "全局配置查询接口")
    public ApiResult findGlobal() {
        return handle(clusterService::findGlobal);
    }

    /**
     * 任务初始化接口，服务重启之后会调用任务状态回退，主机锁失效
     */
    @RequestMapping(value = { "exec/reset" })
    @ApiOperation(value = "状态初始化接口", notes = "状态初始化接口")
    public ApiResult reset() {
        return handle(() -> {
            callbackService.reset();
            return new ArrayList<>();
        });
    }

    /**
     * 任务执行接口
     */
    @RequestMapping(value = { "exec/{playCode}" }, method = RequestMethod.POST)
    @ApiOperation(value = "任务执行接口", notes = "任务执行接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "targets",value = "目标主机，如果为空视为所有主机",required = true),
            @ApiImplicitParam(name = "playCode",value = "任务编码")})
    public ApiResult exec(@RequestBody List<String> targets, @PathVariable String playCode) {
        return handle(() -> {
            String taskId = execService.exec(targets,playCode);
            TaskManager.start(taskId);
            return taskId;
        });
    }

    /**
     * 任务继续执行接口
     */
    @RequestMapping(value = { "exec/resume" }, method = RequestMethod.POST)
    @ApiOperation(value = "任务继续执行接口", notes = "任务继续执行接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "uuid",value = "任务ID,通过任务执行接口获取",required = true)})
    public ApiResult resume(@RequestBody String uuid) {
        return handle(() -> {
            execService.resume(uuid);
            TaskManager.start(uuid);
            return new ArrayList<>();
        });
    }

    /**
     * 任务继续执行接口
     */
    @RequestMapping(value = { "exec/pause" }, method = RequestMethod.POST)
    @ApiOperation(value = "任务暂停接口", notes = "任务暂停接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "uuid",value = "任务ID,通过任务执行接口获取",required = true)})
    public ApiResult pause(@RequestBody String uuid) {
        return handle(() -> {
            execService.pause(uuid);
            return new ArrayList<>();
        });
    }

    /**
     * 查询所有的任务列表
     */
    @RequestMapping(value = { "exec/tasks" },method = RequestMethod.GET)
    @ApiOperation(value = "查询所有的任务列表", notes = "查询所有的任务列表")
    public ApiResult tasks() {
        return handle(TaskManager::tasks);
    }

    /**
     * 任务执行查询接口
     */
    @RequestMapping(value = { "exec/task" },method = RequestMethod.GET)
    @ApiOperation(value = "任务ID查询接口", notes = "任务ID查询接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "playCode",value = "任务编码",required = true)})
    public ApiResult task(@RequestParam String playCode) {
        return handle(() -> execService.getLatestUuid(playCode));
    }

    /**
     * 任务执行查询接口
     */
    @RequestMapping(value = { "exec/query" },method = RequestMethod.GET)
    @ApiOperation(value = "任务执行查询接口", notes = "任务执行查询接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "uuid",value = "任务ID,通过任务执行接口获取",required = true)})
    public ApiResult query(@RequestParam String uuid) {
        return handle(() -> execService.query(uuid));
    }

    /**
     * 初始化playbook查询接口
     */
    @RequestMapping(value = { "exec/playbooks" }, method = RequestMethod.GET)
    @ApiOperation(value = "playbook查询接口", notes = "查询指定任务下的playbooks列表")
    @ApiImplicitParams({@ApiImplicitParam(name = "playCode",value = "业务编码",required = true)})
    public ApiResult getPlaybooks(@RequestParam String playCode) {
        return handle(() -> execService.initPlaybooks(playCode));
    }

    /**
     *  用户 执行脚本curl回调主机信息，保存主机的校验状态
     * @param host 主机信息，里面可以有设备信息
     */
    @RequestMapping(value = { "callback/host" }, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存主机信息", notes = "回调保存主机信息")
    public ApiResult callbackHost(@RequestParam String host) {
        return handle(() -> {
            callbackService.saveHost(JSON.parseObject(host,SysClusterHost.class));
            return new ArrayList<>();
        });
    }

    @RequestMapping(value = { "callback/role" }, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存角色信息", notes = "回调保存角色信息")
    public ApiResult callbackRole(@RequestParam String hostRole) {
        return handle(() -> {
            callbackService.saveHostRole(JSON.parseObject(hostRole,SysClusterHostRole.class));
            return new ArrayList<>();
        });
    }

    @RequestMapping(value = { "callback/dev" }, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存设备信息", notes = "回调保存设备信息")
    public ApiResult callbackDev(@RequestParam String roleDev) {
        return handle(() -> {
            callbackService.saveRoleDev(JSON.parseObject(roleDev, SysClusterHostRoleDev.class));
            return new ArrayList<>();
        });
    }

    @RequestMapping(value = { "callback/global" }, method = RequestMethod.POST)
    @ApiOperation(value = "回调保存全局信息", notes = "回调保存全局信息")
    public ApiResult callbackGlobal(@RequestParam String global) {
        return handle(() -> {
            JSONObject json = JSON.parseObject(global);
            HashMap<String,String> map = new HashMap<>();
            if (null!=json){
                for (String key:json.keySet()){
                    map.put(key,json.get(key).toString());
                }
            }
            callbackService.saveGlobal(map);
            return new ArrayList<>();
        });
    }
    
    @RequestMapping(value = { "callback/host" }, method = RequestMethod.DELETE)
    @ApiOperation(value = "回调删除接口", notes = "回调删除接口")
    public ApiResult calldelHost(@RequestBody  String ip) {
        return handle(() -> {
            hostService.deleteHost(ip,true);
            return new ArrayList<>();
        });
    }

    /**
     *  上传文件接口
     * @param template 文件模板数据流
     * @return 解析结果
     */
    @RequestMapping(value={"upload"})
    public ApiResult upload(@RequestParam("template") MultipartFile template) {
        return handle(() -> {
            hostService.saveTemplate(template.getInputStream());
            return new ArrayList<>();
        });
    }
}
