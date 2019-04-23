package com.bonc.bdos.utils;

import org.apache.commons.lang.StringUtils;

/**
 * 单位转换工具类 2017年6月8日14:54:37
 *
 * @author daien
 *
 */
public class ConvertUtil {

	public static double convertCpu(String cpu) {
		if (StringUtils.isEmpty(cpu)) {
			return 0d;
		}
		double ret;
		if (cpu.contains("m")) {
			ret = Double.parseDouble(cpu.replace("m", "")) / 1000d;
		} else {
			ret = Double.parseDouble(cpu);
		}

		return ret;
	}

	/**
	 * 单位为G 进制为1000
	 * 
	 * @param memory
	 * @return double
	 */
	public static double convertMemory(String memory) {
		double ret;
		try {
			ret = Double.parseDouble(memory);
			return ret;
		} catch (NumberFormatException e) {
			ret = parseMemory(memory);
			ret = ret * Math.pow(10, -9);
			return ret;
		}
	}

	/**
	 * 单位为G 进制为1024
	 * 
	 * @param memory
	 * @return
	 */
	public static double convertMemoryBy2(String memory) {
		double ret;
		try {
			ret = Double.parseDouble(memory);
			return ret;
		} catch (NumberFormatException e) {
			ret = parseMemory(memory);
			ret = ret / Math.pow(2, 30);
			return ret;
		}
	}

	/**
	 * convertMemory:转换内存值字符串为无单位数值. <br/>
	 * 单位为字节
	 * 
	 * @param memory
	 * @return double
	 */
	public static double parseMemory(String memory) {
		if (StringUtils.isEmpty(memory)) {
			return 0d;
		}
		double ret;
		if (memory.endsWith("n")) {
			ret = Double.parseDouble(memory.replace("n", "")) * Math.pow(10, -9);
		} else if (memory.endsWith("u")) {
			ret = Double.parseDouble(memory.replace("u", "")) * Math.pow(10, -6);
		} else if (memory.endsWith("m")) {
			ret = Double.parseDouble(memory.replace("m", "")) * Math.pow(10, -3);
		} else if (memory.endsWith("k")) {
			ret = Double.parseDouble(memory.replace("k", "")) * Math.pow(10, 3);
		} else if (memory.endsWith("K")) {
			ret = Double.parseDouble(memory.replace("K", "")) * Math.pow(10, 3);
		} else if (memory.endsWith("M")) {
			ret = Double.parseDouble(memory.replace("M", "")) * Math.pow(10, 6);
		} else if (memory.endsWith("G")) {
			ret = Double.parseDouble(memory.replace("G", "")) * Math.pow(10, 9);
		} else if (memory.endsWith("T")) {
			ret = Double.parseDouble(memory.replace("T", "")) * Math.pow(10, 12);
		} else if (memory.endsWith("P")) {
			ret = Double.parseDouble(memory.replace("P", "")) * Math.pow(10, 15);
		} else if (memory.endsWith("E")) {
			ret = Double.parseDouble(memory.replace("E", "")) * Math.pow(10, 18);
		} else if (memory.endsWith("Ki")) {
			ret = Double.parseDouble(memory.replace("Ki", "")) * Math.pow(2, 10);
		} else if (memory.endsWith("Mi")) {
			ret = Double.parseDouble(memory.replace("Mi", "")) * Math.pow(2, 20);
		} else if (memory.endsWith("Gi")) {
			ret = Double.parseDouble(memory.replace("Gi", "")) * Math.pow(2, 30);
		} else if (memory.endsWith("Ti")) {
			ret = Double.parseDouble(memory.replace("Ti", "")) * Math.pow(2, 40);
		} else if (memory.endsWith("Pi")) {
			ret = Double.parseDouble(memory.replace("Pi", "")) * Math.pow(2, 50);
		} else if (memory.endsWith("Ei")) {
			ret = Double.parseDouble(memory.replace("Ei", "")) * Math.pow(2, 60);
		} else if (memory.endsWith("kB")) {
			ret = Double.parseDouble(memory.replace("kB", "")) * Math.pow(2, 10);
		} else if (memory.endsWith("KB")) {
			ret = Double.parseDouble(memory.replace("KB", "")) * Math.pow(2, 10);
		} else if (memory.endsWith("MB")) {
			ret = Double.parseDouble(memory.replace("MB", "")) * Math.pow(2, 20);
		} else if (memory.endsWith("GB")) {
			ret = Double.parseDouble(memory.replace("GB", "")) * Math.pow(2, 30);
		} else if (memory.endsWith("TB")) {
			ret = Double.parseDouble(memory.replace("TB", "")) * Math.pow(2, 40);
		} else if (memory.endsWith("PB")) {
			ret = Double.parseDouble(memory.replace("PB", "")) * Math.pow(2, 50);
		} else if (memory.endsWith("EB")) {
			ret = Double.parseDouble(memory.replace("EB", "")) * Math.pow(2, 60);
		} else {
			ret = Double.parseDouble(memory);
		}
		return ret;
	}
}
