package com.bonc.bdos.service.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.bonc.bdos.utils.DateUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ApiModel(value = "主机信息")
@Entity
@Table(name = "`sys_cluster_host`")
@Data
public class SysClusterHost implements Serializable,Cloneable {

	public static final char NO_CHECK = '0';
	public static final char ERROR = '1';
	public static final char SUCCESS = '2';

	private static final HashMap<Character,String> STATUS_DESC = new HashMap<Character, String>(){{
		put('0',"未校验");
		put('1',"校验失败");
		put('2',"校验成功");
	}};

	public SysClusterHost() {
		this.status = SysClusterHost.NO_CHECK;
		this.sshPort = 22;
		this.createDate = new Timestamp(DateUtil.getCurrentTimeMillis());
		this.updateDate = createDate;
		this.hostLock = false;
	}

	public SysClusterHost(HashMap<String, Object> reqhost) {
        this.status = SysClusterHost.NO_CHECK;
        this.sshPort = 22;
        this.createDate = new Timestamp(DateUtil.getCurrentTimeMillis());
        this.updateDate = createDate;
        this.hostLock = false;
        this.ip = String.valueOf(reqhost.get("ip"));
        this.username = String.valueOf(reqhost.get("username"));
        this.password = String.valueOf(reqhost.get("password"));
    }
	/**
	 *
	 */
	private static final long serialVersionUID = 3053015296950705164L;

	@Id
	@Column(name = "`ip`", length = 15 )
	@ApiModelProperty(value = "ip", required = true, example = "192.168.1.1", dataType = "String")
    @Pattern(regexp="(?=(\\b|\\D))(((\\d{1,2})|(1\\d{1,2})|(2[0-4]\\d)|(25[0-5]))\\.){3}((\\d{1,2})|(1\\d{1,2})|(2[0-4]\\d)|(25[0-5]))(?=(\\b|\\D))")
    private String ip;

	@Column(name = "`hostname`", length = 32)
	@ApiModelProperty(value = "hostname", example = "", dataType = "String")
	private String hostname;

	@Column(name = "`ssh_port`")
	@ApiModelProperty(value = "sshPort", required = true, example = "22", dataType = "int")
    @Max(value = 65536)
	private Integer sshPort;

	@Column(name = "`memory`")
	@ApiModelProperty(value = "memory", example = "32", dataType = "int")
	private Integer memory;

	@Column(name = "`cpu`")
	@ApiModelProperty(value = "cpu", example = "8", dataType = "int")
	private Integer cpu;

	@Column(name = "`username`",length = 32)
	@ApiModelProperty(value = "username", required = true, example = "root", dataType = "String")
	private String username;

	@Column(name = "`password`", length = 32)
	@ApiModelProperty(value = "password", required = true, example = "root", dataType = "String")
	private String password;

	@Column(name = "`user`", length = 32)
	@ApiModelProperty(value = "user", required = true, example = "高阳", dataType = "String")
	private String user;

	@Column(name = "`phone`", length = 32)
	@ApiModelProperty(value = "phone", required = true, example = "18611754986", dataType = "String")
	private String phone;

    @Column(name = "`create_date`")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(hidden = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createDate;

	@Column(name = "`update_date`")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(hidden = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp updateDate;

	@Column(name = "`message`")
	@ApiModelProperty(hidden = true)
	private String message;

	@Transient
    @ApiModelProperty(hidden = true)
	private String roleId;

	/**
	 * 主要维护主机的状态
	 *
	 * 0: 主机要加入进群，但是还没验证，初始状态 1: 校验成功 2: 校验失败
	 */
	@Column(name = "`status`")
    @ApiModelProperty(hidden = true)
	private Character status;

	/**
	 * 主机锁 false 未锁状态，true 已锁状态
	 */
	@Column(name = "`host_lock`")
    @ApiModelProperty(hidden = true)
	private Boolean hostLock;

	@Transient
    @ApiModelProperty(hidden = true)
	private List<SysClusterHostRoleDev> devs = new ArrayList<>();

	@Transient
    @ApiModelProperty(hidden = true)
	private HashMap<String,SysClusterHostRole> roles = new HashMap<>();

	public HashMap<String, SysClusterHostRole> getRoles() {
		return roles;
	}

	public void addRole(SysClusterHostRole hostRole) {
		if (hostRole!=null&& !StringUtils.isEmpty(hostRole.getRoleCode())) {
			this.roles.put(hostRole.getRoleCode(),hostRole);
		}
	}

	@Override
	public SysClusterHost clone() throws CloneNotSupportedException {
        return   (SysClusterHost) super.clone();
	}

    @ApiModelProperty(hidden = true)
	public String getStatusDesc(){
		if (SysClusterHost.STATUS_DESC.containsKey(this.status)) {
			return this.ip+SysClusterHost.STATUS_DESC.get(status);
		}
		return this.ip+"未知的主机状态";
	}

	public void initHostInventory(StringBuffer inv){
		inv.append(ip).append(" ansible_ssh_port=").append(sshPort).append(" ansible_ssh_user=").append(username).
				append(" ansible_ssh_pass='").append(password).append("' ansible_sudo_pass='").append(password).
				append("' this_hostname=").append(ip.replace(".","-")).append(" this_ip=").append(ip).append("\n");
	}

	public boolean check() {
		return this.status == SysClusterHost.SUCCESS;
	}

}
