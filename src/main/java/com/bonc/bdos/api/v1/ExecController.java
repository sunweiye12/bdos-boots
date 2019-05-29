package com.bonc.bdos.api.v1;

import com.bonc.bdos.common.ApiHandle;
import com.bonc.bdos.common.ApiResult;
import com.bonc.bdos.service.service.ExecService;
import com.bonc.bdos.service.tasks.TaskManager;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/exec")
public class ExecController {
    private static final Logger LOG = LoggerFactory.getLogger(ExecController.class);

    private final ExecService execService;

    @Autowired
    public ExecController(ExecService execService) {
        this.execService = execService;
    }


    /**
     * 任务初始化接口，服务重启之后会调用任务状态回退，主机锁失效
     */
    @RequestMapping(value = { "/reset" },method = RequestMethod.GET)
    @ApiOperation(value = "状态初始化接口", notes = "状态初始化接口")
    public ApiResult reset() {
        return ApiHandle.handle(() -> {
            execService.reset();
            return new ArrayList<>();
        },LOG);
    }

    /**
     * 任务执行接口
     */
    @RequestMapping(value = { "/{playCode}" }, method = RequestMethod.POST)
    @ApiOperation(value = "任务执行接口", notes = "任务执行接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "targets",value = "目标主机，如果为空视为所有主机",required = true),
            @ApiImplicitParam(name = "playCode",value = "任务编码")})
    public ApiResult exec(@RequestBody List<String> targets, @PathVariable String playCode) {
        return ApiHandle.handle(() -> {
            String taskId = execService.exec(targets,playCode);
            TaskManager.start(taskId);
            return taskId;
        },LOG);
    }

    /**
     * 任务继续执行接口
     */
    @RequestMapping(value = { "/resume" }, method = RequestMethod.POST)
    @ApiOperation(value = "任务继续执行接口", notes = "任务继续执行接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "uuid",value = "任务ID,通过任务执行接口获取",required = true)})
    public ApiResult resume(@RequestBody String uuid) {
        return ApiHandle.handle(() -> {
            execService.resume(uuid);
            TaskManager.start(uuid);
            return new ArrayList<>();
        },LOG);
    }

    /**
     * 任务继续执行接口
     */
    @RequestMapping(value = { "/pause" }, method = RequestMethod.POST)
    @ApiOperation(value = "任务暂停接口", notes = "任务暂停接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "uuid",value = "任务ID,通过任务执行接口获取",required = true)})
    public ApiResult pause(@RequestBody String uuid) {
        return ApiHandle.handle(() -> {
            execService.pause(uuid);
            return new ArrayList<>();
        },LOG);
    }

    /**
     * 查询所有的任务列表
     */
    @RequestMapping(value = { "/tasks" },method = RequestMethod.GET)
    @ApiOperation(value = "查询所有的任务列表", notes = "查询所有的任务列表")
    public ApiResult tasks() {
        return ApiHandle.handle(TaskManager::tasks,LOG);
    }

    /**
     * 任务执行查询接口
     */
    @RequestMapping(value = { "/task" },method = RequestMethod.GET)
    @ApiOperation(value = "任务ID查询接口", notes = "任务ID查询接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "playCode",value = "任务编码",required = true)})
    public ApiResult task(@RequestParam String playCode) {
        return ApiHandle.handle(() -> execService.getLatestUuid(playCode),LOG);
    }

    /**
     * 任务执行查询接口
     */
    @RequestMapping(value = { "/query" },method = RequestMethod.GET)
    @ApiOperation(value = "任务执行查询接口", notes = "任务执行查询接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "uuid",value = "任务ID,通过任务执行接口获取",required = true)})
    public ApiResult query(@RequestParam String uuid) {
        return ApiHandle.handle(() -> execService.query(uuid),LOG);
    }

    /**
     * 初始化playbook查询接口
     */
    @RequestMapping(value = { "/playbooks" }, method = RequestMethod.GET)
    @ApiOperation(value = "playbook查询接口", notes = "查询指定任务下的playbooks列表")
    @ApiImplicitParams({@ApiImplicitParam(name = "playCode",value = "业务编码",required = true)})
    public ApiResult getPlaybooks(@RequestParam String playCode) {
        return ApiHandle.handle(() -> execService.initPlaybooks(playCode),LOG);
    }

}
