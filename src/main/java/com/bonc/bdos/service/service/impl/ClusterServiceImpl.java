package com.bonc.bdos.service.service.impl;

import com.bonc.bdos.consts.ReturnCode;
import com.bonc.bdos.service.Global;
import com.bonc.bdos.service.entity.*;
import com.bonc.bdos.service.exception.ClusterException;
import com.bonc.bdos.service.repository.*;
import com.bonc.bdos.service.repository.*;
import com.bonc.bdos.service.service.ClusterService;
import com.bonc.bdos.service.service.HostService;
import com.bonc.bdos.service.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;
import java.util.Map.Entry;

@Service("clusterService")
public class ClusterServiceImpl extends Global implements ClusterService {

	private final SysClusterInfoRepository clusterInfoDao;
	private final SysClusterHostRepository clusterHostDao;
	private final SysClusterRoleRepository clusterRoleDao;
	private final SysClusterHostRoleRepository clusterHostRoleDao;
	private final SysClusterHostRoleDevRepository clusterRoleDevDao;
	private final SysClusterStoreCfgRepository clusterStoreCfgDao;
	private EntityManager em;

	private final HostService hostService;

	@Autowired
	public ClusterServiceImpl(SysClusterInfoRepository clusterInfoDao, SysClusterHostRepository clusterHostDao, SysClusterHostRoleRepository clusterHostRoleDao,
							  SysClusterHostRoleDevRepository clusterRoleDevDao, SysClusterStoreCfgRepository clusterStoreCfgDao,
							  HostService hostService, SysClusterRoleRepository clusterRoleDao) {
		this.clusterInfoDao = clusterInfoDao;
		this.clusterHostDao = clusterHostDao;
		this.clusterRoleDao = clusterRoleDao;
		this.clusterHostRoleDao = clusterHostRoleDao;
		this.clusterRoleDevDao = clusterRoleDevDao;
		this.clusterStoreCfgDao = clusterStoreCfgDao;

		this.hostService = hostService;
	}
	
    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.em = entityManager;
    }

	@Override
	public void init() {
		loadCfg(clusterInfoDao.findAll());
	}

	@Override
	@Transactional
	public void saveRoles(HashMap<String, Set<String>> roleSet) {
		List<SysClusterHost> hosts = clusterHostDao.findAll();
		HashMap<String,SysClusterHost> hostMap = new HashMap<>();
		for (SysClusterHost host : hosts){
			hostMap.put(host.getIp(),host);
		}

		// 1.涉及的主机状态必须都校验通过     (1)判断传入的ip是否存在于数据表中      (2)ip对应的主机是否已经校验通过    (3)ip是否已经锁住
		List<String> errorMsgs = new ArrayList<>();
		for (Set<String> ipSet:roleSet.values()){
			for (String ip:ipSet){
				SysClusterHost host = hostMap.get(ip);
				if (null == host){
					errorMsgs.add(ip+"不存在！");
					continue;
				}
				if (!host.check())		{errorMsgs.add(ip+host.getStatusDesc());}

				if(host.getHostLock())		{errorMsgs.add(ip+"已锁住");}
			}
		}
		if (!errorMsgs.isEmpty())		{throw  new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK,errorMsgs,"主机校验不合法");}

		// (4)判断需要操作的角色主机是否包含已经安装好的主机     如果已安装好则不能进行操作
		HashMap<String,HashMap<String, SysClusterHostRole>> roleMap = new HashMap<>();
		List<SysClusterHostRole> roles = clusterHostRoleDao.findAll();
		for (SysClusterHostRole role:roles){
			if (!roleMap.containsKey(role.getRoleCode())){
				roleMap.put(role.getRoleCode(),new HashMap<>());
			}
			roleMap.get(role.getRoleCode()).put(role.getIp(),role);

			if (role.isInstalled()  &&  roleSet.containsKey(role.getRoleCode())  &&  !roleSet.get(role.getRoleCode()).contains(role.getIp())) {
				errorMsgs.add(role.getStatusDesc());
			}
		}
		if (!errorMsgs.isEmpty())		{throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK,errorMsgs,"已经安装的好的角色不可删除");}

		// 2.根据数据表中角色集合roleMap和 传入的参数角色集合roleSet，参数集合中与表角色集合相比有新节点，则比较出哪个节点是新增的，哪个节点是需要废弃掉的，并且处理对应的设备信息
		Set<String> diffIps = new HashSet<>();
		for (String roleCode: roleSet.keySet()){
			Set<String> newIps = roleSet.get(roleCode);
			Set<String> oldIps = (roleMap.get(roleCode)!=null)?roleMap.get(roleCode).keySet():new HashSet<>();

			// 添加新增主机角色对应关系
			Set<String> addIps = new HashSet<>(newIps);
			addIps.removeAll(oldIps);
			diffIps.addAll(addIps);
			for(String ip:addIps){
				clusterHostRoleDao.save(new SysClusterHostRole(ip,roleCode));
			}

			// 删除废弃主机角色对应关系
			Set<String> delIps = new HashSet<>(oldIps);
			//dqy   后面循环删除的集合，应该与此次保持一致  delIps
			delIps.removeAll(newIps);
			
			diffIps.addAll(delIps);
			for(String ip:delIps){
				clusterHostRoleDao.deleteById(roleMap.get(roleCode).get(ip).getId());
			}
		}
		
		Set<String> targets = new HashSet<>();

		// 清理受到影响主机所有未安装设备的角色
		for (SysClusterHostRole role: roles){
			if (diffIps.contains(role.getIp())&&!role.isInstalled()&&!role.getRoleCode().equals(SysClusterRole.DEFAULT_ROLE)){
				//clusterRoleDevDao.deleteByHostRoleId(role.getId());
				targets.add(role.getIp());
			}
		}
		
		calculateDev(targets);
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
	public HashMap<String, String> findGlobal() {
		return Global.getTotal();
	}

	class StoreAllocation{

		// 提供存储资源的数据
		private HashMap<String, SysClusterHostRoleDev> provides = new HashMap<>();
		// 使用资源的需求配置
		private List<SysClusterStoreCfg> consumeCfg = new ArrayList<>();
		// 计算结果
		private List<SysClusterHostRoleDev> consumes = new ArrayList<>();

		private HashMap<String,SysClusterHostRole> roleMap = new HashMap<>();

		// 主机可使用总空间
		private int allSize = 0;
		// 角色需求最小总存储空间
		private int minSize = 0;
		// 角色需要最大宗存储空间
		private int maxSize = 0;
		
		private String errinfo;

		StoreAllocation(SysClusterHost host){

			// 设置主机设备的提供者
			for (SysClusterHostRoleDev dev : host.getDevs()){
				if (dev.isEnable()){
					this.provides.put(dev.getDevName(),dev);
					this.provides.get(dev.getDevName()).setDevSizeUsed(dev.getDevSizeUsed()+5);
				}
			}

			// 调整主机设备的使用量，并设置角色消费配置  以及主机角色关系
			for(SysClusterHostRole hostRole : host.getRoles().values()){
				if (hostRole.isInstalled()){
					for(SysClusterHostRoleDev dev:hostRole.getDevs()){
						//linux磁盘设备名    默认为8位    如 /dev/sd[a-p]  /dev/vd[a-p]   此次操作是在default角色集合中，记录已安装角色已使用的磁盘大小，方便计算磁盘可用量
						this.provides.get(dev.getDevName().substring(0,8)).setDevSizeUsed(dev.getDevSize()+this.provides.get(dev.getDevName().substring(0,8)).getDevSizeUsed());
					}
				}else{
					List<SysClusterStoreCfg> storeCfg = clusterStoreCfgDao.findAllByRoleCode(hostRole.getRoleCode());
					if(null!=storeCfg) 		{this.consumeCfg.addAll(storeCfg);}
					this.roleMap.put(hostRole.getRoleCode(),hostRole);
				}
			}

			// 计算总存储资源量
			for(SysClusterHostRoleDev provide:this.provides.values()){
				allSize = allSize+provide.getDevSize() - provide.getDevSizeUsed();
			}

			for(SysClusterStoreCfg cfg: this.consumeCfg){
				minSize += cfg.getMinSize();
				maxSize += cfg.getMaxSize();
			}
		}

		
		boolean calculate(){
		    List<SysClusterStoreCfg> cfgList = new ArrayList<>();
			if (allSize<minSize)	{
			    setErrinfo(cfgList);
			    return false;
			 }
			int index = 0;
			List<SysClusterHostRoleDev> provideList = new ArrayList<>(provides.values());
			
			//取出type为2的角色
			
			for(SysClusterStoreCfg cfg:this.consumeCfg) {
			    if(cfg.getStoreType() == '2') {
			        cfgList.add(cfg);
			    }
			}
			
			//如果有类型为2的角色才进行特殊角色的筛选
			boolean flag = true;
			if(cfgList.size()>0) {
			    flag = calspectype(cfgList,provideList);
			}
			
			if(!flag) {
			    setErrinfo(cfgList);
			    return false;
			 }
			// 将所有的分配结果添加到消费者列表里面
			for(SysClusterStoreCfg cfg: this.consumeCfg){
			    
			    if(cfg.getStoreType() == '2') {continue;}
			    
				//allocSize  为消费者（安装角色）需要使用的磁盘大小
				int allocSize = allSize>maxSize?cfg.getMaxSize():cfg.getMinSize();
				while (allocSize>0){
					//计算当前磁盘可使用的存储      如果3块磁盘，前3个角色默认分到这三块磁盘上
				    int i = index++%provideList.size();
					int size = provideList.get(i).accessAlloc(allocSize);
					
					if (size>0){
						//组装消费者（安装角色）所使用的磁盘情况  如 [{sdb  10G},{sdc  20G}],一对多     new SysClusterRoleDev(SysClusterHostRole,devName,devSize,vgName)
						this.consumes.add(new SysClusterHostRoleDev(roleMap.get(cfg.getRoleCode()),provideList.get(i).getDevName(),provideList.get(i).getPartType(),size,cfg.getName()));
					}
					allocSize -= size;
				}
			}

			// 分配成功之后将所有主机角色设置为已分配状态
			for(SysClusterHostRole role:roleMap.values()){
				role.setStatus(SysClusterHostRole.ALLOCATED);
			}
			return true;
		}
		
		@SuppressWarnings("unchecked")
        boolean calspectype(List<SysClusterStoreCfg> cfgList,List<SysClusterHostRoleDev> provideList) {
            List<HashMap<SysClusterStoreCfg, SysClusterHostRoleDev>> result = new ArrayList<>();
            
            //优先对type=2的角色进行分配     分出所有的组合   总数量为  磁盘个数的角色次幂     
            for(SysClusterStoreCfg rolecfg : cfgList) {
                List<HashMap<SysClusterStoreCfg, SysClusterHostRoleDev>> tmp = new ArrayList<>();
                HashMap<SysClusterStoreCfg, SysClusterHostRoleDev> tMap = new HashMap<>();
                for(SysClusterHostRoleDev provide:provideList) {
                    if(result.size()<=0) {
                        tMap.put(rolecfg, provide);
                        tmp.add((HashMap<SysClusterStoreCfg, SysClusterHostRoleDev>) tMap.clone());
                    }else {
                        for(HashMap<SysClusterStoreCfg, SysClusterHostRoleDev> remap:result) {
                            HashMap<SysClusterStoreCfg, SysClusterHostRoleDev> clonemap = (HashMap<SysClusterStoreCfg, SysClusterHostRoleDev>) remap.clone();
                            clonemap.put(rolecfg, provide);
                            tmp.add(clonemap);
                        }
                    }
                }
                result = tmp;
            }
            
            // 在所有磁盘角色组合中，拿出满足要求的组合      磁盘的大小需要满足角色所需存储的最小要求
            for(HashMap<SysClusterStoreCfg, SysClusterHostRoleDev> remap:result) {
                //组装比较数据           HashMap<磁盘大小,角色需求量>
                HashMap<SysClusterHostRoleDev, Integer> comparelist = new HashMap<>();
                for(Entry<SysClusterStoreCfg, SysClusterHostRoleDev> entry:remap.entrySet()) {
                    SysClusterStoreCfg rolecfg = entry.getKey();
                    SysClusterHostRoleDev roleDev = entry.getValue();
                    roleDev = provides.get(roleDev.getDevName());
                    if(comparelist.get(roleDev)==null) {
                        comparelist.put(roleDev, rolecfg.getMinSize());
                    }else {
                        comparelist.put(roleDev, rolecfg.getMinSize()+comparelist.get(roleDev));
                    }
                }
                
                //比较磁盘大小是否满足就是需求量   磁盘大小小于需求量则不满足 剔除该组合
                if(compareSize(comparelist)) {
                    for(Entry<SysClusterStoreCfg, SysClusterHostRoleDev> entry:remap.entrySet()) {
                        SysClusterStoreCfg rolecfg = entry.getKey();
                        SysClusterHostRoleDev roleDev = entry.getValue();
                        
                        roleDev = provides.get(roleDev.getDevName());
                        roleDev.setDevSizeUsed(roleDev.getDevSizeUsed()+rolecfg.getMinSize());
                        this.consumes.add(new SysClusterHostRoleDev(roleMap.get(rolecfg.getRoleCode()),roleDev.getDevName(),roleDev.getPartType(),rolecfg.getMinSize(),rolecfg.getName()));
                    }
                    return true;
                }
            }
            return false;
        }
		
		boolean compareSize(HashMap<SysClusterHostRoleDev, Integer> comparelist) {
		    for(Entry<SysClusterHostRoleDev, Integer> entry : comparelist.entrySet()) {
                if((entry.getKey().getDevSize()-entry.getKey().getDevSizeUsed()) < entry.getValue()) {
                    return false;
                }
            }
		    return true;
		}

		List<SysClusterHostRoleDev> getConsumes(){
			return this.consumes;
		}

		boolean getResult(){
			return this.allSize > this.minSize;
		}

		int getDiffSpace(){
			return allSize - minSize;
		}
		
		void setErrinfo(List<SysClusterStoreCfg> rolecfgs) {
		    StringBuilder sb = new StringBuilder();
		    if(rolecfgs.size()>0) {
		        for(SysClusterStoreCfg cfg : rolecfgs) {
		            sb.append("角色:").append(cfg.getRoleCode()).append("需要").append(cfg.getMinSize()).append("GB存储空间").append("\n");
		        }
		    }else {
		        sb.append("需要存储").append(-getDiffSpace()).append("GB空间");
            }
		    this.errinfo = sb.toString();
		}
		
		String getErrinfo() {
		    return errinfo;
		}

		List<SysClusterHostRole> getRoleSet(){
			return new ArrayList<>(roleMap.values());
		}
	}

	private boolean checkHost(SysClusterHost host,Collection<String> msg){
		if (!host.getHostLock()&&host.check()){
			return true;
		}
		if (host.getHostLock()){
			msg.add(host.getIp()+"已锁住！");
		}
		if (!host.check()){
			msg.add(host.getStatusDesc());
		}
		return false;
	}
	
	@Override
	@Transactional
	public void calculateDev(Set<String> targets) {
		List<String> errorHost = new ArrayList<>();
		// 检查通过的主机映射
		HashMap<String,SysClusterHost> hostMap = new HashMap<>();
		List<SysClusterHost> hosts = hostService.findHosts();

		Set<String>  ips;
		//dqy  主机是否存在只需要在targets不为空时校验
		Set<String> allHostIp = new HashSet<>();
		// 如果targets无效，对所有主机进行校验，否则对增量主机进行校验  组装需要进行分配磁盘的主机ip
		if(null==targets||targets.isEmpty()){
			for(SysClusterHost host:hosts){
				if(checkHost(host,errorHost)){
					hostMap.put(host.getIp(),host);
				}
			}
		}else{
			for(SysClusterHost host:hosts){
				if(targets.contains(host.getIp()) && checkHost(host,errorHost)){
					hostMap.put(host.getIp(),host);
				}
				allHostIp.add(host.getIp());
			}
			// targets的主机都必须存
			if(!allHostIp.containsAll(targets)) {
				throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK ,targets,"部分主机不存在！");
			}
		}
		if(!errorHost.isEmpty())		{throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK,errorHost,"部分主机未完成校验！请先完成校验");}
		ips = new HashSet<>(hostMap.keySet());
		
		List<StoreAllocation> saList = new ArrayList<>();
		for (String ip : ips){
			StoreAllocation sa = new StoreAllocation(hostMap.get(ip));
			if(!sa.calculate()){
				errorHost.add("主机"+ip+":"+sa.getErrinfo());
			}
			saList.add(sa);
		}

		if (errorHost.isEmpty()){
		    em.clear();
			for(StoreAllocation sa: saList){
			    
			    for(SysClusterHostRoleDev roleDev:sa.getConsumes()) {
			        //删除设备状态为初始状态的磁盘         保持数据库数据一致
			        if(roleDev.getStatus()=='0') {
		                 clusterRoleDevDao.deleteByHostRoleIdAndStatus(roleDev.getHostRoleId(),roleDev.getStatus());
			        }
			    }
			    
			    clusterRoleDevDao.saveAll(sa.getConsumes());
				clusterHostRoleDao.saveAll(sa.getRoleSet());
			}
		}else{
			throw new ClusterException(ReturnCode.CODE_CLUSTER_DEV_NOT_ENOUGH ,errorHost,"部分主机存储空间不足！");
		}
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
}
