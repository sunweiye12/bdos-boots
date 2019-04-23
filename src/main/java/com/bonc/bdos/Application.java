package com.bonc.bdos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.bonc.bdos.utils.SpringUtils;

/**
 *
 * @author ruzz
 *
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) throws Exception {

        SpringApplication application = new SpringApplication(Application.class);
        SpringUtils.setApplicationContext(application.run(args));

    }
}
