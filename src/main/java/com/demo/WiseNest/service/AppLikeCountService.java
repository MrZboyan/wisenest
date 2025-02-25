package com.demo.WiseNest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.WiseNest.model.dto.appLike.AppLikeCountCreateRequest;
import com.demo.WiseNest.model.dto.appLike.AppLikeCountDTO;
import com.demo.WiseNest.model.entity.AppLikeCount;

import java.util.List;

/**
 * 点赞总数
 */
public interface AppLikeCountService extends IService<AppLikeCount> {

    /**
     * 为应用创建总点赞数记录
     *
     * @param addRequest 添加请求
     */
    void createAppLikeCount(AppLikeCountCreateRequest addRequest);

    /**
     * 获取热门应用点赞数
     *
     * @return 点赞数
     */
    List<AppLikeCountDTO> doAppLikeCountTop();

    /**
     * 获取应用点赞数
     *
     * @param appId 应用id
     */
    Long doAppLikeCount(Long appId);

}
