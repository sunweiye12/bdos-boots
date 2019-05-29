package com.bonc.bdos.service.cluster.service.impl;

import com.bonc.bdos.consts.ReturnCode;
import com.bonc.bdos.service.cluster.entity.*;
import com.bonc.bdos.service.cluster.exception.ClusterException;
import com.bonc.bdos.service.cluster.model.ExecProcess;
import com.bonc.bdos.service.cluster.repository.*;
import com.bonc.bdos.service.cluster.service.ExecService;
import com.bonc.bdos.service.cluster.tasks.TaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service("execService")
public class ExecServiceImpl implements ExecService{

	private final SysInstallPlayRepository installPlayDao;
	private final SysInstallPlaybookRepository installPlaybookDao;
	private final SysClusterHostRepository clusterHostDao;
	private final SysClusterHostRoleRepository clusterHostRoleDao;
	private final SysInstallPlayExecRepository installPlayExecDao;
	private final SysInstallHostControlRepository installHostControlDao;
	private final SysClusterHostRoleDevRepository clusterRoleDevDao;
//	private final PlayExecFactory factory;


	@Autowired
	public ExecServiceImpl(SysInstallPlayRepository installPlayDao,SysInstallPlaybookRepository installPlaybookDao,
						   SysClusterHostRepository clusterHostDao,SysClusterHostRoleRepository clusterHostRoleDao,
						   SysInstallPlayExecRepository installPlayExecDao, SysInstallHostControlRepository installHostControlDao,
						   SysClusterHostRoleDevRepository clusterRoleDevDao) {
		this.installPlayDao = installPlayDao;
		this.installPlaybookDao = installPlaybookDao;
		this.clusterHostDao = clusterHostDao;
		this.clusterHostRoleDao = clusterHostRoleDao;
		this.installPlayExecDao = installPlayExecDao;
		this.installHostControlDao = installHostControlDao;
		this.clusterRoleDevDao = clusterRoleDevDao;
	}

	/**
	 *  playExec 执行数据构造器
	 *
	 *  通过构造playExec 构造playbook 构造hostControl 三级完成playExec 参数的组装
	 *
	 *  注： 每一步构造如果失败都会向上级设置失败，并可以通过playExec 的 getConstructErrs 方法获取失败的详细原因
	 */
	class PlayExecFactory{
		private final SysInstallPlay play;  // play配置参数
		private final HashMap<String,SysClusterHost> hostMap; // 全量的主机信息
		private final List<String> targets = new ArrayList<>(); //目标机器的ID
		private List<SysInstallHostControl> playbookError = new ArrayList<>();  //返回的错误信息列表
		private final String playName;

		/**
		 * 当play 是部分锁的时候 targets必须有效
		 * @param play		执行计划
		 * @param targets		目标主机
		 */
		PlayExecFactory(SysInstallPlay play,Collection<String> targets,HashMap<String,SysClusterHost> hostMap,String playName) throws ClusterException {
			this.play = play;
			//lockType为增量主机  targets目标主机为空，返回error
			if (!play.getLockType()&&(null == targets || targets.isEmpty()))			{throw new ClusterException(ReturnCode.CODE_CLUSTER_PARAM_IS_EMPTY,"目标主机为空");}
			this.hostMap = hostMap;
			this.targets.addAll(targets);
			this.playName = playName;
		}
		
        /**
		 *  构造playExec参数报文
		 */
		void constructPlayExec(SysInstallPlayExec exec) {
			// 生成32位token  组装SysInstallPlayExec exec
			exec.setStatus(SysInstallPlayExec.INIT);
			exec.setTargetsJson(this.targets);
			exec.setPlayName(playName);
			
			//获取执行play的所有的playbook
			List<SysInstallPlaybook> playbooks = installPlaybookDao.findByPlayCodeOrderByIndex(this.play.getPlayCode());

			// 包含继续执行逻辑0
			for (SysInstallPlaybook playbook: playbooks){
			    
			    
				if (playbook.getIndex()<exec.getCurIndex())		{continue;}

				// 构造playbook
				boolean flag = constructPlaybook(playbook);

				// 如果有playbook 构造失败，设置playExec 构造失败
				if (!flag && exec.getFlag())  {exec.setFlag(false);}

				// 添加playbook
				exec.addPlaybook(playbook);
			}
		}

		private boolean constructPlaybook(SysInstallPlaybook playbook) {
		    // 构造playbook
			List<SysInstallHostControl> controls = installHostControlDao.findByPlaybookId(playbook.getId());

			// 构造主机控制列表
			for (SysInstallHostControl control:controls){
				// 获取控制主机列表并构造主机控制列表
				boolean flag = constructHosts(control);

				// 如果主机控制不满足  设置playbook构造失败
				if (!flag&&playbook.isFlag()) {playbook.setFlag(false);}

				// 将主机控制对象添加到playbook里面
				playbook.addRoles(control);
			}

			return playbook.isFlag();
		}
		
		private SysClusterHost constructHost (SysClusterHostRole hostRole) {
		    List<SysClusterHostRoleDev> devs = clusterRoleDevDao.findByHostRoleId(hostRole.getId());
		    SysClusterHost host = null;
            try {
                host = hostMap.get(hostRole.getIp()).clone();
				host.setDevs(devs);
				host.setRoleId(hostRole.getId());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            return host;
		}

		/**
		 *  构造hostControl 中的主机列表 ，如果构造成功
		 *
		 *  控制类型一：所有的角色状态主机必须是目标状态  target_enable:false   full_status
		 *  控制类型二：所有的目标角色状态主机必须包含控制机  target_enable:true  not_full_status
		 *
		 *
		 * @param control  主机控制的限制逻辑配置对象
		 * @return  主机是否具备 true 具备 false 不具备
		 */
		private boolean constructHosts(SysInstallHostControl control) {
		    //  根据主机控制列表中的角色  获取对应的目标主机信息
			//  根据当前角色   获取对应的目标机器信息
			List<SysClusterHostRole> hostRoles = clusterHostRoleDao.findByRoleCode(control.getRoleCode());

			List<String> errorHost = new ArrayList<>();
			
			if (!control.getTargetEnable()){
				// 类型一、所有角色的状态必须是设定状态  全量主机   status  除为*的情况下不匹配status  其余全部匹配  
				for (SysClusterHostRole hostRole : hostRoles){
				    //control -> status状态选择
					if (control.getStatus()!='*' && hostRole.getStatus() != control.getStatus()){
						control.setFlag(false);
						errorHost.add(hostRole.getIp());
					}
					// 存储驱动执行依赖的  主机目标ip
					control.addHost(constructHost(hostRole));
				}
			}else{
				// 类型二、所有目标机的角色状态必须是设定状态，先找出所有状态对的主机，再确认一下是否有target不在这些主机里面    targets列表主机
			    // 除为*的情况下不匹配status 其余status全部匹配  
				HashMap<String, SysClusterHostRole> statusHosts = new HashMap<>();
				for (SysClusterHostRole hostRole:hostRoles){
					if (control.getStatus()=='*' || hostRole.getStatus() == control.getStatus()){
						statusHosts.put(hostRole.getIp(),hostRole);
					}
				}
				for (String hostId:this.targets){
					if (!statusHosts.containsKey(hostId)){
						control.setFlag(false);
						errorHost.add(hostId);
					}else {
					    control.addHost(constructHost(statusHosts.get(hostId)));
					}
				}
			}
			
			// 如果有状态不对的主机，将control 的hosts设定为异常的主机清单用于数据组装   清空control
			if (!control.getFlag()) {
				control.clearHosts();
				for (String hostId:errorHost){
					control.addHost(this.hostMap.get(hostId));
				}
				putError(control);
			}

			return control.getFlag();
		}
		
		
        private void putError(SysInstallHostControl control) {
            playbookError.add(control);
        }
        
        /**
         * 获取playExec 组装失败的原因
         * @return  组装失败的原因清单
         */
        List<String> getErrorMsg(){
            List<String> errorMsg = new ArrayList<>();
            if(playbookError.size()<=0) {return errorMsg;}
            
            for(SysInstallHostControl con:this.playbookError) {
                StringBuilder ips = new StringBuilder();
                for(String ip:con.getHosts().keySet()) {
                    ips.append("-");
                    ips.append(ip);
                }
                errorMsg.add("SysInstallHostControl表id为："+con.getPlaybookId()+"，状态为："+con.getStatus()+" 的"+con.getRoleCode()+"主机列表"+ips+"不满足执行条件！");
            }
            return errorMsg;
        }
	}

	/**
	 * if targets is null checkAllHost  or check targets host
	 * @param targets 目标机器  如果为[] ,视为全部主机加锁
	 * @return all host map
	 */
	private synchronized HashMap<String,SysClusterHost> lockHosts(Set<String> targets) throws ClusterException {
		List<SysClusterHost> hosts = clusterHostDao.findAll();

		boolean flag = targets.isEmpty();

		// 获取有冲突的主机，先查询当前锁住的主机，再和targets取交集
		List<String> lockHostIps = new ArrayList<>();
		HashMap<String,SysClusterHost> hostMap=new HashMap<>();
		for (SysClusterHost host : hosts){
			if(flag){
				targets.add(host.getIp());
			}
			//dqy  如果targets包含hostip，并且hostLock主机锁为true，代表有其他play操作当前targets主机，此次play不能执行
			if (targets.contains(host.getIp())&&host.getHostLock()){
				lockHostIps.add(host.getIp());
			}
			hostMap.put(host.getIp(),host);
		}

		if (!lockHostIps.isEmpty()) 		{throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK, lockHostIps,"部分主机已锁住");}

		if (!hostMap.keySet().containsAll(targets))				{throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK, targets,"主机不存在");}

		// 对主机加锁
		for (String ip:targets){
			SysClusterHost host = hostMap.get(ip);
			host.setHostLock(true);
			clusterHostDao.save(host);
		}
		return hostMap;
	}

	@Override
	@Transactional
	public String exec(List<String> targets, String playCode)  {
		SysInstallPlayExec exec = new SysInstallPlayExec(playCode);
		playExec(exec,targets);
		return exec.getUuid();
	}

	private void playExec(SysInstallPlayExec exec, List<String> targets) throws ClusterException {
		//获取play
		Optional<SysInstallPlay> playOpt = installPlayDao.findById(exec.getPlayCode());
		if (!playOpt.isPresent()||!playOpt.get().getStatus())               {throw new ClusterException(ReturnCode.CODE_CLUSTER_PLAY_NOT_EXIST,"不存在该任务");}
		SysInstallPlay play = playOpt.get();

		//根据play中的lockType，获得需要操作的目标主机    true全量主机   false当前targets主机
		HashSet<String> ips = play.getLockType() || null == targets? new HashSet<>():new HashSet<>(targets);

		// step 1 主机校验，主机锁校验   {ip:SysClusterHost}
		HashMap<String,SysClusterHost> hashMap = lockHosts(ips);

		// step 2  constructPlayExec  通过playExec构造器完成 执行参数的构造
		PlayExecFactory playExecFactory = new PlayExecFactory(play,ips,hashMap,play.getPlayName());
		playExecFactory.constructPlayExec(exec);

		if (!exec.getFlag())                                {throw new ClusterException(ReturnCode.CODE_CLUSTER_PLAY_CANT_EXEC, playExecFactory.getErrorMsg(),"当前的集群状态不具备执行此任务！");}

		// step 3 insertPlayExec  确保exec的主键自动生成
		installPlayExecDao.save(exec);

		// step 4 callDriver
		try{
			TaskManager.create(exec);
		}catch (Exception e){
			e.printStackTrace();
			throw new ClusterException(ReturnCode.CODE_CLUSTER_BOOTSTRAP_CALL_FAIL,"执行任务调度失败！");
		}
	}

	@Override
	@Transactional
	public void resume(String uuid) {
		Optional<SysInstallPlayExec> execOpt = installPlayExecDao.findById(uuid);
		if (!execOpt.isPresent())				{throw new ClusterException(ReturnCode.CODE_CLUSTER_TASK_NOT_EXIST,"该任务不存在");}

		SysInstallPlayExec exec = execOpt.get();
		if (!exec.isStop())					{throw new ClusterException(ReturnCode.CODE_CLUSTER_TASK_NOT_FAILED,"该任务状态不可恢复执行");}

		playExec(exec,exec.getTargetIps());
	}

	@Override
    @Transactional
	public void pause(String uuid) {
        Optional<SysInstallPlayExec> execOpt = installPlayExecDao.findById(uuid);
        if (!execOpt.isPresent())				{throw new ClusterException(ReturnCode.CODE_CLUSTER_TASK_NOT_EXIST,"该任务不存在");}
        SysInstallPlayExec exec = execOpt.get();

        if (!exec.isRun())					{throw new ClusterException(ReturnCode.CODE_CLUSTER_PLAY_NOT_RUNNING,"当前任务未在运行中");}

		TaskManager.destroy(uuid);
	}

	@Override
	public ExecProcess query(String uuid)  {
		SysInstallPlayExec exec = TaskManager.get(uuid);
		if (null == exec){
			Optional<SysInstallPlayExec> optional = installPlayExecDao.findById(uuid);
			if (!optional.isPresent())				{throw new ClusterException(ReturnCode.CODE_CLUSTER_TASK_NOT_EXIST,"该任务不存在");}

			exec = optional.get();

			exec.setPlaybooks(initPlaybooks(exec.getPlayCode()));
		}
		return new ExecProcess(exec);
	}

    @Override
    public List<SysInstallPlaybook> initPlaybooks(String playCode) {
        return installPlaybookDao.findByPlayCodeOrderByIndex(playCode);
    }

	@Override
	public String getLatestUuid(String playCode) {
		List<SysInstallPlayExec> execList = installPlayExecDao.findAllByPlayCodeOrderByCreateDateDesc(playCode);
		if (execList.size()>0){
			return execList.get(0).getUuid();
		}else{
			throw new ClusterException(ReturnCode.CODE_CLUSTER_TASK_NOT_EXIST,"该任务不存在");
		}
	}

}
