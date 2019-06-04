package com.bonc.bdos.service.service.impl;

import com.bonc.bdos.consts.ReturnCode;
import com.bonc.bdos.service.entity.*;
import com.bonc.bdos.service.exception.ClusterException;
import com.bonc.bdos.service.repository.SysClusterHostRoleDevRepository;
import com.bonc.bdos.service.repository.SysClusterHostRoleRepository;
import com.bonc.bdos.service.repository.SysClusterStoreCfgRepository;
import com.bonc.bdos.service.service.DevService;
import com.bonc.bdos.service.service.HostService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;

@Service
public class DevServiceImpl implements DevService {
    private final SysClusterHostRoleRepository clusterHostRoleDao;
    private final SysClusterHostRoleDevRepository clusterRoleDevDao;
    private final SysClusterStoreCfgRepository clusterStoreCfgDao;
    private EntityManager em;

    private final HostService hostService;

    public DevServiceImpl(SysClusterHostRoleRepository clusterHostRoleDao, SysClusterHostRoleDevRepository clusterRoleDevDao,
                          SysClusterStoreCfgRepository clusterStoreCfgDao, HostService hostService) {
        this.clusterStoreCfgDao = clusterStoreCfgDao;
        this.clusterHostRoleDao = clusterHostRoleDao;
        this.clusterRoleDevDao = clusterRoleDevDao;
        this.hostService = hostService;
    }

    // 缓存每种角色的配置信息
    private static HashMap<String,List<SysClusterStoreCfg>> storeCache = new HashMap<>() ;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.em = entityManager;
    }

    @Override
    public List<SysClusterHostRoleDev> findDev(String ip) {
        SysClusterHostRole hostRole = clusterHostRoleDao.findByIpAndRoleCode(ip, SysClusterRole.DEFAULT_ROLE);
        if (null!=hostRole){
            return clusterRoleDevDao.findByHostRoleIdOrderByDevName(hostRole.getId());
        }else{
            return new ArrayList<>();
        }
    }


    @Override
    @Transactional
    public void enableDev(String id, boolean enable)  {
        // 检查设备是否存在
        Optional<SysClusterHostRoleDev> optional = clusterRoleDevDao.findById(id);
        if (!optional.isPresent()) {
            throw  new ClusterException(ReturnCode.CODE_CLUSTER_DEV_NOT_EXIST,"设备不存在！");
        }

        SysClusterHostRoleDev dev = optional.get();

        if (dev.isUsed()&&!enable) {
            throw  new ClusterException(ReturnCode.CODE_CLUSTER_DEV_IN_USERD," 设备已经被使用");
        }
        //状态为可用  0 1
        if (dev.isEnable()){
            dev.disable();
        }else {
            dev.enable();
        }
        //  启停设备
        clusterRoleDevDao.save(dev);
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
         *  我们将磁盘分配等级定义为如下几个等级：
         *  1:  所有的配置的ratio 系数都能落入磁盘，这种分配结果是我们期望的，保证了所有需求独立满足
         *  2:  part 类型的ratio系数配置都能落入磁盘， 这种分配结果次之，保证了disk类型的基本合理需求
         *  3:  满足了part类型的最小需求，往往我们可能会    尽可能的多   让他满足自己的需求，然后其他的补齐流程
         *  4:  part 类型的最小配置能独立落盘，这种分配结果打底，这个都满足不了，方案不成立！
         */
        class DevRatio{
            //记录所有应用按照期望值分配存储的叠加值
            int totalRatio=0;
            // 记录磁盘上分配的part 类型应用占用的叠加值
            int partRatio=0;
            // 记录所有应用按照最小值分配的存储叠加值
            int totalMin=0;
            // 记录磁盘上分配的part 类型应用的叠加值
            int partMin=0;

            int level = 0b1111;



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
         *   2: 满足part 类型的最优分配需求
         *   3: 满足所有类型的最优分配需求
         */
        class PlanRatio {
            int maxLevelNum=0;
            int partRatio = 0;

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
                    // allocData 本身需要深度生成一份，但是里面的数据体不能被深度复制，直接使用原来的引用
                    for (Map.Entry<SysClusterStoreCfg,SysClusterHostRoleDev> entity : allocData.entrySet()){
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

            Iterable<? extends Map.Entry<SysClusterStoreCfg, SysClusterHostRoleDev>> entrySet() {
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
                // 这里认为可用的设备是
                if (dev.isEnable() && dev.getEnableSize() > 20){
                    this.provides.put(dev.getDevName(),dev);
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
                    // 优先读取缓存配置
                    String roleCode = hostRole.getRoleCode();
                    if (storeCache.containsKey(roleCode)){
                        this.consumeCfg.addAll(storeCache.get(roleCode));
                    }else{
                        List<SysClusterStoreCfg> storeCfg = clusterStoreCfgDao.findAllByRoleCode(roleCode);
                        if(null!=storeCfg) {
                            this.consumeCfg.addAll(storeCfg);
                            storeCache.put(roleCode,storeCfg);
                        }
                    }
                    this.roleMap.put(roleCode,hostRole);
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
            if (maxSize>0){
                floatRatio=(allSize-minSize)*100/(maxSize);
                floatRatio=floatRatio>100?100:floatRatio;
            }
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
            boolean result =  allocateDev(optimalPlan);
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
        private boolean allocateDev(AllocPlan optimalPlan) {
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
            // 就一个的话，直接返回吧
            if (optimalPlans.size()==1){
                return optimalPlans.get(0);
            }

            double varianceMax = 0;
            AllocPlan finalPlan = optimalPlans.get(0);

            for(AllocPlan plan:optimalPlans) {
                // 获取方案等级 计算负载系数
                PlanRatio planRatio = plan.planRatio;
                float planLoad = 0f;
                for (SysClusterHostRoleDev dev: plan.values()){
                    planLoad+=planRatio.devRatio.get(dev).totalRatio;
                }
                float averageLoad = planLoad/provides.size();

                double varianceLoad = 0f;
                for(SysClusterHostRoleDev provider: provides.values()){
                    varianceLoad += Math.sqrt(planRatio.contains(provider)?(planRatio.devRatio.get(provider).totalRatio-averageLoad):averageLoad);
                }

                if(varianceLoad>varianceMax){
                    finalPlan = plan;
                }
            }
            return finalPlan;
        }


        /**
         *  根据最优方案装筛选出最终方案，那么什么级别组合的分配方案式最优的呢？有一下特征
         *  1: 包含的最高指标数最多的方案一般最优
         *  2: 我们尽量期望 part 类型的方案指标高一点，因为他们不能使用其他磁盘  partRatio 认为part 类型的最终使用权/量越高
         *  3:  在保证特征2的前提下  完成特征1。
         * @param allocPlans 目标信息
         */
        private void optimalPlans(List<AllocPlan> allocPlans) {
            // 1： 数据准备阶段，主要准备方案指标和最大等级数，并汇总，最大的方案指标，以及最大方案指标中对应的最大等级数量
            int maxPartRatio = 0;
            HashMap<Integer,Integer> planLevelNum = new HashMap<>();
            for (AllocPlan plan: allocPlans){
                int planPartRatio = 0;
                int levelNum = 0;
                for (SysClusterHostRoleDev dev:plan.values()){
                    DevRatio ratio = plan.planRatio.get(dev);

                    // 汇总part 的最终ratio 作为 plan 的ratio
                    if (ratio.partRatio>dev.getEnableSize()){
                        planPartRatio+=dev.getEnableSize();
                    }else{
                        planPartRatio+=ratio.partRatio;
                    }

                    // 统计 设备level 超过最高level 的数量，都为0 也没有问题，他们都是同等级的，还会做最后的筛选
                    if (ratio.level>=0b1000){
                        levelNum++;
                    }
                }

                // 判断当前plan 是不是最优的 partRatio
                if(maxPartRatio<planPartRatio){
                    maxPartRatio = planPartRatio;
                }

                // 记录最优的partRatio 对应最大的等级数的最大值，作为方案筛选使用，如果一个好多相同的方案指标一样，但是最高等级数不一样的话，那么获取最高等级数量多的方案
                Integer num = planLevelNum.get(maxPartRatio);
                if (num==null||num<levelNum){
                    planLevelNum.put(maxPartRatio,levelNum);
                }

                // 设置当前方案中的等级数量和方案指标值
                plan.planRatio.maxLevelNum = levelNum;
                plan.planRatio.partRatio = planPartRatio;
            }

            //2: 数据筛选阶段，取出最大方案指标对应的最大等级数量，迭代提出不匹配的方案
            int maxLevelNum = planLevelNum.get(maxPartRatio);
            Iterator<AllocPlan> iterator =  allocPlans.iterator();
            int removeNum = 0;
            while (iterator.hasNext()){
                AllocPlan plan = iterator.next();
                if (plan.planRatio.partRatio != maxPartRatio || plan.planRatio.maxLevelNum != maxLevelNum){
                    iterator.remove();
                    removeNum++;
                }
            }
            LOG.warn("最优方案指标：{}，最优等级数： {}，满足条件的方案数：{}，剔除不满足的方案数：",maxPartRatio,maxLevelNum,allocPlans.size(),removeNum);
        }

        // 整理组合结果集，然后计算每个结果集合的结果指标，用于筛选结果集合
        private  void calculatePlanRatio(List<AllocPlan> allocPlan) {
            // 以迭代器的方式遍历分配的方案，不满足的方案可以剔去
            Iterator<AllocPlan> iterator = allocPlan.iterator();
            while (iterator.hasNext()){
                AllocPlan plan = iterator.next();

                PlanRatio planRatio = plan.planRatio;
                // 遍历该方案下的所有映射关系 计算设备指标 和 方案指标等级
                for(Map.Entry<SysClusterStoreCfg, SysClusterHostRoleDev> entry:plan.entrySet()) {
                    SysClusterStoreCfg costumer = entry.getKey();
                    SysClusterHostRoleDev roleDev = entry.getValue();
                    int enableSize = roleDev.getEnableSize();

                    // 设置当前磁盘对比的各个系数值  这四个指标大小排序有争议的是 partRatio 和totalMin  ratio.partMin<{ratio.totalMin,ratio.partRatio}<ratio.totalRatio
                    DevRatio ratio = planRatio.get(provides.get(roleDev.getDevName()));
                    if(costumer.getStoreType()==SysClusterStoreCfg.TYPE_PART){
                        ratio.partMin=ratio.partMin+costumer.getMinSize();
                        ratio.partRatio = ratio.partRatio+(floatRatio*(maxSize-minSize)/100+minSize);
                    }
                    ratio.totalMin = ratio.totalMin+costumer.getMinSize();
                    ratio.totalRatio = ratio.totalRatio+(floatRatio*(maxSize-minSize)/100+minSize);

                    // 淘汰不满足刚需的指标
                    if (enableSize<ratio.partMin){
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
    public void allocate(Set<String> targets) {
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
}


