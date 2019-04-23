package com.bonc.bdos.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepUtil {

    private static Logger logger = LoggerFactory.getLogger(SleepUtil.class); // 日志记录

    private SleepUtil() {}

    public static void sleep(int sleepTime) {
        logger.info("---------------------sleep " + sleepTime + " ms ------------------------");
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            logger.error("sleep error: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
