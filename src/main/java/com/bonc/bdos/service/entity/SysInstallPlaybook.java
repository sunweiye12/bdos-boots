package com.bonc.bdos.service.entity;

import com.bonc.bdos.api.v1.ClusterController;
import com.bonc.bdos.consts.ReturnCode;
import com.bonc.bdos.service.Global;
import com.bonc.bdos.service.exception.ClusterException;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

@Entity
@Table(name = "`sys_install_playbook`")
@Data
public class SysInstallPlaybook implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterController.class);

    private static final long serialVersionUID = -2102082194607883083L;

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

    public void addRoles(SysInstallHostControl role) {
        if (null != role && !StringUtils.isEmpty(role.getRoleCode())) {
            roles.put(role.getRoleCode(), role.getHosts());
        }
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

        String hostPath = Global.getWorkDir() +File.separator +"hosts"+ File.separator + "hosts" + File.separator;
        File dir = new File(hostPath);
        if(!dir.exists()&&!dir.mkdirs()){
            LOG.error("主机目录不存在且创建失败！");
            throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_DIR_NOT_EXISTED,  new ArrayList<>(),"主机host文件目录不存在，且无法创建");
        }
        String invFilePath = hostPath + taskName+ "-" + playbook + ".host";
        FileWriter invWriter = new FileWriter(invFilePath, false);
        invWriter.write(buffer.toString());
        invWriter.close();

        return invFilePath;
    }

}
