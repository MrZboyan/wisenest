package com.demo.WiseNest.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.WiseNest.model.dto.question.AiGenerateQuestionRequest;
import com.demo.WiseNest.model.dto.question.QuestionContentDTO;
import com.demo.WiseNest.model.dto.question.QuestionQueryRequest;
import com.demo.WiseNest.model.entity.Question;
import com.demo.WiseNest.model.vo.QuestionVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 题目服务
 */
public interface QuestionService extends IService<Question> {

    /**
     * 校验数据
     *
     * @param question  题目
     * @param add 对创建的数据进行校验
     */
    void validQuestion(Question question, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest 查询条件
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);
    
    /**
     * 获取题目封装
     *
     * @param question 题目
     */
    QuestionVO getQuestionVO(Question question, HttpServletRequest request);

    /**
     * 分页获取题目封装
     *
     * @param questionPage 分页数据
     */
    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);

    /**
     * 获取AI生成题目
     *
     * @param aiGenerateQuestionRequest ai生成请求
     * @return 生成的数据
     */
    List<QuestionContentDTO> getAiGenerateQuestion(AiGenerateQuestionRequest aiGenerateQuestionRequest);

    /**
     * 获取AI生成题目SSE
     *
     * @param aiGenerateQuestionRequest ai生成请求
     * @return 生成的数据
     */
    SseEmitter getAiGenerateQuestionSSE(AiGenerateQuestionRequest aiGenerateQuestionRequest, HttpServletRequest request);

    /**
     * 获取AI生成题目 QWen
     *
     * @param aiGenerateQuestionRequest ai生成请求
     * @return 生成的数据
     */
    List<QuestionContentDTO> getAiGenerateQuestionMax(AiGenerateQuestionRequest aiGenerateQuestionRequest);

    /**
     * 获取AI生成题目SSE QWen
     *
     * @param aiGenerateQuestionRequest ai生成请求
     */
    SseEmitter getAiGenerateQuestionSSEMax(AiGenerateQuestionRequest aiGenerateQuestionRequest, HttpServletRequest request);
}
