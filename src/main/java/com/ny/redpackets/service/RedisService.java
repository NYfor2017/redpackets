package com.ny.redpackets.service;

import com.ny.redpackets.constant.CommonConstant;
import com.ny.redpackets.dao.OrderDao;
import com.ny.redpackets.dao.PacketDao;
import com.ny.redpackets.dao.RedisDao;
import com.ny.redpackets.model.Packet;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author N.Y
 * @date 2019-06-29 17:29
 */
@Service
public class RedisService {

    private Logger logger = LoggerFactory.getLogger(RedisService.class);

    /**
     * GOOD_SIZE：红包的个数
     * WAIT_QUEUE_SIZE：等待队列的大小
     * size：请求线程数目
     * isFinish：红包活动是否已经结束了
     * repeatCount：重复请求抢红包的线程数目
     * requestCount：请求抢红包的线程数目
     */
    public static final int GOOD_SIZE = 1000;
    private int WAIT_QUEUE_SIZE = GOOD_SIZE * 3;
    private AtomicInteger size = new AtomicInteger();
    private volatile boolean isFinish = false;
    private volatile int repeatCount = 0;
    private volatile int requestCount = 0;

    @Autowired
    ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private PacketDao packetDao;
    @Autowired
    private OrderDao orderDao;
    @Autowired
    private RedisDao redisDao;

    //阻塞队列
    private BlockingQueue<String> requestQueue = new ArrayBlockingQueue<>(WAIT_QUEUE_SIZE);

    //设置一个阻塞队列，将号码放进去，如果队列里面的数目已经超过了WAIT_QUEUE_SIZE，则tel不能进入队列里面
    public boolean joinRequestQueue(String tel){
        if(size.getAndIncrement()<=WAIT_QUEUE_SIZE){
            try{
                requestQueue.put(tel);  //如果等待数目还没有满，则可以将tel放入阻塞队列中
            }catch (InterruptedException e){
                logger.error(e.getMessage(), e);  //不然的话就打印错误信息
            }
            return true;
        }
        return false;
    }

    public void start(String packetName){
        logger.info("活动开始");
        logger.info("缓存中红包个数为："+String.valueOf(redisDao.getPacketsList().size(packetName)));
        isFinish = false;
        while(!isFinish){
            if(!requestQueue.isEmpty()){
                //从阻塞队列拿出一个tel
                //因为已经在ThreadPoolConfig配置好了ThreadPoolTaskExecutor，所以在此利用线程池创建线程
                String tel = requestQueue.poll();
                threadPoolTaskExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        ++requestCount;
                        //如果该tel没有在成功列表中，则为其分配一个红包
                        if(!redisDao.isMemberOfSuccessList(tel)){
                            Object packetId = redisDao.getPacketsList().leftPop(packetName);
                            if(StringUtils.isEmpty(packetId)){
                                //没有直接设置isFinish为true是因为可能同时存在其他线程在redis得到了packetId
                                //但是因为异常的原因，导致packetId归还到redis里面去了，这样其他用户可以再次发送请求
                                //如果tel抢不到红包，则加入失败队列
                                logger.info(tel+"没有抢到红包");
                                redisDao.addToFailedList(tel);
                            }else{
                                try{
                                    //如果这个号码在成功队列，那么就用事务检查它是否已经抢到过红包
                                    int result = packetDao.bindRedPacket(packetId.toString(), tel);
                                    if(result>=0){
                                        logger.info(tel+"抢到红包！");
                                        redisDao.addToSuccessList(tel);
                                    }else{
                                        logger.info(tel+"已经抢过红包了");
                                        throw new Exception();
                                    }
                                }catch (Exception e){
                                    //如果重复插入手机号，会出现主键重复异常，这时候恢复库存，由于前端的控制，理论上不会重复提交手机号
                                    //但为了防止意外或者恶意请求的发送，因此try catch
                                    //对于拦截重复手机号，后端做了三层拦截
                                    //第一层：redisDao.isMemberOfSuccessList;
                                    //第二层：存储过程中根绝手机号查询i_order表是否已经存在数据
                                    //第三层：存储过程中，由于两个线程同时请求，有可能都会从i_order中查不到，最终都会执行插入操作
                                    //          但由于tel是不允许重复的，那么就依靠重复异常抛出错误了
                                    logger.info(tel+"抢红包出现了异常，现在恢复库存");
                                    ++requestCount;
                                    redisDao.getPacketsList().rightPush(packetName, packetId);
                                }
                            }
                        }else{
                            ++repeatCount;
                            logger.warn(tel+"已经抢成功过一次！");
                        }
                    }
                });
            }
        }
    }

    public void stop(String packetName){
        System.out.println("还剩"+redisDao.getPacketsList().size(packetName));
        System.out.println("已有"+redisDao.getSizeOfSuccessList());
        System.out.println("有"+repeatCount+"次重复请求");
        System.out.println("有"+requestCount+"次请求参与抢红包");
        System.out.println("还剩"+requestQueue.size()+"个请求未被处理");
        System.out.println("有"+redisDao.getSizeOfFailedList());
        System.out.println("请求队列中共有"+size.get()+"名用户");
        isFinish = true;
        //扫尾阶段
        redisDao.delete(CommonConstant.RedisKey.SUCCESS_LIST);
        redisDao.delete(CommonConstant.RedisKey.FAILED_LIST);
        redisDao.delete(packetName);
        size.set(0);
        repeatCount = 0;
        requestCount = 0;
        requestQueue.clear();
        orderDao.deleteAll();
    }

    public Response checkRedPacket(String tel){
        Response response = new Response();
        String resultMsg = "";
        if(redisDao.isMemberOfSuccessList(tel)){
            Packet packet = packetDao.findByTel(tel);
            resultMsg += "恭喜您抢到了一份金额为："+packet.getValue()+"元的红包，现已存入您的账户";
            response.setStatus(2000);
            response.setMessage(resultMsg);
        }else if(redisDao.isMemberOfFailedList(tel)){
            resultMsg += "很遗憾失败了";
            response.setStatus(2000);
            response.setMessage(resultMsg);
        }else{
            response.setStatus(2001);
            response.setMessage("继续等待");
        }
        return response;
    }
}
