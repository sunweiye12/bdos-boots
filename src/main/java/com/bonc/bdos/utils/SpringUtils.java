package com.bonc.bdos.utils;

import org.springframework.context.ApplicationContext;

public class SpringUtils {

    private static ApplicationContext applicationContext;

    private SpringUtils() {}

    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    // 通过name获取 Bean.
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    // 通过class获取Bean.
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}
