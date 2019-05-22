package com.bonc.bdos.service.cluster.service.impl;

import com.bonc.bdos.service.cluster.Global;
import com.bonc.bdos.service.cluster.entity.*;
import com.bonc.bdos.service.cluster.repository.SysClusterHostRepository;
import com.bonc.bdos.service.cluster.repository.SysClusterHostRoleRepository;
import com.bonc.bdos.service.cluster.repository.SysClusterRoleDevRepository;
import com.bonc.bdos.service.cluster.repository.SysInstallPlayExecRepository;
import com.bonc.bdos.service.cluster.service.CallbackService;
import com.bonc.bdos.service.cluster.service.ClusterService;
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
public class CallbackServiceImpl implements CallbackService{

	private final SysInstallPlayExecRepository installPlayExecDao;
	private final SysClusterHostRepository clusterHostDao;
	private final SysClusterHostRoleRepository clusterHostRoleDao;
	private final SysClusterRoleDevRepository clusterRoleDevDao;

	private final ClusterService clusterService;

	@Autowired
	public CallbackServiceImpl(SysInstallPlayExecRepository installPlayExecDao,
							   SysClusterHostRepository clusterHostDao,SysClusterHostRoleRepository clusterHostRoleDao,
							   SysClusterRoleDevRepository clusterRoleDevDao,ClusterService clusterService) {
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

	private void unlockHost(SysClusterHost host){
		if (null!=host && host.getHostLock()){
			host.setHostLock(false);
			clusterHostDao.save(host);
		}
	}

	@Override
	@Transactional
	public void finish(SysInstallPlayExec finish) {
		//保存任务状态
		installPlayExecDao.save(finish);

		// 解锁主机
		for(String ip:finish.getTargetIps()){
			Optional<SysClusterHost> optional = clusterHostDao.findById(ip);
			optional.ifPresent(this::unlockHost);
		}
	}

	@Override
	@Transactional
	public void reset() {
		// 将所有运行中的PLAY状态置为失败
		List<SysInstallPlayExec> execs = installPlayExecDao.findByStatus(SysInstallPlayExec.RUNNING);
		for (SysInstallPlayExec exec:execs){
			exec.setStatus(SysInstallPlayExec.FAILED);
		}
		installPlayExecDao.saveAll(execs);

		// 解锁所有主机
		List<SysClusterHost> hosts = clusterHostDao.findAll();
		for(SysClusterHost host:hosts){
			unlockHost(host);
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

			List<SysClusterRoleDev> roleDevList = clusterRoleDevDao.findByHostRoleId(hostRole.getId());
			HashMap<String,SysClusterRoleDev> devMap = new HashMap<>();
			for (SysClusterRoleDev roleDev:roleDevList){
				devMap.put(roleDev.getDevName(),roleDev);
			}

			// 保存主机信息
			clusterHostDao.save(hostInfo);

			// 保存默认角色的状态
			hostRole.setStatus(hostInfo.getStatus());
			hostRole.setHostname(hostInfo.getHostname());
			clusterHostRoleDao.save(hostRole);

			//保存设备信息
			if (null!=host.getDevs()&&!host.getDevs().isEmpty()){
				for (SysClusterRoleDev roleDev:host.getDevs()){
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
	public void saveRoleDev(SysClusterRoleDev roleDev) {
		Optional<SysClusterRoleDev> optional = clusterRoleDevDao.findById(roleDev.getId());
		if (optional.isPresent()){
			SysClusterRoleDev devInfo = optional.get();
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
