package com.bonc.bdos.service.cluster.entity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "`sys_install_playbook`")
public class SysInstallPlaybook implements Serializable {

    private static final long serialVersionUID = -2102082194607883083L;

    private static final String HOST_PATH = SysInstallPlaybook.class.getResource("/").getPath() + File.separator + "hosts" + File.separator;

    @Id
    @Column(name = "`id`")
    private Long id;

    @Column(name = "`play_code`", length = 32)
    private String playCode;

    @Column(name = "`playbook`", length = 32)
    private String playbook;

    @Column(name = "`playbook_name`", length = 32)
    private String playbookName;

    /**
     * 同一个play 下面的playbook 的index 从 1 一直持续增长
     */
    @Column(name = "`index`")
    private int index;

    /**
     * 构造标识：默认是true true : 对应playbook 的数据可以构造成功 false : 对应playbook 的数据构造失败
     */
    @Transient
    private boolean flag = true;

    /**
     * 这个playbook 包含多组控制机，按roleCode分组
     */
    @Transient
    private HashMap<String, HashMap<String, SysClusterHost>> roles = new HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlayCode() {
        return playCode;
    }

    public void setPlayCode(String playCode) {
        this.playCode = playCode;
    }

    public String getPlaybook() {
        return playbook;
    }

    public void setPlaybook(String playbook) {
        this.playbook = playbook;
    }

    public String getPlaybookName() {
        return playbookName;
    }

    public void setPlaybookName(String playbookName) {
        this.playbookName = playbookName;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void addRoles(SysInstallHostControl role) {
        if (null != role && !StringUtils.isEmpty(role.getRoleCode())) {
            roles.put(role.getRoleCode(), role.getHosts());
        }
    }

    public boolean getFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public HashMap<String, HashMap<String, SysClusterHost>> getRoles() {
        return roles;
    }

    public HashMap<String, SysClusterHost> getRoles(String roleCode) {
        return roles.get(roleCode);
    }

    public String initPlaybookInv(String taskName) throws IOException {
        StringBuffer buffer = new StringBuffer();
        for (String roleName : getRoles().keySet()) {
            buffer.append("[").append(roleName).append("]").append("\n");
            for (String ip : getRoles().get(roleName).keySet()) {
                getRoles().get(roleName).get(ip).initHostInventory(buffer);
            }
            buffer.append("\n");
        }

        String invFilePath = HOST_PATH + taskName+ "-" + playbook + ".host";
        FileWriter invWriter = new FileWriter(invFilePath, false);
        invWriter.write(buffer.toString());
        invWriter.close();

        return invFilePath;
    }

    /*
     * public int getWeight() { return weight; } public void setWeight(int
     * weight) { this.weight = weight; }
     */

    @Override
    public String toString() {
        return "SysInstallPlaybook [id=" + id + ", playCode=" + playCode + ", playBook=" + playbook + ", index=" + index
                + "]";
    }
}
