package com.bonc.bdos.service.cluster.tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.bonc.bdos.service.cluster.entity.SysInstallPlayExec;

public class TaskManager {

    private static Map<String, CmdExecutor> taskMap = Collections.synchronizedMap(new HashMap<>());

    public static void create(SysInstallPlayExec exec) {
        // 创建任务执行器
        CmdExecutor ce = new CmdExecutor(exec);

        // 将任务添加到全局任务表里面
        taskMap.put(exec.getUuid(),ce);
    }

    public static void start(String uuid) {
        if (taskMap.containsKey(uuid)){
            taskMap.get(uuid).start();
        }
    }

    /**
     *  删除一个任务，从内存里面删除任务
     * @param uuid 任务ID
     */
    public static void destroy(String uuid){
        if(taskMap.containsKey(uuid)){
            CmdExecutor cm = taskMap.get(uuid);
            cm.destroyTask();
            taskMap.remove(uuid);
        }
    }


    public static SysInstallPlayExec get(String uuid) {
        if (taskMap.containsKey(uuid)){
            return taskMap.get(uuid).getTask();
        }
        return null;
    }

    public static void remove(String uuid) {
        taskMap.remove(uuid);
    }
}
