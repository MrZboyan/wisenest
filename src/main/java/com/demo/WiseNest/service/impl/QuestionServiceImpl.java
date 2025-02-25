package com.demo.WiseNest.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.app.ApplicationResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.WiseNest.api.ChatGLM.AIGeneralMessage;
import com.demo.WiseNest.api.ChatGLM.AiManager;
import com.demo.WiseNest.api.QWen.QWenAIClient;
import com.demo.WiseNest.constant.CommonConstant;
import com.demo.WiseNest.exception.ErrorCode;
import com.demo.WiseNest.exception.ThrowUtils;
import com.demo.WiseNest.mapper.QuestionMapper;
import com.demo.WiseNest.model.dto.question.AiGenerateQuestionRequest;
import com.demo.WiseNest.model.dto.question.QuestionContentDTO;
import com.demo.WiseNest.model.dto.question.QuestionQueryRequest;
import com.demo.WiseNest.model.entity.App;
import com.demo.WiseNest.model.entity.Question;
import com.demo.WiseNest.model.entity.User;
import com.demo.WiseNest.model.enums.AppTypeEnum;
import com.demo.WiseNest.model.vo.QuestionVO;
import com.demo.WiseNest.model.vo.UserVO;
import com.demo.WiseNest.service.AppService;
import com.demo.WiseNest.service.QuestionService;
import com.demo.WiseNest.service.UserService;
import com.demo.WiseNest.utils.SqlUtils;
import com.zhipu.oapi.service.v4.model.ModelData;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 题目服务实现
 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    @Resource
    private AiManager aiManager;

    @Resource
    private Scheduler vipScheduler;

    @Resource
    private QWenAIClient qWenAIClient;

    // 处理拼接字符串 高效安全
    private static final ThreadLocal<StringBuilder> threadLocalBuilder =
            ThreadLocal.withInitial(StringBuilder::new);

    // region 基本逻辑
    /**
     * 校验数据
     *
     * @param question 题目
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        Long appId = question.getAppId();
        String questionContent = question.getQuestionContent();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(questionContent), ErrorCode.PARAMS_ERROR, "题目内容不能为空");
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 非法");
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
        if (appId != null) {
            App app = appService.getById(appId);
            ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest 获取查询条件请求
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionQueryRequest.getId();
        String questionContent = questionQueryRequest.getQuestionContent();
        Long appId = questionQueryRequest.getAppId();
        Long userId = questionQueryRequest.getUserId();
        Long notId = questionQueryRequest.getNotId();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // todo 补充需要的查询条件
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(questionContent), "questionContent", questionContent);

        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(appId), "appId", appId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_DESC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param question 题目
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);

        // endregion

        return questionVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionPage 分页对象
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(),
                questionPage.getSize(),
                questionPage.getTotal());

        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream()
                .map(QuestionVO::objToVo)
                .collect(Collectors.toList());

        // 关联查询用户信息
        Set<Long> userIdSet = questionList.stream()
                .map(Question::getUserId)
                .collect(Collectors.toSet());
        // 以id 作为键存入到map中 方便后续操作
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                // 最终生成一个以用户 id为 key 用户数据为 value 的 map
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
        });

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }
    // endregion

    // region Ai生成题目
    /**
     * 生成题目的用户消息
     *
     * @param app 应用
     * @param questionNumber 题目数量
     * @param optionNumber 选项数量
     */
    private String getGenerateQuestionUserMessage(App app, int questionNumber, int optionNumber) {
        // Java 5以上版本 会将连续的字符串拼接自动优化为 StringBuilder，因此于显式调用 StringBuilder 的性能几乎相同。
        return app.getAppName() + "\n" +
                app.getAppDesc() + "\n" +
                Objects.requireNonNull(AppTypeEnum.getEnumByValue(app.getAppType())).getText() + "\n" +
                questionNumber + "\n" +
                optionNumber;
    }

    /**
     * 流式返回字符串处理
     */
    private void processCharacter(char c, AtomicInteger count, StringBuilder userMessageBuilder,
                                  SseEmitter sseEmitter) throws IOException {
        // 统计括号计数
        if (c == '{') {
            count.incrementAndGet();
        }
        if (count.get() > 0) {
            userMessageBuilder.append(c);
        }
        // 当遇到右括号时，判断是否为完整的 JSON 数据
        if (c == '}') {
            count.decrementAndGet();
            if (count.get() == 0) {
                // 发送完整 JSON
                sseEmitter.send(JSONUtil.toJsonStr(userMessageBuilder.toString()));
                userMessageBuilder.setLength(0); // 重置
            }
        }
        /*
        当前字符
        public char c;

        作用：统计左括号 { 的数量
        递增：每当遇到左括号 { 时，计数器增加1。
        递减：每当遇到右括号 } 时，计数器减少1。
        判断：通过计数器的值来判断当前是否处于一个完整的JSON对象内部，
        当计数器为0时，表示一个完整的JSON对象已经结束。
        public AtomicInteger count;

        在计数器大于 0 的情况下，将当前字符追加到 StringBuilder 中
        public StringBuilder userMessageBuilder;

        流式响应
        public SseEmitter sseEmitter;
        */
    }

    /**
     * 获取Ai生成题目 GLM
     *
     * @param aiGenerateQuestionRequest 请求参数
     */
    @Override
    public List<QuestionContentDTO> getAiGenerateQuestion(AiGenerateQuestionRequest aiGenerateQuestionRequest) {
        // 校验参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        // 获取应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 封装 Prompt
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        // todo 调试信息
        // System.out.println(userMessage);
        // AI 生成
        String result = aiManager.doSyncStableRequest(AIGeneralMessage.GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage);

        // 截取需要的 JSON 信息
        int start = result.indexOf("[");
        int end = result.lastIndexOf("]");

        String json = result.substring(start, end + 1);
        return JSONUtil.toList(json, QuestionContentDTO.class);
    }

    /**
     * 获取AI生成题目 QWen
     *
     * @param aiGenerateQuestionRequest ai生成请求
     * @return 生成的数据
     */
    @Override
    public List<QuestionContentDTO> getAiGenerateQuestionMax(AiGenerateQuestionRequest aiGenerateQuestionRequest) {
        // 1. 获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        // 2. 校验应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 封装 Prompt
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);

        // 4. 调用 AI 生成题目
        String result = qWenAIClient.generateQuestion(userMessage);
        // 截取需要的 JSON 信息
        int start = result.indexOf("[");
        int end = result.lastIndexOf("]");

        String json = result.substring(start, end + 1);
        // 5. 解析并返回目标 JSON
        return JSONUtil.toList(json, QuestionContentDTO.class);
    }

    /**
     * 获取Ai生成题目 流式
     *
     * @param aiGenerateQuestionRequest 请求参数
     */
    @Override
    public SseEmitter getAiGenerateQuestionSSE(AiGenerateQuestionRequest aiGenerateQuestionRequest,
                                               HttpServletRequest request) {
        // 获取参数
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        User loginUser = userService.getLoginUser(request);

        // 获取应用信息
        Long appId = aiGenerateQuestionRequest.getAppId();
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        // 封装 Prompt
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        // todo 调试信息
        // System.out.println(userMessage);
        // 建立sse对象 参数0L代表不超时
        SseEmitter sseEmitter = new SseEmitter(0L);

        // 左括号计数器 生成后返回为json格式，每道题以 { 开头，以 } 结尾 此处需要线程安全 保证原子性
        AtomicInteger count = new AtomicInteger(0);

        // 拼接字符串 定义一个高性能的字符串变量 用于拼接 并且保证线程安全
        StringBuilder userMessageBuilder = threadLocalBuilder.get();

        // 判断用户是否为vip或管理员用户如果是则使用全局线程池
        Scheduler scheduler = Schedulers.io();
        if ("vip".equals(loginUser.getUserRole()) || "admin".equals(loginUser.getUserRole())) {
            scheduler = vipScheduler;
        }
        // AI 生成 流式返回
        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(AIGeneralMessage.GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage, 0.99f);
        modelDataFlowable
                // 指定操作在对应线程池进行
                .observeOn(scheduler)
                // 295 - 298 行代码为处理返回的流式数据 获取内容
                .map(modelData -> modelData.getChoices().get(0).getDelta().getContent())
                // 将匹配到的所有字符串中的换行符和制表符替换为空格
                .map(massage -> massage.replace("\t", " ").replace("\n", " "))
                // 过滤空字符串 只保留非空字符串
                .filter(StrUtil::isNotBlank)
                // 将字符串拆分为字符流
                .flatMap(massage -> Flowable.fromIterable(() -> massage.chars().mapToObj(c -> (char) c).iterator()))
                // 将流式数据发送给客户端
                .doOnNext(c -> {
                    // todo 调试信息
                    System.out.print(c);
                    this.processCharacter(c, count, userMessageBuilder, sseEmitter);
                })
                // 如果在处理过程中发生错误，调用 sseEmitter.completeWithError 终止连接，并向客户端发送错误信息。
                .doOnError(throwable -> {
                    log.error("处理 SSE 时出错: {}", throwable.getMessage(), throwable);
                    sseEmitter.send("发生意外错误，请稍后重试");
                    sseEmitter.completeWithError(throwable);
                })
                // 当流结束时，调用 sseEmitter.complete() 结束连接。
                .doOnComplete(sseEmitter::complete)
                // 观察者流 这使得前边所有操作链开始执行
                .subscribe();
        // 清除ThreadLocal 防止内存泄漏
        userMessageBuilder.setLength(0);
        threadLocalBuilder.remove();

        return sseEmitter;
    }

    /**
     * 获取Ai生成题目 流式 QWen
     *
     * @param aiGenerateQuestionRequest 请求参数
     */
    @Override
    public SseEmitter getAiGenerateQuestionSSEMax(AiGenerateQuestionRequest aiGenerateQuestionRequest,
                                               HttpServletRequest request) {
        // 获取参数
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        User loginUser = userService.getLoginUser(request);

        // 获取应用信息
        Long appId = aiGenerateQuestionRequest.getAppId();
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        // 封装 Prompt
        String userMessage = this.getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        // todo 调试信息
        // System.out.println(userMessage);
        // 建立sse对象 参数0L代表不超时
        SseEmitter sseEmitter = new SseEmitter(0L);

        // 左括号计数器 生成后返回为json格式，每道题以 { 开头，以 } 结尾 此处需要线程安全 保证原子性
        AtomicInteger count = new AtomicInteger(0);

        // 拼接字符串 定义一个高性能的字符串变量 用于拼接 并且保证线程安全
        StringBuilder userMessageBuilder = threadLocalBuilder.get();

        // 判断用户是否为vip或管理员用户如果是则使用全局线程池
        Scheduler scheduler = Schedulers.io();
        if ("vip".equals(loginUser.getUserRole()) || "admin".equals(loginUser.getUserRole())) {
            scheduler = vipScheduler;
        }
        Flowable<ApplicationResult> dataResultFlowable = qWenAIClient.generateQuestionStream(userMessage);
        dataResultFlowable
                // 指定操作在对应线程池进行
                .observeOn(scheduler)
                // 下方两行代码为处理返回的流式数据 获取内容
                .map(dataResult -> dataResult.getOutput().getText())
                // 将匹配到的所有字符串中的换行符和制表符替换为空格
                .map(massage -> massage.replace("\t", " ").replace("\n", " "))
                // 过滤空字符串 只保留非空字符串
                .filter(StrUtil::isNotBlank)
                // 将字符串拆分为字符流
                .flatMap(massage -> Flowable.fromIterable(() -> massage.chars().mapToObj(c -> (char) c).iterator()))
                // 将流式数据发送给客户端
                .doOnNext(c -> {
                    // todo 调试信息
                    System.out.print(c);
                    this.processCharacter(c, count, userMessageBuilder, sseEmitter);
                })
                // 如果在处理过程中发生错误，调用 sseEmitter.completeWithError 终止连接，并向客户端发送错误信息。
                .doOnError(throwable -> {
                    log.error("处理 SSE 时出错: {}", throwable.getMessage(), throwable);
                    sseEmitter.send("发生意外错误，请稍后重试");
                    sseEmitter.completeWithError(throwable);
                })
                // 当流结束时，调用 sseEmitter.complete() 结束连接。
                .doOnComplete(sseEmitter::complete)
                // 观察者流 这使得前边所有操作链开始执行
                .subscribe();
        // 清除ThreadLocal 防止内存泄漏
        userMessageBuilder.setLength(0);
        threadLocalBuilder.remove();

        return sseEmitter;
    }
    // endregion
}
