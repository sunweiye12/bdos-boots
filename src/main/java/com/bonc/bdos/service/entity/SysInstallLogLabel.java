package com.bonc.bdos.service.entity;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;

@Entity
@Table(name = "`sys_install_log_label`")
@Data
public class SysInstallLogLabel implements Serializable {
	private static final long serialVersionUID = -2102082194607883083L;

	/**
	 *  定义一个默认的playbook,一个公共的label配置都配置到这下面
	 *
	 *  个性的配置在每个独立的playbook名字下面配置就行
	 */
	public static final String DEFAULT_PLAYBOOK="playbook";

	public static final char MESSAGE = '0';
	public static final char STYLE = '1';
	public static final char PERCENT = '2';

	@Id
	@Column(name = "`id`")
	private Long id;

	@Column(name = "`playbook`",length = 32,nullable = false)
	private String playbook;

	/**
	 *  标签正则字符串，用于匹配日志输出
	 */
	@Column(name = "`label_regex`",nullable = false)
	private String labelRegex;

	/**
	 *  对应匹配信息的异常表述  可以是颜色，可以是特定的描述信息，可以是0-100的执行进度
	 */
	@Column(name = "`label_handle`",length = 64,nullable = false)
	private String labelHandle;

	/**
	 *  对应日志信息的类型，0 设置异常信息，1，设置颜色信息；2  0-100的进度数值
	 */
	@Column(name = "`label_type`",nullable=false)
	private char labelType;

	/**
	 *  用于格式化message信息的时候获取参数的序号列表
	 *  0: 代表整个正则匹配的字符串数据
	 *  1...n 代表第n个正则group匹配的字符串
	 *
	 *  如果配置为 3,1,2 那么代表你的正则至少又三个匹配组，他们将对应组的顺序按照你的配置传给你，这样你就能按着自己的期望格式化字符串了
	 *  如果为null 将所有数据 按顺序列出来
	 */
	@Column(name="`group_order`")
	private String groupOrder;

	@Column(name = "`memo`")
	private String memo;

	@Transient
	private Pattern pattern;

	@Transient
	private List<Integer> orders;

	public void setLabelRegex(String labelRegex) {
		this.labelRegex = labelRegex;
		if (null!=labelRegex){
			this.pattern = Pattern.compile(labelRegex);
		}
	}

	public void setGroupOrder(String groupOrder) {
		this.groupOrder = groupOrder;
		// 通过json 将序号列表转化成为数组
		this.orders = JSON.parseArray("["+(StringUtils.isEmpty(groupOrder) ?"0":groupOrder)+"]",Integer.class);
	}
}
