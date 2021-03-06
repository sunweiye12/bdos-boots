package com.bonc.bdos.sys;

import com.bonc.bdos.service.Global;
import com.bonc.bdos.service.repository.SysInstallLogLabelRepository;
import com.bonc.bdos.service.repository.SysInstallPlaybookRepository;
import com.bonc.bdos.service.service.CallService;
import com.bonc.bdos.service.service.ExecService;
import com.bonc.bdos.service.tasks.CmdExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/*
 * @desc:应用数据初始化
 * @author:
 * @time:
 */

@Component
public class AppInit implements CommandLineRunner{

    private final Global global;
    private final CallService callbackService;
    private final ExecService execService;
    private final SysInstallLogLabelRepository labelDao;
    private final SysInstallPlaybookRepository playbookDao;

    @Autowired
    public AppInit(Global global, CallService callbackService, ExecService execService, SysInstallLogLabelRepository labelDao, SysInstallPlaybookRepository playbookDao) {
        this.global = global;

        this.callbackService = callbackService;
        this.execService = execService;

        this.labelDao = labelDao;

        this.playbookDao = playbookDao;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>>>>>>>>>>>>>>服务启动执行，执行加载数据等操作<<<<<<<<<<<<<");
        // 缓存全局参数
        global.init();

        // 释放任务资源
        execService.reset();

        // 初始化任务标签信息
        CmdExecutor.init(callbackService,labelDao.findAll(),playbookDao.findAll());
    }

}
