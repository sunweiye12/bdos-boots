package com.bonc.bdos.service.repository;

import com.bonc.bdos.service.entity.SysInstallHostControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysInstallHostControlRepository extends JpaRepository<SysInstallHostControl, String> {

    List<SysInstallHostControl> findByPlaybookId(Long id);
}
