package com.bonc.bdos.service.cluster.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "`sys_cluster_role`")
public class SysClusterRole implements Serializable {

	private static final long serialVersionUID = -2599707778227199288L;
	public  static final String DEFAULT_ROLE = "default";


	@Id
	@Column(name = "`role_code`", length = 32)
	private String roleCode;

	@Column(name = "`role_desc`")
	private String roleDesc;

	/**
	 *   0： 每个机器上都有的主机
	 *   1： 界面展示的角色
	 *   2： 界面不展示的角色
	 */
	@Column(name = "`role_type`")
	private char roleType;

	public String getRoleCode() {
		return roleCode;
	}

	public void setRoleCode(String roleCode) {
		this.roleCode = roleCode;
	}

	public String getRoleDesc() {
		return roleDesc;
	}

	public void setRoleDesc(String roleDesc) {
		this.roleDesc = roleDesc;
	}

	public char getRoleType() {
		return roleType;
	}

	public void setRoleType(char roleType) {
		this.roleType = roleType;
	}

	@Override
	public String toString() {
		return "SysClusterRole [roleCode=" + roleCode + ", roleDesc=" + roleDesc + ", roleType=" + roleType + "]";
	}
	
}
