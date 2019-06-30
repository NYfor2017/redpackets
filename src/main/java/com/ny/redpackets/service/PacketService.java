package com.ny.redpackets.service;

import com.ny.redpackets.dao.PacketDao;
import com.ny.redpackets.dao.RedisDao;
import com.ny.redpackets.model.Packet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author N.Y
 * @date 2019-06-29 17:28
 */
@Service
public class PacketService {

    @Autowired
    private PacketDao packetDao;

    @Autowired
    private RedisDao redisDao;

    @Autowired
    private RedisService redisService;

    /**
     * 初始化强抢红包的环境，设定好红包数量，然后随机生成红包id和红包value，批量存入数据库中
     * @param packetName
     * @return
     */
    public List<Packet> initDataBase(String packetName){
        List<Packet> packets = packetDao.findAll();
        if(packets.isEmpty()){
            List<Packet> list = new ArrayList<>(RedisService.GOOD_SIZE);
            for(int i=0;i<RedisService.GOOD_SIZE;i++){
                Packet packet = new Packet();
                packet.setId(UUID.randomUUID().toString());
                packet.setName(packetName);
                packet.setValue(new Random().nextInt(100));
                list.add(packet);
            }
            packets = packetDao.saveAll(list);
        }
        return packets;
    }

    /**
     * 根据给定的红包名，启动特定的redisService
     * @param packetName
     * @return
     */
    public String start(String packetName){
        List<Packet> packets = initDataBase(packetName);
        List<String> packetIds = packets.stream().map(Packet::getId).collect(Collectors.toList());
        redisDao.getPacketsList().leftPushAll(packetName, packetIds);
        redisService.start(packetName);
        return "success";
    }

    /**
     * 关闭特定红包活动
     * @param packetName
     * @return
     */
    public String stop(String packetName){
        redisService.start(packetName);
        return "success";
    }


}
