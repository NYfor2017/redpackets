package com.ny.redpackets.controller;

import com.ny.redpackets.service.PacketService;
import com.ny.redpackets.service.RedisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Random;

/**
 * @author N.Y
 * @date 2019-06-29 22:12
 */
@RestController
public class IndexController {

    @Resource
    private RedisService redisService;

    @Resource
    private PacketService packetService;

    @GetMapping("/seckill")
    public String requestRedPacket(){
        if(redisService.joinRequestQueue(""+ new Random().nextInt(200000))){
            return "success";
        }
        return "failed";
    }

    @GetMapping("/check")
    public String check(@RequestParam("tel") String tel){
        return redisService.checkRedPacket(tel).getMessage();
    }


    @GetMapping("/start")
    public String start(@RequestParam("packetName")String packetName){
        packetService.start(packetName);
        return "success";
    }

    @GetMapping("/stop")
    public String stop(@RequestParam("packetName")String packetName){
        redisService.stop(packetName);
        return "success";
    }

}
