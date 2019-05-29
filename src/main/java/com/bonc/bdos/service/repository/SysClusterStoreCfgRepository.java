package com.bonc.bdos.service.repository;

import com.bonc.bdos.service.entity.SysClusterStoreCfg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClusterStoreCfgRepository extends JpaRepository<SysClusterStoreCfg, String> {

    List<SysClusterStoreCfg> findAllByRoleCode(String roleCode);
}
