package com.bonc.bdos.service.service.impl;

import com.bonc.bdos.service.Global;
import com.bonc.bdos.service.entity.*;
import com.bonc.bdos.service.repository.SysClusterHostRepository;
import com.bonc.bdos.service.repository.SysClusterHostRoleDevRepository;
import com.bonc.bdos.service.repository.SysClusterHostRoleRepository;
import com.bonc.bdos.service.repository.SysInstallPlayExecRepository;
import com.bonc.bdos.service.service.CallService;
import com.bonc.bdos.service.service.ClusterService;
import com.bonc.bdos.utils.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service("callbackService")
public class CallServiceImpl implements CallService {

	private final SysInstallPlayExecRepository installPlayExecDao;
	private final SysClusterHostRepository clusterHostDao;
	private final SysClusterHostRoleRepository clusterHostRoleDao;
	private final SysClusterHostRoleDevRepository clusterRoleDevDao;

	private final ClusterService clusterService;

	@Autowired
	public CallServiceImpl(SysInstallPlayExecRepository installPlayExecDao,
						   SysClusterHostRepository clusterHostDao, SysClusterHostRoleRepository clusterHostRoleDao,
						   SysClusterHostRoleDevRepository clusterRoleDevDao, ClusterService clusterService) {
		this.installPlayExecDao = installPlayExecDao;
		this.clusterHostDao = clusterHostDao;
		this.clusterHostRoleDao = clusterHostRoleDao;
		this.clusterRoleDevDao = clusterRoleDevDao;

		this.clusterService = clusterService;
	}

	@Override
	@Transactional
	public void start(SysInstallPlayExec exec)  {
		exec.setStatus(SysInstallPlayExec.RUNNING);
		installPlayExecDao.save(exec);
	}

	@Override
	@Transactional
	public void finish(SysInstallPlayExec finish) {
		//保存任务状态
		installPlayExecDao.save(finish);

		// 解锁主机
		for(String ip:finish.getTargetIps()){
			Optional<SysClusterHost> optional = clusterHostDao.findById(ip);
			optional.ifPresent(clusterService::unlockHost);
		}
	}

	@Override
	@Transactional
	public void saveHost(SysClusterHost host) {
		Optional<SysClusterHost> optional = clusterHostDao.findById(host.getIp());
		if (optional.isPresent()){
			SysClusterHost hostInfo = optional.get();
			hostInfo.setStatus(host.getStatus());
			hostInfo.setCpu(host.getCpu());
			hostInfo.setMemory(host.getMemory());
			hostInfo.setStatus(host.getStatus());
			if (!StringUtils.isEmpty(host.getHostname())){
				hostInfo.setHostname(host.getHostname());
			}
			

			SysClusterHostRole hostRole = clusterHostRoleDao.findByIpAndRoleCode(hostInfo.getIp(), SysClusterRole.DEFAULT_ROLE);

			List<SysClusterHostRoleDev> roleDevList = clusterRoleDevDao.findByHostRoleId(hostRole.getId());
			HashMap<String, SysClusterHostRoleDev> devMap = new HashMap<>();
			for (SysClusterHostRoleDev roleDev:roleDevList){
				devMap.put(roleDev.getDevName(),roleDev);
			}

			// 保存主机信息
			clusterHostDao.save(hostInfo);

			// 保存默认角色的状态
			hostRole.setStatus(hostInfo.getStatus());
			clusterHostRoleDao.save(hostRole);

			//保存设备信息
			if (null!=host.getDevs()&&!host.getDevs().isEmpty()){
				for (SysClusterHostRoleDev roleDev:host.getDevs()){
					roleDev.setIp(hostInfo.getIp());
					roleDev.setHostRoleId(hostRole.getId());

					// 如果设备已经存在，更新就行了
					if (devMap.containsKey(roleDev.getDevName())){
						roleDev.setId(devMap.get(roleDev.getDevName()).getId());
						roleDev.setUpdateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
					}else{
						roleDev.setCreateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
					}

					clusterRoleDevDao.save(roleDev);
				}
			}
		}

	}

	@Override
	@Transactional
	public void saveHostRole(SysClusterHostRole hostRole) {
		Optional<SysClusterHostRole> optional =  clusterHostRoleDao.findById(hostRole.getId());
		if (optional.isPresent()){
			SysClusterHostRole hostRoleInfo = optional.get();
			hostRoleInfo.setStatus(hostRole.getStatus());
			clusterHostRoleDao.save(hostRoleInfo);
		}
	}

	/**
	 * 只能根据ID更新
	 * @param roleDev 设备信息
	 */
	@Override
	@Transactional
	public void saveRoleDev(SysClusterHostRoleDev roleDev) {
		Optional<SysClusterHostRoleDev> optional = clusterRoleDevDao.findById(roleDev.getId());
		if (optional.isPresent()){
			SysClusterHostRoleDev devInfo = optional.get();
			devInfo.setUpdateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
			devInfo.setStatus(roleDev.getStatus());
			devInfo.setDevSizeUsed(roleDev.getDevSizeUsed());
			devInfo.setDevName(roleDev.getDevName());
			devInfo.setPartType(roleDev.getPartType());
			clusterRoleDevDao.save(devInfo);
		}
	}

	@Override
	@Transactional
	public void saveGlobal(Map<String, String> info) {
		clusterService.saveGlobal(info, Global.INNER_SET);
	}
}
