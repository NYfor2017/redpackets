抢红包系统是一个基于Springboot、JPA、Redis的并发系统，使用JMeter进行压力测试，可以承载10000个线程的并发量。

Springboot提供的组件很多，其中还使用了定时器来设置红包发行时间。

------

抢红包1.0：主要是实现抢红包功能

这里的主要难点在于，Redis缓存和并发的使用。

Redis是将信息存入内存作为缓存机制，也可以调整为将缓存信息存入数据库中。

在使用过程中，由于这里要使用到Redis事务，所以先对Redis进行配置。

```Java
@Configuration
public class RedisConfig {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    //因为spring中是以方法为单位进行bean的定义，所以这就算是一个bean了，Redis的配置有两种，一种是StringRedis Template，另一种是Redis Template
    //这两种方式的序列化方式不一样，前者是string的序列化方式，后者是用jdk的序列化方式
    //下述可以看出是使用StringRedisTemplate
    @Bean
    public RedisTemplate<String, Object> functionDomainRedisTemplate(){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        //设置序列化方式
        redisTemplate.setKeySerializer(new StringRedisSerializer());  
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer());

        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }
}
```

接下来是用Redis进行DAO层的设置，但是这里的DAO层持久化的位置是在内存，也就是设置Redis缓存。

```java
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
```

Redis支持的是原子化存储数据，所以为了能够支持并发，我们这里引用ThreadPool来进行并发处理。首先先配置好ThreadPool。

```java
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

```

然后在Service层使用Redis的缓存功能。

```java
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

```

其实到这里，可能有些小伙伴还是比较懵，为什么一定要分success_list和failed_list，为什么要redisTemplate.opsForList()可以直接得到Set的数目？这里先留个坑哈。

