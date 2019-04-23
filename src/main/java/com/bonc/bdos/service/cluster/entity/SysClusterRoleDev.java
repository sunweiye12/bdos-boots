package com.bonc.bdos.service.cluster.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.bonc.bdos.service.component.util.DateUtil;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;

@Entity
@Table(name = "`sys_cluster_role_dev`")
@Cacheable(false)
public class SysClusterRoleDev implements Serializable {

	private static final long serialVersionUID = 2299808939612775242L;

	private static final char UN_USED = '0';
	private static final char DISABLED = '1';
	private static final char IN_USED = '2';

	private static HashMap<Character,String> statusDesc = new HashMap<Character,String>(){{
		put(UN_USED,"未使用");
		put(DISABLED,"已禁用");
		put(IN_USED,"已使用");
	}};

	public SysClusterRoleDev() {}

	public SysClusterRoleDev(SysClusterHostRole hostRole, String devName,String partType,int devSize,String vgName) {
		this.hostRoleId = hostRole.getId();
		this.devName = devName;
		this.devSize = devSize;
		this.name = vgName;
		this.ip = hostRole.getIp();
		this.status = SysClusterRoleDev.UN_USED;
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getHostRoleId() {
		return hostRoleId;
	}

	public void setHostRoleId(String hostRoleId) {
		this.hostRoleId = hostRoleId;
	}

	public String getDevName() {
		return devName;
	}

	public void setDevName(String devName) {
		this.devName = devName;
	}

	public int getDevSize() {
		return devSize;
	}

	public void setDevSize(int devSize) {
		this.devSize = devSize;
	}

	public int getDevSizeUsed() {
		return devSizeUsed;
	}

	public void setDevSizeUsed(int devSizeUsed) {
		this.devSizeUsed = devSizeUsed;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

	public Timestamp getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Timestamp updateDate) {
		this.updateDate = updateDate;
	}

	public char getStatus() {
		return status;
	}

	public void setStatus(char status) {
		this.status = status;
	}
	
	public String getPartType() {
        return partType;
    }

    public void setPartType(String partType) {
        this.partType = partType;
    }

	public boolean isEnable(){
		return SysClusterRoleDev.DISABLED != this.status;
	}

	public boolean isUsed(){
		return SysClusterRoleDev.IN_USED == this.status;
	}

	public int getEnableSpace(){
		return this.devSize - this.devSizeUsed;
	}

	public int accessAlloc( int size){
		int allocSpace = getEnableSpace() >= size ? size:getEnableSpace();
		setDevSizeUsed(getDevSizeUsed()+allocSpace);
		return allocSpace;
	}

	@Override
    public String toString() {
        return "SysClusterRoleDev [id=" + id + ", ip=" + ip + ", hostRoleId=" + hostRoleId + ", devName=" + devName
                + ", devSize=" + devSize + ", devSizeUsed=" + devSizeUsed + ", name=" + name + ", partType=" + partType
                + ", createDate=" + createDate + ", updateDate=" + updateDate + ", status=" + status + "]";
    }

    public void enable() {
		this.status = UN_USED;
	}

	public void disable() {
		this.status = DISABLED;
	}

	public String getStatusDesc() {
		if (SysClusterRoleDev.statusDesc.containsKey(this.status)) {
			return this.devName+SysClusterRoleDev.statusDesc.get(this.status);
		}
		return this.devName+"未知的设备状态";
	}
}
