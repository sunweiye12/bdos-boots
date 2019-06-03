package com.bonc.bdos.service.service;

import com.bonc.bdos.service.entity.SysClusterHostRoleDev;

import java.util.List;
import java.util.Set;

public interface DevService {
    /**
     *  查询主机的可用设备
     * @return 设备列表
     */
    List<SysClusterHostRoleDev> findDev(String ip);



    /**
     * 逻辑：
     * 1.检查节点ip是否存在
     * 2.检查节点ip对应的状态是否需要更新
     *  0 未使用     1 已禁用     2 已使用
     *  0->1   0->2   1->0   2->0
     *
     * 主机设备状态更新接口(default角色对应的设备),是否启用主机的设备可以被使用
     * @param id 主机设备ID
     * @param enable  true  启用 false  停用
     *
     * 已经
     */
    void enableDev(String id, boolean enable) ;


    /**
     * 对主机做设备分配,支持对集群全部主机做设备分配和对指定主机做设备分配
     * 主要解决 主机 与 角色 之间存储的供需问题 通过角色状态感知需要分配存储的主机
     * 角色状态
     *
     * 每次计算完成存储之后重新计算一下磁盘使用量，加加减减的容易对不上
     *
     * 1.判断targets是否为空   为空则对全量主机进行操作     不为空则对targets进行操作
     * 2.设置设备的提供者  每一块磁盘都是一个提供者，直到把一块磁盘分完
     * 3.设置主机的消费者，每一个角色都是一个消费者，调整主机设备的使用量，并设置角色消费配置  以及主机角色关系，
     *     （已安装过的角色在提供者中减去），未安装的角色加入到消费者中
     * 4.根据消费者集合，计算每个角色在那个磁盘上使用多少存储
     * 5.修改角色表和设备表
     *
     * @param targets 目标主机
     */
    void allocate(Set<String> targets) ;
}
