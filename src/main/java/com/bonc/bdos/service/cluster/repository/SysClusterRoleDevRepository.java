package com.bonc.bdos.service.cluster.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.bonc.bdos.service.cluster.entity.SysClusterRoleDev;

public interface SysClusterRoleDevRepository extends JpaRepository<SysClusterRoleDev, String> {

	/**
	 * 
	 * @param 
	 * 		roleId
	 * @return
	 * 		List<SysClusterRoleDev>
	 */
	public List<SysClusterRoleDev> findByHostRoleId(String roleId);
	
	
	/**
	 * 
	 * @param 
	 * 		id
	 * 		devName
	 * @return
	 * 		SysClusterRoleDev
	 */ 
	public SysClusterRoleDev findByIdAndDevName(String id,String devName);


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
	List<SysClusterRoleDev> findByIp(String ip);
	
    @Modifying(clearAutomatically=true)
	@Query(value ="INSERT INTO sys_cluster_role_dev "
	        + "( id, create_date, dev_name, dev_size, dev_size_used, host_role_id, ip, name, status, update_date)"
	        + " VALUES "
	        + "( ?10,?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)",nativeQuery = true)
	void saveDev(Timestamp createDate,String devName,int devSize,int devSizeUsed,String hostRoleId,String ip,String name,char status,Timestamp updateDate,String uuid);
}
