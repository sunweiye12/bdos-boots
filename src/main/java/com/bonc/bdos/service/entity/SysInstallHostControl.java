package com.bonc.bdos.service.entity;

import com.bonc.bdos.utils.StringUtils;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;

@Entity
@Table(name = "`sys_install_host_control`")
@Data
public class SysInstallHostControl implements Serializable {

	private static final long serialVersionUID = -2102082194607883083L;
	
	@Id
	@Column(name = "`id`")
	private Long id;
	
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


	public void addHost(SysClusterHost host) {
		if (null!=host && !StringUtils.isEmpty(host.getIp())) {	hosts.put(host.getIp(),host);}
	}

	public void clearHosts(){
		this.hosts.clear();
	}

	String getConstructMsg() {
		return "状态为："+getStatus()+" 的"+getRoleCode()+"主机列表"+hosts.keySet().toString()+"不满足执行条件！";
	}
}
