package com.demo.WiseNest.model.vo;

import cn.hutool.json.JSONUtil;
import com.demo.WiseNest.model.dto.question.QuestionContentDTO;
import com.demo.WiseNest.model.entity.Question;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 题目视图 用于传回前端的展示信息
 */
@Data
public class QuestionVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 题目内容（json格式）
     */
    private List<QuestionContentDTO> questionContent;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建用户信息
     */
    private UserVO user;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     */
    public static Question voToObj(QuestionVO questionVO) {
        if (questionVO == null) {
            return null;
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionVO, question);
        List<QuestionContentDTO> content = questionVO.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(content));
        return question;
    }

    /**
     * 对象转封装类
     */
    public static QuestionVO objToVo(Question question) {
        if (question == null) {
            return null;
        }
        QuestionVO questionVO = new QuestionVO();
        BeanUtils.copyProperties(question, questionVO);
        if (question.getQuestionContent() != null) {
            questionVO.setQuestionContent(JSONUtil.toList(question.getQuestionContent(), QuestionContentDTO.class));

        }
        return questionVO;
    }
}
