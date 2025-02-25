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
格式，并推送到客户端进行实时填充，让创作者可以随时对题目内容进行修改，对于 VIP 创作者将获得专有的 AI 访问资源，生成更加迅速。
创作者在完成自己的应用之后，可以提交审核，此时管理员审核通过之后，应用会自动发布到平台主页中，可以被所有用户访问。
用户在作答之后会根据创作者所制定的评分规则，得到自己的评分结果，如果创作者选用 AI 评分，那用户将会得到一份非常详细的评分报告。
用户可以在个人中心查看自己过去所用的答题记录；此外，用户还可以对自己喜欢的应用进行点赞收藏，也可以通过二维码的形式对应用进行分享。  

在管理员方面，管理员可以对所有用户进行管控，审核创作者提交的应用信息、评分规则、应用试题等。管理员可以查看所有用户的答题记录，
并且根据单个应用的热门答案进行统计分析，并把它们可视化为饼状图。还能根据点赞数，按照天、周、月、年来分析时段内的明星应用。  
  
  
## 整体流程图
![流程图.png](src/main/resources/script/%E6%B5%81%E7%A8%8B%E5%9B%BE.png)
## 功能模块
- 用户登录与权限校验
- 应用模块
- 答题模块
- 评分模块
- 点赞模块
- AI 模块

### 用户登录与权限校验

- ### **用户登录**  

用户可以注册账号进行登录，后端校验通过后可以正常使用平台的所有功能，并且会生成一个 Session
保存用户信息，并存储在 redis 中，以实现分布式 session 的验证。
```java
// 记住用户登录态
request.getSession().setAttribute(USER_LOGIN_STATE, user);
```
同时还提供了 JWT 的验证方式，在登录成功后也会设置生成的 token 到响应头中去。
由于密钥信息统一设置在 yml 配置文件中，所以同样支持分布式登录。
还会将用户信息存入本地缓存中，缓解频繁获取登录用户信息的场景下，数据库的压力。
并设置合理的过期时间，防止内存过大和缓存击穿的问题。
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
所有已经登录的用户都具有发布应用、参与答题、点赞收藏等功能的使用权限。
可以作为创作者对自己的应用进行编辑更改等操作。给应用设置类型，比如时评分类应用还是测评类应用。
为不同类型的应用设置不同的评分规则。
在主页中，只有已经被审核通过的应用才能被用户看到。
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
创作者可以选择自定义生成题目，也可以使用 AI 生成题目。题目与答案以 json 格式存储在数据库中。
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
  ]
}
```
### 答题与评分模块
用户在答题时获取该应用的题目并且生成唯一的答题记录 id，
这样可以避免用户在一次答题中多次提交，导致产生重复的数据，
在作答完毕后会按照应用设置的评分规则进行评分。 
此时后台系统中会采用自定义注解的形式获取应用类型和评分策略。整体设计采用策略模式，
不同应用类型对应不同评分算法，天然适配策略模式。
#### 评分规则具体实现
定义 ScoringStrategyExecutor 执行器 根据注解信息判断采用哪个评分算法。
```java
// 例如匹配到测评类应用的自定义评分策略算法
// 将注解修饰在评分策略的实现类中，携带应用类型和评分策略信息
@ScoringStrategyConfig(appType = 1, scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {
    public UserAnswer doScore(List<String> choices, App app) {
        
        // 评分算法....
        
        // 返回用户答案对象
        return userAnswer;
    }
}
```
  
**在实际场景中，因为一个应用会被多个用户进行访问，此时会导致答题记录表的内容迅速增长。
因此为了优化查询，引入了 ShardingJDBC 进行数据分片，根据应用的 ID 进行分表，大数据量下，
能让单表的查询性能提升300%。**

### AI 模块
- 创作者  

    做为应用的作者，可能需要为应用手动添加大量题目，相当耗时。
为了解决这个问题，引入了 AI 模块，根据应用名称和应用描述快速生成对应的题目内容。  

![AI生成题目.png](src/main/resources/script/AI%E7%94%9F%E6%88%90%E9%A2%98%E7%9B%AE.png)  

为避免等待时间过长，需要对 AI 所生成的题目进行实时的返回，优化用户的体验。
此外还会**根据用户的身份（普通用户/ VIP 用户）来使用不同的 AI 访问资源**，进一步提高生成的效率和体验。
```java
// 判断用户是否为vip或管理员用户 如果是则使用自定义的线程池
Scheduler scheduler = Schedulers.io();
if ("vip".equals(loginUser.getUserRole()) || "admin".equals(loginUser.getUserRole())) {
    scheduler = vipScheduler;
}
```

- 答题用户  
    在用户完成答题时，如果应用本身的评分策略采用了 AI 评分，此时就需要调用 AI 评分的功能。
    通过将应用的题目和用户的答案选项传给 AI ，片刻后就会生成一份详细的评分报告。  

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
总鼠标记录每个应用的总点赞数量。所以有如下的两个定时任务，每隔一段时间将缓存中的数据同步到数据库中去：
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
这样就保证了系统整体的可靠性，每个模块的职责单一，高内聚底耦合。


