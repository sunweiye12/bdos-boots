package com.bonc.bdos.service.cluster.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bonc.bdos.service.cluster.entity.SysClusterStoreCfg;

import java.util.List;

public interface SysClusterStoreCfgRepository extends JpaRepository<SysClusterStoreCfg, String> {

    List<SysClusterStoreCfg> findAllByRoleCode(String roleCode);
}
