package com.bonc.bdos.service.repository;

import java.util.List;

import com.bonc.bdos.service.entity.SysInstallPlayExec;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysInstallPlayExecRepository extends JpaRepository<SysInstallPlayExec, String> {



    /**
     * Description:根据status查询
     * @param 
     * 		status	执行状态
     * @return
     * List<SysInstallPlayExec>
     */
    List<SysInstallPlayExec>  findByStatus(char status);

    List<SysInstallPlayExec> findAllByPlayCodeOrderByCreateDateDesc(String playCode);
}
