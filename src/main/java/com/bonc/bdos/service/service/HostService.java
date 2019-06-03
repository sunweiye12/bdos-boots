package com.bonc.bdos.service.service;

import com.bonc.bdos.service.entity.SysClusterHost;
import com.bonc.bdos.service.entity.SysClusterHostRoleDev;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface HostService {
	
	/**
	 * 保存主机信息,没有进行新增,存在进行更新; 
	 * 校验一、除了ID STATUS 其他字段都不能为空    IP满足IP结构
//	 * 校验二、不能插入多个相同的IP，IP是逻辑主键			@ 后将IP设计未主键，不用对IP做唯一校验
//	 * 校验三、有任务在执行，集群状态主机状态都无法变更		@ 主机锁控制之后可以将执行校验改为主机锁校验
	 * 
	 * 逻辑一、STATUS将设置为0    save
	 * @param host  主机信息
	 */
	void saveHost(SysClusterHost host) ;

	/**
	 * 校验一、 主机是否有锁
	 * 校验二、主机上没有安装好的角色
	 * 
	 * 逻辑 、   删除dev  role   host
	 * @param ip  主机IP    flag主机安装标识
	 */
	void deleteHost(String ip,boolean flag) ;
	
	/**
	 * 主机查询接口
	 * 
	 * 逻辑：
	 * 1.获取SysClusterHost   主机信息
	 * 2.根据主机列表获取SysClusterHostRole  主机角色信息
	 * 3.遍历所有角色    (1)role == default 将设备添加到主机信息里   (2)  查询角色对应的设备 添加到角色中
	 * 
	 * 主要返回 主机数据，主机设备数据，角色数据，(角色设备数据,磁盘使用占比，考虑扩容和清理操作)
	 * 
	 * @return 
	 * data:[{
	 * 	addr:""
	 * 	username:""
	 * 	...
	 * 	devs:[{devName:/dev/sdb,devSize:500G,used:200G}] 只要default
	 * 	roles:{
	 * 		docker: { 不要default
	 * 		roleName: docker
	 * 		roleDevs:[{devName:/dev/sdb1,devSize:100G,used:50G}]
	 * 		}
	 * 	}
	 * }]
	 */
	List<SysClusterHost> findHosts();

	/**
	 *  根据模板文件上传主机信息
	 * @param template 模板文件
	 */
    void saveTemplate(InputStream template) throws IOException;

}
