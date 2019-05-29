package com.bonc.bdos.service.repository;

import java.util.List;

import com.bonc.bdos.service.entity.SysClusterHostRoleDev;
import org.springframework.data.jpa.repository.JpaRepository;

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
