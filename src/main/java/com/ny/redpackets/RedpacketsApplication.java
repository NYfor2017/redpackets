package com.ny.redpackets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RedpacketsApplication {

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(RedpacketsApplication.class);
        SpringApplication.run(RedpacketsApplication.class, args);
        logger.info("redpackets活动开启啦~~");

    }

}
