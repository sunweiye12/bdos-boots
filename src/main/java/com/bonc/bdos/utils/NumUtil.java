package com.bonc.bdos.utils;

import java.text.DecimalFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NumUtil {
    private static Logger logger = LoggerFactory.getLogger(NumUtil.class); // 日志记录

    private NumUtil() {}

    public static String formatUnit(int value, String oldUnit, String toUnit) {

        float val = formatByUnit(value, oldUnit, toUnit);

        return formatNum(val);
    }

    /**
     * String转为double
     *
     * @param str
     * @return
     */
    public static double toDouble(String str) {

        Double val = 1.00;
        try {
            val = Double.valueOf(str);
        } catch (Exception e) {
            logger.error("String转为double时出错：" + e);
        }
        return val;
    }

    /**
     * long转为int
     *
     * @param longVal
     * @return
     */
    public static int toInteger(Long longVal) {

        int val = 0;

        try {
            val = longVal.intValue();
        } catch (Exception e) {
            logger.error("long转为int时出错：" + e);
        }
        return val;
    }

    public static String getPercent(String usedResource, String allResource) {

        String percent = "0.00";

        try {

            double used = toDouble(usedResource);
            double all = toDouble(allResource);
            percent = formatNum((used / all) * 100);

        } catch (Exception e) {
            logger.error("计算百分比出错：" + e);
        }

        return percent;
    }

    public static String formatPercent(double num) {

        return format(num, "#0.00%");
    }

    public static String formatNum(double num) {

        return format(num, "#0.00");
    }

    public static String format(double num, String format) {
        DecimalFormat df = new DecimalFormat(format);
        return df.format(num);
    }

    /**
     * 默认参数单位是M
     *
     * @param i
     * @return
     */
    public static String formatUnit(double i) {
        String ret = i + "M";
        if (i > 1024) {

            ret = formatNum(i / 1024) + "G";
        }

        return ret;
    }

    /**
     * 处理带单位的值
     *
     * @param value
     *            原数据值
     * @param oldUnitStr
     *            原数据单位
     * @param newUnitStr
     *            转换后的单位
     * @return newValue 转换后的数据值
     */
    public static float formatByUnit(float value, String oldUnitStr, String newUnitStr) {
        // unit : MB\M,GB\G,TB\T
        long oldUnit = unitStringToInt(oldUnitStr);
        long newUnit = unitStringToInt(newUnitStr);

        if (oldUnit == 0 && newUnit == 0) {
            return value;
        }

        return value * oldUnit / newUnit;
    }

    public static String formatByUnit(double value, String oldUnitStr, String newUnitStr) {

        float val = formatByUnit((float) value, oldUnitStr, newUnitStr);

        return formatNum(val);
    }

    /**
     * 通过String单位转换为int单位，用作数值转换
     *
     * @param unitStr
     * @return
     */
    public static long unitStringToInt(String unitStr) {
        long b = 1;
        long kb = 1024 * b;
        long mb = 1024 * kb;
        long gb = mb * 1024;
        long tb = gb * 1024;
        long unit = 0;

        if ("Bit".equalsIgnoreCase(unitStr) || "B".equalsIgnoreCase(unitStr)) {
            unit = b;
        }
        if ("KB".equalsIgnoreCase(unitStr) || "K".equalsIgnoreCase(unitStr)) {
            unit = kb;
        }
        if ("MB".equalsIgnoreCase(unitStr) || "M".equalsIgnoreCase(unitStr)) {
            unit = mb;
        }
        if ("GB".equalsIgnoreCase(unitStr) || "G".equalsIgnoreCase(unitStr)) {
            unit = gb;
        }
        if ("TB".equalsIgnoreCase(unitStr) || "T".equalsIgnoreCase(unitStr)) {
            unit = tb;
        }

        return unit;
    }

    /**
     * 判断数值是否等于0
     *
     * @param d
     * @return
     */
    public static boolean isZero(double d) {
        double e = 0.000000000001;
        return Math.abs(d) < e;
    }

}
