package com.bonc.bdos.service.cluster.model;

import com.bonc.bdos.service.cluster.entity.SysInstallPlayExec;
import com.bonc.bdos.service.cluster.entity.SysInstallPlaybook;

import java.util.List;

public  class ExecProcess{
    private char status;
    private String stdout;
    private Integer present;
    private Integer size;
    private String msg;
    private List<String> targetIps;
    
    public char getStatus() {
        return status;
    }

    public String getStdout() {
        return stdout;
    }

    public Integer getPresent() {
        return present;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public String getMsg() {
        return msg;
    }

    public List<String> getTargetIps() {
        return targetIps;
    }

    public ExecProcess(SysInstallPlayExec exec) {
        // 设置执行状态
        this.status = exec.getStatus();
        this.size = exec.getCurIndex();
        this.targetIps = exec.getTargetIps();
        this.stdout = exec.getStdout();
        this.msg = exec.getMessage();
        int size=0;
        // 获取已经完成的任务数量
        for(SysInstallPlaybook playbook:exec.getPlaybooks()){
            if (playbook.getIndex()<exec.getCurIndex()){
                size++;
            }
        }
        this.size = size;
        this.present = (exec.getPercent()+size*100)*100/(100*exec.getPlaybooks().size());
    }
}