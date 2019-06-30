package com.ny.redpackets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author N.Y
 * @date 2019-06-29 17:21
 * 线程池用于并发环境
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(){
        //采用默认的线程拒绝策略，处理出错时直接抛出RejectedExecutionException
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(8);
        threadPoolTaskExecutor.setMaxPoolSize(16);
        threadPoolTaskExecutor.setKeepAliveSeconds(3000);
        threadPoolTaskExecutor.setQueueCapacity(2000);
        return threadPoolTaskExecutor;
    }

}
