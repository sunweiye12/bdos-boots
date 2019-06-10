package com.bonc.bdos.service.service;

import com.bonc.bdos.service.entity.SysClusterHost;
import com.bonc.bdos.service.entity.SysClusterInfo;
import com.bonc.bdos.service.entity.SysClusterRole;
import com.bonc.bdos.service.entity.SysClusterStoreCfg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ClusterService {

	/**
	 * 集群状态保存接口,主要更新那些主机上安装什么东西,和global相关的配置如: master_vip; 以及每个主机可用的设备信息(defaults角色)
	 * 
	 * 校验一: 如果有exec 在执行中不可以更改集群状态
	 * 校验二: 如果主机上有角色已经安装好了不可以更改这个角色,需要走卸载流程
	 * 校验三: 如果主机状态未校验通过，主机的任何状态无法更新
	 * 
	 * 
//	 * 场景一: 主机设备调整 (界面调整,回掉调整)[default角色下]
//	 * 	1. 设备表里没数据可以直接插入(主键是devName)
//	 * 	2. 设备表里有数据，如果是使用中:状态无法进行更改;如果不是:更新完成之后,清空    该主机    对应的设备表的所有角色记录(未使用的状态),并修改角色状态为未分配
	 * 
	 * 场景二: 角色分布(界面调整)
	 * 	1. 如果角色已经安装,不可清理改主机对应的分布
	 * 	2. 更新之后，清理    涉及到的所有主机   设备表的所有角色记录(未使用的状态),并修改角色状态为未分配
	 * 
	 * 逻辑：
	 * 1.参数中传入主机对应的状态必须都校验通过     
	 *     (1)判断传入的ip是否存在于数据表中     
	 *     (2)ip对应的主机是否已经校验通过   
	 *     (3)ip是否已经锁住
	 *     (4)判断需要操作的角色主机是否包含已经安装好的主机     如果已安装好则不能进行操作
	 * 2.根据数据表中角色集合roleMap和 传入的参数角色集合roleSet，参数集合中与表角色集合相比有新节点，则比较出哪个节点是新增的，哪个节点是需要废弃掉的，并且处理对应的设备信息
	 * 
	 * @param hosts
	 * {
	 * 	roles:{ //不要 default
	 * 		docker:[ip1,ip2]
	 * 	}
	 * }
	 * 
	 */
	void saveRoles(List<SysClusterHost> hosts);

	/**
	 * 查询集群分布试图
	 * 
	 * 只要返回 global数据和角色对应的主机IP
	 * @return
	 * data:{
	 * 		docker:[{
	 * 			addr:""
	 * 			username:""
	 * 			password:""
	 * 		
	 * 	     }]
	 * }
	 */
	HashMap<String,List<HashMap<String,Character>>> findRoles();


	/**
	 *  保存全局配置
	 *  逻辑：判断是否有任务在执行     否插入
	 * @param global  全局配置
	 */
	void saveGlobal(Map<String,String> global,char cfgType) ;

	/**
	 * 查询全局配置
	 * @return map 全局配置
	 */
	List<SysClusterInfo> findGlobal();

    List<SysClusterStoreCfg> storeCfg();

    List<SysClusterRole> roleCfg();

	void unlockHost(SysClusterHost host);

	List<SysClusterHost> rolePolicy(List<SysClusterHost> hosts);
}
