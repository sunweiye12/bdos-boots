package com.bonc.bdos.service.cluster.model;

import java.util.HashMap;
import java.util.Set;

public class ClusterModel {

	/**
	 *  存储集群全局配置信息
	 */
	private HashMap<String, String> global;
	
	/**
	 * 存储某个角色安装在集群的那些主机上面
	 *  roleCode->ipList
	 */
	private HashMap<String, Set<String>> roles;
	
	
	public HashMap<String, String> getGlobal() {
		return global;
	}

	public void setGlobal(HashMap<String, String> global) {
		this.global = global;
	}

	public HashMap<String, Set<String>> getRoles() {
		return roles;
	}

	public void setRoles(HashMap<String, Set<String>> roles) {
		this.roles = roles;
	}

}
