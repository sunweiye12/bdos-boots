package com.bonc.bdos.service.service.impl;

import com.bonc.bdos.consts.ReturnCode;
import com.bonc.bdos.service.Global;
import com.bonc.bdos.service.entity.*;
import com.bonc.bdos.service.exception.ClusterException;
import com.bonc.bdos.service.repository.*;
import com.bonc.bdos.service.service.ClusterService;
import com.bonc.bdos.service.service.HostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service("clusterService")
public class ClusterServiceImpl extends Global implements ClusterService {
	private final SysClusterInfoRepository clusterInfoDao;
	private final SysClusterHostRepository clusterHostDao;
	private final SysClusterRoleRepository clusterRoleDao;
	private final SysClusterRoleNumRepository clusterRoleNumDao;
	private final SysClusterRolePolicyRepository clusterRolePolicyDao;
	private final SysClusterHostRoleRepository clusterHostRoleDao;
	private final SysClusterStoreCfgRepository clusterStoreCfgDao;

	private final HostService hostService;

	@Autowired
	public ClusterServiceImpl(SysClusterInfoRepository clusterInfoDao, SysClusterHostRepository clusterHostDao, SysClusterRoleNumRepository clusterRoleNumDao,
							  SysClusterStoreCfgRepository clusterStoreCfgDao, SysClusterHostRoleRepository clusterHostRoleDao,
							  HostService hostService, SysClusterRoleRepository clusterRoleDao, SysClusterRolePolicyRepository clusterRolePolicyDao) {
		this.clusterInfoDao = clusterInfoDao;
		this.clusterHostDao = clusterHostDao;
		this.clusterRoleNumDao = clusterRoleNumDao;
		this.clusterRoleDao = clusterRoleDao;
		this.clusterRolePolicyDao = clusterRolePolicyDao;
		this.clusterHostRoleDao = clusterHostRoleDao;
		this.clusterStoreCfgDao = clusterStoreCfgDao;

		this.hostService = hostService;
	}

	@Override
	public void init() {
		loadCfg(clusterInfoDao.findAll());
	}

	@Override
	@Transactional
	public void saveRoles(List<SysClusterHost> hosts) {
		// 1.涉及的主机状态必须都校验通过     (1)判断传入的ip是否存在于数据表中      (2)ip对应的主机是否已经校验通过    (3)ip是否已经锁住
		List<String> installedRole = new ArrayList<>();

		// (4)判断需要操作的角色主机是否包含已经安装好的主机     如果已安装好则不能进行操作
		HashMap<String,HashMap<String, SysClusterHostRole>> hostRoleMap = new HashMap<>();
		List<SysClusterHostRole> hostRoles = clusterHostRoleDao.findAll();
		for (SysClusterHostRole role:hostRoles){
			if (!hostRoleMap.containsKey(role.getIp())){
				hostRoleMap.put(role.getIp(),new HashMap<>());
			}
			hostRoleMap.get(role.getIp()).put(role.getRoleCode(),role);
		}

		for (SysClusterHost host: hosts){
			// 新增主机角色逻辑
			for (String roleCode: host.getRoles().keySet()){
				if (!hostRoleMap.containsKey(host.getIp())||!hostRoleMap.get(host.getIp()).containsKey(roleCode)){
					clusterHostRoleDao.save(new SysClusterHostRole(host.getIp(),roleCode));
				}
			}
			// 删除主机角色逻辑
			if (hostRoleMap.containsKey(host.getIp())){
				for (String roleCode: hostRoleMap.get(host.getIp()).keySet()){
					if (null!=host.getRoles()&&!host.getRoles().containsKey(roleCode)){
						SysClusterHostRole hostRole = hostRoleMap.get(host.getIp()).get(roleCode);
						if (hostRole.isInstalled()){
							installedRole.add(hostRole.getIp()+":"+hostRole.getRoleCode());
						}else{
							clusterHostRoleDao.deleteById(hostRole.getId());
						}
					}
				}
			}
		}

		if (!installedRole.isEmpty())		{throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK,installedRole,"已经安装的好的角色不可删除");}
	}

	@Override
	public HashMap<String,List<HashMap<String,Character>>> findRoles() {
		HashMap<String,List<HashMap<String,Character>>> roles = new HashMap<>();
		List<SysClusterHostRole> roleList = clusterHostRoleDao.findAll();
		for(SysClusterHostRole role:roleList){
			if (!roles.containsKey(role.getRoleCode()))	{roles.put(role.getRoleCode(),new ArrayList<>());}
			roles.get(role.getRoleCode()).add(new HashMap<String, Character>(){{
				put(role.getIp(),role.getStatus());
			}});
		}
		return roles;
	}


	@Override
	@Transactional
	public void saveGlobal(Map<String, String> global,char cfgType)  {
		for(String key:global.keySet()){
			SysClusterInfo cfg = Global.getEntity(key);
			if (cfg == null)		{continue;}
			cfg.setCfgValue(global.get(key));
			clusterInfoDao.save(cfg);
		}
	}

	@Override
	public List<SysClusterInfo> findGlobal() {
		return clusterInfoDao.findAll();
	}

	@Override
	public List<SysClusterStoreCfg> storeCfg() {
		return clusterStoreCfgDao.findAll();
	}

	@Override
	public List<SysClusterRole> roleCfg() {
		return clusterRoleDao.findAll();
	}

	@Override
	public void unlockHost(SysClusterHost host){
		if (null!=host && host.getHostLock()){
			host.setHostLock(false);
			clusterHostDao.save(host);
		}
	}

	private class RolePolicyHelper {
		// 推荐主机信息
		private List<SysClusterHost> hosts;
		// 角色安装在那些机器上面
		Map<String,List<SysClusterHost>> roleHost = new HashMap<>();
		// 角色亲和性控制
		Map<String,List<SysClusterRolePolicy>> policyCtl = new HashMap<>();
		// 数量亲和性控制
		Map<String,List<SysClusterRoleNum>> numCtl = new HashMap<>();

		// 角色推荐顺序
        List<SysClusterRole> roles ;

		RolePolicyHelper(List<SysClusterHost> hosts) {
			this.hosts = hosts;
			for (SysClusterHost host: hosts){
				for (String roleCode: host.getRoles().keySet()){
					if(!roleHost.containsKey(roleCode)){
						roleHost.put(roleCode,new ArrayList<>());
					}
					roleHost.get(roleCode).add(host);
				}
			}
//			roleHost.put(SysClusterRole.DEFAULT_ROLE,hosts);
			roles = roleCfg();
		}

		private void initRolePolicyCtl() {
			List<SysClusterRolePolicy> policyList = clusterRolePolicyDao.findAllByOrderById();
			for (SysClusterRolePolicy policy: policyList){
				if(!policyCtl.containsKey(policy.getRoleCode())){
					policyCtl.put(policy.getRoleCode(),new ArrayList<>());
				}
				policyCtl.get(policy.getRoleCode()).add(policy);
			}
		}

		private void initRoleNumCtl() {
			List<SysClusterRoleNum> numList = clusterRoleNumDao.findAllByOrderByRefNum();
			for (SysClusterRoleNum num: numList){
				if(!numCtl.containsKey(num.getRoleCode())){
					numCtl.put(num.getRoleCode(),new ArrayList<>());
				}
				numCtl.get(num.getRoleCode()).add(num);
			}
		}

		void initRoleCtl(){
			initRolePolicyCtl();
			initRoleNumCtl();

			// 填充默认角色控制数据
			for (SysClusterRole role: roles){
				String roleCode = role.getRoleCode();
				if (!roleHost.containsKey(roleCode)){
					roleHost.put(roleCode,new ArrayList<>());
				}
				if (!policyCtl.containsKey(roleCode)){
					policyCtl.put(roleCode,new ArrayList<>());
				}
				if(!numCtl.containsKey(roleCode)){
					numCtl.put(roleCode,new ArrayList<>());
				}
			}
		}

		void doPolicy(){
			for(SysClusterRole role: roles){
				// 采用策略推荐拓展安装角色,
				policyCheck(role.getRoleCode());
			}
		}

		/**
		 *  判断一个角色还差几个机器没满足，如果没有配置数目控制，将返回一个很大的值，标识始终使用策略控制
		 * @param roleCode 角色信息
		 * @return 差额主机数
		 */
		private int checkNum(String roleCode) {
			int roleNum = Integer.MAX_VALUE;
			int roleSize = roleHost.get(roleCode).size();
			for (SysClusterRoleNum ctl:numCtl.get(roleCode)){
				int refNum = roleHost.get(ctl.getRef()).size();
				if(refNum > ctl.getRefNum()){
					if (ctl.getRefType() == SysClusterRoleNum.TYPE_NUM){
						roleNum = ctl.getRoleNum();
					}
					if(ctl.getRefType() == SysClusterRoleNum.TYPE_PERCENT ){
						roleNum = refNum*ctl.getRoleNum()/100;
					}
				}
			}
			return roleNum-roleSize;
		}

		/**
		 * 根据数量
		 * @param roleCode 需要推荐的角色编码
		 */
		private void policyCheck(String roleCode) {
            // 先判断推荐的角色是否满足数量控制 如果num>0才可以增加角色的安装，否则只能减少角色的安装
            int num = checkNum(roleCode);

			// 如果策略是强控制，更新主机列表中的角色信息
			for (SysClusterRolePolicy policy: policyCtl.get(roleCode)){
				List<SysClusterHost> refHosts = roleHost.get(policy.getRef());
				if (policy.getForce()){
					for (SysClusterHost host:refHosts){
						if (policy.getAffine()&&host.getRoles().get(roleCode)==null ){
							host.getRoles().put(roleCode,new SysClusterHostRole(host.getIp(),roleCode));
							roleHost.get(roleCode).add(host);
							num--;
						}
					}
					for (SysClusterHost host: hosts){
                        if (!policy.getAffine()&&host.getRoles().get(policy.getRef())==null&&host.getRoles().get(roleCode)==null){
                            host.getRoles().put(roleCode,new SysClusterHostRole(host.getIp(),roleCode));
                            roleHost.get(roleCode).add(host);
                            num--;
                        }
                    }
				}
			}

            List<SysClusterHost> candidates = new ArrayList<>();
			List<SysClusterHost> back = new ArrayList<>();
			if (num>0){
				// 根据弱控制选择候选主机
				for (SysClusterRolePolicy policy: policyCtl.get(roleCode)){
					List<SysClusterHost> refHosts = roleHost.get(policy.getRef());
					if (!policy.getForce()){
						if(policy.getAffine()){
							for (SysClusterHost host:refHosts){
								if (!host.getRoles().containsKey(roleCode)){
									candidates.add(host);
								}
							}
						}else{
                            for (SysClusterHost host:refHosts){
                                if (!host.getRoles().containsKey(roleCode)){
                                    back.add(host);
                                }
                            }
						}
					}
				}
                num = policyRole(roleCode, num, candidates.stream().filter(t-> !back.contains(t)).collect(Collectors.toList()));
            }

            // 从非强制非亲和主机中选择
            if (num>0){
                policyRole(roleCode,num,back);
            }
		}

        private int policyRole(String roleCode, int num, List<SysClusterHost> candidates) {
            List<SysClusterHost> targets = candidates;
            if (candidates.size()>num){
                targets = candidates.subList(0,num);
                for (int index=num;index<candidates.size();index++){
                    for (int min=0;min<targets.size();min++){
                        if (targets.get(min).getRoles().keySet().size()>candidates.get(index).getRoles().keySet().size()){
                            targets.set(min,candidates.get(index));
                            break;
                        }
                    }
                }
            }
            for (SysClusterHost host: targets){
                host.getRoles().put(roleCode,new SysClusterHostRole(host.getIp(),roleCode));
                roleHost.get(roleCode).add(host);
                num--;
            }
            return num;
        }
    }

	/**
	 *  角色安装推荐的核心逻辑
	 * @param hosts 推荐主机的列表
	 * @return  推荐的安装结果
	 */
	@Override
	@Transactional
	public List<SysClusterHost> rolePolicy(List<SysClusterHost> hosts) {
	    if (hosts.isEmpty()){
	        hosts = hostService.findHosts();
        }
		RolePolicyHelper policyHelper = new RolePolicyHelper(hosts);
		policyHelper.initRoleCtl();
		policyHelper.doPolicy();
		saveRoles(hosts);
		return hosts;
	}
}
