package com.bonc.bdos.service.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 *  用于角色安装数量的把控，主要支持2中设置方式
 *  1： 分段式数量设置，比如机器的数量超过50，100 ,的时候，分别设置多少合适
 *  2： 比例设置，如果当某个角色的数量超过多少的时候，应该设置为这个角色的比例，然后取整
 *
 *  后面规则的数量控制会覆盖前面的规则控制
 */
@Entity
@Table(name = "`sys_cluster_role_num`")
@Data
public class SysClusterRoleNum implements Serializable {

    public static final char TYPE_NUM = '1';
    public static final char TYPE_PERCENT = '2';

    @Id
    @Column(name = "`id`")
    private Integer id;

    @Column(name = "`role_code`",length = 32)
    private String roleCode;

    @Column(name = "`ref`",length = 32)
    private String ref;

    @Column(name = "`ref_num`")
    private Integer refNum;

    @Column(name = "`ref_type`",length = 1)
    private char refType;

    @Column(name = "`role_num`")
    private Integer roleNum;

    @Column(name = "`memo`")
    private String memo;
}
