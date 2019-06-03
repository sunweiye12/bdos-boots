package com.bonc.bdos.service.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "`sys_cluster_role_policy`")
@Data
public class SysClusterRolePolicy implements Serializable {
    @Id
    @Column(name = "`id`")
    private Integer id;

    /**
     *  需要推荐的角色
     */
    @Column(name = "`role_code`", length = 32)
    private String roleCode;

    /**
     *  依赖的角色编码
     */
    @Column(name = "`ref`", length = 32)
    private String ref;

    /**
     *  亲和角色还是反亲和角色
     */
    @Column(name = "`affine`")
    private Boolean affine;

    /**
     *  是否强制亲和角色
     */
    @Column(name = "`force`")
    private Boolean force;

    @Column(name = "`memo`")
    private String memo;
}
