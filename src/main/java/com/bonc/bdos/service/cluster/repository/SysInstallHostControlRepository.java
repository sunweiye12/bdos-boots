package com.bonc.bdos.service.cluster.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bonc.bdos.service.cluster.entity.SysInstallHostControl;

import java.util.List;

public interface SysInstallHostControlRepository extends JpaRepository<SysInstallHostControl, String> {

    List<SysInstallHostControl> findByPlaybookId(Long id);
}
