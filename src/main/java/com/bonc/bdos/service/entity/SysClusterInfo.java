package com.bonc.bdos.service.entity;

import com.bonc.bdos.service.Global;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.HashMap;

@Entity
@Table(name = "`sys_cluster_info`")
@Data
public class SysClusterInfo implements Serializable{

	private static final long serialVersionUID = 1819099555348375258L;

	private static final HashMap<Character,String> TYPE_DESC = new HashMap<Character, String>(){{
		put(Global.READ_ONLY , "配置不可以修改");
		put(Global.INNER_SET, "配置只能内部修改");
		put(Global.OUTER_SET, "配置只能外部修改");
	}};

	@Id
	@Column(name = "`cfg_key`" ,length = 64)
	private String cfgKey;

	@Column(name = "`cfg_value`")
	private String cfgValue;

	@Column(name = "`cfg_type`")
	private char cfgType;

	@Column(name = "`memo`")
	private String memo;

	public void setCfgValue(String cfgValue) {
		this.cfgValue = cfgValue;
		Global.updateGlobal(this);
	}

	public boolean isEnable(char type) {
		return this.cfgType == type;
	}

	public String getTypeDesc(){
		return getCfgKey() + TYPE_DESC.getOrDefault(this.cfgType, "未知的配置类型") ;
	}
}
