package com.ny.redpackets.service;

/**
 * @author N.Y
 * @date 2019-06-29 21:26
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Component（把普通pojo实例化到spring容器中，相当于配置文件中的<bean id="" class=""/>）
 * 此类作为定时任务
 */
@Component
public class TaskService {

    @Autowired
    private PacketService packetService;

    @Scheduled(cron = "0 50 20 * * ?")
    public void taskStart(){
        packetService.start("red");
    }

    //5分钟之后结束
    @Scheduled(cron = "0 55 20 * * ?")
    public void taskStop(){
        packetService.stop("red");
    }
}
