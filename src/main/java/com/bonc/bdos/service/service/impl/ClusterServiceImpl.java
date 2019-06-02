package com.bonc.bdos.service.service.impl;

import com.bonc.bdos.consts.ReturnCode;
import com.bonc.bdos.service.Global;
import com.bonc.bdos.service.entity.*;
import com.bonc.bdos.service.exception.ClusterException;
import com.bonc.bdos.service.repository.*;
import com.bonc.bdos.service.service.ClusterService;
import com.bonc.bdos.service.service.HostService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Service("clusterService")
public class ClusterServiceImpl extends Global implements ClusterService {
	private final SysClusterInfoRepository clusterInfoDao;
	private final SysClusterHostRepository clusterHostDao;
	private final SysClusterRoleRepository clusterRoleDao;
	private final SysClusterRoleNumRepository clusterRoleNumDao;
	private final SysClusterRolePolicyRepository clusterRolePolicyDao;
	private final SysClusterHostRoleRepository clusterHostRoleDao;
	private final SysClusterHostRoleDevRepository clusterRoleDevDao;
	private final SysClusterStoreCfgRepository clusterStoreCfgDao;
	private EntityManager em;

	private final HostService hostService;

	@Autowired
	public ClusterServiceImpl(SysClusterInfoRepository clusterInfoDao, SysClusterHostRepository clusterHostDao, SysClusterRoleNumRepository clusterRoleNumDao,
							  SysClusterHostRoleDevRepository clusterRoleDevDao, SysClusterStoreCfgRepository clusterStoreCfgDao, SysClusterHostRoleRepository clusterHostRoleDao,
							  HostService hostService, SysClusterRoleRepository clusterRoleDao, SysClusterRolePolicyRepository clusterRolePolicyDao) {
		this.clusterInfoDao = clusterInfoDao;
		this.clusterHostDao = clusterHostDao;
		this.clusterRoleNumDao = clusterRoleNumDao;
		this.clusterRoleDao = clusterRoleDao;
		this.clusterRolePolicyDao = clusterRolePolicyDao;
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
//		for (Set<String> ipSet:roleSet.values()){
//			for (String ip:ipSet){
//				SysClusterHost host = hostMap.get(ip);
//				if (null == host){
//					errorMsgs.add(ip+"不存在！");
//					continue;
//				}
//				if (!host.check())		{errorMsgs.add(ip+host.getStatusDesc());}
//
//				if(host.getHostLock())		{errorMsgs.add(ip+"已锁住");}
//			}
//		}
//		if (!errorMsgs.isEmpty())		{throw  new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK,errorMsgs,"主机校验不合法");}

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
		
//		calculateDev(targets);
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
        private final Logger LOG = LoggerFactory.getLogger(StoreAllocation.class);

        private final String ip;
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

		// 磁盘浮动分配系数，表达每个需求者能得到的冗余占比 (allSize-minSize)/(maxSize-minSize) 但是这种存在 maxSize=minSize的风险，所以逻辑调整为 (allSize-minSize)/maxSize
        // 数据范围 0-1
		private int floatRatio;

		private String error;

        /**
         *  记录每块设备的等级积累，所有设备的指标均满足，认为方案满足改 指标等级
         */
		class DevRatio{
		    // 所有的配置的ratio 系数都能落入磁盘，这种分配结果是我们期望的，保证了所有需求独立满足
            int totalRatio=0;
		    // part 类型的ratio系数配置都能落入磁盘， 这种分配结果次之，保证了disk类型的基本合理需求
		    int partRatio=0;
            // 满足了part类型的最小需求，往往我们可能会    尽可能的多   让他满足自己的需求，然后其他的补齐流程
            int totalMin=0;
		    // part 类型的最小配置能独立落盘，这种分配结果打底，这个都满足不了就分配失败把
		    int partMin=0;

		    int realLoad = 0;

//		    int maxLevel = 0b0001;

		    int level = 0b1111;

		    float loadRatio = 0f;

            /**
             *  获取有效的设备存储叠加量， 真实的负载量：  在保证 part 有效基本负载的情况下，还需要承载vg 类型的目标负载
             */
		    void initEffectLoadValue(){
		        if (level > 0b1000){
                    realLoad = totalRatio;
                }else if (level> 0b0100){
                    realLoad = totalRatio;
                }else if (level > 0b0010){
                    realLoad = totalRatio;
                }else{
                    realLoad = partMin;
                    for (SysClusterStoreCfg load: vgLoads){
                        realLoad += load.getMaxSize()*floatRatio/100;
                    }
                }
            }

		    // 每块设备都承载自己的消费对象，在这里用负载对象 load 存储
		    List<SysClusterStoreCfg> vgLoads = new ArrayList<>();

            // part 类型消费对象的负载 列表
            List<SysClusterStoreCfg> partLoads = new ArrayList<>();
            /**
             *  想设备指标中添加消费者
             * @param costumer  消费者
             */
            public void add(SysClusterStoreCfg costumer) {
                switch (costumer.getStoreType()){
                    case SysClusterStoreCfg.TYPE_PART:
                        partLoads.add(costumer);
                        break;
                    case SysClusterStoreCfg.TYPE_VG:
                        vgLoads.add(costumer);
                        break;
                }
            }

            List<SysClusterStoreCfg> vgLoads(){
                return vgLoads;
            }

            List<SysClusterStoreCfg> partLoads(){
                return partLoads;
            }
        }

        /**
         *  方案指标信息，每个方案指标包含了这个方案的满足等级，根据方案指标等级筛选方案结果，和所有设备的指标信息
         *  方案指标分为四个等级
         *   0: 满足part 类型的基本分配需求
         *   1: 满足所有类型的基本分配需求
         *   2: 满足part 类型的最有分配需求
         *   3: 满足所有类型的最有分配需求
         */
        class PlanRatio {
            int maxLevelNum=0;
            int partRatio = 0;
//            private static final int LEVEL_MIN=0b0001;

            // 使用二进制控制  初始1111 标识
//            int level = 0b1111;

            // 每个方案里面都记录了所有设备的指标信息，每个设备指标，记录了设备的负载情况，以及每个需求的分配方案
            HashMap<SysClusterHostRoleDev,DevRatio> devRatio = new HashMap<>();

            DevRatio get(SysClusterHostRoleDev dev){
                return devRatio.get(dev);
            }

            public void put(SysClusterHostRoleDev dev) {
                if (!devRatio.containsKey(dev)){
                    devRatio.put(dev,new DevRatio());
                }
            }

            public boolean contains(SysClusterHostRoleDev dev){
                return devRatio.containsKey(dev);
            }

        }

        /**
         *  分配方案，里面包含了一组consume 和 provide 的映射关系 和 一个指标测评对象
         *
         *  该对象需要支持半深度clone ，因为再进行方案生成的时候，需要对生成方案进行深度复制，完成数据的膨胀，但是又不能全部深度，因为有些数据的引用性质又方便了逻辑的控制
         *  深度clone: 需要递归式的保证子数据都能得到完整的copy
         *  浅clone: 对象 clone的时候调用native 完成当前对象自身的copy ，当属性中有复杂对象的时候，只是将引用复制了一份，数据本身容易被污染。
         */
        @Data
        class AllocPlan implements Cloneable{
            // 方案指标
            PlanRatio planRatio = new PlanRatio();

            // 方案评分 默认是0 越高的分数，将作为最优方案
            float grade = 0L;

            // 分配结果
            HashMap<SysClusterStoreCfg,SysClusterHostRoleDev> allocData = new HashMap<>();

            List<SysClusterStoreCfg> partConsumer;

            @Override
            public AllocPlan clone(){
                try {
                    AllocPlan plan = (AllocPlan) super.clone();
                    plan.planRatio = new PlanRatio();
                    plan.allocData = new HashMap<>();
                    for (Entry<SysClusterStoreCfg,SysClusterHostRoleDev> entity : allocData.entrySet()){
                        plan.put(entity.getKey(),entity.getValue());
                    }
                    return plan;
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                    throw new ClusterException(ReturnCode.CODE_DATA_CLONE_ERROR ,new ArrayList<>(),"分配方案clone 失败！");
                }
            }

            public void put(SysClusterStoreCfg consume, SysClusterHostRoleDev provide) {
                allocData.put(consume,provide);
                planRatio.put(provide);
            }

            Iterable<? extends Entry<SysClusterStoreCfg, SysClusterHostRoleDev>> entrySet() {
                return allocData.entrySet();
            }

            public Collection<SysClusterHostRoleDev> values() {
                return allocData.values();
            }

            public SysClusterHostRoleDev get(SysClusterStoreCfg consume) {
                return allocData.get(consume);
            }
        }

		StoreAllocation(SysClusterHost host){
            this.ip = host.getIp();
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

			// 基于实际数据计算浮动系数，在使用设备上面，如果没有超出最大浮动系数可以使用完整个盘都用上,不考虑磁盘预留,  如果超过1 将设置1
            floatRatio=(allSize-minSize)*100/(maxSize);
            floatRatio=floatRatio>100?100:floatRatio;

            LOG.info("主机{} 磁盘分配浮动系数：{}",ip,floatRatio);
		}

        /**
         *   经过一系列方案流程，尽量将设备分配的更加合理，通过方案预算，评级，打分等流程最终筛选出最优方案，然后根据最优方案分盘，补齐分配流程
         * @return 是否分配成功
         */
		boolean calculate(){
		    LOG.info("主机：{} 开始进行磁盘分配逻辑计算",ip);
			if (allSize<minSize)	{
			    error = "磁盘总存储空间不足";
			    LOG.error("{} 磁盘总存储空间不足{}GB,最少需要：{}GB",ip,allSize,minSize);
                return false;
            }

			// 1.  组合分配结果，将n个存储配置落地到m块设备的所有组合数，默认情况下，我们先对需求进行独立落地，如果满足不了后面可以进行补救
            List<AllocPlan> allocPlans = combinationAlloc();

            // 2.  计算所有结果方案 的设备指标 和方案指标， 如果所有的方案被默认淘汰，怎不满足刚需，分配失败
            calculatePlanRatio(allocPlans);

            // 3.  根据指标信息删选出最优指标方案
            optimalPlans(allocPlans);
            if (allocPlans.isEmpty()){
                error = "最优指标未匹配出来";
                LOG.error("{}最优指标未匹配出来",ip);
                return false;
            }

            // 4.  给方案评分 从最优方案中筛选最终的组合方案,得到唯一的组合方案
            AllocPlan optimalPlan = finalPlan(allocPlans);

			// 5. 将所有的分配结果添加到消费者列表里面
            boolean result =  allocPlan(optimalPlan);
            LOG.info("磁盘最终分配{}！，已分配：{},总需非配：{}",result?"成功":"失败",this.consumes,this.consumeCfg);

			// 6. 分配成功之后将所有主机角色设置为已分配状态
            if (result){
                for(SysClusterHostRole role:roleMap.values()){
                    role.setStatus(SysClusterHostRole.ALLOCATED);
                }
            }else{
                error = "磁盘逻辑划分逻辑出错";
            }
			return result;
		}


        /**
         *  根据最优方案分配设备占用
         * @param optimalPlan 最优方案
         * @return 分配结果
         */
        private boolean allocPlan(AllocPlan optimalPlan) {
            // 优先分配独占式磁盘，计算分配系数
            for (SysClusterHostRoleDev dev: optimalPlan.planRatio.devRatio.keySet()){
                DevRatio ratio = optimalPlan.planRatio.get(dev);
                List<SysClusterStoreCfg> partLoads = ratio.partLoads();
                if (partLoads.size()>0){
                    int realRatio = dev.getEnableSize()*100/ratio.partRatio;
                    realRatio = realRatio>100?100:realRatio;
                    for (SysClusterStoreCfg consume: partLoads){
                        int size = realRatio*floatRatio*(consume.getMaxSize()-consume.getMinSize())/10000+consume.getMinSize();
                        this.consumes.add(new SysClusterHostRoleDev(roleMap.get(consume.getRoleCode()),dev.getDevName(),dev.getPartType(),size,consume.getName()));
                    }
                }
            }

            // vg 类型的消费者分配，如果当前磁盘不够了(不能满足最小需求认为是不够了)  那么交给补齐逻辑去处理把
            HashMap<SysClusterStoreCfg,Integer> remainConsumer = new HashMap<>();
            for (SysClusterHostRoleDev dev: optimalPlan.planRatio.devRatio.keySet()){
                DevRatio ratio = optimalPlan.planRatio.get(dev);
                List<SysClusterStoreCfg> vgLoads = ratio.vgLoads();
                for (SysClusterStoreCfg consume: vgLoads){
                    int realRatio = dev.getEnableSize()*10000/consume.getMaxSize()*floatRatio;
                    realRatio = realRatio>100?100:realRatio;
                    int baseRatio = dev.getEnableSize()*100/consume.getMinSize();
                    if (baseRatio<100){
                        remainConsumer.put(consume,consume.getMaxSize()*floatRatio/100);
                    }else{
                        int size = realRatio*floatRatio*(consume.getMaxSize()-consume.getMinSize())/10000+consume.getMinSize();
                        this.consumes.add(new SysClusterHostRoleDev(roleMap.get(consume.getRoleCode()),dev.getDevName(),dev.getPartType(),size,consume.getName()));
                    }
                }
            }

            // 填充逻辑
            if (remainConsumer.size()>0){
                return fullConsumer(remainConsumer);
            }else{
                return true;
            }
        }

        /**
         *  循环遍历可用设备填饱 消费
         * @param remainConsumer  剩余的消费者和他们的需求量
         * @return 是否能够包
         */
        private boolean fullConsumer(HashMap<SysClusterStoreCfg,Integer> remainConsumer){
            int index = 0;
            int guard = 100000;
            List<SysClusterHostRoleDev> provideList = new ArrayList<>(provides.values());
            int provideSize = provideList.size();
            for(SysClusterStoreCfg consume: remainConsumer.keySet()){
                if(consume.getStoreType() == SysClusterStoreCfg.TYPE_PART) {continue;}
                //allocSize  为消费者（安装角色）需要使用的磁盘大小
                int allocSize = remainConsumer.get(consume);
                while (allocSize>0&&index<guard){
                    //计算当前磁盘可使用的存储      如果3块磁盘，前3个角色默认分到这三块磁盘上
                    int cur = index++%provideSize;
                    int size = provideList.get(cur).accessAlloc(allocSize);

                    //组装消费者（安装角色）所使用的磁盘情况  如 [{sdb  10G},{sdc  20G}],一对多     new SysClusterRoleDev(SysClusterHostRole,devName,devSize,vgName)
                    if (size>0){
                        this.consumes.add(new SysClusterHostRoleDev(roleMap.get(consume.getRoleCode()),provideList.get(cur).getDevName(),provideList.get(cur).getPartType(),size,consume.getName()));
                    }

                    // 如果分配的空间到达了期望的一半就推出把，少点也没事
                    if (size-consume.getMinSize()==allocSize-size){
                        break;
                    }

                    allocSize -= size;
                }
            }
            return index<guard;
        }

        /**
         *  依据一定标准删选出可用的 分配结果,  这个逻辑属于角色层，想要分出优质的分配结果，主要靠这个逻辑调控
         * @param optimalPlans 结果指标信息,筛选最优解
         * @return 最终方案
         */
        AllocPlan finalPlan(List<AllocPlan> optimalPlans) {
            double varianceMax = 0;
            AllocPlan finalPlan = optimalPlans.get(0);

            for(AllocPlan plan:optimalPlans) {
                // 获取方案等级 计算负载系数
                PlanRatio planRatio = plan.planRatio;
                float planLoad = 0f;
                for(SysClusterHostRoleDev dev: plan.values()){
                    DevRatio ratio = planRatio.devRatio.get(dev);
                    ratio.initEffectLoadValue();
                    ratio.loadRatio = (ratio.realLoad*1f)/dev.getEnableSize();
                    planLoad+=ratio.loadRatio;
                }
                float averageLoad = planLoad/provides.size();

                double varianceLoad = 0f;
                for(SysClusterHostRoleDev provider: provides.values()){
                    varianceLoad += Math.sqrt(planRatio.contains(provider)?(planRatio.devRatio.get(provider).realLoad-averageLoad):averageLoad);
                }

                if(varianceLoad>varianceMax){
                    finalPlan = plan;
                }
            }

            LOG.info("筛选的最终分配方案是：{}",finalPlan);
		    return finalPlan;
		}


        /**
         *  根据最优方案装筛选出最终方案，那么什么级别组合的分配方案式最优的呢？有一下特征
         *  1: 包含的最高指标数最多的方案一般最优
         *  2: 我们尽量期望 part 类型的方案指标高一点，因为他们不能使用其他磁盘
         *  3:  在保证特征2的前提下  完成特征1。
         * @param allocPlans 目标信息
         */
        private void optimalPlans(List<AllocPlan> allocPlans) {
            int maxPartRatio = 0;
            HashMap<Integer,Integer> planLevelNum = new HashMap<>();
            for (AllocPlan plan: allocPlans){
                int planPartRatio = 0;
                int levelNum = 0;
                for (SysClusterHostRoleDev dev:plan.values()){
                    DevRatio ratio = plan.planRatio.get(dev);
                    if (ratio.partRatio>dev.getEnableSize()){
                        planPartRatio+=dev.getEnableSize();
                    }else{
                        planPartRatio+=ratio.partRatio;
                    }
                    planPartRatio += (ratio.partRatio>dev.getEnableSize()?dev.getDevSizeUsed():ratio.partRatio);
                    if (ratio.level>=0b1000){
                        levelNum++;
                    }
                }
                if(maxPartRatio<planPartRatio){
                    maxPartRatio = planPartRatio;
                }

                Integer num = planLevelNum.get(maxPartRatio);
                if (num==null||num<levelNum){
                    planLevelNum.put(maxPartRatio,levelNum);
                }
                plan.planRatio.maxLevelNum = levelNum;
                plan.planRatio.partRatio = planPartRatio;
            }

            int maxLevelNum = planLevelNum.get(maxPartRatio);
            Iterator<AllocPlan> iterator =  allocPlans.iterator();
            while (iterator.hasNext()){
                AllocPlan plan = iterator.next();
                if (plan.planRatio.partRatio != maxPartRatio || plan.planRatio.maxLevelNum != maxLevelNum){
                    LOG.warn("当前分配方案不是最优类型分配条件,最优partRatio：{}，最优levelNum： {}",maxPartRatio,maxLevelNum);
                    iterator.remove();
                }
            }
        }

        // 整理组合结果集，然后计算每个结果集合的结果指标，用于筛选结果集合
        private  void calculatePlanRatio(List<AllocPlan> allocPlan) {
//            int maxLevel = PlanRatio.LEVEL_MIN;

            // 以迭代器的方式遍历分配的方案，不满足的方案可以剔去
            Iterator<AllocPlan> iterator = allocPlan.iterator();
            while (iterator.hasNext()){
                AllocPlan plan = iterator.next();

                PlanRatio planRatio = plan.planRatio;
                // 遍历该方案下的所有映射关系 计算设备指标 和 方案指标等级
                for(Entry<SysClusterStoreCfg, SysClusterHostRoleDev> entry:plan.entrySet()) {
                    SysClusterStoreCfg costumer = entry.getKey();
                    SysClusterHostRoleDev roleDev = entry.getValue();
                    int enableSize = roleDev.getEnableSize();

                    // 设置当前磁盘对比的各个系数值  这四个指标大小排序有争议的是 partRatio 和totalMin  ratio.partMin<{ratio.totalMin,ratio.partRatio}<ratio.totalRatio
                    DevRatio ratio = planRatio.get(provides.get(roleDev.getDevName()));
                    if(costumer.getStoreType()==SysClusterStoreCfg.TYPE_PART){
                        ratio.partMin=ratio.partMin+costumer.getMinSize();
                        ratio.partRatio = ratio.partRatio+floatRatio*minSize/100;
                    }
                    ratio.totalMin = ratio.totalMin+costumer.getMinSize();
                    ratio.totalRatio = ratio.totalRatio+floatRatio*maxSize/100;

                    // 淘汰不满足刚需的指标
                    if (enableSize<ratio.partMin){
                        LOG.warn("当前分配方案不满足part 类型分配条件,可用存储：{}GB，需要存储>{}GB,方案集合： {}",enableSize,ratio.partMin,plan);
                        iterator.remove();
                        break;
                    }

                    // 如果没被淘汰掉，将分配关系写入负载列表里面去
                    ratio.add(costumer);

                    // 不满足 第二等级 将第二位清空，  虽然不满足第二等级，但是第三等级有可能就满足了，他俩大小不固定
                    if (enableSize<ratio.totalMin){
                        ratio.level = ratio.level&0b1101;
                    }
                    // 不满足第三等级将位清空    如果这个等级都不满足就不用探测下一个等级了
                    if (enableSize<ratio.partRatio){
                        ratio.level = ratio.level&0b1011;
                        continue;
                    }
                    // 前三个等级都满足 不满最高等级的情况
                    if (enableSize<ratio.totalRatio){
                        ratio.level = ratio.level&0b0111;
                    }
                }
            }
        }

        /*
         *
         *  通过克隆数据和组合逻辑实现数据膨胀，删选分配结果， 通常独占式分配对磁盘数量有着明显的占用需求，我们从删选结果中尽量让他们都分配到一个盘上去
         */
        List<AllocPlan> combinationAlloc() {
            List<AllocPlan> planSet = new ArrayList<>();
            //1： 膨胀分配组合结果集
            for(SysClusterStoreCfg consume : consumeCfg) {
                List<AllocPlan> inflationPlanSet = new ArrayList<>();
                for(SysClusterHostRoleDev provide:provides.values()) {
                    if(planSet.size()==0) {
                        AllocPlan plan = new AllocPlan();
                        plan.put(consume, provide);
                        inflationPlanSet.add(plan);
                    } else {
                        for(AllocPlan allocPlan:planSet) {
                            AllocPlan inflationPlan = allocPlan.clone();
                            inflationPlan.put(consume, provide);
                            inflationPlanSet.add(inflationPlan);
                        }
                    }
                }
                planSet = inflationPlanSet;
            }
            return planSet;
        }

		List<SysClusterHostRoleDev> getConsumes(){
			return this.consumes;
		}

		boolean getResult(){
			return this.allSize > this.minSize;
		}

//		int getDiffSpace(){
//			return allSize - minSize;
//		}
		
//		void setErrorInfo(List<SysClusterStoreCfg> roleCfgs) {
//		    StringBuilder sb = new StringBuilder();
//		    if(roleCfgs.size()>0) {
//		        for(SysClusterStoreCfg cfg : roleCfgs) {
//		            sb.append("角色:").append(cfg.getRoleCode()).append("需要").append(cfg.getMinSize()).append("GB存储空间").append("\n");
//		        }
//		    }else {
//		        sb.append("需要存储").append(-getDiffSpace()).append("GB空间");
//            }
//		    this.error = sb.toString();
//		}
		
		String getErrorInfo() {
		    return error;
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
				errorHost.add("主机"+ip+":"+sa.getErrorInfo());
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

		HashMap<String,Set<String>> roleSet = new HashMap<>();
		for (SysClusterHost host:hosts){
			for (String roleCode: host.getRoles().keySet()){
				if (!roleSet.containsKey(roleCode)){
					roleSet.put(roleCode,new HashSet<>());
				}
				roleSet.get(roleCode).add(host.getIp());
			}
		}
		saveRoles(roleSet);
		return hosts;
	}
}
