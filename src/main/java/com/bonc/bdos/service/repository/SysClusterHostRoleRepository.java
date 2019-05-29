package com.bonc.bdos.service.repository;

import com.bonc.bdos.service.entity.SysClusterHostRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClusterHostRoleRepository extends JpaRepository<SysClusterHostRole,String> {


    /**
     * Description:根据hostId查询
     * @param 
     * 		ip	ip地址
     * @return
     * List<SysClusterHostRoleRepository>
     */
    List<SysClusterHostRole>  findByIp(String ip);

    List<SysClusterHostRole> findByRoleCode(String roleCode);

    void deleteByIp(String ip);

    SysClusterHostRole findByIpAndRoleCode(String ip, String defaultRole);
}
