package com.ny.redpackets.constant;

/**
 * @author N.Y
 * @date 2019-06-29 16:32
 * 用于redis缓存的队列key
 */

public class CommonConstant {

    public interface RedisKey{
        String SUCCESS_LIST = "success_list";
        String FAILED_LIST = "fail_list";
    }
}
