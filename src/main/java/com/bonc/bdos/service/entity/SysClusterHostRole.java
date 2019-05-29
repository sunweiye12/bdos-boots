package com.bonc.bdos.service.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.bonc.bdos.utils.DateUtil;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Entity
@Table(name = "`sys_cluster_host_role`")
@Data
public class SysClusterHostRole implements Serializable {

	private static final long serialVersionUID = 3627389863587969194L;
	public static final char UNINSTALL = '0';
	public static final char ALLOCATED = '1';
	public static final char INSTALLED = '2';

	private static HashMap<Character,String> statusDesc = new HashMap<Character,String>(){{
		put('0',"未安装");
		put('1',"已分配存储");
		put('2',"已安装");
	}};

	public SysClusterHostRole() {}

	public SysClusterHostRole(String ip,String roleCode){
		this.ip = ip;
		this.roleCode = roleCode;
		this.createDate = new Timestamp(DateUtil.getCurrentTimeMillis());
		this.updateDate = this.createDate;
		this.status = SysClusterHostRole.UNINSTALL;
	}

	@Id
	@GenericGenerator(name = "uuidGenerator", strategy = "uuid")
	@GeneratedValue(generator = "uuidGenerator")
	private String id;

	@Column(name = "`role_code`", length = 32)
	private String roleCode;

	@Column(name = "`ip`", length = 15)
	private String ip;
	
	@Column(name = "`hostname`", length = 32)
    private String hostname;

	@Column(name = "`create_date`")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Timestamp createDate;

	@Column(name = "`update_date`")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Timestamp updateDate;

	/**
	 * 主机角色状态 主要控制角色在主机上的生命状态
	 * 0: 标识主机上要进行某角色的安装
	 * 1: 标识主机已经完成存储分配，只有完成存储分配的角色才能进行安装
	 * 2: 安装成功
	 * 
	 * default 这个默认的角色的状态编码单独设计,他是一个特殊的角色,主要用于管理主机上面的原始磁盘   
	 * default 状态与主机状态对应，保持一致
	 * 0: 未进行主机校验
	 * 1: 校验失败
	 * 2: 校验通过
	 */
	@Column(name = "`status`")
	private char status;

	@Transient
	private List<SysClusterHostRoleDev> devs = new ArrayList<>();

    public boolean isInstalled() {
		return this.status == SysClusterHostRole.INSTALLED;
	}

	public String getStatusDesc(){
		if (SysClusterHostRole.statusDesc.containsKey(this.status)) {
			return this.ip + SysClusterHostRole.statusDesc.get(this.status) + this.roleCode;
		}
		return this.ip + "未知的角色状态" +this.roleCode;
	}
}
