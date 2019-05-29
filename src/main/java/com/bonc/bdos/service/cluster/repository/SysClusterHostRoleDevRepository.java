package com.bonc.bdos.service.cluster.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.bonc.bdos.service.cluster.entity.SysClusterHostRoleDev;

public interface SysClusterHostRoleDevRepository extends JpaRepository<SysClusterHostRoleDev, String> {

	/**
	 * 
	 * @param 
	 * 		roleId
	 * @return
	 * 		List<SysClusterRoleDev>
	 */
	List<SysClusterHostRoleDev> findByHostRoleId(String roleId);
	
	
	/**
	 * 
	 * @param 
	 * 		id
	 * 		devName
	 * @return
	 * 		SysClusterRoleDev
	 */ 
	SysClusterHostRoleDev findByIdAndDevName(String id, String devName);


	/**
	 * 根据主机角色ID 删除下面的所有设备信息
	 * @param hostRoleId 主机角色主键
	 */
    void deleteByHostRoleId(String hostRoleId);
    
    /**
     * 根据主机角色ID 和状态         删除下面的所有设备信息
     * @param hostRoleId   主机角色主键         status状态
     */
    void deleteByHostRoleIdAndStatus(String hostRoleId,char status);

	/**
	 *
	 * @param ip
	 */
    void deleteByIp(String ip);

	/**
	 *
	 * @param ip
	 * @return
	 */
	List<SysClusterHostRoleDev> findByIp(String ip);

}
