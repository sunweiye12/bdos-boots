package com.bonc.bdos.service.cluster.service;

import com.bonc.bdos.service.cluster.entity.SysInstallPlaybook;
import com.bonc.bdos.service.cluster.model.ExecProcess;

import java.util.List;

public interface ExecService {

	/**
	 * 对主机操作的各种接口,包含开始执行和继续执行
	 * 校验1: 同一个时间点一个主机只能在一个play中执行,通过LOCK_TYPE 判断锁target 主机还是全部主机
	 * 校验2: host_control 校验流程
	 * 
	 * 调用play的时候插入一条记录
	 * 
	 * 逻辑：
	 * 1.初始化一条执行记录SysInstallPlayExec
	 * 2.
	 * 根据playcode获取play，是否存在并且是否可用
	 * 根据play中的lockType获得目标主机ips，true全量主机，false为targets主机
	 * 3.ips主机校验，主机锁校验，拿到需要操作的所有主机信息SysClusterHost
	 * 4.初始化执行数据构造对象(play:play配置参     hostMap:全量的主机信息   targets:目标机器的ID)
	 * 开始构造SYS_INSTALL_PLAY_EXEC表数据
	 * (1)获取执行play的所有playbook
	 * (2)处理playbook(包含继续执行逻辑，可根据index判断)
	 * (3)构造playbook
	 * (4)根据SysInstallHostControl构造主机控制列表
	 * 根据当前角色roleCode和当前目标状态 target_enable构造主机列表
	 * 5.生成表数据
	 * 6.调用ansible驱动模块
	 * 
	 * @param targets : 目标主机列表 主键
	 * @param playCode： URL编码 ,对应操作的内容
	 * 
	 * @return data:{uuid：""}
	 */
	String exec(List<String> targets, String playCode) ;
	
	/**
	 * 完成一个对失败执行计划的延续执行,同一个时间点一个主机只能在一个play中执行
	 * 校验1: uuid 存在 且  处于一种失败的状态
	 * 校验2: 从失败的那个playbook 往下的 host_control 开始校验
	 * 
	 * 调用接口的时候，对play做更新操作 主要更新请求参数和play的状态
	 * @param uuid play 生成的uuid
	 */
	void resume(String uuid) ;
	
	/**
	 * 逻辑：
	 * 根据执行记录表，获取到对应的执行日志
	 * 
	 * 执行进度查询接口，请求play中所有playbook的日志信息
	 * @return  data:{
	 *     status:
	 *     stdout:
	 *     present:
	 * }
	 */
	ExecProcess query(String uuid) ;
	
	/**
	 * 页面初始化数据使用
	 * 初始化需要执行的playbook列表
	 * 
	 * @param  playCode 任务类型
	 * @return List<SysInstallPlaybook>
	 */
	List<SysInstallPlaybook> initPlaybooks(String playCode);

	/**
	 *  根据playCode 查询最新的任务ID
	 * @param playCode 任务编码
	 * @return 任务ID
	 */
	String getLatestUuid(String playCode);
}
