package com.bonc.bdos.service.cluster.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;



@Entity
@Table(name = "`sys_cluster_store_cfg`")
public class SysClusterStoreCfg implements Serializable {

	private static final long serialVersionUID = 2299808939612775242L;

	@Id
	@Column(name = "`name`",length = 32)
	private String name;

	@Column(name = "`role_code`",length = 32)
	private String roleCode;

	@Column(name = "`store_type`")
	private char storeType;

	@Column(name = "`min_size`")
	private int minSize;
	
	@Column(name = "`max_size`")
	private int maxSize;

	@Column(name = "`extend`")
	private int extend;
	
	@Column(name = "`level`")
	private char level;

	public String getRoleCode() {
		return roleCode;
	}

	public void setRoleCode(String roleCode) {
		this.roleCode = roleCode;
	}

	public char getStoreType() {
		return storeType;
	}

	public void setStoreType(char storeType) {
		this.storeType = storeType;
	}

	public int getMinSize() {
		return minSize;
	}

	public void setMinSize(int minSize) {
		this.minSize = minSize;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	public int getExtend() {
		return extend;
	}

	public void setExtend(int extend) {
		this.extend = extend;
	}

	public char getLevel() {
		return level;
	}

	public void setLevel(char level) {
		this.level = level;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "SysClusterStoreCfg [  roleCode=" + roleCode + ", storeType=" + storeType + ", minSize="
				+ minSize + ", maxSize=" + maxSize + ", extend=" + extend + ", level=" + level + ", name=" + name + "]";
	}
	
}
