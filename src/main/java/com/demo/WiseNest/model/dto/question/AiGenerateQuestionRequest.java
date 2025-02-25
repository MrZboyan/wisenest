package com.demo.WiseNest.model.dto.question;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建题目请求
 */
@Data
public class AiGenerateQuestionRequest implements Serializable {
    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 生成题目的数量
     */
    private Integer questionNumber = 10;

    /**
     * 选项数
     */
    private Integer optionNumber = 4;

    @Serial
    private static final long serialVersionUID = 1L;
}
