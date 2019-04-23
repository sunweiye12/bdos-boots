package com.bonc.bdos.service.cluster.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bonc.bdos.service.cluster.entity.SysClusterHost;

public interface SysClusterHostRepository extends JpaRepository<SysClusterHost,String> {
 
    List<SysClusterHost>  findByHostLock(Boolean hostLock);
}
