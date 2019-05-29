package com.bonc.bdos.service.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.bonc.bdos.utils.DateUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "`sys_install_play_exec`")
@ApiModel(value = "任务信息",description = "执行任务的详细信息")
@Data
public class SysInstallPlayExec implements Serializable {

	private static final long serialVersionUID = -2102082194607883083L;

	public static final char INIT = '0';
	public static final char RUNNING = '1';
	public static final char SUCCESS = '2';
	public static final char FAILED = '3';
	public static final char PAUSE = '4';

	public SysInstallPlayExec() {
		setCreateDate(new Timestamp(DateUtil.getCurrentTimeMillis()));
	}

	/**
	 *  curIndex 任务执行的playbook序号，playbook的序号从1 开始递增，所以 curIndex=0 代表该任务还没有失败过
	 * @param playCode 任务的编码
	 */
	public SysInstallPlayExec(String playCode) {
		this.uuid = UUID.randomUUID().toString().replace("-","");
		this.playCode = playCode;
		this.curIndex = 0;
		this.createDate = new Timestamp(DateUtil.getCurrentTimeMillis());
	}

	private static final HashMap<Character,String> STATUS_DESC = new HashMap<Character,String>(){{
		put(SysInstallPlayExec.INIT,"任务未开始执行");
		put(SysInstallPlayExec.RUNNING,"任务正在执行中...");
		put(SysInstallPlayExec.SUCCESS,"任务执行成功");
		put(SysInstallPlayExec.FAILED,"任务执行失败");
	}};


	@Id
	private String uuid;

	@Column(name = "`play_code`",length = 32)
	private String playCode;

	@Column(name = "play_name",length = 32)
	@ApiModelProperty(hidden = true)
	private String playName;
	
	@Column(name = "`cur_index`")
	private int curIndex;

	@Column(name = "`percent`")
	private int percent;
	
	@Column(name = "`cmd`" ,columnDefinition="LONGTEXT")
	@ApiModelProperty(hidden = true)
	private String cmd;
	
	@Column(name = "`message`",length = 512)
	@ApiModelProperty(hidden = true)
	private String message;
	
	@Column(name = "`create_date`")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(hidden = true)
	private Timestamp createDate;

	@Column(name = "`begin_date`")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(hidden = true)
	private Timestamp beginDate;
	
	@Column(name = "`end_date`")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(hidden = true)
	private Timestamp endDate;

	/* 集群操作跟踪状态
	 * 0: 未开始执行	(ExecService.exec 执行时候数据插入的初始状态) ;
	 * 1: 执行中		(CallbackService.start 接口调用会更新为执行中); 
	 * 2: 执行成功		(CallbackService.finish 执行完成之后根据执行结果更新); 
	 * 3: 执行失败		(CallbackService.finish 执行完成之后根据执行结果更新) 
	 */
	@Column(name = "`status`")
	@ApiModelProperty(hidden = true)
	private char status;

	/**
	 *  存储任务执行时候的主机列表，用于继续执行的时候锁重新计算
	 */
	@Column(name = "`targets`",length = 2048)
	@ApiModelProperty(hidden = true)
	private String targets;

	@Column(name = "`stdout`",columnDefinition="LONGTEXT")
	@ApiModelProperty(hidden = true)
	private String stdout;

	/**
	 *  playExec 构造标识  默认成功，如果构造失败设置未false
	 *
	 */
	@Transient
	@ApiModelProperty(hidden = true)
	private Boolean flag = true;

	/**
	 *  定义该playExec 对应的所有playbook报文数据
	 */
	@Transient
	@ApiModelProperty(hidden = true)
	private List<SysInstallPlaybook> playbooks = new ArrayList<>();

	public void addPlaybook(SysInstallPlaybook playbook) {
		this.playbooks.add(playbook);
	}

	@ApiModelProperty(hidden = true)
	public boolean isRun() {
		return this.status == SysInstallPlayExec.RUNNING;
	}

	@ApiModelProperty(hidden = true)
	public List<String> getTargetIps(){
		return JSON.parseArray(this.targets,String.class);
	}

	@ApiModelProperty(hidden = true)
	public void setTargetsJson(List<String> targets) {
		this.targets = JSON.toJSONString(targets);
	}

	@ApiModelProperty(hidden = true)
	public boolean isFailed() {
		return this.status == SysInstallPlayExec.FAILED;
	}

	@ApiModelProperty(hidden = true)
	public boolean isStop() {
		return this.status == SysInstallPlayExec.FAILED||this.status == SysInstallPlayExec.PAUSE;
	}

	@ApiModelProperty(hidden = true)
	public boolean isStart() {
		return this.status == SysInstallPlayExec.INIT;
	}

	public String getStdout() {
		return stdout==null?"":stdout;
	}

}
