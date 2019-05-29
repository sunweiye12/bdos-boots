package com.bonc.bdos.service.model;

import com.bonc.bdos.service.entity.SysInstallPlayExec;
import lombok.Data;

import java.util.List;

@Data
public  class ExecProcess{
    private char status;
    private String stdout;
    private Integer present;
    private Integer size;
    private String msg;
    private List<String> targetIps;

    public ExecProcess(SysInstallPlayExec exec) {
        // 设置执行状态
        this.status = exec.getStatus();
        this.targetIps = exec.getTargetIps();
        this.stdout = exec.getStdout();
        this.msg = exec.getMessage();
        this.size = exec.getCurIndex();
        this.present = (exec.getPercent()+size*100)*100/(100*exec.getPlaybooks().size());
    }
}