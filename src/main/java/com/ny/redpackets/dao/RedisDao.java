package com.ny.redpackets.dao;

import com.ny.redpackets.constant.CommonConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author N.Y
 * @date 2019-06-29 16:20
 */
@Repository
public class RedisDao {
    @Autowired
    private RedisTemplate redisTemplate;

    //将ele加入到success_list集合中
    public void addToSuccessList(String ele){
        redisTemplate.opsForSet().add(CommonConstant.RedisKey.SUCCESS_LIST, ele);
    }

    //确认ele是否在success_list
    public boolean isMemberOfSuccessList(String ele){
        return redisTemplate.opsForSet().isMember(CommonConstant.RedisKey.SUCCESS_LIST, ele);
    }

    //将ele加入到failed_list集合中
    public void addToFailedList(String ele){
        redisTemplate.opsForSet().add(CommonConstant.RedisKey.FAILED_LIST, ele);
    }

    //将ele从failed_list集合中移除
    public void removeFromFailedList(String ele){
        redisTemplate.opsForSet().remove(CommonConstant.RedisKey.FAILED_LIST, ele);
    }

    //确认ele是否在failed_list中
    public boolean isMemberOfFailedList(String ele){
        return redisTemplate.opsForSet().isMember(CommonConstant.RedisKey.FAILED_LIST, ele);
    }

    //得到success_list的大小
    public Long getSizeOfSuccessList(){
        return redisTemplate.opsForSet().size(CommonConstant.RedisKey.SUCCESS_LIST);
    }

    //得到failed_list的大小
    public Long getSizeOfFailedList(){
        return redisTemplate.opsForSet().size(CommonConstant.RedisKey.FAILED_LIST);
    }

    //得到list
    public ListOperations getPacketsList(){
        return redisTemplate.opsForList();
    }

    //删除某个键，像是failed_list或者success_list
    public void delete(String key){
        redisTemplate.delete(key);
    }

}
