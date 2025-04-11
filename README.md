








# 知巢智能应用平台
**采用技术栈：SpringBoot、MySQL、Redis、Caffeine本地缓存、Mybatis-Plus、ShardingSphere
分库分表、SSE、RxJava、Schedule 定时任务、阿里云OOS、Chat GLM 等**  

**整体架构：SpringMVC 三层架构**
## 项目介绍
本项目是一个智能应用平台，旨在为用户提供一个集答题、创作和分享于一体的综合性平台。
用户可以在平台上参与答题、查看评分结果，并作为创作者发布自己的应用。
平台还支持 AI 生成题目和详细的评分报告，提升了用户体验和内容丰富度。

用户可以在主页看到平台中所有的应用，并参与答题，根据答题情况获得对应的评分结果。
还可以作为创作者发布自己的应用，并且支持高度自定义，创作者能自定义应用图标、题目内容、评分规则等。
在创作题目内容时，可以选择使用平台的 AI 大模型来生成题目，平台会将生成好的题目解析为 JSON
格式，并推送到客户端进行实时填充，让创作者可以随时对题目内容进行修改。
创作者在完成自己的应用之后，可以提交审核，此时管理员审核通过之后，应用会自动发布到平台主页中，可以被所有用户访问。
用户在作答之后会根据创作者所制定的评分规则，得到自己的评分结果，如果创作者选用 AI 评分，那用户将会得到一份非常详细的评分报告。
用户可以在个人中心查看自己过去所有的答题记录；此外，用户还可以对自己喜欢的应用进行点赞或是分享自己对该应用的看法，也可以通过二维码的形式对应用进行分享。  

在管理员方面，管理员可以对所有用户进行管控，审核创作者提交的应用信息、评分规则、试题等。管理员可以查看所有用户的答题记录，
并且根据单个应用的热门答案进行统计分析，并把它们可视化为饼状图。 
  
  
## 整体流程图
![流程图.png](src/main/resources/script/%E6%B5%81%E7%A8%8B%E5%9B%BE.png)
## 功能模块
- 架构基础通用工具类
- 用户登录与权限校验
- 应用模块
- 答题模块
- 评分模块
- 点赞模块
- AI 模块
- 评论模块

### 用户登录与权限校验

- ### **用户登录**  

用户可以注册账号进行登录，后端校验通过后可以正常使用平台的所有功能，在此之前，仅能够访问主页， 
登录成功后会生成一个 Session 保存用户信息，并存储在 redis 中，以实现分布式 session 的验证。
```java
// 记住用户登录态
request.getSession().setAttribute(USER_LOGIN_STATE, user);
```
同时还提供了 JWT 的验证方式，在登录成功后也会设置生成的 token 到响应头中去。
由于密钥信息统一设置在 application.yml 配置文件中，所以同样支持分布式登录。
还会将用户信息存入本地缓存中，上下文中可能需要频繁获取登录用户的信息，该做法
能够缓解数据库的压力。 并设置合理的过期时间，防止内存过大和缓存击穿的问题。
```java
// 生成 JWT Token 并设置到响应头中
String jwtToken = JwtUtils.generateToken(loginUserVO.getId());
response.setHeader("Authorization", "Bearer " + jwtToken);
// 存入本地缓存 caffeine 
userCacheMap.put(user.getId(), user);
```
- ### **权限校验**  
当用户访问某个需要权限的接口时，会被权限校验 AOP 进行拦截，
切面类会对打上自定义权限注解的方法执行权限校验的逻辑。
```java
// 用户必须具有管理员身份才能访问该接口
@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)

// AOP 会拦截所有打上@AuthCheck注解的方法 执行权限校验逻辑
@Around("@annotation(authCheck)")
public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
    // 获取注解中的角色信息
    String mustRole = authCheck.mustRole();
    // 获取 request 中的用户信息
    RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
    HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
    
    // 身份校验......
    
    // 通过校验 放行
    return joinPoint.proceed();
}
```

### 应用模块
- #### 基本逻辑
所有已经登录的用户都具有发布应用、参与答题、点赞、评论等功能的使用权限。
可以作为创作者对自己的应用进行编辑、更新操作。给应用设置类型，比如是评分类应用还是测评类应用。
为不同类型的应用设置不同的评分规则。 在主页中，只有已经被审核通过的应用才能被用户看到。
```java
// 主页展示接口
// 只能获取过审的应用 
appQueryRequest.setReviewStatus(ReviewStatusEnum.PASS.getValue());
// 查询数据库
Page<App> appPage = appService.page(new Page<>(current, size),
        appService.getQueryWrapper(appQueryRequest));
// 获取封装类
return ResultUtils.success(appService.getAppVOPage(appPage, request));
```
- #### 设置题目与答案
创作者可以选择自定义生成题目，也可以使用 AI 生成题目。题目以 json 格式存储在数据库中，每道题目为一条记录。
同时包含了难度信息： 0 简单，1 中等，2 困难。
```json
{
  "title": "撒哈拉沙漠位于哪个洲？",
  "options": [
    {
      "result": "错误",
      "score": 0,
      "value": "亚洲",
      "key": "A"
    },
    {
      "result": "正确",
      "score": 1,
      "value": "非洲",
      "key": "B"
    }
  ],
  "difficulty": "0" 
}
```
### 答题与评分模块
用户在答题时获取该应用的题目并且生成唯一的答题记录 id， 这样可以避免用户在一次答题中多次提交，导致产生重复的数据，
在作答完毕后会按照应用设置的评分规则进行评分。 此时后台系统中会采用自定义注解的形式获取应用类型和评分策略。
整体设计采用策略模式，不同应用类型对应不同评分算法，天然适配策略设计模式。
#### 评分规则具体实现
在用户请求评分时，前端向后端发送用户的选项集合、应用的类型、采用的评分策略等，
需要根据这些信息选择对应的评分算法进行评分。但平台中会有多种应用类型和不同的评分策略，
要想动态的根据这些信息匹配到对应的评分算法，最佳实现就是策略设计模式，这样可以大幅减少重复代码的编写，
无论是哪种应用，采用了哪种评分算法，都只需要调用一个评分接口。
##### 策略模式实现
在前端发送的数据中包含应用类型和评分策略信息，此时就可以根据这些信息动态的匹配到对应的评分算法。
但前面提到平台中会有多种应用类型和不同的评分策略，而当前我们知晓这些评分算法都具有共同的行为，
比如都是评分，只是实现不同，就可以抽象出这些行为，作为一个约定，在Java中就需要用到接口来实现约定。
定义一个接口 ScoringStrategy，在接口中定义一个抽象方法 doScore（即约定），用于执行评分操作。
接下来只需要让具有共同行为的评分算法实现该接口即可。这样不同评分算法就可以根据一个接口来实现不同的评分策略。
但也会有新的问题，就是怎样让程序知道当前应用对应了哪个评分算法呢？
其实我们可以为 ScoringStrategy 的实现类打上一些标记，记录应用信息和所需要用到的评分策略，
在程序执行时根据这些标记信息来判断应该采用哪个评分算法。实现该操作需要用到反射机制，动态获取到应用类型和评分策略信息。
那么就可以使用自定义注解的方法来解决。 

定义 ScoringStrategyConfig 注解
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface ScoringStrategyConfig {

    int appType();

    int scoringStrategy();
}
``` 
可以看到 ScoringStrategyConfig 注解中定义了 appType 和 scoringStrategy 两个属性，即应用类型和评分策略。
在实现类中打上该注解，携带应用类型和评分策略信息，这样在程序执行时，就可以根据这些信息来判断应该采用哪个评分算法。
```java
// 例如匹配到测评类应用的自定义评分策略算法
// 将注解修饰在评分策略的实现类中，携带应用类型和评分策略信息
@ScoringStrategyConfig(appType = 1, scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {
    public UserAnswer doScore(List<String> choices, App app) {
        
        // 评分算法具体实现....
        
        // 返回用户答案对象
        return userAnswer;
    }
}
```
但此时的评分接口和这些评分算法的具体实现之间没有直接的关联关系，因此定义一个执行器，将评分接口和评分算法的具体实现进行关联。
在调用评分接口时，需要将应用类型和评分策略信息一起传递给执行器，执行器根据这些信息来选择对应的评分算法进行评分。
由执行器来扫描带有 ScoringStrategyConfig 注解的类，寻找和传入信息匹配的实现类，并执行其中的 doScore 方法。
定义一个 ScoringStrategyExecutor 执行器。
```java
@Slf4j
@Service
public class ScoringStrategyExecutor {
    /**
     * 注入评分策略列表，将所有应用对应的策略注入
     */
    @Resource
    private List<ScoringStrategy> scoringStrategyList;

    public UserAnswer doScore(List<String> choiceList, App app) throws Exception {
        Integer appType = app.getAppType();
        Integer appScoringStrategy = app.getScoringStrategy();
        if (appType == null || appScoringStrategy == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
        }
        // 核心代码 加载所有策略类 通过反射匹配对应算法并执行
        for (ScoringStrategy strategy : scoringStrategyList) {
            if (strategy.getClass().isAnnotationPresent(ScoringStrategyConfig.class)) {
                ScoringStrategyConfig scoringStrategyConfig =
                        strategy.getClass().getAnnotation(ScoringStrategyConfig.class);
                if (scoringStrategyConfig.appType() == appType
                        && scoringStrategyConfig.scoringStrategy() == appScoringStrategy) {
                    return strategy.doScore(choiceList, app);
                }
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
    }
}
```
可以看到上述的代码中，使用了反射机制来动态获取到实现类中的应用类型和评分策略信息，并动态的匹配到对应的评分算法进行评分。
在核心代码中可以注意到，大量使用反射机制，可能会导致性能问题，因此需要优化，同时继续遵循开闭原则，保证足够的可拓展性。

##### 优化思路
动态匹配需要使用反射机制，这是不可避免的，原代码中评分接口被调用时都会执行大量反射，这意味着每位用户调用评分接口，
都需要重复执行反射操作，从而导致性能问题。针对这个问题，我们可以在项目启动之初就直接加载所有的评分算法，并存储在缓存中。
这样只需要根据前端传回的数据，去缓存中匹配对应的评分算法，从而避免每次调用都需要进行大量反射操作。
```java
@Slf4j
@Service
public class ScoringStrategyExecutor {

    /**
     * 注入评分策略列表，将所有应用对应的策略注入
     */
    @Resource
    private List<ScoringStrategy> scoringStrategyList;

    /**
     * 初始化阶段构建映射（应用启动时执行一次）
     * 使用 ConcurrentHashMap 以确保线程安全
     */
    private static final Map<String, ScoringStrategy> strategyMap = new ConcurrentHashMap<>();

    /**
     * 初始化评分策略映射
     * 在应用启动时执行一次，将所有的评分策略根据其配置信息注入到 strategyMap 中
     */
    @PostConstruct
    public void initStrategies() {
        for (ScoringStrategy strategy : scoringStrategyList) {
            // 获取策略类上的 ScoringStrategyConfig 注解
            ScoringStrategyConfig config = strategy.getClass().getAnnotation(ScoringStrategyConfig.class);
            if (config == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "缺少注解信息");
            }
            // 构建键值对，键为 appType 和 scoringStrategy 的组合
            String key = config.appType() + ":" + config.scoringStrategy();
            // 将策略对象放入映射中
            strategyMap.put(key, strategy);
        }
        log.info("评分策略加载完成！");
    }

    /**
     * 根据应用类型和评分策略执行评分
     */
    public UserAnswer doScore(List<TopicAnswerDTO> userAnswer, App app) {
        // 构建查找策略的键
        String key = app.getAppType() + ":" + app.getScoringStrategy();
        // 从映射中获取对应的策略
        ScoringStrategy strategy = strategyMap.get(key);
        if (strategy == null) {
            // 如果找不到匹配的策略，抛出业务异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
        }
        // 执行评分策略
        return strategy.doScore(userAnswer, app);
    }

}
```
这样就得到了一个性能极高的实现方案，优化了性能和可扩展性。

  
**在实际场景中，因为一个应用会被多个用户进行访问，此时会导致答题记录表的内容迅速增长。
因此为了优化查询，引入了 ShardingJDBC 进行数据分片，根据应用的 ID 进行取模分片，大数据量下，
能让单表的查询性能提升300%。**

### AI 模块
- 创作者  
做为应用的作者，可能需要为应用手动添加大量题目，相当耗时。
为了解决这个问题，引入了 AI 模块，根据应用名称和应用描述快速生成对应的题目内容。  

![AI生成题目.png](src/main/resources/script/AI%E7%94%9F%E6%88%90%E9%A2%98%E7%9B%AE.png)

- 答题用户  
在用户完成答题时，如果应用本身的评分策略采用了 AI 评分，此时就需要调用 AI 评分的功能。
通过将应用的题目和用户的答案选项传给 AI ，片刻后就会生成一份详细的评分报告。  

这样会引出一个新的问题，用户请求AI模块时，使用的是默认的主线程，此时如果AI模块的调用时间过长，那么主线程会一直处于阻塞状态，
会导致整个系统出现卡顿现象。为了解决这个问题，引入了线程池隔离，将请求和处理AI响应的线程分离开。
```java
// 线程池配置
@Bean
public Scheduler topicScheduler() {
    ThreadFactory threadFactory = new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "topicSchedulerThreadPool-" + threadNumber.getAndIncrement());
            thread.setDaemon(false); // 设置为非守护线程
            return thread;
        }
    };
    // 自定义线程池
    ThreadPoolExecutor executorService = new ThreadPoolExecutor(
            10, // 核心线程数
            20, // 最大线程数
            60L, // 空闲线程存活时间
            TimeUnit.SECONDS, // 时间单位
            new LinkedBlockingQueue<>(100), // 阻塞队列
            threadFactory, // 自定义线程工厂
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
    );
    return Schedulers.from(executorService);
}
//处理 AI 响应时将使用的线程替换为自定义的线程池
aiResponseStream.observeOn(topicScheduler)...//后续处理逻辑
```

**此外，在实际场景中，如果有某个热门应用，在短时间内可能会有很多用户进行答题访问，此时如果评分策略是 AI，
那么就会出现大量消耗 AI token的情况。而该场景下，往往有一部分用户所填写的答案都是相同的，对于相同答案，
是没有必要再让 AI 生成一份评分结果的。为了解决这个需求，采用分布式锁保证同一时间仅有一个请求访问到 AI，
同时再生成完毕后，将结果保存在缓存中，并设置合理过期时间，以防造成缓存击穿。**  

- 具体实现
```java
// 从缓存中获取答案 若命中直接返回结果
String answerJson = answerCacheMap.getIfPresent(cacheKey);
return userAnswer;
// 否则需要保证相同的答案在同一时间内，只有一个用户调用 AI 评分模块
RLock lock = redissonClient.getLock(AIGeneralMessage.AI_ANSWER_LOCK + cacheKey);

// 进行评分....

// 结束后将结果添加到缓存 并将锁释放
answerCacheMap.put(cacheKey, json);
lock.unlock();
```

### 点赞模块
用户可以给自己喜欢的应用进行点赞收藏，也意味着在某一时段会有某个明星应用突然爆火，点赞量激增。
如果每个点赞操作都需要操作一次数据库，则会导致数据库持续高压，系统可靠性降低。
- 解决方式：  
    将点赞或取消点赞视为一次操作，构建合适的 key 存储在 redis 中，并添加定时任务，让这些请求分批次、
    分时间段的批量从缓存中，载入数据库，形成一个异步的操作。减少数据库的压力，即使点赞量激增，请求会落在
    redis 中，具备相当高的并发量。

#### 具体实现：
在点赞模块中，设计两张表：点赞记录表和点赞总数表
记录表负责记录点赞的具体应用、点赞的用户、时间等信息
总数表记录每个应用的总点赞数量。所以有如下的两个定时任务，每隔一段时间将缓存中的数据同步到数据库中去：
```java
@Scheduled(fixedDelay = 1000 * 60 * 2)
public void syncLikeRecordsScheduled(){
    // 更新记录表到数据库....
}
@Scheduled(fixedDelay = 1000 * 60 * 2)
public void syncLikeCountsScheduled(){
    // 更新点赞总数到数据库....
}
```
这样就保证了系统整体的可靠性，每个模块的职责单一，高内聚低耦合。


