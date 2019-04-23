package com.bonc.bdos.utils;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeUtil {

    private static Logger logger = LoggerFactory.getLogger(RuntimeUtil.class);

    private RuntimeUtil() {}

    public static boolean exec(String command) {
        Process proc;

        try {
            logger.info("----start command : " + command);
            proc = Runtime.getRuntime().exec(command);
            proc.waitFor();

        } catch (IOException e) {
            logger.error("----start python error form util , " + e.getMessage(), e);

            return false;
        } catch (InterruptedException e) {
            logger.error("----start python error form util , " + e.getMessage(), e);
            Thread.currentThread().interrupt();
            return false;
        }

        return true;
    }
}
