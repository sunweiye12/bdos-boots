package com.bonc.bdos.service.cluster.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bonc.bdos.service.cluster.entity.SysInstallPlaybook;

import java.util.List;

public interface SysInstallPlaybookRepository extends JpaRepository<SysInstallPlaybook, String> {


    /**
     * 查询一个playCode下所有的playbook 通过 index 正序排列
     * @param playCode play编码
     * @return 有序的playbook列表
     */
    List<SysInstallPlaybook> findByPlayCodeOrderByIndex(String playCode);
}
