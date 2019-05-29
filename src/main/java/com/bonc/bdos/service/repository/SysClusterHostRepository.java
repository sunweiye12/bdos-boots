package com.bonc.bdos.service.repository;

import java.util.List;

import com.bonc.bdos.service.entity.SysClusterHost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysClusterHostRepository extends JpaRepository<SysClusterHost,String> {
 
    List<SysClusterHost>  findByHostLock(Boolean hostLock);
}
