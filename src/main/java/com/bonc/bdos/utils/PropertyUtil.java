package com.bonc.bdos.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PropertiesLoaderUtils;

public class PropertyUtil {
    private static Map<String, String> propertiesMap;

    private static Logger logger = LoggerFactory.getLogger(PropertyUtil.class);

    private PropertyUtil() {}

    // Default as in PropertyPlaceholderConfigurer

    public static void processProperties(Properties props) {

        propertiesMap = new HashMap<>();
        for (Object key : props.keySet()) {
            String keyStr = key.toString();

            try {
                // PropertiesLoaderUtils的默认编码是ISO-8859-1,在这里转码一下
                propertiesMap.put(keyStr, new String(props.getProperty(keyStr).getBytes("ISO-8859-1"), "utf-8"));
            } catch (UnsupportedEncodingException e) {
                logger.error("UnsupportedEncodingException 错误信息 ： " + e.getMessage(), e);
            } catch (java.lang.Exception e) {
                logger.error("错误信息 ： " + e.getMessage(), e);
            }
        }
    }

    public static void loadAllProperties() {
        try {

            Properties properties = PropertiesLoaderUtils.loadAllProperties("bdos.properties");
            processProperties(properties);
        } catch (IOException e) {
            logger.error("IOException 错误信息 ： " + e.getMessage(), e);
        }
    }

    public static String getProperty(String name) {
        try {
            return propertiesMap.get(name);
        } catch (Exception e) {
            logger.error("错误信息 ： " + e.getMessage(), e);
            return null;
        }
    }
}
