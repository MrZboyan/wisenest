package com.demo.WiseNest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.WiseNest.model.dto.statistic.AppAnswerCountDTO;
import com.demo.WiseNest.model.dto.statistic.AppAnswerResultCountDTO;
import com.demo.WiseNest.model.entity.UserAnswer;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author Zangdibo
 * description 针对表【user_answer(用户答题记录)】的数据库操作Mapper
 * createDate 2024-09-07 12:53:24
 * Entity com.demo.WiseNest.model.entity.UserAnswer
 */
public interface UserAnswerMapper extends BaseMapper<UserAnswer> {

    // 查询某个应用的前十个热门回答
    @Select("SELECT ua.appId AS appId, a.appName AS appName, COUNT(ua.userId) AS answerCount " +
            "FROM user_answer ua " +
            "JOIN app a ON ua.appId = a.Id " +
            "GROUP BY ua.appId, a.appName " +
            "ORDER BY answerCount DESC " +
            "LIMIT 10;")
    List<AppAnswerCountDTO> doAppAnswerCount();


    // 查询回答数最多的热门应用 倒序
    @Select("select resultName, count(resultName) as resultCount from user_answer " +
            "where appId = #{appId} group by resultName order by resultCount desc")
    List<AppAnswerResultCountDTO> doAppAnswerResultCount(Long appId);

}




