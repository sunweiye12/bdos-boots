package com.bonc.bdos.service.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "`sys_install_play`")
@Data
public class SysInstallPlay implements Serializable {

	private static final long serialVersionUID = -2102082194607883083L;

	@Id
	@Column(name = "`play_code`",length = 32)
	private String playCode;

	@Column(name = "`play_name`",length = 32)
	private String playName;

	@Column(name = "`play_desc`")
	private String playDesc;

	/**
	 * 锁类型： 目标play锁主机的范围控制为  全量主机和增量主机两种
	 *
	 * true: 全量主机： 全量主机在验证锁的时候会去校验集群全部的主机的锁状态，并且会锁住目标主机
	 * false: 增量主机： 增量主机锁，会校验targets 参数中目标机器的锁状态， 仅仅锁住目标主机
	 */
	@Column(name = "`lock_type`")
	private Boolean lockType;

	/**
	 *  可用状态，true 可以被外部调用 false 不可以被外部调用
	 */
	@Column(name = "`status`")
	private Boolean status;

}
