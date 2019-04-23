package com.bonc.bdos.service.cluster.service;

import com.bonc.bdos.service.cluster.entity.*;

import java.util.HashMap;
import java.util.Map;

public interface CallbackService {

	/**
	 * 逻辑：
	 * 1、判断该任务是否存在
	 * 2、判断参数中的token是否与表数据中的相同
	 * 3、判断当前的任务的执行状态
	 * 4、更新任务执行状态和时间
	 * 
	 * 主要更新开始时间 和执行状态为执行中
	 * 
	 * 更新状态 未开始执行 为 执行中 0->1
	 * 更新开始时间为NOW()
	 */
	void start(SysInstallPlayExec exec) ;
	
/*	*
	 * 逻辑：
	 * 1.判断该执行状态是否存在     当前任务是否存在    当前token是否正确      当前任务是否在执行中
	 * 2.判断log是否插入过，有则更新，否则插入
	 * 
	 * playbook 开始执行、执行中、执行完成
	 *
	 * token 校验
	 *
	 * 开始执行回调： 校验play是否是running 状态,如果是running,则在playbook中插入一条记录，并返回主键和成功，否则返回失败
	 * 执行中回调：根据主键和状态更新stdout,和时间，如果更新条数为1 返回成功，更新条数为0 返回失败
	 * 执行完成： 根据主键完成更新记录
	 *//*
	String process(SysInstallLog log) ;*/
	
	/**
	 * 当一个打的play执行完成之后，会对整个集群的状态进行更新，并最后更新play的状态标识。 解锁目标主机
	 * 
	 * 逻辑：
     * 1.判断当前任务是否存在    当前token是否正确      当前任务状态是否正确
     * 2.遍历roles中的role
     * 3.遍历ip，更新SysclusterHostRole表的角色状态和时间   
     * 如果是默认角色的话需要更新主机表的主机状态
     * 同时更新设备信息
     * 4.保存global信息，更新执行记录表信息
     * 5.设备解锁
	 * 
	 * 根据所有playbook 汇总的信息，更新global 和 hostRole 以及 roleDev信息
	 * 
	 * @param  finish
	 *  data: {
	 * 		global:{
	 * 			key:value
	 *	 	},
	 * 		roles:{
	 * 			docker:[
	 * 				{
	 *                 "127.0.0.1":{
	 * 					id:""
	 * 					addr:""
	 * 					status:0
	 * 					devs:[{
	 * 						status:
	 * 						devName:
	 * 						devSize:
	 * 						devSizeUsed:
	 * 					}
	 * 				]
	 *             }
	 * 			}]
	 * 		}
	 * }
	 */
	void finish(SysInstallPlayExec finish) ;

	/**
	 *  bootstrap 服务重启的时候需要将所有执行的PLAY状态变为失败，解锁全部主机
	 */
	void reset();

	void saveHost(SysClusterHost host);

	void saveHostRole(SysClusterHostRole hostRole);

	void saveRoleDev(SysClusterRoleDev roleDev);

	void saveGlobal(Map<String,String> info);
}
