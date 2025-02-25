package com.demo.WiseNest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.WiseNest.model.dto.appLike.AppLikeCountDTO;
import com.demo.WiseNest.model.entity.AppLikeCount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author Zangdibo
 * description 针对表【app_like_count】的数据库操作Mapper
 * createDate 2025-02-13 21:12:12
 * Entity com.demo.WiseNest.model.entity.AppLikeCount
 */
public interface AppLikeCountMapper extends BaseMapper<AppLikeCount> {

    /**
     * 获取热门应用点赞数
     * 根据 appId 按照点赞数降序排列前 10 个热门应用
     *
     * @return 点赞数
     */
    @Select("SELECT appId, likeCount " +
            "FROM app_like_count " +
            "WHERE isDelete = 0 " +
            "ORDER BY likeCount DESC " +
            "LIMIT 10;")
    List<AppLikeCountDTO> doAppLikeCount();

    /**
     * 更新点赞数
     *
     * @param appId 应用ID
     * @param delta 变化量
     */
    @Update("UPDATE app_like_count " +
            "SET likeCount = app_like_count.likeCount + #{delta} " +
            "WHERE appId = #{appId}")
    void incrementCount(@Param("appId") Long appId, @Param("delta") Long delta);

}




