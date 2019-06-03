package com.bonc.bdos.service.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.bonc.bdos.utils.DateUtil;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;

@Entity
@Table(name = "`sys_cluster_host_role_dev`")
@Data
public class SysClusterHostRoleDev implements Serializable {

	private static final long serialVersionUID = 2299808939612775242L;

	private static final char UN_USED = '0';
	private static final char DISABLED = '1';
	private static final char IN_USED = '2';

	private static HashMap<Character,String> statusDesc = new HashMap<Character,String>(){{
		put(UN_USED,"未使用");
		put(DISABLED,"已禁用");
		put(IN_USED,"已使用");
	}};

	public SysClusterHostRoleDev() {}

	public SysClusterHostRoleDev(SysClusterHostRole hostRole, String devName, String partType, int devSize, String vgName) {
		this.hostRoleId = hostRole.getId();
		this.devName = devName;
		this.devSize = devSize;
		this.name = vgName;
		this.ip = hostRole.getIp();
		this.status = SysClusterHostRoleDev.UN_USED;
		this.createDate = new Timestamp(DateUtil.getCurrentTimeMillis());
		this.updateDate = this.createDate;
		this.devSizeUsed = 0;
		this.partType = partType;
	}

	@Id
	@GenericGenerator(name = "uuidGenerator", strategy = "uuid")
	@GeneratedValue(generator = "uuidGenerator")
	private String id;

	@Column(name = "`ip`",length = 15)
	private String ip;

	@Column(name = "`host_role_id`", length = 32)
	private String hostRoleId;

	@Column(name = "`dev_name`",length = 10)
	private String devName;
	
	@Column(name = "`dev_size`")
	private int devSize;
	
	@Column(name = "`dev_size_used`")
	private int devSizeUsed;
	
	@Column(name = "`name`",length = 32)
	private String name;
	
	@Column(name = "`part_type`",length = 32)
    private String partType;
	
    @Column(name = "`create_date`")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Timestamp createDate;

	@Column(name = "`update_date`")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Timestamp updateDate;

	/**
	 * 设备盘的状态
	 * 0: 初始状态 未使用
	 * 1: 禁用设备  用于default角色的使用控制，其他角色没有这个状态，只有0状态才能禁用
	 * 2: 已使用
	 */
	@Column(name = "`status`")
	private char status;

	public boolean isEnable(){
		return SysClusterHostRoleDev.DISABLED != this.status;
	}

	public boolean isUsed(){
		return SysClusterHostRoleDev.IN_USED == this.status;
	}

	/**
	 *  从当前设备中分配一块size 大小空间 ,返回能分到的空间
	 * @param size 需要分配空间大小
	 * @return  实际分配空间大小
	 */
	public int accessAlloc( int size){
		int allocSpace = getEnableSize() >= size ? size:getEnableSize();
		setDevSizeUsed(getDevSizeUsed()+allocSpace);
		return allocSpace;
	}

    public void enable() {
		this.status = UN_USED;
	}

	public void disable() {
		this.status = DISABLED;
	}

	public String getStatusDesc() {
		if (SysClusterHostRoleDev.statusDesc.containsKey(this.status)) {
			return this.devName+ SysClusterHostRoleDev.statusDesc.get(this.status);
		}
		return this.devName+"未知的设备状态";
	}

	public int getEnableSize() {
		return devSize-devSizeUsed;
	}
}
