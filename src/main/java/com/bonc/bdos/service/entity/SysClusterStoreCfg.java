package com.bonc.bdos.service.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;



@Entity
@Table(name = "`sys_cluster_store_cfg`")
@Data
public class SysClusterStoreCfg implements Serializable {

	private static final long serialVersionUID = 2299808939612775242L;

	/**
	 *  TYPE_VG: 表示 存储配置可以来自多块存储盘，最终合并成为一个VG
	 *  TYPE_PART: 表示存储将形成一个独立的磁盘，不可有多块盘合并而成
	 */
	public static final char TYPE_VG='1';
	public static final char TYPE_PART='2';

	@Id
	@Column(name = "`id`")
	private Integer id;

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
}
