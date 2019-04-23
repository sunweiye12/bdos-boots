package com.bonc.bdos.service.cluster.entity;

import com.bonc.bdos.utils.StringUtils;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;

@Entity
@Table(name = "`sys_install_host_control`")
public class SysInstallHostControl implements Serializable {


	private static final long serialVersionUID = -2102082194607883083L;
	
	@Id
	@GenericGenerator(name = "uuidGenerator", strategy = "uuid")
	@GeneratedValue(generator = "uuidGenerator")
	private String id;
	
	@Column(name = "`playbook_id`",length = 32)
	private Long playbookId;

	@Column(name = "`role_code`",length = 32)
	private String roleCode;
	
	@Column(name = "`status`")
	private char status;

	@Column(name = "`target_enable`")
	private Boolean targetEnable;
	
	@Column(name = "`control`",length = 32)
	private String control;

	/**
	 *  hostControl 主机组装标识，默认是true，如果主机组装失败则设置未false
	 */
	@Transient
	private Boolean flag = true;

	/**
	 *  控制主机组，如果主机控制数据能够组装成功，则hosts存储对应的控制主机信息
	 */
	@Transient
    private HashMap<String,SysClusterHost> hosts = new HashMap<>();

	public Long getPlaybookId() {
		return playbookId;
	}

	public void setPlaybookId(Long playbookId) {
		this.playbookId = playbookId;
	}

	public String getRoleCode() {
		return roleCode;
	}

	public void setRoleCode(String roleCode) {
		this.roleCode = roleCode;
	}

	public char getStatus() {
		return status;
	}

	public void setStatus(char status) {
		this.status = status;
	}

	public Boolean getTargetEnable() {
		return targetEnable;
	}

	public void setTargetEnable(Boolean targetEnable) {
		this.targetEnable = targetEnable;
	}

	public String getControl() {
		return control;
	}

	public void setControl(String control) {
		this.control = control;
	}

	public Boolean getFlag() {
		return flag;
	}

	public void setFlag(Boolean flag) {
		this.flag = flag;
	}

    public HashMap<String, SysClusterHost> getHosts() {
		return hosts;
	}

	public void addHost(SysClusterHost host) {
		if (null!=host && !StringUtils.isEmpty(host.getIp())) {	hosts.put(host.getIp(),host);}
	}

	public void clearHosts(){
		this.hosts.clear();
	}
	
	public String getId() {
	    return id;
	}

	public void setId(String id) {
	    this.id = id;
	}
	

	@Override
	public String toString() {
	    return "SysInstallHostControl [id=" + id + ", playbookId=" + playbookId + ", RoleCode=" + roleCode + ", status=" + status + ", targetEnable=" + targetEnable + ", control=" + control + "]";
	}

	String getConstructMsg() {
		return "状态为："+getStatus()+" 的"+getRoleCode()+"主机列表"+hosts.keySet().toString()+"不满足执行条件！";
	}
}
