package com.bonc.bdos.service.repository;

import com.bonc.bdos.service.entity.SysClusterRolePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClusterRolePolicyRepository extends JpaRepository<SysClusterRolePolicy, String> {
    List<SysClusterRolePolicy> findAllByOrderById();
}
